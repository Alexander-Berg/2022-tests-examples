# coding: utf-8
import mock
import pytest
import six

from awacs.model.balancer.generator import get_would_be_injected_full_knob_ids
from awacs.model.util import clone_pb
from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ANY_MODULE, wrap, ValidationCtx
from awacs.wrappers.main import (Balancer2, Hasher, Hashing, BalancingPolicy, GeneratedProxyBackends, Pwr2,
                                 GeneratedProxyBackendsProxyOptions, GeneratedProxyBackendsNannySnapshot, Rr,
                                 InstanceMacro, Dynamic)
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_validation_exception


def test_balancer2_knobs():
    ctx = ValidationCtx()

    pb = modules_pb2.Balancer2Module(attempts=5)

    rr_pb = pb.rr
    rr_pb.k_weights_file.id = 'test-knob-id-1'

    rr = Rr(rr_pb)
    v = rr.get('weights_file')
    assert v.type == v.KNOB
    assert v.value.pb == rr_pb.k_weights_file
    assert rr.get_included_knob_ids('test-namespace', ctx=ctx) == {('test-namespace', 'test-knob-id-1')}

    pb.k_attempts_file.id = 'test-knob-id-2'

    balancer = Balancer2(pb)

    assert get_would_be_injected_full_knob_ids('test-namespace', balancer, ctx=ctx) == {
        ('test-namespace', 'test-knob-id-1'),
        ('test-namespace', 'test-knob-id-2'),
    }

    assert balancer.get_included_knob_ids('test-namespace', ctx=ctx) == {
        ('test-namespace', 'test-knob-id-1'),
        ('test-namespace', 'test-knob-id-2'),
    }


def test_balancer2():
    pb = modules_pb2.Balancer2Module()

    balancer2 = Balancer2(pb)
    e = get_validation_exception(balancer2.validate)
    e.match('attempts: is required')

    pb.attempts = 2

    e = get_validation_exception(balancer2.validate)
    e.match('at least one of the "active", "dynamic", "hashing", "leastconn", "pwr2", "rendezvous_hashing", "rr", '
            '"weighted2" must be specified')

    pb.weighted2.min_weight = 100
    pb.weighted2.history_time = 'x'
    balancer2.update_pb(pb)

    e = get_validation_exception(balancer2.validate)
    e.match('at least one of the "backends", "generated_proxy_backends" must be specified')

    pb.generated_proxy_backends.SetInParent()
    balancer2.update_pb(pb)
    with mock.patch.object(balancer2.generated_proxy_backends, 'validate'):
        e = get_validation_exception(balancer2.validate)

    e.match('history_time.*is not a valid timedelta string')
    assert list(e.value.path) == ['weighted2', 'history_time']

    pb.weighted2.history_time = '20s'
    pb.ClearField('generated_proxy_backends')
    balancer2.update_pb(pb)

    e = get_validation_exception(balancer2.validate)
    e.match('at least one of the "backends", "generated_proxy_backends" must be specified')

    backend_1 = pb.backends.add()
    backend_1.name = 'a'
    backend_1.weight = 10

    backend_2 = pb.backends.add()
    backend_2.name = 'a'
    backend_2.weight = 20

    balancer2.update_pb(pb)
    assert len(balancer2.backends) == 2

    assert balancer2.backends[0].pb.weight == 10
    assert balancer2.backends[1].pb.weight == 20

    with mock.patch.object(balancer2.backends[0], 'validate'):
        e = get_validation_exception(balancer2.validate)
    e.match(r'backends\[1\] -> name: duplicate backend name: "a"')

    backend_2.name = ''
    balancer2.update_pb(pb)

    with mock.patch.object(balancer2.backends[0], 'validate') as b_0_validate:
        with mock.patch.object(balancer2.backends[1], 'validate',
                               side_effect=ValidationError('BAD')) as b_1_validate:
            e = get_validation_exception(balancer2.validate)
    e.match(r'backends\[1\].*BAD')
    b_0_validate.assert_called_once()
    b_1_validate.assert_called_once()

    with mock.patch.object(balancer2.backends[0], 'validate') as b_0_validate:
        with mock.patch.object(balancer2.backends[1], 'validate') as b_1_validate:
            balancer2.validate()
    b_0_validate.assert_called_once()
    b_1_validate.assert_called_once()

    c = balancer2.to_config()
    assert set(c.table.keys()) == {'attempts', 'weighted2', 'unique_policy'}
    weighted2_c = c.table['weighted2']
    assert set(weighted2_c.table.keys()) == {'correction_params', 'slow_reply_time', 'a'}
    assert len(weighted2_c.array) == 1

    pb.ClearField('weighted2')
    pb.hashing.SetInParent()
    balancer2.update_pb(pb)

    with mock.patch.object(balancer2.backends[0], 'validate'):
        with mock.patch.object(balancer2.backends[1], 'validate'):
            with mock.patch.object(balancer2.hashing, 'validate', side_effect=ValidationError('BAD')):
                e = get_validation_exception(balancer2.validate)
    e.match('hashing.*BAD')

    with mock.patch.object(balancer2.backends[0], 'validate'):
        with mock.patch.object(balancer2.backends[1], 'validate'):
            with mock.patch.object(balancer2.hashing, 'validate'):
                balancer2.validate()

    pb.return_last_5xx = True
    with mock.patch.object(balancer2.backends[0], 'validate'):
        with mock.patch.object(balancer2.backends[1], 'validate'):
            with mock.patch.object(balancer2.hashing, 'validate'):
                e = get_validation_exception(balancer2.validate)
    e.match('return_last_5xx.*at least one 5xx must be specified in status_code_blacklist')

    pb.status_code_blacklist.append('503')
    with mock.patch.object(balancer2.backends[0], 'validate'):
        with mock.patch.object(balancer2.backends[1], 'validate'):
            with mock.patch.object(balancer2.hashing, 'validate'):
                balancer2.validate()

    pb.balancing_policy \
        .retry_policy.balancing_policy \
        .active_policy.balancing_policy \
        .retry_policy.balancing_policy \
        .unique_policy.SetInParent()
    pb.rr.SetInParent()
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match('balancing_policy: active_policy can only be followed by unique_policy')

    pb.balancing_policy \
        .retry_policy.balancing_policy \
        .active_policy.balancing_policy \
        .unique_policy.SetInParent()
    balancer2.update_pb(pb)
    with mock.patch.object(balancer2.backends[0], 'validate'):
        with mock.patch.object(balancer2.backends[1], 'validate'):
            balancer2.validate()


