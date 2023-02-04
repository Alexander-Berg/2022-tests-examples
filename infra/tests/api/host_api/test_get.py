"""Tests host get/list API."""

import uuid
from functools import partial

import pytest
import http.client

import walle.constants as wc
import walle.expert.automation_plot
from infra.walle.server.tests.lib.util import TestCase, mock_location, mock_host_health_status, mock_uuid_for_inv
from sepelib.core.exceptions import LogicalError
from walle import restrictions, authorization
from walle.clients import staff, racktables as rt
from walle.errors import BadRequestError
from walle.expert.types import CheckType, CheckStatus
from walle.hosts import HostState, HostStatus, HostLocation, HealthStatus, InfinibandInfo, parse_physical_location_query
from walle.operations_log.constants import Operation
from walle.util.deploy_config import DeployConfigPolicies
from walle.util.host_health import get_failure_reason


@pytest.mark.parametrize("status", HostStatus.ALL)
@pytest.mark.parametrize("status_author", [authorization.ISSUER_WALLE, TestCase.api_issuer])
def test_get_host(walle_test, iterate_authentication, status, status_author):
    host0 = walle_test.mock_host(
        {
            "inv": 0,
            "name": "first",
            "uuid": "f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0",
            "status": status,
            "status_author": status_author,
            "status_reason": "reason-mock",
        }
    )
    host1 = walle_test.mock_host({"inv": 1, "uuid": "e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1e1", "name": "second"})

    for test_host in (host0, host1):
        for host_identifier in ("inv", "uuid", "name"):
            result = walle_test.api_client.get("/v1/hosts/{}".format(getattr(test_host, host_identifier)))
            assert result.status_code == http.client.OK
            assert result.json == test_host.to_api_obj()

    walle_test.hosts.assert_equal()


def test_get_host_by_different_uuid_representations(walle_test, iterate_authentication):
    test_host = walle_test.mock_host()

    uuid_flat = test_host.uuid
    uuid_dashed = uuid.UUID(uuid_flat)
    for test_uuid in (uuid_flat, uuid_dashed):
        result = walle_test.api_client.get("/v1/hosts/{}".format(test_uuid))
        assert result.status_code == http.client.OK
        assert result.json == test_host.to_api_obj()

    walle_test.hosts.assert_equal()


def test_get_missing_host(walle_test):
    walle_test.mock_host({"inv": 0, "name": "test0"})

    result = walle_test.api_client.get("/v1/hosts/1")
    assert result.status_code == http.client.NOT_FOUND
    walle_test.hosts.assert_equal()

    result = walle_test.api_client.get("/v1/hosts/missing")
    assert result.status_code == http.client.NOT_FOUND
    walle_test.hosts.assert_equal()

    result = walle_test.api_client.get("/v1/hosts/99999999999999999999999999999999")
    assert result.status_code == http.client.NOT_FOUND
    walle_test.hosts.assert_equal()


