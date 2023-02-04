import pytest
from dataclasses import dataclass

from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestSetStatus, AcceptanceTestSetLaunchDocument
)
from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)


@dataclass
class CancelNotFinishedAcceptancesCase:
    name: str
    acceptances: list[list[tuple[str, AcceptanceTestSetStatus]]]  # for each release list of acceptances(stage + status)
    cancel_stage: str

    def __str__(self):
        return self.name


CASES = [
    CancelNotFinishedAcceptancesCase(
        name='all_done',
        acceptances=[[('testing', AcceptanceTestSetStatus.FINISHED)]],
        cancel_stage='testing'
    ),
    CancelNotFinishedAcceptancesCase(
        name='one_executing',
        acceptances=[[('testing', AcceptanceTestSetStatus.EXECUTING)]],
        cancel_stage='testing'
    ),
    CancelNotFinishedAcceptancesCase(
        name='one_executing_one_finished_1_release',
        acceptances=[[('testing', AcceptanceTestSetStatus.EXECUTING), ('testing', AcceptanceTestSetStatus.FINISHED)]],
        cancel_stage='testing'
    ),
    CancelNotFinishedAcceptancesCase(
        name='one_executing_one_finished_2_releases',
        acceptances=[
            [('testing', AcceptanceTestSetStatus.EXECUTING)],
            [('testing', AcceptanceTestSetStatus.FINISHED)]
        ],
        cancel_stage='testing'
    ),
    CancelNotFinishedAcceptancesCase(
        name='executing_different_stage_one_release',
        acceptances=[[('testing', AcceptanceTestSetStatus.EXECUTING), ('stable', AcceptanceTestSetStatus.EXECUTING)]],
        cancel_stage='testing'
    ),
    CancelNotFinishedAcceptancesCase(
        name='multiple_executing_in_1_release',
        acceptances=[[('testing', AcceptanceTestSetStatus.EXECUTING), ('testing', AcceptanceTestSetStatus.EXECUTING)]],
        cancel_stage='testing'
    ),
    CancelNotFinishedAcceptancesCase(
        name='multiple_executing_in_2_releases',
        acceptances=[
            [('testing', AcceptanceTestSetStatus.EXECUTING)],
            [('testing', AcceptanceTestSetStatus.EXECUTING)]
        ],
        cancel_stage='testing'
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_finalize_acceptances(mongo: MongoFixture,
                                    release_factory: ReleaseFactory,
                                    deploy_factory: DeployFactory,
                                    service_config_factory: ServiceConfigFactory,
                                    acceptance_factory: AcceptanceFactory,
                                    case: CancelNotFinishedAcceptancesCase):
    service_config = await service_config_factory(name='maps-fake-service')
    target_state = {}
    for i, release_acceptances in enumerate(case.acceptances):
        release_id = await release_factory(
            service_config=service_config,
            major_version=i+1,
            minor_version=1,
            origin_arc_hash=f'fake-hash-{i}'
        )
        target_state[release_id] = {}
        for stage, acceptance_status in release_acceptances:
            acceptance_id = await acceptance_factory(
                release_id=release_id,
                deploys=[],
                tasks=[],
                status=acceptance_status,
                stage=stage,
            )
            if acceptance_status != AcceptanceTestSetStatus.EXECUTING or stage != case.cancel_stage:
                target_state[release_id][acceptance_id] = acceptance_status
            else:
                target_state[release_id][acceptance_id] = AcceptanceTestSetStatus.TO_CANCEL
    async with await mongo.async_client.start_session() as session:
        await AcceptanceApi(session).cancel_not_finished_acceptances(service_config=service_config, stage=case.cancel_stage)

    async for release in mongo.async_client.get_database().release.find():
        release_id = str(release['_id'])
        for acceptance in release['acceptance']:
            acceptance_doc = AcceptanceTestSetLaunchDocument.build_from_mongo(acceptance)
            assert target_state[release_id][str(acceptance_doc.acceptance_id)] == acceptance_doc.status
