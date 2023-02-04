from datetime import datetime, timezone
from unittest import mock

import pytest
from smb.common.http_client import BaseHttpClientException
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.booking_yang.server.lib import OrdersDomain

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

list_orders_data = [
    dict(
        id=111,
        reservation_datetime=dt("2020-01-01 18:00:00"),
        reservation_timezone="Europe/Moscow",
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        booking_verdict="booked",
        booking_meta={"org_name": "Кафе", "org_phone": "+7 (000) 000-00-01"},
    ),
    dict(
        id=222,
        reservation_datetime=dt("2020-02-02 22:22:22"),
        reservation_timezone="Europe/Minsk",
        customer_name="Сидоров",
        customer_phone="+7 (222) 000-00-00",
        booking_verdict="no_place",
        booking_meta={"org_name": "Кафе 2", "org_phone": "+7 (222) 000-00-02"},
    ),
]


def make_order_data(**overrides) -> dict:
    order = dict(
        id=111,
        reservation_datetime=dt("2020-11-11 07:00:00"),
        reservation_timezone="Europe/Moscow",
        customer_name="Иван Петров",
        customer_phone="+7 (000) 000-00-00",
        booking_verdict="booked",
        booking_meta={"org_name": "Кафе", "org_phone": "+7 (000) 000-00-01"},
    )
    order.update(**overrides)

    return order


async def test_sends_sms_for_all_orders(domain, dm, yasms):
    dm.retrieve_orders_for_sending_sms.coro.return_value = list_orders_data

    await domain.notify_about_processed_tasks()

    yasms.send_sms.assert_has_calls(
        [
            mock.call(phone=70000000000, text=Any(str)),
            mock.call(phone=72220000000, text=Any(str)),
        ]
    )


@pytest.mark.freeze_time
async def test_mark_all_orders_as_sent_sms(domain, dm, yasms):
    dm.retrieve_orders_for_sending_sms.coro.return_value = list_orders_data

    await domain.notify_about_processed_tasks()

    dm.update_orders.assert_has_calls(
        [
            mock.call(order_ids=[111], sms_sent_at=datetime.now(timezone.utc)),
            mock.call(order_ids=[222], sms_sent_at=datetime.now(timezone.utc)),
        ]
    )


@pytest.mark.freeze_time
async def test_processes_other_orders_if_yasms_raises(domain, dm, yasms):
    dm.retrieve_orders_for_sending_sms.coro.return_value = list_orders_data
    yasms.send_sms.coro.side_effect = (BaseHttpClientException(), None)

    await domain.notify_about_processed_tasks()

    dm.update_orders.assert_called_once_with(
        order_ids=[222], sms_sent_at=datetime.now(timezone.utc)
    )


async def test_does_not_process_other_orders_if_yasms_raises_unknown_error(
    domain, dm, yasms
):
    dm.retrieve_orders_for_sending_sms.coro.return_value = list_orders_data
    yasms.send_sms.coro.side_effect = Exception()

    with pytest.raises(Exception):
        await domain.notify_about_processed_tasks()

    yasms.send_sms.assert_called_once_with(phone=70000000000, text=Any(str))
    dm.update_orders.assert_not_called()


async def test_does_nothing_if_no_orders(domain, dm, yasms):
    dm.retrieve_orders_for_sending_sms.coro.return_value = []

    await domain.notify_about_processed_tasks()

    yasms.send_sms.assert_not_called()
    dm.update_orders.assert_not_called()


@pytest.mark.freeze_time
async def test_marks_order_as_sms_sent_if_yasms_client_not_exists(
    domain, dm, geoproduct, geosearch, yang, doorman
):
    dm.retrieve_orders_for_sending_sms.coro.return_value = list_orders_data
    domain = OrdersDomain(
        dm=dm,
        geoproduct=geoproduct,
        geosearch=geosearch,
        yang=yang,
        yasms=None,
        doorman=doorman,
        new_yang_format=False,
        disconnect_orgs=False,
    )

    await domain.notify_about_processed_tasks()

    dm.update_orders.assert_has_calls(
        [
            mock.call(order_ids=[111], sms_sent_at=datetime.now(timezone.utc)),
            mock.call(order_ids=[222], sms_sent_at=datetime.now(timezone.utc)),
        ]
    )


@pytest.mark.parametrize(
    "booking_verdict, expected_sms",
    [
        (
            "booked",
            "Подтверждена запись в «Кафе» на 10:00 11.11.2020 на имя "
            "Иван Петров. Если захотите отменить, позвоните им: +7 (000) 000-00-01",
        ),
        (
            "no_place_contacts_transfered",
            "Привет! Не получилось записать вас в «Кафе» на 10:00 11.11.2020 "
            "— на это время нет мест. Не переживайте — они сами вам "
            "позвонят и предложат другие варианты. Не хотите ждать? Позвоните им: "
            "+7 (000) 000-00-01",
        ),
        (
            "no_place",
            "Привет! Не получилось записать вас в «Кафе» на 10:00 11.11.2020 "
            "— на это время нет мест. Чтобы записаться на другое время, "
            "позвоните им: +7 (000) 000-00-01",
        ),
        (
            "not_enough_information_contacts_transfered",
            "Привет! Не получилось записать вас в «Кафе» на 10:00 11.11.2020. "
            "Не переживайте — они сами вам позвонят и запишут. "
            "Не хотите ждать? Позвоните им: +7 (000) 000-00-01",
        ),
        (
            "not_enough_information",
            "Привет! Не получилось записать вас в «Кафе» на 10:00 11.11.2020. "
            "Чтобы записаться самостоятельно, позвоните им: "
            "+7 (000) 000-00-01",
        ),
        (
            "call_failed",
            "Привет! К сожалению, не получилось связаться с «Кафе» "
            "и подтвердить запись на 10:00 11.11.2020. Вы можете попробовать "
            "позвонить им самостоятельно: +7 (000) 000-00-01",
        ),
        (
            "generic_failure",
            "Привет! Не получилось записать вас в «Кафе» на 10:00 11.11.2020. "
            "Позвоните им напрямую: +7 (000) 000-00-01",
        ),
        (
            "generic_failure",
            "Привет! Не получилось записать вас в «Кафе» на 10:00 11.11.2020. "
            "Позвоните им напрямую: +7 (000) 000-00-01",
        ),
    ],
)
async def test_sends_sms_according_verdict(
    domain, dm, yasms, booking_verdict, expected_sms
):
    dm.retrieve_orders_for_sending_sms.coro.return_value = [
        make_order_data(booking_verdict=booking_verdict)
    ]

    await domain.notify_about_processed_tasks()

    yasms.send_sms.assert_called_with(phone=70000000000, text=expected_sms)
