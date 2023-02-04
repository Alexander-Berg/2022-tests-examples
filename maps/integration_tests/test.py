import re
import signal
import time

import pytest
import requests

from maps.infra.pycare.example.proto import example_pb2
from maps.infra.pycare.metrics import ServiceMetrics


def test_nginx_and_app_starts(nginx):
    pass


def test_simple_handle(nginx):
    response = nginx.request(
        path='/hello',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_dedicated_slb_vhost(nginx):
    response = nginx.request(
        path='/hello',
        vhost='example.datavalidation.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_common_slb_vhost(nginx):
    for environment in ('unstable', 'testing', 'datavalidation', 'datatesting'):
        response = nginx.request(
            path='/hello',
            vhost=f'example.common.{environment}.maps.yandex.net'
        )
        assert response.status_code == 200


def test_handle_with_container_vhost(nginx):
    response = nginx.request(
        path='/ping',
        vhost='random-abc-def-42.yp-c.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_path_slot(nginx):
    response = nginx.request(
        path='/hello/user',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    assert response.text == 'Hello, user'


def test_handle_with_custom_regex_in_slot(nginx):
    response = nginx.request(
        path='/hello/user/and/more',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_custom_vhost(nginx):
    response = nginx.request(
        path='/vhosted',
        vhost='core-internal.maps.yandex-team.ru'
    )
    assert response.status_code == 200


def test_handle_with_custom_vhost_in_route_table(nginx):
    response = nginx.request(
        path='/vhosted_table',
        vhost='custom.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_restricted_to_local(nginx):
    response = nginx.request(
        path='/restricted',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_prefix(nginx):
    response = nginx.request(
        path='/prefixed/test',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_custom_methods(nginx):
    response = nginx.request(
        path='/methods',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 405
    response = nginx.request(
        path='/methods',
        method='put',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/methods',
        method='patch',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200


def test_handle_with_all_methods(nginx):
    response = nginx.request(
        path='/all_methods',
        method='get',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/all_methods',
        method='head',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/all_methods',
        method='post',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/all_methods',
        method='put',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/all_methods',
        method='patch',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/all_methods',
        method='delete',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    # response = nginx.request(
    #     path='/all_methods',
    #     method='connect',  # TODO: somehow this method slows down the next request to web-server
    #     vhost='example.maps.yandex.net'
    # )
    # assert response.status_code == 200


def test_handle_with_nginx_patch(nginx):
    response = nginx.request(
        path='/whitelisted',
        vhost='example.maps.yandex.net'
    )
    # All requests to this handle are blocked by data/whitelist.auth
    assert response.status_code == 403


def test_handle_with_app_param(testapp):
    response = testapp.request('/app')
    assert response.status_code == 200


@pytest.mark.skip  # TODO: add support of view-class
def test_view(nginx):
    response = nginx.request(
        path='/view',
        method='get',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200
    response = nginx.request(
        path='/view',
        method='post',
        vhost='example.maps.yandex.net'
    )
    assert response.status_code == 200


def test_roquefort_starts(roquefort):
    assert roquefort.list_signals()


def test_roquefort_aware_of_all_handles_unless_explicitly_disabled(roquefort):
    signals = roquefort.list_signals()
    handles = signals.with_prefix('example').handles()
    assert {
        '/ping',           # default one
        '/hello',          # simple
        '/hello/_',        # with regexp in path slot
        '/hello/_/_',      # with custom regexp in path
        '/vhosted',        # with custom vhost
        '/methods',        # with custom methods
        '/whitelisted',    # with nginx config patch
        '/restricted',     # restricted to local
        '/custom_codes',   # with custom metric codes
        '/prefixed/test',  # with prefix in route table
        '/vhosted_table',  # with vhost in route table
    }.issubset(handles)
    assert '/no_metrics' not in handles  # explicitly disabled


def test_roquefort_aware_of_handle_with_custom_codes(roquefort):
    signals = roquefort.list_signals()
    metric_names = signals.with_prefix('example').with_handle('/custom_codes').names()
    assert {'204', '503'}.issubset(metric_names)


def test_params_all_passed(testapp):
    response = testapp.request('/params/test?flag=true&optional=hello&custom=world')
    assert response.status_code == 200
    json_response = response.json()
    assert json_response['param'] == 'test'
    assert json_response['flag'] is True
    assert json_response['optional'] == 'hello'
    assert json_response['custom'] == 'Parameter(world)'


def test_params_optional_not_passed(testapp):
    response = testapp.request('/params/test?flag=true&custom=world')
    assert response.status_code == 200
    json_response = response.json()
    assert json_response['optional'] is None


def test_params_required_not_passed(testapp):
    response = testapp.request('/params/test?flag=true&optional=hello')
    assert response.status_code == 400


def test_params_bad_value_passed(testapp):
    response = testapp.request('/params/test?flag=bad_value&optional=hello&custom=world')
    assert response.status_code == 400


def test_proto_valid(testapp):
    proto_request = example_pb2.TestRequest(query='test query')
    response = testapp.request('/proto', method='post', data=proto_request.SerializeToString())
    assert response.status_code == 200
    proto_response = example_pb2.TestResponse()
    proto_response.ParseFromString(response.content)
    assert proto_response.content == 'Query: test query'


def test_proto_invalid(testapp):
    response = testapp.request('/proto', method='post', data=b'bad proto')
    assert response.status_code == 400


def test_grace_period(testapp_once):  # use separate testapp process
    response = testapp_once.request('/ping')
    assert response.status_code == 200

    testapp_once.send_signal(signal.SIGTERM)

    # we should have some time before termination
    # when /ping returns 500 and app serves requests
    prev_ping_failed = False
    ping_failed = False
    while True:
        time.sleep(0.2)
        try:
            response = testapp_once.request('/ping')
            ping_failed = bool(response.status_code == 503)
            response = testapp_once.request('/hello')
            assert response.status_code == 200
        except requests.exceptions.ConnectionError:
            assert ping_failed
            break
        else:
            if prev_ping_failed:
                assert ping_failed
            prev_ping_failed = ping_failed

    # Explicit wait to prevent sending another SIGTERM
    # inside stop() just before process exit,
    # which leads to weird exit code: -15
    testapp_once.process.wait()


def test_detach_attach(testapp):
    response = testapp.request('/ping')
    assert response.status_code == 200
    response = testapp.request('/hello')
    assert response.status_code == 200

    testapp.detach()

    for _ in range(20):
        time.sleep(1)
        response = testapp.request('/ping')
        if response.status_code == 503:
            break

    response = testapp.request('/ping')
    assert response.status_code == 503
    response = testapp.request('/hello')
    assert response.status_code == 200

    testapp.attach()

    for _ in range(20):
        time.sleep(1)
        response = testapp.request('/ping')
        if response.status_code == 200:
            break

    response = testapp.request('/ping')
    assert response.status_code == 200
    response = testapp.request('/hello')
    assert response.status_code == 200


def test_handle_with_no_cancel(testapp):
    response = testapp.request('/timeout_with_tracking?secs=0', read_timeout=1)
    assert response.status_code == 200
    assert response.json()['finished_requests'] == 1

    with pytest.raises(requests.Timeout):
        testapp.request('/timeout_with_tracking?secs=3', read_timeout=1)

    time.sleep(3)  # wait for previous request to finish

    response = testapp.request('/timeout_with_tracking?secs=0', read_timeout=1)
    assert response.status_code == 200
    assert response.json()['finished_requests'] == 3


def test_access_logs(testapp_once):  # use separate testapp process
    response = testapp_once.request('/hello')
    assert response.status_code == 200

    response = testapp_once.request('/exception')
    assert response.status_code == 500

    with pytest.raises(requests.Timeout):
        testapp_once.request('/timeout_with_tracking?secs=3', read_timeout=1)

    testapp_once.stop(timeout=10)  # we must stop the process to get its stderr
    logs = testapp_once.stderr.decode()

    assert re.search(r'\[INFO\] [0-9a-z]{32} ::1 GET /hello => HTTP 200 \(\d+ ms\)$', logs, re.M)
    assert re.search(r'\[INFO\] [0-9a-z]{32} ::1 GET /exception => HTTP 500 \(\d+ ms\)$', logs, re.M)
    assert re.search(
        r'\[INFO\] [0-9a-z]{32} ::1 GET /timeout_with_tracking\?secs=3 => HTTP 499 \(\d+ ms\)$', logs, re.M
    )


def request_yasm_metrics(testapp):
    responses = list(testapp.request_stats())
    assert responses and all(response.status_code == 200 for response in responses)

    service_metrics = [ServiceMetrics.deserialize(response.json()) for response in responses]
    return ServiceMetrics.aggregate(*service_metrics).extract_yasm_metrics()


def test_log_metrics(testapp_once):  # use separate testapp process
    yasm_metrics = request_yasm_metrics(testapp_once)

    assert ('sample_metric_axxv', 0.) in yasm_metrics
    assert ('sample_metric_annv', 0.) in yasm_metrics
    assert ('sample_metric_ammm', 0.) in yasm_metrics
    assert ('sample_metric_avvt', 0.) in yasm_metrics
    assert ('sample_metric_avvx', 0.) in yasm_metrics
    assert ('sample_metric1_ahhh', (
        (0, 0.),
        (1, 0.),
        (2, 0.),
        (3, 0.),
        (4, 0.),
    )) in yasm_metrics
    assert ('sample_metric2_ahhh', (
        (1., 0.),
        (2., 0.),
        (4., 0.),
        (8., 0.),
        (16., 0.),
        (32., 0.),
    )) in yasm_metrics

    for value in (3., 2., 5., 1., 4.):
        response = testapp_once.request(f'/log-metric?value={value}')
        assert response.status_code == 200

    yasm_metrics = request_yasm_metrics(testapp_once)

    assert ('sample_metric_axxv', 5.) in yasm_metrics
    assert ('sample_metric_annv', 1.) in yasm_metrics
    assert ('sample_metric_ammm', 15.) in yasm_metrics
    assert ('sample_metric_avvt', 4.) in yasm_metrics
    assert ('sample_metric_avvx', 3.) in yasm_metrics
    assert ('sample_metric1_ahhh', (
        (0, 0.),
        (1, 1.),
        (2, 1.),
        (3, 1.),
        (4, 2.),
    )) in yasm_metrics
    assert ('sample_metric2_ahhh', (
        (1., 1.),
        (2., 2.),
        (4., 2.),
        (8., 0.),
        (16., 0.),
        (32., 0.),
    )) in yasm_metrics

    # test metrics reset after request
    yasm_metrics = request_yasm_metrics(testapp_once)

    assert ('sample_metric_axxv', 0.) in yasm_metrics
    assert ('sample_metric_annv', 0.) in yasm_metrics
    assert ('sample_metric_ammm', 0.) in yasm_metrics
    assert ('sample_metric_avvt', 0.) in yasm_metrics
    assert ('sample_metric_avvx', 0.) in yasm_metrics
    assert ('sample_metric1_ahhh', (
        (0, 0.),
        (1, 0.),
        (2, 0.),
        (3, 0.),
        (4, 0.),
    )) in yasm_metrics
    assert ('sample_metric2_ahhh', (
        (1., 0.),
        (2., 0.),
        (4., 0.),
        (8., 0.),
        (16., 0.),
        (32., 0.),
    )) in yasm_metrics
