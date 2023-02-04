import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


async def test_returns_order_details(factory, dm):
    order_id = await factory.create_order(yang_suite_id="111", task_result_got_at=None)

    got = await dm.retrieve_order_by_suite("111")

    assert got == dict(
        id=order_id,
        permalink=12345,
        reservation_datetime=dt("2020-01-01 18:00:00"),
        reservation_timezone="Europe/Moscow",
        person_count=3,
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        comment="Столик у окна",
        call_agreement_accepted=True,
        yang_suite_id="111",
        client_id=777,
        biz_id=888,
        time_to_call=dt("2020-01-01 16:00:00"),
        created_at=dt("2019-01-01 00:00:00"),
        yang_task_created_at=dt("2019-01-01 23:00:00"),
        task_created_at=dt("2019-01-02 00:00:00"),
        task_result_got_at=None,
        sms_sent_at=dt("2019-01-04 00:00:00"),
        sent_result_event_at=dt("2019-01-08 00:00:00"),
        exported_as_created_at=dt("2019-01-05 00:00:00"),
        exported_as_processed_at=dt("2019-01-06 00:00:00"),
        exported_as_notified_at=dt("2019-01-07 00:00:00"),
        booking_verdict="booked",
        booking_meta=None,
        customer_passport_uid=7654332,
    )


async def test_returns_nothing_for_unknown_suite(factory, dm):
    await factory.create_order(yang_suite_id="111")

    got = await dm.retrieve_order_by_suite("999")

    assert got is None


async def test_returns_nothing_for_already_processed_order(factory, dm):
    await factory.create_order(
        yang_suite_id="111", task_result_got_at=dt("2019-01-03 00:00:00")
    )

    got = await dm.retrieve_order_by_suite("111")

    assert got is None
