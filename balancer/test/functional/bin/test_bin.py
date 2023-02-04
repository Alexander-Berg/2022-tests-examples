# -*- coding: utf-8 -*-
import os
import pytest
import time

import configs

from balancer.test.util.list_balancing_modes import list_balancing_modes
from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.sd import SDCacheConfig


@pytest.mark.parametrize('mode', ['basic', 'extended'])
def test_valid_config(ctx, mode):
    """
    BALANCER-869
    If config is valid, "balancer -K" should exit with return code 0
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    result = ctx.manager.balancer.check_config(configs.OkConfig(mode=mode))
    assert result.return_code == 0


@pytest.mark.parametrize(
    'config',
    [
        configs.NotLuaConfig(),
        configs.UnknownParamConfig(),
        configs.UnknownModuleConfig(),
        configs.InvalidParamValueConfig(),
        configs.MissingRequiredParamConfig(),
        configs.NoInstanceConfig(),
        configs.NonexistentConfig(),
    ],
    ids=[
        'not_lua',
        'unknown_param',
        'unknown_module',
        'invalid_param_value',
        'missing_required_param',
        'no_instance_config',
        'nonexistent',
    ],
)
def test_not_valid_config(ctx, config):
    """
    BALANCER-869
    If config is not valid, "balancer -K" should fail with exit code 1
    """
    result = ctx.manager.balancer.check_config(config)
    assert result.return_code == 1


@pytest.mark.parametrize(
    ('mode', 'opts', 'ret'),
    [
        ('basic', [], 0),
        ('extended', [], 1),
        ('extended', ['--skip-backend-checks'], 1),
        ('extended', ['--skip-ip-checks'], 0),
        ('extended', ['--skip-extended-checks'], 0),
    ],
    ids=[
        'basic',
        'extended',
        'skip-backend-checks',
        'skip-ip-checks',
        'skip-extended-checks'
    ]
)
def test_bind_fail(ctx, mode, opts, ret):
    """
    BALANCER-2920
    """
    result = ctx.manager.balancer.check_config(configs.BindFail(mode=mode), opts)
    assert result.return_code == ret


def test_ignore_bind_errors(ctx):
    """
    BALANCER-2920 BALANCER-2820
    """
    ignore_bind_errors_file = ctx.manager.fs.create_file('ignore_bind_errors.txt')
    result = ctx.manager.balancer.check_config(configs.BindFail(ignore_bind_errors_file=ignore_bind_errors_file))
    assert result.return_code == 1
    ctx.manager.fs.rewrite(ignore_bind_errors_file, '0100::10')
    result = ctx.manager.balancer.check_config(configs.BindFail(ignore_bind_errors_file=ignore_bind_errors_file))
    assert result.return_code == 0


@pytest.mark.parametrize(
    ['quorum', 'amount_quorum', 'real', 'fake', 'mode', 'opts'],
    [
        (0, 0, 0, 1, 'extended', []),
        (0.5, 1, 1, 1, 'extended', []),
        (0.5, 3, 3, 3, 'extended', []),
        (1, 1, 1, 0, 'extended', []),
        (1, 6, 6, 0, 'extended', []),
        (1, 1, 0, 1, 'basic', []),
        (1, 1, 0, 1, 'extended', ['--skip-backend-checks']),
        (1, 1, 0, 1, 'extended', ['--skip-extended-checks']),
    ],
    ids=[
        "skip",
        "half-1",
        "half-6",
        "full-1",
        "full-6",
        'basic',
        'skip-backend-checks',
        'skip-extended-checks',
    ]
)
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_backends_check_ok(ctx, algo, quorum, amount_quorum, real, fake, mode, opts):
    """
    BALANCER-2898
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='fake')

    result = ctx.manager.balancer.check_config(configs.BackendsCheck(
        mode=mode, algo=algo,
        quorum1=quorum, amount_quorum1=amount_quorum, real1=real, fake1=fake,
        quorum2=quorum, amount_quorum2=amount_quorum, real2=real, fake2=fake
    ), opts=opts)
    if mode == 'extended' and not opts:
        # a single check per group
        for g in ("group1", "group2"):
            assert result.stderr.count("CHECK   {}".format(g)) == 1
            if quorum:
                assert result.stderr.count("ERROR   {}".format(g)) == fake
                assert result.stderr.count("SUCCESS {}".format(g)) == 1
            else:
                assert result.stderr.count("SKIPPED {}".format(g)) == 1
    assert result.return_code == 0


