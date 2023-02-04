import pytest

from maps_adv.adv_store.v2.lib.data_managers.campaigns import (
    BaseCampaignsDataManager,
    CampaignsDataManager,
)
from maps_adv.adv_store.v2.lib.data_managers.events import (
    BaseEventsDataManager,
    EventsDataManager,
)
from maps_adv.adv_store.v2.lib.data_managers.moderation import (
    BaseModerationDataManager,
    ModerationDataManager,
)

from . import coro_mock


@pytest.fixture
def campaigns_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_campaigns_dm")
    return request.getfixturevalue("_campaigns_dm")


@pytest.fixture
def _campaigns_dm(config, db):
    return CampaignsDataManager(
        db,
        config["DASHBOARD_API_URL"],
        "hahn",
        "yt_token",
        "//home/campaigns_change_log_table",
    )


@pytest.fixture
def _mock_campaigns_dm():
    class MockDm(BaseCampaignsDataManager):
        create_campaign = coro_mock()
        update_campaign = coro_mock()
        set_status = coro_mock()
        list_campaigns_by_orders = coro_mock()
        list_campaigns_for_budget_analysis = coro_mock()
        list_short_campaigns = coro_mock()
        list_campaigns_summary = coro_mock()
        retrieve_campaign = coro_mock()
        get_status = coro_mock()
        campaign_exists = coro_mock()
        list_campaigns_for_charger = coro_mock()
        list_campaigns_for_charger_cpa = coro_mock()
        refresh_auto_daily_budgets = coro_mock()
        stop_campaigns = coro_mock()
        retrieve_existing_campaign_ids = coro_mock()
        retrieve_targetings = coro_mock()
        set_direct_moderation = coro_mock()
        backup_campaigns_change_log = coro_mock()
        retrieve_campaign_data_for_monitorings = coro_mock()
        set_paid_till = coro_mock()

    return MockDm()


@pytest.fixture
def moderation_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_moderation_dm")
    return request.getfixturevalue("_moderation_dm")


@pytest.fixture
def _moderation_dm(db):
    return ModerationDataManager(db)


@pytest.fixture
def _mock_moderation_dm():
    class MockDm(BaseModerationDataManager):
        list_campaigns = coro_mock()
        create_direct_moderation_for_campaign = coro_mock()
        update_direct_moderation = coro_mock()
        retrieve_direct_moderations_by_status = coro_mock()
        retrieve_direct_moderation = coro_mock()

    return MockDm()


@pytest.fixture
def events_dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_events_dm")
    return request.getfixturevalue("_events_dm")


@pytest.fixture
def _events_dm(db):
    return EventsDataManager(db)


@pytest.fixture
def _mock_events_dm():
    class MockDm(BaseEventsDataManager):
        create_event = coro_mock()
        create_events_for_campaigns = coro_mock()
        retrieve_campaigns_events_by_orders = coro_mock()
        create_events_stopped_budget_reached = coro_mock()
        create_event_stopped_manually = coro_mock()
        create_event_end_datetime_changed = coro_mock()
        create_event_budget_decreased = coro_mock()

    return MockDm()
