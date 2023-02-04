from unittest.mock import call, Mock

from dns.resolver import NXDOMAIN

from infra.walle.server.tests.lib.util import monkeypatch_function, timestamp
from infra.walle.server.tests.scenario.utils import get_scenario_params
from sepelib.yandex.dns_api import DnsApiError
from walle.clients import startrek
from walle.clients.dns import slayer_dns_api
from walle.hosts import HostState
from walle.scenario.marker import MarkerStatus
from walle.scenario.scenario import Scenario, ScenarioHostState, StageInfo
from walle.scenario.stage.ensure_dns_access_stage import EnsureDnsAccessStage

MOCK_TICKET = "TEST-1"
MOCK_INV = 1
MOCK_ZONE = "mock_zone"
MOCK_ACL_LIST = [slayer_dns_api.DnsAclKey("mock", slayer_dns_api.DnsAclKeyType.USER)]
MOCK_OWNER = "mock"
BAD_OWNER = "bad-mock"
MOCK_COMMENT_ID = "a1"
MOCK_EXISTING_HOSTNAMES = []
TEXT_NO_PERMISSIONS_TO_MODIFY = """**Пожалуйста, вручную удалите из DNS A и AAAA-записи некоторых хостов сценария.**
Сервис для создания заявок на редактирование DNS-зон: https://dns.tt.yandex-team.ru , руководство: https://wiki.yandex-team.ru/traffic/dnsmgr/
<{Список FQDN для ввода в форму "FQDN templates" на сайте https://dns.tt.yandex-team.ru/ :
%%
mocked-1.mock
%%
}>

Зоны, записи в которых Wall-e не имеет прав изменить:

Зона: mock_zone
Ответственные: @mock
<{Хосты:
%%
mocked-1.mock
%%
}>



<{English version of this text
**Please manually remove A and AAAA DNS-records of several scenario hosts.**
Service to create changes requests in DNS zones: https://dns.tt.yandex-team.ru , manual: https://wiki.yandex-team.ru/traffic/dnsmgr/
<{List of FQDNs to put into "FQDN templates" on https://dns.tt.yandex-team.ru/ :
%%
mocked-1.mock
%%
}>

Zones which Wall-e has no permissions to modify:

Zone: mock_zone
Responsibles: @mock
<{Hosts:
%%
mocked-1.mock
%%
}>



}>
"""
TEXT_ZONE_WITH_DNS_API_ERROR = """**Пожалуйста, вручную удалите из DNS A и AAAA-записи некоторых хостов сценария.**
Сервис для создания заявок на редактирование DNS-зон: https://dns.tt.yandex-team.ru , руководство: https://wiki.yandex-team.ru/traffic/dnsmgr/
<{Список FQDN для ввода в форму "FQDN templates" на сайте https://dns.tt.yandex-team.ru/ :
%%
mocked-1.mock
%%
}>


Зоны, для которых DNS API выдает ошибку:

Зона: mock_zone
<{Ошибка:
DNS communication error: Server returned 400 Bad Request: zone:'mock_zone' could not be detected as controlled by api
}>
<{Хосты:
%%
mocked-1.mock
%%
}>


<{English version of this text
**Please manually remove A and AAAA DNS-records of several scenario hosts.**
Service to create changes requests in DNS zones: https://dns.tt.yandex-team.ru , manual: https://wiki.yandex-team.ru/traffic/dnsmgr/
<{List of FQDNs to put into "FQDN templates" on https://dns.tt.yandex-team.ru/ :
%%
mocked-1.mock
%%
}>


Zones, which result in DNS API error:

Zone: mock_zone
<{Error:
DNS communication error: Server returned 400 Bad Request: zone:'mock_zone' could not be detected as controlled by api
}>
<{Hosts:
%%
mocked-1.mock
%%
}>


}>
"""


def get_mocked_startrek_client():
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(return_value={"self": "url"}), "add_comment")
    return mock_startrek_client


def mocked_dns_api_response_zone_not_managed():
    class MockResponse:
        def __init__(self, json_data, status_code):
            self.json_data = json_data
            self.status_code = status_code
            self.reason = "Bad Request"

        def json(self):
            return self.json_data

    mocked_response_data = {
        "success": False,
        "errors": [
            {
                "code": 400,
                "message": "zone:'%s' could not be detected as controlled by api" % MOCK_ZONE,
                "error": "Bad Request",
            },
        ],
    }

    return MockResponse(mocked_response_data, 400)