class TestGetWithFields:
    @pytest.fixture
    def sample_host(self, walle_test):
        return walle_test.mock_host(
            {
                "inv": 0,
                "name": "zero",
                "config": "base",
                "status": HostStatus.READY,
                "status_reason": "reason-mock",
                "health": mock_host_health_status(),
            }
        )

    @pytest.mark.usefixtures("sample_host")
    @pytest.mark.parametrize("field_list", ["name", ["name"]])
    def test_one_field_may_be_in_different_forms(self, walle_test, field_list):
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": field_list})
        assert http.client.OK == result.status_code
        assert {"result": [{"name": "zero", "inv": 0}], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    def test_request_for_no_fields_return_empty_objects(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": ""})
        assert http.client.OK == result.status_code
        assert {"result": [{}], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    @pytest.mark.parametrize("field_list", [None, []])
    def test_request_without_fields_return_default_fields(self, walle_test, field_list):
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": field_list})
        assert http.client.OK == result.status_code
        expected_host_json = {
            "uuid": mock_uuid_for_inv(0),
            "inv": 0,
            "name": "zero",
            "state": HostState.FREE,
            "status": HostStatus.READY,
            "status_author": walle_test.api_issuer,
            "status_reason": "reason-mock",
            "config": "base",
        }
        assert {"result": [expected_host_json], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    @pytest.mark.parametrize("field_list", ["status,config,status_reason", ["status", "config", "status_reason"]])
    def test_request_multiple_fields_return_all_required_fields_for_present_fields(self, walle_test, field_list):
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": field_list})
        assert http.client.OK == result.status_code

        expected_host_json = {"status": HostStatus.READY, "config": "base", "status_reason": "reason-mock", "inv": 0}
        assert {"result": [expected_host_json], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    @pytest.mark.parametrize("field_list", ["status,location.switch", ["status", "location.switch"]])
    def test_request_for_fields_with_dots_return_requested_nested_field(self, walle_test, field_list):
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": field_list})
        assert http.client.OK == result.status_code

        expected_host_json = {"status": HostStatus.READY, "location": {"switch": "switch-mock"}, "inv": 0}
        assert {"result": [expected_host_json], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    @pytest.mark.parametrize("field_list", ["status,location.meh", "status,meh.location", "status,meh"])
    def test_request_for_absent_fields_return_document_without_missing_fields(self, walle_test, field_list):
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": field_list})
        assert http.client.OK == result.status_code

        expected_host_json = {"status": HostStatus.READY, "inv": 0}
        assert {"result": [expected_host_json], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    def test_request_for_forbidden_fields_skips_forbidden_fields(self, walle_test):
        # forbidden fields
        result = walle_test.api_client.get("/v1/hosts", query_string={"fields": ["status", "active_check_min_time"]})
        assert http.client.OK == result.status_code
        assert {"result": [{"status": HostStatus.READY, "inv": 0}], "total": 1} == result.json

    @pytest.mark.usefixtures("sample_host")
    def test_request_for_forbidden_inner_fields_does_not_skip_allowed_inner_fields(self, walle_test):
        # forbidden fields
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={"fields": ["status", "health.status", "health.expert_info"]}
        )
        assert http.client.OK == result.status_code
        assert {
            "result": [{"status": HostStatus.READY, "health": {"status": "ok"}, "inv": 0}],
            "total": 1,
        } == result.json


def test_return_empty_list_when_no_hosts_exist(walle_test):
    result = walle_test.api_client.get("/v1/hosts")
    assert result.status_code == http.client.OK
    assert result.json == {"result": [], "total": 0}


@pytest.mark.usefixtures("iterate_authentication")
class FilterHostsTestCase:
    @classmethod
    @pytest.fixture(autouse=True)
    def sample_hosts(cls, walle_test):
        project1 = walle_test.mock_project({"id": "project1"})
        project2 = walle_test.mock_project({"id": "project2"})
        project3 = walle_test.mock_project({"id": "project3"})

        walle_test.mock_host(
            {
                "inv": 0,
                "name": "zero",
                "project": project1.id,
                "state": HostState.ASSIGNED,
                "status": HostStatus.READY,
                "provisioner": walle_test.host_provisioner,
                "config": walle_test.host_deploy_config,
                "ticket": "BURNE-1001",
            }
        )
        walle_test.mock_host(
            {
                "inv": 1,
                "name": "first",
                "project": project2.id,
                "state": HostState.ASSIGNED,
                "status": HostStatus.DEAD,
                "provisioner": None,
                "config": None,
                "ticket": "BURNE-1002",
            }
        )
        walle_test.mock_host(
            {
                "inv": 2,
                "name": "second",
                "project": project3.id,
                "state": HostState.ASSIGNED,
                "status": HostStatus.DEAD,
                "provisioner": walle_test.project_provisioner,
                "config": walle_test.project_deploy_config,
            }
        )

        return walle_test.hosts.objects


class TestFilters(FilterHostsTestCase):
    def test_return_all_hosts_when_no_filter_specified(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts")
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts)

    def test_return_requested_count_of_hosts_when_limit_specified(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"limit": 2})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[:2], total=len(sample_hosts), cursor=0)

    def test_skip_first_hosts_when_big_offset_specified(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"offset": 1001, "limit": 1})
        assert result.status_code == http.client.BAD_REQUEST

    def test_start_with_specified_inv_when_cursor_specified(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"cursor": 1, "limit": 1})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[1:2], cursor=1, total=len(sample_hosts))

    def test_filters_hosts_by_host_deploy_config(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"config": walle_test.host_deploy_config})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[:1])

    def test_filters_hosts_by_project_deploy_config(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"config": walle_test.project_deploy_config})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[1:])

    def test_filters_host_by_names_substring_beginning_of_name(self, walle_test, sample_hosts):
        # name is substring
        result = walle_test.api_client.get("/v1/hosts", query_string={"name": "s"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[1], sample_hosts[2]])

    def test_filters_host_by_names_substring_middle_of_name(self, walle_test, sample_hosts):
        # name is substring
        result = walle_test.api_client.get("/v1/hosts", query_string={"name": "o"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0], sample_hosts[2]])

    def test_filters_hosts_by_name_with_pattern_anchored_at_beginning(self, walle_test, sample_hosts):
        # name is left-anchored pattern
        result = walle_test.api_client.get("/v1/hosts", query_string={"name": "s*"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[2]])

    def test_filters_hosts_by_name_with_pattern_anchored_at_end(self, walle_test, sample_hosts):
        # name is right-anchored pattern
        result = walle_test.api_client.get("/v1/hosts", query_string={"name": "*o"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0]])

    def test_filters_hosts_by_project_id(self, walle_test, sample_hosts):
        result = walle_test.api_client.get(
            "/v1/hosts",
            query_string={
                "project": [
                    # there is also a "default" project which is not used in this test
                    walle_test.projects.objects[1].id,
                    walle_test.projects.objects[3].id,
                ]
            },
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0], sample_hosts[2]])

    def test_filters_by_multiple_filters(self, walle_test, sample_hosts):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={"config": walle_test.project_deploy_config, "name": "i"}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[1]])

    def test_request_with_filter_by_non_existing_status_is_bad(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"status": "some-invalid-value"})
        assert result.status_code == http.client.BAD_REQUEST

    @pytest.mark.parametrize("filter_keyword", ["status", "status__in"])
    def test_filters_by_status_one_of(self, walle_test, sample_hosts, filter_keyword):
        result = walle_test.api_client.get("/v1/hosts", query_string={filter_keyword: HostStatus.DEAD})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[1:])

    def test_filters_by_status_not_one_of(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"status__nin": HostStatus.READY})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[1:])

    @pytest.mark.parametrize("filter_keyword", ["status", "status__in"])
    def test_filters_by_status_one_of_list(self, walle_test, sample_hosts, filter_keyword):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={filter_keyword: [HostStatus.DEAD, HostStatus.READY]}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts)

    def test_filters_by_status_not_one_of_list(self, walle_test):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={"status__nin": [HostStatus.DEAD, HostStatus.READY]}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [])

    def test_using_unknown_modifiers_is_bad_request(self, walle_test):
        # check for bad modifiers
        result = walle_test.api_client.get("/v1/hosts", query_string={"status__all": HostStatus.READY})
        assert result.status_code == http.client.BAD_REQUEST

    @pytest.mark.parametrize("filter_keyword", ["ticket", "ticket__in"])
    def test_filters_by_ticket_one_of(self, walle_test, sample_hosts, filter_keyword):
        result = walle_test.api_client.get("/v1/hosts", query_string={filter_keyword: "BURNE-1002"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[1]])

    def test_filters_by_ticket_not_one_of(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"ticket__nin": "BURNE-1001"})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[1:])

    @pytest.mark.parametrize("filter_keyword", ["ticket", "ticket__in"])
    def test_filters_by_ticket_one_of_list(self, walle_test, sample_hosts, filter_keyword):
        result = walle_test.api_client.get("/v1/hosts", query_string={filter_keyword: "BURNE-1001,BURNE-1002"})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[:2])

    def test_filters_by_ticket_not_one_of_list(self, walle_test, sample_hosts):
        result = walle_test.api_client.get("/v1/hosts", query_string={"ticket__nin": "BURNE-1001,BURNE-1003"})
        assert result.status_code == http.client.OK
        _assert_equal(result, sample_hosts[1:])

    def test_return_empty_list_when_filter_does_not_match_anything(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"ticket": "BURNE-1003"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [])


