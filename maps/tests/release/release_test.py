import logging

import pytest

from maps.infra.sedem.cli.commands.release import release_step, release_start
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result,
    MachineRelease,
    MachineCandidate,
    ReleaseComponent,
    ReleaseStatus,
    set_sedem_machine,
)
from maps.infra.sedem.common.release.sandbox.release_spec import (
    SandboxDeployUnitSpec,
    SandboxReleaseSpec,
    SandboxTaskParameter,
    YavSecret,
    YavSecretParameter,
)
from maps.infra.sedem.common.release.utils import ReleaseError
from maps.infra.sedem.machine.lib.collections import ReleaseCandidateStatus
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.pylibs.fixtures.garden_fixture import GardenDeployUnit, GardenModule
from maps.pylibs.fixtures.sandbox.resources import SandboxTasksBinaryResource
from maps.pylibs.terminal_utils.dialog import ConfirmationError

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


# TODO: Add more checks for post conditions
# TODO: Add tests for failures checks (too soon release, trying to release nonexistent tag/release candidate, etc.)
class TestReleaseStart:

    def test_first_release(self, fixture_factory):
        """
        Sedem machine has no releases yet and there is one release candidate in sandbox.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineCandidate(
                trunk_revision=revision_to_release,
            ),
        )

        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')

        assert_click_result(release_start, ['maps/infra/teapot'])

        expected_docker_tag = f'maps/core-teapot:{revision_to_release}'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == expected_docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == expected_docker_tag

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-core-teapot')
        assert len(releases) == 1
        release = releases[0]
        assert release.major == 1 and release.minor == 1
        assert release.origin_commit.revision == revision_to_release
        assert len(release.deploys) == 2
        for deploy in release.deploys:
            assert deploy.deploy_unit in ('testing', 'load')

    def test_second_release(self, fixture_factory):
        """
        Sedem machine has one release created, and we are making new release.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=1,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')

        assert_click_result(release_start, ['maps/infra/teapot'])

        expected_docker_tag = f'maps/core-teapot:{revision_to_release}'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == expected_docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == expected_docker_tag

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-core-teapot')
        assert len(releases) == 2

    def test_release_already_released(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 1
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        docker_tag = f'maps/core-teapot:{revision_to_release}'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag=docker_tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag=docker_tag)

        with pytest.raises(ReleaseError,
                           match=f'Commit r{revision_to_release}.* is already released as v1.1'):
            assert_click_result(release_start, ['maps/infra/teapot', str(revision_to_release)])

        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == docker_tag

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-core-teapot')
        assert len(releases) == 1

    def test_redeploy_failed(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 1
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deploys=[MachineRelease.Deploy('testing', failed=True)]
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )

        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')

        assert_click_result(release_step, ['maps/infra/teapot', 'v1.1', 'testing'])

    def test_release_no_candidates(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        set_sedem_machine(fixture_factory, service_path='maps/infra/teapot')

        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')

        with pytest.raises(ReleaseError, match='No candidates available for release'):
            assert_click_result(release_start, ['maps/infra/teapot'])

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-core-teapot')
        assert len(releases) == 0

    def test_release_candidate_already_released(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=1,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=1,
            )
        )

        with pytest.raises(ReleaseError, match='No candidates available for release'):
            assert_click_result(release_start, ['maps/infra/teapot'])

        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == 'maps/core-teapot:1'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == 'maps/core-teapot:1'

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-core-teapot')
        assert len(releases) == 1

    def test_release_start_garden(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        api_fixture.abc.add_service(123, 'maps-core-garden')
        api_fixture.abc.add_members('maps-core-garden', ['john-doe', 'jane-doe'])

        revision_to_release = 100

        set_sedem_machine(
            fixture_factory,
            'maps/garden/modules/backa_export',
            MachineCandidate(
                trunk_revision=100,
            )
        )

        assert_click_result(release_start, ['maps/garden/modules/backa_export'])

        expected_module_version = f'{revision_to_release}'
        assert api_fixture.garden.module_version(
            deploy_unit=GardenDeployUnit.TESTING, module_name='backa_export') == expected_module_version

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-garden-backa-export')
        assert len(releases) == 1
        release = releases[0]
        assert release.major == 1 and release.minor == 1
        assert release.origin_commit.revision == revision_to_release
        assert len(release.deploys) == 1
        assert release.deploys[0].deploy_unit == 'testing'

    @pytest.mark.parametrize('create_deploy_units', (False, True))
    def test_release_start_sandbox(self, fixture_factory, create_deploy_units):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        arcadia_fixture: ArcadiaFixture = fixture_factory(ArcadiaFixture)

        revision_to_release = 100
        commit_to_release = arcadia_fixture.commit_info(revision=revision_to_release)
        resource = SandboxTasksBinaryResource(attributes={
            'service_name': 'maps-core-ecstatic-reconfigurator',
            **commit_to_release.sandbox_attributes()
        })

        spec_template = SandboxDeployUnitSpec(
            name='<template>',
            secrets=[
                YavSecretParameter(
                    name='required_secret',
                    secret=YavSecret(
                        secret_id='sec-12345',
                    ),
                ),
                YavSecretParameter(
                    name='empty_secret',
                    secret=None,
                ),
            ],
            parameters=[
                SandboxTaskParameter(
                    name='string',
                    jsonValue='"some string"',
                ),
                SandboxTaskParameter(
                    name='empty',
                    jsonValue='null',
                )
            ],
        )

        if create_deploy_units:
            api_fixture.sandbox.add_template(
                alias='MAPS_CORE_ECSTATIC_RECONFIGURATOR_POSTCOMMIT_TESTING',
                task_type='ECSTATIC_RECONFIGURATOR',
            )
            api_fixture.sandbox.add_scheduler(
                task_type='ECSTATIC_RECONFIGURATOR',
                tags=[
                    'SEDEM_MANAGED',
                    'SERVICE:MAPS_CORE_ECSTATIC_RECONFIGURATOR_SCHEDULER_TESTING',
                ],
            )

        set_sedem_machine(
            fixture_factory,
            'maps/infra/ecstatic/sandbox',
            MachineCandidate(
                trunk_revision=revision_to_release,
                release_spec=SandboxReleaseSpec(
                    task_type='ECSTATIC_RECONFIGURATOR',
                    resource_id=resource.id,
                    deploy_units=[
                        spec_template.copy(update={'name': 'postcommit_testing'}),
                        spec_template.copy(update={'name': 'scheduler_testing'}),
                    ],
                ),
            ),
        )

        assert_click_result(release_start, ['maps/infra/ecstatic/sandbox'])

        template = api_fixture.sandbox.template(alias='MAPS_CORE_ECSTATIC_RECONFIGURATOR_POSTCOMMIT_TESTING')
        assert template.resource_id == resource.id
        assert template.parameters == spec_template.parameters_as_dict()
        scheduler = api_fixture.sandbox.scheduler(tag='SERVICE:MAPS_CORE_ECSTATIC_RECONFIGURATOR_SCHEDULER_TESTING')
        assert scheduler.resource_id == resource.id
        assert scheduler.parameters == spec_template.parameters_as_dict()
        # These parameters came from sedem_config: https://nda.ya.ru/t/XnaepnNs4Ujjtt
        assert scheduler.description.startswith('Reconfigure ecstatic')
        assert set(scheduler.tags) == {
            'SEDEM_MANAGED', 'SERVICE:MAPS_CORE_ECSTATIC_RECONFIGURATOR_SCHEDULER_TESTING', 'MY_SHINY_TAG',
        }
        assert scheduler.kill_timeout == 600
        assert scheduler.priority == {'class': 'SERVICE', 'subclass': 'HIGH'}
        assert scheduler.schedule == {
            'start_time': '2020-01-01T12:00:00+00:00',
            'repetition': {'interval': 900},
            'retry': {
                'ignore': True,
                'interval': None,
            },
            'sequential_run': True,
            'fail_on_error': False,
        }
        assert scheduler.notifications == [
            {'transport': 'email', 'recipients': ['geo-infra-notifications'], 'statuses': ['BREAK', 'FAILURE']}
        ]
        assert scheduler.scheduler_notifications == [
            {'transport': 'email', 'recipients': ['geo-infra-notifications'], 'statuses': ['DELETED', 'FAILURE']}
        ]
        assert scheduler.semaphores == {
            'acquires': [{'name': 'MAPS_CORE_ECSTATIC_RECONFIGURATOR_STABLE', 'capacity': 0, 'weight': 1}],
            'release': ['BREAK', 'FINISH', 'WAIT']
        }

        assert api_fixture.sandbox.delegated_secrets() == spec_template.secret_ids_as_list()

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        releases = machine_fixture.releases(service_name='maps-core-ecstatic-reconfigurator')
        assert len(releases) == 1

        release = releases[0]
        assert release.major == 1 and release.minor == 1
        assert release.origin_commit.revision == revision_to_release
        assert len(release.deploys) == 2
        for deploy in release.deploys:
            assert deploy.deploy_unit in ('postcommit_testing', 'scheduler_testing')

    def test_release_custom_message(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineCandidate(
                trunk_revision=100,
            )
        )

        custom_message = 'Custom release message'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')

        assert_click_result(release_start, ['maps/infra/teapot', '--message', custom_message])

        # expected to create branch v1 with custom release message
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
        assert release.message == custom_message

    def test_release_custom_initial_stage(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/renderer/tilesgen',
            MachineCandidate(
                trunk_revision=100,
            )
        )

        api_fixture.nanny.set_docker_tag(service_name='maps_core_renderer_tilesgen_prestable',
                                         docker_tag='maps/core-renderer-tilesgen:1')

        assert_click_result(release_start, ['maps/renderer/tilesgen'])

        # expected to deploy to prestable as it set as initial stage
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-renderer-tilesgen')
        assert len(release.deploys) == 1
        assert release.deploys[0].deploy_unit == 'prestable'


class TestReleaseStep:

    @pytest.mark.parametrize('component', [
        ReleaseComponent(name='maps-core-teapot',
                         path='maps/infra/teapot',
                         deploy_type='rtc'),
        ReleaseComponent(name='maps-garden-backa-export',
                         path='maps/garden/modules/backa_export',
                         deploy_type='garden')])
    def test_release_step_testing(self,
                                  fixture_factory,
                                  component: ReleaseComponent):
        """
        Sedem machine has one branch created with a tag to be released in testing.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100
        if component.deploy_type == 'rtc':
            component_path = 'maps/garden/modules/backa_export'
        else:
            component_path = 'maps/infra/teapot'

        set_sedem_machine(
            fixture_factory,
            component_path,
            MachineCandidate(
                trunk_revision=100,
            )
        )

        set_sedem_machine(
            fixture_factory,
            component.path,
            MachineRelease(
                version='v123.1',
                trunk_revision=revision_to_release,
            ),
            MachineCandidate(
                trunk_revision=100,
            )
        )

        if component.deploy_type == 'rtc':
            api_fixture.nanny.set_docker_tag('maps_core_teapot_testing', 'maps/core-teapot:1')
            api_fixture.nanny.set_docker_tag('maps_core_teapot_load', 'maps/core-teapot:1')
        else:
            api_fixture.garden.set_module(GardenDeployUnit.TESTING,
                                          GardenModule(module_name='backa_export', module_version='1'))

        assert_click_result(release_step, [component.path, 'v123', 'testing'])

        # Sedem Machine created release to testing.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name=component.name)
        if component.deploy_type == 'rtc':
            assert len(release.deploys) == 2
            for deploy in release.deploys:
                assert deploy.deploy_unit in ('testing', 'load')
        else:
            assert len(release.deploys) == 1
            assert release.deploys[0].deploy_unit == 'testing'

    def test_release_step_stable(self, fixture_factory):
        """
        Sedem machine has a branch with a tag released to testing. Tag should be successfully released to stable.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        expected_docker_tag = f'maps/core-teapot:{revision_to_release}'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag=expected_docker_tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag=expected_docker_tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:1')

        assert_click_result(release_step, ['maps/infra/teapot', 'V1.1', 'stable'])

        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_stable') == expected_docker_tag

        # Sedem Machine created release to stable.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
        assert any(deploy.deploy_unit == 'stable' for deploy in release.deploys)

    def test_release_step_stable_garden(self, fixture_factory):
        """
        Sedem machine has a branch with a tag released to testing. Tag should be successfully released to stable.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100
        set_sedem_machine(
            fixture_factory,
            'maps/garden/modules/backa_export',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=1,
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )

        expected_module_version = f'{revision_to_release}'
        api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.TESTING,
                                      module=GardenModule(module_name='backa_export',
                                                          module_version=expected_module_version))
        api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.STABLE,
                                      module=GardenModule(module_name='backa_export',
                                                          module_version='1'))

        assert_click_result(release_step, ['maps/garden/modules/backa_export', 'V1.1', 'stable'])

        assert api_fixture.garden.module_version(deploy_unit=GardenDeployUnit.STABLE,
                                                 module_name='backa_export') == expected_module_version

        # Sedem Machine created release to stable.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-garden-backa-export')
        assert any(deploy.deploy_unit == 'stable' for deploy in release.deploys)

    def test_nanny_has_crit_alerts(self, fixture_factory):
        """
            Try to release testing->stable while there were some CRITs in testing. Expect an assertion.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        expected_docker_tag = f'maps/core-teapot:{revision_to_release}'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag=expected_docker_tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag=expected_docker_tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:1')

        api_fixture.juggler.add_alert_crit(
            host='maps_core_teapot_testing', service='deploy-unit-meta',
        )
        api_fixture.juggler.add_alert_crit(
            host='maps_core_teapot_load', service='deploy-unit-meta',
        )

        with pytest.raises(ConfirmationError, match=(
            r'There were Juggler CRITs in "testing" stage since last deploy.+:\n'
            r'maps_core_teapot_load: https://nda.ya.ru/.+\n'
            r'maps_core_teapot_testing: https://nda.ya.ru/.+'
        )):
            assert_click_result(release_step, ['maps/infra/teapot', 'v1.1', 'stable'])

    def test_release_not_ready_hotfix(self, fixture_factory):
        """
            Try to release hotfix while it is not ready. Expect an assertion.
        """
        fixture_factory(ApiFixture)
        fixture_factory(ArcadiaFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.2',
                trunk_revision=1,
                status=ReleaseStatus.PREPARING,
            ),
        )

        with pytest.raises(ReleaseError, match=r'Release v1\.2 is PREPARING and cannot be deployed'):
            assert_click_result(release_step, ['maps/infra/teapot', 'v1.2', 'testing'])

    def test_has_no_stage(self, fixture_factory):
        """
            Running step to stage that is not in deploy profile. Expect an assertion.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100

        set_sedem_machine(
            fixture_factory,
            'maps/garden/modules/tie_here_graph',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.TESTING,
                                      module=GardenModule(module_name='tie_here_graph',
                                                          module_version=f'{revision_to_release}'))

        with pytest.raises(
                ReleaseError,
                match='Service maps-garden-tie-here-graph has no stage "stable" in deploy flow'):
            assert_click_result(release_step, ['maps/garden/modules/tie_here_graph', 'v1.1', 'stable'])

    def test_branch_creation_fail(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100

        # Old release
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v1.1',
                trunk_revision=25,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        # New release would be v2.1 with trunk_revision=100, but branch wasn't created.

        tag = f'maps/core-teapot:{revision_to_release}'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag=tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag=tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag=tag)

        with pytest.raises(
                ReleaseError,
                match=r'Unable to find release v2.1.*'):
            assert_click_result(release_step, ['maps/infra/teapot', 'v2.1', 'testing'])

    def test_skipping_stage(self, fixture_factory):
        """
            Running deploy to stage next after expected. Expect a confirmation dialog.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)  # noqa

        revision_to_release = 100

        set_sedem_machine(
            fixture_factory,
            'maps/infra/ecstatic/coordinator',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )

        with pytest.raises(ConfirmationError, match='.*Expected stage for release v1.1 is prestable'):
            assert_click_result(release_step, ['maps/infra/ecstatic/coordinator', 'v1.1', 'stable'])

    def test_deploy_to_previous_stages_after_complete_release(self, fixture_factory):
        """
            Running deploy to previous stages after release to the last one. Expect no confirmation.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100

        set_sedem_machine(
            fixture_factory,
            'maps/infra/ecstatic/coordinator',
            MachineRelease(
                version='v1.1',
                trunk_revision=revision_to_release,
                deployed_to=['testing', 'prestable', 'stable'],
            ),
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )

        tag = f'maps/core-ecstatic-coordinator:{revision_to_release}'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_ecstatic_coordinator_load', docker_tag=tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_ecstatic_coordinator_testing', docker_tag=tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_ecstatic_coordinator_prestable', docker_tag=tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_ecstatic_coordinator_stable', docker_tag=tag)

        assert_click_result(release_step, ['maps/infra/ecstatic/coordinator', 'v1.1', 'prestable'])
        assert_click_result(release_step, ['maps/infra/ecstatic/coordinator', 'v1.1', 'testing'])


class TestRelease:

    def test_release_teapot_complete(self, fixture_factory):
        """
        Creating complete release cycle with release candidate, deploying it to testing and then to stable.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        revision_to_release = 100
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

        # Release works.
        expected_docker_tag = f'maps/core-teapot:{revision_to_release}'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == expected_docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == expected_docker_tag
        # Stable is yet to be released.
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_stable') == 'maps/core-teapot:1'

        assert_click_result(release_step, ['maps/infra/teapot', 'V1.1', 'stable', '--force'])

        # New docker tag is deployed to stable by sedem.
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_stable') == expected_docker_tag

        # Sedem Machine created release to stable.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
        assert any(deploy.deploy_unit == 'stable' for deploy in release.deploys)

    def test_release_garden_complete(self, fixture_factory):
        """
        Creating complete release cycle with release candidate, deploying it to testing and then to stable.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        revision_to_release = 100
        set_sedem_machine(
            fixture_factory,
            'maps/garden/modules/backa_export',
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )
        api_fixture.abc.add_service(123, 'maps-core-garden')

        api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.TESTING,
                                      module=GardenModule(module_name='backa_export', module_version='1'))
        api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.STABLE,
                                      module=GardenModule(module_name='backa_export', module_version='1'))

        assert_click_result(release_start, ['maps/garden/modules/backa_export'])

        expected_module_version = f'{revision_to_release}'
        assert api_fixture.garden.module_version(deploy_unit=GardenDeployUnit.TESTING,
                                                 module_name='backa_export') == expected_module_version
        # Stable is yet to be released.
        assert api_fixture.garden.module_version(deploy_unit=GardenDeployUnit.STABLE,
                                                 module_name='backa_export') == '1'

        assert_click_result(release_step, ['maps/garden/modules/backa_export', 'V1.1', 'stable', '--force'])

        assert api_fixture.garden.module_version(deploy_unit=GardenDeployUnit.STABLE,
                                                 module_name='backa_export') == expected_module_version

        # Sedem Machine created release to stable.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-garden-backa-export')
        assert any(deploy.deploy_unit == 'stable' for deploy in release.deploys)

    def test_release_for_sox_service(self, fixture_factory):
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)

        api_fixture.abc.add_service(123, 'maps-core-teaspoon')
        api_fixture.abc.add_members('maps-core-teaspoon', ['john-doe', 'jane-doe'])

        revision_to_release = 100
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teaspoon',
            MachineCandidate(
                trunk_revision=revision_to_release,
                status=ReleaseCandidateStatus.READY
            )
        )

        api_fixture.nanny.set_docker_tag(service_name='maps_core_teaspoon_testing',
                                         docker_tag='maps/core-teaspoon:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teaspoon_load',
                                         docker_tag='maps/core-teaspoon:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teaspoon_prestable',
                                         docker_tag='maps/core-teaspoon:1')

        # Testing is deployed without approval
        assert_click_result(release_start, ['maps/infra/teaspoon'])

        expected_docker_tag = f'maps/core-teaspoon:{revision_to_release}'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teaspoon_testing') == expected_docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teaspoon_load') == expected_docker_tag

        # Deploy to prestable requires approval
        with pytest.raises(ReleaseError,
                           match=r'Release v1\.1 deployment to \(pre-\)stable is not approved yet\n'
                                 r'See: https://st.yandex-team.ru/MAPSRELEASES-1'):
            assert_click_result(release_step, ['maps/infra/teaspoon', 'v1.1', 'prestable', '--force'])

        assert api_fixture.nanny.docker_tag(service_name='maps_core_teaspoon_prestable') == 'maps/core-teaspoon:1'

        # Approve prestable
        approval_uuid = api_fixture.pushok.uuid_from_st_ticket(st_ticket='MAPSRELEASES-1', release_version='v1.1')
        api_fixture.pushok.approve(approval_uuid)
        machine_fixture.wait_for_approval_update()

        # Approved deploy should succeed
        assert_click_result(release_step, ['maps/infra/teaspoon', 'v1.1', 'prestable', '--force'])

        assert api_fixture.nanny.docker_tag(service_name='maps_core_teaspoon_prestable') == expected_docker_tag
