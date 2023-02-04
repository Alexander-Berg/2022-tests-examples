"""Tests server preorder processing."""

import json
from unittest.mock import call

import pytest
import requests
from requests.models import Response

import walle.preorders
from infra.walle.server.tests.lib.util import (
    TestCase,
    load_mock_data,
    patch,
    monkeypatch_inventory_get_host_info_and_check_status,
    monkeypatch_locks,
    mock_host_adding,
    mock_schedule_host_preparing,
    monkeypatch_audit_log,
    monkeypatch_network_get_current_host_switch_port,
    BOT_PROJECT_ID,
    mock_schedule_wait_for_bot_acquirement,
    monkeypatch_config,
)
from walle import constants
from walle import restrictions
from walle.admin_requests.severity import EineTag
from walle.clients import bot, inventory
from walle.constants import EINE_PROFILES_WITH_DC_SUPPORT
from walle.hosts import HostState
from walle.physical_location_tree import LocationNamesMap


@pytest.fixture
def test(request, mp, monkeypatch_timestamp):
    monkeypatch_locks(mp)
    monkeypatch_audit_log(mp)
    monkeypatch_inventory_get_host_info_and_check_status(mp)
    monkeypatch_network_get_current_host_switch_port(mp)
    mp.function(inventory.get_eine_profiles, return_value=EINE_PROFILES_WITH_DC_SUPPORT)
    return TestCase.create(request)


@pytest.fixture
def renaming_test(request, mp, monkeypatch_timestamp):
    monkeypatch_locks(mp)
    monkeypatch_audit_log(mp)
    monkeypatch_network_get_current_host_switch_port(mp)
    return TestCase.create(request)


@pytest.fixture
def shortnames():
    LocationNamesMap(path="country-mock|city-mock|dc-mock", name="mdc").save(force_insert=True)
    LocationNamesMap(path="country-mock|city-mock|dc-mock|queue-mock", name="m-queue").save(force_insert=True)


@pytest.fixture
def bot_preorder_raw_data():
    return load_mock_data("mocks/bot-preorders.json")


def test_get_preorder_info(mp, bot_preorder_raw_data):
    BOT_PREORDER_ID = 9906
    BOT_PREORDER_INFO = {
        "id": BOT_PREORDER_ID,
        "status": "CLOSE",
        "owner": "robot-di-bot-prod",
        "bot_project_id": "100000227",
        "servers": {
            "108071386": "taken",
            "108071387": "taken",
            "108071388": "taken",
            "108071389": "taken",
            "108071390": "taken",
            "108071391": "taken",
            "108071392": "taken",
            "108071393": "taken",
        },
    }
    ok_response = Response()
    ok_response.status_code = requests.codes.ok
    mp.function(bot.json_request, return_value=(ok_response, json.loads(bot_preorder_raw_data)))
    response = bot.get_preorder_info(BOT_PREORDER_ID)

    assert response == BOT_PREORDER_INFO


def test_processing(test):
    test.preorders.mock(dict(id=1, processed=True))
    test.preorders.mock(dict(id=2, processed=False))
    test.preorders.mock(dict(id=3, processed=True))
    test.preorders.mock(dict(id=4, processed=False))

    processed_ids = []

    def process_preorder(preorder, _):
        processed_ids.append(preorder.id)

    with patch("walle.preorders._process_preorder", side_effect=process_preorder):
        walle.preorders._process_preorders()

    assert sorted(processed_ids) == [2, 4]
    test.preorders.assert_equal()


def test_process_invalid_preorder(test, mp):
    get_preorder_info = mp.function(bot.get_preorder_info, side_effect=bot.InvalidPreorderIdError(2))
    test.preorders.mock(dict(id=1, processed=False))

    preorder = test.preorders.mock(dict(id=2, processed=False))
    del preorder.audit_log_id
    preorder.save()

    test.preorders.mock(dict(id=3, processed=False))

    _process_preorder(preorder)
    preorder.errors = ["The preorder has been deleted from BOT."]
    preorder.processed = True

    get_preorder_info.assert_called_once_with(preorder.id)
    test.preorders.assert_equal()


