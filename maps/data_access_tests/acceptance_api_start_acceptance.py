import pytest
from bson import ObjectId

from maps.infra.sedem.machine.tests.typing import (
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.acceptance_api import AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestStatus
from maps.infra.sedem.machine.lib.config_api import ServiceConfig


@pytest.mark.asyncio
async def test_start_acceptance(mongo: MongoFixture, release_factory: ReleaseFactory, deploy_factory: DeployFactory,
                                service_config_factory: ServiceConfigFactory):
    service_config = await service_config_factory(
        name='maps-fake-service',
        sox=False,
        acceptances=[
            ServiceConfig.AcceptanceTestConfiguration(stage='testing', scheduler_id='1')
        ],
    )
    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )
    deploy_id = await deploy_factory(
        service_config=service_config,
    )

    async with await mongo.async_client.start_session() as session:
        await AcceptanceApi(session).start_acceptance(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            deploy_ids=[deploy_id],
            stage='testing',
        )

    release_document = await mongo.async_client.get_database().release.find_one({'_id': ObjectId(release_id)})
    assert len(release_document['acceptance']) == 1
    acceptance_document = release_document['acceptance'][0]

    assert acceptance_document['status'] == AcceptanceTestSetStatus.PENDING
    assert len(acceptance_document['launches']) == 1
    launch = acceptance_document['launches'][0]
    assert launch['status'] == AcceptanceTestStatus.PENDING
    assert launch['scheduler_id'] == '1'

    assert len(acceptance_document['deploys']) == 1
    assert acceptance_document['deploys'][0] == ObjectId(deploy_id)


@pytest.mark.asyncio
async def test_start_acceptance_for_nonexistent_release(mongo: MongoFixture,
                                                        service_config_factory: ServiceConfigFactory):
    service_config = await service_config_factory(
        name='maps-fake-service',
        sox=False,
        acceptances=[
            ServiceConfig.AcceptanceTestConfiguration(stage='testing', scheduler_id='1')
        ],
    )
    with pytest.raises(AssertionError, match=r'Unable to create acceptance for maps-fake-service release v1.1'):
        async with await mongo.async_client.start_session() as session:
            await AcceptanceApi(session).start_acceptance(
                service_config=service_config,
                major_version=1,
                minor_version=1,
                deploy_ids=[str(ObjectId())],
                stage='testing',
            )


@pytest.mark.asyncio
async def test_start_no_acceptances(mongo: MongoFixture, service_config_factory: ServiceConfigFactory):
    service_config = await service_config_factory(
        name='maps-fake-service',
        sox=False
    )
    async with await mongo.async_client.start_session() as session:
        acceptance_id = await AcceptanceApi(session).start_acceptance(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            deploy_ids=[str(ObjectId())],
            stage='testing',
        )
    assert acceptance_id is None


@pytest.mark.asyncio
async def test_start_acceptances_in_other_stage(mongo: MongoFixture, service_config_factory: ServiceConfigFactory):
    service_config = await service_config_factory(
        name='maps-fake-service',
        sox=False,
        acceptances=[
            ServiceConfig.AcceptanceTestConfiguration(stage='testing', scheduler_id='1')
        ],
    )
    async with await mongo.async_client.start_session() as session:
        acceptance_id = await AcceptanceApi(session).start_acceptance(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            deploy_ids=[str(ObjectId())],
            stage='prestable',
        )
    assert acceptance_id is None
