import asyncio
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.tests.typing import (
    MongoFixture,
    DeployFactory,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.deploy_api import (
    DeployApi,
    DeployCommitRequest,
    DeployStatus,
    DeployStatusDocument,
)
from maps.infra.sedem.machine.lib.release_api import ReleaseApi


@pytest.mark.asyncio
async def test_commit_deploy(mongo: MongoFixture,
                             service_config_factory: ServiceConfigFactory,
                             release_factory: ReleaseFactory,
                             deploy_factory: DeployFactory) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        stages=[
            ServiceConfig.Stage(
                name=name,
                deploy_units=list(deploy_units),
            )
            for name, deploy_units in (
                ('testing', ('testing', 'load')),
                ('prestable', ('prestable')),
                ('stable', ('stable')),
            )
        ],
    )

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    deploy_ids = []
    for deploy_unit in ('testing', 'load'):
        # Previous deploys is still waiting to complete
        await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                executing=DeployStatusDocument.Executing(),
            ),
        )
        # Prepare actual deploys
        deploy_ids.append(await deploy_factory(
            service_config=service_config,
            release_major_version=1,
            release_minor_version=1,
            author='john-doe',
            deploy_unit=deploy_unit,
            status=DeployStatusDocument(
                pending=DeployStatusDocument.Pending(),
            ),
        ))
    testing_deploy_id, load_deploy_id = deploy_ids

    async with await mongo.async_client.start_session() as session:
        await DeployApi(session).commit_deploys(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            deploy_requests=[
                DeployCommitRequest(
                    deploy_id=testing_deploy_id,
                    status=DeployCommitRequest.Status.SUCCESS,
                ),
                DeployCommitRequest(
                    deploy_id=load_deploy_id,
                    status=DeployCommitRequest.Status.FAILURE,
                    reason='Permission denied',
                )
            ],
        )
        release = await ReleaseApi(session).load_release(release_id)

    testing_deploy = next(
        (deploy for deploy in release.deploys
         if str(deploy.deploy_id) == testing_deploy_id),
        None
    )
    load_deploy = next(
        (deploy for deploy in release.deploys
         if str(deploy.deploy_id) == load_deploy_id),
        None
    )
    previous_deploys = [
        deploy for deploy in release.deploys
        if str(deploy.deploy_id) not in {testing_deploy_id, load_deploy_id}
    ]
    # Load wasn't deployed at all
    assert load_deploy.deploy_status.status() == DeployStatus.FAILURE
    assert load_deploy.deploy_status.failure.reason == 'Permission denied'
    assert load_deploy.start_time is not None
    assert load_deploy.end_time is not None
    # Testing is now waiting for actual deploy to finish
    assert testing_deploy.deploy_status.status() == DeployStatus.EXECUTING
    assert testing_deploy.start_time is not None
    assert testing_deploy.end_time is None
    # Previous deploys are cancelled
    for deploy in previous_deploys:
        assert deploy.deploy_status.status() == DeployStatus.CANCELLED
        assert deploy.start_time is not None
        assert deploy.end_time is not None


@dataclass
class CommitDeployFailsTest:
    name: str

    release_major: int
    deploy_id: str

    expected_error: str

    def __str__(self) -> str:
        return self.name


