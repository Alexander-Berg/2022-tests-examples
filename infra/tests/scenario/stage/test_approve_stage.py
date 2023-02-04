from unittest.mock import call, Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_function, monkeypatch_method
from sepelib.core.constants import WEEK_SECONDS, HOUR_SECONDS
from walle.clients import abc, bot, startrek, ok
from walle.models import monkeypatch_timestamp
from walle.scenario.constants import TicketStatus, TemplatePath
from walle.scenario.errors import HostDoesNotHaveBotProjectIDError
from walle.scenario.marker import Marker
from walle.scenario.scenario import Scenario, ScenarioHostState
from walle.scenario.stage import approve_stage
from walle.scenario.stage.approve_stage import (
    ApproveStage,
    _get_owners,
    ApproveInfo,
    HostInfo,
    HostDiff,
    APPROVAL_ISSUE_TYPE,
    PRODUCT_HEAD,
    HARDWARE_RESOURCES_OWNER,
    _UNKNOWN,
    ASSIGNEE,
)
from walle.scenario.stage_info import StageInfo
from walle.util.template_loader import JinjaTemplateRenderer

PLANNER_ID = 500
BOT_PROJECT_ID = 1000
MOCK_TICKET_KEY = "TICKET_KEY"
MOCK_TICKET_SUMMARY = "ticket_key: {}, responsibles: {}"
MOCK_TICKET_QUEUE = "mock queue"
MOCK_TICKET_TAGS = ["mock-tag-1", "mock-tag-2"]
MOCK_DEFAULT_APPROVERS = ["biba", "boba"]


product_head = {
    "person": {"login": "login1", "is_robot": False},
    "role": {
        "scope": {"slug": "doesnt_matter"},
        "code": abc.Role.PRODUCT_HEAD,
    },
}
person_hardware_resources_owner = {
    "person": {"login": "login2", "is_robot": False},
    "role": {
        "scope": {"slug": "doesnt_matter"},
        "code": abc.Role.HARDWARE_RESOURCES_OWNER,
    },
}
robot_hardware_resources_owner = {
    "person": {"login": "login2", "is_robot": True},
    "role": {
        "scope": {"slug": "doesnt_matter"},
        "code": abc.Role.HARDWARE_RESOURCES_OWNER,
    },
}


def get_mocked_startrek_client(ticket_status=TicketStatus.CLOSED):
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(return_value={"status": {"key": ticket_status}}), "get_issue")
    mock_startrek_client.attach_mock(Mock(), "close_issue")
    mock_startrek_client.attach_mock(Mock(return_value={"key": "TEST-1"}), "create_issue")
    mock_startrek_client.attach_mock(Mock(return_value={"self": "url"}), "add_comment")
    return mock_startrek_client


def get_mocked_ok_client(approvement_status=ok.ApprovementStatus.CLOSED, is_approved=False):
    mock_ok_client = Mock()
    mock_ok_client.attach_mock(
        Mock(return_value=ok.Approvement(None, None, None, status=approvement_status, is_approved=is_approved)),
        "get_approvement",
    )
    mock_ok_client.attach_mock(
        Mock(return_value=ok.Approvement(None, None, None, status=approvement_status, is_approved=is_approved, uuid=1)),
        "create_approvement",
    )
    mock_ok_client.attach_mock(Mock(), "close_approvement")
    return mock_ok_client


@pytest.mark.parametrize("dataclass", [ApproveInfo, HostInfo])
def test_approve_info_fields_return_not_true_value_by_default(dataclass):
    instance = dataclass()
    for value in instance.__dict__.values():
        assert not value


@pytest.mark.usefixtures("disable_caches")
@pytest.mark.parametrize(
    ["members", "result_members", "result_role_types"],
    [
        [[person_hardware_resources_owner, product_head], [person_hardware_resources_owner], HARDWARE_RESOURCES_OWNER],
        [[product_head], [product_head], PRODUCT_HEAD],
        [
            [person_hardware_resources_owner, robot_hardware_resources_owner],
            [person_hardware_resources_owner],
            HARDWARE_RESOURCES_OWNER,
        ],
    ],
)
def test_get_owners(mp, walle_test, members, result_members, result_role_types):
    monkeypatch_function(mp, abc.get_service_members, module=abc, return_value=members)
    _, members, role_type = _get_owners(service_id=len(members))
    assert members == result_members and role_type == result_role_types


@pytest.mark.parametrize(
    ["parent_ticket_key", "responsibles", "result"],
    [
        [
            "TEST-1",
            "1",
            dict(
                queue=MOCK_TICKET_QUEUE,
                summary=MOCK_TICKET_SUMMARY.format("TEST-1", "1"),
                parent="TEST-1",
                tags=MOCK_TICKET_TAGS,
                type=APPROVAL_ISSUE_TYPE,
            ),
        ],
        [
            "TEST-2",
            ["1"],
            dict(
                queue=MOCK_TICKET_QUEUE,
                summary=MOCK_TICKET_SUMMARY.format("TEST-2", "1"),
                parent="TEST-2",
                tags=MOCK_TICKET_TAGS,
                type=APPROVAL_ISSUE_TYPE,
            ),
        ],
        [
            "TEST-3",
            ["1", "2"],
            dict(
                queue=MOCK_TICKET_QUEUE,
                summary=MOCK_TICKET_SUMMARY.format("TEST-3", "1, 2"),
                parent="TEST-3",
                tags=MOCK_TICKET_TAGS,
                type=APPROVAL_ISSUE_TYPE,
            ),
        ],
    ],
)
def test_prepare_data_for_ticket_creation(mp, parent_ticket_key, responsibles, result):
    mp.config("scenario.rtc_approve_stage.queue", MOCK_TICKET_QUEUE)
    mp.config("scenario.rtc_approve_stage.summary", MOCK_TICKET_SUMMARY)
    mp.config("scenario.rtc_approve_stage.tags", MOCK_TICKET_TAGS)
    assert ApproveStage._prepare_data_for_ticket_creation(parent_ticket_key, responsibles) == result


