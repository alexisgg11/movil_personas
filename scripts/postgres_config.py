"""Local PostgreSQL settings shared by the integration tools."""
from dataclasses import dataclass
import os
from urllib.parse import urlsplit


@dataclass(frozen=True)
class PostgresConfig:
    host: str
    port: int
    user: str

    def arguments(self, database):
        return ['-w', '-h', self.host, '-p', str(self.port), '-U', self.user, '-d', database]

    def jdbc_url(self, database):
        host = '[' + self.host + ']' if ':' in self.host else self.host
        return f'jdbc:postgresql://{host}:{self.port}/{database}'


def load_postgres(root):
    env = os.environ.copy()
    for line in (root / 'backend/.env').read_text(encoding='utf-8-sig').splitlines():
        key, separator, value = line.strip().partition('=')
        if separator and not key.startswith('#'):
            env[key.strip()] = value.strip().strip('"').strip("'")
    url = env['DB_URL']
    if not url.startswith('jdbc:postgresql://'):
        raise ValueError('DB_URL must be a PostgreSQL JDBC URL')
    parsed = urlsplit(url[5:])
    if not parsed.hostname or parsed.username or parsed.query or parsed.fragment:
        raise ValueError('DB_URL must contain a host and database without credentials or parameters')
    env['PGPASSWORD'] = env['DB_PASSWORD']
    env['PGCLIENTENCODING'] = 'UTF8'
    return env, PostgresConfig(parsed.hostname, parsed.port or 5432, env['DB_USER'])
