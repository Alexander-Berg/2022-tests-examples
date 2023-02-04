# -*- coding: utf-8 -*-
import multiprocessing
import time
import pytest
import random
import shutil
import socket
import os

from configs import BalancerSDConfig
from balancer.test.util.sd import SDCacheConfig

from balancer.test.util.predef.handler.server.http import DummyConfig
from balancer.test.util.sync import Counter
from balancer.test.util.proto.handler.server import State
from balancer.test.util.proto.handler.server.http import StaticResponseHandler, StaticResponseConfig
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError
from balancer.test.util import asserts
from balancer.test.util.list_balancing_modes import list_balancing_modes


class RequestHandler(StaticResponseHandler):
    def handle_parsed_request(self, raw_request, stream):
        if raw_request.request_line.path != '/check':
            self.state.request_counter.inc()

            if self.config.random_delay:
                time.sleep(random.random() * self.config.delay)
            elif self.config.delay:
                time.sleep(self.config.delay)

        stream.write_response(self.config.response)
        self.finish_response()


class BackendState(State):
    def __init__(self, config):
        super(BackendState, self).__init__(config)
        self.request_counter = Counter()


class BackendConfig(StaticResponseConfig):
    HANDLER_TYPE = RequestHandler
    STATE_TYPE = BackendState

    def __init__(self, delay=0, random_delay=0):
        super(BackendConfig, self).__init__(response=http.response.ok())
        self.delay = delay
        self.random_delay = random_delay


def check_stderr(ctx):
    stderr = ctx.manager.fs.read_file(ctx.balancer.stderr_file)
    filtered_stderr = []

    to_filter = [
        'WARNING: ASan is ignoring requested __asan_handle_no_return',
        'False positive error reports may follow',
        'For details see https://github.com/google/sanitizers/issues/189',
    ]

    for line in stderr.split('\n'):
        passed = True
        for f in to_filter:
            if f in line:
                passed = False
                break

        if passed:
            filtered_stderr.append(line)

    assert '\n'.join(filtered_stderr) == ""


def send_requests(ctx, request_count, request_generator=lambda i: http.request.get('/')):
    for i in xrange(request_count):
        request = request_generator(i)
        resp = ctx.perform_request(request)
        asserts.status(resp, 200)


def get_sd_update_counter(ctx):
    return ctx.get_unistat()["sd-update_succ_summ"]


def wait_reload(ctx, backends, prev_update_count=None):
    expected_ports = set([backend.server_config.port for backend in backends])

    for i in xrange(0, 10):
        try:
            send_requests(ctx, 5, lambda _: http.request.get('/check'))
        except:
            pass

        result = ctx.call_json_event("dump_backends")
        actual_ports = set()
        for worker_backends in result:
            if len(worker_backends) > 1:
                raise ValueError("invalid dump backends result")

            for backend in worker_backends[0]["backends"]:
                actual_ports.add(backend["proxy"]["port"])
        assert len(actual_ports) > 0

        if (actual_ports == expected_ports and
                (not prev_update_count or get_sd_update_counter(ctx) > prev_update_count)):
            break

        time.sleep(i*i*0.05)

    assert actual_ports == expected_ports
    if prev_update_count:
        assert get_sd_update_counter(ctx) > prev_update_count


def check_request_distribution(algo, r1, r2):
    if algo in ['hashing', 'consistent_hashing', 'rendezvous_hashing', 'subnet', 'dynamic_hashing']:
        assert (r1 > 0) != (r2 > 0)
    elif algo not in ['pwr2']:
        assert r1 > 0 and r2 > 0


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_simple(ctx, algo):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    ctx.start_balancer(BalancerSDConfig(algo=algo))

    request_count = 50

    send_requests(ctx, request_count)

    check_request_distribution(algo, ctx.backend1.state.request_counter.value, ctx.backend2.state.request_counter.value)

    assert ctx.backend1.state.request_counter.value + ctx.backend2.state.request_counter.value == request_count

    check_stderr(ctx)


