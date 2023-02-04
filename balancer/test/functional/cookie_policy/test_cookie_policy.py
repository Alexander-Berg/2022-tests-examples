# -*- coding: utf-8 -*-
import time
import pytest

from configs import CookiePolicyConfig

from balancer.test.util.predef import http
from balancer.test.util.balancer import asserts
from balancer.test.util.predef.handler.server.http import SimpleConfig


_IS_BRO = [
    'Mozilla/5.0 (Android 6.0; Mobile; rv:68.0) Gecko/68.0 Firefox/68.0',
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3729.169 Safari/537.36'
]

_IS_GDPR = [
    'is_gdpr_b=CJq6ShBv',
    'is_gdpr_b=CJq6ShBvGAE=',
]


@pytest.mark.parametrize('match', [True, False], ids=['match', 'nomatch'])
@pytest.mark.parametrize('mode', [None, 'off', 'dry_run', 'fix'])
@pytest.mark.parametrize('bro', [True, False], ids=['bro', 'nobro'])
def test_samesite_none(ctx, mode, bro, match):
    """
    BALANCER-2839
    Balancer reports policy violation and fixes the cookie if enforce is true
    """
    cookies = [
        'u=',
        'n=; SameSite=None',
        'ns=; SameSite=None; Secure',
        'l=; SameSite=Lax',
        's=; SameSite=Strict',
    ]

    ctx.start_backend(SimpleConfig(
        response=http.response.ok(headers={'Set-Cookie': cookies}, data="xxx")
    ))

    ua = _IS_BRO[bro]

    if mode != 'fix':
        expect = cookies
    elif bro and match:
        expect = [
            'u=; SameSite=None; Secure',
            'n=; SameSite=None; Secure',
            'ns=; SameSite=None; Secure',
            'l=; SameSite=None; Secure',
            's=; SameSite=None; Secure',
        ]
    elif bro and not match:
        expect = [
            'u=',
            'n=; SameSite=None; Secure',
            'ns=; SameSite=None; Secure',
            'l=; SameSite=Lax',
            's=; SameSite=Strict',
        ]
    elif not bro and match:
        expect = [
            'u=',
            'n=',
            'ns=; Secure',
            'l=',
            's=',
        ]
    else:
        expect = [
            'u=',
            'n=',
            'ns=; Secure',
            'l=; SameSite=Lax',
            's=; SameSite=Strict',
        ]

    ctx.start_balancer(CookiePolicyConfig(
        ssn_name_re='.*' if match else None,
        ssn_policy_mode=mode,
        ssn_policy=True,
    ))

    resp = ctx.perform_request(http.request.get(headers={'user-agent': ua}))
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', expect)

    # see the module unittests for detailed logging and stats checks
    time.sleep(1.5)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-total_summ'] == 1
    assert unistat['cpol-xxx-modified-resp_summ'] == (1 if mode == 'fix' else 0)
    assert unistat['cpol-xxx-parser-total_summ'] == 1
    assert unistat['cpol-xxx-parser-cookie-total_summ'] == 5
    assert unistat['cpol-xxx-ssn_policy-total_summ'] == 1
    assert unistat['cpol-xxx-ssn_policy-cookie-total_summ'] == (0 if mode == 'off' else 5)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    if mode != 'off':
        assert '[cpol u:xxx-ssn_policy' in accesslog


