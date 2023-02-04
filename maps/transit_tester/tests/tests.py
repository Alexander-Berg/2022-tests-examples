import json
import pytest
from unittest import mock


from maps.garden.sdk.core import DataValidationWarning, Version
from maps.garden.sdk import test_utils
from maps.garden.modules.transit_indexer import defs
from maps.garden.modules.transit_indexer.lib import transit_indexer
from maps.garden.modules.transit_tester.lib import transit_tester
from maps.garden.modules.transit_tester.lib import transit_testing_task
from maps.libs.config.py.config import read_config_file
from yandex.maps.proto.common2 import geo_object_pb2
from yandex.maps.proto.search import search_pb2, transit_pb2


FAKE_RESOURCE = None
TESTING_QUERIES = [
    {
        "normalized_query": "test-1",
        "ll": "ll-1",
        "spn": "spn-1"
    },
    {
        "normalized_query": "test-2",
        "ll": "ll-2",
        "spn": "spn-2"
    },
    {
        "normalized_query": "test-3",
        "ll": "ll-3",
        "spn": "spn-3"
    }
]


class YtTableMock:
    read_table = lambda _: TESTING_QUERIES
    row_count = lambda _: len(TESTING_QUERIES)


def generate_route(id):
    obj = geo_object_pb2.GeoObject()
    route_meta = obj.metadata.add()
    route_meta.Extensions[transit_pb2.TRANSIT_ROUTE_METADATA].route_id = id

    return obj


def generate_proto_response(found_routes_count, route_id_prefix='route_'):
    proto = geo_object_pb2.GeoObject()
    metadata = proto.metadata.add()
    metadata.Extensions[search_pb2.SEARCH_RESPONSE_METADATA].found = found_routes_count

    for i in range(found_routes_count):
        proto.geo_object.extend([generate_route(route_id_prefix + str(i))])

    return proto.SerializeToString()


def add_routes_to_response(proto_string, routes_to_add, route_id_prefix='route_'):
    proto = geo_object_pb2.GeoObject()
    proto.ParseFromString(proto_string)

    for i in range(len(proto.geo_object), len(proto.geo_object) + routes_to_add):
        proto.geo_object.extend([generate_route(route_id_prefix + str(i))])

    proto.metadata[0].Extensions[search_pb2.SEARCH_RESPONSE_METADATA].found = len(proto.geo_object)

    return proto.SerializeToString()


def get_service_url():
    config = json.loads(read_config_file('/transit_tester.json'))
    return config['transit_url']


def get_service_stable_url():
    config = json.loads(read_config_file('/transit_tester.json'))
    return config['stable_url']


def get_testsets():
    config = json.loads(read_config_file('/transit_tester.json'))
    return config['testsets']


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(transit_tester.fill_graph)


@mock.patch.object(transit_testing_task.TransitIndexTesterTask, '__call__')
def test_module(mocked_call, environment_settings):
    cook = test_utils.GraphCook(environment_settings)
    transit_indexer.fill_graph(cook.input_builder())
    transit_tester.fill_graph(cook.target_builder())
    transit_deployed_resource = cook.create_input_resource(defs.TRANSIT_INDEX_DEPLOYED.format(stage="validation_stage"))
    transit_deployed_resource.version = Version(properties={"release": "0.0.0-0"})

    test_utils.execute(cook)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_different_routes_found__raises(environment_settings, requests_mock):
    requests_mock.get(get_service_url(), status_code=200, content=generate_proto_response(3, 'test_'))
    requests_mock.get(get_service_stable_url(), status_code=200, content=generate_proto_response(5, 'stable_'))

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    with pytest.raises(DataValidationWarning):
        task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_testing_didnt_find_routes__raises(environment_settings, requests_mock):
    requests_mock.get(get_service_url(), status_code=200, content=generate_proto_response(0))
    requests_mock.get(get_service_stable_url(), status_code=200, content=generate_proto_response(3))

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    with pytest.raises(DataValidationWarning):
        task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_more_than_threshold_tests_failed__raises(environment_settings, requests_mock):
    same_content = generate_proto_response(1)
    requests_mock.get(get_service_url(), [
        {'status_code': 200, 'content': same_content},
        {'status_code': 200, 'content': generate_proto_response(1, 'testing_')}
    ])
    requests_mock.get(get_service_stable_url(), status_code=200, content=same_content)

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    with pytest.raises(DataValidationWarning):
        task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_only_first_routes_are_same__wont_raise(environment_settings, requests_mock):
    same_content = generate_proto_response(1)

    requests_mock.get(get_service_url(), status_code=200, content=add_routes_to_response(same_content, 2, 'testing_'))
    requests_mock.get(get_service_stable_url(), status_code=200, content=add_routes_to_response(same_content, 4, 'stable_'))

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_both_didnt_find_routes__wont_raise(environment_settings, requests_mock):
    empty_response = generate_proto_response(0)
    requests_mock.get(get_service_url(), status_code=200, content=empty_response)
    requests_mock.get(get_service_stable_url(), status_code=200, content=empty_response)

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_testing_responses_on_second_try__wont_raise(environment_settings, requests_mock):
    requests_mock.get(get_service_url(), [
        {'status_code': 404},
        {'status_code': 200, 'content': generate_proto_response(1)}])
    requests_mock.get(get_service_stable_url(), status_code=200, content=generate_proto_response(1))

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_stable_service_returns_404__raises(environment_settings, requests_mock):
    requests_mock.get(get_service_url(), status_code=404)
    requests_mock.get(get_service_stable_url(), status_code=200, content=generate_proto_response(3))

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    with pytest.raises(DataValidationWarning):
        task(FAKE_RESOURCE, FAKE_RESOURCE)


@mock.patch('maps.garden.sdk.yt.YtTaskBase.get_yt_client', return_value=YtTableMock)
def test_tester_task__when_testing_service_returns_404__raises(environment_settings, requests_mock):
    requests_mock.get(get_service_url(), status_code=200, content=generate_proto_response(3))
    requests_mock.get(get_service_stable_url(), status_code=404)

    task = transit_testing_task.TransitIndexTesterTask()
    task.load_environment_settings(environment_settings)
    with pytest.raises(DataValidationWarning):
        task(FAKE_RESOURCE, FAKE_RESOURCE)