class FileEnv:
    def __init__(self, ctx, file_template):
        self.ctx = ctx
        self.file_template = file_template
        self.backends_file = ctx.manager.fs.create_file('backends')

    def start_balancer(self, algo):
        self.ctx.start_balancer(BalancerSDConfig(algo=algo, backends_file=self.backends_file))

    def switch_to(self, backend):
        file_content = self.file_template.format(backend.server_config.port)
        self.ctx.manager.fs.rewrite(self.backends_file, file_content)

    def get_endpointset_update_counter(self, test_service_no):
        return None


class RemoteSDEnv:
    def __init__(self, ctx, cache_dir='./sd_cache'):
        self.ctx = ctx
        self.cache_dir = cache_dir
        if self.cache_dir:
            shutil.rmtree(self.cache_dir, ignore_errors=True)

        ctx.start_backend(SDCacheConfig(), name='sd_mock')

    def start_balancer(self, algo, endpoint_sets_count=1, **kwargs):
        self.ctx.start_balancer(BalancerSDConfig(
            algo=algo, sd_port=self.ctx.sd_mock.server_config.port, cache_dir=self.cache_dir,
            endpoint_sets_count=endpoint_sets_count, **kwargs))

    def switch_to(self, backend, test_service_no=1):
        self.ctx.sd_mock.state.set_endpointset(
            'test-cluster{}'.format(test_service_no),
            'test-service{}'.format(test_service_no),
            [{'fqdn': 'backend', 'ip6_address': '::1', 'port': backend.server_config.port}])

    def set_timestamp(self, ts):
        self.ctx.sd_mock.state.set_timestamp(ts)

    def get_endpointset_update_counter(self, test_service_no):
        return self.ctx.get_unistat()['sd-test-cluster{}-test-service{}-update_summ'.format(test_service_no, test_service_no)]


def make_env(env_type, ctx):
    if env_type == "file":
        return FileEnv(ctx, ONE_LOCALHOST_BACKEND)
    elif env_type == "remote_sd":
        return RemoteSDEnv(ctx)
    elif env_type == "remote_sd_memcache":
        return RemoteSDEnv(ctx, cache_dir=None)
    else:
        assert False


def list_env_types():
    res = ["file", "remote_sd"]
    res.append("remote_sd_memcache")
    return res


ONE_LOCALHOST_BACKEND = """
endpoint_set {{
    endpoints {{
        fqdn: \"localhost\"
        port: {}
        ip4_address: \"127.0.0.1\"
    }}
}}
"""


def test_remote_sd_without_cache(ctx):
    ctx.start_backend(BackendConfig(), name='backend1')

    env = RemoteSDEnv(ctx, cache_dir=None)

    ctx.sd_mock.state.set_endpointset(
        'test-cluster1',
        'test-service1',
        [{'fqdn': 'localhost', 'ip4_address': '127.0.0.1', 'port': ctx.backend1.server_config.port}])

    try:
        env.start_balancer('rr')
        assert True
    except:
        assert False


def test_no_cached_addrs(ctx):
    """
    BALANCER-2679
    emulate ip address change for endpoint with dns-resolvable fqdn
    """
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')
    env = make_env("remote_sd", ctx)

    ctx.sd_mock.state.set_endpointset(
        'test-cluster1',
        'test-service1',
        [{'fqdn': 'localhost', 'ip4_address': '127.0.0.1', 'port': ctx.backend1.server_config.port}])
    env.switch_to(ctx.backend1)
    env.start_balancer('rr')

    send_requests(ctx, 10)

    ctx.sd_mock.state.set_endpointset(
        'test-cluster1',
        'test-service1',
        [{'fqdn': 'localhost', 'ip6_address': '127.0.0.42', 'port': ctx.backend1.server_config.port}])

    failed = False

    for i in xrange(0, 10):
        try:
            request = http.request.get()
            resp = ctx.perform_request(request)
            asserts.status(resp, 200)
            time.sleep(i*i*0.05)
            continue
        except:
            failed = True
            break

        time.sleep(i*i*0.05)

    assert failed


