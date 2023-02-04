import pytest
from maps.infra.sedem.machine.lib.acceptance_api import AcceptanceTestSetStatus, AcceptanceApi

from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)


@pytest.mark.asyncio
@pytest.mark.parametrize('status', list(AcceptanceTestSetStatus))
async def test_set_acceptance_status(
        mongo: MongoFixture,
        release_factory: ReleaseFactory,
        service_config_factory: ServiceConfigFactory,
        acceptance_factory: AcceptanceFactory,
        status: AcceptanceTestSetStatus
):
    service_config = await service_config_factory(name='maps-fake-service')
    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='fake-hash'
    )
    acceptance_id = await acceptance_factory(
        release_id=release_id,
        deploys=[],
        tasks=[],
        status=AcceptanceTestSetStatus.PENDING,
        stage='testing',
    )
    async with await mongo.async_client.start_session() as session:
        await AcceptanceApi(session).set_acceptance_status(acceptance_id, status)

    releases = await mongo.async_client.release.find()
    assert len(releases) == 1
    release_doc = releases[0]
    assert len(release_doc['acceptance']) == 1
    acceptance = release_doc['acceptance'][0]
    assert acceptance.status == status
    if status == AcceptanceTestSetStatus.EXECUTING:
        assert acceptance['start_time']
        assert 'end_time' not in acceptance
    if status == AcceptanceTestSetStatus.FINISHED:
        assert acceptance['end_time']
