import pytest
from datetime import datetime
from nose.tools import assert_true, assert_false
import maps.analyzer.services.jams_analyzer.tools.jams_uploader.lib.common as common


def set_coordination_data(this_data, associates_data):
    def mock_get_coordination_data(hostname, associates, vhost, data_section):
        return this_data[data_section] if this_data else None, \
            [x[data_section] for x in associates_data]
    common.get_coordination_data = mock_get_coordination_data


hostname = "host1"
associates = ["host2", "host3"]
vhost = "vhost"
data_section = "data"


def test_local_failure():
    set_coordination_data(None, [])
    assert_false(common.pretty_good(hostname, associates, vhost, data_section))


def test_local_good():
    set_coordination_data(
        {"data": {"size": 100000, "last_signal_time": 1518536874}},
        []
        )
    assert_true(common.pretty_good(hostname, associates, vhost, data_section))


def test_local_trash():
    set_coordination_data(
        {"data": {"size123": 100000, "last_signal_time": 1518536874}},
        []
        )
    assert_true(common.pretty_good(hostname, associates, vhost, data_section))


@pytest.fixture()
def ts(request):
    return int((datetime.utcnow()-datetime(1970, 1, 1)).total_seconds())


def test_local_expired(ts):
    set_coordination_data(
        {"data": {"size": 100000, "last_signal_time": ts - 300}},
        [
            {"data": {"size": 10000, "last_signal_time": ts - 1}},
            {"data": {"size": 10000, "last_signal_time": ts - 1000}}
        ])
    assert_false(common.pretty_good(hostname, associates, vhost, data_section))


def test_all_expired_but_pretty_good(ts):
    set_coordination_data(
        {"data": {"size": 100000, "last_signal_time": ts - 300}},
        [
            {"data": {"size": 10000, "last_signal_time": ts - 200}},
            {"data": {"size": 100000, "last_signal_time": ts - 1000}}
        ])
    assert_true(common.pretty_good(hostname, associates, vhost, data_section))


def test_worst_but_not_expired(ts):
    set_coordination_data(
        {"data": {"size": 10000, "last_signal_time": ts - 1}},
        [
            {"data": {"size": 100000, "last_signal_time": ts - 200}},
            {"data": {"size": 100000, "last_signal_time": ts - 1000}}
        ])
    assert_true(common.pretty_good(hostname, associates, vhost, data_section))


def test_pretty_good(ts):
    set_coordination_data(
        {"data": {"size": 100000, "last_signal_time": ts - 1}},
        [
            {"data": {"size": 10000, "last_signal_time": ts - 1}},
            {"data": {"size": 105000, "last_signal_time": ts - 1}}
        ])
    assert_true(common.pretty_good(hostname, associates, vhost, data_section))


def test_not_so_good(ts):
    set_coordination_data(
        {"data": {"size": 100000, "last_signal_time": ts - 1}},
        [
            {"data": {"size": 10000, "last_signal_time": ts - 1}},
            {"data": {"size": 205000, "last_signal_time": ts - 1}}
        ])
    assert_false(common.pretty_good(hostname, associates, vhost, data_section))
