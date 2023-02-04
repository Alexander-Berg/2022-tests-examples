from itertools import islice

import pytest

from maps.infra.sedem.machine.tests.typing import (
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory
)
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.release_api import ReleaseApi, ReleaseCompletion, ReleaseStatus


async def init_lookup_data(service_config: ServiceConfig, release_factory: ReleaseFactory) -> int:
    total = 0
    for major_version in range(1, 11):
        for minor_version in range(1, 5):
            if major_version < 9:
                completion = {
                    1: ReleaseCompletion(ready=ReleaseCompletion.Ready()),
                    2: ReleaseCompletion(broken=ReleaseCompletion.Broken(
                        build=ReleaseCompletion.Broken.BrokenBuild(reason='some reason')
                    )),
                    3: ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                        build=ReleaseCompletion.Preparing.PreparingBuild(operation_id='12345')
                    )),
                    4: ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                        hotfix=ReleaseCompletion.Preparing.PreparingHotfix()
                    )),
                }[minor_version]
            else:
                completion = ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                    major=ReleaseCompletion.Preparing.PreparingMajor()
                ))
            if minor_version < 4:
                release_arc_hash = f'hash{major_version}.{minor_version}.{1 if minor_version == 1 else 2}'
                release_revision = major_version * 100 + minor_version * 10 + (1 if minor_version == 1 else 2)
            else:
                release_arc_hash = None
                release_revision = None
            await release_factory(
                service_config=service_config,
                major_version=major_version,
                minor_version=minor_version,
                origin_arc_hash=f'hash{major_version}.{minor_version}.1',
                origin_revision=major_version * 100 + minor_version * 10 + 1,
                release_arc_hash=release_arc_hash,
                release_revision=release_revision,
                completion=completion,
            )
            total += 1
            if minor_version == 1 and completion.status() != ReleaseStatus.READY:
                break
    return total


@pytest.mark.asyncio
async def test_lookup_all(mongo: MongoFixture,
                          release_factory: ReleaseFactory,
                          service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    total = await init_lookup_data(service_config, release_factory)

    async with await mongo.async_client.start_session() as session:
        releases, _ = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
        )

    assert len(releases) == total
    for i, release in enumerate(islice(releases, 1, None), start=1):
        prev_release = releases[i-1]
        assert (prev_release.major, prev_release.minor) >= (release.major, release.minor)


@pytest.mark.asyncio
async def test_lookup_pagination(mongo: MongoFixture,
                                 release_factory: ReleaseFactory,
                                 service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    total = await init_lookup_data(service_config, release_factory)

    limit = 10
    pages = 0
    async with await mongo.async_client.start_session() as session:
        next_id = None
        while True:
            releases, next_id = await ReleaseApi(session).lookup_releases(
                service_config=service_config,
                limit=limit,
                next_id=next_id
            )
            if next_id is None:
                break
            pages += 1
            assert len(releases) <= limit

    assert pages == total // limit


@pytest.mark.asyncio
async def test_lookup_by_arc_hash(mongo: MongoFixture,
                                  release_factory: ReleaseFactory,
                                  service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    # Lookup major release
    async with await mongo.async_client.start_session() as session:
        releases, _ = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
            arc_commit_hash='hash3.1.1',
        )

    assert len(releases) == 1
    assert releases[0].origin_commit.arc_commit_hash == 'hash3.1.1'
    assert releases[0].release_commit.arc_commit_hash == 'hash3.1.1'

    # Lookup hotfix
    async with await mongo.async_client.start_session() as session:
        releases, _ = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
            arc_commit_hash='hash3.2.2',
        )

    assert len(releases) == 1
    assert releases[0].origin_commit.arc_commit_hash == 'hash3.2.1'
    assert releases[0].release_commit.arc_commit_hash == 'hash3.2.2'


@pytest.mark.asyncio
async def test_lookup_by_revision(mongo: MongoFixture,
                                  release_factory: ReleaseFactory,
                                  service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    # Lookup major release
    async with await mongo.async_client.start_session() as session:
        releases, _ = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
            revision=311,
        )

    assert len(releases) == 1
    assert releases[0].origin_commit.revision == 311
    assert releases[0].release_commit.revision == 311

    # Lookup hotfix
    async with await mongo.async_client.start_session() as session:
        releases, _ = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
            revision=322,
        )

    assert len(releases) == 1
    assert releases[0].origin_commit.revision == 321
    assert releases[0].release_commit.revision == 322


