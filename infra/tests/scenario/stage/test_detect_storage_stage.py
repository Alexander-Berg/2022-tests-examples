from unittest.mock import call, Mock

import pytest

from infra.walle.server.tests.lib.util import monkeypatch_function, timestamp
from infra.walle.server.tests.scenario.utils import mock_scenario
from walle.clients import bot, startrek
from walle.hosts import HostState
from walle.scenario.marker import MarkerStatus
from walle.scenario.scenario import ScenarioHostState, StageInfo
from walle.scenario.stage.detect_storage_stage import DetectStorageStage
from walle.scenario.stages import ScenarioRootStage

MOCK_INVS = [1, 2, 3]
MOCK_STORAGES = dict(
    zip(
        MOCK_INVS,
        [
            [bot.StorageInfo(inv="111", type="type1"), bot.StorageInfo(inv="222", type="type2")],
            [bot.StorageInfo(inv="333", type="type1")],
            [],
        ],
    )
)
MOCK_TICKET = "TEST-1"
MOCK_COMMENT_ID = "a1"
COMMENT_TEXT = """**Обнаружены подключенные внешние дисковые хранилища.**
<{Перечень хостов и хранилищ
#|
|| FQDN | Инвентарный номер | Хранилища (инвентарный номер, тип) ||

||

mocked-1.mock

| 1 |

111 (type1)

222 (type2)

||

||

mocked-2.mock

| 2 |

333 (type1)

||

|#
}>

<{English version of this text
**Connected external disk storages detected.**
<{List of hosts and storages
#|
|| FQDN | Inventory number | Storages (inventory number, type) ||

||

mocked-1.mock

| 1 |

111 (type1)

222 (type2)

||

||

mocked-2.mock

| 2 |

333 (type1)

||

|#
}>
}>"""


def monkeypatch_startrek(mp):
    mock_startrek_client = Mock()
    mock_startrek_client.attach_mock(Mock(return_value={"id": MOCK_COMMENT_ID}), "add_comment")
    monkeypatch_function(mp, startrek.get_client, module=startrek, return_value=mock_startrek_client)
    return mock_startrek_client


def create_script():
    return ScenarioRootStage([DetectStorageStage()])


def create_scenario(walle_test, **params):
    hosts = []
    for inv in MOCK_INVS:
        hosts.append(walle_test.mock_host({"inv": inv, "state": HostState.ASSIGNED}))
    scenario = mock_scenario(
        hosts={
            str(host.uuid): ScenarioHostState(inv=host.inv, status="queue", timestamp=timestamp()) for host in hosts
        },
        ticket_key=MOCK_TICKET,
        **params
    )
    walle_test.scenarios.add(scenario)
    return scenario


def test_empty_data(walle_test, mp):
    bot_mock = monkeypatch_function(mp, bot.get_storages, module=bot, return_value=[])
    st_mock = monkeypatch_startrek(mp)
    scenario = create_scenario(walle_test)
    stage = DetectStorageStage()
    stage_info = StageInfo()

    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert bot_mock.mock_calls == [call(inv) for inv in MOCK_INVS]
    st_mock.add_comment.assert_not_called()


def test_with_existing_storages(walle_test, mp):
    bot_mock = monkeypatch_function(mp, bot.get_storages, module=bot, side_effect=lambda inv: MOCK_STORAGES[inv])
    st_mock = monkeypatch_startrek(mp)
    scenario = create_scenario(walle_test)
    stage = DetectStorageStage()
    stage_info = StageInfo()

    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert bot_mock.mock_calls == [call(inv) for inv in MOCK_INVS]
    st_mock.add_comment.assert_called_once_with(issue_id=MOCK_TICKET, text=COMMENT_TEXT)


def test_with_fail_bot_request(walle_test, mp):
    def mock_storages(inv):
        if inv == MOCK_INVS[1] and mock_storages.call_count == 0:
            mock_storages.call_count += 1
            raise bot.BotInternalError("mock error")
        return MOCK_STORAGES[inv]

    mock_storages.call_count = 0

    bot_mock = monkeypatch_function(mp, bot.get_storages, module=bot, side_effect=mock_storages)
    st_mock = monkeypatch_startrek(mp)
    scenario = create_scenario(walle_test)
    stage = DetectStorageStage()
    stage_info = StageInfo()

    with pytest.raises(bot.BotInternalError):
        stage.run(stage_info, scenario)
    assert bot_mock.mock_calls == [call(inv) for inv in MOCK_INVS[:2]]
    st_mock.add_comment.assert_not_called()

    assert stage.run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert bot_mock.mock_calls == [call(inv) for inv in MOCK_INVS[:2] + MOCK_INVS[1:]]
    st_mock.add_comment.assert_called_once_with(issue_id=MOCK_TICKET, text=COMMENT_TEXT)


def test_save(walle_test, mp):
    bot_mock = monkeypatch_function(mp, bot.get_storages, module=bot, side_effect=lambda inv: MOCK_STORAGES[inv])
    script = create_script()
    stage_info = script.serialize()
    scenario = create_scenario(walle_test, stage_info=stage_info)

    st_mock = monkeypatch_startrek(mp)
    assert script.children[0].run(stage_info, scenario).status == MarkerStatus.SUCCESS
    assert bot_mock.mock_calls == [call(inv) for inv in MOCK_INVS]
    st_mock.add_comment.assert_called_once_with(issue_id=MOCK_TICKET, text=COMMENT_TEXT)

    scenario.save()
    walle_test.scenarios.assert_equal()