@pytest.mark.usefixtures("iterate_authentication")
@pytest.mark.parametrize("status", HostStatus.ALL_TASK)
@pytest.mark.parametrize("issuer", ["affe@", "robot-walle"])
def test_filters_by_task_owner(walle_test, status, issuer):
    walle_test.mock_host(
        {"inv": 100499, "name": "hostwedontcareabout", "status": status, "status_reason": "reason-mock"},
        task_kwargs={"owner": "somebody@"},
    )

    host = walle_test.mock_host(
        {"inv": 100500, "name": "affehost", "status": status, "status_reason": "reason-mock"},
        task_kwargs={"owner": issuer},
    )

    result = walle_test.api_client.get("/v1/hosts", query_string={"task_owner": issuer})
    assert result.status_code == http.client.OK
    _assert_equal(result, [host])


class TestQuery(FilterHostsTestCase):
    def test_empty_query_returns_empty_list(self, walle_test):
        result = walle_test.api_client.post("/v1/get-hosts", data={})
        assert result.status_code == http.client.OK
        _assert_equal(result, [])

    def test_query_with_empty_criteria_returns_empty_list(self, walle_test):
        result = walle_test.api_client.post("/v1/get-hosts", data={"names": []})
        assert result.status_code == http.client.OK
        _assert_equal(result, [])

    def test_query_selects_requested_hosts_by_name(self, walle_test, sample_hosts):
        result = walle_test.api_client.post("/v1/get-hosts", data={"names": ["first", "second"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[1], sample_hosts[2]])

    def test_query_by_name_and_pattern_returns_matching_hosts_for_both__substring(self, walle_test, sample_hosts):
        # names and patterns 'or'-ed
        result = walle_test.api_client.post("/v1/get-hosts", data={"names": ["first"], "patterns": ["o"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0], sample_hosts[1], sample_hosts[2]])

    def test_query_by_name_and_pattern_returns_matching_hosts_for_both__anchored_at_end(self, walle_test, sample_hosts):
        result = walle_test.api_client.post("/v1/get-hosts", data={"names": ["first"], "patterns": ["*o"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0], sample_hosts[1]])

    def test_query_by_name_and_inventory_number_returns_matching_hosts_for_both(self, walle_test, sample_hosts):
        result = walle_test.api_client.post("/v1/get-hosts", data={"invs": [0], "names": ["second"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0], sample_hosts[2]])

    def test_query_by_uuid_single(self, walle_test, sample_hosts):
        result = walle_test.api_client.post("/v1/get-hosts", data={"uuids": [sample_hosts[0].uuid]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[0]])

    def test_query_by_uuid_single_list(self, walle_test, sample_hosts):
        result = walle_test.api_client.post("/v1/get-hosts", data={"uuids": [h.uuid for h in sample_hosts[:2]]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [h for h in sample_hosts[:2]])

    def test_queried_hosts_got_filtered_by_name_substring(self, walle_test, sample_hosts):
        result = walle_test.api_client.post(
            "/v1/get-hosts", data={"names": ["first", "second"]}, query_string={"name": "o"}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[2]])

    def test_queried_hosts_got_filtered_by_status(self, walle_test, sample_hosts):
        result = walle_test.api_client.post(
            "/v1/get-hosts", data={"names": ["zero", "first"]}, query_string={"status": HostStatus.DEAD}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[1]])

    def test_queried_hosts_got_filtered_by_deploy_config(self, walle_test, sample_hosts):
        query_string = {"config": walle_test.project_deploy_config}
        result = walle_test.api_client.post(
            "/v1/get-hosts", data={"invs": [0], "names": ["first"]}, query_string=query_string
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [sample_hosts[1]])

        walle_test.hosts.assert_equal()