def test_connection_attempts():
    pb = modules_pb2.Balancer2Module(attempts=2)
    pb.weighted2.min_weight = 100
    instance = pb.generated_proxy_backends.instances.add()
    instance.host = '127.0.0.1'
    instance.port = 8081

    pb.connection_attempts = 1
    pb.generated_proxy_backends.proxy_options.keepalive_count = 1

    balancer2 = Balancer2(pb)
    balancer2.validate()

    pb.generated_proxy_backends.proxy_options.keepalive_count = 0
    balancer2.update_pb(pb)
    c1 = balancer2.to_config()
    assert 'connection_attempts' in c1.table.keys()
    assert c1.table['connection_attempts'] == 1

    pb.connection_attempts = 0
    pb.generated_proxy_backends.proxy_options.keepalive_count = 1
    balancer2.update_pb(pb)
    c2 = balancer2.to_config()
    assert 'connection_attempts' not in c2.table.keys()


def test_fast_attempts():
    pb = modules_pb2.Balancer2Module(attempts=2)
    pb.weighted2.min_weight = 100
    instance = pb.generated_proxy_backends.instances.add()
    instance.host = '127.0.0.1'
    instance.port = 8081

    pb.fast_attempts = 1
    pb.generated_proxy_backends.proxy_options.keepalive_count = 1

    balancer2 = Balancer2(pb)
    balancer2.validate()

    pb.generated_proxy_backends.proxy_options.keepalive_count = 0
    balancer2.update_pb(pb)
    c1 = balancer2.to_config()
    assert 'fast_attempts' in c1.table.keys()
    assert c1.table['fast_attempts'] == 1

    pb.fast_attempts = 0
    pb.generated_proxy_backends.proxy_options.keepalive_count = 1
    balancer2.update_pb(pb)
    c2 = balancer2.to_config()
    assert 'fast_attempts' not in c2.table.keys()


def test_hashing():
    pb = modules_pb2.Hashing()
    hashing = Hashing(pb)

    e = get_validation_exception(hashing.validate)
    e.match('must be preceded by "hasher" or "headers_hasher" or "cookie_hasher" or "cgi_hasher" module')

    hasher_pb = modules_pb2.HasherModule()
    hasher = Hasher(hasher_pb)
    hashing.validate(preceding_modules=[hasher])
    hashing.validate(preceding_modules=[ANY_MODULE])


def test_dynamic_hashing():
    pb = modules_pb2.Balancer2Module.Dynamic()
    pb.hashing.SetInParent()
    dynamic_hashing = Dynamic(pb)
    e = get_validation_exception(dynamic_hashing.validate)
    e.match('max_pessimized_share: is required')

    pb.max_pessimized_share = 0.5
    dynamic_hashing = Dynamic(pb)
    e = get_validation_exception(dynamic_hashing.validate)
    e.match('must be preceded by "hasher" or "headers_hasher" or "cookie_hasher" or "cgi_hasher" module')

    hasher_pb = modules_pb2.HasherModule()
    hasher = Hasher(hasher_pb)
    dynamic_hashing.validate(preceding_modules=[hasher])
    dynamic_hashing.validate(preceding_modules=[ANY_MODULE])

    instance_macro_pb = modules_pb2.InstanceMacro()
    instance_macro = InstanceMacro(instance_macro_pb)
    e = get_validation_exception(dynamic_hashing.validate, preceding_modules=[instance_macro, hasher])
    e.match('can only be used if preceded by instance_macro or main module with state_directory, or l7_macro of version 0.0.3+')

    instance_macro_pb.state_directory = '/test'
    instance_macro = InstanceMacro(instance_macro_pb)
    dynamic_hashing.validate(preceding_modules=[instance_macro, hasher])


