import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema import ServiceConfig
from maps.infra.sedem.lib.config.schema.balancers import (
    Balancer,
    BalancerUpstream,
    ENVIRONMENTS_WHITELIST,
    EndpointSet,
    L3BalancerAllocation,
    L7BalancerAllocation,
    MASQUERADE_FQDN,
    UpstreamMatcher,
    UpstreamMirroring,
    UpstreamHashing,
    UpstreamParams,
    UpstreamRewriteAction,
)
from maps.infra.sedem.lib.config.schema.tests.shared import DEFAULT_CONFIG_CONTENT, extract_errors


def test_valid_matcher() -> None:
    matcher = UpstreamMatcher.parse_obj({
        'type': 'match',
        'regexp': 'POST.*',
    })

    assert matcher == UpstreamMatcher.construct(
        type='match',
        regexp='POST.*',
    )


def test_invalid_matcher() -> None:
    with pytest.raises(ValidationError) as exc:
        UpstreamMatcher.parse_obj({
            'type': 'invalid',
            'regexp': '*',
        })

    assert extract_errors(exc) == [
        "unexpected value; permitted: 'host', 'url', 'cgi', 'uri', 'path', 'match'",
        'bad regexp: nothing to repeat at position 0',
    ]


def test_valid_hashing() -> None:
    hashing = UpstreamHashing.parse_obj({
        'type': 'hashing',
        'parameters': ['x', 'y', 'z'],
    })

    assert hashing == UpstreamHashing.construct(
        type='hashing',
        parameters=['x', 'y', 'z'],
    )


def test_invalid_hashing() -> None:
    with pytest.raises(ValidationError) as exc:
        UpstreamHashing.parse_obj({
            'type': 'unknown',
            'parameters': 'x,y,z',
        })

    assert extract_errors(exc) == [
        "unexpected value; permitted: 'hashing', 'rendezvous_hashing'",
        'value is not a valid list',
    ]


def test_valid_rewrite_action() -> None:
    rewrite_action = UpstreamRewriteAction.parse_obj({
        'regexp': '(.*)apikey=([0-9a-f-]+)(.*)',
        'rewrite': '%1apikey=1234%3',
        'split': 'cgi',
    })

    assert rewrite_action == UpstreamRewriteAction.construct(
        regexp='(.*)apikey=([0-9a-f-]+)(.*)',
        rewrite='%1apikey=1234%3',
        split='cgi',
    )


def test_invalid_rewrite_action() -> None:
    with pytest.raises(ValidationError) as exc:
        UpstreamRewriteAction.parse_obj({
            'regexp': '*',
            'rewrite': {},
            'split': 'unknown',
        })

    assert extract_errors(exc) == [
        'bad regexp: nothing to repeat at position 0',
        'str type expected',
        "unexpected value; permitted: 'url', 'cgi', 'path'",
    ]


def test_valid_minimal_mirroring() -> None:
    mirroring = UpstreamMirroring.parse_obj({
        'rate': 0.1,
        'fqdn': 'example.com',
    })

    assert mirroring == UpstreamMirroring.construct(
        rate=0.1,
        fqdn='example.com',
        stagings=[],
        host=None,
        rewrite=[],
        remove_credentials=True,
    )


def test_valid_masquerade_mirroring() -> None:
    mirroring = UpstreamMirroring.parse_obj({
        'rate': 0.1,
        'fqdn': MASQUERADE_FQDN,
        'host': 'example.com',
        'remove_credentials': False,
    })

    assert mirroring == UpstreamMirroring.construct(
        rate=0.1,
        fqdn=MASQUERADE_FQDN,
        stagings=[],
        host='example.com',
        rewrite=[],
        remove_credentials=False,
    )


def test_valid_featured_mirroring() -> None:
    mirroring = UpstreamMirroring.parse_obj({
        'rate': 0.1,
        'stagings': ['datavalidation'],
        'host': 'localhost',
        'rewrite': [{
            'regexp': '(.*)apikey=([0-9a-f-]+)(.*)',
            'rewrite': '%1apikey=1234%3',
        }],
        'remove_credentials': False,
    })

    assert mirroring == UpstreamMirroring.construct(
        rate=0.1,
        fqdn=None,
        stagings=['datavalidation'],
        host='localhost',
        rewrite=[UpstreamRewriteAction(
            regexp='(.*)apikey=([0-9a-f-]+)(.*)',
            rewrite='%1apikey=1234%3',
            split=None,
        )],
        remove_credentials=False,
    )


