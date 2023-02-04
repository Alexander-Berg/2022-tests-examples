import json
from unittest.mock import Mock

import gevent
import pytest

from walle.clients import tvm, utils
from walle.projects import map_cms_project_alias_to_tvm_app_id


def prj_alias(project_id, cms_url):
    return "cms:{}:{}".format(project_id, cms_url)


FIRST_PROJECT = {"id": "some-project", "cms_settings": [{"cms": "1", "cms_tvm_app_id": 10001000}]}
FIRST_PROJECT_ALIAS = prj_alias(FIRST_PROJECT["id"], FIRST_PROJECT["cms_settings"][0]["cms"])


SECOND_PROJECT = {"id": "another-project", "cms_settings": [{"cms": "2", "cms_tvm_app_id": 20002000}]}
SECOND_PROJECT_ALIAS = prj_alias(SECOND_PROJECT["id"], SECOND_PROJECT["cms_settings"][0]["cms"])


WALLE_TVM_APP_ID = 1000000
KNOWN_SRC_TVM_APP_ID = 2001020
UNKNOWN_SRC_TVM_APP_ID = 3002030

service_ticket = "{}-service-ticket".format


@pytest.fixture(autouse=True)
def setup_config(mp):
    mp.config("tvm.api_url", "https://tvm-api.yandex.net/2")
    mp.config("tvm.secret", "very secret secret")
    mp.config("tvm.app_id", WALLE_TVM_APP_ID)


@pytest.fixture()
def service_context_mock(mp):
    sc_mock = Mock()
    sc_mock.sign = lambda ts, tvm_app_id: "signature"
    sc_mock.check.return_value = Mock()
    mp.method(tvm.TvmServiceTicketManager._get_service_context, return_value=sc_mock, obj=tvm.TvmServiceTicketManager)
    return sc_mock


@pytest.fixture()
def tvm_server(mp):
    class TVMServer:
        """Mock tvm api side, returning ticket mock in their format or error if dst is not known"""

        def __init__(self):
            self.tvm_app_id_to_alias = {WALLE_TVM_APP_ID: "walle"}

        def add_app_id_alias(self, tvm_app_id, alias):
            self.tvm_app_id_to_alias[tvm_app_id] = alias

        def respond(self, _method, _url, **kwargs):
            dst_app_ids = map(int, kwargs["data"]["dst"].split(","))
            resp = utils.requests.Response()
            resp.headers = {"Content-Type": "application/json"}
            resp.status_code = 200

            resp_content = {}
            for app_id in dst_app_ids:
                try:
                    ticket_resp = {"ticket": service_ticket(self.tvm_app_id_to_alias[app_id])}
                except KeyError as e:
                    ticket_resp = {"error": "Destination not found, client_id={}".format(e.args[0])}
                resp_content[str(app_id)] = ticket_resp
            resp._content = json.dumps(resp_content)
            return resp

    server = TVMServer()
    mp.request(side_effect=server.respond)
    return server


@pytest.fixture()
def project_with_tvm(walle_test):
    return walle_test.mock_project(FIRST_PROJECT)


@pytest.fixture()
def ticket_manager(mp, walle_test, service_context_mock, project_with_tvm, tvm_server):
    tvm_server.add_app_id_alias(FIRST_PROJECT["cms_settings"][0]["cms_tvm_app_id"], FIRST_PROJECT_ALIAS)
    tvm_server.add_app_id_alias(KNOWN_SRC_TVM_APP_ID, "idm-testing")
    return tvm.TvmServiceTicketManager(
        [map_cms_project_alias_to_tvm_app_id, lambda: {"idm-testing": KNOWN_SRC_TVM_APP_ID}]
    )


@pytest.fixture()
def generator_is_ready(ticket_manager):
    ticket_manager._ready.wait()


def test_returns_service_tickets(ticket_manager):
    assert ticket_manager.get_ticket_for_alias(FIRST_PROJECT_ALIAS) == "{}-service-ticket".format(FIRST_PROJECT_ALIAS)


def test_uses_tvm_api_handle(mp, ticket_manager):
    api_request_mock = mp.method(
        tvm.TvmServiceTicketManager._api_request, wrap_original=True, obj=tvm.TvmServiceTicketManager
    )
    ticket_manager._refresh_tickets()
    assert api_request_mock.mock_calls[0][1] == (ticket_manager, "POST", "/ticket/")


