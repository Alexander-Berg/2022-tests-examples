from datetime import datetime

import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.booking_yang.server.lib.data_managers.orders import NaiveDateTime

pytestmark = [pytest.mark.asyncio]

creation_data = {
    "permalink": 12345,
    "reservation_datetime": dt("2020-01-01 18:00:00"),
    "reservation_timezone": "Europe/Moscow",
    "person_count": 3,
    "customer_name": "Иван Петров",
    "customer_phone": "+7 (000) 000-00-00",
    "customer_passport_uid": 4567892,
    "comment": "Столик у окна",
    "call_agreement_accepted": True,
    "biz_id": 123,
    "time_to_call": dt("2020-01-02 00:00:00"),
    "created_at": dt("2020-11-11 11:11:11"),
}


@pytest.mark.parametrize("time_to_call", [None, dt("2020-01-02 00:00:00")])
@pytest.mark.parametrize("customer_passport_uid", [None, 4567892])
async def test_creates_order(customer_passport_uid, time_to_call, dm, con):
    data = creation_data.copy()
    data["time_to_call"] = time_to_call
    data["customer_passport_uid"] = customer_passport_uid

    await dm.create_order(**data)

    got = dict(await con.fetchrow("SELECT * FROM orders"))

    assert got == dict(
        id=Any(int),
        permalink=12345,
        reservation_datetime=dt("2020-01-01 18:00:00"),
        reservation_timezone="Europe/Moscow",
        person_count=3,
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        customer_passport_uid=customer_passport_uid,
        comment="Столик у окна",
        call_agreement_accepted=True,
        created_at=dt("2020-11-11 11:11:11"),
        yang_task_created_at=None,
        task_created_at=None,
        task_result_got_at=None,
        sms_sent_at=None,
        exported_as_created_at=None,
        exported_as_processed_at=None,
        exported_as_notified_at=None,
        sent_result_event_at=None,
        biz_id=123,
        time_to_call=time_to_call,
        yang_suite_id=None,
        client_id=None,
        booking_verdict=None,
        booking_meta=None,
    )


async def test_returns_order_id(dm, con):
    order_id = await dm.create_order(**creation_data)

    sql = "SELECT EXISTS(SELECT * FROM orders WHERE id = $1)"
    assert await con.fetchval(sql, order_id) is True


async def test_raises_for_naive_datetime_param(dm):
    creation_params = creation_data.copy()
    creation_params["reservation_datetime"] = datetime(2020, 1, 1)

    with pytest.raises(
        NaiveDateTime,
        match="reservation_datetime must be aware with timezone: 2020-01-01 00:00:00.",
    ):
        await dm.create_order(**creation_params)
