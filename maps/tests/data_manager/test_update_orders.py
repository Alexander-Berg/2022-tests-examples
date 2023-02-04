import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.booking_yang.server.lib.data_managers.orders import UnknownDbFields

pytestmark = [pytest.mark.asyncio]


def make_order_1_data() -> dict:
    return dict(
        permalink=11111,
        reservation_datetime=dt("2020-01-01 01:10:10"),
        reservation_timezone="Europe/Moscow",
        person_count=1,
        customer_name="Иван Петров 1",
        customer_phone="+7 (111) 111-11-11",
        comment="Столик у окна 1",
        call_agreement_accepted=True,
        yang_suite_id="1111111111",
        client_id=11,
        biz_id=111,
        time_to_call=dt("2020-01-01 00:10:10"),
        yang_task_created_at=None,
        task_created_at=dt("2011-01-01 00:00:00"),
        task_result_got_at=dt("2011-01-02 00:00:00"),
        sms_sent_at=dt("2011-01-03 00:00:00"),
        sent_result_event_at=dt("2011-01-08 00:00:00"),
        exported_as_created_at=dt("2011-01-04 00:00:00"),
        exported_as_processed_at=dt("2011-01-05 00:00:00"),
        exported_as_notified_at=dt("2011-01-06 00:00:00"),
        booking_verdict="booked",
        booking_meta={"key1": "value1"},
        created_at=dt("2011-01-05 00:00:00"),
        customer_passport_uid=7654332,
    )


def make_order_2_data() -> dict:
    return dict(
        permalink=22222,
        reservation_datetime=dt("2020-02-02 02:20:20"),
        reservation_timezone="Europe/Minsk",
        person_count=2,
        customer_name="Иван Петров 2",
        customer_phone="+7 (222) 222-22-22",
        comment="Столик у окна 2",
        call_agreement_accepted=True,
        yang_suite_id="222222222",
        client_id=22,
        biz_id=222,
        time_to_call=dt("2020-02-02 00:20:20"),
        yang_task_created_at=None,
        task_created_at=dt("2012-01-01 00:00:00"),
        task_result_got_at=dt("2012-01-02 00:00:00"),
        sms_sent_at=dt("2012-01-03 00:00:00"),
        sent_result_event_at=dt("2012-01-08 00:00:00"),
        exported_as_created_at=dt("2012-01-04 00:00:00"),
        exported_as_processed_at=dt("2012-01-05 00:00:00"),
        exported_as_notified_at=dt("2012-01-06 00:00:00"),
        booking_verdict="no_place",
        booking_meta={"key2": "value2"},
        created_at=dt("2012-01-05 00:00:00"),
        customer_passport_uid=1243256,
    )


def update_data(data: dict, **overrides) -> dict:
    data.update(**overrides)
    return data


@pytest.mark.parametrize(
    "updated_fields",
    [
        {"permalink": 999},
        {"reservation_datetime": dt("2019-01-01 18:00:00")},
        {"reservation_timezone": "Africa/Bamako"},
        {"person_count": 1},
        {"customer_name": "Сидоров"},
        {"customer_phone": "+7 (999) 999-99-99"},
        {"comment": "new comment"},
        {"call_agreement_accepted": False},
        {"yang_task_created_at": dt("2019-12-13 12:12:12")},
        {"task_created_at": dt("2019-12-12 12:12:12")},
        {"task_result_got_at": dt("2019-12-12 12:12:12")},
        {"sms_sent_at": dt("2019-12-12 12:12:12")},
        {"exported_as_created_at": dt("2019-12-12 12:12:12")},
        {"exported_as_processed_at": dt("2019-12-12 12:12:12")},
        {"exported_as_notified_at": dt("2019-12-12 12:12:12")},
        {"client_id": 999},
        {"biz_id": 999},
        {"booking_meta": {"org_phone": "+7 (999) 999-99-99", "org_name": "ООО Гараж"}},
        # update to None works
        {"yang_suite_id": None},
        {"client_id": None},
        {"biz_id": None},
        {"task_created_at": None},
        {"task_result_got_at": None},
        {"sms_sent_at": None},
        {"exported_as_created_at": None},
        {"exported_as_processed_at": None},
        {"exported_as_notified_at": None},
        {"booking_verdict": None},
        {"booking_meta": None},
        {"customer_passport_uid": 876543},
        # multiple fields
        {"permalink": 999, "person_count": 1},
    ],
)
async def test_updates_only_requested_fields(factory, dm, updated_fields, con):
    order_data_1 = make_order_1_data()
    order_data_2 = make_order_2_data()
    order_id_1 = await factory.create_order(**order_data_1)
    order_id_2 = await factory.create_order(**order_data_2)

    await dm.update_orders(order_ids=[order_id_1, order_id_2], **updated_fields)

    rows = await con.fetch("SELECT * FROM orders ORDER BY created_at")
    assert [dict(row) for row in rows] == [
        update_data(order_data_1, id=order_id_1, **updated_fields),
        update_data(order_data_2, id=order_id_2, **updated_fields),
    ]


async def test_raises_for_unknown_fields(factory, dm):
    with pytest.raises(UnknownDbFields) as exc:
        await dm.update_orders(order_ids=[], bad_field=132)

    assert exc.value.args == ("Unknown DB fields: {'bad_field'}",)


async def test_does_nothing_for_unknown_order(factory, dm, con):
    order_data = make_order_1_data()
    order_id = await factory.create_order(**order_data)

    await dm.update_orders(order_ids=[999], yang_suite_id="12345")

    assert await con.fetchval("SELECT COUNT(*) FROM orders") == 1
    assert await factory.retrieve_order(order_id) == update_data(
        order_data, id=order_id
    )


async def test_does_nothing_if_nothing_to_update(factory, dm, con):
    order_data = make_order_1_data()
    order_id = await factory.create_order(**order_data)

    await dm.update_orders(order_ids=[order_id])

    assert await con.fetchval("SELECT COUNT(*) FROM orders") == 1
    assert await factory.retrieve_order(order_id) == update_data(
        order_data, id=order_id
    )
