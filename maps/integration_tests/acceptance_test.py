import asyncio
import logging
import pytest

from maps.infra.sedem.client.machine_api import MachineApi
from maps.infra.sedem.client.sedem_api import SedemApi
from maps.infra.sedem.proto import sedem_pb2
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.sandbox.resources import YaPackageResource
from maps.pylibs.fixtures.sandbox.base_task import TaskStatus


logger = logging.getLogger()
FAKE_OAUTH = 'OAuth AQAD-FAKE'
DeployStatusProto = sedem_pb2.DeployCommitRequest.DeployStatus


def create_configs(acceptance_type: str):
    if acceptance_type == 'scheduler':
        acceptance = [
            sedem_pb2.AcceptanceTest(
                stage='testing',
                sandbox_scheduler=sedem_pb2.AcceptanceTest.SandboxScheduler(
                    id='123'
                )
            )
        ]
    else:
        acceptance = [
            sedem_pb2.AcceptanceTest(
                stage='testing',
                sandbox_template=sedem_pb2.AcceptanceTest.SandboxTemplate(
                    name='ACCEPTANCE_TEMPLATE'
                )
            )
        ]

    teaspoon_config = sedem_pb2.ServiceConfig(
        name='maps-core-teaspoon',
        path='maps/infra/teaspoon',
        abc_slug='maps-core-teaspoon',
        release_type=sedem_pb2.ServiceConfig.ReleaseType.NANNY,
        stages=[
            sedem_pb2.ServiceConfig.Stage(name='testing', deploy_units=['testing']),
            sedem_pb2.ServiceConfig.Stage(name='prestable', deploy_units=['prestable']),
            sedem_pb2.ServiceConfig.Stage(name='stable', deploy_units=['stable']),
        ],
        acceptance=acceptance
    )
    sedem_api = SedemApi(FAKE_OAUTH)
    request = sedem_pb2.UpdateConfigurationRequest(
        service_config=teaspoon_config,
    )
    sedem_api.configuration_update(
        configuration=request,
        revision=12345
    )