@pytest.mark.parametrize("ticket_key", ["TEST-1", "TEST-2"])
@pytest.mark.parametrize(
    ["responsible", "result_responsible"], [["1", ["1"]], [["1"], ["1"]], [["1", "2"], ["1", "2"]]]
)
@pytest.mark.parametrize(
    ["hosts", "result_message"],
    [
        [[10], JinjaTemplateRenderer().render_template(TemplatePath.OK_APPROVE_MESSAGE, invs=[10])],
        [[10, 20], JinjaTemplateRenderer().render_template(TemplatePath.OK_APPROVE_MESSAGE, invs=[10, 20])],
    ],
)
def test_prepare_data_for_approvement_creation(ticket_key, responsible, result_responsible, hosts, result_message):
    result = dict(approvers=result_responsible, ticket_key=ticket_key, text=result_message)
    assert ApproveStage._prepare_data_for_approvement_creation(ticket_key, responsible, hosts) == result


@pytest.mark.parametrize(
    "values",
    [
        [ApproveInfo(responsibles=1)],
        [ApproveInfo(responsibles=1), ApproveInfo(responsibles=2)],
    ],
)
def test_set_approve_data(values):
    stage_info = StageInfo()
    ApproveStage._set_approve_data(stage_info, values)
    assert stage_info.data == {ApproveStage.INITIAL_APPROVE_INFOS: [value._to_dict() for value in values]}


@pytest.mark.parametrize("values", [[ApproveInfo()], [ApproveInfo(responsibles=[1]), ApproveInfo(invs=[1, 2, 3])]])
def test_get_approve_data(values):
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: [value._to_dict() for value in values]})
    assert ApproveStage._get_approve_data(stage_info) == values


@pytest.mark.parametrize("values", [[HostInfo()], [HostInfo(host_inv=1), HostInfo(host_inv=2)]])
def test_get_host_info(values):
    stage_info = StageInfo(data={ApproveStage.RESOLVED_HOSTS: [value._to_dict() for value in values]})
    assert ApproveStage._get_host_info(stage_info) == values


@pytest.mark.parametrize("is_local_closure", [True, False])
def test_close_ticket_if_it_is_closed(is_local_closure):
    scenario = Scenario()
    mock_startrek_client = get_mocked_startrek_client()
    approve_info = ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.CLOSED)

    ApproveStage()._close_ticket(scenario, mock_startrek_client, approve_info, is_local_closure)

    assert approve_info.ticket_status == TicketStatus.CLOSED
    mock_startrek_client.get_issue.assert_has_calls([])
    mock_startrek_client.close_issue.assert_has_calls([])


def test_close_ticket_if_it_is_local_closure():
    scenario = Scenario()
    mock_startrek_client = get_mocked_startrek_client()
    approve_info = ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)

    ApproveStage()._close_ticket(scenario, mock_startrek_client, approve_info, is_local_closure=True)

    assert approve_info.ticket_status == TicketStatus.CLOSED
    mock_startrek_client.get_issue.assert_has_calls([])
    mock_startrek_client.close_issue.assert_has_calls([])


def test_close_ticket_if_ticket_already_closed_not_by_stage():
    scenario = Scenario()
    mock_startrek_client = get_mocked_startrek_client()
    approve_info = ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)

    ApproveStage()._close_ticket(scenario, mock_startrek_client, approve_info, is_local_closure=False)

    assert approve_info.ticket_status == TicketStatus.CLOSED
    mock_startrek_client.get_issue.assert_called_with(approve_info.ticket_key)
    mock_startrek_client.close_issue.assert_has_calls([])


def test_close_ticket_if_ticket_open():
    scenario = Scenario()
    mock_startrek_client = get_mocked_startrek_client(ticket_status=TicketStatus.OPEN)
    approve_info = ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)

    ApproveStage()._close_ticket(scenario, mock_startrek_client, approve_info, is_local_closure=False)

    assert approve_info.ticket_status == TicketStatus.CLOSED
    mock_startrek_client.get_issue.assert_called_with(approve_info.ticket_key)
    mock_startrek_client.close_issue.assert_called_with(
        approve_info.ticket_key, transition="closed", resolution="successful"
    )


def test_close_ticket_with_exc_from_startrek():
    scenario = Scenario()
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(side_effect=startrek.StartrekClientError("error")), "get_issue")
    approve_info = ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)

    ApproveStage()._close_ticket(scenario, mock_startrek_client, approve_info, is_local_closure=False)

    assert approve_info.ticket_status == TicketStatus.OPEN
    mock_startrek_client.get_issue.assert_called_with(approve_info.ticket_key)
    mock_startrek_client.close_issue.assert_has_calls([])


def test_close_approvement_if_it_is_closed_in_approve_info():
    mock_ok_client = get_mocked_ok_client()
    scenario = Scenario()
    approve_info = ApproveInfo(approvement_status=ok.ApprovementStatus.CLOSED)

    ApproveStage()._close_approvement(scenario, mock_ok_client, approve_info)

    assert approve_info.approvement_status == ok.ApprovementStatus.CLOSED
    mock_ok_client.get_approvement.assert_has_calls([])
    mock_ok_client.close_approvement.assert_has_calls([])


def test_close_approvement_if_it_is_closed_in_ok():
    mock_ok_client = get_mocked_ok_client()
    scenario = Scenario()
    approve_info = ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid="1")

    ApproveStage()._close_approvement(scenario, mock_ok_client, approve_info)

    assert approve_info.approvement_status == ok.ApprovementStatus.CLOSED
    mock_ok_client.get_approvement.assert_called_with(approve_info.approvement_uuid)
    mock_ok_client.close_approvement.assert_has_calls([])


def test_close_approvement_if_it_is_not_closed_in_ok():
    mock_ok_client = get_mocked_ok_client(approvement_status=ok.ApprovementStatus.IN_PROGRESS)
    scenario = Scenario()
    approve_info = ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid="1")

    ApproveStage()._close_approvement(scenario, mock_ok_client, approve_info)

    assert approve_info.approvement_status == ok.ApprovementStatus.CLOSED
    mock_ok_client.get_approvement.assert_called_with(approve_info.approvement_uuid)
    mock_ok_client.close_approvement.assert_called_with(approve_info.approvement_uuid)


def test_close_approvement_with_exc_from_ok():
    mock_ok_client = Mock()
    mock_ok_client.attach_mock(Mock(side_effect=ok.OKError), "get_approvement")
    scenario = Scenario()
    approve_info = ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid="1")

    ApproveStage()._close_approvement(scenario, mock_ok_client, approve_info)

    assert approve_info.approvement_status == ok.ApprovementStatus.IN_PROGRESS
    mock_ok_client.get_approvement.assert_called_with(approve_info.approvement_uuid)
    mock_ok_client.close_approvement.assert_has_calls([])