@pytest.mark.parametrize('env_type', list_env_types())
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_update(ctx, algo, env_type):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    env = make_env(env_type, ctx)

    env.switch_to(ctx.backend1)
    env.start_balancer(algo)

    active_backend = 1

    request_count = 10

    for i in xrange(10):
        prev1 = ctx.backend1.state.request_counter.value
        prev2 = ctx.backend2.state.request_counter.value

        send_requests(ctx, request_count)

        assert active_backend == 1 or active_backend == 2
        assert ctx.backend1.state.request_counter.value == prev1 + (request_count if active_backend == 1 else 0)
        assert ctx.backend2.state.request_counter.value == prev2 + (request_count if active_backend == 2 else 0)

        prev_update_count = get_sd_update_counter(ctx)
        if active_backend == 1:
            env.switch_to(ctx.backend2)
            active_backend = 2
            wait_reload(ctx, [ctx.backend2, ], prev_update_count)
        else:
            env.switch_to(ctx.backend1)
            active_backend = 1
            wait_reload(ctx, [ctx.backend1, ], prev_update_count)

    c = env.get_endpointset_update_counter(1)
    if c:
        assert c >= 10

    check_stderr(ctx)


def test_not_resolvable(ctx):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')
    env = RemoteSDEnv(ctx, cache_dir=None)

    with pytest.raises(BalancerStartError):
        env.start_balancer('rr', sd_host="not_resolvable")


def test_cached_ip(ctx):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    env = make_env("remote_sd", ctx)

    env.switch_to(ctx.backend1)
    env.start_balancer('rr', sd_host="not_resolvable", sd_cached_ip="127.0.0.1")

    active_backend = 1

    request_count = 10

    for i in xrange(10):
        prev1 = ctx.backend1.state.request_counter.value
        prev2 = ctx.backend2.state.request_counter.value

        send_requests(ctx, request_count)

        assert active_backend == 1 or active_backend == 2
        assert ctx.backend1.state.request_counter.value == prev1 + (request_count if active_backend == 1 else 0)
        assert ctx.backend2.state.request_counter.value == prev2 + (request_count if active_backend == 2 else 0)

        prev_update_count = get_sd_update_counter(ctx)
        if active_backend == 1:
            env.switch_to(ctx.backend2)
            active_backend = 2
            wait_reload(ctx, [ctx.backend2, ], prev_update_count)
        else:
            env.switch_to(ctx.backend1)
            active_backend = 1
            wait_reload(ctx, [ctx.backend1, ], prev_update_count)

    c = env.get_endpointset_update_counter(1)
    if c:
        assert c >= 10

    check_stderr(ctx)


@pytest.mark.skip(reason="there are no weights in yp sd by now")
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_weights(ctx, algo):
    if algo in ["leastconn", "pwr2"]:
        return

    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    backends_template = """
    endpoint_set {{
        endpoints {{
            port: {}
            weight: {}
            fqdn: \"localhost\"
            ip4_address: \"127.0.0.1\"
        }}
        endpoints {{
            port: {}
            weight: {}
            fqdn: \"localhost\"
            ip4_address: \"127.0.0.1\"
        }}
    }};"""

    backends_content = backends_template.format(
        ctx.backend1.server_config.port, 1.0, ctx.backend2.server_config.port, 2.0)
    fat_backend = 2

    backends_file = ctx.manager.fs.create_file('backends')
    ctx.manager.fs.rewrite(backends_file, backends_content)

    ctx.start_balancer(BalancerSDConfig(algo=algo, backends_file=backends_file))

    request_count = 100

    for _ in xrange(5):

        prev1 = ctx.backend1.state.request_counter.value
        prev2 = ctx.backend2.state.request_counter.value

        send_requests(ctx, request_count, lambda i: http.request.get('/?text=' + str(i)))

        reqs1 = ctx.backend1.state.request_counter.value - prev1
        reqs2 = ctx.backend2.state.request_counter.value - prev2

        assert fat_backend == 1 or fat_backend == 2

        if fat_backend == 1:
            assert reqs1 > 1.5 * reqs2
        else:
            assert reqs2 > 1.5 * reqs1

        assert reqs1 + reqs2 == request_count

        if fat_backend == 1:
            backends_content = backends_template.format(
                ctx.backend1.server_config.port, 1.0, ctx.backend2.server_config.port, 2.0)
            fat_backend = 2
        else:
            backends_content = backends_template.format(
                ctx.backend1.server_config.port, 2.0, ctx.backend2.server_config.port, 1.0)
            fat_backend = 1

        ctx.manager.fs.rewrite(backends_file, backends_content)

        time.sleep(2)

    check_stderr(ctx)