@pytest.mark.usefixtures("generator_is_ready")
def test_refreshes_tickets_when_asked_for_unknown_projects(mp, walle_test, ticket_manager, tvm_server):
    walle_test.mock_project(SECOND_PROJECT)  # new project generator doesn't know about
    tvm_server.add_app_id_alias(SECOND_PROJECT["cms_settings"][0]["cms_tvm_app_id"], SECOND_PROJECT_ALIAS)
    refresh_tickets_mock = mp.method(
        tvm.TvmServiceTicketManager._refresh_tickets, wrap_original=True, obj=tvm.TvmServiceTicketManager
    )
    ticket = ticket_manager.get_ticket_for_alias(SECOND_PROJECT_ALIAS)
    assert refresh_tickets_mock.called
    assert ticket == "{}-service-ticket".format(SECOND_PROJECT_ALIAS)


@pytest.mark.usefixtures("generator_is_ready")
def test_handles_tvm_errors(mp, walle_test, ticket_manager):
    # To provoke an error from server, we ask it for tvm app id we didn't mock in it
    walle_test.mock_project(SECOND_PROJECT)

    handle_update_error_mock = mp.method(
        tvm.TvmServiceTicketManager._handle_service_ticket_update_error,
        wrap_original=True,
        obj=tvm.TvmServiceTicketManager,
    )
    with pytest.raises(tvm.TvmServiceTicketUpdateError):
        ticket_manager.get_ticket_for_alias(SECOND_PROJECT_ALIAS)
    assert handle_update_error_mock.called

    # check if manager state is consistent after exception
    assert ticket_manager.get_ticket_for_alias(FIRST_PROJECT_ALIAS) == "{}-service-ticket".format(FIRST_PROJECT_ALIAS)


@pytest.mark.usefixtures("project_with_tvm")
def test_blocks_during_ticket_refresh(walle_test, mp, ticket_manager, tvm_server):
    restart_block = gevent.event.Event()
    restart_block.clear()

    orig_update_alias_to_tvm_app_id = tvm.TvmServiceTicketManager._update_alias_to_tvm_app_id

    def waiter(self):
        """We need some way to stop execution inside client restart part to check if it blocks
        This function will block until we set the event, then execute the original function
        """
        restart_block.wait()
        return orig_update_alias_to_tvm_app_id(self)

    mp.method(
        tvm.TvmServiceTicketManager._update_alias_to_tvm_app_id, side_effect=waiter, obj=tvm.TvmServiceTicketManager
    )

    greenlet_started = gevent.event.Event()
    greenlet_started.clear()

    def generator_asker():
        """Will cause generator restart by asking for the project it doesn't know about"""
        walle_test.mock_project(SECOND_PROJECT)
        tvm_server.add_app_id_alias(SECOND_PROJECT["cms_settings"][0]["cms_tvm_app_id"], SECOND_PROJECT_ALIAS)
        greenlet_started.set()
        return ticket_manager.get_ticket_for_alias(SECOND_PROJECT_ALIAS)

    client = gevent.spawn(generator_asker)

    greenlet_started.wait()
    assert not ticket_manager.is_ready()

    restart_block.set()
    client.join()

    assert client.value == service_ticket(SECOND_PROJECT_ALIAS)


@pytest.mark.usefixtures("generator_is_ready")
def test_sends_juggler_events(mp, ticket_manager):
    send_mock = mp.method(tvm.TvmServiceTicketManager._notify_juggler, obj=tvm.TvmServiceTicketManager)
    ticket_manager._set_need_update()
    ticket_manager._ready.wait()
    assert send_mock.called


@pytest.mark.usefixtures("project_with_tvm")
def test_project_id_to_tvm_app_id_mapper():
    expected_projects = {
        "cms:{}:{}".format(FIRST_PROJECT["id"], FIRST_PROJECT["cms_settings"][0]["cms"]): FIRST_PROJECT["cms_settings"][
            0
        ]["cms_tvm_app_id"]
    }
    assert map_cms_project_alias_to_tvm_app_id() == expected_projects


@pytest.mark.usefixtures("generator_is_ready")
def test_source_app_id_is_allowed(ticket_manager, service_context_mock):
    service_context_mock.check.return_value.src = KNOWN_SRC_TVM_APP_ID
    ticket_manager.check_service_ticket("some-tvm-ticket", ["idm-testing", "idm-stable"])


def test_source_app_id_is_not_allowed(ticket_manager, service_context_mock):
    service_context_mock.check.return_value.src = KNOWN_SRC_TVM_APP_ID
    with pytest.raises(tvm.TvmSourceIsNotAllowed):
        ticket_manager.check_service_ticket("some-tvm-ticket", ["non-matching-alias"])


def test_raises_on_unknown_source(ticket_manager, service_context_mock):
    service_context_mock.check.return_value.src = UNKNOWN_SRC_TVM_APP_ID
    with pytest.raises(tvm.TvmUnknownAppId):
        ticket_manager.check_service_ticket("some-tvm-ticket", ["idm-testing", "idm-stable"])