@pytest.mark.parametrize(
    ["owners", "result"],
    [[[{"person": {"login": "a"}}], ["a"]], [[{"person": {"login": "a"}}, {"person": {"login": "b"}}], ["a", "b"]]],
)
def test_collect_logins_from_abc_info(owners, result):
    assert ApproveStage._collect_logins_from_abc_info(owners) == result


def test_get_abc_service_id_and_bot_project_id_successfully(mp):
    mp.function(bot.get_host_info, return_value={"bot_project_id": BOT_PROJECT_ID})
    mp.function(bot.get_oebs_projects, return_value={BOT_PROJECT_ID: {"planner_id": PLANNER_ID}})

    result = ApproveStage._get_abc_service_id_and_bot_project_id(1)

    assert result == (PLANNER_ID, BOT_PROJECT_ID)


def test_get_abc_service_id_and_bot_project_id_without_existing_bot_project_id(mp):
    mp.function(bot.get_host_info, return_value={"bot_project_id": ""})
    mp.function(bot.get_oebs_projects, return_value={BOT_PROJECT_ID: {"planner_id": PLANNER_ID}})

    with pytest.raises(HostDoesNotHaveBotProjectIDError):
        ApproveStage._get_abc_service_id_and_bot_project_id(1)


def test_group_hosts_by_bot_project_ids():
    result = ApproveStage()._group_hosts_by_bot_project_id(
        [
            HostInfo(bot_project_id=1, host_inv=1),
            HostInfo(bot_project_id=1, host_inv=2),
            HostInfo(bot_project_id=2, host_inv=3),
            HostInfo(bot_project_id=2, host_inv=4),
            HostInfo(bot_project_id=3, host_inv=5),
            HostInfo(bot_project_id=1, host_inv=6),
        ]
    )
    assert result == {"1": [1, 2, 6], "2": [3, 4], "3": [5]}


def test_group_hosts_by_responsible():
    result = ApproveStage()._group_hosts_by_responsible(
        [
            HostInfo(host_inv=1, owners=["a"], role_type="A", service_id=1),
            HostInfo(host_inv=2, owners=["a"], role_type="A", service_id=1),
            HostInfo(host_inv=3, owners=["a", "b"], role_type="A", service_id=2),
            HostInfo(host_inv=4, owners=["a", "b"], role_type="B", service_id=2),
            HostInfo(host_inv=5, owners=["b"], role_type="A", service_id=4),
            HostInfo(host_inv=6, owners=["a", "c"], role_type="A", service_id=3),
        ]
    )
    assert result == [
        ApproveInfo(responsibles=["a"], invs=[1, 2], role_types={"A"}, service_id=1),
        ApproveInfo(responsibles=["a", "b"], invs=[3, 4], role_types={"A", "B"}, service_id=2),
        ApproveInfo(responsibles=["a", "c"], invs=[6], role_types={"A"}, service_id=3),
        ApproveInfo(responsibles=["b"], invs=[5], role_types={"A"}, service_id=4),
    ]


@pytest.mark.parametrize(["is_cancel_set_val", "result"], [(False, False), (True, True)])
def test_is_cancel_set(mp, is_cancel_set_val, result):
    stage = ApproveStage()
    stage_info = StageInfo(data={ApproveStage.IS_CANCEL_SET: is_cancel_set_val})

    assert stage._is_cancel_needed(stage_info) == result


def test_is_all_data_self_contained_before_action_done():
    stage_info = StageInfo()

    assert not ApproveStage()._is_all_data_self_contained(stage_info, None)


def test_is_all_data_self_contained_with_old_ticket(mp):
    monkeypatch_method(mp, ApproveStage._get_ticket_author_and_timestamp, obj=ApproveStage, return_value=["wall-e", 0])
    monkeypatch_timestamp(mp, cur_time=WEEK_SECONDS + 1)

    scenario = Scenario(script_args={"responsible": "wall-e"})
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: None})

    assert not ApproveStage()._is_all_data_self_contained(stage_info, scenario)


def test_is_all_data_self_contained_without_ticket_author_is_owner_of_all_hosts(mp):
    monkeypatch_method(mp, ApproveStage._get_ticket_author_and_timestamp, obj=ApproveStage, return_value=["wall-e", 0])
    monkeypatch_timestamp(mp, cur_time=WEEK_SECONDS - 1)

    scenario = Scenario()
    stage_info = StageInfo(
        data={
            ApproveStage.INITIAL_APPROVE_INFOS: [
                ApproveInfo(responsibles="wall-e")._to_dict(),
                ApproveInfo(responsibles="random")._to_dict(),
            ]
        }
    )

    assert not ApproveStage()._is_all_data_self_contained(stage_info, scenario)


def test_is_all_data_self_contained_with_ticket_author_is_owner_of_all_hosts(mp):
    monkeypatch_method(mp, ApproveStage._get_ticket_author_and_timestamp, obj=ApproveStage, return_value=["wall-e", 0])
    monkeypatch_timestamp(mp, cur_time=WEEK_SECONDS - 1)

    scenario = Scenario()
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: [ApproveInfo(responsibles="wall-e")._to_dict()]})

    assert ApproveStage()._is_all_data_self_contained(stage_info, scenario)


@pytest.mark.parametrize(
    ["current_timestamp", "result"], [(HOUR_SECONDS - 1, False), (HOUR_SECONDS, False), (HOUR_SECONDS + 1, True)]
)
def test_is_time_for_check_owners(mp, current_timestamp, result):
    monkeypatch_timestamp(mp, cur_time=current_timestamp)
    stage_info = StageInfo(data={ApproveStage.LAST_OWNERS_GATHERING_TIMESTAMP: 0})

    assert ApproveStage()._is_time_for_check_owners(stage_info) == result


