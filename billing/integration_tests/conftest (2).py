import json
import subprocess
from typing import Dict, List

import pytest

from .db_fixtures import *  # noqa


@pytest.fixture(autouse=True)
def yandexpay_base_url():
    with open('yandexpay.port') as f:
        port = int(f.read().strip())

    return f'http://localhost:{port}'


@pytest.fixture(autouse=True)
def interaction_mock_server_url():
    with open('yandexpay-interaction-mock.port') as f:
        port = int(f.read().strip())

    return f'http://localhost:{port}'


@pytest.fixture(autouse=True)
def run_yandexpay_bin():
    # кажется тут я перехожу грань здравого смысла? или нет?...

    with open('yandexpay.run') as f:
        run_ctx = json.loads(f.read().strip())

    env: Dict[str, str] = run_ctx['env']
    base_cmd: List[str] = run_ctx['cmd']

    def run(cmd: List[str]):
        completed_process = subprocess.run(
            base_cmd + cmd,
            env=env,
            capture_output=True,
        )
        if completed_process.returncode != 0:
            raise RuntimeError(completed_process.stderr.decode())
        return completed_process.stdout.decode()

    return run


@pytest.fixture(autouse=True)
def get_csrf_token(run_yandexpay_bin):
    def get(uid, yandexuid, timestamp=None):
        cmd = [
            'issue_csrf_token',
            '--uid', str(uid),
            '--yandexuid', str(yandexuid),
        ]
        if timestamp:
            cmd.extend(['--timestamp', str(timestamp)])

        output = run_yandexpay_bin(cmd)
        data = json.loads(output)
        return data['token']

    return get
