import os

import pytest

from smb.common.multiruntime.lib.basics import is_arcadia_python


@pytest.fixture(autouse=True)
def set_os_env(mocker):
    def _set_env(key, value):
        os.environ[key] = value

    mocker.patch.dict(os.environ, os.environ.copy())
    yield _set_env


@pytest.fixture
def write_dotenv(tmpdir):
    path = str(tmpdir.join(".env"))
    open(path, "a").close()

    _used_path = {path}

    def _write(content: str):
        _used_path.add(_write.path)
        with open(_write.path, "w") as f:
            f.write(content)

    _write.path = path

    yield _write

    for _path in _used_path:
        if os.path.exists(_path):
            os.remove(_path)


@pytest.fixture(autouse=True)
def mock_yav(mocker):
    if is_arcadia_python:
        _path = "library.python.vault_client.client.VaultClient"
    else:
        _path = "vault_client.client.VaultClient"

    mocker.patch(f"{_path}.check_status")
    return mocker.patch(f"{_path}.get_version")