@pytest.mark.parametrize(
    ["initial_ai", "check_ai", "initial_bot_ids", "check_bot_ids", "result"],
    [
        [
            [ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()],
            [ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()],
            {1: [1, 2]},
            {1: [1, 2]},
            True,
        ],
        [
            [ApproveInfo(invs=[1, 3], responsibles=(1, 2))._to_dict()],
            [ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()],
            {1: [1, 2]},
            {1: [1, 2]},
            False,
        ],
        [
            [
                ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict(),
                ApproveInfo(invs=[3], responsibles=(3,))._to_dict(),
            ],
            [
                ApproveInfo(invs=[3], responsibles=(3,))._to_dict(),
                ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict(),
            ],
            {1: [1, 2]},
            {1: [1, 2]},
            False,
        ],
        [
            [ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()],
            [ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()],
            {1: [1, 2]},
            {1: [1, 3]},
            False,
        ],
        [[ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()], [], {1: [1, 2]}, {1: [1, 3]}, False],
        [[], [ApproveInfo(invs=[1, 2], responsibles=(1, 2))._to_dict()], {1: [1, 2]}, {1: [1, 3]}, False],
    ],
)
def test_is_owners_and_ids_the_same(initial_ai, check_ai, initial_bot_ids, check_bot_ids, result):
    stage_info = StageInfo(
        data={
            ApproveStage.INITIAL_APPROVE_INFOS: initial_ai,
            ApproveStage.APPROVE_INFOS_FOR_CHECK: check_ai,
            ApproveStage.BOT_PROJECT_IDS_INITIAL: initial_bot_ids,
            ApproveStage.BOT_PROJECT_IDS_FOR_CHECK: check_bot_ids,
        }
    )

    assert ApproveStage()._is_owners_and_ids_the_same(stage_info) == result


@pytest.mark.parametrize(
    ["items", "result"],
    [
        [(ApproveInfo(),), True],
        [(ApproveStage(), None), False],
        [(None,), False],
        [(ApproveInfo(), ApproveInfo()), True],
    ],
)
def test_do_items_exist(items, result):
    assert ApproveStage._do_items_exist(*items) == result


@pytest.mark.parametrize(
    ["initial", "check", "result"],
    [
        [ApproveInfo(invs=[1, 2], responsibles=(1, 2)), ApproveInfo(invs=[1, 2], responsibles=(1, 2)), True],
        [ApproveInfo(invs=[1], responsibles=(1,)), ApproveInfo(invs=[1], responsibles=(1,)), True],
        [ApproveInfo(invs=[1], responsibles=(1,)), ApproveInfo(invs=[2], responsibles=(1,)), False],
        [ApproveInfo(invs=[1], responsibles=(1,)), ApproveInfo(invs=[1], responsibles=(2,)), False],
    ],
)
def test_do_approve_items_have_same_fields(initial, check, result):
    assert ApproveStage._do_approve_items_have_same_fields(initial, check) == result


def test_is_restart_needed_if_rollback_set():
    stage_info = StageInfo(data={ApproveStage.IS_ROLLBACK_SET: True})

    assert ApproveStage()._is_restart_needed(stage_info)


def test_resolve_host_owner_successfully(mp):
    MOCK_SERVICE_ID = 1
    MOCK_OWNERS = [{"person": {"login": "TEST"}}]
    MOCK_TIMESTAMP = 999
    MOCK_ROLE = "A"
    monkeypatch_function(mp, _get_owners, module=approve_stage, return_value=(MOCK_TIMESTAMP, MOCK_OWNERS, MOCK_ROLE))
    monkeypatch_method(
        mp,
        ApproveStage._get_abc_service_id_and_bot_project_id,
        obj=ApproveStage,
        return_value=(MOCK_SERVICE_ID, BOT_PROJECT_ID),
    )

    result = ApproveStage()._resolve_host_owner(1)
    assert (
        HostInfo(
            service_id=MOCK_SERVICE_ID,
            owners=["TEST"],
            timestamp=MOCK_TIMESTAMP,
            bot_project_id=BOT_PROJECT_ID,
            host_inv=1,
            role_type=MOCK_ROLE,
        )
        == result
    )


def test_resolve_host_owner_for_empty_service(mp):
    MOCK_TIMESTAMP = 999
    MOCK_SERVICE_ID = 1
    monkeypatch_function(mp, _get_owners, module=approve_stage, return_value=(MOCK_TIMESTAMP, [], None))
    monkeypatch_method(
        mp, ApproveStage._get_abc_service_id_and_bot_project_id, obj=ApproveStage, return_value=(1, BOT_PROJECT_ID)
    )

    result = ApproveStage()._resolve_host_owner(1)

    assert (
        HostInfo(
            service_id=MOCK_SERVICE_ID,
            owners=[_UNKNOWN],
            timestamp=MOCK_TIMESTAMP,
            bot_project_id=BOT_PROJECT_ID,
            host_inv=1,
            role_type=_UNKNOWN,
        )
        == result
    )


def test_get_host_owners_successfully(mp):
    monkeypatch_method(mp, ApproveStage._resolve_host_owner, obj=ApproveStage, return_value=1)

    assert ApproveStage()._get_host_owners([1]) == ([1], [])


def test_get_host_owners_with_exception(mp):
    monkeypatch_method(mp, ApproveStage._resolve_host_owner, obj=ApproveStage, side_effect=Exception)

    assert ApproveStage()._get_host_owners([1]) == ([], [1])


def test_collect_owners_of_hosts_with_not_resolved_hosts(mp):
    OWNERS_KEY = "owners_key"
    BOT_KEY = "bot_project_ids_key"
    monkeypatch_method(mp, ApproveStage._get_host_owners, obj=ApproveStage, return_value=([], [1]))
    stage_info = StageInfo()

    result = ApproveStage()._collect_owners_of_hosts(
        stage_info, {1: ScenarioHostState(inv=1)}, OWNERS_KEY, BOT_KEY, MOCK_TICKET_KEY
    )

    assert result == Marker.failure()
    assert stage_info.data[ApproveStage.RESOLVED_HOSTS] == []
    assert stage_info.data[ApproveStage.STAGE_TMP_DATA] == [1]


# @pytest.mark.parametrize(["owners", "role_type", "responsibles", ""])
def test_collect_owners_of_hosts_successfully(mp):
    monkeypatch_function(mp, startrek.get_client, module=startrek)
    OWNERS_KEY = "owners_key"
    BOT_KEY = "bot_project_ids_key"
    monkeypatch_method(
        mp,
        ApproveStage._get_host_owners,
        obj=ApproveStage,
        return_value=([HostInfo(host_inv=1, bot_project_id=2, owners=["b", "a"], role_type="A")], []),
    )
    stage_info = StageInfo()

    result = ApproveStage()._collect_owners_of_hosts(
        stage_info, {1: ScenarioHostState(inv=1)}, OWNERS_KEY, BOT_KEY, MOCK_TICKET_KEY
    )

    assert result == Marker.success()
    assert ApproveStage.STAGE_TMP_DATA not in stage_info.data
    assert ApproveStage.RESOLVED_HOSTS not in stage_info.data
    assert stage_info.data[OWNERS_KEY] == [ApproveInfo(invs=[1], responsibles=["a", "b"], role_types={"A"})._to_dict()]
    assert stage_info.data[BOT_KEY] == {"2": [1]}