def test_rr():
    pb = modules_pb2.Rr()
    rr = Rr(pb)
    rr.validate()

    pb.randomize_initial_state = True
    rr.validate()

    pb.count_of_randomized_requests_on_weights_application = 123
    e = get_validation_exception(rr.validate)
    e.match('"randomize_initial_state" and "count_of_randomized_requests_on_weights_application" '
            'can not be used together')


def test_pwr2():
    pb = modules_pb2.Pwr2()
    weighted_criterion = pb.combined_criterion.criteria.add()

    pwr2 = Pwr2(pb)

    e = get_validation_exception(pwr2.validate)
    e.match('weight: is required')

    weighted_criterion.weight = -1

    pwr2.update_pb(pb)
    e = get_validation_exception(pwr2.validate, preceding_modules=[ANY_MODULE])
    e.match(r'combined_criterion -> criterion\[0\] -> criterion: is required')

    weighted_criterion.criterion.fail_rate_criterion.history_time = '10s'
    pwr2.update_pb(pb)
    e = get_validation_exception(pwr2.validate)
    e.match(r'combined_criterion -> criterion\[0\] -> weight: criterion weight must be positive: -1.0 <= 0')

    weighted_criterion.weight = 1
    pwr2.update_pb(pb)
    pwr2.validate()

    pb.combined_criterion.criteria.pop()
    pwr2.update_pb(pb)
    e = get_validation_exception(pwr2.validate)
    e.match('combined_criterion -> criteria: is required')


def test_balancing_policy():
    at_least_one_message = (
        'at least one of the "active_policy", "by_hash_policy", "by_name_from_header_policy", "by_name_policy", '
        '"retry_policy", "simple_policy", "timeout_policy", "unique_policy", "watermark_policy" must be specified')
    pb = modules_pb2.BalancingPolicy()
    pol = BalancingPolicy(pb)
    e = get_validation_exception(pol.validate)
    e.match(at_least_one_message)

    # test simple_policy
    pb = modules_pb2.BalancingPolicy()
    pb.simple_policy.SetInParent()

    pol = BalancingPolicy(pb)
    assert pol.simple_policy
    pol.validate()

    # test unique_policy
    pb = modules_pb2.BalancingPolicy()
    pb.unique_policy.SetInParent()

    pol = BalancingPolicy(pb)
    assert pol.unique_policy
    pol.validate()
    assert pol.list_policy_kinds() == ['unique_policy']

    # test timeout_policy
    pb = modules_pb2.BalancingPolicy()
    pb.timeout_policy.SetInParent()

    pol = BalancingPolicy(pb)
    assert pol.timeout_policy
    e = get_validation_exception(pol.validate)
    e.match('timeout: is required')

    pb.timeout_policy.timeout = '10s'
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate)
    e.match('timeout_policy -> balancing_policy: is required')

    pb.timeout_policy.balancing_policy.unique_policy.SetInParent()
    pol.update_pb(pb)
    pol.validate()

    # test active_policy
    pb = modules_pb2.BalancingPolicy()
    pb.active_policy.SetInParent()

    pol = BalancingPolicy(pb)
    e = get_validation_exception(pol.validate)
    e.match('active_policy -> balancing_policy: is required')

    pb.active_policy.balancing_policy.unique_policy.SetInParent()
    pol.update_pb(pb)
    pol.validate()

    pb.active_policy.skip_attempts = 3
    pol.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        pol.validate()
    e.match('active_policy -> skip_attempts: must be preceded by balancer2 module')

    balancer_pb = modules_pb2.Balancer2Module()
    balancer = Balancer2(balancer_pb)
    e = get_validation_exception(pol.validate, preceding_modules=[balancer])
    e.match('active_policy -> skip_attempts: can only be used with "hashing" balancing type')

    balancer_2_pb = clone_pb(balancer_pb)
    balancer_2 = Balancer2(balancer_2_pb)

    balancer_pb.hashing.SetInParent()
    balancer.update_pb(balancer_pb)

    e = get_validation_exception(pol.validate, preceding_modules=[balancer, balancer_2])
    e.match('active_policy -> skip_attempts: can only be used with "hashing" balancing type')

    pol.validate(preceding_modules=[balancer_2, balancer])

    pb.active_policy.skip_attempts = -3
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate, preceding_modules=[balancer])
    e.match('active_policy -> skip_attempts: must be non-negative')

    pb.active_policy.skip_attempts = 0
    pb.active_policy.f_skip_attempts.type = pb.active_policy.f_skip_attempts.GET_STR_VAR
    pb.active_policy.f_skip_attempts.get_str_var_params.var = 'some_var_name'
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate, preceding_modules=[balancer])
    e.match('active_policy -> skip_attempts: only the following functions allowed here: "count_backends"')

    pb.active_policy.f_skip_attempts.type = pb.active_policy.f_skip_attempts.COUNT_BACKENDS
    pb.active_policy.f_skip_attempts.ClearField('get_str_var_params')
    pb.active_policy.f_skip_attempts.count_backends_params.SetInParent()
    pol.update_pb(pb)
    pol.validate(preceding_modules=[balancer])
    assert pol.list_policy_kinds() == ['active_policy', 'unique_policy']

    with mock.patch.object(balancer, 'count_backends', return_value=100500):
        pol_config = pol.to_config(preceding_modules=[balancer])
        assert pol_config.table['active_policy'].table['skip_attempts'] == 100500
        assert pol_config.table['active_policy'].table['unique_policy'].table == {}

    # test by_name_policy
    pb = modules_pb2.BalancingPolicy()
    pb.by_name_policy.SetInParent()

    pol = BalancingPolicy(pb)
    e = get_validation_exception(pol.validate)
    e.match('name: is required')

    pb.by_name_policy.name = 'geo'
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate)
    e.match('by_name_policy -> balancing_policy: is required')

    pb.active_policy.balancing_policy.unique_policy.SetInParent()
    pol.update_pb(pb)
    pol.validate()
    assert pol.list_policy_kinds() == ['active_policy', 'unique_policy']

    pb.ClearField('active_policy')
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate)
    e.match(at_least_one_message)

    pb.by_name_from_header_policy.SetInParent()
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate)
    e.match('by_name_from_header_policy -> hints: is required')

    pb.by_name_from_header_policy.hints.add()
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate)
    e.match('by_name_from_header_policy -> balancing_policy: is required')

    pb.by_name_from_header_policy.balancing_policy.unique_policy.SetInParent()
    pol.update_pb(pb)
    e = get_validation_exception(pol.validate)
    e.match(r'by_name_from_header_policy -> hints\[0\] -> hint: is required')

    pb.by_name_from_header_policy.hints[0].hint = 'pum'
    pb.by_name_from_header_policy.hints[0].backend = 'purum'
    pol.update_pb(pb)
    pol.validate()

    assert pol.list_policy_kinds() == ['by_name_from_header_policy', 'unique_policy']


