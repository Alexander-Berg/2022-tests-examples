from collections import defaultdict
from dataclasses import dataclass

import pytest

from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestStatus, AcceptanceTestLaunchDocument
)
from maps.infra.sedem.machine.lib.config_api import ServiceConfig
from maps.infra.sedem.machine.lib.deploy_api import DeployStatusDocument
from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)


@dataclass
class LookupRequiredAcceptances:
    name: str
    acceptances: list[str]
    target_stage: str

    def __str__(self):
        return self.name


CASES = [
    LookupRequiredAcceptances(
        name='simple_test',
        acceptances=['testing'],
        target_stage='prestable'
    ),
    LookupRequiredAcceptances(
        name='2_acceptances',
        acceptances=['testing'] * 2,
        target_stage='prestable'
    ),
    LookupRequiredAcceptances(
        name='no_acceptances',
        acceptances=[],
        target_stage='prestable'
    ),
    LookupRequiredAcceptances(
        name='acceptances_for_other_stage',
        acceptances=['testing'],
        target_stage='stable'
    ),
    LookupRequiredAcceptances(
        name='no_acceptances',
        acceptances=[],
        target_stage='testing'
    )
]

PREVIOUS_STAGES = {
    'unstable': None,
    'testing': 'unstable',
    'prestable': 'testing',
    'stable': 'prestable'
}


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_lookup_acceptance_for_deploy(mongo: MongoFixture,
                                            release_factory: ReleaseFactory,
                                            deploy_factory: DeployFactory,
                                            service_config_factory: ServiceConfigFactory,
                                            acceptance_factory: AcceptanceFactory,
                                            case: LookupRequiredAcceptances):
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
    created_acceptances = defaultdict(list)
    for stage in case.acceptances:
        acceptance_id = await acceptance_factory(
            release_id=release_id,
            deploys=deploys,
            tasks=[AcceptanceTestLaunchDocument(task_id='1', scheduler_id='1', status=AcceptanceTestStatus.SUCCESS)],
            status=AcceptanceTestSetStatus.FINISHED,
            stage=stage
        )
        created_acceptances[stage].append(acceptance_id)

    async with await mongo.async_client.start_session() as session:
        maybe_acceptance = await AcceptanceApi(session).lookup_required_acceptance_for_deploy(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            target_stage=case.target_stage
        )

    previous_stage = PREVIOUS_STAGES[case.target_stage]
    if previous_stage is None:
        assert maybe_acceptance is None
    if maybe_acceptance is None:
        assert previous_stage not in created_acceptances.keys()
    else:
        assert str(created_acceptances[previous_stage][-1]) == str(maybe_acceptance.acceptance_id)


@pytest.mark.asyncio
async def test_lookup_acceptance_for_deploy_without_prestable(
        mongo: MongoFixture,
        release_factory: ReleaseFactory,
        service_config_factory: ServiceConfigFactory,
        acceptance_factory: AcceptanceFactory
):
    service_config = await service_config_factory(
        name='maps-fake-service',
        stages=[
            ServiceConfig.Stage(name='testing', deploy_units=['testing']),
            ServiceConfig.Stage(name='stable', deploy_units=['stable']),
        ]
    )

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )
    acceptance_id = await acceptance_factory(
        release_id=release_id,
        deploys=[],
        tasks=[AcceptanceTestLaunchDocument(task_id='1', scheduler_id='1', status=AcceptanceTestStatus.SUCCESS)],
        status=AcceptanceTestSetStatus.FINISHED,
        stage='testing'
    )
    async with await mongo.async_client.start_session() as session:
        maybe_acceptance = await AcceptanceApi(session).lookup_required_acceptance_for_deploy(
            service_config=service_config,
            major_version=1,
            minor_version=1,
            target_stage='stable'
        )
    assert maybe_acceptance
    acceptance = maybe_acceptance
    assert str(acceptance.acceptance_id) == acceptance_id