def test_collect_owners_of_hosts_from_empty_service(mp):
    monkeypatch_function(mp, startrek.get_client, module=startrek)
    mp.config("scenario.rtc_approve_stage.default_approvers", MOCK_DEFAULT_APPROVERS)
    OWNERS_KEY = "owners_key"
    BOT_KEY = "bot_project_ids_key"
    monkeypatch_method(
        mp,
        ApproveStage._get_host_owners,
        obj=ApproveStage,
        return_value=([HostInfo(host_inv=1, bot_project_id=2, owners=[_UNKNOWN], role_type=_UNKNOWN)], []),
    )
    stage_info = StageInfo()

    result = ApproveStage()._collect_owners_of_hosts(
        stage_info, {1: ScenarioHostState(inv=1)}, OWNERS_KEY, BOT_KEY, MOCK_TICKET_KEY
    )

    assert result == Marker.success()
    assert ApproveStage.STAGE_TMP_DATA not in stage_info.data
    assert ApproveStage.RESOLVED_HOSTS not in stage_info.data
    assert stage_info.data[OWNERS_KEY] == [
        ApproveInfo(
            invs=[1], responsibles=MOCK_DEFAULT_APPROVERS, role_types={ASSIGNEE}, is_owners_unknown=True
        )._to_dict()
    ]
    assert stage_info.data[BOT_KEY] == {"2": [1]}


def test_collect_owners_of_hosts_with_previously_error_hosts(mp):
    monkeypatch_function(mp, startrek.get_client, module=startrek)
    OWNERS_KEY = "owners_key"
    BOT_KEY = "bot_project_ids_key"
    monkeypatch_method(
        mp,
        ApproveStage._get_host_owners,
        obj=ApproveStage,
        return_value=([HostInfo(host_inv=1, bot_project_id=2, owners=["b", "a"], role_type="A")], []),
    )
    stage_info = StageInfo(
        data={
            ApproveStage.STAGE_TMP_DATA: [1],
            ApproveStage.RESOLVED_HOSTS: [
                HostInfo(host_inv=2, bot_project_id=3, owners=["b", "a"], role_type="A")._to_dict()
            ],
        }
    )

    result = ApproveStage()._collect_owners_of_hosts(
        stage_info, {1: ScenarioHostState(inv=1), 2: ScenarioHostState(inv=2)}, OWNERS_KEY, BOT_KEY, MOCK_TICKET_KEY
    )

    assert result == Marker.success()
    assert ApproveStage.STAGE_TMP_DATA not in stage_info.data
    assert ApproveStage.RESOLVED_HOSTS not in stage_info.data
    assert stage_info.data[OWNERS_KEY] == [
        ApproveInfo(invs=[1, 2], responsibles=["a", "b"], role_types={"A"})._to_dict()
    ]
    assert stage_info.data[BOT_KEY] == {"2": [1], "3": [2]}


@pytest.mark.parametrize(
    ["func_name", "is_set_timestamp", "infos_key", "bot_ids_key"],
    [
        (ApproveStage().action, True, ApproveStage.INITIAL_APPROVE_INFOS, ApproveStage.BOT_PROJECT_IDS_INITIAL),
        (
            ApproveStage()._collect_approve_infos_for_check,
            False,
            ApproveStage.APPROVE_INFOS_FOR_CHECK,
            ApproveStage.BOT_PROJECT_IDS_FOR_CHECK,
        ),
    ],
)
def test_action_successfully(mp, func_name, is_set_timestamp, infos_key, bot_ids_key):
    monkeypatch_function(mp, startrek.get_client, module=startrek)
    # it's just the same logic of _collect_owners_of_hosts, so let's check timestamp and that's all
    monkeypatch_method(
        mp,
        ApproveStage._get_host_owners,
        obj=ApproveStage,
        return_value=([HostInfo(host_inv=1, bot_project_id=2, owners=["b", "a"], role_type="A")], []),
    )
    monkeypatch_timestamp(mp, cur_time=0)
    stage_info = StageInfo()
    scenario = Scenario(hosts={1: ScenarioHostState(inv=1)})

    result = func_name(stage_info, scenario)

    assert result == Marker.success()
    assert ApproveStage.STAGE_TMP_DATA not in stage_info.data
    assert ApproveStage.RESOLVED_HOSTS not in stage_info.data
    assert stage_info.data[infos_key] == [ApproveInfo(invs=[1], responsibles=["a", "b"], role_types={"A"})._to_dict()]
    assert stage_info.data[bot_ids_key] == {"2": [1]}

    if is_set_timestamp:
        assert stage_info.data[ApproveStage.LAST_OWNERS_GATHERING_TIMESTAMP] == 0
    else:
        assert ApproveStage.LAST_OWNERS_GATHERING_TIMESTAMP not in stage_info.data


def test_rollback_successfully(mp):
    mock_ok_client = get_mocked_ok_client(approvement_status=ok.ApprovementStatus.IN_PROGRESS)
    mock_startrek_client = get_mocked_startrek_client(ticket_status=TicketStatus.OPEN)
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)
    monkeypatch_method(mp, ApproveStage._add_message_with_diff, obj=ApproveStage)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.IN_PROGRESS,
            approvement_uuid=1,
            ticket_key="TEST-1",
            ticket_status=TicketStatus.OPEN,
        )._to_dict(),
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.IN_PROGRESS,
            approvement_uuid=2,
            ticket_key="TEST-2",
            ticket_status=TicketStatus.OPEN,
        )._to_dict(),
    ]

    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos, ApproveStage.IS_ROLLBACK_SET: True})

    ApproveStage()._rollback(stage_info, scenario)

    assert ApproveStage.IS_ROLLBACK_SET not in stage_info.data
    assert ApproveStage.INITIAL_APPROVE_INFOS not in stage_info.data

    mock_ok_client.get_approvement.assert_has_calls([call(1), call(2)])
    mock_ok_client.close_approvement.assert_has_calls([call(1), call(2)])

    mock_startrek_client.get_issue.assert_has_calls([call("TEST-1"), call("TEST-2")])
    mock_startrek_client.close_issue.assert_has_calls(
        [
            call("TEST-1", transition="closed", resolution="refusal"),
            call("TEST-2", transition="closed", resolution="refusal"),
        ]
    )


