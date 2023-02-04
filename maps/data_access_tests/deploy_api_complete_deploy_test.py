import pytest

from maps.infra.sedem.machine.tests.typing import (
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.deploy_api import DeployApi, DeployStatus, DeployStatusDocument, ActivationStatus


@pytest.mark.asyncio
async def test_complete_deploy(mongo: MongoFixture,
                               release_factory: ReleaseFactory,
                               deploy_factory: DeployFactory,
                               service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        stages=[
            ServiceConfig.Stage(
                name=name,
                deploy_units=list(deploy_units),
            )
            for name, deploy_units in (
                ('testing', ('testing', 'load')),
                ('prestable', ('prestable')),
                ('stable', ('stable')),
            )
        ],
    )

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    deploy_ids = []
    for deploy_unit in ('testing', 'load'):
        deploy_ids.append(await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                executing=DeployStatusDocument.Executing(),
            ),
        ))
    testing_deploy_id, load_deploy_id = deploy_ids

    async with await mongo.async_client.start_session() as session:
        await DeployApi(session).complete_deploy(
            deploy_id=load_deploy_id,
            status=ActivationStatus.INACTIVE,
        )
        await DeployApi(session).complete_deploy(
            deploy_id=testing_deploy_id,
            status=ActivationStatus.ACTIVE,
        )
        testing_deploy = await DeployApi(session).load_deploy(testing_deploy_id)
        load_deploy = await DeployApi(session).load_deploy(load_deploy_id)

    assert testing_deploy.status == DeployStatus.SUCCESS
    assert load_deploy.status == DeployStatus.CANCELLED
