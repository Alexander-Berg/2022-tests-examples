import pytest

from maps_adv.geosmb.clients.market import ServiceStatus
from maps_adv.geosmb.landlord.proto import (
    common_pb2,
    organization_details_pb2,
    services_pb2,
)

pytestmark = [pytest.mark.asyncio]

URL = "/external/fetch_landing_data/"


async def test_returns_promoted_services_if_exists(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    await factory.create_promoted_service_list(biz_id=22)
    for idx in (4, 8, 15, 16, 23, 42):
        await factory.create_promoted_service(
            biz_id=22,
            service_id=idx,
            title=f"Стрижечка_{idx}",
            cost=str(idx),
            image=f"http://image.link/{idx}",
            url=f"http://url.com/{idx}",
            description=f"desc_{idx}",
        )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.services == services_pb2.Services(
        items=[
            services_pb2.ServiceItem(
                name=f"Стрижечка_{idx}",
                image=common_pb2.ImageTemplate(template_url=f"http://image.link/{idx}"),
                cost=common_pb2.Decimal(value=str(idx)),
                type=services_pb2.ServiceItem.ServiceItemType.PROMOTED,
                id=idx,
                url=f"http://url.com/{idx}",
                description=f"desc_{idx}",
            )
            for idx in (4, 8, 15, 16, 23, 42)
        ]
    )


async def test_returns_big_prices_correctly(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    await factory.create_promoted_service_list(biz_id=22)
    await factory.create_promoted_service(
        biz_id=22,
        service_id=8,
        title="Стрижечка",
        cost=str(2_000_000_000),
        image="http://image.link/",
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.services.items[0].cost == common_pb2.Decimal(value=str("2000000000"))


async def test_returns_crm_services_as_expected_if_no_promoted_services(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.services == services_pb2.Services(
        items=[
            # with optional fields
            services_pb2.ServiceItem(
                name="Стрижка",
                image=common_pb2.ImageTemplate(template_url="https://strizhka.url/pic"),
                description="Подстрижём под горшок профессионально",
                categories=["Классно выйдет", "Вообще топовая услуга"],
                min_cost=common_pb2.Decimal(value="15.99"),
                type=services_pb2.ServiceItem.ServiceItemType.MARKET,
                id=200,
                action_type=services_pb2.ServiceItem.ServiceActionType.LINK,
                min_duration=30,
                service_action_type_settings=dict(
                    link=dict(
                        link="https://ya.ru",
                        button_text="ссылка"
                    )
                ),
            ),
            services_pb2.ServiceItem(
                name="Стрижка собаки",
                description="Ты чо пёс?",
                categories=["Вообще топовая услуга", "Популярное"],
                type=services_pb2.ServiceItem.ServiceItemType.MARKET,
                id=300,
            ),
            # without optional fields
            services_pb2.ServiceItem(
                name="Стрижка кота",
                categories=["Популярное", "Классно выйдет"],
                cost=common_pb2.Decimal(value="25.99"),
                type=services_pb2.ServiceItem.ServiceItemType.MARKET,
                id=400,
            ),
        ]
    )


async def test_returns_min_cost_if_more_than_one_service_price(
    api, factory, market_int
):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
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

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    service = got.services.items[0]
    assert not service.HasField("cost")
    assert service.min_cost == common_pb2.Decimal(value="15.99")


async def test_returns_cost_if_only_one_service_price(api, factory, market_int):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
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

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    service = got.services.items[0]
    assert not service.HasField("min_cost")
    assert service.cost == common_pb2.Decimal(value="25.99")


async def test_does_not_return_services_if_no_servises(api, factory, market_int):
    market_int.fetch_services.coro.return_value = []
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert not got.HasField("services")


@pytest.mark.config(DISABLE_PROMOTED_SERVICES=True)
async def test_does_not_return_promoted_services_with_flag(api, factory):
    data_id = await factory.insert_landing_data()
    await factory.insert_biz_state(
        biz_id=22, slug="cafe", published=True, stable_version=data_id
    )
    await factory.create_promoted_service_list(biz_id=22)
    await factory.create_promoted_service(
        biz_id=22,
        service_id=8,
        title="Стрижечка",
        cost=str(2_000_000_000),
        image="http://image.link/",
    )

    got = await api.post(
        URL,
        proto=organization_details_pb2.OrganizationDetailsInput(
            slug="cafe",
            token="fetch_data_token",
        ),
        decode_as=organization_details_pb2.OrganizationDetails,
        expected_status=200,
    )

    assert got.services == services_pb2.Services(
        items=[
            # with optional fields
            services_pb2.ServiceItem(
                name="Стрижка",
                image=common_pb2.ImageTemplate(template_url="https://strizhka.url/pic"),
                description="Подстрижём под горшок профессионально",
                categories=["Классно выйдет", "Вообще топовая услуга"],
                min_cost=common_pb2.Decimal(value="15.99"),
                type=services_pb2.ServiceItem.ServiceItemType.MARKET,
                id=200,
                action_type=services_pb2.ServiceItem.ServiceActionType.LINK,
                min_duration=30,
                service_action_type_settings=dict(
                    link=dict(
                        link="https://ya.ru",
                        button_text="ссылка"
                    )
                ),
            ),
            services_pb2.ServiceItem(
                name="Стрижка собаки",
                description="Ты чо пёс?",
                categories=["Вообще топовая услуга", "Популярное"],
                type=services_pb2.ServiceItem.ServiceItemType.MARKET,
                id=300,
            ),
            # without optional fields
            services_pb2.ServiceItem(
                name="Стрижка кота",
                categories=["Популярное", "Классно выйдет"],
                cost=common_pb2.Decimal(value="25.99"),
                type=services_pb2.ServiceItem.ServiceItemType.MARKET,
                id=400,
            ),
        ]
    )
