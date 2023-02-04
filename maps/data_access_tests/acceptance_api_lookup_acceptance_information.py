import pytest

from maps.infra.sedem.machine.lib.acceptance_api import AcceptanceApi, AcceptanceTestSetStatus
from maps.infra.sedem.machine.lib.deploy_api import DeployStatusDocument
from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)


@pytest.mark.asyncio
async def test_lookup_release_info(mongo: MongoFixture,
                                   release_factory: ReleaseFactory,
                                   deploy_factory: DeployFactory,
                                   service_config_factory: ServiceConfigFactory,
                                   acceptance_factory: AcceptanceFactory):
    service_config = await service_config_factory(name='maps-fake-service')

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        release_arc_hash='my-super-hash'
    )
    deploy_id = await deploy_factory(
        service_config=service_config,
        author='john-doe',
        status=DeployStatusDocument(success=DeployStatusDocument.Success())
    )
    acceptance_id = await acceptance_factory(
        release_id=release_id,
        deploys=[deploy_id],
        stage='testing',
        tasks=[],
        status=AcceptanceTestSetStatus.PENDING,
    )

    async with await mongo.async_client.start_session() as session:
        acceptance_info = await AcceptanceApi(session).lookup_acceptance_information(acceptance_id)

    assert acceptance_info.arc_commit_hash == 'my-super-hash'
    assert acceptance_info.service_name == 'maps-fake-service'
    assert acceptance_info.minor_version == 1
    assert acceptance_info.major_version == 1
    assert acceptance_info.stage == 'testing'
    assert acceptance_info.deploy_author == 'john-doe'
