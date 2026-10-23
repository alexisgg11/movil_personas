"""Integration checks on a disposable database; never modifies application records."""
import concurrent.futures
import json
import os
from pathlib import Path
import socket
import shutil
import struct
import subprocess
import time
import urllib.error
import urllib.request
import uuid
import zlib

ROOT = Path(__file__).resolve().parents[1]
LOCAL = ROOT / '.local'
TOKEN = uuid.uuid4().hex[:12]
DATABASE = 'personas_verify_' + TOKEN
WORK = LOCAL / ('verify-' + TOKEN)
WORK.mkdir(parents=True)
JAR = WORK / 'api-under-test.jar'
shutil.copy2(ROOT / 'backend/target/personas-api-1.0.0.jar', JAR)
env = os.environ.copy()
for line in (ROOT / 'backend/.env').read_text(encoding='utf-8-sig').splitlines():
    if '=' in line and not line.startswith('#'):
        key, value = line.split('=', 1)
        env[key] = value.strip().strip('"')
env['PGPASSWORD'] = env['DB_PASSWORD']
env['TEMP'] = env['TMP'] = str(LOCAL / 'tmp')
PG = Path(env.get('PERSONAS_PG_BIN', r'C:\Program Files\PostgreSQL\18\bin'))
JAVA = Path(env.get('PERSONAS_JAVA_HOME', r'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot')) / 'bin/java.exe'

def sql(database, text):
    result = subprocess.run([str(PG / 'psql.exe'), '-w', '-h', '127.0.0.1', '-p', '55440', '-U', 'personas', '-d', database, '-v', 'ON_ERROR_STOP=1', '-At', '-c', text], env=env, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError('PostgreSQL verification failed: ' + result.stderr.strip())
    return result.stdout.strip()

def image(red):
    def chunk(name, data):
        return struct.pack('!I', len(data)) + name + data + struct.pack('!I', zlib.crc32(name + data))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 8, 8, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress((b'\x00' + bytes([red, 30, 90]) * 8) * 8)) + chunk(b'IEND', b'')

def request(method, path, data=None, photo=None, multipart=False):
    headers = {}
    body = None
    if multipart:
        boundary = 'test-' + uuid.uuid4().hex
        headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary
        body = ('--' + boundary + '\r\nContent-Disposition: form-data; name="datos"\r\nContent-Type: application/json\r\n\r\n').encode() + json.dumps(data).encode()
        if photo is not None:
            body += ('\r\n--' + boundary + '\r\nContent-Disposition: form-data; name="foto"; filename="photo.png"\r\nContent-Type: image/png\r\n\r\n').encode() + photo
        body += ('\r\n--' + boundary + '--\r\n').encode()
    elif data is not None:
        headers['Content-Type'] = 'application/json'
        body = json.dumps(data).encode()
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.read()

checks = []
def check(label, status, result):
    actual, body = result
    assert actual == status, (label, actual, body.decode(errors='replace'))
    checks.append(label)
    return json.loads(body) if body[:1] in (b'{', b'[') else body

