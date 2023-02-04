from unittest.mock import create_autospec

import pytest

from maps.pylibs.infrastructure_api.awacs import awacs_api
from maps.pylibs.infrastructure_api.nanny import yp_lite
from mock_awacs import get_mock_awacs, initialize


@pytest.fixture(scope='function', autouse=True)
def awacs(monkeypatch):
    monkeypatch.setattr(awacs_api, 'AwacsApi', get_mock_awacs)
    return initialize(
        fqdns=['core-teapot.maps.yandex.net', 'core-teacup.maps.yandex.net'],
        locations={'core-teapot.maps.yandex.net': ['man', 'vla', 'sas'],
                   'core-teacup.maps.yandex.net': ['man', 'vla', 'sas']},
        pause=['core-teapot.maps.yandex.netvla']
    )


@pytest.fixture(scope='function', autouse=True)
def yp(monkeypatch):
    mock = create_autospec(yp_lite.YpLiteManager)
    monkeypatch.setattr(yp_lite, 'YpLiteManager', mock)
