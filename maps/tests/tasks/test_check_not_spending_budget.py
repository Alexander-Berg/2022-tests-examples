from unittest.mock import Mock

import pytest

from maps_adv.statistics.dashboard.server.lib.tasks import check_not_spending_budget

pytestmark = [pytest.mark.asyncio]


@pytest.mark.mock_dm
@pytest.mark.mock_domain
async def test_runs_data_manager_sync_of_category_search_reports_as_expected(
    warden_client_mock, domain
):
    context = Mock()
    context.client = warden_client_mock

    await check_not_spending_budget(context, domain=domain)

    domain.check_not_spending_budget.assert_called_once_with()


@pytest.mark.mock_dm
@pytest.mark.mock_domain
async def test_does_not_raise_if_run_task_with_additional_params(
    warden_client_mock, domain
):
    await check_not_spending_budget(
        Mock(), domain=domain, imposible_additional_param="param"
    )
