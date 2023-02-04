import asyncio

import pytest

from maps.infra.sedem.client.machine_api import MachineApi
from maps.infra.sedem.machine.tests.typing import CliSettingsFactory


@pytest.mark.asyncio
async def test_actual_cli_version(machine_api: MachineApi,
                                  cli_settings_factory: CliSettingsFactory) -> None:
    await cli_settings_factory(min_valid_version=1)

    response = await asyncio.to_thread(
        machine_api.cli_info,
        version=123,
    )
    assert not response.need_update


@pytest.mark.asyncio
async def test_outdated_cli_version(machine_api: MachineApi,
                                    cli_settings_factory: CliSettingsFactory) -> None:
    await cli_settings_factory(min_valid_version=100500)

    response = await asyncio.to_thread(
        machine_api.cli_info,
        version=123,
    )
    assert response.need_update