class TestFilterByStatus:
    @classmethod
    @pytest.fixture(autouse=True)
    def setup_test(cls, walle_test):
        walle_test.mock_host({"inv": 0, "status": HostStatus.READY})
        walle_test.mock_host({"inv": 1, "status": HostStatus.MANUAL})
        walle_test.mock_host({"inv": 2, "status": HostStatus.DEAD})
        walle_test.mock_host({"inv": 3, "status": Operation.REBOOT.host_status}, task_kwargs={"error": "error-mock"})
        walle_test.mock_host({"inv": 4, "status": Operation.PROFILE.host_status})
        walle_test.mock_host({"inv": 5, "status": Operation.SWITCH_VLANS.host_status})
        walle_test.mock_host({"inv": 6, "status": Operation.DEACTIVATE.host_status})
        walle_test.mock_host({"inv": 7, "status": Operation.CHECK_DNS.host_status}, task_kwargs={"error": "error-mock"})

    @pytest.fixture(params=["GET", "POST"])
    def request_method(self, request, walle_test):
        method = request.param
        if method == "GET":
            request_path = "/v1/hosts"
            data = None
        elif method == "POST":
            request_path = "/v1/get-hosts"
            data = {"names": [host.name for host in walle_test.hosts.objects]}
        else:
            raise LogicalError

        return partial(walle_test.api_client.open, request_path, method=method, data=data)

    def test_filter_by_single_status(self, walle_test, request_method):
        result = request_method(query_string={"status": Operation.REBOOT.host_status})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[3]])

    @pytest.mark.parametrize("keyword", ["status", "status__in"])
    def test_filter_by_list_of_statuses(self, walle_test, request_method, keyword):
        result = request_method(query_string={keyword: [HostStatus.READY, Operation.CHECK_DNS.host_status]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[0], walle_test.hosts.objects[7]])

    def test_filter_by_list_by_exclusion(self, walle_test, request_method):
        result = request_method(query_string={"status__nin": [HostStatus.READY, HostStatus.MANUAL]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [2, 3, 4, 5, 6, 7]])

    @pytest.mark.parametrize("keyword", ["status", "status__in"])
    def test_filter_no_matching_for_part_of_statuses(self, walle_test, request_method, keyword):
        result = request_method(query_string={keyword: [HostStatus.DEAD, Operation.DELETE.host_status]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[2]])

    @pytest.mark.parametrize("keyword", ["status", "status__in"])
    def test_filter_no_matching_hosts_by_inclusion(self, request_method, keyword):
        result = request_method(
            query_string={keyword: [Operation.SWITCH_PROJECT.host_status, Operation.DELETE.host_status]}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [])

    def test_filter_no_matching_hosts_by_exclusion(self, request_method):
        result = request_method(
            query_string={
                "status__nin": [
                    HostStatus.READY,
                    HostStatus.MANUAL,
                    HostStatus.DEAD,
                    Operation.REBOOT.host_status,
                    Operation.PROFILE.host_status,
                    Operation.SWITCH_VLANS.host_status,
                    Operation.DEACTIVATE.host_status,
                    Operation.CHECK_DNS.host_status,
                ]
            }
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [])

    def test_status_filter_steady(self, walle_test, request_method):
        result = request_method(query_string={"status": HostStatus.FILTER_STEADY})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [0, 1, 2]])

    def test_status_filter_steady_with_additional_status_list(self, walle_test, request_method):
        result = request_method(query_string={"status": [HostStatus.FILTER_STEADY, Operation.REBOOT.host_status]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [0, 1, 2, 3]])

    def test_status_filter_task(self, walle_test, request_method):
        result = request_method(query_string={"status": HostStatus.FILTER_TASK})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [3, 4, 5, 6, 7]])

    def test_status_filter_task_with_additional_status_list(self, walle_test, request_method):
        result = request_method(query_string={"status": [HostStatus.FILTER_TASK, HostStatus.READY]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [0, 3, 4, 5, 6, 7]])

    def test_status_filter_error(self, walle_test, request_method):
        result = request_method(query_string={"status": HostStatus.FILTER_ERROR})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [3, 7]])

    def test_status_filter_error_with_additional_status_list(self, walle_test, request_method):
        result = request_method(query_string={"status": [HostStatus.FILTER_ERROR, HostStatus.READY]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in [0, 3, 7]])


class TestFilteringByHealthStatus:
    @classmethod
    @pytest.fixture(autouse=True)
    def setup_test(cls, walle_test):
        walle_test.mock_host({"inv": 0})
        walle_test.mock_host({"inv": 1, "health": mock_host_health_status(status=HealthStatus.STATUS_OK)})
        walle_test.mock_host(
            {
                "inv": 2,
                "health": mock_host_health_status(
                    status=HealthStatus.STATUS_FAILURE,
                    human_reasons=[get_failure_reason(CheckType.UNREACHABLE, CheckStatus.FAILED)],
                ),
            }
        )
        walle_test.mock_host(
            {
                "inv": 3,
                "health": mock_host_health_status(
                    status=HealthStatus.STATUS_FAILURE,
                    human_reasons=[get_failure_reason(CheckType.SSH, CheckStatus.SUSPECTED)],
                ),
            }
        )
        walle_test.mock_host(
            {
                "inv": 4,
                "health": mock_host_health_status(
                    status=HealthStatus.STATUS_FAILURE,
                    human_reasons=[get_failure_reason(CheckType.SSH, CheckStatus.FAILED)],
                ),
            }
        )
        walle_test.mock_host(
            {
                "inv": 5,
                "health": mock_host_health_status(
                    status=HealthStatus.STATUS_FAILURE,
                    human_reasons=[
                        get_failure_reason(CheckType.SSH, CheckStatus.FAILED),
                        get_failure_reason(CheckType.UNREACHABLE, CheckStatus.FAILED),
                    ],
                ),
            }
        )

    def test_no_filter(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts")
        assert result.status_code == http.client.OK
        _assert_equal(result, walle_test.hosts.objects)

    def test_filter_by_missing_health(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"health": "no"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[0]])

    def test_filter_by_ok_health_status(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"health": "ok"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[1]])

    def test_filter_by_failure_health_status(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"health": "failure"})
        assert result.status_code == http.client.OK
        _assert_equal(result, walle_test.hosts.objects[2:])

    def test_filter_by_failing_check(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"health": "ssh"})
        assert result.status_code == http.client.OK
        _assert_equal(result, walle_test.hosts.objects[3:])

    @pytest.mark.parametrize("keyword", ["health", "health__in"])
    def test_filter_by_many_failing_checks(self, walle_test, keyword):
        result = walle_test.api_client.get("/v1/hosts", query_string={keyword: ["ssh", "unreachable"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, walle_test.hosts.objects[2:])

    def test_filter_by_many_failing_checks_by_exclusion(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"health__nin": "ssh"})
        assert result.status_code == http.client.OK
        _assert_equal(result, walle_test.hosts.objects[:3])

    @pytest.mark.parametrize("keyword", ["health", "health__in"])
    def test_filter_by_status_reason(self, walle_test, keyword):
        result = walle_test.api_client.get("/v1/hosts", query_string={keyword: "ssh.suspected"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[3]])

    def test_filter_by_status_reason_by_exclusion(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"health__nin": "ssh.suspected"})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[i] for i in set(range(6)) - {3}])

    @pytest.mark.parametrize("inclusion_keyword", ["health", "health__in"])
    def test_filter_by_status_by_both_inclusion_and_exclusion(self, walle_test, inclusion_keyword):
        query_string = {inclusion_keyword: ["ssh"], "health__nin": ["unreachable"]}
        result = walle_test.api_client.get("/v1/hosts", query_string=query_string)
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[3], walle_test.hosts.objects[4]])


