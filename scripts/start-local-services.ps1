param([switch]$SkipAdb)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$localRoot = Join-Path $projectRoot '.local'
$backendRoot = Join-Path $projectRoot 'backend'
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
$jdk = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
if ($env:PERSONAS_PG_BIN) { $pgBin = $env:PERSONAS_PG_BIN }
if ($env:PERSONAS_JAVA_HOME) { $jdk = $env:PERSONAS_JAVA_HOME }
$java = Join-Path $jdk 'bin\java.exe'
$pgData = Join-Path $localRoot 'postgres'
$port = 55440
New-Item -ItemType Directory -Path $localRoot -Force | Out-Null
$shortTemp = Join-Path $localRoot 'tmp'
New-Item -ItemType Directory -Path $shortTemp -Force | Out-Null
$previousTemp = $env:TEMP; $previousTmp = $env:TMP
$env:TEMP = $shortTemp; $env:TMP = $shortTemp
function Hash-Text([string]$value) {
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($value)))).Replace('-', '').ToLowerInvariant() } finally { $sha.Dispose() }
}
function Source-Hash {
    $entries = @(Get-Item (Join-Path $backendRoot 'pom.xml')) + @(Get-ChildItem (Join-Path $backendRoot 'src') -Recurse -File | Sort-Object FullName)
    $hashes = foreach ($entry in $entries) {
        $sha = [Security.Cryptography.SHA256]::Create()
        $stream = [IO.File]::OpenRead($entry.FullName)
        try { $entry.FullName.Substring($backendRoot.Length) + ':' + [BitConverter]::ToString($sha.ComputeHash($stream)) } finally { $stream.Dispose(); $sha.Dispose() }
    }
    return Hash-Text ($hashes -join "`n")
}
function Health {
    try { return Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/health' -TimeoutSec 2 } catch { return $null }
}
function Hidden-Process([string]$exe, [string]$arguments, [string]$directory) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $exe; $info.Arguments = $arguments; $info.WorkingDirectory = $directory
    $info.UseShellExecute = $true; $info.WindowStyle = [Diagnostics.ProcessWindowStyle]::Hidden
    return [Diagnostics.Process]::Start($info)
}
function Psql([string]$database, [string]$sql) {
    $result = & (Join-Path $pgBin 'psql.exe') '-w' '-h' '127.0.0.1' '-p' "$port" '-U' 'personas' '-d' $database '-v' 'ON_ERROR_STOP=1' '-At' '-c' $sql
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar el PostgreSQL propio de Personas.' }
    return $result
}
$mutex = New-Object Threading.Mutex($false, ('Local\Personas_' + (Hash-Text $projectRoot).Substring(0, 24)))
$locked = $false
$previousPgPassword = $env:PGPASSWORD
try {
    try { $locked = $mutex.WaitOne(180000) } catch [Threading.AbandonedMutexException] { $locked = $true }
    if (-not $locked) { throw 'Otro arranque de Personas sigue en curso. Vuelve a ejecutar Run.' }
    if (-not (Test-Path $java) -or -not (Test-Path (Join-Path $pgBin 'initdb.exe'))) { throw 'Instala Java 25 y PostgreSQL 18, o configura PERSONAS_JAVA_HOME y PERSONAS_PG_BIN.' }
    $envFile = Join-Path $backendRoot '.env'
    if (-not (Test-Path $envFile)) {
        $random = New-Object byte[] 32
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($random) } finally { $rng.Dispose() }
        $password = ([BitConverter]::ToString($random)).Replace('-', '').ToLowerInvariant()
        [IO.File]::WriteAllText($envFile, "DB_URL=jdbc:postgresql://127.0.0.1:$port/personas_app`nDB_USER=personas`nDB_PASSWORD=$password`nPORT=8081`nPHOTO_DIRECTORY=storage/fotos`n", [Text.UTF8Encoding]::new($false))
    }
    $passwordLine = Get-Content $envFile | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
    if (-not $passwordLine) { throw 'backend/.env no contiene DB_PASSWORD.' }
    $env:PGPASSWORD = $passwordLine.Substring(12).Trim().Trim('"')
    if (-not (Test-Path (Join-Path $pgData 'PG_VERSION'))) {
        $passwordFile = Join-Path $localRoot 'init-password.tmp'
        [IO.File]::WriteAllText($passwordFile, $env:PGPASSWORD, [Text.UTF8Encoding]::new($false))
        try {
            & (Join-Path $pgBin 'initdb.exe') '-D' $pgData '-U' 'personas' '-E' 'UTF8' '--locale=C' '--auth=scram-sha-256' "--pwfile=$passwordFile" | Out-Null
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo inicializar el cluster PostgreSQL de Personas.' }
        } finally { Remove-Item -LiteralPath $passwordFile -Force -ErrorAction SilentlyContinue }
    }
    & (Join-Path $pgBin 'pg_isready.exe') '-h' '127.0.0.1' '-p' "$port" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        $pgArguments = "-D `"$pgData`" -l `"$(Join-Path $localRoot 'postgres.log')`" -o `"-p $port -h 127.0.0.1`" -w -t 40 start"
        $pgStart = Hidden-Process (Join-Path $pgBin 'pg_ctl.exe') $pgArguments $projectRoot
        if (-not $pgStart.WaitForExit(45000) -or $pgStart.ExitCode -ne 0) { throw 'No se pudo iniciar PostgreSQL. Revisa .local/postgres.log.' }
    }
    $runningData = (Psql 'postgres' 'SHOW data_directory').Replace('/', '\').TrimEnd('\')
    if ($runningData -ine $pgData.TrimEnd('\')) { throw "El puerto $port pertenece a otro cluster. No se modifico ese servicio." }
    if ((Psql 'postgres' "SELECT 1 FROM pg_database WHERE datname='personas_app'") -ne '1') { Psql 'postgres' 'CREATE DATABASE personas_app' | Out-Null }
    $instanceFile = Join-Path $localRoot 'instance.txt'
    if (-not (Test-Path $instanceFile)) { [IO.File]::WriteAllText($instanceFile, [Guid]::NewGuid().ToString()) }
    $instance = [IO.File]::ReadAllText($instanceFile).Trim()
    $sourceHash = Source-Hash
    $stateFile = Join-Path $localRoot 'api-state.json'
    $state = if (Test-Path $stateFile) { Get-Content $stateFile -Raw | ConvertFrom-Json } else { $null }
    $health = Health
    if ($health -and ($health.instancia -ne $instance -or $health.aplicacion -ne 'personas')) { throw 'El puerto 8081 pertenece a otra API. No se detuvo ningun servicio ajeno.' }
    $ownedProcess = $null
    if ($state) {
        $candidate = Get-Process -Id $state.pid -ErrorAction SilentlyContinue
        if ($candidate -and $candidate.StartTime.ToUniversalTime().Ticks.ToString() -eq $state.startTicks -and $candidate.Path -ieq $java -and $state.instance -eq $instance) { $ownedProcess = $candidate }
    }
    if (-not ($health -and $health.estado -eq 'ok' -and $health.version -eq $sourceHash -and $state.hash -eq $sourceHash)) {
        if ($health -and -not $ownedProcess) { throw 'La API necesita actualizarse pero no se pudo verificar su proceso. No se detuvo ningun servicio.' }
        if ($ownedProcess) { $ownedProcess.Kill(); $ownedProcess.WaitForExit(15000) | Out-Null }
        $jar = Join-Path $backendRoot 'target\personas-api-1.0.0.jar'
        $builtFile = Join-Path $localRoot 'backend-build.txt'
        if (-not (Test-Path $jar) -or -not (Test-Path $builtFile) -or [IO.File]::ReadAllText($builtFile).Trim() -ne $sourceHash) {
            Write-Output 'Compilando y verificando la API de Personas...'
            & (Join-Path $PSScriptRoot 'maven.ps1') -MavenArguments @('test', 'package')
            [IO.File]::WriteAllText($builtFile, $sourceHash)
        }
        $log = (Join-Path $localRoot 'api.log').Replace('\', '/')
        $arguments = "-Dapp.instance=$instance -Dapp.build=$sourceHash -Dlogging.file.name=`"$log`" -jar `"$jar`""
        $apiProcess = Hidden-Process $java $arguments $backendRoot
        $state = @{ pid=$apiProcess.Id; startTicks=$apiProcess.StartTime.ToUniversalTime().Ticks.ToString(); instance=$instance; hash=$sourceHash }
        [IO.File]::WriteAllText($stateFile, ($state | ConvertTo-Json))
        $ready = $false
        for ($attempt = 0; $attempt -lt 80; $attempt++) {
            $apiProcess.Refresh()
            if ($apiProcess.HasExited) { throw 'La API se detuvo. Revisa .local/api.log.' }
            $health = Health
            if ($health -and $health.estado -eq 'ok' -and $health.instancia -eq $instance -and $health.version -eq $sourceHash) { $ready = $true; break }
            Start-Sleep -Milliseconds 750
        }
        if (-not $ready) { throw 'La API no confirmo su conexion con PostgreSQL. Revisa .local/api.log.' }
    }
    if (-not $SkipAdb) {
        $sdkCandidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
        $sdkProperties = Join-Path $projectRoot 'local.properties'
        if (Test-Path $sdkProperties) {
            $sdkLine = Get-Content $sdkProperties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
            if ($sdkLine) { $sdkCandidates += $sdkLine.Substring(8).Replace('\:', ':').Replace('\\', '\') }
        }
        $sdkCandidates += 'C:\Android\Sdk'
        $adb = $sdkCandidates | Where-Object { $_ } | ForEach-Object { Join-Path $_ 'platform-tools\adb.exe' } |
            Where-Object { Test-Path $_ } | Select-Object -First 1
        if ($adb) {
            $adbServer = Hidden-Process $adb 'start-server' $projectRoot
            if ($adbServer.WaitForExit(15000)) {
                $devices = & $adb 'devices'
                foreach ($line in $devices) {
                    if ($line -match '^(\S+)\s+device$') {
                        $serial = $Matches[1]
                        if ($serial -notlike 'emulator-*') { & $adb '-s' $serial 'reverse' 'tcp:8081' 'tcp:8081' | Out-Null }
                    }
                }
            }
        }
    }
    Write-Output 'Personas listo: PostgreSQL propio en 55440 y API en 8081.'
} finally {
    $env:PGPASSWORD = $previousPgPassword
    $env:TEMP = $previousTemp; $env:TMP = $previousTmp
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