def test_swat_5935_regression():
    instance_macro_pb = modules_pb2.InstanceMacro()
    instance_macro_pb.sd.SetInParent()
    instance_macro_pb.unistat.SetInParent()
    instance_macro = InstanceMacro(instance_macro_pb)

    pb = modules_pb2.Balancer2Module()
    pb.connection_attempts = 1
    pb.generated_proxy_backends.proxy_options.SetInParent()
    pb.generated_proxy_backends.endpoint_sets.add(cluster='sas', id='test-1')
    pb.generated_proxy_backends.endpoint_sets.add(cluster='man', id='test-2')
    pb.active.request = r'GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n'
    pb.active.delay = '20s'
    pb.f_attempts.type = pb.f_attempts.COUNT_BACKENDS
    pb.f_attempts.count_backends_params.compat_enable_sd_support.value = False

    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(instance_macro,))
    e.match(r'attempts: !f count_backends\(false\) can not be used with endpoint sets')

    pb.f_attempts.count_backends_params.ClearField('compat_enable_sd_support')
    m.validate(preceding_modules=(instance_macro,))

    pb.f_attempts.type = pb.f_attempts.COUNT_BACKENDS_SD
    pb.f_attempts.count_backends_sd_params.SetInParent()
    m = wrap(pb)
    m.validate(preceding_modules=(instance_macro,))

    pb.ClearField('f_attempts')
    pb.attempts = 1
    pb.active.quorum.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.quorum.f_value.get_total_weight_percent_params.value = 50
    m = wrap(pb)

    e = get_validation_exception(m.validate, preceding_modules=(instance_macro,))
    e.match(r'active -> quorum: !f get_total_weight_percent\(\) can not be used with endpoint sets')

    pb.active.quorum.ClearField('f_value')
    pb.active.quorum.value = 5
    pb.active.hysteresis.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.hysteresis.f_value.get_total_weight_percent_params.value = 50

    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(instance_macro,))
    e.match(r'active -> hysteresis: !f get_total_weight_percent\(\) can not be used with endpoint sets')

    pb.active.hysteresis.ClearField('f_value')
    pb.active.hysteresis.value = 5
    m = wrap(pb)
    m.validate(preceding_modules=(instance_macro,))


def test_generated_proxy_backends_2():
    pb = modules_pb2.GeneratedProxyBackends()
    m = GeneratedProxyBackends(pb)
    e = get_validation_exception(m.validate)
    e.match('proxy_options: is required')

    pb.proxy_options.SetInParent()
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('at least one of the "endpoint_sets", "gencfg_groups", "include_backends", "instances", '
            '"nanny_snapshots" must be specified')

    pb.nanny_snapshots.add()
    pb.gencfg_groups.add()
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('at most one of the "endpoint_sets", "gencfg_groups", "include_backends", "instances", '
            '"nanny_snapshots" must be specified')

    del pb.gencfg_groups[:]
    m.update_pb(pb)

    with mock.patch.object(GeneratedProxyBackendsProxyOptions, 'validate') as v1, \
            mock.patch.object(GeneratedProxyBackendsNannySnapshot, 'validate') as v2:
        m.validate()

    assert v1.called
    assert v2.called