def test_invalid_mirroring() -> None:
    with pytest.raises(ValidationError) as exc:
        UpstreamMirroring.parse_obj({
            'rate': 'abc',
            'fqdn': [],
            'stagings': ['unknown'],
            'host': [],
            'rewrite': {},
            'remove_credentials': 11,
        })

    envs = "', '".join(ENVIRONMENTS_WHITELIST)
    envs = f"'{envs}'"
    assert extract_errors(exc) == [
        'value is not a valid float',
        'str type expected',
        f'unexpected value; permitted: {envs}',
        'str type expected',
        'value is not a valid list',
        'value could not be parsed to a boolean',
    ]


def test_valid_endpoint_set() -> None:
    endpoint_set = EndpointSet.parse_obj({
        'id': 'slug',
        'locations': ['SAS', 'VLA'],
    })

    assert endpoint_set == EndpointSet.construct(
        id='slug',
        locations=['SAS', 'VLA'],
    )


def test_invalid_endpoint_set() -> None:
    with pytest.raises(ValidationError) as exc:
        EndpointSet.parse_obj({
            'id': [],
            'locations': ['UNKNOWN'],
        })

    assert extract_errors(exc) == [
        'str type expected',
        "unexpected value; permitted: 'SAS', 'MAN', 'VLA', 'MYT', 'IVA'",
    ]


def test_valid_minimal_upstream() -> None:
    upstream = BalancerUpstream.parse_obj({})

    assert upstream == BalancerUpstream.construct(
        matcher=[],
        order=None,
        rewrite=[],
        host=None,
        host_rewrite=True,
        backend_timeout=UpstreamParams().backend_timeout,
        recv_timeout=UpstreamParams().recv_timeout,
        attempts=UpstreamParams().attempts,
        hashing=None,
        mirroring=[],
        antirobot_service=None,
        stagings=[],
        endpoint_sets=[],
    )


def test_valid_featured_upstream() -> None:
    upstream = BalancerUpstream.parse_obj({
        'matcher': [{'type': 'match', 'regexp': '/uri.*'}],
        'order': '12345678',
        'rewrite': [{'regexp': '.*', 'rewrite': '%1'}],
        'host': 'example.com',
        'host_rewrite': False,
        'backend_timeout': '30s',
        'recv_timeout': '10s',
        'attempts': 5,
        'hashing': {'type': 'hashing', 'parameters': ['x', 'y', 'z']},
        'mirroring': [{'rate': 0.1, 'fqdn': 'another.com'}],
        'antirobot_service': 'some-slug',
        'endpoint_sets': [{'id': 'some-service-id', 'locations': ['MAN', 'VLA']}],
    })

    assert upstream == BalancerUpstream.construct(
        matcher=[UpstreamMatcher(type='match', regexp='/uri.*')],
        order='12345678',
        rewrite=[UpstreamRewriteAction(regexp='.*', rewrite='%1')],
        host='example.com',
        host_rewrite=False,
        backend_timeout='30s',
        recv_timeout='10s',
        attempts=5,
        hashing=UpstreamHashing(type='hashing', parameters=['x', 'y', 'z']),
        mirroring=[UpstreamMirroring(rate=0.1, fqdn='another.com')],
        antirobot_service='some-slug',
        stagings=[],
        endpoint_sets=[EndpointSet(id='some-service-id', locations=['MAN', 'VLA'])],
    )


def test_valid_minimal_dedicated_balancer() -> None:
    balancer = Balancer.parse_obj({
        'fqdn': 'example.com',
        'instances_count': 1,
    })

    assert balancer == Balancer.construct(
        automanaged=True,
        common=False,
        l3_only=False,

        name='default',
        fqdn='example.com',

        traffic_type=L3BalancerAllocation.TrafficType.INTERNAL,
        protocol=L3BalancerAllocation.Protocol.HTTP,

        instances_count=1,
        datacenters=[],
        instance_size=L7BalancerAllocation.Preset.MICRO,
        network_macro=None,
        https_cert=None,
        enable_sslv3=False,

        upstreams=None,
    )


def test_too_long_fqdn_for_dedicated_balancer() -> None:
    with pytest.raises(ValidationError) as exc:
        Balancer.parse_obj({
            'fqdn': 'very-very-very-very-very-long-fqdn.datavalidation.maps.yandex.net',
            'instances_count': 1,
        })

    assert extract_errors(exc) == [
        'ensure this value has at most 61 characters'
    ]


def test_valid_minimal_upstreams_mapping() -> None:
    balancer = Balancer.parse_obj({
        'fqdn': 'example.com',
        'instances_count': 1,
        'upstreams': {
            'default': {},
        },
    })

    assert balancer.upstreams == {
        'default': BalancerUpstream(),
    }


