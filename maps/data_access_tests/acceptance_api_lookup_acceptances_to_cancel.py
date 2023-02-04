import pytest
from dataclasses import dataclass

from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestStatus, AcceptanceTestLaunchDocument
)
from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)


@dataclass
class LookupAcceptancesToCancelCase:
    name: str
    acceptances: list[list[dict]]

    def __str__(self):
        return self.name


CASES = [
    LookupAcceptancesToCancelCase(
        name='1_acceptance',
        acceptances=[[
            {
                'status': AcceptanceTestSetStatus.TO_CANCEL,
                'tests': {'1': AcceptanceTestStatus.EXECUTING}
            }
        ]]
    ),
    LookupAcceptancesToCancelCase(
        name='executing_and_success_acceptances',
        acceptances=[[
            {
                'status': AcceptanceTestSetStatus.TO_CANCEL,
                'tests': {
                    '1': AcceptanceTestStatus.EXECUTING,
                    '2': AcceptanceTestStatus.SUCCESS
                }
            }
        ]]
    ),
    LookupAcceptancesToCancelCase(
        name='executing_and_pending_acceptances',
        acceptances=[[
            {
                'status': AcceptanceTestSetStatus.TO_CANCEL,
                'tests': {
                    '1': AcceptanceTestStatus.EXECUTING,
                    '2': AcceptanceTestStatus.PENDING
                }
            }
        ]]
    ),
    LookupAcceptancesToCancelCase(
        name='2_releases',
        acceptances=[
            [
                {
                    'status': AcceptanceTestSetStatus.TO_CANCEL,
                    'tests': {'1': AcceptanceTestStatus.EXECUTING}
                }
            ],
            [
                {
                    'status': AcceptanceTestSetStatus.TO_CANCEL,
                    'tests': {'2': AcceptanceTestStatus.EXECUTING}
                }
            ],

        ]
    ),
    LookupAcceptancesToCancelCase(
        name='2_acceptances',
        acceptances=[
            [
                {
                    'status': AcceptanceTestSetStatus.TO_CANCEL,
                    'tests': {'1': AcceptanceTestStatus.EXECUTING}
                },
                {
                    'status': AcceptanceTestSetStatus.TO_CANCEL,
                    'tests': {'2': AcceptanceTestStatus.EXECUTING}
                }
            ],
        ]
    ),

]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_lookup_acceptances_to_cancel(mongo: MongoFixture,
                                            release_factory: ReleaseFactory,
                                            deploy_factory: DeployFactory,
                                            service_config_factory: ServiceConfigFactory,
                                            acceptance_factory: AcceptanceFactory,
                                            case: LookupAcceptancesToCancelCase):
    service_config = await service_config_factory(name='maps-fake-service')
    must_be_found = set()
    for i, release_acceptances in enumerate(case.acceptances):
        release_id = await release_factory(
            service_config=service_config,
            major_version=i + 1,
            minor_version=1,
            origin_arc_hash=f'fake-hash-{i}'
        )
        for acceptance in release_acceptances:
            acceptance_status = acceptance['status']
            tests = []
            for task_id, task_status in acceptance['tests'].items():
                tests.append(AcceptanceTestLaunchDocument(task_id=task_id, status=task_status))
                if acceptance_status == AcceptanceTestSetStatus.TO_CANCEL and task_status == AcceptanceTestStatus.EXECUTING:
                    must_be_found.add(task_id)
            await acceptance_factory(
                release_id,
                deploys=[],
                tasks=tests,
                status=acceptance_status,
            )

    async with await mongo.async_client.start_session() as session:
        found_tasks = set(await AcceptanceApi(session).lookup_acceptances_to_cancel())
    assert found_tasks == must_be_found