def test_active_quorum_update(ctx):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')
    ctx.start_backend(DummyConfig(), name='backend3')

    backends_template = """
    endpoint_set {{
        endpoints {{
            fqdn: \"localhost\"
            port: {}
            ip4_address: \"127.0.0.1\"
        }}
        endpoints {{
            fqdn: \"localhost\"
            port: {}
            ip4_address: \"127.0.0.1\"
        }}
    }}"""

    backends_with_quorum = backends_template.format(ctx.backend1.server_config.port, ctx.backend2.server_config.port)
    backends_without_quorum = backends_template.format(ctx.backend1.server_config.port, ctx.backend3.server_config.port)

    backends_file = ctx.manager.fs.create_file('backends')
    ctx.manager.fs.rewrite(backends_file, backends_with_quorum)
    has_quorum = True

    ctx.start_balancer(BalancerSDConfig(
        algo='active', backends_file=backends_file, active_quorum=1.5, active_hysteresis=0.0))

    request_count = 100

    for _ in xrange(10):
        prev1 = ctx.backend1.state.request_counter.value
        prev2 = ctx.backend2.state.request_counter.value

        for i in xrange(request_count):
            if has_quorum:
                resp = ctx.perform_request(http.request.get())
                asserts.status(resp, 200)
            else:
                ctx.perform_request_xfail(http.request.get())

        reqs1 = ctx.backend1.state.request_counter.value - prev1
        reqs2 = ctx.backend2.state.request_counter.value - prev2

        if has_quorum:
            assert reqs1 + reqs2 == request_count
        else:
            assert reqs1 + reqs2 == 0

        if has_quorum:
            ctx.manager.fs.rewrite(backends_file, backends_without_quorum)
            has_quorum = False
            wait_reload(ctx, [ctx.backend1, ctx.backend3])
        else:
            ctx.manager.fs.rewrite(backends_file, backends_with_quorum)
            has_quorum = True
            wait_reload(ctx, [ctx.backend1, ctx.backend2])

        # wait quorum recalc
        time.sleep(2)

    check_stderr(ctx)


stopped = multiprocessing.Value('i', 0)
send_counter = multiprocessing.Value('i', 0)


def send_parallel_requests(ctx, request_generator=lambda i: http.request.get()):
    i = 0
    errors = 0
    while not stopped.value:
        request = request_generator(i)
        try:
            resp = ctx.perform_request(request)
        except:
            errors += 1
            if errors > 10:
                raise
            time.sleep(0.1)

        asserts.status(resp, 200)

        with send_counter.get_lock():
            send_counter.value += 1

        i += 1


