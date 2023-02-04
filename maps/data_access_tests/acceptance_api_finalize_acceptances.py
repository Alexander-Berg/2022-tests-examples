from dataclasses import dataclass

import pytest
from bson import ObjectId

from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi,
    AcceptanceTestLaunchDocument,
    AcceptanceTestSetLaunchDocument,
    AcceptanceTestSetStatus,
    AcceptanceTestStatus,
)
from maps.infra.sedem.machine.lib.deploy_api import DeployStatusDocument


@dataclass
class AcceptanceTestSet:
    status: AcceptanceTestSetStatus
    launches: list[AcceptanceTestLaunchDocument]


@dataclass
class FinalizeAcceptancesTestCase:
    name: str
    target_status: AcceptanceTestSetStatus
    acceptances: list[AcceptanceTestSet]

    def __str__(self):
        return self.name


CASES = [
    FinalizeAcceptancesTestCase(
        name='finishing_test_set_with_single_successful_test',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.SUCCESS,
                    ),
                ],
            ),
        ],
    ),
    FinalizeAcceptancesTestCase(
        name='finishing_test_set_with_single_executing_test',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.EXECUTING,
                    ),
                ],
            ),
        ]
    ),
    FinalizeAcceptancesTestCase(
        name='finishing_test_set_with_successful_and_failed_tests',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.SUCCESS,
                    ),
                    AcceptanceTestLaunchDocument(
                        task_id='2',
                        template_name='TEMPLATE',
                        status=AcceptanceTestStatus.FAILURE,
                    ),
                ],
            ),
        ]
    ),
    FinalizeAcceptancesTestCase(
        name='finishing_test_set_with_successful_and_executing_tests',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.EXECUTING,
                    ),
                    AcceptanceTestLaunchDocument(
                        task_id='2',
                        template_name='TEMPLATE',
                        status=AcceptanceTestStatus.SUCCESS,
                    ),
                ],
            ),
        ]
    ),
    FinalizeAcceptancesTestCase(
        name='finishing_test_set_with_successful_and_pending_tests',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.PENDING,
                    ),
                    AcceptanceTestLaunchDocument(
                        task_id='2',
                        template_name='TEMPLATE',
                        status=AcceptanceTestStatus.SUCCESS,
                    ),
                ],
            ),
        ]
    ),
    FinalizeAcceptancesTestCase(
        name='finishing_two_test_sets_with_executing_test_each',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.EXECUTING,
                    ),
                ],
            ),
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='2',
                        scheduler_id='2',
                        status=AcceptanceTestStatus.EXECUTING,
                    ),
                ],
            ),
        ]
    ),
    FinalizeAcceptancesTestCase(
        name='finishing_two_test_sets_with_executing_and_successful_test_accordingly',
        target_status=AcceptanceTestSetStatus.FINISHED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.EXECUTING,
                    ),
                ],
            ),
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.EXECUTING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='2',
                        scheduler_id='2',
                        status=AcceptanceTestStatus.SUCCESS,
                    ),
                ],
            ),
        ]
    ),
    FinalizeAcceptancesTestCase(
        name='cancelling_two_test_sets_with_pending_test_each',
        target_status=AcceptanceTestSetStatus.CANCELLED,
        acceptances=[
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.TO_CANCEL,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='1',
                        scheduler_id='1',
                        status=AcceptanceTestStatus.PENDING,
                    ),
                ],
            ),
            AcceptanceTestSet(
                status=AcceptanceTestSetStatus.PENDING,
                launches=[
                    AcceptanceTestLaunchDocument(
                        task_id='2',
                        scheduler_id='2',
                        status=AcceptanceTestStatus.PENDING,
                    ),
                ],
            ),
        ]
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_finalize_acceptances(mongo: MongoFixture,
                                    release_factory: ReleaseFactory,
                                    deploy_factory: DeployFactory,
                                    service_config_factory: ServiceConfigFactory,
                                    acceptance_factory: AcceptanceFactory,
                                    case: FinalizeAcceptancesTestCase):
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
    for acceptance_test_set in case.acceptances:
        await acceptance_factory(
            release_id=release_id,
            deploys=deploys,
            tasks=acceptance_test_set.launches,
            status=acceptance_test_set.status,
        )

    target_status = case.target_status
    if target_status == AcceptanceTestSetStatus.FINISHED:
        current_status = AcceptanceTestSetStatus.EXECUTING
    elif target_status == AcceptanceTestSetStatus.CANCELLED:
        current_status = AcceptanceTestSetStatus.TO_CANCEL
    else:
        assert False, f'Bad target status: {target_status}'

    async with await mongo.async_client.start_session() as session:
        await AcceptanceApi(session).finalize_acceptances(
            current_status=current_status,
            target_status=target_status,
        )

    release_document = await mongo.async_client.get_database().release.find_one(
        {'_id': ObjectId(release_id)},
        {'acceptance': 1}
    )

    for initial_state, updated_state in zip(case.acceptances, release_document['acceptance']):
        updated_state = AcceptanceTestSetLaunchDocument.build_from_mongo(updated_state)
        if initial_state.status == current_status and all(
            launch.status in (
                AcceptanceTestStatus.FAILURE,
                AcceptanceTestStatus.SUCCESS,
                AcceptanceTestStatus.PENDING,
            )
            for launch in updated_state.launches
        ):
            required_status = target_status
        else:
            required_status = initial_state.status

        assert updated_state.status == required_status
        if required_status == target_status:
            assert updated_state.start_time is not None
            assert updated_state.end_time is not None