@pytest.mark.parametrize('mode', [None, 'off', 'dry_run', 'fix'])
@pytest.mark.parametrize('gdpr', [True, False], ids=["gdpr", "nogdpr"])
def test_eprivacy_lifetime(ctx, gdpr, mode):
    """
    BALANCER-2839
    Balancer reports policy violation and fixes the cookie if enforce is true
    """
    cookies = [
        'ok=1;Max-Age=1',
        'long=2;Max-Age=316224000',
    ]

    ctx.start_backend(SimpleConfig(
        response=http.response.ok(headers={'Set-Cookie': cookies}, data="xxx")
    ))

    if mode == 'fix' and gdpr:
        expect = [
            'ok=1;Max-Age=1',
            'long=2; Max-Age=63072000',
        ]
    else:
        expect = cookies

    ctx.start_balancer(CookiePolicyConfig(
        epl_policy_mode=mode,
        epl_policy=True,
    ))

    resp = ctx.perform_request(http.request.get(headers={'cookie': _IS_GDPR[gdpr]}))

    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', expect)

    # see the module unittests for detailed logging and stats checks
    time.sleep(1.5)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-total_summ'] == 1
    assert unistat['cpol-xxx-modified-resp_summ'] == (1 if mode == 'fix' and gdpr else 0)
    assert unistat['cpol-xxx-parser-total_summ'] == 1
    assert unistat['cpol-xxx-parser-cookie-total_summ'] == 2
    assert unistat['cpol-xxx-epl_policy-total_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-cookie-total_summ'] == (0 if mode == 'off' or not gdpr else 2)
    if gdpr and mode != 'off':
        accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
        assert '[cpol u:xxx-epl_policy' in accesslog


@pytest.mark.parametrize('mode', [None, 'off', 'dry_run', 'fix'])
def test_protected_cookie(ctx, mode):
    """
    BALANCER-3065
    Balancer reports policy violation and drops the cookie if enforce is true
    """
    cookies = [
        'ok=1',
        'prot=2',
    ]

    ctx.start_backend(SimpleConfig(
        response=http.response.ok(headers={'Set-Cookie': cookies}, data="xxx")
    ))

    if mode == 'fix':
        expect = [
            'ok=1',
        ]
    else:
        expect = cookies

    ctx.start_balancer(CookiePolicyConfig(
        prc_policy_mode=mode,
        prc_policy=True,
        prc_name_re='prot'
    ))

    resp = ctx.perform_request(http.request.get())

    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', expect)

    # see the module unittests for detailed logging and stats checks
    time.sleep(1.5)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-total_summ'] == 1
    assert unistat['cpol-xxx-modified-resp_summ'] == (1 if mode == 'fix' else 0)
    assert unistat['cpol-xxx-parser-total_summ'] == 1
    assert unistat['cpol-xxx-parser-cookie-total_summ'] == 2
    assert unistat['cpol-xxx-prc_policy-total_summ'] == 1
    assert unistat['cpol-xxx-prc_policy-cookie-total_summ'] == (0 if mode == 'off' else 2)
    accesslog = ctx.manager.fs.read_file(ctx.balancer.config.accesslog)
    if mode != "off":
        assert '[cpol u:xxx-prc_policy' in accesslog