@pytest.mark.parametrize(
    'skip_same_groups',
    [None, True, False],
    ids=['default', 'on', 'off']
)
def test_backends_check_same_groups(ctx, skip_same_groups):
    """
    BALANCER-2898
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='fake')

    result = ctx.manager.balancer.check_config(configs.BackendsCheck(
        mode='extended', algo='rr', skip_same_groups=skip_same_groups,
        quorum1=1, real1=1, fake1=0, group1='group',
        quorum2=1, real2=1, fake2=0, group2='group',
    ), opts=[])
    if skip_same_groups in (None, True):
        assert result.stderr.count("CHECK   group") == 1
        assert result.stderr.count("SUCCESS group") == 1
        assert result.stderr.count("SKIPPED group") == 1
    else:
        assert result.stderr.count("CHECK   group") == 2
        assert result.stderr.count("SUCCESS group") == 2
        assert result.stderr.count("SKIPPED group") == 0
    assert result.return_code == 0


@pytest.mark.parametrize(
    ('quorum1', 'amount_quorum1', 'real1', 'fake1', 'quorum2', 'amount_quorum2', 'real2', 'fake2', 'opts'),
    [
        (0.00, 0, 0, 0, 0.50, 0, 3, 3, []),
        (0.51, 0, 3, 3, 0.50, 0, 3, 3, []),
        (0.50, 0, 3, 3, 0.00, 0, 0, 0, []),
        (0.50, 0, 3, 3, 0.51, 0, 3, 3, []),
        (0.50, 0, 3, 3, 0.51, 0, 3, 3, ['--skip-ip-checks']),
        (0.00, 0, 0, 0, 0.00, 3, 3, 3, []),
        (0.00, 4, 3, 3, 0.00, 3, 3, 3, []),
        (0.00, 3, 3, 3, 0.00, 0, 0, 0, []),
        (0.00, 3, 3, 3, 0.00, 4, 3, 3, []),
    ],
    ids=[
        "empty-1",
        "few-1",
        "empty-2",
        "few-2",
        "skip-ip-checks",
        "qty-empty-1",
        "qty-few-1",
        "qty-empty-2",
        "qty-few-2",
    ]
)
@pytest.mark.parametrize('algo', list_balancing_modes())
def test_backends_check_fail(ctx, algo, quorum1, amount_quorum1, real1, fake1, quorum2, amount_quorum2, real2, fake2, opts):
    """
    BALANCER-2898
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='fake')

    result = ctx.manager.balancer.check_config(configs.BackendsCheck(
        algo=algo,
        quorum1=quorum1, amount_quorum1=amount_quorum1, real1=real1, fake1=fake1,
        quorum2=quorum2, amount_quorum2=amount_quorum2, real2=real2, fake2=fake2
    ), opts=opts)

    if (real1 + fake1) * (real2 + fake2):
        assert result.stderr.count("CHECK   group1") == 1
        assert result.stderr.count("CHECK   group2") == 1
        if real1 < quorum1 * (real1 + fake1) or real1 < amount_quorum1:
            assert result.stderr.count("FAILED  group1") == 1
        else:
            assert result.stderr.count("SUCCESS group1") == 1
        if real2 < quorum2 * (real2 + fake2) or real2 < amount_quorum2:
            assert result.stderr.count("FAILED  group2") == 1
        else:
            assert result.stderr.count("SUCCESS group2") == 1
        assert result.stderr.count("ERROR   group1") == fake1
        assert result.stderr.count("ERROR   group2") == fake2
    else:
        # zero backends config triggers balancer2 config error
        assert result.stderr.count('CHECK   group1') == 0
        assert result.stderr.count('CHECK   group2') == 0
    assert result.return_code == 1


