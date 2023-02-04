import pytest
import datetime

from balance.constants import *
from tests import object_builder as ob

NOW = datetime.datetime.now()
HOUR_AFTER = NOW + datetime.timedelta(hours=1)


@pytest.fixture
def client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


def create_client_service_data(client, service_id=ServiceId.DIRECT, **kwargs):
    client_service_data = ob.create_client_service_data(**kwargs)
    client.service_data[service_id] = client_service_data
    return client_service_data


def test_client_service_data_currency_on(client):
    client_service_data = create_client_service_data(client=client)
    assert client_service_data.currency_on() == 'RUB'
    assert client.currency_on() == 'RUB'


def test_client_service_data_currency_on_no_migrate_to_currency_dt(client):
    client_service_data = create_client_service_data(client=client, migrate_to_currency_dt=None)
    assert client_service_data.currency_on() is None
    assert client.currency_on() is None


def test_client_service_data_currency_on_no_migrate_to_currency_dt_in_future(client):
    client_service_data = create_client_service_data(client=client,
                                                     migrate_to_currency_dt=NOW + datetime.timedelta(hours=1))
    assert client_service_data.currency_on() is None
    assert client.currency_on() is None


def test_client_service_data_currency_on_dt(client):
    client_service_data = create_client_service_data(client=client,
                                                     migrate_to_currency_dt=NOW + datetime.timedelta(hours=1))
    assert client_service_data.currency_on(dt=NOW + datetime.timedelta(hours=2)) == 'RUB'
    assert client.currency_on(dt=NOW + datetime.timedelta(hours=2)) == 'RUB'


def test_client_currency_on_fish_client(client):
    assert client.currency_on() is None


def test_client_currency_on_direct_by_default(client):
    create_client_service_data(client=client, service_id=ServiceId.MARKET, migrate_to_currency_dt=NOW)
    assert client.currency_on() is None


def test_client_currency_on_by_service(client):
    create_client_service_data(client=client, service_id=ServiceId.MARKET, migrate_to_currency_dt=NOW)
    assert client.currency_on(service_id=ServiceId.MARKET) == 'RUB'