def test_create_hosts_diff():
    scenario = Scenario(hosts={1: ScenarioHostState(inv=1)})
    approve_infos_initial = [ApproveInfo(responsibles=["wall-e"], invs=[1])._to_dict()]
    approve_infos_check = [ApproveInfo(responsibles=["eva"], invs=[1])._to_dict()]
    bpid_initial = {11: [1]}
    bpid_check = {22: [1]}
    stage_info = StageInfo(
        data={
            ApproveStage.INITIAL_APPROVE_INFOS: approve_infos_initial,
            ApproveStage.APPROVE_INFOS_FOR_CHECK: approve_infos_check,
            ApproveStage.BOT_PROJECT_IDS_INITIAL: bpid_initial,
            ApproveStage.BOT_PROJECT_IDS_FOR_CHECK: bpid_check,
        }
    )

    result = ApproveStage()._create_hosts_diff(stage_info, scenario)

    valid_diff = HostDiff(inv=1)
    valid_diff._set_vars(initial_responsibles="wall-e", current_responsibles="eva", initial_bpid=11, current_bpid=22)
    assert [valid_diff] == list(result)


def test_rollback_with_failed_ticket_closure(mp):
    mock_ok_client = get_mocked_ok_client(approvement_status=ok.ApprovementStatus.IN_PROGRESS)
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(side_effect=startrek.StartrekClientError("error")), "get_issue")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.IN_PROGRESS,
            approvement_uuid=1,
            ticket_key="TEST-1",
            ticket_status=TicketStatus.OPEN,
        )._to_dict(),
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.IN_PROGRESS,
            approvement_uuid=2,
            ticket_key="TEST-2",
            ticket_status=TicketStatus.OPEN,
        )._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos, ApproveStage.IS_ROLLBACK_SET: True})

    ApproveStage()._rollback(stage_info, scenario)

    assert stage_info.data[ApproveStage.IS_ROLLBACK_SET]
    result_approve_infos = [
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.CLOSED,
            approvement_uuid=1,
            ticket_key="TEST-1",
            ticket_status=TicketStatus.OPEN,
        )._to_dict(),
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.CLOSED,
            approvement_uuid=2,
            ticket_key="TEST-2",
            ticket_status=TicketStatus.OPEN,
        )._to_dict(),
    ]
    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    mock_ok_client.get_approvement.assert_has_calls([call(1), call(2)])
    mock_ok_client.close_approvement.assert_has_calls([call(1), call(2)])

    mock_startrek_client.get_issue.assert_has_calls([call("TEST-1"), call("TEST-2")])
    assert not mock_startrek_client.close_issue.called


def test_close_linked_tickets_successfully(mp):
    mock_startrek_client = get_mocked_startrek_client(ticket_status=TicketStatus.OPEN)
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)._to_dict(),
        ApproveInfo(ticket_key="TEST-2", ticket_status=TicketStatus.OPEN)._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    assert ApproveStage().close_linked_tickets(stage_info, scenario) == Marker.success()

    mock_startrek_client.get_issue.assert_has_calls([call("TEST-1"), call("TEST-2")])
    mock_startrek_client.close_issue.assert_has_calls(
        [
            call("TEST-1", transition="closed", resolution="successful"),
            call("TEST-2", transition="closed", resolution="successful"),
        ]
    )

    result_approve_infos = [
        ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.CLOSED)._to_dict(),
        ApproveInfo(ticket_key="TEST-2", ticket_status=TicketStatus.CLOSED)._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos


def test_close_linked_tickets_with_exc_from_startrek(mp):
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(side_effect=startrek.StartrekError("error")), "get_issue")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)._to_dict(),
        ApproveInfo(ticket_key="TEST-2", ticket_status=TicketStatus.OPEN)._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    assert ApproveStage().close_linked_tickets(stage_info, scenario) == Marker.in_progress()

    mock_startrek_client.get_issue.assert_has_calls([call("TEST-1"), call("TEST-2")])
    assert not mock_startrek_client.close_issue.called

    result_approve_infos = [
        ApproveInfo(ticket_key="TEST-1", ticket_status=TicketStatus.OPEN)._to_dict(),
        ApproveInfo(ticket_key="TEST-2", ticket_status=TicketStatus.OPEN)._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos


def test_check_approvement_resolution_successfully(mp):
    mock_ok_client = get_mocked_ok_client(approvement_status=ok.ApprovementStatus.CLOSED, is_approved=True)
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid=1)._to_dict(),
        ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid=2)._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().check_approvement_resolution(stage_info, scenario)

    assert result == Marker.success()

    result_approve_infos = [
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.CLOSED, approvement_uuid=1, is_approvement_resolved=True
        )._to_dict(),
        ApproveInfo(
            approvement_status=ok.ApprovementStatus.CLOSED, approvement_uuid=2, is_approvement_resolved=True
        )._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    mock_ok_client.get_approvement.assert_has_calls([call(1), call(2)])