@pytest.mark.parametrize('env_type', list_env_types())
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_multiprocess(ctx, algo, env_type):
    ctx.start_backend(BackendConfig(random_delay=0.1), name='backend1')
    ctx.start_backend(BackendConfig(random_delay=0.1), name='backend2')

    env = make_env(env_type, ctx)

    env.switch_to(ctx.backend1)
    env.start_balancer(algo)

    active_backend = 1

    prev1 = ctx.backend1.state.request_counter.value
    prev2 = ctx.backend2.state.request_counter.value

    stopped.value = 0
    send_counter.value = 0
    processes = []
    for i in xrange(5):
        p = multiprocessing.Process(target=send_parallel_requests, args=(ctx,))
        p.start()
        processes.append(p)

    request_count = 100

    for _ in xrange(10):
        prev1 = ctx.backend1.state.request_counter.value
        prev2 = ctx.backend2.state.request_counter.value
        send_prev = send_counter.value

        while send_counter.value <= send_prev + request_count:
            have_alive = False
            for p in processes:
                if p.is_alive():
                    have_alive = True

            if not have_alive:
                assert False

            time.sleep(0.1)

        assert active_backend == 1 or active_backend == 2

        prev_update_count = get_sd_update_counter(ctx)
        if active_backend == 1:
            assert ctx.backend1.state.request_counter.value - prev1 >= request_count - len(processes)
            assert ctx.backend2.state.request_counter.value <= prev2 + len(processes)
            env.switch_to(ctx.backend2)
            active_backend = 2
            wait_reload(ctx, [ctx.backend2, ], prev_update_count)
        else:
            assert ctx.backend2.state.request_counter.value - prev2 >= request_count - len(processes)
            assert ctx.backend1.state.request_counter.value <= prev1 + len(processes)
            env.switch_to(ctx.backend1)
            active_backend = 1
            wait_reload(ctx, [ctx.backend1, ], prev_update_count)

        ctx.perform_request(http.request.get())

    stopped.value = 1

    for p in processes:
        p.join()

    c = env.get_endpointset_update_counter(1)
    if c:
        assert c >= 10

    check_stderr(ctx)


@pytest.mark.parametrize('env_type', list_env_types())
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_compound_endpointset(ctx, algo, env_type):
    if env_type == "file":
        return

    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')
    ctx.start_backend(BackendConfig(), name='backend3')

    env = make_env(env_type, ctx)

    env.switch_to(ctx.backend1, 1)
    env.switch_to(ctx.backend3, 2)

    env.start_balancer(algo, 2)

    active_backend = 1

    request_count = 100

    for i in xrange(10):
        prev1 = ctx.backend1.state.request_counter.value
        prev2 = ctx.backend2.state.request_counter.value
        prev3 = ctx.backend3.state.request_counter.value

        send_requests(ctx, request_count)

        assert active_backend == 1 or active_backend == 2
        assert ctx.backend1.state.request_counter.value + ctx.backend3.state.request_counter.value == \
            prev1 + prev3 + (request_count if active_backend == 1 else 0)
        assert ctx.backend2.state.request_counter.value == \
            prev2 + (request_count if active_backend == 2 else 0)

        if active_backend == 1:
            check_request_distribution(
                algo,
                ctx.backend1.state.request_counter.value - prev1,
                ctx.backend3.state.request_counter.value - prev3
            )

        if active_backend == 1:
            env.switch_to(ctx.backend2, 1)
            env.switch_to(ctx.backend2, 2)
            active_backend = 2
            wait_reload(ctx, [ctx.backend2, ctx.backend2])
        else:
            env.switch_to(ctx.backend1, 1)
            env.switch_to(ctx.backend3, 2)
            active_backend = 1
            wait_reload(ctx, [ctx.backend1, ctx.backend3])

    for i in [1, 2]:
        c = env.get_endpointset_update_counter(i)
        if c:
            assert c >= 10

    check_stderr(ctx)


@pytest.mark.parametrize('env_type', list_env_types())
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_yp_timestamp(ctx, env_type, algo):
    if env_type == "file":
        return

    ctx.start_backend(BackendConfig(), name='backend2')

    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    env = make_env(env_type, ctx)

    env.set_timestamp(100)
    env.switch_to(ctx.backend1)

    env.start_balancer(algo)

    assert ctx.get_unistat()['sd-test-cluster1-test-service1-yp_timestamp_max_axxx'] == 100
    assert ctx.get_unistat()['sd-test-cluster1-test-service1-yp_timestamp_min_annn'] == 100

    env.set_timestamp(101)
    env.switch_to(ctx.backend2)
    wait_reload(ctx, [ctx.backend2, ])

    prev = get_sd_update_counter(ctx)
    for i in range(0, 10):
        if get_sd_update_counter(ctx) > 10 * prev:
            break
        time.sleep(i * 0.1)

    assert ctx.get_unistat()['sd-test-cluster1-test-service1-yp_timestamp_max_axxx'] == 101
    assert ctx.get_unistat()['sd-test-cluster1-test-service1-yp_timestamp_min_annn'] == 101

    check_stderr(ctx)