async def atest_correct_launch(fixture_factory, acceptance_type: str):
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    nanny_fixture = api_fixture.nanny
    sandbox_fixture = api_fixture.sandbox
    machine_api = MachineApi(FAKE_OAUTH)
    revision = 98
    commit_info = arcadia_fixture.commit_info(revision)
    resource_attributes = {
        **commit_info.sandbox_attributes(),
        'resource_name': 'core-teaspoon'
    }
    YaPackageResource(attributes=resource_attributes)

    await asyncio.to_thread(
        machine_api.release_create,
        service_name='maps-core-teaspoon',
        major_version=1,
        revision=revision,
        message=f'Release on r{revision}'
    )
    nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                 'maps/core-teaspoon:98')

    release_deploy_validate_result = await asyncio.to_thread(
        machine_api.release_deploy_validate,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )

    assert len(release_deploy_validate_result.checks) == 0

    release_deploy_prepare_result = await asyncio.to_thread(
        machine_api.release_deploy_prepare,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )
    await asyncio.to_thread(
        machine_api.release_deploy_commit,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing',
        deploy_commit_request=sedem_pb2.DeployCommitRequest(
            deploys={
                deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                for deploy_id in release_deploy_prepare_result.deploys.values()
            }
        ),
    )
    release_deploy_validate_result = await asyncio.to_thread(
        machine_api.release_deploy_validate,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='prestable'
    )
    assert len(release_deploy_validate_result.checks) == 1
    Check = sedem_pb2.DeployValidationResponse.Check
    assert release_deploy_validate_result.checks[0].status == Check.Status.WARN
    assert release_deploy_validate_result.checks[0].message == 'Acceptances for deploy to prestable were not started'

    job_manager = machine_fixture.get_app().job_manager
    sandbox_fixture.add_scheduler(scheduler_id=123, task_type='SHOOT_VIA_TANKAPI')
    sandbox_fixture.add_template(alias='ACCEPTANCE_TEMPLATE', task_type='SHOOT_VIA_TANKAPI')
    job_manager.update_deploy_statuses_job.run_soon()
    await job_manager.update_deploy_statuses_job.wait_next_iteration()
    job_manager.launch_acceptances_job.run_soon()
    await job_manager.launch_acceptances_job.wait_next_iteration()
    job_manager.set_acceptances_launch_status_executing_job.run_soon()
    await job_manager.set_acceptances_launch_status_executing_job.wait_next_iteration()

    job_manager.finalize_acceptances_job.run_soon()
    await job_manager.finalize_acceptances_job.wait_next_iteration()
    for task in sandbox_fixture.tasks():
        if task.type != 'SHOOT_VIA_TANKAPI':
            continue
        task.status = TaskStatus.EXECUTING
        break
    else:
        assert False, 'Not found SHOOT_VIA_TANKAPI task'
    job_manager.update_executing_acceptances_status_job.run_soon()
    await job_manager.update_executing_acceptances_status_job.wait_next_iteration()

    releases = await asyncio.to_thread(
        machine_fixture.releases,
        service_name='maps-core-teaspoon',
    )
    acceptance_test_set = releases[0].acceptance[0]
    assert acceptance_test_set.status == 'executing'
    acceptance_launch = acceptance_test_set.launches[0]
    assert acceptance_launch.task_id is not None
    if acceptance_type == 'scheduler':
        assert acceptance_launch.scheduler_id == '123'
        assert acceptance_launch.template_name is None
    else:
        assert acceptance_launch.template_name == 'ACCEPTANCE_TEMPLATE'
        assert acceptance_launch.scheduler_id is None
    assert acceptance_launch.status == 'executing'
    release_deploy_validate_result = await asyncio.to_thread(
        machine_api.release_deploy_validate,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='prestable'
    )
    assert len(release_deploy_validate_result.checks) == 1
    assert release_deploy_validate_result.checks[0].status == Check.Status.WARN
    assert 'is still executing' in release_deploy_validate_result.checks[0].message

    for task in sandbox_fixture.tasks():
        if task.type != 'SHOOT_VIA_TANKAPI':
            continue
        task.status = TaskStatus.SUCCESS
        assert 'Acceptance test for maps-core-teaspoon release v1.1' in task.description
        break
    else:
        assert False, 'Not found SHOOT_VIA_TANKAPI task'
    job_manager.update_executing_acceptances_status_job.run_soon()
    await job_manager.update_executing_acceptances_status_job.wait_next_iteration()
    job_manager.finalize_acceptances_job.run_soon()
    await job_manager.finalize_acceptances_job.wait_next_iteration()

    releases = await asyncio.to_thread(
        machine_fixture.releases,
        service_name='maps-core-teaspoon',
    )
    st_comments = api_fixture.startrek.get_issue_comments(releases[0].st_ticket)
    assert any('Acceptance' in st_comment for st_comment in st_comments)
    acceptance_test_set = releases[0].acceptance[0]
    assert acceptance_test_set.status == 'finished'
    acceptance_launch = acceptance_test_set.launches[0]
    assert acceptance_launch.task_id is not None
    if acceptance_type == 'scheduler':
        assert acceptance_launch.scheduler_id == '123'
        assert acceptance_launch.template_name is None
    else:
        assert acceptance_launch.template_name == 'ACCEPTANCE_TEMPLATE'
        assert acceptance_launch.scheduler_id is None
    assert acceptance_launch.status == 'success'
    release_deploy_validate_result = await asyncio.to_thread(
        machine_api.release_deploy_validate,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='prestable'
    )
    assert len(release_deploy_validate_result.checks) == 1
    assert release_deploy_validate_result.checks[0].status == Check.Status.OK


@pytest.mark.parametrize('acceptance_type', ['scheduler', 'template'])
def test_correct_launch(fixture_factory, acceptance_type: str):
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    create_configs(acceptance_type)
    machine_fixture.run_until_complete(atest_correct_launch(fixture_factory, acceptance_type))


