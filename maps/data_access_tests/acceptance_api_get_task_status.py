from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestStatus, AcceptanceTestLaunchDocument
)
from maps.infra.sedem.machine.lib.collections import DeployStatusDocument
from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)


@dataclass
class GetTaskStatusTestCase:
    name: str
    tasks: list[AcceptanceTestLaunchDocument]

    def __str__(self):
        return self.name


CASES = [
    GetTaskStatusTestCase(
        name='one_task',
        tasks=[
            AcceptanceTestLaunchDocument(task_id='1', scheduler_id='1', status=AcceptanceTestStatus.FAILURE)
        ]
    ),
    GetTaskStatusTestCase(
        name='two_tasks',
        tasks=[
            AcceptanceTestLaunchDocument(scheduler_id='1', status=AcceptanceTestStatus.PENDING),
            AcceptanceTestLaunchDocument(task_id='2', scheduler_id='2', status=AcceptanceTestStatus.EXECUTING)
        ]
    ),
    GetTaskStatusTestCase(
        name='template',
        tasks=[
            AcceptanceTestLaunchDocument(template_name='TEMPLATE', status=AcceptanceTestStatus.PENDING),
        ]
    ),
    GetTaskStatusTestCase(
        name='template_and_scheduler',
        tasks=[
            AcceptanceTestLaunchDocument(scheduler_id='1', status=AcceptanceTestStatus.PENDING),
            AcceptanceTestLaunchDocument(task_id='2', template_name='TEMPLATE', status=AcceptanceTestStatus.EXECUTING)
        ]
    )

]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_get_task_status(mongo: MongoFixture,
                               release_factory: ReleaseFactory,
                               deploy_factory: DeployFactory,
                               service_config_factory: ServiceConfigFactory,
                               acceptance_factory: AcceptanceFactory,
                               case: GetTaskStatusTestCase):
    service_config = await service_config_factory(name='maps-fake-service')

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )
    deploys = [
        await deploy_factory(
            service_config=service_config,
            status=DeployStatusDocument(success=DeployStatusDocument.Success())
        ),
    ]
    acceptance_id = await acceptance_factory(
        release_id=release_id,
        deploys=deploys,
        tasks=case.tasks,
        status=AcceptanceTestSetStatus.EXECUTING,
    )

    async with await mongo.async_client.start_session() as session:
        for task in case.tasks:
            found_status = await AcceptanceApi(session).get_task_status(
                acceptance_id=acceptance_id,
                test_launch=task
            )
            assert task.status == found_status
