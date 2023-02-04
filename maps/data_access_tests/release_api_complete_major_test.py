import pytest

from maps.infra.sedem.machine.tests.typing import ReleaseFactory, MongoFixture, ServiceConfigFactory
from maps.infra.sedem.machine.lib.release_api import ReleaseApi, ReleaseCompletion, ReleaseStatus


@pytest.mark.asyncio
async def test_complete_major_release(mongo: MongoFixture,
                                      release_factory: ReleaseFactory,
                                      service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    release_ids = []
    for major_version in range(1, 4):
        release_ids.append(await release_factory(
            service_config=service_config,
            major_version=major_version,
            minor_version=1,
            origin_arc_hash=f'hash{major_version}',
            release_arc_hash=f'hash{major_version}',
            st_ticket=None,
            arc_branch=None,
            completion=ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                major=ReleaseCompletion.Preparing.PreparingMajor()
            )),
        ))

    async with await mongo.async_client.start_session() as session:
        await ReleaseApi(session).set_major_release_ticket(
            release_id=release_ids[1],
            st_ticket='TESTQUEUE-1',
        )
        await ReleaseApi(session).set_major_release_arc_branch(
            release_id=release_ids[1],
            arc_branch='release/maps/fake-service',
        )
        await ReleaseApi(session).set_major_release_complete(
            release_id=release_ids[1],
        )
        release = await ReleaseApi(session).load_release(release_ids[1])

    assert release.st_ticket == 'TESTQUEUE-1'
    assert release.arc_branch == 'release/maps/fake-service'
    assert release.completion.status() == ReleaseStatus.READY
    assert release.completed_at is not None
