from itertools import islice

import pytest

from maps.infra.sedem.common.release.garden.release_spec import (
    GardenModule,
    GardenReleaseSpec,
)
from maps.infra.sedem.common.release.nanny.release_spec import (
    DockerImage,
    NannyReleaseSpec,
)
from maps.infra.sedem.common.release.sandbox.release_spec import (
    SandboxReleaseSpec,
)
from maps.infra.sedem.machine.tests.typing import (
    CommitFactory,
    MongoFixture,
    ReleaseCandidateFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.candidate_api import (
    ReleaseCandidateApi,
    ReleaseCandidateStatus,
)
from maps.infra.sedem.machine.lib.collections import RELEASE_TYPES
from maps.infra.sedem.machine.lib.config_api import ServiceConfig


async def init_lookup_data(service_config: ServiceConfig,
                           release_candidate_factory: ReleaseCandidateFactory) -> int:
    total = 6
    for i in range(1, total + 1):
        arc_hash = f'hash{i}'
        if i % 2 == 0:
            revision = 100 + i
        else:
            revision = None

        if i <= 4:
            status = ReleaseCandidateStatus.READY
        elif i == 5:
            status = ReleaseCandidateStatus.BROKEN
        else:
            status = ReleaseCandidateStatus.BUILDING

        if status == ReleaseCandidateStatus.BUILDING:
            release_spec = None
        else:
            if service_config.release_type == 'NANNY':
                release_spec = NannyReleaseSpec(
                    docker=DockerImage(name='maps/core-teapot', tag=revision or arc_hash),
                    environments=[],
                )
            elif service_config.release_type == 'GARDEN':
                release_spec = GardenReleaseSpec(
                    module=GardenModule(name='ymapsdf', version=revision or arc_hash),
                )
            elif service_config.release_type == 'SANDBOX':
                release_spec = SandboxReleaseSpec(
                    task_type='MAPS_EXAMPLE_TASK',
                    resource_id=str(10000 + i),
                    deploy_units=[],
                )
            else:
                assert False, f'Unknown release type {service_config.release_type}'

        await release_candidate_factory(
            service_config=service_config,
            task_id=str(100 + i),
            arc_hash=arc_hash,
            revision=revision,
            release_spec=release_spec,
            status=status,
        )
    return total


@pytest.mark.asyncio
@pytest.mark.parametrize('release_type', RELEASE_TYPES)
async def test_candidate_lookup(mongo: MongoFixture,
                                commit_factory: CommitFactory,
                                service_config_factory: ServiceConfigFactory,
                                release_candidate_factory: ReleaseCandidateFactory,
                                release_type: str) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        release_type=release_type,
    )
    await init_lookup_data(service_config, release_candidate_factory)

    async with await mongo.async_client.start_session() as session:
        candidate = await ReleaseCandidateApi(session).lookup_candidate(
            service_config=service_config,
            arc_commit_hash='hash2',
        )

    assert candidate.service_name == service_config.name
    assert candidate.task_id == '102'
    assert candidate.commit.arc_commit_hash == 'hash2'


@pytest.mark.asyncio
@pytest.mark.parametrize('release_type', RELEASE_TYPES)
async def test_list_trunk_candidates_all(mongo: MongoFixture,
                                         commit_factory: CommitFactory,
                                         service_config_factory: ServiceConfigFactory,
                                         release_candidate_factory: ReleaseCandidateFactory,
                                         release_type: str) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        release_type=release_type,
    )
    total = await init_lookup_data(service_config, release_candidate_factory)

    async with await mongo.async_client.start_session() as session:
        candidates = await ReleaseCandidateApi(session).list_trunk_candidates(
            service_config=service_config,
            lowest_svn_revision=1,
        )

    assert len(candidates) == total // 2
    for i, candidate in enumerate(islice(candidates, 1, None), start=1):
        prev_candidate = candidates[i-1]
        assert prev_candidate.commit.svn_revision >= candidate.commit.svn_revision


@pytest.mark.asyncio
async def test_list_trunk_candidates_filtered(mongo: MongoFixture,
                                              commit_factory: CommitFactory,
                                              service_config_factory: ServiceConfigFactory,
                                              release_candidate_factory: ReleaseCandidateFactory) -> None:
    service_config = await service_config_factory(name='maps-fake-service')
    await init_lookup_data(service_config, release_candidate_factory)

    async with await mongo.async_client.start_session() as session:
        candidates = await ReleaseCandidateApi(session).list_trunk_candidates(
            service_config=service_config,
            lowest_svn_revision=103,
        )

    assert len(candidates)
    for candidate in candidates:
        assert candidate.commit.svn_revision >= 103
