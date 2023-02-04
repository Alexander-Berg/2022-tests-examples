import pytest

from maps.infra.sedem.cli.commands.release import reject, release_step
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result, set_sedem_machine, MachineRelease, MachineCandidate
)


class TestReleaseReject:
    def test_reject_tag(self, fixture_factory):
        """
        Reject specific Tag
        """
        fixture_factory(ApiFixture)

        scope_to_reject = 10
        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version=f'v{scope_to_reject}.1',
                trunk_revision=9,
                deployed_to='testing',
            ),
            MachineCandidate(
                trunk_revision=9,
            )
        )

        assert_click_result(reject, ['maps/infra/teapot', f'v{scope_to_reject}.1', '-m', ''])

        machine_fixture: MachineFixture = fixture_factory(MachineFixture)
        release, *_ = machine_fixture.releases(service_name='maps-core-teapot')
        assert release.rejected and release.rejected.reason == 'No reason'

        with pytest.raises(Exception, match='Release v10.1 is REJECTED.*'):
            assert_click_result(release_step, ['maps/infra/teapot', f'v{scope_to_reject}.1', 'stable'])

    def test_reject_invalid(self, fixture_factory):
        """
        Reject nonexisting version
        """
        fixture_factory(ApiFixture)

        set_sedem_machine(
            fixture_factory,
            'maps/infra/teapot',
            MachineRelease(
                version='v9.1',
                trunk_revision=1,
            ),
        )

        with pytest.raises(Exception, match='Release v10.1 not found'):
            assert_click_result(reject, ['maps/infra/teapot', 'v10.1', '-m', 'whatever'])
