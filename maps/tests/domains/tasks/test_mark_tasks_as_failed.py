import pytest

from maps_adv.warden.server.lib.domains.tasks import Domain

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_called_with_passed_extra_time(dm, config):
    domain = Domain(dm, config["EXTRA_EXECUTION_TIME"])

    await domain.mark_tasks_as_failed()

    dm.mark_tasks_as_failed.assert_called_with(
        extra_time=config["EXTRA_EXECUTION_TIME"]
    )