class TestFilteringByHealthStatusWithAutomationPlot:
    @classmethod
    @pytest.fixture(autouse=True)
    def mock_automation_plot(cls, mp, walle_test):
        walle_test.automation_plot.mock(
            {
                "checks": [
                    {
                        "name": "automation_plot_check_mock",
                        "enabled": True,
                        "reboot": True,
                        "redeploy": True,
                    }
                ]
            }
        )
        # explicitly patch walle.expert.automation_plot.get_all_automation_plots_checks to avoid cached side-effects
        mp.function(
            walle.expert.automation_plot.get_all_automation_plots_checks, return_value={"automation_plot_check_mock"}
        )

    @classmethod
    @pytest.fixture
    def mock_failed_automation_plot_host(cls, walle_test):
        walle_test.mock_host(
            {
                "inv": 100,
                "health": mock_host_health_status(
                    status=HealthStatus.STATUS_FAILURE,
                    human_reasons=[get_failure_reason("automation_plot_check_mock", CheckStatus.FAILED)],
                ),
            }
        )

    @pytest.mark.parametrize("inclusion_keyword", ["health", "health__in", "health__nin"])
    def test_non_existing_check_returns_bad_request(self, walle_test, inclusion_keyword):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={inclusion_keyword: ["non_existing_valid_check_name"]}
        )
        assert result.status_code == http.client.BAD_REQUEST
        assert result.json['message'] == "Health args: non_existing_valid_check_name are not supported"

    @pytest.mark.parametrize("inclusion_keyword", ["health", "health__in", "health__nin"])
    def test_existing_check_returns_no_hosts(self, walle_test, inclusion_keyword):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={inclusion_keyword: ["automation_plot_check_mock"]}
        )
        assert result.status_code == http.client.OK
        assert result.json['total'] == 0

    @pytest.mark.parametrize("inclusion_keyword", ["health", "health__in"])
    def test_filter_by_automation_plot_check_status_by_inclusion(
        self, walle_test, mock_failed_automation_plot_host, inclusion_keyword
    ):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={inclusion_keyword: ["automation_plot_check_mock"]}
        )
        assert result.status_code == http.client.OK
        assert result.json['total'] == 1


class TestFilterByProjectsTags:
    @classmethod
    @pytest.fixture(autouse=True)
    def setup_test(cls, walle_test):
        walle_test.mock_project({"id": "walle-test-yt", "name": "Test YT", "tags": ["walle_test", "yt"]})
        walle_test.mock_host({"inv": 1, "project": "walle-test-yt"})

        walle_test.mock_project({"id": "walle-test-rtc", "name": "Test RTC", "tags": ["walle_test", "rtc"]})
        walle_test.mock_host({"inv": 2, "project": "walle-test-rtc"})

        walle_test.mock_project({"id": "walle-test-market", "name": "Test Market"})
        walle_test.mock_host({"inv": 3, "project": "walle-test-market"})

    def test_find_hosts_from_all_projects_with_common_tag(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"tags": ["walle_test"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, walle_test.hosts.objects[:2])

    def test_find_hosts_project_with_unique_tag(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"tags": ["yt"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[0]])

    def test_find_hosts_by_project_and_not_matching_tags(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"project": "walle-test-market", "tags": ["yt"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [])

    def test_find_hosts_by_project_and_matching_tags(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"project": "walle-test-yt", "tags": ["yt"]})
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[0]])


class TestSort:
    @classmethod
    @pytest.fixture(autouse=True)
    def setup_test(cls, walle_test):
        walle_test.mock_project({"id": "project1", "name": "Project 1"})
        walle_test.mock_host(
            {
                "inv": 1,
                "project": "project1",
                "location": HostLocation(switch="C", country="RU", city="MYT", datacenter="MPLMASS"),
            }
        )

        walle_test.mock_project({"id": "project2", "name": "Project 2"})
        walle_test.mock_host(
            {
                "inv": 2,
                "project": "project2",
                "location": HostLocation(switch="B", country="FI", city="MANTSALA", datacenter="B"),
            }
        )

        walle_test.mock_project({"id": "project3", "name": "Project 3"})
        walle_test.mock_host(
            {
                "inv": 3,
                "project": "project3",
                "location": HostLocation(switch="A", country="RU", city="IVA", datacenter="IVNIT"),
            }
        )

    def test_with_one_field(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "switch"})
        assert result.status_code == http.client.OK
        _assert_equal(
            result, [walle_test.hosts.objects[2], walle_test.hosts.objects[1], walle_test.hosts.objects[0]], sort=False
        )

    def test_with_one_field_asc(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "switch:asc"})
        assert result.status_code == http.client.OK
        _assert_equal(
            result, [walle_test.hosts.objects[2], walle_test.hosts.objects[1], walle_test.hosts.objects[0]], sort=False
        )

    def test_with_one_field_desc(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "switch:desc"})
        assert result.status_code == http.client.OK
        _assert_equal(
            result, [walle_test.hosts.objects[0], walle_test.hosts.objects[1], walle_test.hosts.objects[2]], sort=False
        )

    def test_with_filter_asc(self, walle_test):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={"physical_location": "RU", "sort-by": "project:asc"}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[0], walle_test.hosts.objects[2]], sort=False)

    def test_with_filter_desc(self, walle_test):
        result = walle_test.api_client.get(
            "/v1/hosts", query_string={"physical_location": "RU", "sort-by": "project:desc"}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[2], walle_test.hosts.objects[0]], sort=False)

    def test_with_datacenter_asc(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "datacenter:asc"})
        assert result.status_code == http.client.OK
        _assert_equal(
            result, [walle_test.hosts.objects[1], walle_test.hosts.objects[2], walle_test.hosts.objects[0]], sort=False
        )

    def test_with_datacenter_desc(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "datacenter:desc"})
        assert result.status_code == http.client.OK
        _assert_equal(
            result, [walle_test.hosts.objects[0], walle_test.hosts.objects[2], walle_test.hosts.objects[1]], sort=False
        )

    def test_combine(self, walle_test):
        walle_test.mock_host(
            {
                "inv": 4,
                "project": "project3",
                "location": HostLocation(switch="Z", country="RU", city="IVA", datacenter="IVNIT"),
            }
        )

        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "datacenter:asc,switch:desc"})
        assert result.status_code == http.client.OK
        _assert_equal(
            result,
            [
                walle_test.hosts.objects[1],
                walle_test.hosts.objects[3],
                walle_test.hosts.objects[2],
                walle_test.hosts.objects[0],
            ],
            sort=False,
        )

    def test_with_cursor(self, walle_test):
        result = walle_test.api_client.get("/v1/hosts", query_string={"sort-by": "project", "cursor": 1})
        assert result.status_code == http.client.BAD_REQUEST

    def test_get_hosts(self, walle_test):
        result = walle_test.api_client.post(
            "/v1/get-hosts", query_string={"sort-by": "project:desc"}, data={"invs": [1, 3]}
        )
        assert result.status_code == http.client.OK
        _assert_equal(result, [walle_test.hosts.objects[2], walle_test.hosts.objects[0]], sort=False)