def test_proxy_options():
    pb = modules_pb2.GeneratedProxyBackends.ProxyOptions()

    m = GeneratedProxyBackendsProxyOptions(pb)
    m.validate()

    pb.https_settings.SetInParent()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('https_settings -> ca_file: is required')

    pb.https_settings.ca_file = 'xxx'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('https_settings -> verify_depth: is required')

    pb.https_settings.verify_depth = -1
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('https_settings -> verify_depth: must be positive')

    pb.https_settings.verify_depth = 20
    m.update_pb(pb)
    m.validate()

    pb.backend_timeout = 'x'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('backend_timeout: "x" is not a valid timedelta string')

    pb.backend_timeout = '1s'
    pb.connect_timeout = 'x'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('connect_timeout: "x" is not a valid timedelta string')

    pb.connect_timeout = '20ms'
    pb.resolve_timeout = 'x'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('resolve_timeout: "x" is not a valid timedelta string')

    pb.resolve_timeout = '1000s'
    pb.keepalive_count = -1
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('keepalive_count: must be non-negative')

    pb.keepalive_count = 5
    pb.fail_on_5xx.value = True
    pb.buffering = True
    pb.status_code_blacklist.append('XXX')
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('status_code_blacklist: unknown status code or family: XXX')

    pb.status_code_blacklist[0] = '2xx'
    m.validate()

    pb.status_code_blacklist_exceptions.append('XXX')
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('status_code_blacklist_exceptions: unknown status code or family: XXX')

    pb.status_code_blacklist_exceptions[0] = '500'
    m.validate()


def test_generated_proxy_backends():
    pb = modules_pb2.GeneratedProxyBackends()
    pb.proxy_options.connect_timeout = '100ms'
    pb.proxy_options.https_settings.verify_depth = 1
    pb.proxy_options.https_settings.ca_file = 'test.ca'
    m = GeneratedProxyBackends(pb)

    e = get_validation_exception(m.validate)
    e.match('at least one of the "endpoint_sets", "gencfg_groups", "include_backends", "instances", '
            '"nanny_snapshots" must be specified')

    pb.include_backends.SetInParent()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('include_backends -> type: must be equal to "BY_ID"')

    pb.include_backends.type = pb.include_backends.BY_ID
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('include_backends -> ids: is required')

    pb.include_backends.ids.append('')
    e = get_validation_exception(m.validate)
    e.match(r'include_backends -> ids\[0\]: is required')

    pb.include_backends.ids[0] = 'namespace-1/backend-1/uh-oh'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'include_backends -> ids\[0\]: must contain at most one slash')

    for backend_id in ('backend-1', 'namespace-1/backend-1'):
        pb.include_backends.ids[0] = backend_id
        m.update_pb(pb)
        m.validate()

    pb.nanny_snapshots.add()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('at most one of the "endpoint_sets", "gencfg_groups", "include_backends", "instances", '
            '"nanny_snapshots" must be specified')

    pb.ClearField('include_backends')
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'nanny_snapshots\[0\] -> service_id: is required')

    pb.nanny_snapshots[0].service_id = 'service-id'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'nanny_snapshots\[0\] -> snapshot_id: is required')

    pb.nanny_snapshots[0].snapshot_id = 'snapshot-id'
    m.update_pb(pb)
    m.validate()

    pb.ClearField('nanny_snapshots')
    pb.gencfg_groups.add()
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'gencfg_groups\[0\] -> name: is required')

    pb.gencfg_groups[0].name = 'name'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'gencfg_groups\[0\] -> version: is required')

    pb.gencfg_groups[0].version = 'version'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match(r'gencfg_groups\[0\] -> version: must be either equal to "trunk" or start with "tags/" prefix')

    pb.gencfg_groups[0].version = 'tags/stable-101-r315'
    m.update_pb(pb)
    m.validate()


def test_nanny_snapshot():
    pb = modules_pb2.GeneratedProxyBackends.NannySnapshot()
    m = wrap(pb)

    e = get_validation_exception(m.validate)
    e.match('service_id: is required')

    pb.service_id = 'x'
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('snapshot_id: is required')

    pb.snapshot_id = 'x'
    m.update_pb(pb)

    m.validate()

    pb.port.value = -1
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('port: is not a valid port')


def test_gencfg_group():
    pb = modules_pb2.GeneratedProxyBackends.GencfgGroup()
    m = wrap(pb)

    e = get_validation_exception(m.validate)
    e.match('name: is required')

    pb.name = 'x'
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('version: is required')

    pb.version = 'x'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('version: must be either equal to "trunk" or start with "tags/" prefix')

    pb.version = 'tags/x'
    m.update_pb(pb)
    m.validate()

    pb.port.value = -1
    m.update_pb(pb)

    e = get_validation_exception(m.validate)
    e.match('port: is not a valid port')


def test_instance():
    pb = modules_pb2.GeneratedProxyBackends.Instance()
    m = wrap(pb)

    e = get_validation_exception(m.validate)
    e.match('weight: is required and must not be 0; please use -1 instead of 0 to assign zero weight to backend')
    m.update_pb(pb)

    pb.weight = 123
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('host: is required')

    pb.host = 'x'
    m.update_pb(pb)
    e = get_validation_exception(m.validate)

    e.match('port: is required')
    pb.port = 123
    m.update_pb(pb)
    m.validate()

    pb.port = -123
    m.update_pb(pb)
    e = get_validation_exception(m.validate)
    e.match('port: is not a valid port')


