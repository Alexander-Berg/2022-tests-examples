import logging

import pytest
from maps.infra.sedem.cli.commands.release import rollback
from maps.infra.sedem.cli.lib.release.snapshot import SnapshotState
from maps.infra.sedem.common.release.utils import ReleaseError
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.pylibs.fixtures.garden_fixture import GardenDeployUnit, GardenModule
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result, set_sedem_machine, MachineRelease, MachineCandidate
)

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)


class TestRollback:

    def test_rollback_to_released_rtc(self, fixture_factory):
        """
        Release machine has previous release that sedem will rollback for RTC.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(version='v1.1', trunk_revision=10, deployed_to='testing'),
            MachineRelease(version='v2.1', trunk_revision=20, deployed_to='testing'),
            MachineCandidate(
                trunk_revision=10,
            )
        )
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:20')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:20')

        assert_click_result(rollback, ['maps/infra/teapot', 'testing', 'V1.1'])

        expected_docker_tag = 'maps/core-teapot:10'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_load') == expected_docker_tag
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_testing') == expected_docker_tag

    def test_rollback_to_released_garden(self, fixture_factory):
        """
        Release machine has previous release that sedem will rollback for Garden.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/garden/modules/backa_export',
            MachineRelease(version='v1.1', trunk_revision=10, deployed_to='testing'),
            MachineRelease(version='v2.1', trunk_revision=20, deployed_to='testing'),
            MachineCandidate(
                trunk_revision=10,
            )
        )
        current_module = GardenModule(
            module_name='backa_export',
            module_version='20',
        )
        rollback_module = GardenModule(
            module_name='backa_export',
            module_version='10',
        )
        api_fixture.garden.set_module(deploy_unit=GardenDeployUnit.TESTING, module=current_module)

        assert_click_result(rollback, ['maps/garden/modules/backa_export', 'testing', 'V1.1'])

        assert api_fixture.garden.module_version(
            deploy_unit=GardenDeployUnit.TESTING, module_name='backa_export') == rollback_module.module_version

    def test_rollback_to_unreleased(self, fixture_factory):
        """
        Rollback to version that wasn't released to that step. Expect fail.
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(version='v1.1', trunk_revision=10, deployed_to='testing'),
            MachineRelease(version='v2.1', trunk_revision=20, deployed_to='testing'),
            MachineCandidate(
                trunk_revision=20,
            )
        )
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_load', docker_tag='maps/core-teapot:20')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_testing', docker_tag='maps/core-teapot:20')
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:10')

        with pytest.raises(ReleaseError, match='Release v2.1 was never deployed to stable.*'):
            assert_click_result(rollback, ['maps/infra/teapot', 'stable', 'v2.1'])

        expected_docker_tag = 'maps/core-teapot:10'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_stable') == expected_docker_tag

    def test_no_rollback_to_ready_snapshot(self, fixture_factory):
        """
        Rollback to version that still has snapshot in nanny.
        Expect creation of a new snapshot.
        Rationale: GEOINFRA-1911
        """
        api_fixture: ApiFixture = fixture_factory(ApiFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(version='v1.1', trunk_revision=10, deployed_to='stable'),
            MachineRelease(version='v2.1', trunk_revision=20, deployed_to='stable'),
            MachineCandidate(
                trunk_revision=10,
            )
        )
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:10',
                                         state=SnapshotState.PREPARED)
        api_fixture.nanny.set_docker_tag(service_name='maps_core_teapot_stable', docker_tag='maps/core-teapot:20')

        assert_click_result(rollback, ['maps/infra/teapot', 'stable', 'V1.1'])

        expected_docker_tag = 'maps/core-teapot:10'
        assert api_fixture.nanny.docker_tag(service_name='maps_core_teapot_stable') == expected_docker_tag
        assert len(api_fixture.nanny.snapshots(service_name='maps_core_teapot_stable')) == 3
