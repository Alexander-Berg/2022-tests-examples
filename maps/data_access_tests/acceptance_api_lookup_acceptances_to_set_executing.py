from dataclasses import dataclass

import pytest

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
class LookupAcceptancesToSetExecutingCase:
    name: str
    acceptances: list[dict[str, AcceptanceTestStatus]]

    def __str__(self):
        return self.name


CASES = [
    LookupAcceptancesToSetExecutingCase(
        name='nothing_started',
        acceptances=[
            {'1': AcceptanceTestStatus.PENDING}
        ]
    ),
    LookupAcceptancesToSetExecutingCase(
        name='one_started',
        acceptances=[
            {'1': AcceptanceTestStatus.PENDING, '2': AcceptanceTestStatus.EXECUTING}
        ]
    ),
    LookupAcceptancesToSetExecutingCase(
        name='all_started',
        acceptances=[
            {'1': AcceptanceTestStatus.EXECUTING, '2': AcceptanceTestStatus.EXECUTING}
        ]
    ),
    LookupAcceptancesToSetExecutingCase(
        name='two_started_acceptances',
        acceptances=[
            {'1': AcceptanceTestStatus.EXECUTING},
            {'2': AcceptanceTestStatus.EXECUTING}
        ]
    ),
    LookupAcceptancesToSetExecutingCase(
        name='1_started_1_pending_acceptance',
        acceptances=[
            {'1': AcceptanceTestStatus.EXECUTING},
            {'2': AcceptanceTestStatus.PENDING}
        ]
    )
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_lookup_acceptances_to_set_executing(
        mongo: MongoFixture,
        release_factory: ReleaseFactory,
        deploy_factory: DeployFactory,
        service_config_factory: ServiceConfigFactory,
        acceptance_factory: AcceptanceFactory,
        case: LookupAcceptancesToSetExecutingCase
):
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
    must_be_found = set()
    for acceptance_tasks in case.acceptances:
        acceptance_id = await acceptance_factory(
            release_id=release_id,
            deploys=deploys,
            tasks=[
                AcceptanceTestLaunchDocument(task_id=task_id, scheduler_id=task_id, status=status)
                for task_id, status in acceptance_tasks.items()
            ],
            status=AcceptanceTestSetStatus.PENDING,
        )
        if all(
            status in (
                AcceptanceTestStatus.FAILURE,
                AcceptanceTestStatus.SUCCESS,
                AcceptanceTestStatus.EXECUTING
            )
            for status in acceptance_tasks.values()
        ):
            must_be_found.add(acceptance_id)
    found_acceptances = set()
    async with await mongo.async_client.start_session() as session:
        async for st_ticket, acceptance in AcceptanceApi(session).lookup_acceptances_to_set_executing():
            found_acceptances.add(str(acceptance.acceptance_id))
            assert acceptance.start_time is None
            assert acceptance.end_time is None

    assert must_be_found == found_acceptances
