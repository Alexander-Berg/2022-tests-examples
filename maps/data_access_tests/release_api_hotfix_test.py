import asyncio
import typing as tp
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.tests.typing import (
    CommitFactory, ReleaseFactory, MongoFixture, ServiceConfigFactory
)
from maps.infra.sedem.machine.lib.release_api import ReleaseApi, ReleaseStatus


@pytest.mark.asyncio
async def test_create_hotfix(mongo: MongoFixture,
                             release_factory: ReleaseFactory,
                             commit_factory: CommitFactory,
                             service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        release_arc_hash='hash1',
    )

    async with await mongo.async_client.start_session() as session:
        release_id = await ReleaseApi(session).create_hotfix(
            service_config=service_config,
            major_version=1,
            minor_version=2,
            author='jane-doe',
            commit=commit_factory('hash2'),
        )

        release = await ReleaseApi(session).load_release(release_id)

    assert release.major == 1
    assert release.minor == 2
    assert release.completion.status() == ReleaseStatus.MERGING


@pytest.mark.asyncio
async def test_create_hotfix_without_major(mongo: MongoFixture,
                                           commit_factory: CommitFactory,
                                           service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    with pytest.raises(Exception, match=r'Release v1.1 not found and cannot be hotfixed'):
        async with await mongo.async_client.start_session() as session:
            await ReleaseApi(session).create_hotfix(
                service_config=service_config,
                major_version=1,
                minor_version=2,
                author='jane-doe',
                commit=commit_factory('hash2'),
            )


@pytest.mark.asyncio
async def test_create_hotfix_from_same_major_commit(mongo: MongoFixture,
                                                    release_factory: ReleaseFactory,
                                                    commit_factory: CommitFactory,
                                                    service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        release_arc_hash='hash1',
    )

    with pytest.raises(Exception, match=r'Commit \w+ is already contained in release v1.1'):
        async with await mongo.async_client.start_session() as session:
            await ReleaseApi(session).create_hotfix(
                service_config=service_config,
                major_version=1,
                minor_version=2,
                author='jane-doe',
                commit=commit_factory('hash1'),
            )


@pytest.mark.asyncio
async def test_create_hotfix_from_another_major_commit(mongo: MongoFixture,
                                                       release_factory: ReleaseFactory,
                                                       commit_factory: CommitFactory,
                                                       service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        release_arc_hash='hash1',
    )
    await release_factory(
        service_config=service_config,
        major_version=2,
        minor_version=1,
        origin_arc_hash='hash2',
        release_arc_hash='hash2',
    )

    async with await mongo.async_client.start_session() as session:
        release_id = await ReleaseApi(session).create_hotfix(
            service_config=service_config,
            major_version=1,
            minor_version=2,
            author='jane-doe',
            commit=commit_factory('hash2'),
        )

        release = await ReleaseApi(session).load_release(release_id)

    assert release.major == 1
    assert release.minor == 2


@dataclass
class ConcurrentCreationTest:
    @dataclass
    class Params:
        minor: int
        author: str
        commit_hash: str

    name: str
    first: 'Params'
    second: 'Params'

    expected_error: tp.Optional[str]

    def __str__(self) -> str:
        return self.name


CONCURRENT_TEST_CASES = [
    ConcurrentCreationTest(
        name='retry',
        first=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        expected_error=None,
    ),
    ConcurrentCreationTest(
        name='same_version_and_commit_diff_authors',
        first=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            minor=2,
            author='jane-doe',
            commit_hash='hash1',
        ),
        expected_error=None,
    ),
    ConcurrentCreationTest(
        name='same_commit_diff_versions',
        first=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            minor=3,
            author='john-doe',
            commit_hash='hash1',
        ),
        expected_error=None,
    ),
    ConcurrentCreationTest(
        name='same_version_diff_commits',
        first=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash2',
        ),
        expected_error=r'Hotfix v1\.2 already created',
    ),
    ConcurrentCreationTest(
        name='diff_versions_diff_commits',
        first=ConcurrentCreationTest.Params(
            minor=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            minor=3,
            author='john-doe',
            commit_hash='hash2',
        ),
        expected_error=r'Another hotfix for v1 is merging',
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CONCURRENT_TEST_CASES, ids=str)
async def test_concurrent_hotfix(mongo: MongoFixture,
                                 release_factory: ReleaseFactory,
                                 commit_factory: CommitFactory,
                                 service_config_factory: ServiceConfigFactory,
                                 case: ConcurrentCreationTest) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash0',
        release_arc_hash='hash0',
    )

    async with await mongo.async_client.start_session() as session1:
        async with await mongo.async_client.start_session() as session2:
            results = await asyncio.gather(
                ReleaseApi(session1).create_hotfix(
                    service_config=service_config,
                    major_version=1,
                    minor_version=case.first.minor,
                    author=case.first.author,
                    commit=commit_factory(case.first.commit_hash),
                ),
                ReleaseApi(session2).create_hotfix(
                    service_config=service_config,
                    major_version=1,
                    minor_version=case.second.minor,
                    author=case.second.author,
                    commit=commit_factory(case.second.commit_hash),
                ),
                return_exceptions=True
            )

    release_ids = []
    errors = []
    for result in results:
        if isinstance(result, Exception):
            errors.append(result)
        else:
            assert isinstance(result, str)
            release_ids.append(result)

    if case.expected_error is not None:
        assert len(errors)  # expected error not found
        with pytest.raises(Exception, match=case.expected_error):
            raise errors.pop()
    if errors:
        raise errors.pop()  # unexpected error

    async with await mongo.async_client.start_session() as session:
        for release_id in release_ids:
            release = await ReleaseApi(session).load_release(release_id)

            assert release.major == 1
            assert release.minor in (case.first.minor, case.second.minor)
            assert release.completion.status() == ReleaseStatus.MERGING
