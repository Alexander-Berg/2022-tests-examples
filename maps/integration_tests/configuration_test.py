import logging
import typing as tp

import pytest

from maps.infra.sedem.client.machine_api import MachineApi, MachineNotFoundError
from maps.infra.sedem.client.sedem_api import SedemApi
from maps.infra.sedem.proto import sedem_pb2
from maps.pylibs.fixtures.api_fixture import ApiFixture

logger = logging.getLogger(__name__)


FAKE_OAUTH = 'OAuth AQAD-FAKE'


class TestConfiguration:

    @staticmethod
    def configuration_update(configs: tp.List[sedem_pb2.ServiceConfig], revision: int):
        sedem_api = SedemApi(FAKE_OAUTH)
        for config in configs:
            request = sedem_pb2.UpdateConfigurationRequest(
                service_config=config,
            )
            sedem_api.configuration_update(
                configuration=request,
                revision=revision
            )

    def test_configuration_get(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa

        teacup_config = sedem_pb2.ServiceConfig(
            name='maps-core-teacup',
            path='maps/infra/teacup',
            abc_slug='maps-core-teacup',
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            sox=False,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                sedem_pb2.ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
            acceptance=[
                sedem_pb2.AcceptanceTest(
                    stage='testing',
                    sandbox_scheduler=sedem_pb2.AcceptanceTest.SandboxScheduler(
                        id='12345'
                    )
                ),
                sedem_pb2.AcceptanceTest(
                    stage='testing',
                    sandbox_template=sedem_pb2.AcceptanceTest.SandboxTemplate(
                        name='MY_FANCY_TEMPLATE'
                    )
                ),
            ]
        )
        ymapsdf_config = sedem_pb2.ServiceConfig(
            name='maps-garden-ymapsdf',
            path='maps/garden/modules/ymapsdf',
            abc_slug='maps-garden-ymapsdf',
            release_type=sedem_pb2.ServiceConfig.ReleaseType.GARDEN,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                sedem_pb2.ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
        )
        b2bgeo_config = sedem_pb2.ServiceConfig(
            name='maps-b2bgeo-pipedrive-gate',
            path='maps/b2bgeo/pipedrive_gate',
            abc_slug='maps-b2bgeo-pipedrive-gate',
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                sedem_pb2.ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
        )
        teaspoon_config = sedem_pb2.ServiceConfig(
            name='maps-core-teaspoon',
            path='maps/infra/teaspoon',
            abc_slug='maps-core-teaspoon',
            release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
            sox=True,
            stages=[
                sedem_pb2.ServiceConfig.Stage(name='testing', deploy_units=['testing']),
                sedem_pb2.ServiceConfig.Stage(name='stable', deploy_units=['stable']),
            ],
        )
        self.configuration_update(
            configs=[teacup_config, ymapsdf_config, b2bgeo_config, teaspoon_config],
            revision=6536417
        )

        machine_api = MachineApi(FAKE_OAUTH)
        teacup_config_machine = machine_api.configuration_get(service_name='maps-core-teacup')
        assert teacup_config == teacup_config_machine.service_config
        ymapsdf_config_machine = machine_api.configuration_get(service_name='maps-garden-ymapsdf')
        assert ymapsdf_config == ymapsdf_config_machine.service_config
        b2b_config_machine = machine_api.configuration_get(service_name='maps-b2bgeo-pipedrive-gate')
        assert b2bgeo_config == b2b_config_machine.service_config
        teaspoon_config_machine = machine_api.configuration_get(service_name='maps-core-teaspoon')
        assert teaspoon_config == teaspoon_config_machine.service_config

    def test_nonexistent_configuration(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa

        machine_api = MachineApi(FAKE_OAUTH)
        with pytest.raises(MachineNotFoundError):
            machine_api.configuration_get(service_name='maps-core-teacup')
