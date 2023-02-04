import typing as tp
from contextlib import nullcontext
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.tests.typing import (
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.deploy_api import DeployApi
from maps.infra.sedem.machine.lib.release_api import ReleaseCompletion


async def init_releases(service_config: ServiceConfig,
                        release_factory: ReleaseFactory) -> None:
    for minor_version in range(1, 4):
        completion = {
            1: ReleaseCompletion(ready=ReleaseCompletion.Ready()),
            2: ReleaseCompletion(broken=ReleaseCompletion.Broken(
                build=ReleaseCompletion.Broken.BrokenBuild(reason='some reason')
            )),
            3: ReleaseCompletion(preparing=ReleaseCompletion.Preparing(
                build=ReleaseCompletion.Preparing.PreparingBuild(operation_id='12345')
            )),
        }[minor_version]
        await release_factory(
            service_config=service_config,
            major_version=1,
            minor_version=minor_version,
            st_ticket='TESTQUEUE-1',
            origin_arc_hash=f'hash1.{minor_version}.1',
            origin_revision=1 * 100 + minor_version * 10 + 1,
            release_arc_hash=f'hash1.{minor_version}.{1 if minor_version == 1 else 2}',
            release_revision=100 + minor_version * 10 + (1 if minor_version == 1 else 2),
            completion=completion,
        )


@dataclass
class PrepareDeployTest:
    name: str

    release_major: int
    release_minor: int
    stage: str

    expected_error: tp.Optional[str]

    def __str__(self) -> str:
        return self.name


PREPARE_DEPLOY_TEST_CASES = [
    PrepareDeployTest(
        name='success',
        release_major=1,
        release_minor=1,
        stage='testing',
        expected_error=None,
    ),
    PrepareDeployTest(
        name='non-existing_stage',
        release_major=1,
        release_minor=1,
        stage='unstable',
        expected_error=r'No stage "unstable" for [^\s]+ found',
    ),
    PrepareDeployTest(
        name='broken_release',
        release_major=1,
        release_minor=2,
        stage='testing',
        expected_error=r'No ready release v1.2 for [^\s]+ found',
    ),
    PrepareDeployTest(
        name='preparing_release',
        release_major=1,
        release_minor=3,
        stage='testing',
        expected_error=None,  # FIXME: Should raise an error as preparing release should not be deployed
    ),
    PrepareDeployTest(
        name='non-existing_release',
        release_major=1,
        release_minor=4,
        stage='testing',
        expected_error=r'No ready release v1.4 for [^\s]+ found',
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', PREPARE_DEPLOY_TEST_CASES, ids=str)
async def test_prepare_deploy(mongo: MongoFixture,
                              release_factory: ReleaseFactory,
                              service_config_factory: ServiceConfigFactory,
                              case: PrepareDeployTest) -> None:
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

    await init_releases(
        service_config=service_config,
        release_factory=release_factory,
    )

    if case.expected_error is not None:
        check_expected_error = pytest.raises(Exception, match=case.expected_error)
    else:
        check_expected_error = nullcontext()

    with check_expected_error:
        async with await mongo.async_client.start_session() as session:
            deploys = await DeployApi(session).prepare_deploys(
                service_config=service_config,
                major_version=case.release_major,
                minor_version=case.release_minor,
                author='john-doe',
                stage=case.stage,
            )

    if case.expected_error is not None:
        return

    expected_deploy_units = next(
        (stage.deploy_units
         for stage in service_config.stages
         if stage.name == case.stage),
        None
    )
    actual_deploy_units = [deploy.deploy_unit for deploy in deploys]
    assert expected_deploy_units == actual_deploy_units
