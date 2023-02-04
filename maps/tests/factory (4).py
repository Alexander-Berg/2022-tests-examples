from datetime import datetime
from typing import Optional

import pytest
from asyncpg import Connection
from smb.common.testing_utils import dt

_default_reservation_dt = dt("2020-01-01 18:00:00")
_default_time_to_call = dt("2020-01-01 16:00:00")
_default_order_created_at = dt("2019-01-01 00:00:00")
_default_yang_task_created_at = dt("2019-01-01 23:00:00")
_default_task_created_at = dt("2019-01-02 00:00:00")
_default_task_result_got_at = dt("2019-01-03 00:00:00")
_default_sms_sent_at = dt("2019-01-04 00:00:00")
_default_yt_created_orders_exported_at = dt("2019-01-05 00:00:00")
_default_yt_processed_orders_exported_at = dt("2019-01-06 00:00:00")
_default_yt_sms_sent_exported_at = dt("2019-01-07 00:00:00")
_default_sent_result_event_at = dt("2019-01-08 00:00:00")


class Factory:
    _con: Connection

    def __init__(self, con: Connection):
        self._con = con

    async def create_order(
        self,
        order_id: Optional[int] = None,
        permalink: int = 12345,
        reservation_datetime: datetime = _default_reservation_dt,
        reservation_timezone: str = "Europe/Moscow",
        person_count: int = 3,
        customer_name: str = "Иван Петров",
        customer_phone: str = "+7 (000) 000-00-00",
        comment: str = "Столик у окна",
        call_agreement_accepted: bool = True,
        yang_suite_id: Optional[str] = "6789",
        client_id: Optional[int] = 777,
        biz_id: Optional[int] = 888,
        time_to_call: Optional[datetime] = _default_time_to_call,
        yang_task_created_at: Optional[datetime] = _default_yang_task_created_at,
        task_created_at: Optional[datetime] = _default_task_created_at,
        task_result_got_at: Optional[datetime] = _default_task_result_got_at,
        sms_sent_at: Optional[datetime] = _default_sms_sent_at,
        exported_as_created_at: Optional[
            datetime
        ] = _default_yt_created_orders_exported_at,
        exported_as_processed_at: Optional[
            datetime
        ] = _default_yt_processed_orders_exported_at,
        sent_result_event_at: Optional[datetime] = _default_sent_result_event_at,
        exported_as_notified_at: Optional[datetime] = _default_yt_sms_sent_exported_at,
        booking_verdict: Optional[str] = "booked",
        booking_meta: Optional[dict] = None,
        created_at: datetime = _default_order_created_at,
        customer_passport_uid: Optional[int] = 7654332,
    ) -> int:
        sql = """
            INSERT INTO orders (
                id, permalink, reservation_datetime, reservation_timezone, person_count,
                customer_name, customer_phone, customer_passport_uid, comment,
                call_agreement_accepted, yang_suite_id, client_id, biz_id, time_to_call,
                yang_task_created_at, task_created_at, task_result_got_at, sms_sent_at,
                sent_result_event_at, exported_as_created_at, exported_as_processed_at,
                exported_as_notified_at, booking_verdict, booking_meta, created_at
            )
            VALUES (
                coalesce($1, nextval('orders_id_seq')), $2, $3, $4, $5,
                $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18,
                $19, $20, $21, $22, $23, $24, $25
            )
            RETURNING (id)
        """

        return await self._con.fetchval(
            sql,
            order_id,
            permalink,
            reservation_datetime,
            reservation_timezone,
            person_count,
            customer_name,
            customer_phone,
            customer_passport_uid,
            comment,
            call_agreement_accepted,
            yang_suite_id,
            client_id,
            biz_id,
            time_to_call,
            yang_task_created_at,
            task_created_at,
            task_result_got_at,
            sms_sent_at,
            sent_result_event_at,
            exported_as_created_at,
            exported_as_processed_at,
            exported_as_notified_at,
            booking_verdict,
            booking_meta,
            created_at,
        )

    async def create_empty_order(
        self,
        permalink: int = 12345,
        reservation_datetime: datetime = _default_reservation_dt,
        reservation_timezone: str = "Europe/Moscow",
        person_count: int = 3,
        customer_name: str = "Иван Петров",
        customer_phone: str = "+7 (000) 000-00-00",
        comment: str = "Столик у окна",
        call_agreement_accepted: bool = True,
        yang_suite_id: Optional[str] = None,
        client_id: Optional[int] = None,
        biz_id: Optional[int] = None,
        time_to_call=None,
        yang_task_created_at: Optional[datetime] = None,
        task_created_at: Optional[datetime] = None,
        task_result_got_at: Optional[datetime] = None,
        sms_sent_at: Optional[datetime] = None,
        sent_result_event_at: Optional[datetime] = None,
        exported_as_created_at: Optional[datetime] = None,
        exported_as_processed_at: Optional[datetime] = None,
        exported_as_notified_at: Optional[datetime] = None,
        booking_verdict: Optional[str] = None,
        booking_meta: Optional[dict] = None,
        created_at: datetime = _default_order_created_at,
        customer_passport_uid: Optional[int] = 7654332,
    ) -> int:
        return await self.create_order(
            permalink=permalink,
            reservation_datetime=reservation_datetime,
            reservation_timezone=reservation_timezone,
            person_count=person_count,
            customer_name=customer_name,
            customer_phone=customer_phone,
            comment=comment,
            call_agreement_accepted=call_agreement_accepted,
            yang_task_created_at=yang_task_created_at,
            task_created_at=task_created_at,
            task_result_got_at=task_result_got_at,
            sms_sent_at=sms_sent_at,
            sent_result_event_at=sent_result_event_at,
            exported_as_created_at=exported_as_created_at,
            exported_as_processed_at=exported_as_processed_at,
            exported_as_notified_at=exported_as_notified_at,
            yang_suite_id=yang_suite_id,
            client_id=client_id,
            biz_id=biz_id,
            time_to_call=time_to_call,
            booking_verdict=booking_verdict,
            booking_meta=booking_meta,
            created_at=created_at,
            customer_passport_uid=customer_passport_uid,
        )

    async def retrieve_order(self, order_id: int) -> dict:
        sql = """
            SELECT *
            FROM orders
            WHERE id=$1
        """
        row = await self._con.fetchrow(sql, order_id)
        return dict(row) if row else None


@pytest.fixture
async def factory(con):
    return Factory(con)
