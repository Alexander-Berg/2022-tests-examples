import pytest

from infra.walle.server.tests.scenario.utils import MockedQloudClient
from infra.walle.server.tests.lib.util import monkeypatch_function
from walle.clients import qloud
from walle.scenario.host_groups_builders import by_bot_project_id


@pytest.fixture
def qloud_client(mp):
    qloud_client = MockedQloudClient()
    monkeypatch_function(mp, qloud.get_client, module=qloud, return_value=qloud_client)
    return qloud_client


@pytest.fixture
def mock_get_abc_project_slug_from_bot_project_id(monkeypatch):
    def _mocked_get_abc_project_slug_from_bot_project_id(_bot_project_id: int) -> str:
        return str(_bot_project_id)

    monkeypatch.setattr(
        by_bot_project_id, "_get_abc_project_slug_from_bot_project_id", _mocked_get_abc_project_slug_from_bot_project_id
    )