def test_file_switch(ctx):
    """
    BALANCER-2839
    File switch changes modes for the policies
    """
    ssn = [
        'ssn=;SameSite=None',
        'ssn=; SameSite=None; Secure'
    ]
    epl = [
        'lma=;Max-Age=316224000',
        'lma=; Max-Age=63072000'
    ]
    syn = [
        '=invalid',
        None
    ]

    cookie = [
        ssn[0],
        epl[0],
        syn[0],
    ]

    file_switch = ctx.manager.fs.create_file('file_switch')
    ctx.start_backend(SimpleConfig(
        response=http.response.ok(headers={
            'set-cookie': cookie,
        }, data="xxx")
    ))

    ctx.start_balancer(CookiePolicyConfig(
        ssn_policy_mode='fix',
        ssn_policy=True,
        epl_policy_mode='fix',
        epl_policy=True,
        parser_mode='fix',
        file_switch=file_switch
    ))
    req = http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    })

    exp_0 = [
        ssn[1],
        epl[1],
    ]
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', exp_0)

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-modified-resp_summ'] == 1
    assert unistat['cpol-xxx-parser-fix_summ'] == 1
    assert unistat['cpol-xxx-parser-fix-dryRun_summ'] == 0
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-fix-dryRun_summ'] == 0
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 0
    assert unistat['cpol-xxx-ssn_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-ssn_policy-fix-dryRun_summ'] == 0
    assert unistat['cpol-xxx-ssn_policy-off_summ'] == 0

    ctx.manager.fs.rewrite(
        file_switch,
        """
        {
            "policy_modes": {
                "ssn_policy": {"mode": "dry_run"},
                "epl_policy": {"mode": "dry_run"}
            },
            "parser_mode": {"mode": "dry_run"}
        }
        """
    )

    time.sleep(1.5)
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', cookie)

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-modified-resp_summ'] == 1
    assert unistat['cpol-xxx-parser-fix_summ'] == 1
    assert unistat['cpol-xxx-parser-fix-dryRun_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-fix-dryRun_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 0
    assert unistat['cpol-xxx-ssn_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-ssn_policy-fix-dryRun_summ'] == 1
    assert unistat['cpol-xxx-ssn_policy-off_summ'] == 0

    ctx.manager.fs.rewrite(
        file_switch, '{ "policy_modes": { "ssn_policy": {"mode": "dry_run"}}}'
    )

    time.sleep(1.5)
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', [
        ssn[0],
        epl[1],
    ])

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-modified-resp_summ'] == 2
    assert unistat['cpol-xxx-parser-fix_summ'] == 2
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 2
    assert unistat['cpol-xxx-epl_policy-fix-dryRun_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 0
    assert unistat['cpol-xxx-ssn_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-ssn_policy-fix-dryRun_summ'] == 2
    assert unistat['cpol-xxx-ssn_policy-off_summ'] == 0

    ctx.manager.fs.rewrite(
        file_switch, '{ "policy_modes": { "epl_policy": {"mode": "off"}}}'
    )

    time.sleep(1.5)
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', [
        ssn[1],
        epl[0],
    ])

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-off_summ'] == 0
    assert unistat['cpol-xxx-modified-resp_summ'] == 3
    assert unistat['cpol-xxx-parser-fix_summ'] == 3
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 2
    assert unistat['cpol-xxx-epl_policy-fix-dryRun_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 1
    assert unistat['cpol-xxx-ssn_policy-fix_summ'] == 2
    assert unistat['cpol-xxx-ssn_policy-fix-dryRun_summ'] == 2
    assert unistat['cpol-xxx-ssn_policy-off_summ'] == 0

    ctx.manager.fs.rewrite(file_switch, '{"parser_mode":{"mode": "off"}}')

    time.sleep(1.5)
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', [
        ssn[0],
        epl[0],
        syn[0],
    ])

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-off_summ'] == 1
    assert unistat['cpol-xxx-modified-resp_summ'] == 3


