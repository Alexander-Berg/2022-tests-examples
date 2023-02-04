from unittest.mock import ANY, Mock

import pytest
from juggler_sdk.downtimes import (
    RemoveDowntimesResponse,
    SetDowntimesResponse,
    GetDowntimesResponse,
    Downtime,
    DowntimeSearchFilter,
)

from infra.walle.server.tests.lib.util import monkeypatch_config, monkeypatch_request, mock_response
from infra.walle.server.tests.lib.util import monkeypatch_function
from walle.clients import juggler

TEST_SOURCE = "walle_test_source"


@pytest.fixture()
def enable_juggler_events(mp):
    monkeypatch_config(mp, "juggler.events_enabled", True)
    monkeypatch_config(mp, "juggler.agent_host_port", "localhost:9090")


@pytest.fixture
def mock_juggler_api(mp):
    monkeypatch_function(mp, juggler._get_juggler_client, module=juggler, return_value=MockedJugglerApi())


@pytest.fixture
def mock_juggler_config(mp):
    mp.config("juggler.source", TEST_SOURCE)


class MockedJugglerApi:
    def __init__(self, *args, **kwargs):
        pass

    def remove_downtimes(self, downtime_ids):
        return RemoveDowntimesResponse(downtimes=downtime_ids)

    def set_downtimes(self, filters, start_time=None, end_time=None, description=None, source=None, downtime_id=None):
        return SetDowntimesResponse(downtime_id=1)

    def get_downtimes(self, filters, page, page_size):
        items = [
            Downtime(i, [DowntimeSearchFilter(host=j)], None, None, None, None) for i in range(1, 4) for j in range(2)
        ]
        return GetDowntimesResponse(items=items, page=1, page_size=1, total=1)


def _mock_received_events(events=None):
    if events is None:
        events = [{"code": 200}]

    return {
        "accepted_events": len([event for event in events if event["code"] == 200]),
        "events": events,
    }


def test_send_event_pushes_event_to_configured_juggler_port(enable_juggler_events, mp):
    mock_request = monkeypatch_request(mp, mock_response(_mock_received_events()))

    juggler.send_event("test_event", juggler.JugglerCheckStatus.OK, "test event sent ok")

    assert mock_request.called


def test_send_event_logs_error_when_send_event_failed(enable_juggler_events, mp):
    monkeypatch_request(mp, side_effect=Exception("exception mock"))
    mock_error_logger = mp.method(juggler.log.error, obj=juggler.log)

    juggler.send_event("test_event", juggler.JugglerCheckStatus.OK, "test event sent ok")

    mock_error_logger.assert_called_once_with(
        "Error in communication with juggler agent: %s. Events data was %s", "exception mock", ANY, exc_info=1
    )


def test_send_event_logs_error_when_some_events_not_accepted(enable_juggler_events, mp):
    mock_receive_events = [{"error": "service not provided", "code": 400}]
    monkeypatch_request(mp, mock_response(_mock_received_events(mock_receive_events)))

    mock_error_logger = mp.method(juggler.log.error, obj=juggler.log)

    juggler.send_event("test_event", juggler.JugglerCheckStatus.OK, "test event sent ok")

    mock_error_logger.assert_called_once_with(
        "Event %s:%s was rejected by juggler: %s. Event data was %s",
        ANY,  # host name
        "test_event.walle-unknown",
        "service not provided",
        ANY,  # event data in json format
    )


def test_send_event_logs_error_when_all_events_are_lost(enable_juggler_events, mp):
    monkeypatch_request(mp, mock_response(_mock_received_events([])))
    mock_error_logger = mp.method(juggler.log.error, obj=juggler.log)

    juggler.send_event("test_event", juggler.JugglerCheckStatus.OK, "test event sent ok")

    mock_error_logger.assert_called_once_with(
        "Events has been lost by juggler agent. Event data was %s", ANY  # event data in json format
    )


