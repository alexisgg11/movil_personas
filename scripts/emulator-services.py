"""Run an isolated real API/database while emulator UI tests are in progress."""
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import urllib.request
import uuid

root = Path(__file__).resolve().parents[1]
qa = root / '.local/emulator-qa'
qa.mkdir(parents=True, exist_ok=True)
token = uuid.uuid4().hex[:12]
database = 'personas_emulator_' + token
work = qa / token
work.mkdir()
env = os.environ.copy()
for line in (root / 'backend/.env').read_text(encoding='utf-8-sig').splitlines():
    if '=' in line and not line.startswith('#'):
        key, value = line.split('=', 1)
        env[key] = value.strip().strip('"')
env['PGPASSWORD'] = env['DB_PASSWORD']
env['TMP'] = env['TEMP'] = str(root / '.local/tmp')
pg = Path(env.get('PERSONAS_PG_BIN', r'C:\Program Files\PostgreSQL\18\bin'))
java = Path(env.get('PERSONAS_JAVA_HOME', r'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot')) / 'bin/java.exe'
def sql(db, statement):
    p = subprocess.run([str(pg / 'psql.exe'), '-w', '-h', '127.0.0.1', '-p', '55440', '-U', 'personas', '-d', db, '-v', 'ON_ERROR_STOP=1', '-At', '-c', statement], env=env, text=True, capture_output=True)
    if p.returncode: raise RuntimeError(p.stderr)
    return p.stdout.strip()
process = None
created = False
stop = qa / 'stop-services'
stop.unlink(missing_ok=True)
try:
    sql('postgres', 'CREATE DATABASE ' + database)
    created = True
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    jar = work / 'api.jar'
    shutil.copy2(root / 'backend/target/personas-api-1.0.0.jar', jar)
    env.update(DB_URL=f'jdbc:postgresql://127.0.0.1:55440/{database}', PORT=str(port), PHOTO_DIRECTORY=str(work / 'photos'))
    with (work / 'api.log').open('wb') as log:
        process = subprocess.Popen([str(java), '-Dapp.instance=emulator-' + token, '-jar', str(jar)], cwd=work, env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
    for attempt in range(80):
        if process.poll() is not None: raise RuntimeError('API stopped; inspect ' + str(work / 'api.log'))
        try:
            with urllib.request.urlopen(f'http://127.0.0.1:{port}/api/health', timeout=1) as response:
                if response.status == 200: break
        except OSError: pass
        time.sleep(.5)
    else: raise RuntimeError('API startup timed out')
    metadata = dict(token=token, database=database, port=port, url=f'http://10.0.2.2:{port}', apiPid=process.pid, work=str(work))
    (qa / 'services.json').write_text(json.dumps(metadata, indent=2), encoding='utf-8')
    print(json.dumps(metadata), flush=True)
    while not stop.exists():
        if process.poll() is not None: raise RuntimeError('The emulator test API exited unexpectedly')
        time.sleep(.5)
    print(json.dumps({'remainingPeople': sql(database, 'SELECT count(*) FROM personas.persona'), 'activePeople': sql(database, "SELECT count(*) FROM personas.persona WHERE estado='ACTIVO'")}), flush=True)
finally:
    if process and process.poll() is None:
        process.terminate()
        process.wait(timeout=20)
    if created: sql('postgres', 'DROP DATABASE ' + database + ' WITH (FORCE)')
    stop.unlink(missing_ok=True)
    print('Isolated emulator services stopped; application data unchanged.', flush=True)