@pytest.mark.parametrize(
    "preorder_side_effects",
    (
        [bot.BotInternalError("The server returned garbage")],
        [
            bot.BotInternalError("The server returned rubbish"),
            bot.BotInternalError("The server returned litter"),
            bot.BotInternalError("The server returned garbage"),
        ],
    ),
)
def test_process_preorders__remembers_last_bot_error(preorder_side_effects, test):
    preorder = test.preorders.mock(dict(id=2, processed=False))

    with patch("walle.preorders._process_preorder", side_effect=preorder_side_effects):
        for _ in preorder_side_effects:
            walle.preorders._process_preorders()

    # check that only last error stays
    exception_message = "Error in communication with BOT: The server returned garbage"
    preorder.messages = [exception_message]

    test.preorders.assert_equal()


@pytest.mark.parametrize(
    "acquire_host_side_effects",
    (
        [
            bot.BotInternalError("The server returned garbage"),
            bot.InvalidInventoryNumber(2),
            bot.InvalidInventoryNumber(3),
        ],
        [
            bot.BotInternalError("The server returned rubbish"),
            bot.BotInternalError("The server returned litter"),
            bot.BotInternalError("The server returned garbage"),
        ],
    ),
)
def test_process_preorders__remembers_last_bot_errors_for_host(test, mp, shortnames, acquire_host_side_effects):
    mp.function(
        bot.get_preorder_info,
        return_value={
            "status": bot.PreorderStatus.OPEN,
            "servers": {1: bot.ServerStatus.NEW, 2: bot.ServerStatus.NEW, 3: bot.ServerStatus.NEW},
        },
    )

    preorder = test.preorders.mock(dict(id=2, processed=False))
    expected_messages = []
    expected_errors = []

    with patch("walle.clients.bot.acquire_preordered_host", side_effect=acquire_host_side_effects):
        p = preorder.__class__.objects.get(id=preorder.id)
        actual_messages = _process_preorder(p)
        for i, exception in enumerate(acquire_host_side_effects, start=1):
            if isinstance(exception, bot.BotInternalError):
                expected_messages.append("Got an error during processing #{} host: {}.".format(i, exception))
            else:
                expected_errors.append("Failed to acquire #{}: {}".format(i, exception))
                preorder.failed_hosts.append(i)

    preorder.errors = expected_errors if expected_errors else None

    test.preorders.assert_equal()
    assert expected_messages == actual_messages


def test_process_preorders_remembers_both_host_and_preorder_messages(test):
    preorder = test.preorders.mock(
        dict(
            id=2,
            processed=False,
            messages=[
                "Error during processing the preorder: BOT returned garbage, well, that was bad",
            ],
        )
    )

    def process_preorder(preorder, messages):
        messages.append('host message')
        raise bot.BotInternalError("bot communication error")

    with patch("walle.preorders._process_preorder", side_effect=process_preorder):
        walle.preorders._process_preorders()

    preorder.messages = ['host message', 'Error in communication with BOT: bot communication error']
    test.preorders.assert_equal()


def test_process_preorders_removes_previous_errors_on_success(test, mp):
    mp.function(bot.get_preorder_info, return_value={"status": bot.PreorderStatus.OPEN, "servers": {}})

    preorder = test.preorders.mock(
        dict(
            id=2,
            processed=False,
            messages=[
                "Error during processing the preorder: BOT returned garbage, well, that was bad",
            ],
        )
    )

    walle.preorders._process_preorders()

    preorder.messages = None
    test.preorders.assert_equal()


def test_process_hosts_error(test, mp, shortnames):
    get_preorder_info = mp.function(
        bot.get_preorder_info,
        return_value={"status": bot.PreorderStatus.OPEN, "servers": {1: bot.ServerStatus.NEW, 2: bot.ServerStatus.NEW}},
    )

    preorder = test.preorders.mock(dict(id=1, processed=False))

    def acquire_preordered_host(oredr_id, inv, name):
        if inv == 2:
            raise bot.InvalidInventoryNumber(inv)

    acquire_mock = mp.function(bot.acquire_preordered_host, side_effect=acquire_preordered_host)

    _process_preorder(preorder)

    preorder.failed_hosts = [2]
    preorder.acquired_hosts = [1]
    preorder.errors = ["Failed to acquire #2: Host with #2 inventory number is not registered in BOT."]

    get_preorder_info.assert_called_once_with(preorder.id)
    assert acquire_mock.mock_calls == [call(1, 1, "mocked-1.mock"), call(1, 2, "mocked-2.mock")]

    test.preorders.assert_equal()


