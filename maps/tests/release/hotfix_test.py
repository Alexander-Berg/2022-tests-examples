import logging

from maps.infra.sedem.cli.commands.release import release_start, release_hotfix
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result, set_sedem_machine, MachineCandidate,
)
from maps.infra.sedem.common.release.sandbox.release_spec import SandboxReleaseSpec, SandboxDeployUnitSpec
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.sandbox.resources import (
    SandboxTasksBinaryResource,
)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


def test_release_hotfix_nanny(fixture_factory):
    api_fixture: ApiFixture = fixture_factory(ApiFixture)

    revision_to_release = 99
    set_sedem_machine(
        fixture_factory,
        'maps/infra/teapot',
        MachineCandidate(
            trunk_revision=revision_to_release,
        )
    )

    api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
    api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')
    api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:1')

    assert_click_result(release_start, ['maps/infra/teapot'])

    # check release created
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    created_release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
    assert created_release.origin_commit.revision == revision_to_release

    # hotfix created release
    hotfix_revision = 100
    assert_click_result(release_hotfix,
                        ['maps/infra/teapot',
                         '-b', str(created_release.major),
                         '-r', str(hotfix_revision)])
    hotfix_release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
    assert hotfix_release.major == created_release.major and hotfix_release.minor == created_release.minor + 1


def test_release_hotfix_garden(fixture_factory):
    api_fixture = fixture_factory(ApiFixture)
    api_fixture.abc.add_service(123, 'maps-core-garden')

    revision_to_release = 99
    set_sedem_machine(
        fixture_factory,
        'maps/garden/modules/ymapsdf',
        MachineCandidate(
            trunk_revision=revision_to_release,
        )
    )

    assert_click_result(release_start, ['maps/garden/modules/ymapsdf'])

    # check release created
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    created_release, *_ = machine_fixture.releases(service_name='maps-garden-ymapsdf')
    assert created_release.origin_commit.revision == revision_to_release

    # hotfix created release
    hotfix_revision = 100
    assert_click_result(release_hotfix,
                        ['maps/garden/modules/ymapsdf',
                         '-b', str(created_release.major),
                         '-r', str(hotfix_revision)])
    hotfix_release, *_ = machine_fixture.releases(service_name='maps-garden-ymapsdf')
    assert hotfix_release.major == created_release.major and hotfix_release.minor == created_release.minor + 1


def test_release_hotfix_sandbox(fixture_factory):
    fixture_factory(ApiFixture)
    arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

    revision_to_release = 99
    commit99 = arcadia_fixture.commit_info(revision=revision_to_release)
    resource = SandboxTasksBinaryResource(attributes={
        'service_name': 'maps-core-ecstatic-reconfigurator',
        **commit99.sandbox_attributes()
    })

    set_sedem_machine(
        fixture_factory,
        'maps/infra/ecstatic/sandbox',
        MachineCandidate(
            trunk_revision=99,
            release_spec=SandboxReleaseSpec(
                task_type='ECSTATIC_RECONFIGURATOR',
                resource_id=resource.id,
                deploy_units=[
                    SandboxDeployUnitSpec(name='postcommit_stable'),
                    SandboxDeployUnitSpec(name='scheduler_stable'),
                    SandboxDeployUnitSpec(name='postcommit_testing'),
                    SandboxDeployUnitSpec(name='scheduler_testing'),
                ],
            ),
        ),
    )

    assert_click_result(release_start, ['maps/infra/ecstatic/sandbox'])

    # check release created
    machine_fixture: MachineFixture = fixture_factory(MachineFixture)
    created_release, *_ = machine_fixture.releases(service_name='maps-core-ecstatic-reconfigurator')
    assert created_release.origin_commit.revision == revision_to_release

    # hotfix created release
    hotfix_revision = 100
    assert_click_result(release_hotfix,
                        ['maps/infra/ecstatic/sandbox',
                         '-b', str(created_release.major),
                         '-r', str(hotfix_revision)])
    hotfix_release, *_ = machine_fixture.releases(service_name='maps-core-ecstatic-reconfigurator')
    assert hotfix_release.major == created_release.major and hotfix_release.minor == created_release.minor + 1
