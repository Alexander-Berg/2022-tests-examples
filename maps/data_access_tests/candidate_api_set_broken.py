from dataclasses import dataclass

import pytest
from bson import ObjectId

from maps.infra.sedem.common.release.garden.release_spec import (
    GardenModule,
    GardenReleaseSpec,
)
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
async def test_set_candidate_broken(mongo: MongoFixture,
                                    commit_factory: CommitFactory,
                                    service_config_factory: ServiceConfigFactory,
                                    release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        release_type='GARDEN',
    )

    await release_candidate_factory(
        service_config=service_config,
        task_id='12345',
        arc_hash='hash1',
        status=ReleaseCandidateStatus.BUILDING,
    )

    async with await mongo.async_client.start_session() as session:
        candidate_id = await ReleaseCandidateApi(session).set_candidate_broken(
            service_config=service_config,
            task_id='12345',
        )

        collection = mongo.async_client.get_database().release_candidate
        candidate_document = await collection.find_one({'_id': ObjectId(candidate_id)})
        candidate = ReleaseCandidate.parse_obj(candidate_document)

    assert candidate.service_name == service_config.name
    assert candidate.task_id == '12345'
    assert candidate.release_spec is None
    assert candidate.progress.start_time is not None
    assert candidate.progress.end_time is not None
    assert candidate.progress.status == ReleaseCandidateStatus.BROKEN


@dataclass
class BadCandidateTest:
    name: str
    task_id: str
    expected_error: str

    def __str__(self) -> str:
        return self.name


BAD_CANDIDATE_TESTS = [
    BadCandidateTest(
        name='already_set',
        task_id='12345',
        expected_error=r'No building candidate with task id #12345 for [\w-]+ found',
    ),
    BadCandidateTest(
        name='unregistered',
        task_id='67890',
        expected_error=r'No building candidate with task id #67890 for [\w-]+ found',
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', BAD_CANDIDATE_TESTS, ids=str)
async def test_set_bad_candidate_ready(mongo: MongoFixture,
                                       commit_factory: CommitFactory,
                                       service_config_factory: ServiceConfigFactory,
                                       release_candidate_factory: ReleaseCandidateFactory,
                                       case: BadCandidateTest) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        release_type='GARDEN',
    )

    release_spec = GardenReleaseSpec(
        module=GardenModule(
            name='ymapsdf',
            version='latest',
        ),
    )

    await release_candidate_factory(
        service_config=service_config,
        task_id='12345',
        arc_hash='hash1',
        release_spec=release_spec,
        status=ReleaseCandidateStatus.READY,
    )

    with pytest.raises(Exception, match=case.expected_error):
        async with await mongo.async_client.start_session() as session:
            await ReleaseCandidateApi(session).set_candidate_broken(
                service_config=service_config,
                task_id=case.task_id,
            )