def test_gdpr_file_switch(ctx):
    """
    BALANCER-2839
    File switch changes modes for the policies
    """
    epl = [
        'lma=;Max-Age=316224000',
        'lma=; Max-Age=63072000'
    ]

    cookie = [
        epl[0],
    ]

    file_switch = ctx.manager.fs.create_file('gdpr_file_switch')
    ctx.start_backend(SimpleConfig(
        response=http.response.ok(headers={
            'set-cookie': cookie,
        }, data="xxx")
    ))

    ctx.start_balancer(CookiePolicyConfig(
        epl_policy_mode='fix',
        epl_policy=True,
        parser_mode='fix',
        gdpr_file_switch=file_switch
    ))
    req = http.request.get(headers={
        'cookie': _IS_GDPR[1],
    })

    fixed = [
        epl[1],
    ]
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', fixed)

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-modified-resp_summ'] == 1
    assert unistat['cpol-xxx-gdpr-geo-0_summ'] == 0
    assert unistat['cpol-xxx-gdpr-src-None_summ'] == 0
    assert unistat['cpol-xxx-gdpr-geo-1_summ'] == 1
    assert unistat['cpol-xxx-gdpr-src-IsGdprB_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 0

    ctx.manager.fs.rewrite(file_switch, '{ "use_is_gdpr_b": 0}')

    time.sleep(1.5)
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', cookie)

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-modified-resp_summ'] == 1
    assert unistat['cpol-xxx-gdpr-geo-0_summ'] == 1
    assert unistat['cpol-xxx-gdpr-src-None_summ'] == 1
    assert unistat['cpol-xxx-gdpr-geo-1_summ'] == 1
    assert unistat['cpol-xxx-gdpr-src-IsGdprB_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 1
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 1

    ctx.manager.fs.rewrite(file_switch, '{ "use_is_gdpr_b": 1}')

    time.sleep(1.5)
    resp = ctx.perform_request(req)
    asserts.status(resp, 200)
    asserts.content(resp, 'xxx')
    asserts.header(resp, 'Set-Cookie')
    asserts.header_values(resp, 'Set-Cookie', fixed)

    time.sleep(0.1)
    unistat = ctx.get_unistat()
    assert unistat['cpol-xxx-modified-resp_summ'] == 2
    assert unistat['cpol-xxx-gdpr-geo-0_summ'] == 1
    assert unistat['cpol-xxx-gdpr-src-None_summ'] == 1
    assert unistat['cpol-xxx-gdpr-geo-1_summ'] ==2
    assert unistat['cpol-xxx-gdpr-src-IsGdprB_summ'] == 2
    assert unistat['cpol-xxx-epl_policy-fix_summ'] == 2
    assert unistat['cpol-xxx-epl_policy-off_summ'] == 1


@pytest.mark.parametrize('mode', [None, "off", "watch_only", "stable", "stable_fix", "unstable"])
def test_default_yandex_policies(ctx, mode):
    cookies = [
        'i=1; Max-Age=3153600000',
        'my=3; Max-Age=3153600000',
        'sc_123=4; Max-Age=3153600000',
        'yandexuid=5; Max-Age=3153600000',
        'yc=6; Max-Age=3153600000',
        'yclid_123=7; Max-Age=3153600000',
        'yp=8; Max-Age=3153600000',
        'ys=9; Max-Age=3153600000',
        'yabs-frequency=10; Max-Age=3153600000',
        'yandex_gid=11; Max-Age=3153600000',
        'sessguard=12',
        'Session_id=13',
        'sessguard=14',
        'Session_id=15',
        'mda_beacon=16',
        'yandex_login=17',
        'lah=18',
        'ilahu=19',
    ]

    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': cookies
    }, data="xxx")))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies=mode,
    ))
    if mode is None:
        mode = "off"
    req = http.request.get(headers={
        'host': 'yandex.ru',
        'user-agent': _IS_BRO[1],
    })
    resp = ctx.perform_request(req)

    if mode == "unstable":
        asserts.header_values(resp, 'set-cookie', [
            "i=1; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
            "my=3; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
            "sc_123=4; Max-Age=3153600000; SameSite=None; Secure",
            "yandexuid=5; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
            "yc=6; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
            "yclid_123=7; Max-Age=3153600000; SameSite=None; Secure",
            "yp=8; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
            "ys=9; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
            "yabs-frequency=10; Max-Age=3153600000; SameSite=None; Secure",
            "yandex_gid=11; Path=/; Domain=.yandex.ru; Max-Age=3153600000; SameSite=None; Secure",
        ])
    elif mode == "stable_fix":
        asserts.header_values(resp, 'set-cookie', [
            "i=1; Max-Age=3153600000; SameSite=None; Secure",
            "my=3; Max-Age=3153600000; SameSite=None; Secure",
            "sc_123=4; Max-Age=3153600000; SameSite=None; Secure",
            "yandexuid=5; Max-Age=3153600000; SameSite=None; Secure",
            "yc=6; Max-Age=3153600000; SameSite=None; Secure",
            "yclid_123=7; Max-Age=3153600000; SameSite=None; Secure",
            "yp=8; Max-Age=3153600000; SameSite=None; Secure",
            "ys=9; Max-Age=3153600000; SameSite=None; Secure",
            "yabs-frequency=10; Max-Age=3153600000; SameSite=None; Secure",
            "yandex_gid=11; Max-Age=3153600000; SameSite=None; Secure",
        ])
    else:
        asserts.header_values(resp, 'set-cookie', cookies)

    time.sleep(0.1)
    unistat = ctx.get_unistat()

    for p in ['yandex_ssn', 'eprivacy_lifetime', 'passport_protected']:
        k = 'cpol-xxx-{}-total_summ'.format(p)
        if mode == 'off':
            assert k not in unistat
        else:
            assert unistat[k] == 1