def test_active_with_generated_proxy_backends():
    pb = modules_pb2.Balancer2Module(attempts=2)
    pb.connection_attempts = 1
    pb.generated_proxy_backends.proxy_options.SetInParent()
    pb.generated_proxy_backends.instances.add(host='ya.ru', port=80, weight=100.5)
    pb.generated_proxy_backends.instances.add(host='google.com', port=80, weight=2)
    pb.active.request = r'GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n'
    pb.active.delay = '20s'

    balancer2 = Balancer2(pb)
    balancer2.validate()

    assert balancer2.generated_proxy_backends.get_total_weight(allow_different_backend_weights=True) == 102.5
    with pytest.raises(ValidationError):
        balancer2.generated_proxy_backends.get_total_weight()

    assert balancer2.get_total_weight(allow_different_backend_weights=True) == 102.5
    with pytest.raises(ValidationError):
        balancer2.get_total_weight()

    pb.active.hysteresis.value = 2
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match('active -> hysteresis: absolute value is not allowed, please use get_total_weight_percent')

    pb.active.quorum.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.quorum.f_value.get_total_weight_percent_params.value = -50
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match(r'get_total_weight_percent\'s 1st argument \(value\) must be non-negative')

    pb.active.quorum.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.quorum.f_value.get_total_weight_percent_params.value = 50
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match('weights of instances ya.ru:80 and google.com:80 differ. '
            'if this difference is expected and you are sure that specified quorum and '
            'hysteresis percentages are still valid, please set "allow_different_backend_weights" to true')

    pb.active.quorum.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = True
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match('active -> hysteresis: absolute value is not allowed')

    pb.active.quorum.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.quorum.f_value.get_total_weight_percent_params.value = 50
    pb.active.quorum.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = True
    pb.active.hysteresis.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.hysteresis.f_value.get_total_weight_percent_params.value = 10
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match('weights of instances ya.ru:80 and google.com:80 differ. '
            'if this difference is expected and you are sure that specified quorum and '
            'hysteresis percentages are still valid, please set "allow_different_backend_weights" to true')

    pb.active.hysteresis.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = True
    balancer2.update_pb(pb)
    balancer2.validate()

    config = balancer2.to_config()
    assert config.table['active'].table == {
        'delay': '20s',
        'request': u'GET /ping HTTP/1.1\\nHost: beta.mobsearch.yandex.ru\\n\\n',
        'quorum': 51.25,
        'hysteresis': 10.25,
    }

    pb.generated_proxy_backends.instances[0].weight = 10.000000001
    pb.generated_proxy_backends.instances[1].weight = 9.999999999
    pb.active.quorum.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = False
    pb.active.quorum.f_value.get_total_weight_percent_params.ClearField('allow_different_backend_weights')
    balancer2.update_pb(pb)
    balancer2.validate()
    config = balancer2.to_config()
    assert config.table['active'].table == {
        'delay': '20s',
        'request': u'GET /ping HTTP/1.1\\nHost: beta.mobsearch.yandex.ru\\n\\n',
        'quorum': 10,
        'hysteresis': 2,
    }


def test_active_with_backends():
    pb = modules_pb2.Balancer2Module(attempts=2)
    pb.connection_attempts = 1
    backend_1_pb = pb.backends.add(name='backend-1', weight=1)
    backend_1_pb.nested.errordocument.status = 200
    backend_2_pb = pb.backends.add(name='backend-2', weight=100)
    backend_2_pb.nested.errordocument.status = 201
    pb.active.request = r'GET /ping HTTP/1.1\nHost: beta.mobsearch.yandex.ru\n\n'
    pb.active.delay = '20s'

    balancer2 = Balancer2(pb)
    balancer2.validate()

    assert balancer2.get_total_weight(allow_different_backend_weights=True) == 101
    with pytest.raises(ValidationError):
        balancer2.get_total_weight()

    pb.active.quorum.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.quorum.f_value.get_total_weight_percent_params.value = 50
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match(r'weights of backends\[0\] and backends\[1\] differ. '
            r'if this difference is expected and you are sure that specified quorum and '
            r'hysteresis percentages are still valid, please set "allow_different_backend_weights" to true')

    pb.active.quorum.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = True
    balancer2.update_pb(pb)
    balancer2.validate()

    config = balancer2.to_config()
    assert config.table['active'].table.pop('backend-1')
    assert config.table['active'].table.pop('backend-2')
    assert config.table['active'].table == {
        'delay': '20s',
        'request': u'GET /ping HTTP/1.1\\nHost: beta.mobsearch.yandex.ru\\n\\n',
        'quorum': 50.5,
    }

    pb.active.hysteresis.f_value.type = modules_pb2.Call.GET_TOTAL_WEIGHT_PERCENT
    pb.active.hysteresis.f_value.get_total_weight_percent_params.value = 10
    balancer2.update_pb(pb)
    e = get_validation_exception(balancer2.validate)
    e.match(r'weights of backends\[0\] and backends\[1\] differ. '
            r'if this difference is expected and you are sure that specified quorum and '
            r'hysteresis percentages are still valid, please set "allow_different_backend_weights" to true')

    pb.active.hysteresis.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = True
    balancer2.update_pb(pb)
    balancer2.validate()

    config = balancer2.to_config()
    assert config.table['active'].table.pop('backend-1')
    assert config.table['active'].table.pop('backend-2')
    assert config.table['active'].table == {
        'delay': '20s',
        'request': u'GET /ping HTTP/1.1\\nHost: beta.mobsearch.yandex.ru\\n\\n',
        'quorum': 50.5,
        'hysteresis': 10.1,
    }

    pb.backends[0].weight = 10.000000001
    pb.backends[1].weight = 9.999999999
    pb.active.quorum.f_value.get_total_weight_percent_params.allow_different_backend_weights.value = False
    pb.active.quorum.f_value.get_total_weight_percent_params.ClearField('allow_different_backend_weights')
    balancer2.update_pb(pb)
    balancer2.validate()
    config = balancer2.to_config()
    assert config.table['active'].table.pop('backend-1')
    assert config.table['active'].table.pop('backend-2')
    assert config.table['active'].table == {
        'delay': '20s',
        'request': u'GET /ping HTTP/1.1\\nHost: beta.mobsearch.yandex.ru\\n\\n',
        'quorum': 10,
        'hysteresis': 2,
    }


