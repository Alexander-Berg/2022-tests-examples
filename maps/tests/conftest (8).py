import aiohttp.pytest_plugin
import pytest

from maps_adv.common.aiosup import SupClient, ThrottlePolicies

del aiohttp.pytest_plugin.loop


@pytest.fixture
def mock_push(aresponses):
    return lambda h: aresponses.add("sup.server", "/pushes", "POST", h)


@pytest.fixture
async def sup_client(mocker):
    mocker.patch.object(SupClient, "request_max_attempts", 2)
    mocker.patch.object(SupClient, "retry_wait_multiplier", 0.0001)

    async with SupClient(
        "http://sup.server",
        auth_token="some_token",
        project="any_project_name",
        throttle_policies=ThrottlePolicies(
            install_id="default-install-policy",
            device_id="default-device-policy",
            content_id="default-content-policy",
        ),
    ) as client:
        yield client
