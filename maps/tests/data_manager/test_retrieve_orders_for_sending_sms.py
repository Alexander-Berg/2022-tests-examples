import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_processed_orders_with_no_sms_sent(factory, dm):
    order_id_1 = await factory.create_order(
        reservation_datetime=dt("2020-01-01 18:00:00"),
        reservation_timezone="Europe/Moscow",
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        booking_verdict="booked",
        booking_meta={"org_phone": "+7 (000) 000-00-01", "org_name": "Кафе"},
        yang_suite_id="111",
        task_result_got_at=dt("2020-01-01 00:00:00"),
        sms_sent_at=None,
        created_at=dt("2019-01-01 00:00:00"),
    )
    order_id_2 = await factory.create_order(
        reservation_datetime=dt("2020-02-02 22:22:22"),
        reservation_timezone="Europe/Minsk",
        customer_name="Сидоров",
        customer_phone="+7 (222) 000-00-00",
        booking_verdict="no_place",
        booking_meta={"org_phone": "+7 (222) 000-00-02", "org_name": "Кафе 2"},
        yang_suite_id="222",
        task_result_got_at=dt("2020-01-01 00:00:00"),
        sms_sent_at=None,
        created_at=dt("2019-01-02 00:00:00"),
    )

    got = await dm.retrieve_orders_for_sending_sms()

    assert got == [
        dict(
            id=order_id_1,
            reservation_datetime=dt("2020-01-01 18:00:00"),
            reservation_timezone="Europe/Moscow",
            customer_name="Иван Петров",
            customer_phone="+7 (000) 000-00-00",
            booking_verdict="booked",
            booking_meta={"org_phone": "+7 (000) 000-00-01", "org_name": "Кафе"},
        ),
        dict(
            id=order_id_2,
            reservation_datetime=dt("2020-02-02 22:22:22"),
            reservation_timezone="Europe/Minsk",
            customer_name="Сидоров",
            customer_phone="+7 (222) 000-00-00",
            booking_verdict="no_place",
            booking_meta={"org_phone": "+7 (222) 000-00-02", "org_name": "Кафе 2"},
        ),
    ]


@pytest.mark.parametrize(
    "order_params",
    [
        # sms has sent
        dict(
            task_result_got_at=dt("2020-01-01 00:00:00"),
            sms_sent_at=dt("2020-01-01 00:00:00"),
        ),
        # not processed
        dict(task_result_got_at=None, sms_sent_at=None),
    ],
)
async def test_does_not_returns_unfit_order(factory, dm, order_params):
    await factory.create_order(**order_params)

    got = await dm.retrieve_orders_for_sending_sms()

    assert got == []


async def test_returns_orders_sorted_by_creation_time(factory, dm):
    order_id_1 = await factory.create_order(
        yang_suite_id=None,
        task_result_got_at=dt("2020-01-01 00:00:00"),
        sms_sent_at=None,
        created_at=dt("2020-01-02 00:00:00"),
    )
    order_id_2 = await factory.create_order(
        yang_suite_id=None,
        task_result_got_at=dt("2020-01-01 00:00:00"),
        sms_sent_at=None,
        created_at=dt("2020-01-01 00:00:00"),
    )
    order_id_3 = await factory.create_order(
        yang_suite_id=None,
        task_result_got_at=dt("2020-01-01 00:00:00"),
        sms_sent_at=None,
        created_at=dt("2020-01-03 00:00:00"),
    )

    got = await dm.retrieve_orders_for_sending_sms()

    assert [order["id"] for order in got] == [order_id_2, order_id_1, order_id_3]