def test_count_backends_sd():
    balancer2_pb = modules_pb2.Balancer2Module()
    balancer2_pb.rr.SetInParent()
    balancer2_pb.generated_proxy_backends.endpoint_sets.add(cluster='sas', id='xxx')

    balancer2_pb.f_attempts.type = balancer2_pb.f_attempts.COUNT_BACKENDS
    balancer2_pb.f_attempts.count_backends_params.compat_enable_sd_support.value = False

    balancer2 = wrap(balancer2_pb)
    e = get_validation_exception(balancer2.validate)
    assert six.text_type(e.value) == (
        u'attempts: !f count_backends(false) can not be used with endpoint sets, '
        u'please use !f count_backends() instead')

    balancer2_pb = modules_pb2.Balancer2Module()
    balancer2_pb.rr.SetInParent()
    balancer2_pb.generated_proxy_backends.instances.add(host='ya.ru', port=80)

    balancer2_pb.f_attempts.type = balancer2_pb.f_attempts.COUNT_BACKENDS_SD
    balancer2_pb.f_attempts.count_backends_sd_params.SetInParent()

    balancer2 = wrap(balancer2_pb)
    e = get_validation_exception(balancer2.validate)
    assert six.text_type(e.value) == (
        u'attempts: !f count_backends_sd() can only be used with endpoint sets, '
        u'please use !f count_backends() instead')

    balancer2_pb.attempts = 3
    balancer2_pb.ClearField('f_attempts')
    balancer2_pb.f_connection_attempts.type = balancer2_pb.f_connection_attempts.COUNT_BACKENDS_SD
    balancer2_pb.f_connection_attempts.count_backends_sd_params.SetInParent()
    balancer2 = wrap(balancer2_pb)
    e = get_validation_exception(balancer2.validate)
    assert six.text_type(e.value) == (
        u'connection_attempts: !f count_backends_sd() can only be used with endpoint sets, '
        u'please use !f count_backends() instead')

    balancer2_pb.ClearField('f_connection_attempts')
    balancer2_pb.f_fast_attempts.type = balancer2_pb.f_fast_attempts.COUNT_BACKENDS_SD
    balancer2_pb.f_fast_attempts.count_backends_sd_params.SetInParent()
    balancer2 = wrap(balancer2_pb)
    e = get_validation_exception(balancer2.validate)
    assert six.text_type(e.value) == (
        u'fast_attempts: !f count_backends_sd() can only be used with endpoint sets, '
        u'please use !f count_backends() instead')


def test_attempts_rate_limiter():
    pb = modules_pb2.AttemptsRateLimiter()
    m = wrap(pb)
    e = get_validation_exception(m.validate)
    e.match('limit: is required')

    balancer2_pb = modules_pb2.Balancer2Module()
    balancer2_pb.rr.SetInParent()
    balancer2_pb.f_attempts.type = balancer2_pb.f_attempts.COUNT_BACKENDS
    balancer2_pb.f_attempts.count_backends_params.SetInParent()
    balancer2_pb.generated_proxy_backends.endpoint_sets.add(cluster='sas', id='xxx')
    balancer2 = wrap(balancer2_pb)

    pb.limit = 2
    m = wrap(pb)
    m.validate(preceding_modules=(balancer2,))

    balancer2_pb.ClearField('f_attempts')
    balancer2_pb.attempts = 10
    balancer2 = wrap(balancer2_pb)

    m.validate(preceding_modules=(balancer2,))

    pb.limit = 40
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('limit: must be less or equal to 3')

    pb.limit = 0
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('limit: is required')

    pb.limit = -1
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('must be greater than 0')

    pb.limit = 1
    pb.coeff = 1
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('must be less than 1')

    pb.coeff = -1
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('must be greater than 0')

    pb.coeff = 0
    m = wrap(pb)
    m.validate(preceding_modules=(balancer2,))
    c = m.to_config()

    assert c.table == {
        'limit': 1,
        'coeff': m.DEFAULT_COEFF,
        'switch_default': True,
    }
    pb.coeff = 0.5
    m = wrap(pb)
    m.validate(preceding_modules=(balancer2,))
    c = m.to_config()

    assert c.table == {
        'limit': 1,
        'coeff': 0.5,
        'switch_default': True,
    }

    pb.max_budget = 2
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('at most one of "coeff", "max_budget" must be specified')

    pb.coeff = 0
    m = wrap(pb)
    m.validate(preceding_modules=(balancer2,))
    c = m.to_config()

    assert c.table == {
        'limit': 1,
        'max_budget': 2,
        'switch_default': True,
    }

    pb.max_budget = 2
    pb.coeff = 1
    m = wrap(pb)
    e = get_validation_exception(m.validate, preceding_modules=(balancer2,))
    e.match('at most one of "coeff", "max_budget" must be specified')

    pb.coeff = 0
    m = wrap(pb)
    m.validate(preceding_modules=(balancer2,))
    c = m.to_config()

    assert c.table == {
        'limit': 1,
        'max_budget': 2,
        'switch_default': True,
    }

    pb.max_budget = 0
    balancer2.pb.hedged_delay = '10s'
    m = wrap(balancer2.pb)
    e = get_validation_exception(m.validate, preceding_modules=())
    e.match('"attempts_rate_limiter" should be used if "hedged_delay" specified')

    balancer2.pb.attempts_rate_limiter.limit = 0.2
    m = wrap(balancer2.pb)
    e = get_validation_exception(m.validate, preceding_modules=())
    e.match('"attempts_rate_limiter.max_budget" should be used if "hedged_delay" specified')