@pytest.mark.parametrize("approvement_status", [ok.ApprovementStatus.REJECTED, ok.ApprovementStatus.CLOSED])
def test_check_approvement_resolution_if_it_is_not_accepted(mp, approvement_status):
    mock_ok_client = get_mocked_ok_client(approvement_status=approvement_status, is_approved=False)
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)
    monkeypatch_timestamp(mp, cur_time=0)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid=1)._to_dict(),
        ApproveInfo(approvement_status=ok.ApprovementStatus.IN_PROGRESS, approvement_uuid=2)._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().check_approvement_resolution(stage_info, scenario)

    assert result == Marker.in_progress()

    result_approve_infos = [
        ApproveInfo(approvement_status=approvement_status, approvement_uuid=1)._to_dict(),
        ApproveInfo(approvement_status=approvement_status, approvement_uuid=2)._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos
    assert stage_info.data[ApproveStage.IS_ROLLBACK_SET]
    assert stage_info.data[ApproveStage.IS_CANCEL_SET] is True

    mock_ok_client.get_approvement.assert_has_calls([call(1), call(2)])


def test_check_approvement_resolution_if_approve_infos_already_resolved(mp):
    mock_ok_client = get_mocked_ok_client()
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(is_approvement_resolved=True)._to_dict(),
        ApproveInfo(is_approvement_resolved=True)._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().check_approvement_resolution(stage_info, scenario)

    assert result == Marker.success()

    result_approve_infos = [
        ApproveInfo(is_approvement_resolved=True)._to_dict(),
        ApproveInfo(is_approvement_resolved=True)._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    assert not mock_ok_client.get_approvement.called


def test_add_comments_successfully(mp):
    mock_startrek_client = get_mocked_startrek_client()
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(
            approvement_uuid=1, ticket_key="TEST-1", responsibles=["a", "b"], role_types=["A"], service_id=1, invs=[]
        )._to_dict(),
        ApproveInfo(
            approvement_uuid=2, ticket_key="TEST-2", responsibles=["a", "b"], role_types=["B"], service_id=2, invs=[]
        )._to_dict(),
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().add_comments(stage_info, scenario)

    assert result == Marker.success()

    result_infos = [
        ApproveInfo(
            approvement_uuid=1,
            ticket_key="TEST-1",
            responsibles=["a", "b"],
            comment_url="url",
            role_types=["A"],
            service_id=1,
            invs=[],
        )._to_dict(),
        ApproveInfo(
            approvement_uuid=2,
            ticket_key="TEST-2",
            responsibles=["a", "b"],
            comment_url="url",
            role_types=["B"],
            service_id=2,
            invs=[],
        )._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_infos
    assert mock_startrek_client.add_comment.called


def test_add_comments_for_already_commented_approve_infos(mp):
    monkeypatch_function(mp, startrek.get_client, module=startrek)

    scenario = Scenario()
    approve_infos = [ApproveInfo(comment_url="url")._to_dict(), ApproveInfo(comment_url="url")._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().add_comments(stage_info, scenario)

    assert result == Marker.success()

    result_infos = [ApproveInfo(comment_url="url")._to_dict(), ApproveInfo(comment_url="url")._to_dict()]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_infos


def test_add_comments_with_exc_from_startrek(mp):
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(side_effect=startrek.StartrekClientError("error")), "add_comment")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario()
    approve_infos = [
        ApproveInfo(
            approvement_uuid=1, ticket_key="TEST-1", responsibles=["a", "b"], role_types=["A"], service_id=1, invs=[]
        )._to_dict()
    ]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().add_comments(stage_info, scenario)

    assert result == Marker.in_progress()

    result_infos = [
        ApproveInfo(
            approvement_uuid=1, ticket_key="TEST-1", responsibles=["a", "b"], role_types=["A"], service_id=1, invs=[]
        )._to_dict()
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_infos


def test_create_approvements_successfully(mp):
    mock_ok_client = get_mocked_ok_client(approvement_status=ok.ApprovementStatus.IN_PROGRESS)
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)

    scenario = Scenario()
    approve_infos = [ApproveInfo(ticket_key="TEST-1", responsibles=("a", "b"), invs=["1"], role_types={"A"})._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().create_approvements(stage_info, scenario)

    assert result == Marker.success()

    result_approve_infos = [
        ApproveInfo(
            ticket_key="TEST-1",
            responsibles=["a", "b"],
            invs=["1"],
            role_types=["A"],
            approvement_uuid=1,
            approvement_status=ok.ApprovementStatus.IN_PROGRESS,
        )._to_dict()
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    result_output = JinjaTemplateRenderer().render_template(TemplatePath.OK_APPROVE_MESSAGE, role_types="A", invs="1")
    mock_ok_client.create_approvement.assert_called_once_with(
        text=result_output, ticket_key='TEST-1', approvers=['a', 'b']
    )


def test_create_approvements_with_exc_from_ok(mp):
    mock_ok_client = Mock()
    mock_ok_client.attach_mock(Mock(side_effect=ok.OKError), "create_approvement")
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)

    scenario = Scenario()
    approve_infos = [ApproveInfo(ticket_key="TEST-1", responsibles=("a", "b"), invs=["1"], role_types={"A"})._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().create_approvements(stage_info, scenario)

    assert result == Marker.in_progress()

    result_approve_infos = [
        ApproveInfo(ticket_key="TEST-1", responsibles=["a", "b"], invs=["1"], role_types=["A"])._to_dict()
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    result_output = JinjaTemplateRenderer().render_template(TemplatePath.OK_APPROVE_MESSAGE, role_types="A", invs="1")
    mock_ok_client.create_approvement.assert_called_once_with(
        text=result_output, ticket_key='TEST-1', approvers=['a', 'b']
    )


def test_create_approvements_for_infos_with_approvements(mp):
    mock_ok_client = get_mocked_ok_client(approvement_status=ok.ApprovementStatus.IN_PROGRESS)
    monkeypatch_function(mp, ok.get_client, module=ok, return_value=mock_ok_client)

    scenario = Scenario()
    approve_infos = [ApproveInfo(approvement_uuid=1)._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    result = ApproveStage().create_approvements(stage_info, scenario)

    assert result == Marker.success()

    result_approve_infos = [ApproveInfo(approvement_uuid=1)._to_dict()]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    assert not mock_ok_client.create_approvement.called


def test_create_tickets_for_several_approve_infos_successfully(mp):
    mp.config("scenario.rtc_approve_stage.queue", MOCK_TICKET_QUEUE)
    mp.config("scenario.rtc_approve_stage.summary", MOCK_TICKET_SUMMARY)
    mp.config("scenario.rtc_approve_stage.tags", MOCK_TICKET_TAGS)
    mock_startrek_client = get_mocked_startrek_client()
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario(ticket_key="PARENT")
    approve_infos = [ApproveInfo(responsibles=("a", "b"))._to_dict(), ApproveInfo(responsibles=("c",))._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    assert ApproveStage().create_tickets(stage_info, scenario) == Marker.success()

    result_approve_infos = [
        ApproveInfo(ticket_key="TEST-1", responsibles=["a", "b"])._to_dict(),
        ApproveInfo(ticket_key="TEST-1", responsibles=["c"])._to_dict(),
    ]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    mock_startrek_client.create_issue.assert_has_calls(
        [
            call(
                dict(
                    queue=MOCK_TICKET_QUEUE,
                    summary=MOCK_TICKET_SUMMARY.format(scenario.ticket_key, "a, b"),
                    parent=scenario.ticket_key,
                    tags=MOCK_TICKET_TAGS,
                    type=APPROVAL_ISSUE_TYPE,
                )
            ),
            call(
                dict(
                    queue=MOCK_TICKET_QUEUE,
                    summary=MOCK_TICKET_SUMMARY.format(scenario.ticket_key, "c"),
                    parent=scenario.ticket_key,
                    tags=MOCK_TICKET_TAGS,
                    type=APPROVAL_ISSUE_TYPE,
                )
            ),
        ]
    )


def test_create_tickets_for_several_approve_infos_with_exc(mp):
    mp.config("scenario.rtc_approve_stage.queue", MOCK_TICKET_QUEUE)
    mp.config("scenario.rtc_approve_stage.summary", MOCK_TICKET_SUMMARY)
    mp.config("scenario.rtc_approve_stage.tags", MOCK_TICKET_TAGS)
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(side_effect=startrek.StartrekClientError("error")), "create_issue")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario(ticket_key="PARENT")
    approve_infos = [ApproveInfo(responsibles=("a", "b"))._to_dict(), ApproveInfo(responsibles=("c",))._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    assert ApproveStage().create_tickets(stage_info, scenario) == Marker.in_progress()

    result_approve_infos = [ApproveInfo(responsibles=["a", "b"])._to_dict(), ApproveInfo(responsibles=["c"])._to_dict()]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos


def test_create_tickets_for_several_approve_infos_with_tickets(mp):
    mp.config("scenario.rtc_approve_stage.queue", MOCK_TICKET_QUEUE)
    mp.config("scenario.rtc_approve_stage.summary", MOCK_TICKET_SUMMARY)
    mp.config("scenario.rtc_approve_stage.tags", MOCK_TICKET_TAGS)
    mock_startrek_client = get_mocked_startrek_client()
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario()
    approve_infos = [ApproveInfo(ticket_key="1")._to_dict(), ApproveInfo(ticket_key="2")._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    assert ApproveStage().create_tickets(stage_info, scenario) == Marker.success()

    result_approve_infos = [ApproveInfo(ticket_key="1")._to_dict(), ApproveInfo(ticket_key="2")._to_dict()]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    assert not mock_startrek_client.create_issue.called


def test_create_tickets_for_one_approve_info(mp):
    mp.config("scenario.rtc_approve_stage.queue", MOCK_TICKET_QUEUE)
    mp.config("scenario.rtc_approve_stage.summary", MOCK_TICKET_SUMMARY)
    mp.config("scenario.rtc_approve_stage.tags", MOCK_TICKET_TAGS)
    mock_startrek_client = get_mocked_startrek_client()
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    scenario = Scenario(ticket_key="PARENT")
    approve_infos = [ApproveInfo()._to_dict()]
    stage_info = StageInfo(data={ApproveStage.INITIAL_APPROVE_INFOS: approve_infos})

    assert ApproveStage().create_tickets(stage_info, scenario) == Marker.success()

    result_approve_infos = [ApproveInfo(ticket_key="PARENT")._to_dict()]

    assert stage_info.data[ApproveStage.INITIAL_APPROVE_INFOS] == result_approve_infos

    assert not mock_startrek_client.create_issue.called


def test_check_owners_if_collecting_owners_failed(mp):
    monkeypatch_method(mp, ApproveStage._is_time_for_check_owners, obj=ApproveStage, return_value=True)
    monkeypatch_method(
        mp, ApproveStage._collect_approve_infos_for_check, obj=ApproveStage, return_value=Marker.in_progress()
    )

    stage_info = StageInfo()

    assert not ApproveStage()._check_owners(stage_info, None)
    assert ApproveStage.IS_ROLLBACK_SET not in stage_info.data


def test_check_owners_if_owners_not_the_same(mp):
    monkeypatch_timestamp(mp, cur_time=0)
    monkeypatch_method(mp, ApproveStage._is_time_for_check_owners, obj=ApproveStage, return_value=True)
    monkeypatch_method(
        mp, ApproveStage._collect_approve_infos_for_check, obj=ApproveStage, return_value=Marker.success()
    )
    monkeypatch_method(mp, ApproveStage._is_owners_and_ids_the_same, obj=ApproveStage, return_value=False)

    stage_info = StageInfo()

    assert ApproveStage()._check_owners(stage_info, None)
    assert stage_info.data[ApproveStage.IS_ROLLBACK_SET]
    assert stage_info.data[ApproveStage.LAST_OWNERS_GATHERING_TIMESTAMP] == 0


def test_check_owners_if_owners_the_same(mp):
    monkeypatch_timestamp(mp, cur_time=0)
    monkeypatch_method(mp, ApproveStage._is_time_for_check_owners, obj=ApproveStage, return_value=True)
    monkeypatch_method(
        mp, ApproveStage._collect_approve_infos_for_check, obj=ApproveStage, return_value=Marker.success()
    )
    monkeypatch_method(mp, ApproveStage._is_owners_and_ids_the_same, obj=ApproveStage, return_value=True)

    stage_info = StageInfo()

    assert ApproveStage()._check_owners(stage_info, None)
    assert ApproveStage.IS_ROLLBACK_SET not in stage_info.data
    assert stage_info.data[ApproveStage.LAST_OWNERS_GATHERING_TIMESTAMP] == 0


def test_fill_empty_owners_as_ticket_assignee_for_approve_infos_with_owners(mp):
    mp.config("scenario.rtc_approve_stage.default_approvers", MOCK_DEFAULT_APPROVERS)
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(return_value={"assignee": {"id": "zeke"}}), "get_issue")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    approve_infos = [ApproveInfo(responsibles=i) for i in range(10)]

    ApproveStage()._fill_empty_owners_as_ticket_assignee(approve_infos, MOCK_TICKET_KEY)

    assert approve_infos == [ApproveInfo(responsibles=i) for i in range(10)]


@pytest.mark.parametrize(
    ["responsibles", "get_issue_value"], [(["zeke"], {"assignee": {"id": "zeke"}}), (MOCK_DEFAULT_APPROVERS, {})]
)
def test_fill_empty_owners_as_ticket_assignee_for_approve_infos_without_owners(mp, responsibles, get_issue_value):
    mp.config("scenario.rtc_approve_stage.default_approvers", MOCK_DEFAULT_APPROVERS)
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(return_value=get_issue_value), "get_issue")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)

    approve_infos = [ApproveInfo(responsibles=[_UNKNOWN]) for _ in range(3)]

    ApproveStage()._fill_empty_owners_as_ticket_assignee(approve_infos, MOCK_TICKET_KEY)

    assert approve_infos == [
        ApproveInfo(responsibles=responsibles, role_types={ASSIGNEE}, is_owners_unknown=True) for _ in range(3)
    ]
