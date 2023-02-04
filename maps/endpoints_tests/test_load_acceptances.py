import asyncio
import typing as tp
from dataclasses import dataclass
import pytest

from google.protobuf.timestamp_pb2 import Timestamp

from maps.infra.sedem.client.machine_api import MachineApi
from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceTestSetStatus,
    AcceptanceTestStatus,
    AcceptanceTestLaunchDocument,
)
from maps.infra.sedem.machine.lib.deploy_api import DeployStatusDocument
from maps.infra.sedem.proto.sedem_pb2 import Release
from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    ReleaseFactory,
    ServiceConfigFactory,
)


@dataclass
class LoadAcceptancesTestCase:
    name: str
    acceptances: list[dict[str, tp.Any]]
    expected_result: dict[str, Release.AcceptanceStatus]

    def __str__(self):
        return self.name


CASES = [
    LoadAcceptancesTestCase(
        name='1_test',
        acceptances=[{
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='1',
                scheduler_id='1',
                status=AcceptanceTestStatus.SUCCESS,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }],
        expected_result={'testing': Release.AcceptanceStatus.SUCCESS}
    ),
    LoadAcceptancesTestCase(
        name='2_stages',
        acceptances=[{
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='1',
                scheduler_id='1',
                status=AcceptanceTestStatus.SUCCESS,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }, {
            'stage': 'prestable',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='2',
                scheduler_id='2',
                status=AcceptanceTestStatus.SUCCESS,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }],
        expected_result={
            'testing': Release.AcceptanceStatus.SUCCESS,
            'prestable': Release.AcceptanceStatus.SUCCESS,
        }
    ),
    LoadAcceptancesTestCase(
        name='2_acceptances_success',
        acceptances=[{
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='1',
                scheduler_id='1',
                status=AcceptanceTestStatus.SUCCESS,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }, {
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='2',
                scheduler_id='1',
                status=AcceptanceTestStatus.FAILURE,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }],
        expected_result={'testing': Release.AcceptanceStatus.FAILURE}
    ),
    LoadAcceptancesTestCase(
        name='2_acceptances_failure',
        acceptances=[{
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='1',
                scheduler_id='1',
                status=AcceptanceTestStatus.FAILURE,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }, {
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='2',
                scheduler_id='1',
                status=AcceptanceTestStatus.SUCCESS,
            )],
            'status': AcceptanceTestSetStatus.FINISHED,
        }],
        expected_result={'testing': Release.AcceptanceStatus.SUCCESS}
    ),
    LoadAcceptancesTestCase(
        name='executing',
        acceptances=[{
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='2',
                scheduler_id='1',
                status=AcceptanceTestStatus.EXECUTING,
            )],
            'status': AcceptanceTestSetStatus.EXECUTING,
        }],
        expected_result={'testing': Release.AcceptanceStatus.EXECUTING}
    ),
    LoadAcceptancesTestCase(
        name='pending',
        acceptances=[{
            'stage': 'testing',
            'tasks': [AcceptanceTestLaunchDocument(
                task_id='2',
                scheduler_id='1',
                status=AcceptanceTestStatus.PENDING
            )],
            'status': AcceptanceTestSetStatus.PENDING,
        }],
        expected_result={'testing': Release.AcceptanceStatus.PENDING}
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_load_acceptances(machine_api: MachineApi,
                                service_config_factory: ServiceConfigFactory,
                                deploy_factory: DeployFactory,
                                acceptance_factory: AcceptanceFactory,
                                release_factory: ReleaseFactory,
                                case: LoadAcceptancesTestCase):
    service_config = await service_config_factory(name='maps-fake-service')

    release_id = await release_factory(
        service_config=service_config,
        major_version='1',
        minor_version='1',
    )
    for acceptance in case.acceptances:
        deploy_id = await deploy_factory(
            service_config=service_config,
            status=DeployStatusDocument(success=DeployStatusDocument.Success())
        )
        await acceptance_factory(
            release_id=release_id,
            deploys=[deploy_id],
            **acceptance,
        )

    resp = await asyncio.to_thread(
        machine_api.release_lookup,
        service_name='maps-fake-service',
        version='v1.1',
    )

    assert len(resp.releases) == 1
    release = resp.releases[0]

    acceptance_statuses = {}
    for acceptance_info in release.acceptances:
        acceptance_statuses[acceptance_info.stage] = acceptance_info.status
        if acceptance_info.status in (Release.AcceptanceStatus.SUCCESS, Release.AcceptanceStatus.FAILURE,
                                      Release.AcceptanceStatus.CANCELLED):
            assert acceptance_info.end_time != Timestamp()
        if acceptance_info.status != Release.AcceptanceStatus.PENDING:
            assert acceptance_info.start_time != Timestamp()

    assert acceptance_statuses == case.expected_result
