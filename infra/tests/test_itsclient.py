# coding: utf-8
import pytest

from awacs.lib.itsclient import ItsClient


@pytest.fixture
def client():
    yield ItsClient(
        url='https://its.yandex-team.ru/',
        token='DUMMY')


@pytest.mark.vcr
def test_its_client_get_config(client):
    cfg = client.get_config()
    assert set(cfg.keys()) == {'max_age', 'locations', 'values_lifetime', 'ruchkas', 'acl', 'mergers'}
