"""Tests audit log API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import monkeypatch_audit_log
from walle import audit_log, authorization
from walle.util.misc import drop_none


def test_get_empty(walle_test, iterate_authentication):
    result = walle_test.api_client.get("/v1/audit-log")
    assert result.status_code == http.client.OK
    assert result.json == {"result": [], "total": 0}


def test_get_invalid_limit(walle_test):
    result = walle_test.api_client.get("/v1/audit-log?limit=-1")
    assert result.status_code == http.client.BAD_REQUEST

    result = walle_test.api_client.get("/v1/audit-log?limit=aa")
    assert result.status_code == http.client.BAD_REQUEST


def test_get_invalid_type(walle_test):
    result = walle_test.api_client.get("/v1/audit-log?type=invalid")
    assert result.status_code == http.client.BAD_REQUEST


def test_get_by_id(walle_test):
    entry = audit_log.on_add_project(authorization.ISSUER_WALLE, "project-mock", user_project={})

    result = walle_test.api_client.get("/v1/audit-log/" + entry.id + "-invalid")
    assert result.status_code == http.client.NOT_FOUND

    result = walle_test.api_client.get("/v1/audit-log/" + entry.id)
    assert result.status_code == http.client.OK
    assert result.json == entry.to_api_obj()


class TestGet:
    AUTOMATION_PLOT_1_ID = "automation-plot1"
    AUTOMATION_PLOT_2_ID = "automation-plot2"

    MAINTENANCE_PLOT_1_ID = "maintenance-plot1"
    MAINTENANCE_PLOT_2_ID = "maintenance-plot2"

    PROJECT_1_ID = "project1"
    PROJECT_2_ID = "project2"

    PREORDER_1_ID = 100001
    PREORDER_2_ID = 100002

    HOST_1_INV = 1
    HOST_1_NAME = "host_1_name"
    HOST_1_UUID = "host_1_uuid"

    HOST_2_INV = 2
    HOST_2_NAME = "host_2_name"
    HOST_2_UUID = "host_2_uuid"

    def _init_dataset(self, monkeypatch):
        monkeypatch_audit_log(monkeypatch, uuid="1", time=1, patch_create=False)
        audit_log.on_add_host(
            authorization.ISSUER_ANONYMOUS_USER,
            self.PROJECT_1_ID,
            self.HOST_1_INV,
            self.HOST_1_NAME,
            self.HOST_1_UUID,
            preorder_id=None,
            user_host={},
            added_host={},
        )

        monkeypatch_audit_log(monkeypatch, uuid="2", time=2, patch_create=False)
        audit_log.on_update_host(
            authorization.ISSUER_WALLE, self.PROJECT_2_ID, self.HOST_1_INV, self.HOST_1_NAME, self.HOST_1_UUID, {}
        )

        monkeypatch_audit_log(monkeypatch, uuid="3", time=3, patch_create=False)
        audit_log.on_delete_host(
            authorization.ISSUER_ANONYMOUS_USER,
            self.PROJECT_1_ID,
            self.HOST_2_INV,
            self.HOST_2_NAME,
            self.HOST_2_UUID,
            False,
        )

    def test_get_all_records(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log"),
            [
                {"time": 3, "type": audit_log.TYPE_DELETE_HOST},
                {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
                {"time": 1, "type": audit_log.TYPE_ADD_HOST},
            ],
            total=3,
        )

    def test_filter_by_issuer(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"issuer": authorization.ISSUER_ANONYMOUS_USER}),
            [{"time": 3, "type": audit_log.TYPE_DELETE_HOST}, {"time": 1, "type": audit_log.TYPE_ADD_HOST}],
            total=2,
        )

    def test_filter_by_project_id(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"project": self.PROJECT_1_ID}),
            [{"time": 3, "type": audit_log.TYPE_DELETE_HOST}, {"time": 1, "type": audit_log.TYPE_ADD_HOST}],
            total=2,
        )

    def test_filter_by_host_inv(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"host_inv": self.HOST_2_INV}),
            [{"time": 3, "type": audit_log.TYPE_DELETE_HOST}],
            total=1,
        )

    def test_filter_by_host_name(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"host_name": self.HOST_1_NAME}),
            [
                {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
                {"time": 1, "type": audit_log.TYPE_ADD_HOST},
            ],
            total=2,
        )

    @pytest.mark.skip(reason="feature was disable cause mongo regex is slow")
    def test_filter_by_host_name_prefix(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"host_name": self.HOST_1_NAME[:6]}),
            [
                {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
                {"time": 1, "type": audit_log.TYPE_ADD_HOST},
            ],
            total=2,
        )

    def test_filter_by_host_uuid(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"host_uuid": self.HOST_1_UUID}),
            [
                {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
                {"time": 1, "type": audit_log.TYPE_ADD_HOST},
            ],
            total=2,
        )

    def test_filter_by_host_uuid_list(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)
        expected_result = [
            {"time": 3, "type": audit_log.TYPE_DELETE_HOST},
            {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
            {"time": 1, "type": audit_log.TYPE_ADD_HOST},
        ]

        _check_result(
            walle_test.api_client.get(
                "/v1/audit-log", query_string={"host_uuid": ",".join((self.HOST_1_UUID, self.HOST_2_UUID))}
            ),
            expected_result,
            total=3,
        )

    def test_filter_by_preorder_id(self, walle_test, monkeypatch):
        monkeypatch_audit_log(monkeypatch, uuid="4", time=4, patch_create=False)
        audit_log.on_add_preorder(authorization.ISSUER_ANONYMOUS_USER, self.PROJECT_1_ID, {"id": self.PREORDER_1_ID})

        monkeypatch_audit_log(monkeypatch, uuid="5", time=5, patch_create=False)
        audit_log.on_add_preorder(authorization.ISSUER_ANONYMOUS_USER, self.PROJECT_2_ID, {"id": self.PREORDER_2_ID})

        monkeypatch_audit_log(monkeypatch, uuid="6", time=6, patch_create=False)
        audit_log.on_add_host(
            authorization.ISSUER_ANONYMOUS_USER,
            self.PROJECT_2_ID,
            self.HOST_2_INV,
            self.HOST_2_NAME,
            self.HOST_2_UUID,
            preorder_id=self.PREORDER_2_ID,
            user_host={},
            added_host={},
        )

        result = walle_test.api_client.get(
            "/v1/audit-log",
            query_string={"preorder": self.PREORDER_2_ID, "fields": ["id", "type", "preorder", "host_inv"]},
        )

        assert result.json["result"] == [
            {
                "id": "6",
                "type": audit_log.TYPE_ADD_HOST,
                "preorder": self.PREORDER_2_ID,
                "host_inv": self.HOST_2_INV,
                "time": 6,  # 'time' is returned because it is a cursor field, even though we didn't specify it
            },
            {
                "id": "5",
                "type": audit_log.TYPE_ADD_PREORDER,
                "preorder": self.PREORDER_2_ID,
                "time": 5,  # 'time' is returned because it is a cursor field, even though we didn't specify it
            },
        ]

    def test_filter_by_automation_plot_id(self, walle_test, monkeypatch):
        monkeypatch_audit_log(monkeypatch, uuid="4", time=4, patch_create=False)
        audit_log.on_update_automation_plot(authorization.ISSUER_ANONYMOUS_USER, self.AUTOMATION_PLOT_1_ID, {})

        monkeypatch_audit_log(monkeypatch, uuid="5", time=5, patch_create=False)
        audit_log.on_update_automation_plot(authorization.ISSUER_ANONYMOUS_USER, self.AUTOMATION_PLOT_2_ID, {})

        result = walle_test.api_client.get(
            "/v1/audit-log",
            query_string={"automation_plot": self.AUTOMATION_PLOT_2_ID, "fields": ["id", "type", "automation_plot"]},
        )

        assert result.json["result"] == [
            {
                "id": "5",
                "type": audit_log.TYPE_UPDATE_AUTOMATION_PLOT,
                "automation_plot": self.AUTOMATION_PLOT_2_ID,
                "time": 5,
            }
        ]

    def test_filter_by_empty_type_list_returns_empty_result(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)
        _check_result(walle_test.api_client.get("/v1/audit-log?type="), [], total=0)

    def test_filter_by_single_type(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"type": audit_log.TYPE_UPDATE_HOST}),
            [{"time": 2, "type": audit_log.TYPE_UPDATE_HOST}],
            total=1,
        )

    def test_filter_by_list_of_types(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get(
                "/v1/audit-log", query_string={"type": audit_log.TYPE_UPDATE_HOST + "," + audit_log.TYPE_DELETE_HOST}
            ),
            [
                {"time": 3, "type": audit_log.TYPE_DELETE_HOST},
                {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
            ],
            total=2,
        )

    def test_filter_by_start_time(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"start_time": 2}),
            [
                {"time": 3, "type": audit_log.TYPE_DELETE_HOST},
                {"time": 2, "type": audit_log.TYPE_UPDATE_HOST},
            ],
            total=2,
        )

    def test_filter_time_interval(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(
            walle_test.api_client.get("/v1/audit-log", query_string={"start_time": 2, "end_time": 3}),
            [{"time": 2, "type": audit_log.TYPE_UPDATE_HOST}],
            total=1,
        )

    def test_filter_scenario_id(self, walle_test, monkeypatch):
        monkeypatch_audit_log(monkeypatch, uuid="4", time=4, patch_create=False)
        audit_log.on_add_scenario(
            authorization.ISSUER_ANONYMOUS_USER,
            1,
            "test",
            reason="test_scenario",
            scenario_type="test_type",
            ticket_key="WALLE-2807",
            script_args={"a": 1},
            status="CREATED",
        )

        result = walle_test.api_client.get(
            "/v1/audit-log",
            query_string={"scenario_id": 1, "fields": ["id", "type", "scenario_id", "payload", "reason"]},
        )

        assert result.json["result"] == [
            {
                "id": "4",
                "type": audit_log.TYPE_ADD_SCENARIO,
                "scenario_id": 1,
                "reason": "test_scenario",
                "time": 4,
                "payload": {
                    "scenario_name": "test",
                    "scenario_type": "test_type",
                    "ticket_key": "WALLE-2807",
                    "script_args": {"a": 1},
                    "status": "CREATED",
                },
            }
        ]

    def test_filter_maintenance_plot_id(self, walle_test, monkeypatch):
        monkeypatch_audit_log(monkeypatch, uuid="42", time=42, patch_create=False)
        audit_log.on_update_maintenance_plot(authorization.ISSUER_ANONYMOUS_USER, self.MAINTENANCE_PLOT_1_ID, {})

        monkeypatch_audit_log(monkeypatch, uuid="84", time=84, patch_create=False)
        audit_log.on_update_maintenance_plot(authorization.ISSUER_ANONYMOUS_USER, self.MAINTENANCE_PLOT_2_ID, {})

        result = walle_test.api_client.get(
            "/v1/audit-log",
            query_string={"maintenance_plot": self.MAINTENANCE_PLOT_2_ID, "fields": ["id", "type", "maintenance_plot"]},
        )

        assert result.json["result"] == [
            {
                "id": "84",
                "type": audit_log.TYPE_UPDATE_MAINTENANCE_PLOT,
                "maintenance_plot": self.MAINTENANCE_PLOT_2_ID,
                "time": 84,
            }
        ]

    def test_limit_result_items(self, walle_test, monkeypatch):
        self._init_dataset(monkeypatch)

        _check_result(walle_test.api_client.get("/v1/audit-log?limit=0"), [], total=3)
        _check_result(
            walle_test.api_client.get("/v1/audit-log?limit=1"),
            [{"time": 3.0, "type": audit_log.TYPE_DELETE_HOST}],
            2,
            3,
        )


def _check_result(result, objects, next_cursor=None, total=None):
    assert result.status_code == http.client.OK
    assert _strip_result(result.json) == drop_none({"result": objects, "next_cursor": next_cursor, "total": total})


def _strip_result(result_json):
    result_json.copy()

    for entry in result_json["result"]:
        assert set(entry.keys()) == {
            "time",
            "issuer",
            "type",
            "status",
            "status_time",
            "host_inv",
            "host_name",
            "host_uuid",
        }

    result_json["result"] = [{"time": entry["time"], "type": entry["type"]} for entry in result_json["result"]]
    return result_json