def test_ignore_duplicates():
    pb = modules_pb2.GeneratedProxyBackends()
    pb.proxy_options.connect_timeout = '100ms'
    pb.proxy_options.https_settings.verify_depth = 1
    pb.proxy_options.https_settings.ca_file = 'test.ca'
    for _ in range(2):
        instance = pb.instances.add()
        instance.host = '127.0.0.1'
        instance.port = 8081

    m = GeneratedProxyBackends(pb)
    e = get_validation_exception(m.validate)
    e.match('duplicate host and port: 127.0.0.1:8081')

    pb.ignore_duplicates = True
    m.update_pb(pb)
    m.validate()

    c = m.to_config()
    assert len(c.array.args[0].array) == 1
    assert c.array.args[0].array[0].array == ['127.0.0.1', 8081, 0]


def test_delay_settings():
    pb = modules_pb2.Balancer2Module(attempts=2)
    pb.weighted2.min_weight = 100
    pb.generated_proxy_backends.instances.add(host='127.0.0.1', port=8080)
    pb.connection_attempts = 1
    pb.generated_proxy_backends.proxy_options.keepalive_count = 1

    pb.delay_settings.first_delay = '10ms'
    balancer2 = Balancer2(pb)

    with pytest.raises(ValidationError) as e:
        balancer2.validate()
    e.match('delay_multiplier: is required')

    pb.delay_settings.delay_multiplier = 1
    pb.delay_settings.max_random_delay = '0s'

    balancer2.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        balancer2.validate()
    e.match('delay_on_fast: is required')

    pb.delay_settings.delay_on_fast.value = False
    balancer2.update_pb(pb)
    balancer2.validate()

    pb.delay_settings.max_random_delay = '100s'
    balancer2.update_pb(pb)
    with pytest.raises(ValidationError) as e:
        balancer2.validate()
    e.match('must be less or equal to 10s')


def test_dynamic():
    pb = modules_pb2.Balancer2Module(attempts=2)
    pb.dynamic.max_pessimized_share = .1
    pb.generated_proxy_backends.instances.add(host='127.0.0.1', port=8080)
    pb.connection_attempts = 1
    pb.generated_proxy_backends.proxy_options.keepalive_count = 1
    balancer2 = Balancer2(pb)

    with pytest.raises(ValidationError) as e:
        balancer2.validate()
    e.match('dynamic -> backends_name: must be set if balancer2.generated_proxy_backends.include_backends is not used')

    pb.generated_proxy_backends.ClearField('instances')
    pb.generated_proxy_backends.include_backends.type = pb.generated_proxy_backends.include_backends.BY_ID
    pb.generated_proxy_backends.include_backends.ids.extend(['backend_sas', 'backend_vla'])
    balancer2.update_pb(pb)

    balancer2.validate()

    assert balancer2.generated_proxy_backends.include_backends.get_dynamic_backends_name() == 'backend_sas#backend_vla'

    pb.generated_proxy_backends.ClearField('include_backends')
    pb.generated_proxy_backends.include_backends.ids.extend(['backend_vla', 'backend_sas'])
    balancer2.update_pb(pb)
    assert balancer2.generated_proxy_backends.include_backends.get_dynamic_backends_name() == 'backend_sas#backend_vla'

    pb.generated_proxy_backends.ClearField('include_backends')
    pb.generated_proxy_backends.include_backends.ids.extend(['only_backend'])
    balancer2.update_pb(pb)
    assert balancer2.generated_proxy_backends.include_backends.get_dynamic_backends_name() == 'only_backend'

    pb.generated_proxy_backends.ClearField('include_backends')
    pb.generated_proxy_backends.include_backends.ids.extend(['backend_vla', 'backend_sas', 'backend_man', 'backend_iva'])
    balancer2.update_pb(pb)
    assert balancer2.generated_proxy_backends.include_backends.get_dynamic_backends_name() == ('backend_iva#backend_man#'
                                                                                               'backend_sas#backend_vla')