@pytest.mark.asyncio
@pytest.mark.parametrize('lookup_status', list(ReleaseStatus))
async def test_lookup_by_status(mongo: MongoFixture,
                                release_factory: ReleaseFactory,
                                service_config_factory: ServiceConfigFactory,
                                lookup_status: ReleaseStatus) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    async with await mongo.async_client.start_session() as session:
        releases, _ = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
            status=lookup_status,
        )

    if lookup_status == ReleaseStatus.PREPARING:
        assert len(releases) and all(
            release.completion.status() in (ReleaseStatus.MERGING, ReleaseStatus.BUILDING, ReleaseStatus.PREPARING)
            for release in releases
        )
    else:
        assert len(releases) and all(
            release.completion.status() == lookup_status
            for release in releases
        )


@pytest.mark.asyncio
async def test_lookup_empty(mongo: MongoFixture,
                            service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    async with await mongo.async_client.start_session() as session:
        releases, next_id = await ReleaseApi(session).lookup_releases(
            service_config=service_config,
        )

    assert len(releases) == 0
    assert next_id is None


@pytest.mark.asyncio
async def test_load_existing_release(mongo: MongoFixture,
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
        release = await ReleaseApi(session).load_release(release_ids[1])
    assert release.major == 2


@pytest.mark.asyncio
async def test_load_nonexistent_release(mongo: MongoFixture,
                                        release_factory: ReleaseFactory,
                                        service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    with pytest.raises(Exception, match=r'No release \w+ found'):
        async with await mongo.async_client.start_session() as session:
            await ReleaseApi(session).load_release('deadbeef1234567890abcdef')


@pytest.mark.asyncio
async def test_load_bad_release_id(mongo: MongoFixture,
                                   release_factory: ReleaseFactory,
                                   service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    with pytest.raises(Exception, match=r'Invalid deploy id passed'):
        async with await mongo.async_client.start_session() as session:
            await ReleaseApi(session).load_release('not-an-id')


@pytest.mark.asyncio
async def test_lookup_incomplete_major_releases(mongo: MongoFixture,
                                                release_factory: ReleaseFactory,
                                                service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    async with await mongo.async_client.start_session() as session:
        release_ids = [
            release_id
            async for release_id in ReleaseApi(session).lookup_incomplete_major_release_ids()
        ]
        assert all(isinstance(release_id, str) for release_id in release_ids)
        releases = [
            await ReleaseApi(session).load_release(release_id)
            for release_id in release_ids
        ]

    assert len(releases) and all(
        release.completion.status() == ReleaseStatus.PREPARING
        for release in releases
    )


@pytest.mark.asyncio
async def test_lookup_hotfixes_without_merge_task(mongo: MongoFixture,
                                                  release_factory: ReleaseFactory,
                                                  service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    async with await mongo.async_client.start_session() as session:
        release_ids = [
            release_id
            async for release_id in ReleaseApi(session).lookup_hotfix_ids_without_merge_task()
        ]
        assert all(isinstance(release_id, str) for release_id in release_ids)
        releases = [
            await ReleaseApi(session).load_release(release_id)
            for release_id in release_ids
        ]

    assert len(releases) and all(
        release.completion.preparing.hotfix.operation_id is None
        for release in releases
    )


@pytest.mark.asyncio
async def test_lookup_major_release(mongo: MongoFixture,
                                    release_factory: ReleaseFactory,
                                    service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    async with await mongo.async_client.start_session() as session:
        release = await ReleaseApi(session).lookup_major_release(
            service_config=service_config,
            major_version=5,
        )
    assert release.service_name == service_config.name
    assert release.major == 5
    assert release.minor == 1


@pytest.mark.asyncio
async def test_lookup_latest_major_release(mongo: MongoFixture,
                                           release_factory: ReleaseFactory,
                                           deploy_factory: DeployFactory,
                                           service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)

    async with await mongo.async_client.start_session() as session:
        release = await ReleaseApi(session).lookup_latest_by_revision_major_release(
            service_config=service_config,
        )
    assert release.service_name == service_config.name
    assert release.major == 10
    assert release.minor == 1
    assert release.origin_commit.revision == 1011  # major * 100 + minor * 10 + 1


@pytest.mark.asyncio
async def test_lookup_latest_major_release_deployed_to_stable(mongo: MongoFixture,
                                                              release_factory: ReleaseFactory,
                                                              deploy_factory: DeployFactory,
                                                              service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_factory)
    for major_version in range(1, 6):
        await deploy_factory(
            service_config=service_config,
            release_major_version=major_version,
            release_minor_version=1,
            author='john-doe',
            deploy_unit='stable',
        )

    async with await mongo.async_client.start_session() as session:
        release = await ReleaseApi(session).lookup_latest_major_release_deployed_to_stable(
            service_config=service_config,
            upper_bound_major_version=10,
        )
    assert release.service_name == service_config.name
    assert release.major == 5
    assert release.minor == 1
