import json
import typing

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase
from walle import constants
from walle.expert import juggler
from walle.expert.types import CheckStatus
from walle.host_health import HealthCheck


class Check:
    def __init__(self, name, metadata, type="ssh"):
        self.name = name
        self.metadata = metadata
        self.type = type

    @classmethod
    def create_check_from_dict(cls, data):
        return cls(data['fqdn'], data['metadata'], data.get('type', "ssh"))

    def convert_metadata_from_string_to_dict(self):
        self.metadata = juggler.decode_metadata(self.metadata)

    def __eq__(self, other):
        return self.name == other.name and self.metadata == other.metadata and self.type == other.type


def save_test_health(checks: typing.List[Check]):
    for check in checks:
        HealthCheck(
            id=HealthCheck.mk_check_key(check.name, check.type),
            fqdn=check.name,
            type=check.type,
            status=CheckStatus.PASSED,
            status_mtime=100,
            timestamp=100,
            metadata=check.metadata,
        ).save()


@pytest.fixture
def test(request, mp, monkeypatch_timestamp, mp_juggler_source):
    test_case = TestCase.create(request, healthdb=True)

    return test_case


def test_get_none_checks(test):
    result = test.api_client.get("/v1/health-checks")
    assert result.status_code == http.client.OK
    assert result.json['total'] == 0


def test_get_all_checks_but_only_one_in_db(test):
    test_value = Check("mock1", "{'data': 'test', 'data2': '1'}")
    save_test_health([test_value])

    result = test.api_client.get("/v1/health-checks")
    assert result.status_code == http.client.OK
    assert result.json['total'] == 1

    val = Check.create_check_from_dict(result.json['result'][0])
    test_value.convert_metadata_from_string_to_dict()
    assert val == test_value


def test_get_all_checks_few_in_db(test):
    test_values = [Check("mock1", "{'data': 'test', 'data2': '1'}"), Check("mock2", "{'data': 'test2', 'data2': '2'}")]
    save_test_health(test_values)

    result = test.api_client.get("/v1/health-checks")
    assert result.status_code == http.client.OK
    assert result.json['total'] == 2

    for idx, record in enumerate(result.json['result']):
        val = Check.create_check_from_dict(record)
        test_values[idx].convert_metadata_from_string_to_dict()
        assert val == test_values[idx]


def test_get_checks_with_specified_host_name(test):
    test_values = [Check("mock1", "{'data': 'test', 'data2': '1'}"), Check("mock2", "{'data': 'test2', 'data2': '2'}")]
    save_test_health(test_values)

    result = test.api_client.get("/v1/health-checks")
    assert result.json['total'] == 2

    result = test.api_client.get("/v1/health-checks?fqdn=mock1")
    assert result.json['total'] == 1

    val = Check.create_check_from_dict(result.json['result'][0])
    test_values[0].convert_metadata_from_string_to_dict()
    assert val == test_values[0]


def test_check_with_metadata_as_string(test):
    test_value = Check("mock1", "string metadata")
    save_test_health([test_value])

    result = test.api_client.get("/v1/health-checks")
    assert result.status_code == http.client.OK
    assert result.json['total'] == 1

    val = Check.create_check_from_dict(result.json['result'][0])
    test_value.convert_metadata_from_string_to_dict()
    assert val == test_value


def test_get_checks_with_specified_type(test):
    walle_memory_check = Check(
        "mock1",
        json.dumps({'data': 'test', 'data2': '1', "result": {"timestamp": 10, "reason": ["foo"]}}),
        "walle_memory",
    )
    walle_disk_check = Check(
        "mock2",
        json.dumps({'data': 'test2', 'data2': '2', "result": {"timestamp": 10, "reason": ["foo"]}}),
        "walle_disk",
    )
    save_test_health([walle_disk_check, walle_memory_check])

    result = test.api_client.get("/v1/health-checks")
    assert result.json['total'] == 2

    result = test.api_client.get("/v1/health-checks?type=walle_disk")
    assert result.json['total'] == 1

    check_from_api = Check.create_check_from_dict(result.json['result'][0])
    walle_disk_check.convert_metadata_from_string_to_dict()
    assert check_from_api == walle_disk_check


@pytest.fixture()
def set_test_data(test):
    test_values = [Check("mock0", "data1"), Check("mock1", "data2"), Check("mock2", "data3")]
    save_test_health(test_values)

    for test_value in test_values:
        test_value.convert_metadata_from_string_to_dict()
    return test_values


