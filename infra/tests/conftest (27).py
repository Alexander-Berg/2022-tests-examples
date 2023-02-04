import pytest

from infra.rtc_sla_tentacles.backend.lib.harvesters.manager import HarvestersManager


@pytest.fixture
def harvesters_manager(config_interface, harvesters_snapshot_manager) -> HarvestersManager:
    """
        Creates and returns an instance of 'HarvestersManager'.
    """
    return HarvestersManager(config_interface, harvesters_snapshot_manager)