def test_process_new_host(test, mp, shortnames):
    get_preorder_info = mp.function(
        bot.get_preorder_info, return_value={"status": bot.PreorderStatus.OPEN, "servers": {1: bot.ServerStatus.NEW}}
    )

    preorder = test.preorders.mock(dict(id=1, processed=False))
    acquire_mock = mp.function(bot.acquire_preordered_host)

    _process_preorder(preorder)

    preorder.acquired_hosts = [1]

    get_preorder_info.assert_called_once_with(preorder.id)
    acquire_mock.assert_called_once_with(1, 1, "mocked-1.mock")

    test.preorders.assert_equal()


def test_process_taken_host(test, mp, shortnames):
    get_preorder_info = mp.function(
        bot.get_preorder_info, return_value={"status": bot.PreorderStatus.OPEN, "servers": {1: bot.ServerStatus.TAKEN}}
    )
    preorder = test.preorders.mock(dict(id=1, processed=False))
    _process_preorder(preorder)

    preorder.failed_hosts = [1]
    preorder.errors = ["Failed to acquire #1: Host #1 has been already taken."]
    get_preorder_info.assert_called_once_with(preorder.id)

    test.preorders.assert_equal()


def test_process_hosts_shortname(test, mp, monkeypatch_host_uuid):
    monkeypatch_config(mp, "shortnames.cities_with_disabled_name_autogeneration", ["city-mock"])
    mp.function(
        bot.get_preorder_info, return_value={"status": bot.PreorderStatus.OPEN, "servers": {1: bot.ServerStatus.NEW}}
    )
    preorder = test.preorders.mock(dict(id=1, processed=False))
    messages = _process_preorder(preorder)

    test.preorders.assert_equal()
    test.hosts.assert_equal()
    assert messages == [
        "Got an error during processing #1 host: Datacenter or queue country-mock|city-mock|dc-mock"
        " does not have a short name. Please, consult the https://st.yandex-team.ru/WALLESUPPORT.."
    ]

    # create shortnames:
    LocationNamesMap(path="country-mock|city-mock|dc-mock", name="mdc").save(force_insert=True)
    LocationNamesMap(path="country-mock|city-mock|dc-mock|queue-mock", name="m-queue").save(force_insert=True)

    _process_preorder(preorder)

    host = test.mock_host(dict(inv=1, name="mocked-1.mock", state=HostState.FREE, project=preorder.project), save=False)
    mock_host_adding(host, manual=False, reason="Acquire the host from #1 preorder added by mocked-user@.")

    preorder.acquired_hosts.append(1)
    test.hosts.assert_equal()


def test_closed_preorder(test, mp):
    mp.function(walle.preorders._process_host)
    mp.function(walle.clients.bot.acquire_preordered_host)

    mp.function(
        walle.clients.bot.get_preorder_info,
        return_value={
            "status": bot.PreorderStatus.CLOSED,
            "servers": {
                1: bot.ServerStatus.TAKEN,
                2: bot.ServerStatus.NEW,
                3: bot.ServerStatus.NEW,
            },
        },
    )

    preorder = test.preorders.mock(
        {
            "id": 1,
            "processed": False,
            "acquired_hosts": [2],
            "failed_hosts": [1, 3],
            "errors": ["Failed to acquire #1: Host #1 has been already taken."],
        }
    )

    _process_preorder(preorder)
    del preorder.audit_log_id
    preorder.processed = True

    test.preorders.assert_equal()