def test_invalid_upstreams_mapping() -> None:
    with pytest.raises(ValidationError) as exc:
        Balancer.parse_obj({
            'fqdn': 'example.com',
            'instances_count': 1,
            'upstreams': {},
        })

    assert extract_errors(exc) == [
        '"default" upstream is missing',
    ]


def test_invalid_slbping_upstream() -> None:
    with pytest.raises(ValidationError) as exc:
        Balancer.parse_obj({
            'fqdn': 'example.com',
            'instances_count': 1,
            'upstreams': {
                'slbping': {},
                'default': {},
            },
        })

    assert extract_errors(exc) == [
        'customization of "slbping" upstream is not allowed',
    ]


def test_valid_endpoint_set_upstream_for_dedicated_balancer() -> None:
    balancer = Balancer.parse_obj({
        'fqdn': 'example.com',
        'instances_count': 1,
        'datacenters': ['MAN'],
        'upstreams': {
            'default': {
                'endpoint_sets': [
                    {'id': 'endpoint-set-slug', 'locations': ['SAS']}
                ],
            },
        },
    })

    assert balancer.datacenters == ['MAN']
    assert balancer.upstreams == {
        'default': BalancerUpstream(endpoint_sets=[
            EndpointSet(id='endpoint-set-slug', locations=['SAS']),
        ]),
    }


def test_valid_endpoint_set_upstream_for_common_balancer() -> None:
    balancer = Balancer.parse_obj({
        'common': True,
        'fqdn': 'example.common.testing.maps.yandex.net',
        'upstreams': {
            'default': {
                'endpoint_sets': [
                    {'id': 'endpoint-set-slug', 'locations': ['SAS']}
                ],
            },
        },
    })

    assert balancer.common is True
    assert balancer.upstreams == {
        'default': BalancerUpstream(endpoint_sets=[
            EndpointSet(id='endpoint-set-slug', locations=['SAS']),
        ]),
    }


def test_invalid_endpoint_set_upstream() -> None:
    with pytest.raises(ValidationError) as exc:
        Balancer.parse_obj({
            'fqdn': 'example.com',
            'instances_count': 1,
            'upstreams': {
                'default': {
                    'endpoint_sets': [
                        {'id': 'endpoint-set-slug', 'locations': ['SAS']}
                    ],
                },
            },
        })

    assert extract_errors(exc) == [
        '"datacenters" must be specified for balancer with "endpoint_sets" upstream',
    ]


def test_valid_featured_dedicated_balancer() -> None:
    balancer = Balancer.parse_obj({
        'name': 'custom',
        'fqdn': 'example.com',

        'traffic_type': 'external',
        'protocol': 'https',

        'instances_count': 3,
        'datacenters': ['MAN', 'VLA'],
        'instance_size': 'nano',
        'network_macro': '_SOME_MACRO_',
        'https_cert': 'cert-id',
        'enable_sslv3': True,

        'upstreams': {
            'default': {
                'endpoint_sets': [
                    {'id': 'endpoint-set-slug', 'locations': ['SAS']}
                ],
            },
        },
    })

    assert balancer == Balancer.construct(
        automanaged=True,
        common=False,
        l3_only=False,

        name='custom',
        fqdn='example.com',

        traffic_type=L3BalancerAllocation.TrafficType.EXTERNAL,
        protocol=L3BalancerAllocation.Protocol.HTTPS,

        instances_count=3,
        datacenters=['MAN', 'VLA'],
        instance_size=L7BalancerAllocation.Preset.NANO,
        network_macro='_SOME_MACRO_',
        https_cert='cert-id',
        enable_sslv3=True,

        upstreams={
            'default': BalancerUpstream(endpoint_sets=[
                EndpointSet(id='endpoint-set-slug', locations=['SAS']),
            ]),
        },
    )


def test_valid_minimal_common_balancer() -> None:
    balancer = Balancer.parse_obj({
        'common': True,
        'fqdn': 'example.common.testing.maps.yandex.net',
    })

    assert balancer == Balancer.construct(
        automanaged=True,
        common=True,
        l3_only=False,

        name='common_default',
        fqdn='example.common.testing.maps.yandex.net',

        upstreams=None,
    )


def test_valid_common_balancer_with_long_fqdn() -> None:
    balancer = Balancer.parse_obj({
        'common': True,
        'fqdn': 'very-very-very-very-long-fqdn.common.datavalidation.maps.yandex.net',
    })

    assert balancer == Balancer.construct(
        automanaged=True,
        common=True,
        l3_only=False,

        name='common_default',
        fqdn='very-very-very-very-long-fqdn.common.datavalidation.maps.yandex.net',

        upstreams=None,
    )