def test_filtering_by_scenario_id(walle_test):
    walle_test.mock_host({"inv": 0, "scenario_id": 1})
    walle_test.mock_host({"inv": 1, "scenario_id": 1})
    walle_test.mock_host({"inv": 2, "scenario_id": 2}),
    walle_test.mock_host({"inv": 3, "scenario_id": None})

    result = walle_test.api_client.get("/v1/hosts?fields=inv,scenario_id")
    assert result.status_code == http.client.OK
    _assert_equal_invs(result, walle_test.hosts.objects)

    result = walle_test.api_client.get("/v1/hosts", query_string={"scenario_id": [2]})
    assert result.status_code == http.client.OK
    _assert_equal_invs(result, [walle_test.hosts.objects[2]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"scenario_id": [1]})
    assert result.status_code == http.client.OK
    _assert_equal_invs(result, walle_test.hosts.objects[0:2])

    result = walle_test.api_client.get("/v1/hosts", query_string={"scenario_id": None})
    assert result.status_code == http.client.OK
    _assert_equal_invs(result, walle_test.hosts.objects)


def test_filtering_by_restrictions(walle_test):
    walle_test.mock_host({"inv": 0, "restrictions": None})
    walle_test.mock_host({"inv": 1, "restrictions": [restrictions.AUTOMATED_REBOOT]})
    walle_test.mock_host({"inv": 2, "restrictions": [restrictions.AUTOMATED_REPAIRING]})

    result = walle_test.api_client.get("/v1/hosts")
    assert result.status_code == http.client.OK
    _assert_equal(result, walle_test.hosts.objects)

    result = walle_test.api_client.get("/v1/hosts", query_string={"restrictions": ""})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[0]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"restrictions": restrictions.REBOOT})
    assert result.status_code == http.client.OK
    _assert_equal(result, [])

    result = walle_test.api_client.get("/v1/hosts", query_string={"restrictions": restrictions.AUTOMATED_REBOOT})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[1]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"restrictions": restrictions.AUTOMATED_REDEPLOY})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[1]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"restrictions": restrictions.AUTOMATED_DISK_CHANGE})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[2]])

    walle_test.hosts.assert_equal()


