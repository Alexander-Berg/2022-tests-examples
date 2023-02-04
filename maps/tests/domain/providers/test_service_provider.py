import logging
from decimal import Decimal

import pytest

from maps_adv.geosmb.clients.market import (
    BaseHttpClientException,
    ClientAction,
    MarketIntBadProto,
    MarketIntConflict,
    MarketIntNotFound,
    MarketIntUnknownError,
    ServiceStatus,
)
from maps_adv.geosmb.landlord.server.lib.domain.service_provider import ServiceProvider
from maps_adv.geosmb.landlord.server.lib.enums import Feature, ServiceItemType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def caplog_set_level_error(caplog):
    caplog.set_level(logging.ERROR)


@pytest.fixture
def service_provider(market_int, dm):
    return ServiceProvider(
        dm=dm, market_client=market_int, disable_promoted_services=False
    )


@pytest.fixture(autouse=True)
def dm_fetch_services_mock(dm):
    dm.fetch_org_promoted_services.coro.return_value = []


async def test_returns_promoted_services_if_they_exists(service_provider, dm):
    dm.fetch_org_promoted_services.coro.return_value = [
        dict(
            type=ServiceItemType.PROMOTED,
            id=1,
            name="Стрижка кота",
            image="https://strizhka.url/cat",
            cost="15.99",
            url="https://strizhka.url",
            description="Описание",
        ),
        dict(
            type=ServiceItemType.PROMOTED,
            id=2,
            name="Стрижка барашка",
            cost="250.99",
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert result == [
        dict(
            type=ServiceItemType.PROMOTED,
            id=1,
            name="Стрижка кота",
            image="https://strizhka.url/cat",
            cost="15.99",
            url="https://strizhka.url",
            description="Описание",
        ),
        dict(
            type=ServiceItemType.PROMOTED,
            id=2,
            name="Стрижка барашка",
            cost="250.99",
        ),
    ]


async def test_returns_services_from_crm_as_expected_if_no_promoted_services(
    service_provider, dm
):
    dm.fetch_org_promoted_services.coro.return_value = []

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert result == [
        dict(
            type=ServiceItemType.MARKET,
            id=200,
            name="Стрижка",
            image="https://strizhka.url/pic",
            description="Подстрижём под горшок профессионально",
            categories=["Классно выйдет", "Вообще топовая услуга"],
            min_cost="15.99",
            min_duration=30,
            action_type=ClientAction.LINK,
            service_action_type_settings=dict(
                link=dict(link="https://ya.ru", button_text="ссылка")
            ),
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=300,
            name="Стрижка собаки",
            description="Ты чо пёс?",
            categories=["Вообще топовая услуга", "Популярное"],
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=400,
            name="Стрижка кота",
            categories=["Популярное", "Классно выйдет"],
            cost="25.99",
            duration=None,
        ),
    ]


async def test_returns_loaded_services_from_crm_as_expected_if_no_promoted_services(
    service_provider, dm
):
    dm.fetch_cached_landing_config_feature.coro.side_effect = (
        lambda feature: "enabled"
        if feature == Feature.USE_LOADED_MARKET_INT_DATA
        else None
    )

    dm.fetch_org_market_int_services.coro.return_value = [
        {
            "id": 300,
            "name": "Стрижка собаки",
            "description": "Tы чо пёс?",
            "categories": ["Вообще топовая услуга", "Популярное"],
            "min_cost": Decimal(100),
            "min_duration": Decimal(60),
            "action_type": "action",
            "image": "http://images,com/11",
            "type": ServiceItemType.MARKET,
        },
        {
            "id": 400,
            "name": "Стрижка кота",
            "categories": ["Популярное", "Классно выйдет"],
            "min_cost": Decimal(25.99),
            "type": ServiceItemType.MARKET,
        },
    ]

    dm.fetch_org_promoted_services.coro.return_value = []

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert result == [
        dict(
            type=ServiceItemType.MARKET,
            id=300,
            name="Стрижка собаки",
            description="Tы чо пёс?",
            categories=["Вообще топовая услуга", "Популярное"],
            min_cost=Decimal(100),
            min_duration=Decimal(60),
            action_type="action",
            image="http://images,com/11",
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=400,
            name="Стрижка кота",
            categories=["Популярное", "Классно выйдет"],
            min_cost=Decimal(25.99),
        ),
    ]


async def test_returns_min_cost_and_min_duration_if_more_than_one_service_price(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = [
        dict(
            item_id=200,
            biz_id=22,
            name="Стрижка",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="https://strizhka.url/pic",
            updated_at="2020-08-20T16:30:00",
            description="Подстрижём под горшок профессионально",
            service_prices=[
                dict(
                    service_id=20,
                    price=dict(value="25.99", currency="RUB"),
                    duration_minutes=45,
                    schedule=dict(schedule_id=12),
                ),
                dict(
                    service_id=30,
                    price=dict(value="26.99", currency="RUB"),
                    duration_minutes=30,
                    schedule=dict(schedule_id=12),
                ),
                dict(
                    service_id=40,
                    price=dict(value="27.99", currency="RUB"),
                    schedule=dict(schedule_id=12),
                ),
            ],
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert len(result) == 1
    service = result[0]
    assert "cost" not in service
    assert service["min_cost"] == "15.99"
    assert "duration" not in service
    assert service["min_duration"] == 30


async def test_returns_min_cost_if_there_are_no_service_prices(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = [
        dict(
            item_id=200,
            biz_id=22,
            name="Стрижка",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="https://strizhka.url/pic",
            updated_at="2020-08-20T16:30:00",
            description="Подстрижём под горшок профессионально",
            service_prices=[],
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert len(result) == 1
    service = result[0]
    assert "cost" not in service
    assert service["min_cost"] == "15.99"


async def test_does_not_return_duration_if_there_are_no_service_prices(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = [
        dict(
            item_id=200,
            biz_id=22,
            name="Стрижка",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="https://strizhka.url/pic",
            updated_at="2020-08-20T16:30:00",
            description="Подстрижём под горшок профессионально",
            service_prices=[],
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert len(result) == 1
    service = result[0]
    assert "duration" not in service
    assert "min_duration" not in service


async def test_returns_cost_and_duration_if_only_one_service_price(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = [
        dict(
            item_id=200,
            biz_id=22,
            name="Стрижка",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="https://strizhka.url/pic",
            updated_at="2020-08-20T16:30:00",
            description="Подстрижём под горшок профессионально",
            service_prices=[
                dict(
                    service_id=20,
                    price=dict(value="25.99", currency="RUB"),
                    duration_minutes=45,
                    schedule=dict(schedule_id=12),
                ),
            ],
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert len(result) == 1
    service = result[0]
    assert "min_cost" not in service
    assert service["cost"] == "25.99"
    assert "min_duration" not in service
    assert service["duration"] == 45


async def test_returns_no_cost_and_duration_if_there_are_no_prices(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = [
        dict(
            item_id=200,
            biz_id=22,
            name="Стрижка",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1200, 1300],
            duration_minutes=60,
            main_image_url_template="https://strizhka.url/pic",
            updated_at="2020-08-20T16:30:00",
            description="Подстрижём под горшок профессионально",
            service_prices=[],
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert len(result) == 1
    service = result[0]
    assert "min_cost" not in service
    assert "cost" not in service
    assert "min_duration" not in service
    assert "duration" not in service


async def test_returns_no_duration_if_there_is_no_duration(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = [
        dict(
            item_id=200,
            biz_id=22,
            name="Стрижка",
            status=ServiceStatus.PUBLISHED,
            category_ids=[1200, 1300],
            min_price=dict(value="15.99", currency="RUB"),
            duration_minutes=60,
            main_image_url_template="https://strizhka.url/pic",
            updated_at="2020-08-20T16:30:00",
            description="Подстрижём под горшок профессионально",
            service_prices=[
                dict(
                    service_id=20,
                    price=dict(value="25.99", currency="RUB"),
                    price_level=dict(level=1),
                ),
            ],
        ),
    ]

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert len(result) == 1
    service = result[0]
    assert "min_cost" not in service
    assert service["cost"] == "25.99"
    assert "min_duration" not in service
    assert service["duration"] is None


async def test_does_not_return_category_name_if_category_not_found(
    service_provider, market_int
):
    market_int.fetch_service_categories.coro.return_value = {
        1300: "Вообще топовая услуга",
        1400: "Популярное",
        1500: "Ещё категория для красоты",
    }

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert result == [
        dict(
            type=ServiceItemType.MARKET,
            id=200,
            name="Стрижка",
            image="https://strizhka.url/pic",
            description="Подстрижём под горшок профессионально",
            categories=["Вообще топовая услуга"],
            min_cost="15.99",
            min_duration=30,
            action_type=ClientAction.LINK,
            service_action_type_settings=dict(
                link=dict(link="https://ya.ru", button_text="ссылка")
            ),
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=300,
            name="Стрижка собаки",
            description="Ты чо пёс?",
            categories=["Вообще топовая услуга", "Популярное"],
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=400,
            name="Стрижка кота",
            categories=["Популярное"],
            cost="25.99",
            duration=None,
        ),
    ]


async def test_returns_empty_list_if_no_services(service_provider, market_int):
    market_int.fetch_services.coro.return_value = []

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert result == []


@pytest.mark.parametrize(
    "exception",
    [
        MarketIntNotFound,
        MarketIntUnknownError,
        MarketIntBadProto,
        MarketIntConflict,
        BaseHttpClientException,
    ],
)
async def test_returns_services_without_categories_if_errored_on_fetching_categories(
    exception, service_provider, market_int
):
    market_int.fetch_service_categories.coro.side_effect = exception

    result = await service_provider.fetch_services_info(biz_id=8612)

    assert result == [
        dict(
            type=ServiceItemType.MARKET,
            id=200,
            name="Стрижка",
            image="https://strizhka.url/pic",
            description="Подстрижём под горшок профессионально",
            categories=[],
            min_cost="15.99",
            min_duration=30,
            action_type=ClientAction.LINK,
            service_action_type_settings=dict(
                link=dict(link="https://ya.ru", button_text="ссылка")
            ),
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=300,
            name="Стрижка собаки",
            description="Ты чо пёс?",
            categories=[],
        ),
        dict(
            type=ServiceItemType.MARKET,
            id=400,
            name="Стрижка кота",
            categories=[],
            cost="25.99",
            duration=None,
        ),
    ]


@pytest.mark.parametrize(
    "exception",
    [
        MarketIntNotFound,
        MarketIntUnknownError,
        MarketIntBadProto,
        MarketIntConflict,
        BaseHttpClientException,
    ],
)
async def test_returns_empty_list_as_services_if_market_errored_on_fetching_services(
    exception, service_provider, market_int
):
    market_int.fetch_services.coro.side_effect = exception

    result = await service_provider.fetch_services_info(biz_id=8612)

    market_int.fetch_service_categories.assert_not_called()
    assert result == []


@pytest.mark.parametrize(
    "exception",
    [
        MarketIntNotFound,
        MarketIntUnknownError,
        MarketIntBadProto,
        MarketIntConflict,
        BaseHttpClientException,
    ],
)
async def test_logs_market_exception_on_fetching_services(
    exception, service_provider, market_int, caplog
):
    market_int.fetch_services.coro.side_effect = exception

    await service_provider.fetch_services_info(biz_id=8612)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert record.msg == "Failed to get services for biz_id 8612"


@pytest.mark.parametrize(
    "exception",
    [
        MarketIntNotFound,
        MarketIntUnknownError,
        MarketIntBadProto,
        MarketIntConflict,
        BaseHttpClientException,
    ],
)
async def test_logs_market_exception_on_fetching_category_names(
    exception, service_provider, market_int, caplog
):
    market_int.fetch_service_categories.coro.side_effect = exception

    await service_provider.fetch_services_info(biz_id=8612)

    assert len(caplog.records) == 1
    record = caplog.records[0]
    assert record.levelno == logging.ERROR
    assert record.msg == "Failed to get service categories for biz_id 8612"


async def test_does_not_call_for_service_category_names_if_no_services(
    service_provider, market_int
):
    market_int.fetch_services.coro.return_value = []

    await service_provider.fetch_services_info(biz_id=8612)

    market_int.fetch_service_categories.assert_not_called()


async def test_calls_for_data_as_expected(service_provider, market_int, dm):
    await service_provider.fetch_services_info(biz_id=8612)

    dm.fetch_org_promoted_services.assert_called_with(biz_id=8612)
    market_int.fetch_services.assert_called_with(
        biz_id=8612, filtering=dict(statuses=[ServiceStatus.PUBLISHED])
    )
    market_int.fetch_service_categories.assert_called_with(biz_id=8612)


async def test_does_not_call_for_crm_data_if_has_promoted_services(
    service_provider, market_int, dm
):
    dm.fetch_org_promoted_services.coro.return_value = [
        dict(name="Стрижка кота", image="https://strizhka.url/cat", cost="15.99"),
        dict(name="Стрижка барашка", cost="250.99"),
    ]

    await service_provider.fetch_services_info(biz_id=8612)

    dm.fetch_org_promoted_services.assert_called_with(biz_id=8612)
    market_int.fetch_services.assert_not_called()
    market_int.fetch_service_categories.assert_not_called()


async def test_calls_for_data_as_expected_with_disabled_promoted_services(
    market_int, dm
):
    service_provider = ServiceProvider(
        dm=dm, market_client=market_int, disable_promoted_services=True
    )

    await service_provider.fetch_services_info(biz_id=8612)

    dm.fetch_org_promoted_services.assert_not_called()
    market_int.fetch_services.assert_called_with(
        biz_id=8612, filtering=dict(statuses=[ServiceStatus.PUBLISHED])
    )
    market_int.fetch_service_categories.assert_called_with(biz_id=8612)
