import asyncio
from dataclasses import dataclass

import pytest
from bson import ObjectId

from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestStatus,
    AcceptanceTestLaunchDocument
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
class SetTaskLaunchedTestCase:
    name: str
    tasks: dict[str, AcceptanceTestStatus]
    update: dict[str, AcceptanceTestStatus]

    def __str__(self):
        return self.name


CASES = [
    SetTaskLaunchedTestCase(
        name='simple_test',
        tasks={'1': AcceptanceTestStatus.PENDING},
        update={'1': AcceptanceTestStatus.EXECUTING}
    ),
    SetTaskLaunchedTestCase(
        name='concurrent_start',
        tasks={'1': AcceptanceTestStatus.PENDING, '2': AcceptanceTestStatus.PENDING},
        update={'1': AcceptanceTestStatus.EXECUTING, '2': AcceptanceTestStatus.FAILURE}
    ),
    SetTaskLaunchedTestCase(
        name='one_update',
        tasks={'1': AcceptanceTestStatus.PENDING, '2': AcceptanceTestStatus.PENDING},
        update={'1': AcceptanceTestStatus.EXECUTING}
    )
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_set_task_launched(mongo: MongoFixture,
                                 release_factory: ReleaseFactory,
                                 deploy_factory: DeployFactory,
                                 service_config_factory: ServiceConfigFactory,
                                 acceptance_factory: AcceptanceFactory,
                                 case: SetTaskLaunchedTestCase):
    assert not (set(case.update.keys()) - set(case.tasks.keys()))
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
        tasks=[
            AcceptanceTestLaunchDocument(task_id=task, scheduler_id=task, status=status)
            for task, status in case.tasks.items()
        ],
        status=AcceptanceTestSetStatus.EXECUTING,
    )

    async def update_task_launch_info(task_id: str, status: AcceptanceTestStatus) -> None:
        async with await mongo.async_client.start_session() as session:
            await AcceptanceApi(session).update_task_launch_info(
                acceptance_id=acceptance_id,
                test_launch=AcceptanceTestLaunchDocument(
                    task_id=task_id,
                    scheduler_id=task_id,
                    status=status
                )
            )

    results = await asyncio.gather(*[
        update_task_launch_info(task_id, status) for task_id, status in case.update.items()
    ], return_exceptions=True)
    for maybe_exception in results:
        if maybe_exception is None:
            continue
        raise maybe_exception
    expected_task_statuses = case.tasks.copy()
    expected_task_statuses.update(case.update)
    collection = mongo.async_client.get_database().release
    release_document = await collection.find_one(
        {'_id': ObjectId(release_id), 'acceptance.acceptance_id': ObjectId(acceptance_id)},
        {'acceptance.$'}
    )
    assert len(release_document['acceptance']) == 1
    acceptance_document = release_document['acceptance'][0]
    found_tasks = set()
    for task in acceptance_document['launches']:
        task_id = task['task_id']
        task_status = AcceptanceTestStatus(task['status'])
        assert task_status == expected_task_statuses[task_id]
        found_tasks.add(task_id)

    assert found_tasks == set(case.tasks.keys())
