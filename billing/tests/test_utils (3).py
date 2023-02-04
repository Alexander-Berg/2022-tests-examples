from pathlib import Path

import pytest

from itsman.utils import configure_logging, get_token


def test_configure_logging():
    configure_logging()


def test_get_token(monkeypatch, tmpdir):
    monkeypatch.setattr('itsman.utils.PATH_TOKEN', Path('/tmp/aaa/bbb/token'))

    with pytest.raises(ValueError):
        get_token()

    token_path = tmpdir / 'itsman_token'
    token_path.write('www')
    monkeypatch.setattr('itsman.utils.PATH_TOKEN', token_path)

    assert get_token() == 'www'