class TestJugglerClient:
    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_get_source(self, mp):
        SUFFIX = "test"
        client = juggler.JugglerClient()
        assert client._get_source(SUFFIX) == "{}.{}".format(TEST_SOURCE, SUFFIX)

    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_get_fqdn_to_downtimes_map(self):
        client = juggler.JugglerClient()
        result = client.get_fqdn_to_downtimes_map()
        assert result == {1: [1, 2, 3], 0: [1, 2, 3]}

    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_get_all_removable_downtimes(self):
        client = juggler.JugglerClient()
        result = client.get_fqdn_to_downtimes_map()
        assert result == {1: [1, 2, 3], 0: [1, 2, 3]}

    @pytest.mark.parametrize(["fqdn", "result"], [(0, True), (1, True), (2, False), (-9999, False), (999999, False)])
    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_is_downtimed(self, fqdn, result):
        client = juggler.JugglerClient()
        assert result == client.is_downtimed(fqdn)

    @pytest.mark.parametrize("downtime_ids", [[1, 2], [1, 2, 3], [x for x in range(100)]])
    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_remove_downtimes(self, downtime_ids):
        client = juggler.JugglerClient()
        result = client.remove_downtimes(downtime_ids)
        assert result.downtimes == downtime_ids

    @pytest.mark.parametrize("downtime_id", [1, 2, 0])
    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_remove_downtime(self, downtime_id):
        client = juggler.JugglerClient()
        result = client.remove_downtimes([downtime_id])
        assert result.downtimes == [downtime_id]

    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_set_downtime(self):
        client = juggler.JugglerClient()
        result = client.set_downtime("test01.yandex-team.ru", "test")
        assert result == 1

    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_edit_downtime(self):
        client = juggler.JugglerClient()
        result = client.edit_downtime("test01.yandex-team.ru", "test", 0, 0, 0)
        assert result == 1

    @pytest.mark.parametrize(["fqdn", "result"], [[0, [1, 2, 3]], [1, [1, 2, 3]], [2, None], [99999, None]])
    @pytest.mark.usefixtures("mock_juggler_api", "mock_juggler_config")
    def test_clear_downtimes(self, fqdn, result):
        client = juggler.JugglerClient()
        assert client.clear_downtimes(fqdn) == result


class TestExceptionMonitor:
    event_name = "event-name-mock"

    def test_no_exception(self, send_event_mock):
        with juggler.exception_monitor(self.event_name):
            print(2 + 2)  # no exception will raise (at least we hope so)
        send_event_mock.assert_called_once_with(self.event_name, "OK", "OK")

    def test_exception(self, send_event_mock):
        with juggler.exception_monitor(self.event_name):
            0 // 0
        send_event_mock.assert_called_once_with(self.event_name, "CRIT", "integer division or modulo by zero")

    def test_catch_specific_exception(self, send_event_mock):
        msg = "This value is prohibited by the federal law"
        with juggler.exception_monitor(self.event_name, exc_classes=[ValueError]):
            raise ValueError(msg)
        send_event_mock.assert_called_once_with(self.event_name, "CRIT", msg)

    def test_exception_msg_template(self, send_event_mock):
        with juggler.exception_monitor(self.event_name, err_msg_tmpl="Something wonderful happened: {exc}"):
            0 // 0
        send_event_mock.assert_called_once_with(
            self.event_name, "CRIT", "Something wonderful happened: integer division or modulo by zero"
        )

    def test_exception_reraise(self, send_event_mock):
        with pytest.raises(ZeroDivisionError):
            with juggler.exception_monitor(self.event_name, reraise=True):
                0 // 0
        send_event_mock.assert_called_once_with(self.event_name, "CRIT", "integer division or modulo by zero")

    def test_exception_callback(self, send_event_mock, mp):
        callback = Mock()
        msg = "stop wasting precious and rare Juggler resources in these trying times"
        exc_instance = ZeroDivisionError(msg)
        with juggler.exception_monitor(self.event_name, on_exc=callback):
            raise exc_instance
        callback.assert_called_once_with(exc_instance)
        send_event_mock.assert_called_once_with(self.event_name, "CRIT", msg)
