import pytest
from dataclasses import dataclass, field

from maps.infra.sedem.machine.tests.typing import (
    AcceptanceFactory,
    DeployFactory,
    MongoFixture,
    ReleaseFactory,
    ServiceConfigFactory,
)
from maps.infra.sedem.machine.lib.acceptance_api import (
    AcceptanceApi, AcceptanceTestStatus, AcceptanceTestSetStatus,
    AcceptanceTestLaunchDocument
)
from maps.infra.sedem.machine.lib.collections import DeployStatusDocument


@dataclass
class LookupAcceptancesToStartTestCase:
    name: str

    deploy_statuses: list[DeployStatusDocument]
    must_be_found: bool
    schedulers: list[str] = field(default_factory=list)
    templates: list[str] = field(default_factory=list)

    def __str__(self) -> str:
        return self.name


LOOKUP_ACCEPTANCES_TO_START_TEST_CASES = [
    LookupAcceptancesToStartTestCase(
        name='all_success',
        deploy_statuses=[DeployStatusDocument(success=DeployStatusDocument.Success())],
        must_be_found=True,
        schedulers=['1']
    ),
    LookupAcceptancesToStartTestCase(
        name='not_deployed',
        deploy_statuses=[DeployStatusDocument(pending=DeployStatusDocument.Pending())],
        must_be_found=False,
        schedulers=['1']
    ),
    LookupAcceptancesToStartTestCase(
        name='one_unit_deployed',
        deploy_statuses=[DeployStatusDocument(pending=DeployStatusDocument.Pending()),
                         DeployStatusDocument(success=DeployStatusDocument.Success())],
        must_be_found=False,
        schedulers=['1']
    ),
    LookupAcceptancesToStartTestCase(
        name='multiple_units_success',
        deploy_statuses=[DeployStatusDocument(success=DeployStatusDocument.Success()),
                         DeployStatusDocument(success=DeployStatusDocument.Success())],
        must_be_found=True,
        schedulers=['1']
    ),
    LookupAcceptancesToStartTestCase(
        name='template',
        deploy_statuses=[DeployStatusDocument(success=DeployStatusDocument.Success())],
        must_be_found=True,
        templates=['TEMPLATE']
    ),
    LookupAcceptancesToStartTestCase(
        name='template_and_scheduler',
        deploy_statuses=[DeployStatusDocument(success=DeployStatusDocument.Success())],
        must_be_found=True,
        schedulers=['1'],
        templates=['TEMPLATE']
    )

]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', LOOKUP_ACCEPTANCES_TO_START_TEST_CASES, ids=str)
async def test_lookup_acceptances_to_start(mongo: MongoFixture, release_factory: ReleaseFactory,
                                           deploy_factory: DeployFactory,
                                           service_config_factory: ServiceConfigFactory,
                                           acceptance_factory: AcceptanceFactory,
                                           case: LookupAcceptancesToStartTestCase):
    service_config = await service_config_factory(name='maps-fake-service', sox=False)

    release_id = await release_factory(
        service_config=service_config,
        major_version=1,
        minor_version=1,
    )
    deploys = []
    for deploy_status in case.deploy_statuses:
        deploys.append(
            await deploy_factory(
                service_config=service_config,
                status=deploy_status
            )
        )
    acceptance_tasks = [
        AcceptanceTestLaunchDocument(scheduler_id=scheduler_id, status=AcceptanceTestStatus.PENDING)
        for scheduler_id in case.schedulers
    ]
    acceptance_tasks+=[
        AcceptanceTestLaunchDocument(template_name=template_name, status=AcceptanceTestStatus.PENDING)
        for template_name in case.templates
    ]
    acceptance_id = await acceptance_factory(
        release_id=release_id,
        deploys=deploys,
        tasks=acceptance_tasks,
        status=AcceptanceTestSetStatus.PENDING
    )
    async with await mongo.async_client.start_session() as session:
        found_schedulers = set()
        found_templates = set()
        async for acceptance, launch in AcceptanceApi(session).lookup_acceptances_to_start():
            assert case.must_be_found
            assert acceptance == acceptance_id
            if scheduler := launch.scheduler_id:
                found_schedulers.add(scheduler)
            elif template := launch.template_name:
                found_templates.add(template)
            else:
                assert False
        if case.must_be_found:
            assert found_schedulers == set(case.schedulers)
            assert found_templates == set(case.templates)
        else:
            assert found_schedulers == set()
            assert found_templates == set()
