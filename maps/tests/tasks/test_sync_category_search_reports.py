# from datetime import timedelta
from unittest.mock import Mock

import pytest

# from maps_adv.common.helpers import coro_mock
from maps_adv.statistics.dashboard.server.lib.tasks import sync_category_search_reports

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_dm
async def test_runs_data_manager_sync_of_category_search_reports_as_expected(
    warden_client_mock, dm
):
    context = Mock()
    context.client = warden_client_mock

    await sync_category_search_reports(context, dm=dm)

    dm.sync_category_search_reports.assert_called_once_with()


@pytest.mark.mock_dm
async def test_does_not_raise_if_run_task_with_additional_params(
    warden_client_mock, dm
):
    await sync_category_search_reports(
        Mock(), dm=dm, imposible_additional_param="param"
    )