def test_all_checks_in_db(test, set_test_data):
    test_values = set_test_data
    result = test.api_client.get("/v1/health-checks")
    assert result.json['total'] == len(test_values)
    for idx, val in enumerate(result.json['result']):
        transformed_check = Check.create_check_from_dict(val)
        assert transformed_check == test_values[idx]


def test_get_all_checks_with_offset_zero_and_limit_one(test, set_test_data):
    test_values = set_test_data
    result = test.api_client.get("/v1/health-checks", query_string={"offset": 0, "limit": 1})
    assert len(result.json['result']) == 1
    assert Check.create_check_from_dict(result.json['result'][0]) == test_values[0]


def test_get_all_checks_with_offset_zero_and_limit_two(test, set_test_data):
    test_values = set_test_data
    result = test.api_client.get("/v1/health-checks", query_string={"offset": 0, "limit": 2})
    assert len(result.json['result']) == 2

    for idx, val in enumerate(result.json['result']):
        transformed_check = Check.create_check_from_dict(val)
        assert transformed_check == test_values[idx]


def test_get_all_checks_with_offset_zero_and_limit_three(test, set_test_data):
    test_values = set_test_data
    result = test.api_client.get("/v1/health-checks", query_string={"offset": 0, "limit": 4})
    assert len(result.json['result']) == 3

    for idx, val in enumerate(result.json['result']):
        transformed_check = Check.create_check_from_dict(val)
        assert transformed_check == test_values[idx]


def test_get_all_checks_with_offset_one_and_limit_one(test, set_test_data):
    test_values = set_test_data
    result = test.api_client.get("/v1/health-checks", query_string={"offset": 1, "limit": 1})
    assert len(result.json['result']) == 1
    assert Check.create_check_from_dict(result.json['result'][0]) == test_values[1]


@pytest.fixture()
def set_inverted_test_data(test):
    test_values = [Check("mock2", "data3"), Check("mock1", "data2"), Check("mock0", "data1")]

    save_test_health(test_values)
    for test_value in test_values:
        test_value.convert_metadata_from_string_to_dict()
    return test_values


def test_all_checks_with_inverted_test_data_in_db(test, set_inverted_test_data):
    test_values = set_inverted_test_data[::-1]
    result = test.api_client.get("/v1/health-checks")
    assert result.json['total'] == len(test_values)
    for idx, val in enumerate(result.json['result']):
        transformed_check = Check.create_check_from_dict(val)
        assert transformed_check == test_values[idx]


def test_get_all_checks_with_cursor_and_limit_and_inverted_data(test, set_inverted_test_data):
    test_values = set_inverted_test_data[::-1]
    result = test.api_client.get("/v1/health-checks", query_string={"offset": 1, "limit": 2})
    assert len(result.json['result']) == 2
    for idx, val in enumerate(result.json['result'], start=1):
        transformed_check = Check.create_check_from_dict(val)
        assert transformed_check == test_values[idx]


STATUS_PASSED = "OK"


def test_scheme_validation_is_true(test):
    data = {"status": STATUS_PASSED, "metadata": {"reason": "some reason"}, "check_type": "walle_memory"}
    result = test.api_client.post("v1/scheme-validators", data=data)
    assert result.json['result']
    assert result.json['message'] == constants.SUCCESS_MESSAGE


def test_scheme_validation_id_false_by_invalid_check(test):
    metadata = {"wrong metadata": "aaa"}
    data = {"status": STATUS_PASSED, "metadata": metadata, "check_type": "walle_memory"}
    result = test.api_client.post("v1/scheme-validators", data=data)
    assert not result.json['result']
    assert result.json['message'] == (
        ": check walle_memory metadata validation error ({}): "
        "data must be valid exactly by one definition (0 matches found)".format(metadata)
    )


def test_scheme_validation_is_false_by_key_error(test, monkeypatch):
    data = {"status": STATUS_PASSED, "metadata": {"some key": "some value"}, "check_type": "walle_cpu_capping"}
    monkeypatch.setattr(CheckStatus, "ALL_WITHOUT_METADATA", {"passed"})
    result = test.api_client.post("v1/scheme-validators", data=data)
    assert result.json['message'] == "No schema for given status and type."