@pytest.mark.parametrize("bot_hostname", [None, "some-hostname"])
def test_process_host(renaming_test, shortnames, mp, bot_hostname, monkeypatch_host_uuid):
    monkeypatch_inventory_get_host_info_and_check_status(mp, hostname=bot_hostname)
    acquire_host_mock = mp.function(bot.acquire_preordered_host)

    preorder = renaming_test.preorders.mock(dict(id=666, processed=False))
    _process_host(preorder, 1)

    hostname = bot_hostname if bot_hostname else "new-666-1.{}".format(constants.WALLE_HOST_FQDN_SUFFIX)
    host = renaming_test.mock_host(
        dict(inv=1, name=hostname, state=HostState.FREE, project=preorder.project), save=False
    )
    mock_host_adding(host, manual=False, reason="Acquire the host from #666 preorder added by mocked-user@.")
    mock_schedule_wait_for_bot_acquirement(host)

    preorder.acquired_hosts.append(1)

    renaming_test.preorders.assert_equal()
    renaming_test.hosts.assert_equal()
    acquire_host_mock.assert_called_once_with(666, 1, hostname)


@pytest.mark.parametrize("hostname", [None, "host-1.search.not-yandex.net"])
def test_process_existing_host(test, mp, hostname, monkeypatch_host_uuid):
    mp.function(bot.acquire_preordered_host)

    preorder = test.preorders.mock(dict(id=666, processed=False))
    host = test.mock_host(dict(inv=1, name=hostname, state=HostState.FREE, project=preorder.project))

    _process_host(preorder, host.inv)
    preorder.failed_hosts.append(host.inv)
    preorder.errors = ["Failed to acquire #{}: Host #{} already exists in Wall-e.".format(host.inv, host.inv)]

    test.preorders.assert_equal()
    test.hosts.assert_equal()


def test_process_with_preparing(test, mp, shortnames, monkeypatch_host_uuid):
    mp.function(bot.acquire_preordered_host)

    project = test.mock_project({"id": "project-mock", "bot_project_id": BOT_PROJECT_ID})
    preorder = test.preorders.mock(dict(id=666, project=project.id, processed=False, prepare=True))
    _process_host(preorder, 1)

    host = test.mock_host(dict(inv=1, state=HostState.FREE, project=preorder.project), save=False)
    mock_host_adding(host, manual=False, reason="Acquire the host from #666 preorder added by mocked-user@.")
    mock_schedule_host_preparing(
        host,
        bot_project_id=project.bot_project_id,
        manual=False,
        with_auto_healing=True,
        preorder=666,
        reason="Host preparing has been requested by #666 preorder added by mocked-user@.",
        update_firmware_needed=True,
        repair_request_severity=EineTag.LOW,
    )

    preorder.acquired_hosts.append(1)

    test.preorders.assert_equal()
    test.hosts.assert_equal()


@pytest.mark.parametrize("need_certificate", [True, False])
def test_process_with_custom_preparing(mp, test, shortnames, need_certificate, monkeypatch_host_uuid):
    mp.function(walle.clients.inventory.check_deploy_configuration)
    test.mock_project(dict(id="test-project", name="Test project", certificate_deploy=need_certificate))
    preorder = test.preorders.mock(
        dict(
            id=666,
            processed=False,
            prepare=True,
            project="test-project",
            provisioner=test.host_provisioner,
            deploy_config=test.host_deploy_config,
            restrictions=[restrictions.AUTOMATION],
        )
    )

    mp.function(bot.acquire_preordered_host)
    _process_host(preorder, 1)

    host = test.mock_host(dict(inv=1, state=HostState.FREE, project=preorder.project), save=False)
    mock_host_adding(host, manual=False, reason="Acquire the host from #666 preorder added by mocked-user@.")
    mock_schedule_host_preparing(
        host,
        manual=False,
        provisioner=preorder.provisioner,
        config=preorder.deploy_config,
        need_certificate=need_certificate,
        custom_deploy_config=True,
        with_auto_healing=True,
        restrictions=preorder.restrictions,
        preorder=666,
        bot_project_id=BOT_PROJECT_ID,
        reason="Host preparing has been requested by #666 preorder added by mocked-user@.",
        update_firmware_needed=True,
        repair_request_severity=EineTag.LOW,
    )

    preorder.acquired_hosts.append(1)

    test.preorders.assert_equal()
    test.hosts.assert_equal()


def _process_preorder(preorder, messages=None):
    if messages is None:
        messages = []

    walle.preorders._process_preorder(preorder.copy(), messages)
    return messages


def _process_host(preorder, inv, acquire=True):
    walle.preorders._process_host(preorder.copy(), inv, acquire)