def test_quorum_overrides(ctx):
    """
    BALANCER-2898
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='fake')
    quorums_file = ctx.manager.fs.create_file('quorums_file.txt')

    def run_check(fail_quorum=True, fail_amount=False):
        quorum1=quorum2=0.5 if fail_quorum else 0.4
        amount_quorum1=amount_quorum2=4 if fail_amount else 3
        return ctx.manager.balancer.check_config(configs.BackendsCheck(
            algo='rr', quorums_file=quorums_file,
            quorum1=quorum1, amount_quorum1=amount_quorum1, real1=3, fake1=4,
            quorum2=quorum2, amount_quorum2=amount_quorum2, real2=3, fake2=4
        ))

    result = run_check()
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("FAILED  group1") == 1
    assert result.stderr.count("FAILED  group2") == 1
    assert result.return_code == 1

    result = run_check(False, True)
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("FAILED  group1") == 1
    assert result.stderr.count("FAILED  group2") == 1
    assert result.return_code == 1

    ctx.manager.fs.rewrite(quorums_file, "*,0.4")
    result = run_check()
    assert result.stderr.count("QUORUM  *,0.4,-") == 2
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("SUCCESS group1") == 1
    assert result.stderr.count("SUCCESS group2") == 1
    assert result.return_code == 0

    ctx.manager.fs.rewrite(quorums_file, "group1,0.4")
    result = run_check()
    assert result.stderr.count("QUORUM  group1,0.4,-") == 1
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("SUCCESS group1") == 1
    assert result.stderr.count("FAILED  group2") == 1
    assert result.return_code == 1

    ctx.manager.fs.rewrite(quorums_file, "group2,0.4")
    result = run_check()
    assert result.stderr.count("QUORUM  group2,0.4,-") == 1
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("FAILED  group1") == 1
    assert result.stderr.count("SUCCESS group2") == 1
    assert result.return_code == 1

    ctx.manager.fs.rewrite(quorums_file, "group1,0.4\ngroup2,0.4")
    result = run_check()
    assert result.stderr.count("QUORUM  group1,0.4,-") == 1
    assert result.stderr.count("QUORUM  group2,0.4,-") == 1
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("SUCCESS group1") == 1
    assert result.stderr.count("SUCCESS group2") == 1
    assert result.return_code == 0

    ctx.manager.fs.rewrite(quorums_file, "*,-,2")
    result = run_check(False, True)
    assert result.stderr.count("QUORUM  *,-,2") == 2
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("SUCCESS group1") == 1
    assert result.stderr.count("SUCCESS group2") == 1
    assert result.return_code == 0

    ctx.manager.fs.rewrite(quorums_file, "group1,,2")
    result = run_check(False, True)
    assert result.stderr.count("QUORUM  group1,-,2") == 1
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("SUCCESS group1") == 1
    assert result.stderr.count("FAILED  group2") == 1
    assert result.return_code == 1

    ctx.manager.fs.rewrite(quorums_file, "group2,-,2")
    result = run_check(False, True)
    assert result.stderr.count("QUORUM  group2,-,2") == 1
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("FAILED  group1") == 1
    assert result.stderr.count("SUCCESS group2") == 1
    assert result.return_code == 1

    ctx.manager.fs.rewrite(quorums_file, "group1,0.4,2\ngroup2,-,2")
    result = run_check(False, True)
    assert result.stderr.count("QUORUM  group1,0.4,2") == 1
    assert result.stderr.count("QUORUM  group2,-,2") == 1
    assert result.stderr.count("ERROR   group1") == 4
    assert result.stderr.count("ERROR   group2") == 4
    assert result.stderr.count("SUCCESS group1") == 1
    assert result.stderr.count("SUCCESS group2") == 1
    assert result.return_code == 0


def gen_sd_endpointset(backends):
    return [{
        'fqdn': 'localhost',
        'port': b.server_config.port,
        'ip4_address': "127.0.0.1",
        'ip6_address': "::1",
    } for b in backends]


def render_sd_endpointset(eps):
    return "endpoint_set {{\n  {}\n}}\n".format("\n  ".join([
        'endpoints {{ fqdn: "{fqdn}" port: {port} ip4_address: "{ip4_address}" ip6_address: "{ip6_address}" }}'
        .format(**e) for e in eps
    ]))


def init_sd_server(ctx, endpoint_sets):
    for clust, serv, eps in endpoint_sets:
        ctx.sd.state.set_endpointset(
            clust, serv, eps
        )


def check_config_sd(ctx, algo, quorum, real, fake, sd_mode, allow_empty_endpoint_sets=None):
    ctx.start_backend(SDCacheConfig(), name='sd')
    for i in range(real):
        ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real{}'.format(i + 1))

    for i in range(fake):
        ctx.start_fake_backend(name='fake{}'.format(i + 1))

    sd_cache = ctx.manager.fs.create_dir('sd_cache')

    fake_backends = [getattr(ctx, 'fake{}'.format(i + 1)) for i in range(fake)]
    real_backends = [getattr(ctx, 'real{}'.format(i + 1)) for i in range(real)]

    if sd_mode == 'static':
        return sd_cache, ctx.manager.balancer.check_config(configs.BackendsCheckSD(
            algo=algo, sd_cache=sd_cache, quorum=quorum, real=real, fake=fake,
            allow_empty_endpoint_sets=allow_empty_endpoint_sets
        ))
    elif sd_mode == 'file':
        backends_file = ctx.manager.fs.create_file('backends_file')
        ctx.manager.fs.rewrite(backends_file, render_sd_endpointset(gen_sd_endpointset(
            fake_backends + real_backends
        )))
        return sd_cache, ctx.manager.balancer.check_config(configs.BackendsCheckSD(
            algo=algo, sd_cache=sd_cache, quorum=quorum, backends_file=backends_file,
            allow_empty_endpoint_sets=allow_empty_endpoint_sets
        ))
    elif sd_mode.startswith('server'):
        a, b = '1', '2'
        if sd_mode.endswith('2'):
            b, a = a, b
        eps = [
            ('cluster' + a, 'service' + a, gen_sd_endpointset(fake_backends)),
            ('cluster' + b, 'service' + b, gen_sd_endpointset(real_backends)),
        ]
        init_sd_server(ctx, endpoint_sets=eps)
        return sd_cache, ctx.manager.balancer.check_config(configs.BackendsCheckSD(
            algo=algo, sd_cache=sd_cache, quorum=quorum, endpoint_sets=2,
            allow_empty_endpoint_sets=allow_empty_endpoint_sets
        ))
    else:
        assert False


SD_MODES = [
    'static',
    'file',
    'server1',
    'server2',
]


@pytest.mark.parametrize('algo', list_balancing_modes())
@pytest.mark.parametrize(
    ['sd_mode', 'quorum', 'real', 'fake'],
    [
        ('static', 0, 0, 1),
        ('static', 0.5, 1, 1),
        ('static', 0.5, 3, 3),
        ('static', 1, 1, 0),
        ('static', 1, 6, 0),
        ('file', 0, 0, 1),
        ('file', 0.5, 1, 1),
        ('file', 0.5, 3, 3),
        ('file', 1, 1, 0),
        ('file', 1, 6, 0),
        ('server1', 0.5, 1, 1),
        ('server1', 0.5, 3, 3),
        ('server2', 0.5, 1, 1),
        ('server2', 0.5, 3, 3),
    ],
    ids=[
        "static-skip",
        "static-half-1",
        "static-half-6",
        "static-full-1",
        "static-full-6",
        "file-skip",
        "file-half-1",
        "file-half-6",
        "file-full-1",
        "file-full-6",
        "server1-half-1",
        "server1-half-6",
        "server2-half-1",
        "server2-half-6",
    ]
)
def test_backends_check_sd_ok(ctx, algo, sd_mode, quorum, real, fake):
    """
    BALANCER-2898
    """
    sd_cache, result = check_config_sd(ctx, algo, quorum, real, fake, sd_mode)

    assert result.stderr.count("CHECK   sd-group") == 1
    if quorum:
        assert result.stderr.count("ERROR   sd-group") == fake
        assert result.stderr.count("SUCCESS sd-group") == 1
    else:
        assert result.stderr.count("SKIPPED sd-group") == 1

    if sd_mode.startswith('server'):
        assert os.path.isdir(sd_cache)
        assert sorted(os.listdir(sd_cache)) == ['cluster1#service1', 'cluster2#service2']

    assert result.return_code == 0


@pytest.mark.parametrize('algo', list_balancing_modes())
@pytest.mark.parametrize(
    ['sd_mode', 'quorum', 'real', 'fake'],
    [
        ('static', 0.51, 1, 1),
        ('static', 0.51, 3, 3),
        ('file', 0.51, 1, 1),
        ('file', 0.51, 3, 3),
        ('server1', 0.51, 1, 1),
        ('server1', 0.51, 3, 3),
        ('server2', 0.51, 1, 1),
        ('server2', 0.51, 3, 3),
    ],
    ids=[
        "static-few-1",
        "static-few-6",
        "file-few-1",
        "file-few-6",
        "server1-few-1",
        "server1-few-6",
        "server2-few-1",
        "server2-few-6",
    ]
)
def test_backends_check_sd_fail(ctx, algo, sd_mode, quorum, real, fake):
    """
    BALANCER-2898
    """
    sd_cache, result = check_config_sd(ctx, algo, quorum, real, fake, sd_mode)

    assert result.stderr.count("CHECK   sd-group") == 1
    assert result.stderr.count("ERROR   sd-group") == fake
    assert result.stderr.count("FAILED  sd-group") == 1
    assert result.return_code == 1


@pytest.mark.parametrize('algo', list_balancing_modes())
@pytest.mark.parametrize(
    ['sd_mode', 'quorum', 'real', 'fake'],
    [
        ('static', 0, 0, 0),
        ('file', 0, 0, 0),
        ('server1', 0, 1, 0),
        ('server2', 0, 1, 0),
        ('server1', 0, 0, 0),
    ],
    ids=[
        "static",
        "file",
        "server1",
        "server2",
        "server-both",
    ]
)
@pytest.mark.parametrize('allow_empty_endpoint_sets', [True, False], ids=['allow-empty', ''])
def test_backends_check_sd_empty(ctx, algo, sd_mode, quorum, real, fake, allow_empty_endpoint_sets):
    sd_cache, result = check_config_sd(ctx, algo, quorum, real, fake, sd_mode, allow_empty_endpoint_sets)
    if sd_mode == 'static' or allow_empty_endpoint_sets:
        assert result.stderr.count("CHECK   sd-group") == 1
        assert result.stderr.count("SKIPPED sd-group") == 1
        assert result.return_code == 0
    else:
        assert result.stderr.count("CHECK   sd-group") == 0
        assert result.return_code == 1


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_backends_check_sd_fail_no_cache(ctx, algo):
    """
    BALANCER-2898
    """
    sd_cache = ctx.manager.fs.create_dir('sd_cache')
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='sd')
    ctx.start_fake_backend(name='fake')

    result = ctx.manager.balancer.check_config(configs.BackendsCheckSD(
        algo=algo, sd_cache=sd_cache, quorum=0, endpoint_sets=1
    ))

    assert result.stderr.count('CHECK   sd-group') == 0
    assert result.return_code == 1


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_backends_check_sd_ok_cache(ctx, algo):
    """
    BALANCER-2898
    """
    sd_cache, result = check_config_sd(ctx, algo, quorum=0.5, real=4, fake=3, sd_mode='server')
    assert result.return_code == 0
    ctx.sd.stop()
    time.sleep(1)

    result = ctx.manager.balancer.check_config(configs.BackendsCheckSD(
        algo=algo, sd_cache=sd_cache, quorum=0.5, endpoint_sets=2
    ))

    assert result.stderr.count('CHECK   sd-group') == 1
    assert result.stderr.count("ERROR   sd-group") == 3
    assert result.stderr.count("SUCCESS sd-group") == 1
    assert result.return_code == 0


@pytest.mark.parametrize('root_amount_quorum', [0, 1, 2, 3], ids=["skip", "want_one", "want_two", "want_all"])
@pytest.mark.parametrize('algo', list_balancing_modes())
@pytest.mark.parametrize(
    ['quorum1', 'real1', 'fake1'],
    [
        (0, 3, 3),
        (0.3, 3, 3),
        (0.7, 3, 3),
    ],
    ids=[
        "skip_two",
        "ok_two",
        "fail_two",
    ]
)
@pytest.mark.parametrize(
    ['quorum2', 'real2', 'fake2'],
    [
        (0, 1, 1),
        (0.3, 1, 1),
        (0.7, 1, 1),
    ],
    ids=[
        "skip_one",
        "ok_one",
        "fail_one",
    ]
)
def test_2lvl_check_backends(ctx, root_amount_quorum, algo, quorum1, real1, fake1, quorum2, real2, fake2):
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='fake')

    result = ctx.manager.balancer.check_config(configs.BackendsCheck(
        algo=algo,
        quorum1=quorum1, real1=real1, fake1=fake1,
        quorum2=quorum2, real2=real2, fake2=fake2,
        root="root", root_amount_quorum=root_amount_quorum
    ))
    assert result.stderr.count("CHECK   group1") == 0
    assert result.stderr.count("CHECK   group2") == 0
    assert result.stderr.count("CHECK   root") == 1
    expected_errors = 0
    skip = True
    availability = 3
    if root_amount_quorum:
        if quorum1:
            expected_errors += fake1 * 2
            skip = False
            if quorum1 > 1.0 * real1 / (fake1 + real1):
                availability -= 2
        if quorum2:
            expected_errors += fake2
            skip = False
            if quorum2 > 1.0 * real2 / (fake2 + real2):
                availability -= 1
    assert result.stderr.count("ERROR   root") == expected_errors
    if skip:
        assert result.stderr.count("SKIPPED root") == 1
    elif availability >= root_amount_quorum:
        assert result.stderr.count("SUCCESS root") == 1
    else:
        assert result.stderr.count("FAILED  root") == 1


@pytest.mark.parametrize('algo', list_balancing_modes())
def test_2lvl_children_quorum_override(ctx, algo):
    ctx.start_backend(SimpleConfig(response=http.response.ok()), name='real')
    ctx.start_fake_backend(name='fake')
    quorums_file = ctx.manager.fs.create_file('quorums_file.txt')

    def run_check():
        return ctx.manager.balancer.check_config(configs.BackendsCheck(
            algo=algo, quorums_file=quorums_file,
            quorum1=0.5, real1=3, fake1=4,
            quorum2=0.3, real2=3, fake2=4,
            root="root", root_amount_quorum=2
        ))

    result=run_check()
    assert result.stderr.count("CHECK   root") == 1
    assert result.stderr.count("ERROR   root") == 12
    assert result.stderr.count("FAILED  root") == 1
    assert result.return_code == 1

    ctx.manager.fs.rewrite(quorums_file, "group1,0.4")
    result=run_check()
    assert result.stderr.count("QUORUM  group1,0.4,-") == 0
    assert result.stderr.count("CHECK   root") == 1
    assert result.stderr.count("ERROR   root") == 12
    assert result.stderr.count("SUCCESS root") == 1
    assert result.return_code == 0

    ctx.manager.fs.rewrite(quorums_file, "root,-,1")
    result=run_check()
    assert result.stderr.count("QUORUM  root,-,1") == 1
    assert result.stderr.count("CHECK   root") == 1
    assert result.stderr.count("ERROR   root") == 12
    assert result.stderr.count("SUCCESS root") == 1
    assert result.return_code == 0

    ctx.manager.fs.rewrite(quorums_file, "root,-,3\ngroup1,0.4")
    result=run_check()
    assert result.stderr.count("QUORUM  root,-,3") == 1
    assert result.stderr.count("QUORUM  group1,0.4,-") == 0
    assert result.stderr.count("CHECK   root") == 1
    assert result.stderr.count("ERROR   root") == 12
    assert result.stderr.count("SUCCESS root") == 1
    assert result.return_code == 0

    ctx.manager.fs.rewrite(quorums_file, "root,-,1\ngroup2,0.5")
    result=run_check()
    assert result.stderr.count("QUORUM  root,-,1") == 1
    assert result.stderr.count("QUORUM  group2,0.5,-") == 0
    assert result.stderr.count("CHECK   root") == 1
    assert result.stderr.count("ERROR   root") == 12
    assert result.stderr.count("FAILED  root") == 1
    assert result.return_code == 1
