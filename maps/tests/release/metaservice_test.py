from maps.infra.sedem.cli.commands.release import release_step, release_start
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result, set_sedem_machine, MachineRelease, MachineCandidate
)
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.sentinels import DUMMY_REVISION


class TestReleaseStart:

    def test_release(self, fixture_factory):
        """
        Sedem machine has no releases yet and there is one of each release candidate in sandbox.
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
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teacup',
            MachineCandidate(
                trunk_revision=revision_to_release,
            )
        )

        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:1')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teacup_testing', docker_tag='maps/core-teacup:1')

        assert_click_result(release_start, ['maps/infra/teaset'])

        expected_docker_tag = f'maps/core-teapot:{revision_to_release}'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == expected_docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == expected_docker_tag

        expected_docker_tag = f'maps/core-teacup:{revision_to_release}'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teacup_testing') == expected_docker_tag

        # Sedem Machine created release to testing.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
        assert len(release.deploys) == 2
        assert all(deploy.deploy_unit in ('testing', 'load') for deploy in release.deploys)

        release, *_ = machine_fixture.releases(service_name='maps-core-teacup')
        assert len(release.deploys) == 1
        assert release.deploys[0].deploy_unit == 'testing'


class TestReleaseStep:

    def test_release_step(self, fixture_factory):
        """
        Sedem machine has one branch created with a tag to be released in stable.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        revision_to_release = 100
        for service_name in ('teapot', 'teacup'):
            set_sedem_machine(
                fixture_factory,
                f'maps/infra/{service_name}',
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
        expected_docker_tag = f'maps/core-teacup:{revision_to_release}'
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teacup_testing', docker_tag=expected_docker_tag)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teacup_stable', docker_tag='maps/core-teacup:1')
        api_fixture.startrek.clear_issues()

        assert_click_result(release_step, ['maps/infra/teaset', DUMMY_REVISION, 'stable'])

        # Sedem Machine created release to stable.
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
        assert any(deploy.deploy_unit == 'stable' for deploy in release.deploys)
        release, *_ = machine_fixture.releases(service_name='maps-core-teacup')
        assert any(deploy.deploy_unit == 'stable' for deploy in release.deploys)

        # FIXME: uncomment when Sedem Machine will create st-tickets for releases
        # comments = api_fixture.startrek.get_issue_comments(DUMMY_TICKET)
        # assert len(comments) == 2

        # assert comments[0].splitlines() == [
        #     'Deploying as meta-service:',
        #     ' - maps-core-teapot',
        #     ' - maps-core-teacup',
        #     Match.EndsWith(': deploying maps-core-teapot v1.1 to stable'),  # Skipping login.
        # ]