def test_cookie_problems_reporting(ctx):
    """
    BALANCER-2944
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'i=1; Max-Age=3153600000',
            'j',
            'my=3; Max-Age=3153600000',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(default_yandex_policies="unstable"))
    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
        "X-Yandex-Internal-Request": "1",
    }))
    asserts.no_header(resp, "X-Yandex-Cookie-Problems")

    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
        "X-Yandex-Internal-Request": "1",
        "X-Yandex-Show-Cookie-Problems": "1",
    }))
    asserts.header_value(
        resp,
        "X-Yandex-Cookie-Problems",
        str('[cpol u:xxx-parser fail+fix (j)=Drop:1!CookieNameEmpty,] '
            '[cpol u:xxx-eprivacy_lifetime fail+fix i=Fix:1!BigLifetime, my=Fix:1!BigLifetime,] '
            '[cpol u:xxx-yandex_ssn fail+fix i=Fix:1!BadSameSite, my=Fix:1!BadSameSite,]')
    )


def test_default_yandex_policies_override_parser(ctx):
    """
    BALANCER-3044
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'i=1; Max-Age=3153600000',
            'j',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies="stable",
        parser_mode='fix',
    ))

    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    }))

    asserts.header_values(resp, 'set-cookie', [
        'i=1; Max-Age=3153600000'
    ])


def test_default_yandex_policies_override_eprivacy_lifetime(ctx):
    """
    BALANCER-3044
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'i=1; Max-Age=3153600000',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies="stable",
        epl_policy_override=True,
        epl_policy_name='eprivacy_lifetime',
        epl_policy_mode='fix',
    ))
    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    }))
    asserts.header_values(resp, 'set-cookie', [
        'i=1; Max-Age=63072000',
    ])


def test_default_yandex_policies_override_yandex_ssn(ctx):
    """
    BALANCER-3044
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'i=1; Max-Age=3153600000',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies="stable",
        ssn_policy_override=True,
        ssn_policy_name='yandex_ssn',
        ssn_policy_mode='fix',
    ))
    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    }))
    asserts.header_values(resp, 'set-cookie', [
        'i=1; Max-Age=3153600000; SameSite=None; Secure',
    ])


def test_default_yandex_policies_redefine_yandex_ssn(ctx):
    """
    BALANCER-3044
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'i=1; Max-Age=3153600000',
            'my=3; Max-Age=3153600000',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies="stable",
        ssn_policy=True,
        ssn_policy_name='yandex_ssn',
        ssn_policy_mode='fix',
        ssn_name_re='i',
    ))
    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    }))
    asserts.header_values(resp, 'set-cookie', [
        'i=1; Max-Age=3153600000; SameSite=None; Secure',
        'my=3; Max-Age=3153600000',
    ])


def test_default_yandex_policies_override_passport_protected(ctx):
    """
    BALANCER-3044
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'Session_id=1',
            'i=1; Max-Age=3153600000',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies="stable",
        prc_policy_override=True,
        prc_policy_name='passport_protected',
        prc_policy_mode='fix',
    ))
    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    }))
    asserts.header_values(resp, 'set-cookie', [
        'Session_id=1',
        'i=1; Max-Age=3153600000',
    ])


def test_default_yandex_policies_redefine_passport_protected(ctx):
    """
    BALANCER-3044
    """
    ctx.start_backend(SimpleConfig(response=http.response.ok(headers={
        'set-cookie': [
            'Session_id=1',
            'i=1; Max-Age=3153600000',
        ],
    })))
    ctx.start_balancer(CookiePolicyConfig(
        default_yandex_policies="stable",
        prc_policy=True,
        prc_policy_name='passport_protected',
        prc_policy_mode='fix',
        prc_name_re='i'
    ))
    resp = ctx.perform_request(http.request.get(headers={
        'user-agent': _IS_BRO[1],
        'cookie': _IS_GDPR[1],
    }))
    asserts.header_values(resp, 'set-cookie', [
        'Session_id=1',
    ])
