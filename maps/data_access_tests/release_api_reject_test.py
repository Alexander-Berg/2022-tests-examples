import pytest

from maps.infra.sedem.machine.tests.typing import ReleaseFactory, MongoFixture, ServiceConfigFactory
from maps.infra.sedem.machine.lib.release_api import ReleaseApi


@pytest.mark.asyncio
async def test_reject(mongo: MongoFixture,
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
        ))

    async with await mongo.async_client.start_session() as session:
        release_id = await ReleaseApi(session).reject_release(
            service_config=service_config,
            major_version=2,
            minor_version=1,
            author='john-doe',
            reason='reject reason',
        )
        assert isinstance(release_id, str)
        release = await ReleaseApi(session).load_release(release_id)

    assert release_ids[1] == release_id
    assert release.rejected.author == 'john-doe'
    assert release.rejected.reason == 'reject reason'


@pytest.mark.asyncio
async def test_reject_nonexistent_release(mongo: MongoFixture,
                                          service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    with pytest.raises(Exception, match=r'No release v1\.1 for maps-fake-service found'):
        async with await mongo.async_client.start_session() as session:
            await ReleaseApi(session).reject_release(
                service_config=service_config,
                major_version=1,
                minor_version=1,
                author='john-doe',
                reason='reject reason',
            )


@pytest.mark.asyncio
async def test_reject_already_rejected(mongo: MongoFixture,
                                       release_factory: ReleaseFactory,
                                       service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    existing_release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    async with await mongo.async_client.start_session() as session:
        await ReleaseApi(session).reject_release(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            author='john-doe',
            reason='reject reason',
        )
        release_id = await ReleaseApi(session).reject_release(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            author='jane-doe',
            reason='another reject reason',
        )
        assert isinstance(release_id, str)
        release = await ReleaseApi(session).load_release(release_id)

    assert existing_release_id == release_id
    assert release.rejected.author == 'john-doe'
    assert release.rejected.reason == 'reject reason'