def test_filtering_by_location(walle_test):
    walle_test.mock_host({"inv": 0, "location": HostLocation()})
    walle_test.mock_host({"inv": 1, "location": HostLocation(country="COUNTRY")})
    walle_test.mock_host({"inv": 2, "location": HostLocation(country="COUNTRY", city="CITY")})

    result = walle_test.api_client.get("/v1/hosts", query_string={"physical_location": ""})
    assert result.status_code == http.client.OK
    _assert_equal(result, [])

    result = walle_test.api_client.get("/v1/hosts", query_string={"physical_location": "FOO|BAR"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [])

    result = walle_test.api_client.get("/v1/hosts", query_string={"physical_location": "COUNTRY"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[1], walle_test.hosts.objects[2]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"physical_location": "COUNTRY|CITY"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[2]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"physical_location": "COUNTRY|CITY|DATACENTER"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [])

    result = walle_test.api_client.get("/v1/hosts", query_string={"physical_location": "1|2|3|4|5|6"})
    assert result.status_code == http.client.BAD_REQUEST


def test_filtering_by_switch_port(walle_test):
    walle_test.mock_host({"inv": 0, "location": HostLocation()})
    walle_test.mock_host({"inv": 1, "location": HostLocation(switch="SWITCH")})
    walle_test.mock_host({"inv": 2, "location": HostLocation(switch="SWITCH", port="PORT")})

    result = walle_test.api_client.get("/v1/hosts", query_string={"switch": ""})
    assert result.status_code == http.client.OK
    _assert_equal(result, [])

    result = walle_test.api_client.get("/v1/hosts", query_string={"switch": "SWITCH"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[1], walle_test.hosts.objects[2]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"switch": "SWITCH", "port": "PORT"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[2]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"port": "PORT"})
    assert result.status_code == http.client.OK
    _assert_equal(result, [walle_test.hosts.objects[2]])

    result = walle_test.api_client.get("/v1/hosts", query_string={"port": ""})
    assert result.status_code == http.client.OK
    _assert_equal(result, [])


def test_owners_postprocessor(walle_test, mp):
    project1 = walle_test.mock_project(dict(id="project1", owners=["user11", "user12", "@group1"]))
    project2 = walle_test.mock_project(dict(id="project2", owners=["user21", "user22", "@group2"]))

    resolved_owners = {
        "user11": ["user11"],
        "user12": ["user12"],
        "@group1": ["group-member1", "group-member2"],
        "user21": ["user21"],
        "user22": ["user22"],
        "@group2": ["group-member2", "group-member3"],
        wc.ROBOT_WALLE_OWNER: [wc.ROBOT_WALLE_OWNER],
    }

    def resolve_owners_mock(owners):
        return sorted(sum((resolved_owners[owner] for owner in owners), []))

    mp.function(staff.resolve_owners, side_effect=resolve_owners_mock)

    walle_test.mock_host(dict(inv=0, project=project1.id))
    walle_test.mock_host(dict(inv=1, project=project2.id))
    walle_test.mock_host(dict(inv=2, project=project1.id))

    hosts = walle_test.api_client.get(
        "/v1/hosts", query_string={"resolve_owners": True, "fields": ["inv", "owners"]}
    ).json["result"]

    expected_resolved_owners_project_1 = ["group-member1", "group-member2", "user11", "user12", wc.ROBOT_WALLE_OWNER]
    expected_resolved_owners_project_2 = ["group-member2", "group-member3", "user21", "user22", wc.ROBOT_WALLE_OWNER]

    assert hosts == [
        {"inv": 0, "owners": expected_resolved_owners_project_1},
        {"inv": 1, "owners": expected_resolved_owners_project_2},
        {"inv": 2, "owners": expected_resolved_owners_project_1},
    ]


def test_tags_postprocessor(walle_test):
    project1 = walle_test.mock_project(dict(id="project1", tags=["rtc", "yasm"]))
    project2 = walle_test.mock_project(dict(id="project2", tags=["mtn", "yasm"]))

    walle_test.mock_host(dict(inv=0, project=project1.id))
    walle_test.mock_host(dict(inv=1, project=project2.id))
    walle_test.mock_host(dict(inv=2, project=project1.id))

    hosts = walle_test.api_client.get("/v1/hosts", query_string={"resolve_tags": True, "fields": ["inv", "tags"]}).json[
        "result"
    ]

    assert hosts == [
        {"inv": 0, "tags": project1.tags},
        {"inv": 1, "tags": project2.tags},
        {"inv": 2, "tags": project1.tags},
    ]


def test_deploy_configuration_postprocessor(walle_test):
    free_host = walle_test.mock_host({"inv": 0, "state": HostState.FREE})
    default_host = walle_test.mock_host({"inv": 1, "state": HostState.ASSIGNED, "provisioner": None, "config": None})
    custom_host = walle_test.mock_host(
        {
            "inv": 2,
            "state": HostState.ASSIGNED,
            "provisioner": walle_test.host_provisioner,
            "config": walle_test.host_deploy_config,
            "deploy_config_policy": DeployConfigPolicies.DISKMANAGER,
        }
    )

    fields = ["inv", "provisioner", "config", "deploy_config_policy", "custom_deploy_configuration"]

    expected_hosts = [
        {"inv": free_host.inv},
        {"inv": default_host.inv},
        {
            "inv": custom_host.inv,
            "provisioner": custom_host.provisioner,
            "config": custom_host.config,
            "deploy_config_policy": custom_host.deploy_config_policy,
        },
    ]
    for expected_host in expected_hosts:
        assert (
            walle_test.api_client.get("/v1/hosts/{}".format(expected_host["inv"]), query_string={"fields": fields}).json
            == expected_host
        )
    assert walle_test.api_client.get("/v1/hosts", query_string={"fields": fields}).json["result"] == expected_hosts

    expected_hosts = [
        {"inv": free_host.inv},
        {
            "inv": default_host.inv,
            "provisioner": walle_test.project_provisioner,
            "config": walle_test.project_deploy_config,
            "custom_deploy_configuration": False,
        },
        {
            "inv": custom_host.inv,
            "provisioner": custom_host.provisioner,
            "config": custom_host.config,
            "deploy_config_policy": custom_host.deploy_config_policy,
            "custom_deploy_configuration": True,
        },
    ]
    for expected_host in expected_hosts:
        assert (
            walle_test.api_client.get(
                "/v1/hosts/{}".format(expected_host["inv"]),
                query_string={"resolve_deploy_configuration": True, "fields": fields},
            ).json
            == expected_host
        )
    query_string = {"resolve_deploy_configuration": True, "fields": fields}
    assert walle_test.api_client.get("/v1/hosts", query_string=query_string).json["result"] == expected_hosts


@pytest.mark.parametrize("stand_name", ("wall-e", "walle.testing"))
@pytest.mark.parametrize("rack", ("r!ack", "r@ack", "r.ack"))
@pytest.mark.parametrize("short_queue_name", ("q!ueue", "q*ueue"))
def test_juggler_aggregate_name(walle_test, mp, stand_name, short_queue_name, rack):
    walle_test.mock_host(
        dict(
            inv=0,
            status_reason='walle_test',
            location=mock_location(short_queue_name=short_queue_name, rack=rack),
        )
    )
    mp.config("juggler.source", stand_name)
    result = walle_test.api_client.get("/v1/hosts?fields=juggler_aggregate_name")
    assert result.status_code == http.client.OK
    assert result.json["result"] == [{'juggler_aggregate_name': "{}-q.ueue-r.ack".format(stand_name), "inv": 0}]


def test_infiniband_info(walle_test):
    host = walle_test.mock_host()
    cluster_tag = "YATI2"
    ports = ["port1", "port2"]
    host.infiniband_info = InfinibandInfo(cluster_tag, ports)
    host.save()

    result = walle_test.api_client.get("/v1/hosts?fields=infiniband_info")
    assert result.status_code == http.client.OK
    infiniband_info_resp = result.json["result"][0]["infiniband_info"]
    assert infiniband_info_resp["cluster_tag"] == cluster_tag
    assert infiniband_info_resp["ports"] == ports


@pytest.mark.parametrize("rack", ("", None))
def test_juggler_aggregate_name_empty_rack(walle_test, mp, rack, mp_juggler_source):
    walle_test.mock_host(
        dict(
            inv=0,
            status_reason='walle_test',
            location=mock_location(short_queue_name="queue-mock", rack=rack),
        )
    )
    result = walle_test.api_client.get("/v1/hosts?fields=juggler_aggregate_name")
    assert result.status_code == http.client.OK
    assert result.json["result"] == [{'juggler_aggregate_name': "wall-e.unittest-queue-mock", "inv": 0}]


@pytest.mark.usefixtures("iterate_authentication")
def test_get_current_configuration_handles_nonexistent_hostname(mp, walle_test):
    mp.function(rt.get_port_vlan_status, return_value=([100, 200], 100, True))

    walle_test.mock_host({"inv": 0, "location": mock_location()})

    result = walle_test.api_client.get("/v1/hosts/invalid/current-configuration")
    assert result.status_code == http.client.NOT_FOUND


@pytest.mark.usefixtures("iterate_authentication")
def test_get_current_configuration(mp, walle_test):
    mp.function(rt.get_port_vlan_status, return_value=([100, 200], 100, True))
    mp.function(rt.get_port_project_status, return_value=("0x64", True))

    project = walle_test.mock_project({"id": "project1", "hbf_project_id": 200})
    walle_test.mock_host({"inv": 0, "location": mock_location(), "project": project.id})

    result = walle_test.api_client.get("/v1/hosts/0/current-configuration")
    assert result.status_code == http.client.OK
    assert result.json == {
        "vlans": [100, 200],
        "native_vlan": 100,
        "vlans_synced": True,
        "hbf_project_id": "0x64",
        "hbf_project_id_synced": True,
    }

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("iterate_authentication")
def test_get_current_configuration_state_assigned(mp, walle_test):
    mp.function(rt.get_port_vlan_status, return_value=([100, 200], 100, True))
    mp.function(rt.get_port_project_status, return_value=("0x64", True))

    project = walle_test.mock_project({"id": "project1", "hbf_project_id": 200})
    walle_test.mock_host({"inv": 0, "location": mock_location(), "project": project.id, "state": HostState.ASSIGNED})

    result = walle_test.api_client.get("/v1/hosts/0/current-configuration")
    assert result.status_code == http.client.OK
    assert result.json == {
        "vlans": [100, 200],
        "native_vlan": 100,
        "vlans_synced": True,
        "expected_native_vlan": 666,
        "expected_vlans": [666],
        "hbf_project_id": "0x64",
        "hbf_project_id_synced": True,
    }

    walle_test.hosts.assert_equal()


@pytest.mark.usefixtures("iterate_authentication")
def test_get_current_configuration_state_assigned_mtn(mp, walle_test):
    mp.function(rt.get_port_vlan_status, return_value=([100, 200], 100, True))
    mp.function(rt.get_port_project_status, return_value=("0x64", True))

    project = walle_test.mock_project({"id": "project1", "vlan_scheme": wc.VLAN_SCHEME_MTN, "hbf_project_id": 200})
    walle_test.mock_host({"inv": 0, "location": mock_location(), "project": project.id, "state": HostState.ASSIGNED})

    result = walle_test.api_client.get("/v1/hosts/0/current-configuration")
    assert result.status_code == http.client.OK
    expected_vlans = sorted([wc.MTN_NATIVE_VLAN, wc.MTN_FASTBONE_VLAN] + wc.MTN_EXTRA_VLANS)
    assert result.json == {
        "vlans": [100, 200],
        "native_vlan": 100,
        "vlans_synced": True,
        "expected_native_vlan": 666,
        "expected_vlans": expected_vlans,
        "expected_hbf_project_id": '0xc8',
        "hbf_project_id": "0x64",
        "hbf_project_id_synced": True,
    }

    walle_test.hosts.assert_equal()


@pytest.mark.parametrize("physical_locations", [["COUNTRY|CITY|DATACENTER|QUEUE|RACK"]])
def test_parse_physical_location_query_full(walle_test, physical_locations):
    actual = parse_physical_location_query(physical_locations)
    expected = {
        "$or": [
            {
                "$and": [
                    {"location.country": "COUNTRY"},
                    {"location.city": "CITY"},
                    {"location.datacenter": "DATACENTER"},
                    {"location.queue": "QUEUE"},
                    {"location.rack": "RACK"},
                ]
            },
        ]
    }
    assert actual == expected


@pytest.mark.parametrize("physical_locations", [["COUNTRY|CITY"]])
def test_parse_physical_location_query_short(walle_test, physical_locations):
    actual = parse_physical_location_query(physical_locations)
    expected = {
        "$or": [
            {
                "$and": [
                    {"location.country": "COUNTRY"},
                    {"location.city": "CITY"},
                ]
            },
        ]
    }
    assert actual == expected


@pytest.mark.parametrize("physical_locations", [["COUNTRY|CITY|DATACENTER|QUEUE|RACK|FOO"]])
def test_parse_physical_location_query_too_big(walle_test, physical_locations):
    with pytest.raises(BadRequestError):
        parse_physical_location_query(physical_locations)


def _assert_equal(result, objects, cursor=None, total=None, sort=True):
    response = {"result": [obj.to_api_obj() for obj in objects]}
    if sort:
        response["result"].sort(key=lambda obj: obj["inv"])

    if cursor is None or cursor == 0:
        response["total"] = len(objects) if total is None else total
    if cursor is not None:
        response["next_cursor"] = cursor + len(objects)  # assume no missing inventory numbers in test data
    assert result.json == response


def _assert_equal_invs(result, objects):
    from_api = {host['inv'] for host in result.json['result']}
    from_db = {object.inv for object in objects}
    assert from_api == from_db
