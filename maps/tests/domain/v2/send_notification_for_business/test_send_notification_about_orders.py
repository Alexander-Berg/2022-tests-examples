from datetime import timedelta

import pytest

from maps_adv.common.helpers import dt
from maps_adv.geosmb.doorman.client import NotFound
from maps_adv.geosmb.telegraphist.server.lib.enums import NotificationType, Transport
from maps_adv.geosmb.telegraphist.server.tests.helpers import (
    make_order_for_business_notification,
    make_order_item,
)

pytestmark = [pytest.mark.asyncio]


def make_input_params(**updates):
    input_params = dict(
        recipient=dict(
            biz_id=15,
            cabinet_link="http://cabinet.link",
            company_link="http://company.link",
            email="passed_email@yandex.ru",
        ),
        transports=[Transport.EMAIL],
    )
    input_params.update(updates)

    return input_params


async def test_uses_clients_to_fetch_client_data(domain, bvm, doorman):
    await domain.send_notification_for_business(
        **make_input_params(
            order_created=dict(
                client=dict(client_id=160),
                details_link="http://details.link",
                order=make_order_for_business_notification(),
            ),
        )
    )

    doorman.retrieve_client.assert_called_with(biz_id=15, client_id=160)


@pytest.mark.parametrize(
    ("notification_type", "expected_notification_type"),
    [
        ("order_created", NotificationType.ORDER_CREATED_FOR_BUSINESS),
        ("order_changed", NotificationType.ORDER_CHANGED_FOR_BUSINESS),
        ("order_cancelled", NotificationType.ORDER_CANCELLED_FOR_BUSINESS),
    ],
)
async def test_uses_notification_router(
    notification_router,
    domain,
    notification_type,
    expected_notification_type,
):
    expected_notification_details = {
        "client": {
            "name": "client_first_name client_last_name",
            "phone": 1234567890123,
        },
        "org": {
            "formatted_address": "Город, Улица, 1",
            "cabinet_link": "http://cabinet.link",
            "company_link": "http://company.link",
            "tz_offset": timedelta(seconds=10800),
        },
        "order": make_order_for_business_notification(),
        "details_link": "http://details.link",
    }

    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                )
            },
        )
    )

    notification_router.send_business_notification.assert_called_with(
        recipient=dict(biz_id=15, email="passed_email@yandex.ru"),
        transports=[Transport.EMAIL],
        notification_type=expected_notification_type,
        notification_details=expected_notification_details,
    )


@pytest.mark.parametrize(
    "doorman_client_data, expected_call_client_data",
    [
        (
            dict(first_name=None, last_name="Иванов", phone=88002000600),
            dict(name="Иванов", phone=88002000600),
        ),
        (
            dict(first_name="Васятка", last_name=None, phone=88002000600),
            dict(name="Васятка", phone=88002000600),
        ),
        (
            dict(first_name="Васятка", last_name="Иванов", phone=None),
            dict(name="Васятка Иванов", phone=None),
        ),
    ],
)
async def test_composes_client_data_correctly(
    doorman_client_data,
    expected_call_client_data,
    notification_router,
    domain,
    doorman,
):
    doorman.retrieve_client.coro.return_value = doorman_client_data

    await domain.send_notification_for_business(
        **make_input_params(
            order_created=dict(
                client=dict(client_id=160),
                details_link="http://details.link",
                order=make_order_for_business_notification(),
            ),
        )
    )

    call_kwargs = notification_router.send_business_notification.call_args.kwargs
    assert call_kwargs["notification_details"]["client"] == expected_call_client_data


@pytest.mark.parametrize(
    "notification_type", ["order_created", "order_changed", "order_cancelled"]
)
async def test_sorts_order_items_by_timestamp(
    notification_router,
    notification_type,
    domain,
    doorman,
):
    await domain.send_notification_for_business(
        **make_input_params(
            **{
                notification_type: dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(
                        items=[
                            make_order_item(
                                name="Стрижка усов",
                                booking_timestamp=dt("2020-10-01 16:00:00"),
                                employee_name="Грустный Саня",
                            ),
                            make_order_item(
                                name="Стрижка бровей",
                                booking_timestamp=dt("2020-09-01 16:00:00"),
                                employee_name="Грустный Петя",
                            ),
                        ],
                    ),
                )
            },
        )
    )

    call_kwargs = notification_router.send_business_notification.call_args.kwargs
    assert call_kwargs["notification_details"]["order"]["items"] == [
        make_order_item(
            name="Стрижка бровей",
            booking_timestamp=dt("2020-09-01 16:00:00"),
            employee_name="Грустный Петя",
        ),
        make_order_item(
            name="Стрижка усов",
            booking_timestamp=dt("2020-10-01 16:00:00"),
            employee_name="Грустный Саня",
        ),
    ]


async def test_raises_if_client_not_found_in_doorman(domain, doorman):
    doorman.retrieve_client.coro.side_effect = NotFound

    with pytest.raises(NotFound):
        await domain.send_notification_for_business(
            **make_input_params(
                order_created=dict(
                    client=dict(client_id=160),
                    details_link="http://details.link",
                    order=make_order_for_business_notification(),
                ),
            )
        )