async def atest_pass_mount_url_to_template(fixture_factory):
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    nanny_fixture = api_fixture.nanny
    sandbox_fixture = api_fixture.sandbox
    machine_api = MachineApi(FAKE_OAUTH)
    revision = 98
    commit_info = arcadia_fixture.commit_info(revision)
    resource_attributes = {
        **commit_info.sandbox_attributes(),
        'resource_name': 'core-teaspoon'
    }
    YaPackageResource(attributes=resource_attributes)

    await asyncio.to_thread(
        machine_api.release_create,
        service_name='maps-core-teaspoon',
        major_version=1,
        revision=revision,
        message=f'Release on r{revision}'
    )
    nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                 'maps/core-teaspoon:98')
    release_deploy_prepare_result = await asyncio.to_thread(
        machine_api.release_deploy_prepare,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )

    await asyncio.to_thread(
        machine_api.release_deploy_commit,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing',
        deploy_commit_request=sedem_pb2.DeployCommitRequest(
            deploys={
                deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                for deploy_id in release_deploy_prepare_result.deploys.values()
            }
        ),
    )
    sandbox_fixture.add_template('ACCEPTANCE_TEMPLATE', 'ACCEPTANCE_TASK')
    job_manager = machine_fixture.get_app().job_manager
    job_manager.update_deploy_statuses_job.run_soon()
    await job_manager.update_deploy_statuses_job.wait_next_iteration()
    job_manager.launch_acceptances_job.run_soon()
    await job_manager.launch_acceptances_job.wait_next_iteration()
    for task in sandbox_fixture.tasks():
        if task.type != 'ACCEPTANCE_TASK':
            continue
        assert task.status == TaskStatus.SUCCESS
        for parameter in task.custom_fields:
            if parameter.name == 'arcadia_url':
                assert parameter.value == f'arcadia-arc:/#{commit_info.arc_hash}'
                break
        else:
            assert False, 'Not found arcadia_url parameter'
        assert task.notifications == [{
            'transport': 'email',
            'statuses': ['FAILURE', 'BREAK'],
            'recipients': ['robot-maps-sandbox']
        }]
        break
    else:
        assert False, 'Not found acceptance task'


def test_pass_mount_url_to_template(fixture_factory):
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    create_configs('template')
    machine_fixture.run_until_complete(atest_pass_mount_url_to_template(fixture_factory))


async def atest_check_running_acceptance(fixture_factory):
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    nanny_fixture = api_fixture.nanny
    sandbox_fixture = api_fixture.sandbox
    machine_api = MachineApi(FAKE_OAUTH)
    revision = 98
    commit_info = arcadia_fixture.commit_info(revision)
    resource_attributes = {
        **commit_info.sandbox_attributes(),
        'resource_name': 'core-teaspoon'
    }
    YaPackageResource(attributes=resource_attributes)

    await asyncio.to_thread(
        machine_api.release_create,
        service_name='maps-core-teaspoon',
        major_version=1,
        revision=revision,
        message=f'Release on r{revision}'
    )
    nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                 'maps/core-teaspoon:98')

    release_deploy_prepare_result = await asyncio.to_thread(
        machine_api.release_deploy_prepare,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )
    await asyncio.to_thread(
        machine_api.release_deploy_commit,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing',
        deploy_commit_request=sedem_pb2.DeployCommitRequest(
            deploys={
                deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                for deploy_id in release_deploy_prepare_result.deploys.values()
            }
        ),
    )

    job_manager = machine_fixture.get_app().job_manager
    sandbox_fixture.add_scheduler(scheduler_id=123, task_type='SHOOT_VIA_TANKAPI')
    sandbox_fixture.add_template(alias='ACCEPTANCE_TEMPLATE', task_type='SHOOT_VIA_TANKAPI')
    job_manager.update_deploy_statuses_job.run_soon()
    await job_manager.update_deploy_statuses_job.wait_next_iteration()
    job_manager.launch_acceptances_job.run_soon()
    await job_manager.launch_acceptances_job.wait_next_iteration()
    job_manager.set_acceptances_launch_status_executing_job.run_soon()
    await job_manager.set_acceptances_launch_status_executing_job.wait_next_iteration()

    job_manager.finalize_acceptances_job.run_soon()
    await job_manager.finalize_acceptances_job.wait_next_iteration()
    for task in sandbox_fixture.tasks():
        if task.type != 'SHOOT_VIA_TANKAPI':
            continue
        task.status = TaskStatus.EXECUTING
        found_task = task.id
        break
    else:
        assert False, 'Not found SHOOT_VIA_TANKAPI task'
    release_deploy_validate_result = await asyncio.to_thread(
        machine_api.release_deploy_validate,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )
    assert len(release_deploy_validate_result.checks) == 1
    check = release_deploy_validate_result.checks[0]
    assert check.status == sedem_pb2.DeployValidationResponse.Check.Status.WARN
    assert check.message == f'Acceptance task https://sandbox.yandex-team.ru/task/{found_task} is still executing'


