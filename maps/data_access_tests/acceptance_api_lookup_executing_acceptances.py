import pytest
from dataclasses import dataclass

from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestStatus,
    AcceptanceTestLaunchDocument
)
from maps.infra.sedem.machine.lib.deploy_api import DeployStatusDocument


@dataclass
class LookupExecutingTestCase:
    name: str
    tasks: list[AcceptanceTestLaunchDocument]

    def __str__(self):
        return self.name


CASES = [
    LookupExecutingTestCase(
        name='nothing_executing',
        tasks=[]
    ),
    LookupExecutingTestCase(
        name='one_executing',
        tasks=[AcceptanceTestLaunchDocument(task_id='1', scheduler_id='1', status=AcceptanceTestStatus.EXECUTING)]
    ),
    LookupExecutingTestCase(
        name='one_executing_one_done',
        tasks=[
            AcceptanceTestLaunchDocument(task_id='1', scheduler_id='1', status=AcceptanceTestStatus.EXECUTING),
            AcceptanceTestLaunchDocument(task_id='2', scheduler_id='2', status=AcceptanceTestStatus.SUCCESS)
        ]
    ),
    LookupExecutingTestCase(
        name='all_done',
        tasks=[
            AcceptanceTestLaunchDocument(task_id='1', scheduler_id='1', status=AcceptanceTestStatus.SUCCESS),
            AcceptanceTestLaunchDocument(task_id='2', scheduler_id='2', status=AcceptanceTestStatus.FAILURE)
        ]
    )
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_lookup_executing(mongo: MongoFixture,
                                release_factory: ReleaseFactory,
                                deploy_factory: DeployFactory,
                                service_config_factory: ServiceConfigFactory,
                                acceptance_factory: AcceptanceFactory,
                                case: LookupExecutingTestCase):
    service_config = await service_config_factory(name='maps-fake-service', sox=False)
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

    await acceptance_factory(
        release_id=release_id,
        deploys=deploys,
        tasks=case.tasks,
        status=AcceptanceTestSetStatus.EXECUTING,
    )
    found_tasks = set()
    async with await mongo.async_client.start_session() as session:
        async for task_id in AcceptanceApi(session).lookup_executing_acceptances():
            found_tasks.add(task_id)

    assert found_tasks == {
        task.task_id for task in case.tasks
        if task.status == AcceptanceTestStatus.EXECUTING
    }