process = None
created = False
try:
    sql('postgres', 'CREATE DATABASE ' + DATABASE)
    created = True
    schema = (ROOT / 'backend/src/main/resources/db/schema.sql').read_text(encoding='utf-8')
    legacy_schema = schema.replace("estado VARCHAR(8) NOT NULL DEFAULT 'ACTIVO',\n    CONSTRAINT ck_persona_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))", 'activo BOOLEAN NOT NULL DEFAULT TRUE')
    assert legacy_schema != schema
    sql(DATABASE, legacy_schema)
    sql(DATABASE, "INSERT INTO personas.profesion(nombre,nombre_clave) VALUES ('Migracion QA','migracion qa'); INSERT INTO personas.persona(dni,nombres,apellidos,direccion,profesion_id,celular,correo,foto_ruta,activo) VALUES ('00111111','Prueba','Activa','Piura',1,'987654321','prueba@example.com','00000000-0000-0000-0000-000000000001.jpg',true),('00111112','Prueba','Inactiva','Piura',1,'987654321','prueba@example.com','00000000-0000-0000-0000-000000000002.jpg',false)")
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    BASE = f'http://127.0.0.1:{port}'
    env.update(DB_URL=f'jdbc:postgresql://127.0.0.1:55440/{DATABASE}', PORT=str(port), PHOTO_DIRECTORY=str(WORK / 'photos'), SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE='4', SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE='1')
    with (WORK / 'api.log').open('wb') as log:
        process = subprocess.Popen([str(JAVA), '-Dapp.instance=verify-' + TOKEN, '-jar', str(JAR)], cwd=ROOT / 'backend', env=env, stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
    for attempt in range(90):
        if process.poll() is not None:
            raise RuntimeError('Test API exited. See ' + str(WORK / 'api.log'))
        try:
            if request('GET', '/api/health')[0] == 200:
                break
        except OSError:
            pass
        time.sleep(.5)
    else:
        raise RuntimeError('Test API did not become ready')
    check('health backed by PostgreSQL', 200, request('GET', '/api/health'))
    assert sql(DATABASE, "SELECT string_agg(estado,',' ORDER BY dni) FROM personas.persona") == 'ACTIVO,INACTIVO'
    migration = (ROOT / 'backend/src/main/resources/db/migrate-estado.sql').read_text(encoding='utf-8')
    sql(DATABASE, migration)
    sql(DATABASE, migration)
    assert sql(DATABASE, "SELECT string_agg(estado,',' ORDER BY dni) FROM personas.persona") == 'ACTIVO,INACTIVO'
    assert sql(DATABASE, "SELECT count(*) FROM information_schema.columns WHERE table_schema='personas' AND table_name='persona' AND column_name='activo'") == '0'
    checks.append('legacy state migration preserves active and inactive records on repeated startup')
    check('migrated inactive record hidden', 404, request('GET', '/api/personas/00111112'))
    catalog = check('profession catalog', 200, request('GET', '/api/profesiones'))
    profession = check('new profession', 201, request('POST', '/api/profesiones', {'nombre': '  Ingeniería   de Prueba  '}))
    check('normalized duplicate profession', 409, request('POST', '/api/profesiones', {'nombre': 'ingenieria de prueba'}))
    person = dict(dni='01234567', nombres='Alexis', apellidos='Sandoval', direccion='Av. Sánchez Cerro 27, Piura', profesionId=profession['id'], celular='987654321', correo='alexis@correo.com')
    check('missing photo', 400, request('POST', '/api/personas', person, multipart=True))
    check('invalid cellphone', 400, request('POST', '/api/personas', person | {'celular': '887654321'}, image(100), True))
    check('unknown profession', 400, request('POST', '/api/personas', person | {'profesionId': 999999}, image(100), True))
    check('invalid image', 400, request('POST', '/api/personas', person, b'not an image', True))
    created_person = check('create with leading zero and photo', 201, request('POST', '/api/personas', person, image(100), True))
    assert created_person['dni'] == '01234567'
    check('duplicate DNI', 409, request('POST', '/api/personas', person, image(200), True))
    found = check('search only by DNI', 200, request('GET', '/api/personas/01234567'))
    original_photo = check('download photo', 200, request('GET', found['fotoUrl']))
    same = check('update without replacing photo', 200, request('PUT', '/api/personas/01234567', person | {'nombres': 'Alexis actualizado'}, multipart=True))
    assert same['fotoUrl'] == found['fotoUrl']
    replaced = check('update replacing photo', 200, request('PUT', '/api/personas/01234567', person, image(220), True))
    assert replaced['fotoUrl'] != same['fotoUrl']
    new_photo = check('download replaced photo', 200, request('GET', replaced['fotoUrl']))
    assert new_photo != original_photo
    photos = WORK / 'photos'
    assert len(list(photos.glob('*.jpg'))) == 1, 'Replaced/failed photographs were not cleaned'
    check('DNI immutable', 400, request('PUT', '/api/personas/01234567', person | {'dni': '01234568'}, multipart=True))
    check('no full person listing', 405, request('GET', '/api/personas'))
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(lambda name: request('POST', '/api/personas', person | {'dni': '02345678', 'nombres': name}, image(150), True), ['Concurrente A', 'Concurrente B']))
    assert sorted(code for code, _ in results) == [201, 409], results
    winner = next(json.loads(body) for code, body in results if code == 201)
    actual = check('concurrent duplicate does not overwrite', 200, request('GET', '/api/personas/02345678'))
    assert winner['nombres'] == actual['nombres']
    assert len(list(photos.glob('*.jpg'))) == 2, 'Rollback left an orphan photograph'
    check('logical delete', 204, request('DELETE', '/api/personas/01234567'))
    check('deleted person hidden', 404, request('GET', '/api/personas/01234567'))
    check('deleted photo hidden', 404, request('GET', '/api/personas/01234567/foto'))
    exists = check('deleted DNI remains reserved', 200, request('GET', '/api/personas/01234567/existe'))
    assert exists['existe'] is True
    check('deleted DNI cannot be reused', 409, request('POST', '/api/personas', person, image(100), True))
    assert sql(DATABASE, "SELECT estado FROM personas.persona WHERE dni='01234567'") == 'INACTIVO'
    assert sql(DATABASE, "SELECT data_type FROM information_schema.columns WHERE table_schema='personas' AND table_name='persona' AND column_name='dni'") == 'character varying'
    assert sql(DATABASE, "SELECT count(*) FROM information_schema.columns WHERE table_schema='personas' AND table_name='persona' AND column_name='id'") == '0'
    for label, statement in [
        ('database rejects invalid state', "UPDATE personas.persona SET estado='BORRADO' WHERE dni='01234567'"),
        ('database rejects cellphone not starting with 9', "UPDATE personas.persona SET celular='887654321' WHERE dni='01234567'"),
        ('database rejects unknown profession FK', "UPDATE personas.persona SET profesion_id=999999 WHERE dni='01234567'"),
        ('database restricts deletion of referenced profession', f"DELETE FROM personas.profesion WHERE id={profession['id']}")
    ]:
        try:
            sql(DATABASE, statement)
        except RuntimeError:
            checks.append(label)
        else:
            raise AssertionError(label)
    report = {'passed': len(checks), 'checks': checks, 'database': 'disposable', 'applicationDataModified': False}
    (LOCAL / 'api-test-results.json').write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report, ensure_ascii=True))
finally:
    if process and process.poll() is None:
        process.terminate()
        process.wait(timeout=20)
    if created:
        sql('postgres', 'DROP DATABASE ' + DATABASE + ' WITH (FORCE)')
