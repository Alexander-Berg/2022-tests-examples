from datetime import datetime, timedelta, timezone
from freezegun import freeze_time

import pytest

from maps.infra.sedem.machine.tests.typing import (
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.deploy_api import DeployApi, DeployStatus, DeployStatusDocument


async def init_service_config(service_config_factory: ServiceConfigFactory) -> None:
    return await service_config_factory(
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


@pytest.mark.asyncio
async def test_load_existing_deploy(mongo: MongoFixture,
                                    release_factory: ReleaseFactory,
                                    deploy_factory: DeployFactory,
                                    service_config_factory: ServiceConfigFactory) -> None:
    service_config = await init_service_config(service_config_factory)

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    deploy_ids = []
    for deploy_unit in ('testing', 'load', 'prestable', 'stable'):
        deploy_ids.append(await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                success=DeployStatusDocument.Success(),
            ),
        ))

    async with await mongo.async_client.start_session() as session:
        deploy = await DeployApi(session).load_deploy(deploy_ids[2])
    assert deploy.deploy_unit == 'prestable'
    assert deploy.status == DeployStatus.SUCCESS


@pytest.mark.asyncio
async def test_load_nonexistent_deploy(mongo: MongoFixture,
                                       release_factory: ReleaseFactory,
                                       service_config_factory: ServiceConfigFactory) -> None:
    service_config = await init_service_config(service_config_factory)

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    with pytest.raises(Exception, match=r'No deploy \w+ found'):
        async with await mongo.async_client.start_session() as session:
            await DeployApi(session).load_deploy('deadbeef1234567890abcdef')


@pytest.mark.asyncio
async def test_lookup_executing_deploys(mongo: MongoFixture,
                                        release_factory: ReleaseFactory,
                                        deploy_factory: DeployFactory,
                                        service_config_factory: ServiceConfigFactory) -> None:
    service_config = await init_service_config(service_config_factory)

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    for deploy_unit in ('testing', 'load'):
        await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                success=DeployStatusDocument.Success(),
            ),
        )
    expected_deploy_ids = []
    for deploy_unit in ('prestable', 'stable'):
        deploy_id = await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                executing=DeployStatusDocument.Executing(),
            ),
        )
        expected_deploy_ids.append(deploy_id)

    async with await mongo.async_client.start_session() as session:
        deploy_ids_cursor = DeployApi(session).lookup_executing_deploy_ids()
        actual_deploy_ids = [deploy_id async for deploy_id in deploy_ids_cursor]
        expected_deploys = [
            await DeployApi(session).load_deploy(deploy_id)
            for deploy_id in expected_deploy_ids
        ]
        actual_deploys = [
            await DeployApi(session).load_deploy(deploy_id)
            for deploy_id in actual_deploy_ids
        ]
    actual_deploys.sort(key=lambda deploy: deploy.deploy_id)
    expected_deploys.sort(key=lambda deploy: deploy.deploy_id)

    assert actual_deploys == expected_deploys


@pytest.mark.asyncio
async def test_lookup_too_long_deploys(mongo: MongoFixture,
                                       release_factory: ReleaseFactory,
                                       deploy_factory: DeployFactory,
                                       service_config_factory: ServiceConfigFactory) -> None:
    service_config = await init_service_config(service_config_factory)

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )
    expected_deploy_ids = []
    with freeze_time(datetime.now(timezone.utc) - timedelta(hours=2)):
        for deploy_unit in ('prestable', 'stable'):
            deploy_id = await deploy_factory(
                service_config=service_config,
                release_major_version=1,
                release_minor_version=1,
                author='john-doe',
                deploy_unit=deploy_unit,
                status=DeployStatusDocument(
                    executing=DeployStatusDocument.Executing()
                ),
            )
            expected_deploy_ids.append(deploy_id)

        for deploy_unit in ('testing', 'load'):
            await deploy_factory(
                service_config=service_config,
                release_major_version=1,
                release_minor_version=1,
                author='john-doe',
                deploy_unit=deploy_unit,
                status=DeployStatusDocument(
                    success=DeployStatusDocument.Success(),
                ),
            )

    for deploy_unit in ('stable',):
        await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                success=DeployStatusDocument.Executing(),
            ),
        )
    async with await mongo.async_client.start_session() as session:
        deploy_cursor = DeployApi(session).lookup_too_long_deploys()
        actual_deploys = []
        async for elem in deploy_cursor:
            actual_deploys += elem.deploys
        expected_deploys = [
            await DeployApi(session).load_deploy(deploy_id)
            for deploy_id in expected_deploy_ids
        ]
    actual_deploys.sort(key=lambda deploy: deploy.deploy_id)
    expected_deploys.sort(key=lambda deploy: deploy.deploy_id)

    assert actual_deploys == expected_deploys
