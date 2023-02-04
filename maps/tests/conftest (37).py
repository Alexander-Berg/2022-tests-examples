import pytest

from maps_adv.geosmb.doorman.client.lib import DoormanClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("doorman.server", *a)


@pytest.fixture
def mock_create_client(rmock):
    return lambda h: rmock("/v1/create_client/", "POST", h)


@pytest.fixture
def mock_create_clients(rmock):
    return lambda h: rmock("/v1/create_clients/", "POST", h)


@pytest.fixture
def mock_retrieve_clients(rmock):
    return lambda h: rmock("/v1/retrieve_client/", "POST", h)


@pytest.fixture
def mock_list_clients(rmock):
    return lambda h: rmock("/v1/list_clients/", "POST", h)


@pytest.fixture
def mock_list_contacts(rmock):
    return lambda h: rmock("/v1/list_contacts/", "POST", h)


@pytest.fixture
def mock_update_client(rmock):
    return lambda h: rmock("/v1/update_client/", "POST", h)


@pytest.fixture
def mock_add_event(rmock):
    return lambda h: rmock("/v1/add_event/", "POST", h)


@pytest.fixture
def mock_clear_clients_gdpr(rmock):
    return lambda h: rmock("/internal/v1/clear_clients_for_gdpr/", "POST", h)


@pytest.fixture
def mock_search_clients_gdpr(rmock):
    return lambda h: rmock("/internal/v1/search_clients_for_gdpr/", "POST", h)


@pytest.fixture()
async def client(aiotvm):
    client = await DoormanClient(
        url="http://doorman.server", tvm=aiotvm, tvm_destination="doorman"
    )
    yield client
    await client.close()