def test_check_running_acceptance(fixture_factory):
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    create_configs('template')
    machine_fixture.run_until_complete(atest_check_running_acceptance(fixture_factory))


async def atest_cancel_acceptances(fixture_factory):
    api_fixture: ApiFixture = fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    nanny_fixture = api_fixture.nanny
    sandbox_fixture = api_fixture.sandbox
    machine_api = MachineApi(FAKE_OAUTH)
    revision = 98
    commit_info = arcadia_fixture.commit_info(revision)
    resource_attributes = {
        **commit_info.sandbox_attributes(),
        'resource_name': 'core-teaspoon'
    }
    YaPackageResource(attributes=resource_attributes)

    await asyncio.to_thread(
        machine_api.release_create,
        service_name='maps-core-teaspoon',
        major_version=1,
        revision=revision,
        message=f'Release on r{revision}'
    )
    nanny_fixture.set_docker_tag('maps_core_teaspoon_testing',
                                 'maps/core-teaspoon:98')

    release_deploy_prepare_result = await asyncio.to_thread(
        machine_api.release_deploy_prepare,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )
    await asyncio.to_thread(
        machine_api.release_deploy_commit,
        service_name='maps-core-teaspoon',
        version='v1.1',
        deploy_commit_request=sedem_pb2.DeployCommitRequest(
            deploys={
                deploy_id: DeployStatusProto(success=DeployStatusProto.Success())
                for deploy_id in release_deploy_prepare_result.deploys.values()
            },
            comment="test warning"
        ),
        previous_version='v1.0',
        step='testing'
    )
    job_manager = machine_fixture.get_app().job_manager
    sandbox_fixture.add_template(alias='ACCEPTANCE_TEMPLATE', task_type='SHOOT_VIA_TANKAPI')
    job_manager.update_deploy_statuses_job.run_soon()
    await job_manager.update_deploy_statuses_job.wait_next_iteration()
    job_manager.launch_acceptances_job.run_soon()
    await job_manager.launch_acceptances_job.wait_next_iteration()
    job_manager.set_acceptances_launch_status_executing_job.run_soon()
    await job_manager.set_acceptances_launch_status_executing_job.wait_next_iteration()
    for task in sandbox_fixture.tasks():
        if task.type != 'SHOOT_VIA_TANKAPI':
            continue
        task.status = TaskStatus.EXECUTING
        break
    else:
        assert False, 'Not found SHOOT_VIA_TANKAPI task'
    await asyncio.to_thread(
        machine_api.release_deploy_prepare,
        service_name='maps-core-teaspoon',
        version='v1.1',
        step='testing'
    )
    job_manager.cancel_acceptances_job.run_soon()
    await job_manager.cancel_acceptances_job.wait_next_iteration()
    for task in sandbox_fixture.tasks():
        if task.type != 'SHOOT_VIA_TANKAPI':
            continue
        assert task.status == TaskStatus.STOPPED
        break
    else:
        assert False, 'Not found SHOOT_VIA_TANKAPI task'
    job_manager.finalize_cancelled_acceptances_job.run_soon()
    await job_manager.finalize_cancelled_acceptances_job.wait_next_iteration()
    releases = await asyncio.to_thread(
        machine_fixture.releases,
        service_name='maps-core-teaspoon',
    )
    assert releases[0].acceptance[0].status == 'cancelled'
    for task in releases[0].acceptance[0].launches:
        assert task.status == 'failure'


def test_cancel_acceptances(fixture_factory):
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    create_configs('template')
    machine_fixture.run_until_complete(atest_cancel_acceptances(fixture_factory))
