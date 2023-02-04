import pytest

from maps.infra.sedem.common.release.sandbox.release_spec import (
    SandboxReleaseSpec,
)
from maps.infra.sedem.machine.tests.typing import (
    CommitFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.release_api import ReleaseApi, ReleaseCompletion, ReleaseStatus


@pytest.mark.asyncio
async def test_complete_hotfix_release(mongo: MongoFixture,
                                       release_factory: ReleaseFactory,
                                       service_config_factory: ServiceConfigFactory,
                                       commit_factory: CommitFactory) -> None:
    service_config = await service_config_factory(
        name='maps-sandbox-example-task',
        release_type='SANDBOX',
    )

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        origin_revision=100,
        release_arc_hash='hash1',
        release_revision=100,
    )
    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=2,
        origin_arc_hash='hash2',
        origin_revision=200,
        release_arc_hash=None,
        release_revision=None,
        completion=ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
            hotfix=ReleaseCompletion.Preparing.PreparingHotfix()
        )),
    )

    release_spec = SandboxReleaseSpec(
        task_type='MAPS_EXAMPLE_TASK',
        resource_id='1234567',
        deploy_units=[],
    )

    async with await mongo.async_client.start_session() as session:
        await ReleaseApi(session).set_hotfix_merging(
            release_id=release_id,
            operation_id='12345',
        )
        await ReleaseApi(session).set_hotfix_merged_commit(
            release_id=release_id,
            commit=commit_factory('hash3'),
        )
        await ReleaseApi(session).set_hotfix_building(
            release_id=release_id,
            operation_id='67890',
        )
        await ReleaseApi(session).set_hotfix_release_spec(
            release_id=release_id,
            release_spec=release_spec,
        )
        await ReleaseApi(session).set_hotfix_build_complete(
            release_id=release_id,
        )
        release = await ReleaseApi(session).load_release(release_id)

    assert release.release_commit.arc_commit_hash == 'hash3'
    assert release.release_spec == release_spec
    assert release.completion.status() == ReleaseStatus.READY
    assert release.completed_at is not None


@pytest.mark.asyncio
async def test_hotfix_release_merge_fail(mongo: MongoFixture,
                                         release_factory: ReleaseFactory,
                                         service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        release_arc_hash='hash1',
    )
    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=2,
        origin_arc_hash='hash2',
        release_arc_hash=None,
        completion=ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
            hotfix=ReleaseCompletion.Preparing.PreparingHotfix()
        )),
    )

    async with await mongo.async_client.start_session() as session:
        await ReleaseApi(session).set_hotfix_merging(
            release_id=release_id,
            operation_id='12345',
        )
        await ReleaseApi(session).set_hotfix_merge_failed(
            release_id=release_id,
            reason='conflict',
        )
        release = await ReleaseApi(session).load_release(release_id)

    assert release.release_commit is None
    assert release.completion.status() == ReleaseStatus.BROKEN
    assert release.completed_at is not None


@pytest.mark.asyncio
async def test_hotfix_release_build_fail(mongo: MongoFixture,
                                         release_factory: ReleaseFactory,
                                         service_config_factory: ServiceConfigFactory,
                                         commit_factory: CommitFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
        origin_arc_hash='hash1',
        release_arc_hash='hash1',
    )
    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=2,
        origin_arc_hash='hash2',
        release_arc_hash=None,
        completion=ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
            hotfix=ReleaseCompletion.Preparing.PreparingHotfix()
        )),
    )

    async with await mongo.async_client.start_session() as session:
        await ReleaseApi(session).set_hotfix_merging(
            release_id=release_id,
            operation_id='12345',
        )
        await ReleaseApi(session).set_hotfix_merged_commit(
            release_id=release_id,
            commit=commit_factory('hash3'),
        )
        await ReleaseApi(session).set_hotfix_building(
            release_id=release_id,
            operation_id='67890',
        )
        await ReleaseApi(session).set_hotfix_build_failed(
            release_id=release_id,
            reason='broken build',
        )
        release = await ReleaseApi(session).load_release(release_id)

    assert release.release_commit.arc_commit_hash == 'hash3'
    assert release.completion.status() == ReleaseStatus.BROKEN
    assert release.completed_at is not None
