import pytest
from pydantic import ValidationError

from maps.infra.sedem.lib.config.schema import ServiceConfig, ServiceType
from maps.infra.sedem.lib.config.schema.balancers import Balancer, BalancerUpstream, UpstreamMirroring
from maps.infra.sedem.lib.config.schema.secrets import Secret
from maps.infra.sedem.lib.config.schema.tests.shared import DEFAULT_CONFIG_CONTENT, extract_errors


def test_extra_field() -> None:
    config = ServiceConfig.parse_obj({
        'main': {
            'service_name': 'maps-core-fake',
        },
        'deploy': {'deploy_profile': 'default'},
        'deploy_profiles': {'default': {}},
        'unused': 'value',
    })

    assert config.main.service_name == 'maps-core-fake'


def test_new_config_format() -> None:
    config = ServiceConfig.parse_obj({
        'main': {
            'service_name': 'maps-core-fake',
            'service_type': 'garden',
        },
        'deploy': {'deploy_profile': 'default'},
        'deploy_profiles': {'default': {}},
        'balancers': {
            'stable': [
                {'instances_count': 1},
            ],
        },
    })

    assert config.main.service_name == 'maps-core-fake'
    assert config.main.service_type == ServiceType.GARDEN
    assert config.deploy.deploy_profile == 'default'
    assert config.balancers == {
        'stable': [
            Balancer(
                fqdn='core-fake.maps.yandex.net',
                instances_count=1,
            ),
        ],
    }


def test_old_config_format() -> None:
    config = ServiceConfig.parse_obj({
        'main': {
            'name': 'fake',
            'balancer': {
                'stable': [
                    {'instances_count': 1},
                ],
            },
        },
        'deploy': {
            'type': 'garden',
            'deploy_profile': 'default',
        },
        'deploy_profiles': {'default': {}},
    })

    assert config.main.service_name == 'maps-core-fake'
    assert config.main.service_type == ServiceType.GARDEN
    assert config.deploy.deploy_profile == 'default'
    assert config.balancers == {
        'stable': [
            Balancer(
                fqdn='core-fake.maps.yandex.net',
                instances_count=1,
            ),
        ],
    }


def test_consistent_config_with_mirroring() -> None:
    config = ServiceConfig.parse_obj({
        **DEFAULT_CONFIG_CONTENT,
        'balancers': {
            'stable': [{
                'instances_count': 1,
                'upstreams': {
                    'default': {
                        'mirroring': [{
                            'rate': 0.1,
                            'fqdn': 'core-masquerade.maps.yandex.net',
                            'host': 'core-fake.common.testing.maps.yandex.net',
                            'remove_credentials': False,
                        }],
                    },
                },
            }],
        },
        'secrets': {
            'testing': [{
                'secret_id': 'sec-abc',
                'version': 'ver-xyz',
                'key': 'client_secret',
                'self_tvm_id': 1234,
            }],
        },
    })

    assert config.balancers == {
        'stable': [
            Balancer(
                fqdn='core-fake.maps.yandex.net',
                instances_count=1,
                upstreams={'default': BalancerUpstream(
                    mirroring=[UpstreamMirroring(
                        rate=0.1,
                        fqdn='core-masquerade.maps.yandex.net',
                        host='core-fake.common.testing.maps.yandex.net',
                        remove_credentials=False,
                    )],
                )},
            ),
        ],
    }
    assert config.secrets == {
        'testing': [
            Secret(
                secret_id='sec-abc',
                version='ver-xyz',
                key='client_secret',
                self_tvm_id=1234,
            ),
        ],
    }


def test_inconsistent_config_with_mirroring() -> None:
    with pytest.raises(ValidationError) as exc:
        ServiceConfig.parse_obj({
            **DEFAULT_CONFIG_CONTENT,
            'balancers': {
                'stable': [{
                    'instances_count': 1,
                    'upstreams': {
                        'default': {
                            'mirroring': [{
                                'rate': 0.1,
                                'fqdn': 'core-masquerade.maps.yandex.net',
                                'host': 'core-fake.common.testing.maps.yandex.net',
                                'remove_credentials': False,
                            }],
                        },
                    },
                }],
            },
        })

    assert extract_errors(exc) == [
        'mirroring through Masquerade requires providing "self_tvm_id" in secrets for testing',
    ]
