from unittest.mock import patch

import pytest

from maps.infra.sedem.cli.commands.release import release
from maps.pylibs.fixtures.api_fixture import ApiFixture
from maps.infra.sedem.machine.tests.integration_tests.fixtures.machine_fixture import MachineFixture
from maps.pylibs.fixtures.arcadia.fixture import ArcadiaFixture
from maps.infra.sedem.cli.tests.release.utils.test_data import (
    assert_click_result, set_sedem_machine
)


class TestDeprecation:

    def test_deprecated_version(self, fixture_factory):
        """
        Run on deprecated version
        """
        fixture_factory(ApiFixture)
        fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)

        current_version = 99
        machine_fixture.set_latest_cli_version(version=100)

        with pytest.raises(Exception, match=fr'Sedem version r{current_version} is deprecated.+'), \
                patch('maps.infra.sedem.cli.lib.release.deprecation.sedem_version', lambda: f'{current_version}'):
            assert_click_result(release, ['info', 'maps/infra/teapot'])

    def test_latest_version(self, fixture_factory):
        """
        Run on latest version
        """
        fixture_factory(ApiFixture)
        fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)

        set_sedem_machine(fixture_factory, service_path='maps/infra/teapot')

        current_version = 100
        machine_fixture.set_latest_cli_version(version=current_version)

        with patch('maps.infra.sedem.cli.lib.release.deprecation.sedem_version', lambda: f'{current_version}'):
            assert_click_result(release, ['info', 'maps/infra/teapot'])

    def test_backend_unavailable(self, fixture_factory):
        """
        Run when backend is down
        """
        fixture_factory(ApiFixture)
        fixture_factory(ArcadiaFixture)
        machine_fixture: MachineFixture = fixture_factory(MachineFixture)

        current_version = 99
        machine_fixture.set_latest_cli_version(version=current_version)
        machine_fixture.shut_down()

        with (
            pytest.raises(Exception, match=r'Unable to get service configuration'),
            patch('maps.infra.sedem.cli.lib.release.deprecation.sedem_version', lambda: f'{current_version}')
        ):
            assert_click_result(release, ['info', 'maps/infra/teapot'])