COMMIT_DEPLOY_FAILS_TEST_CASES = [
    CommitDeployFailsTest(
        name='non_existant_release',
        release_major=2,
        deploy_id=b'fake--deploy',
        expected_error=r'Release v2.1 with requested deployment ids not found',
    ),
    CommitDeployFailsTest(
        name='non_existant_deploy',
        release_major=1,
        deploy_id=b'fake--deploy',
        expected_error=r'Release v1.1 with requested deployment ids not found',
    ),
    CommitDeployFailsTest(
        name='bad_deploy_id',
        release_major=1,
        deploy_id=b'too-long-deploy-id',
        expected_error=r'Invalid deploy id passed',
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', COMMIT_DEPLOY_FAILS_TEST_CASES, ids=str)
async def test_commit_deploy_fails(mongo: MongoFixture,
                                   service_config_factory: ServiceConfigFactory,
                                   release_factory: ReleaseFactory,
                                   case: CommitDeployFailsTest) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        stages=[
            ServiceConfig.Stage(
                name=name,
                deploy_units=list(deploy_units),
            )
            for name, deploy_units in (
                ('testing', ('testing', 'load')),
                ('prestable', ('prestable')),
                ('stable', ('stable')),
            )
        ],
    )

    await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )

    with pytest.raises(Exception, match=case.expected_error):
        async with await mongo.async_client.start_session() as session:
            await DeployApi(session).commit_deploys(
                service_config=service_config,
                major_version=case.release_major,
                minor_version=1,
                deploy_requests=[
                    DeployCommitRequest(
                        deploy_id=case.deploy_id,
                        status=DeployCommitRequest.Status.SUCCESS,
                    ),
                ],
            )


@pytest.mark.asyncio
async def test_concurrent_commit_deploy(mongo: MongoFixture,
                                        service_config_factory: ServiceConfigFactory,
                                        release_factory: ReleaseFactory,
                                        deploy_factory: DeployFactory) -> None:
    service_config = await service_config_factory(
        name='maps-fake-service',
        stages=[
            ServiceConfig.Stage(
                name=name,
                deploy_units=list(deploy_units),
            )
            for name, deploy_units in (
                ('testing', ('testing', 'load')),
                ('prestable', ('prestable')),
                ('stable', ('stable')),
            )
        ],
    )

    release_ids = []
    deploy_requests = []
    for major_version in (1, 2):
        release_ids.append(await release_factory(
            service_config=service_config,
            major_version=major_version,
            minor_version=1,
            st_ticket=f'TESTQUEUE-{major_version}',
            origin_arc_hash=f'hash{major_version}',
        ))

        release_deploy_requests = []
        for deploy_unit in ('testing', 'load'):
            deploy_id = await deploy_factory(
                service_config=service_config,
                release_major_version=major_version,
                release_minor_version=1,
                author='john-doe',
                deploy_unit=deploy_unit,
                status=DeployStatusDocument(
                    pending=DeployStatusDocument.Pending(),
                ),
            )
            release_deploy_requests.append(
                DeployCommitRequest(
                    deploy_id=deploy_id,
                    status=DeployCommitRequest.Status.SUCCESS,
                )
            )
        deploy_requests.append(release_deploy_requests)
    release1_id, release2_id = release_ids
    release1_deploy_reqs, release2_deploy_reqs = deploy_requests

    async with await mongo.async_client.start_session() as session1:
        async with await mongo.async_client.start_session() as session2:
            await asyncio.gather(
                DeployApi(session1).commit_deploys(
                    service_config=service_config,
                    major_version=1,
                    minor_version=1,
                    deploy_requests=release1_deploy_reqs,
                ),
                DeployApi(session2).commit_deploys(
                    service_config=service_config,
                    major_version=2,
                    minor_version=1,
                    deploy_requests=release2_deploy_reqs,
                )
            )

    async with await mongo.async_client.start_session() as session:
        release1 = await ReleaseApi(session).load_release(release1_id)
        release2 = await ReleaseApi(session).load_release(release2_id)

    release1_deploy_statuses = {
        deploy.deploy_status.status()
        for deploy in release1.deploys
    }
    assert len(release1_deploy_statuses) == 1
    release1_deploy_status = release1_deploy_statuses.pop()

    release2_deploy_statuses = {
        deploy.deploy_status.status()
        for deploy in release2.deploys
    }
    assert len(release2_deploy_statuses) == 1
    release2_deploy_status = release2_deploy_statuses.pop()

    assert release1_deploy_status in {DeployStatus.EXECUTING, DeployStatus.CANCELLED}
    assert release2_deploy_status in {DeployStatus.EXECUTING, DeployStatus.CANCELLED}
    # Concurrent deploys shouldn't cancel each other
    assert (
        release1_deploy_status != DeployStatus.CANCELLED
        or release2_deploy_status != DeployStatus.CANCELLED
    )
