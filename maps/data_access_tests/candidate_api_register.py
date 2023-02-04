import pytest
from bson import ObjectId

from maps.infra.sedem.common.release.nanny.release_spec import (
    DockerImage,
    NannyReleaseSpec,
)
from maps.infra.sedem.common.release.vcs_version import Commit
from maps.infra.sedem.machine.tests.typing import (
    CommitFactory,
    MongoFixture,
    ReleaseCandidateFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.candidate_api import (
    ReleaseCandidate,
    ReleaseCandidateApi,
    ReleaseCandidateStatus,
)


@pytest.mark.asyncio
async def test_register_candidate(mongo: MongoFixture,
                                  commit_factory: CommitFactory,
                                  service_config_factory: ServiceConfigFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')

    async with await mongo.async_client.start_session() as session:
        candidate_id = await ReleaseCandidateApi(session).register_candidate(
            service_config=service_config,
            task_id='12345',
            commit=Commit.parse_arc_proto(commit_factory('hash1')),
        )

        collection = mongo.async_client.get_database().release_candidate
        candidate_document = await collection.find_one({'_id': ObjectId(candidate_id)})
        candidate = ReleaseCandidate.parse_obj(candidate_document)

    assert candidate.service_name == service_config.name
    assert candidate.task_id == '12345'
    assert candidate.progress.start_time is not None
    assert candidate.progress.status == ReleaseCandidateStatus.BUILDING


@pytest.mark.asyncio
async def test_register_already_registered_candidate(mongo: MongoFixture,
                                                     commit_factory: CommitFactory,
                                                     service_config_factory: ServiceConfigFactory,
                                                     release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        release_type='NANNY',
    )

    await release_candidate_factory(
        service_config=service_config,
        task_id='12345',
        arc_hash='hash1',
        release_spec=NannyReleaseSpec(
            docker=DockerImage(name='maps/core-teapot', tag='latest'),
            environments=[],
        ),
        status=ReleaseCandidateStatus.READY,
    )

    async with await mongo.async_client.start_session() as session:
        candidate_id = await ReleaseCandidateApi(session).register_candidate(
            service_config=service_config,
            task_id='45678',
            commit=Commit.parse_arc_proto(commit_factory('hash1')),
        )

        collection = mongo.async_client.get_database().release_candidate
        candidate_document = await collection.find_one({'_id': ObjectId(candidate_id)})
        candidate = ReleaseCandidate.parse_obj(candidate_document)

    assert candidate.service_name == service_config.name
    assert candidate.task_id == '45678'
    assert candidate.release_spec is None
    assert candidate.progress.start_time is not None
    assert candidate.progress.status == ReleaseCandidateStatus.BUILDING
