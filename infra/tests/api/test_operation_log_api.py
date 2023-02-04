import pytest
import http.client

from infra.walle.server.tests.lib.util import ObjectMocker
from walle.operations_log.constants import Operation
from walle.operations_log.operations import OperationLog
from walle.util.misc import drop_none


def _mock_operation_log_entry(
    object_mocker,
    opid="0",
    audit_log_id="0.0",
    inv=1,
    name="mocked-host",
    uuid="0",
    project="test",
    scenario_id=1,
    optype=Operation.DEACTIVATE.type,
    time=0,
    params=None,
    aggregate=None,
):

    data = drop_none(
        dict(
            id=opid,
            audit_log_id=audit_log_id,
            host_inv=inv,
            host_name=name,
            host_uuid=uuid,
            project=project,
            scenario_id=scenario_id,
            type=optype,
            time=time,
            params=params,
            aggregate=aggregate,
        )
    )
    return object_mocker.mock(data)


def test_get_empty(walle_test, iterate_authentication):
    result = walle_test.api_client.get("/v1/operation-log")
    assert result.status_code == http.client.OK
    assert result.json == {"result": [], "total": 0}


def test_get_invalid_type(walle_test):
    result = walle_test.api_client.get("/v1/operation-log?type=invalid")
    assert result.status_code == http.client.BAD_REQUEST


def test_get_by_id(walle_test):
    log = ObjectMocker(OperationLog)
    entry = _mock_operation_log_entry(log, opid="0")

    result = walle_test.api_client.get("/v1/operation-log/invalid")
    assert result.status_code == http.client.NOT_FOUND

    result = walle_test.api_client.get("/v1/operation-log/{}".format(entry.id))
    assert result.status_code == http.client.OK
    assert result.json == entry.to_api_obj()


class TestGetOperationLog:
    @staticmethod
    def _init_dataset():
        log = ObjectMocker(OperationLog)
        return [
            _mock_operation_log_entry(
                log,
                opid="2",
                inv=2,
                optype=Operation.REBOOT.type,
                time=2,
                audit_log_id="0",
                name="mocked-host2",
                scenario_id=0,
                uuid="0",
                project="a",
            ).to_api_obj(),
            _mock_operation_log_entry(
                log,
                opid="1",
                inv=2,
                optype=Operation.REDEPLOY.type,
                time=1,
                audit_log_id="1",
                name="mocked-host2",
                scenario_id=1,
                uuid="0",
                project="b",
            ).to_api_obj(),
            _mock_operation_log_entry(
                log,
                opid="0",
                inv=1,
                optype=Operation.REBOOT.type,
                time=0,
                audit_log_id="2",
                name="mocked-host1",
                scenario_id=0,
                uuid="1",
                project="c",
            ).to_api_obj(),
        ]

    def test_get_all_records(self, walle_test):
        entries = self._init_dataset()

        result = walle_test.api_client.get("/v1/operation-log")
        assert result.status_code == http.client.OK
        assert result.json["result"] == entries

    @pytest.mark.parametrize(
        ["filter_name", "filter_value", "result_entry_indexes"],
        [
            ("id", "0", [2]),
            ("id", "1", [1]),
            ("id", "1, 0", [1, 2]),
            ("audit_log_id", "1", [1]),
            ("audit_log_id", "0", [0]),
            ("audit_log_id", "0, 1", [0, 1]),
            ("host_inv", 1, [2]),
            ("host_inv", 2, [0, 1]),
            ("host_inv", "1, 2", [0, 1, 2]),
            ("host_name", "mocked-host1", [2]),
            ("host_name", "mocked-host2", [0, 1]),
            ("host_name", "mocked-host2, mocked-host1", [0, 1, 2]),
            ("host_uuid", "0", [0, 1]),
            ("host_uuid", "0, 1", [0, 1, 2]),
            ("host_uuid", "1", [2]),
            ("project", "a", [0]),
            ("project", "c", [2]),
            ("scenario_id", 0, [0, 2]),
            ("scenario_id", 1, [1]),
            ("type", Operation.REBOOT.type, [0, 2]),
            ("type", Operation.REDEPLOY.type, [1]),
        ],
    )
    def test_filter_by_one_condition(self, walle_test, filter_name, filter_value, result_entry_indexes):
        dataset = self._init_dataset()
        entries = [dataset[i] for i in result_entry_indexes]

        result = walle_test.api_client.get("/v1/operation-log", query_string={filter_name: filter_value})
        assert result.status_code == http.client.OK
        assert len(result.json["result"]) == len(result_entry_indexes)
        assert result.json["result"] == entries

    @pytest.mark.parametrize(
        ["filter_names", "filter_values", "result_entry_indexes"],
        [
            [("host_name", "type"), ("mocked-host2", Operation.REBOOT.type), [0]],
            [("host_name", "type"), ("mocked-host2,mocked-host1", Operation.REBOOT.type), [0, 2]],
            [("type", "host_inv"), (Operation.REBOOT.type, 2), [0]],
            [("host_uuid", "project"), ("0", "b"), [1]],
        ],
    )
    def test_filter_by_few_conditions(self, walle_test, filter_names, filter_values, result_entry_indexes):
        dataset = self._init_dataset()
        entries = [dataset[i] for i in result_entry_indexes]

        query_string = {key: val for key, val in zip(filter_names, filter_values)}

        result = walle_test.api_client.get("/v1/operation-log", query_string=query_string)
        assert result.status_code == http.client.OK
        assert len(result.json["result"]) == len(result_entry_indexes)
        assert result.json["result"] == entries
