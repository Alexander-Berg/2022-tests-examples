import typing as tp
from dataclasses import dataclass
from pathlib import PurePath
from unittest.mock import create_autospec, call

import pytest

from maps.infra.sedem.lib.config.test_utils import config_factory
from maps.infra.sedem.proto import sedem_pb2
from maps.infra.sedem.sandbox.config_applier.lib.appliers.upload_config import UploadConfig, SedemApi, Context


MOCK_APPLIER_PARAMS = {
    'oauth_token': 'fake-secret',
    'arcadia_root': '/fake-arcadia',
    'arcadia_revision': 42,
    'work_dirs_root': '/fake-cwd',
}


@dataclass
class UploadTestCase:
    name: str
    config: dict[str, tp.Any]
    expected_proto: sedem_pb2.ServiceConfig
    testing_used: bool = False

    def __str__(self) -> str:
        return self.name


TEST_CASES = [
    UploadTestCase(
        name='rtc',
        config={
            'main': {'name': 'mock-rtc'},
            'deploy': {'type': 'rtc'},
            'acceptance': {'testing': [{'scheduler_id': '123'}, {'template_name': 'MY_FANCY_TEMPLATE'}]},
            'resources': {'stable': {}, 'prestable': {}, 'testing': {}, 'load': {}},
        },
        expected_proto=sedem_pb2.ServiceConfig(
            name='maps-core-mock-rtc',
            abc_slug='maps-core-mock-rtc',
            path='maps/mock-rtc',
            sox=False,
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            stages=[sedem_pb2.ServiceConfig.Stage(**{
                'name': 'testing',
                'deploy_units': ['load', 'testing'],
                'required_duration': 30 * 60,  # 30m
            }), sedem_pb2.ServiceConfig.Stage(**{
                'name': 'prestable',
                'deploy_units': ['prestable'],
                'required_duration': 24 * 60 * 60,  # 1d
            }), sedem_pb2.ServiceConfig.Stage(**{
                'name': 'stable',
                'deploy_units': ['stable'],
            })],
            acceptance=[sedem_pb2.AcceptanceTest(**{
                'stage': 'testing',
                'sandbox_scheduler': {'id': '123'},
            }), sedem_pb2.AcceptanceTest(**{
                'stage': 'testing',
                'sandbox_template': {'name': 'MY_FANCY_TEMPLATE'},
            })],
        ),
    ),
    UploadTestCase(
        name='rtc-testing',
        config={
            'main': {'name': 'mock-rtc', 'use_testing_machine': True},
            'deploy': {'type': 'rtc'},
            'resources': {'stable': {}, 'testing': {}},
        },
        expected_proto=sedem_pb2.ServiceConfig(
            name='maps-core-mock-rtc',
            abc_slug='maps-core-mock-rtc',
            path='maps/mock-rtc',
            sox=False,
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            stages=[sedem_pb2.ServiceConfig.Stage(**{
                'name': 'testing',
                'deploy_units': ['testing'],
                'required_duration': 30 * 60,  # 1d
            }), sedem_pb2.ServiceConfig.Stage(**{
                'name': 'stable',
                'deploy_units': ['stable'],
            })],
            acceptance=[],
        ),
        testing_used=True,
    ),
    UploadTestCase(
        name='garden',
        config={
            'main': {'name': 'mock-garden'},
            'deploy': {'type': 'garden'},
            'resources': {'stable': {}, 'testing': {}},
        },
        expected_proto=sedem_pb2.ServiceConfig(
            name='maps-core-mock-garden',
            abc_slug='maps-core-mock-garden',
            path='maps/mock-garden',
            sox=False,
            release_type=sedem_pb2.ServiceConfig.ReleaseType.GARDEN,
            stages=[sedem_pb2.ServiceConfig.Stage(**{
                'name': 'testing',
                'deploy_units': ['testing'],
                'required_duration': 24 * 60 * 60,  # 1d
            }), sedem_pb2.ServiceConfig.Stage(**{
                'name': 'stable',
                'deploy_units': ['stable'],
            })],
            acceptance=[],
        ),
    ),
    UploadTestCase(
        name='sandbox',
        config={
            'main': {'name': 'mock-sandbox'},
            'deploy': {
                'type': 'sandbox',
                'sandbox': {'owner': 'MAPS_GEOINFRA'},
            },
            'resources': {
                'stable': {
                    'sandbox_templates': [{
                        'name': 'postcommit_stable',
                    }],
                    'sandbox_schedulers': [{
                        'name': 'scheduler_stable',
                        'user': 'robot-maps-sandbox',
                    }]
                },
            },
        },
        expected_proto=sedem_pb2.ServiceConfig(
            name='maps-core-mock-sandbox',
            abc_slug='maps-core-mock-sandbox',
            path='maps/mock-sandbox',
            sox=False,
            release_type=sedem_pb2.ServiceConfig.ReleaseType.SANDBOX,
            stages=[sedem_pb2.ServiceConfig.Stage(**{
                'name': 'stable',
                'deploy_units': ['postcommit_stable', 'scheduler_stable'],
            })],
            acceptance=[],
        ),
    ),
]


@pytest.mark.parametrize('case', TEST_CASES, ids=str)
def test_upload(case: UploadTestCase) -> None:
    service_config = config_factory(
        path=PurePath('maps') / case.config['main']['name'],
        config=case.config,
    )
    sedem_client = create_autospec(SedemApi, instance=True)
    testing_sedem_client = create_autospec(SedemApi, instance=True)

    upload_config = UploadConfig(
        sedem_client=sedem_client,
        testing_sedem_client=testing_sedem_client,
        **MOCK_APPLIER_PARAMS,
    )

    assert upload_config.is_applicable_for(service_config)

    upload_config(
        config=service_config,
        context=Context(),
    )

    if case.testing_used:
        assert sedem_client.mock_calls == []
        assert testing_sedem_client.mock_calls == [
            call.configuration_update(
                configuration=sedem_pb2.UpdateConfigurationRequest(service_config=case.expected_proto),
                revision=42,
            ),
        ]
    else:
        assert sedem_client.mock_calls == [
            call.configuration_update(
                configuration=sedem_pb2.UpdateConfigurationRequest(service_config=case.expected_proto),
                revision=42,
            ),
        ]
        assert testing_sedem_client.mock_calls == []