def monkeypatch_dns_client(monkeypatch, is_zone_owner=True, hostnames_removed=False, is_zone_managed_by_dns_api=True):
    dns_api_client = Mock()
    dns_api_client._local_ns_client = Mock()
    dns_api_client._local_ns_client.attach_mock(Mock(return_value=MOCK_ZONE), "get_zone_for_name")

    dns_api_client._api_client = Mock()
    dns_api_client._api_client.login = MOCK_OWNER

    # WALLE-3927
    if is_zone_managed_by_dns_api:
        dns_api_client.attach_mock(Mock(return_value=MOCK_ACL_LIST), "get_zone_owners")
    else:
        dns_api_response = mocked_dns_api_response_zone_not_managed()
        exc = slayer_dns_api.DnsCommunicationError(
            exc=DnsApiError(
                "Server returned {} {}".format(dns_api_response.status_code, dns_api_response.reason),
                response=dns_api_response,
            )
        )
        dns_api_client.attach_mock(Mock(side_effect=exc), "get_zone_owners")

    dns_api_client.attach_mock(Mock(return_value=is_zone_owner), "is_zone_owner")

    if hostnames_removed:
        dns_api_client.attach_mock(Mock(return_value=None), "get_a")
        dns_api_client.attach_mock(Mock(return_value=None), "get_aaaa")
    else:
        dns_api_client.attach_mock(Mock(return_value="mock"), "get_a")
        dns_api_client.attach_mock(Mock(return_value="mock"), "get_aaaa")

    monkeypatch.setattr(slayer_dns_api, "DnsClient", lambda *args, **kwargs: dns_api_client)
    return dns_api_client


def monkeypatch_startrek(mp):
    mock_startrek_client = get_mocked_startrek_client()
    mock_startrek_client.attach_mock(Mock(return_value={"id": MOCK_COMMENT_ID}), "add_comment")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
    return mock_startrek_client


def create_scenario(walle_test, host_state=HostState.ASSIGNED):
    scenario_params = get_scenario_params()
    host = walle_test.mock_host({"inv": MOCK_INV, "state": host_state})
    scenario = Scenario(
        hosts={str(host.uuid): ScenarioHostState(inv=host.inv, status="queue", timestamp=timestamp())},
        ticket_key=MOCK_TICKET,
        **scenario_params
    )
    return scenario


def test_dns_zone_in_control(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    monkeypatch_dns_client(mp)
    scenario = create_scenario(walle_test)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert st_client.add_comment.mock_calls == []


def test_host_is_free(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    monkeypatch_dns_client(mp, is_zone_owner=False, hostnames_removed=False)
    scenario = create_scenario(walle_test, HostState.FREE)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert st_client.add_comment.mock_calls == []


def test_dns_zone_returns_dns_api_error(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    monkeypatch_dns_client(mp, is_zone_managed_by_dns_api=False)
    scenario = create_scenario(walle_test)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    assert stage.run(stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    assert stage_info.data == {stage.COMMENT_ID_FIELD: MOCK_COMMENT_ID}
    assert st_client.add_comment.mock_calls == [call(issue_id="TEST-1", text=TEXT_ZONE_WITH_DNS_API_ERROR)]


def test_dns_zone_is_not_in_control(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    monkeypatch_dns_client(mp, is_zone_owner=False, hostnames_removed=False)
    scenario = create_scenario(walle_test)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    assert stage.run(stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    assert stage_info.data == {stage.COMMENT_ID_FIELD: MOCK_COMMENT_ID}
    assert st_client.add_comment.mock_calls == [call(issue_id="TEST-1", text=TEXT_NO_PERMISSIONS_TO_MODIFY)]


def test_dns_zone_not_in_control_comment_placed(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    monkeypatch_dns_client(mp, is_zone_owner=False, hostnames_removed=False)
    scenario = create_scenario(walle_test)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    stage_info.set_data(stage.COMMENT_ID_FIELD, MOCK_COMMENT_ID)
    assert stage.run(stage_info, scenario).status == MarkerStatus.IN_PROGRESS
    assert st_client.add_comment.mock_calls == []


def test_all_dns_zone_is_not_in_control_hostnames_removed(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    monkeypatch_dns_client(mp, is_zone_owner=False, hostnames_removed=True)
    scenario = create_scenario(walle_test)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert st_client.add_comment.mock_calls == []


def test_get_zone_raize_hostnames_removed(mp, walle_test):
    st_client = monkeypatch_startrek(mp)
    dns_api_client = monkeypatch_dns_client(mp, is_zone_owner=False, hostnames_removed=True)
    dns_api_client.attach_mock(Mock(side_effect=NXDOMAIN), "get_zone_for_name")
    scenario = create_scenario(walle_test)
    stage = EnsureDnsAccessStage()
    stage_info = StageInfo()
    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert st_client.add_comment.mock_calls == []