@pytest.mark.parametrize('algo', list_balancing_modes())
@pytest.mark.parametrize('terminate', [True, False])
def test_termination(ctx, algo, terminate):
    ctx.start_backend(BackendConfig(delay=3), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    backends_template = ONE_LOCALHOST_BACKEND

    backends1 = backends_template.format(ctx.backend1.server_config.port)
    backends2 = backends_template.format(ctx.backend2.server_config.port)

    backends_file = ctx.manager.fs.create_file('backends')
    ctx.manager.fs.rewrite(backends_file, backends1)

    termination_delay = '100ms' if terminate else None
    ctx.start_balancer(BalancerSDConfig(
        algo=algo, backends_file=backends_file, termination_delay=termination_delay))

    stream = ctx.create_http_connection().create_stream()
    stream.write_request(http.request.get().to_raw_request())

    i = 0
    while ctx.backend1.state.request_counter.value == 0:
        assert i < 10
        i += 1
        time.sleep(i*0.1)

    ctx.manager.fs.rewrite(backends_file, backends2)
    wait_reload(ctx, [ctx.backend2, ])

    try:
        stream.read_response()
    except:
        if terminate:
            pass
        else:
            assert False
    else:
        if terminate:
            assert False

    check_stderr(ctx)


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_nocancel_transfer(ctx, algo):
    """
    BALANCER-3230
    """
    ctx.start_backend(BackendConfig(delay=3), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    backends_template1 = ONE_LOCALHOST_BACKEND
    backends_template2 = """
    endpoint_set {{
        endpoints {{
            port: {}
            fqdn: \"localhost\"
            ip4_address: \"127.0.0.1\"
        }}
        endpoints {{
            port: {}
            fqdn: \"localhost\"
            ip4_address: \"127.0.0.1\"
        }}
    }};"""

    backends1 = backends_template1.format(ctx.backend1.server_config.port)
    backends2 = backends_template2.format(ctx.backend1.server_config.port, ctx.backend2.server_config.port)

    can_update = algo in ["dynamic", "dynamic_hashing"]

    backends_file = ctx.manager.fs.create_file('backends')
    ctx.manager.fs.rewrite(backends_file, backends1)

    ctx.start_balancer(BalancerSDConfig(
        algo=algo, backends_file=backends_file, termination_delay='100ms'))

    stream = ctx.create_http_connection().create_stream()
    stream.write_request(http.request.get().to_raw_request())

    i = 0
    while ctx.backend1.state.request_counter.value == 0:
        assert i < 10
        i += 1
        time.sleep(i*0.1)

    ctx.manager.fs.rewrite(backends_file, backends2)
    wait_reload(ctx, [ctx.backend1, ctx.backend2])

    try:
        stream.read_response()
    except:
        if not can_update:
            pass
        else:
            assert False
    else:
        if not can_update:
            assert False

    check_stderr(ctx)


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_ip_update(ctx, algo):
    """
    BALANCER-3304
    """
    ctx.start_backend(BackendConfig(), name='backend1', host='127.0.0.1', family=[socket.AF_INET])
    ctx.start_backend(BackendConfig(), name='backend2', host='127.0.0.2',
                      port=ctx.backend1.server_config.port, family=[socket.AF_INET])

    backends_template = """
    endpoint_set {{
        endpoints {{
            port: {}
            fqdn: \"localhost\"
            ip4_address: \"{}\"
        }}
    }};"""

    backends1 = backends_template.format(ctx.backend1.server_config.port, ctx.backend1.server_config.host)
    backends2 = backends_template.format(ctx.backend2.server_config.port, ctx.backend2.server_config.host)

    backends_file = ctx.manager.fs.create_file('backends')
    ctx.manager.fs.rewrite(backends_file, backends1)

    ctx.start_balancer(BalancerSDConfig(algo=algo, backends_file=backends_file))

    ctx.perform_request(http.request.get())

    assert ctx.backend1.state.request_counter.value == 1

    ctx.manager.fs.rewrite(backends_file, backends2)
    time.sleep(3)

    ctx.perform_request(http.request.get())

    assert ctx.backend2.state.request_counter.value == 1

    check_stderr(ctx)


def test_log(ctx):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    env = make_env("remote_sd", ctx)
    env.switch_to(ctx.backend1)

    env.start_balancer("rr")
    logfile = ctx.balancer.config.sd_log
    env.switch_to(ctx.backend2)
    wait_reload(ctx, [ctx.backend2, ])

    assert os.stat(logfile).st_size > 0

    rotated = logfile + ".1"
    os.rename(logfile, rotated)

    reopenlog_response = ctx.perform_request(
        http.request.get('/admin?action=reopenlog'), port=ctx.balancer.config.admin_port)
    asserts.status(reopenlog_response, 200)

    prev = os.stat(rotated).st_size

    env.switch_to(ctx.backend1)
    wait_reload(ctx, [ctx.backend1, ])

    for i in xrange(0, 10):
        if os.stat(logfile).st_size > 0:
            break

        time.sleep(i * 0.1)

    assert os.stat(rotated).st_size == prev
    assert os.stat(logfile).st_size > 0


def test_count_backends_in_attempts(ctx):
    set1 = []
    set2 = []
    for i in range(1, 11):
        backend = ctx.start_backend(DummyConfig(), name='backend{}'.format(i))
        endpoint = {'fqdn': 'backend', 'ip6_address': '::1', 'port': backend.server_config.port}
        if i < 4:
            set1.append((backend, endpoint))
        else:
            set2.append((backend, endpoint))

    env = make_env("remote_sd", ctx,)

    ctx.sd_mock.state.set_endpointset('test-cluster1', 'test-service1', zip(*set1)[1])

    env.start_balancer("rr", attempts='count_backends')

    unistat = ctx.get_unistat()
    prev = unistat['report-default-backend_attempt_summ']
    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] - prev == len(set1)

    ctx.sd_mock.state.set_endpointset('test-cluster1', 'test-service1', zip(*set2)[1])
    wait_reload(ctx, zip(*set2)[0])

    unistat = ctx.get_unistat()
    prev = unistat['report-default-backend_attempt_summ']
    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-backend_attempt_summ'] - prev == len(set2)


def test_count_backends_in_connection_attempts(ctx):
    set1 = []
    set2 = []
    for i in range(1, 11):
        backend = ctx.start_fake_backend(name='backend{}'.format(i))
        endpoint = {'fqdn': 'backend', 'ip6_address': '::1', 'port': backend.server_config.port}
        if i < 4:
            set1.append((backend, endpoint))
        else:
            set2.append((backend, endpoint))

    env = make_env("remote_sd", ctx)

    ctx.sd_mock.state.set_endpointset('test-cluster1', 'test-service1', zip(*set1)[1])

    env.start_balancer("rr", connection_attempts='count_backends')

    unistat = ctx.get_unistat()
    prev = unistat['report-default-conn_refused_summ']
    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-conn_refused_summ'] - prev == len(set1)

    ctx.sd_mock.state.set_endpointset('test-cluster1', 'test-service1', zip(*set2)[1])
    wait_reload(ctx, zip(*set2)[0])

    unistat = ctx.get_unistat()
    prev = unistat['report-default-conn_refused_summ']
    request = http.request.get().to_raw_request()
    ctx.perform_request_xfail(request)

    unistat = ctx.get_unistat()
    assert unistat['report-default-conn_refused_summ'] - prev == len(set2)


def test_dynamic_ping_backends_after_update(ctx):
    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    env = make_env("remote_sd", ctx)
    env.switch_to(ctx.backend1)

    env.start_balancer("dynamic", dynamic_active=True)
    wait_reload(ctx, [ctx.backend1])

    time.sleep(2)

    env.switch_to(ctx.backend2)
    wait_reload(ctx, [ctx.backend2])

    backend1_requests = ctx.backend1.state.request_counter.value
    assert backend1_requests > 0

    time.sleep(2)

    backend2_requests = ctx.backend2.state.request_counter.value
    assert backend2_requests > 0
    assert backend1_requests == ctx.backend1.state.request_counter.value


@pytest.mark.parametrize('env_type', list_env_types())
@pytest.mark.parametrize('algo', ['dynamic', 'dynamic_hashing'])
def test_dynamic_sd_readiness(ctx, algo, env_type):
    if env_type == "file":
        pytest.skip("file env not supported")

    gen = lambda i: http.request.get('/{}'.format(i))

    ctx.start_backend(BackendConfig(), name='backend1')
    ctx.start_backend(BackendConfig(), name='backend2')

    env = make_env(env_type, ctx)
    ctx.sd_mock.state.set_endpointset(
        'test-cluster{}'.format(1),
        'test-service{}'.format(1),
        [
            {'fqdn': 'backend', 'ip6_address': '::1', 'port': ctx.backend1.server_config.port, 'ready': True},
            {'fqdn': 'backend', 'ip6_address': '::1', 'port': ctx.backend2.server_config.port, 'ready': True},
        ]
    )

    env.start_balancer(algo, max_pessimized_share=0.5)
    wait_reload(ctx, [ctx.backend1, ctx.backend2])

    backend1_requests = ctx.backend1.state.request_counter.value
    backend2_requests = ctx.backend2.state.request_counter.value

    n_requests = 100
    send_requests(ctx, n_requests, gen)

    assert ctx.backend1.state.request_counter.value - backend1_requests > 0 and ctx.backend2.state.request_counter.value - backend2_requests > 0

    for i in range(2):
        ctx.sd_mock.state.set_endpointset(
            'test-cluster{}'.format(1),
            'test-service{}'.format(1),
            [
                {'fqdn': 'backend', 'ip6_address': '::1', 'port': ctx.backend1.server_config.port, 'ready': i == 0},
                {'fqdn': 'backend', 'ip6_address': '::1', 'port': ctx.backend2.server_config.port, 'ready': i == 1},
            ]
        )

        time.sleep(5)

        prev_update_count = get_sd_update_counter(ctx)
        backends = []
        if i == 0:
            backends.append(ctx.backend1)
        else:
            backends.append(ctx.backend2)
        wait_reload(ctx, backends, prev_update_count)

        backend1_requests = ctx.backend1.state.request_counter.value
        backend2_requests = ctx.backend2.state.request_counter.value

        send_requests(ctx, n_requests, gen)

        if i == 0:
            assert ctx.backend1.state.request_counter.value - backend1_requests > 0 and ctx.backend2.state.request_counter.value - backend2_requests == 0
        else:
            assert ctx.backend1.state.request_counter.value - backend1_requests == 0 and ctx.backend2.state.request_counter.value - backend2_requests > 0

    check_stderr(ctx)


def test_forbid_empty_endpoint_set(ctx):
    '''
    BALANCER-3231
    If allow_empty_endpoint_sets = false
    balancer shouldn't start with empty endpoint set
    '''
    env = make_env("remote_sd", ctx)

    ctx.sd_mock.state.set_endpointset(
        'test-cluster1',
        'test-service1',
        [])

    with pytest.raises(BalancerStartError):
        env.start_balancer('rr')


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_allow_empty_endpoint_set(ctx, algo):
    '''
    BALANCER-3231
    If allow_empty_endpoint_sets = true
    balancer should start and fail requests with empty endpoint set
    '''
    env = make_env("remote_sd", ctx)

    ctx.sd_mock.state.set_endpointset(
        'test-cluster1',
        'test-service1',
        [])

    env.start_balancer(algo, allow_empty_endpoint_sets=True)

    ctx.perform_request_xfail(http.request.get())
