import asyncio
import typing as tp
from dataclasses import dataclass

import pytest

from maps.infra.sedem.common.release.nanny.release_spec import (
    DockerImage,
    NannyReleaseSpec,
)
from maps.infra.sedem.machine.tests.typing import (
    CommitFactory,
    MongoFixture,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.release_api import ReleaseApi, ReleaseStatus


@pytest.mark.asyncio
async def test_create_major_release(mongo: MongoFixture,
                                    commit_factory: CommitFactory,
                                    service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    async with await mongo.async_client.start_session() as session:
        release_id = await ReleaseApi(session).create_major_release(
            service_config=service_config,
            major_version=1,
            author='john-doe',
            commit=commit_factory('hash1'),
        )

        release = await ReleaseApi(session).load_release(release_id)

    assert release.major == 1
    assert release.minor == 1
    assert release.completion.status() == ReleaseStatus.PREPARING


@pytest.mark.asyncio
async def test_create_major_release_with_spec(mongo: MongoFixture,
                                              commit_factory: CommitFactory,
                                              service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    release_spec = NannyReleaseSpec(
        docker=DockerImage(name='maps/core-teapot', tag='latest'),
        environments=[],
    )

    async with await mongo.async_client.start_session() as session:
        release_id = await ReleaseApi(session).create_major_release(
            service_config=service_config,
            major_version=1,
            author='john-doe',
            commit=commit_factory('hash1'),
            release_spec=release_spec,
        )

        release = await ReleaseApi(session).load_release(release_id)

    assert release.major == 1
    assert release.minor == 1
    assert release.release_spec == release_spec
    assert release.completion.status() == ReleaseStatus.PREPARING


@dataclass
class ConcurrentCreationTest:
    @dataclass
    class Params:
        major: int
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
            major=1,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            major=1,
            author='john-doe',
            commit_hash='hash1',
        ),
        expected_error=None,
    ),
    ConcurrentCreationTest(
        name='same_version_and_commit_diff_authors',
        first=ConcurrentCreationTest.Params(
            major=1,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            major=1,
            author='jane-doe',
            commit_hash='hash1',
        ),
        expected_error=None,
    ),
    ConcurrentCreationTest(
        name='same_commit_diff_versions',
        first=ConcurrentCreationTest.Params(
            major=1,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            major=2,
            author='john-doe',
            commit_hash='hash1',
        ),
        expected_error=r'Commit \w+ already released',
    ),
    ConcurrentCreationTest(
        name='same_version_diff_commits',
        first=ConcurrentCreationTest.Params(
            major=1,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            major=1,
            author='john-doe',
            commit_hash='hash2',
        ),
        expected_error=r'Release v1\.1 already created',
    ),
    ConcurrentCreationTest(
        name='diff_versions_diff_commits',
        first=ConcurrentCreationTest.Params(
            major=1,
            author='john-doe',
            commit_hash='hash1',
        ),
        second=ConcurrentCreationTest.Params(
            major=2,
            author='john-doe',
            commit_hash='hash2',
        ),
        expected_error=None,
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CONCURRENT_TEST_CASES, ids=str)
async def test_concurrent_create_major(mongo: MongoFixture,
                                       commit_factory: CommitFactory,
                                       service_config_factory: ServiceConfigFactory,
                                       case: ConcurrentCreationTest) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    async with await mongo.async_client.start_session() as session1:
        async with await mongo.async_client.start_session() as session2:
            results = await asyncio.gather(
                ReleaseApi(session1).create_major_release(
                    service_config=service_config,
                    major_version=case.first.major,
                    author=case.first.author,
                    commit=commit_factory(case.first.commit_hash),
                ),
                ReleaseApi(session2).create_major_release(
                    service_config=service_config,
                    major_version=case.second.major,
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

            assert release.major in (case.first.major, case.second.major)
            assert release.minor == 1
            assert release.completion.status() == ReleaseStatus.PREPARING