def test_valid_tld_fqdn() -> None:
    balancer = Balancer.parse_obj({
        'common': True,
        'fqdn': 'example.common.testing.maps.yandex.TLD',
        'upstreams': {
            'default': {'host': 'example.net'},
        },
    })

    assert balancer.fqdn == 'example.common.testing.maps.yandex.TLD'
    assert balancer.upstreams == {
        'default': BalancerUpstream(host='example.net'),
    }


def test_invalid_tld_fqdn() -> None:
    with pytest.raises(ValidationError) as exc:
        Balancer.parse_obj({
            'common': True,
            'fqdn': 'example.common.testing.maps.yandex.TLD',
            'upstreams': {
                'default': {},
            },
        })

    assert extract_errors(exc) == [
        '"host" must be set for upstreams of balancer with ".TLD" FQDN to perform health-checks'
    ]


def test_valid_minimal_l3_only_balancer() -> None:
    balancer = Balancer.parse_obj({
        'l3_only': True,
        'fqdn': 'example.com',
    })

    assert balancer == Balancer.construct(
        automanaged=True,
        common=False,
        l3_only=True,

        name='default',
        fqdn='example.com',

        upstreams=None,
    )


def test_valid_minimal_unmananged_balancer() -> None:
    balancer = Balancer.parse_obj({
        'automanaged': False,
        'fqdn': 'example.com',
    })

    assert balancer == Balancer.construct(
        automanaged=False,
        common=False,
        l3_only=False,

        name='default',
        fqdn='example.com',

        upstreams=None,
    )


def test_invalid_common_l3_only_balancer() -> None:
    with pytest.raises(ValidationError) as exc:
        Balancer.parse_obj({
            'common': True,
            'l3_only': True,
            'fqdn': 'example.common.testing.maps.yandex.net',
        })

    assert extract_errors(exc) == [
        'only one of "common" or "l3_only" can be defined',
    ]


def test_valid_minimal_balancers_section() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'balancers': {
            'stable': [
                {'instances_count': 1},
            ],
            'datatesting': [
                {'l3_only': True},
            ],
            'testing': [
                {'common': True},
            ],
        }
    })

    assert config.balancers == {
        'stable': [
            Balancer(
                fqdn='core-fake.maps.yandex.net',
                instances_count=1,
            ),
        ],
        'datatesting': [
            Balancer(
                l3_only=True,
                fqdn='core-fake.datatesting.maps.yandex.net',
            ),
        ],
        'testing': [
            Balancer(
                common=True,
                fqdn='core-fake.common.testing.maps.yandex.net',
            ),
        ],
    }


def test_balancers_section_with_duplicate_names() -> None:
    with pytest.raises(ValidationError) as exc:
        ServiceConfig.parse_obj({
            **DEFAULT_CONFIG_CONTENT,
            'balancers': {
                'stable': [
                    {'fqdn': 'example.com', 'instances_count': 1},
                    {'fqdn': 'another.com', 'instances_count': 1},
                ],
                'testing': [
                    {'common': True, 'fqdn': 'example.common.testing.maps.yandex.net'},
                    {'common': True, 'name': 'another', 'fqdn': 'another.common.testing.maps.yandex.net'},
                ],
            },
        })

    assert extract_errors(exc) == [
        'duplicated balancer name in stable: "default"',
    ]


def test_balancers_section_with_duplicate_fqdns() -> None:
    with pytest.raises(ValidationError) as exc:
        ServiceConfig.parse_obj({
            **DEFAULT_CONFIG_CONTENT,
            'balancers': {
                'stable': [
                    {'instances_count': 1},
                ],
                'testing': [
                    {'fqdn': 'core-fake.maps.yandex.net', 'instances_count': 1},
                ],
            },
        })

    assert extract_errors(exc) == [
        'duplicated balancer FQDN: "core-fake.maps.yandex.net"',
    ]


def test_balancers_section_with_unsupported_common_environment() -> None:
    with pytest.raises(ValidationError) as exc:
        ServiceConfig.parse_obj({
            **DEFAULT_CONFIG_CONTENT,
            'balancers': {
                'stable': [
                    {'common': True, 'fqdn': 'example.common.testing.maps.yandex.net'},
                ],
            },
        })

    assert extract_errors(exc) == [
        'usage of common balancer is not allowed in stable',
    ]
