# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime
import hamcrest as hm
import pytest

from balance import mapper
from balance.constants import *
from .common import SERVICES_INFO
from .overrides import TicketToEventOverrides
from .test_transaction_base import base

service_ids, con_func = SERVICES_INFO["TicketsToEvents"]


@pytest.fixture()
def product_wo_vat_id(session):
    product_wo_vat = (
        session.query(mapper.Product)
        .filter(
            mapper.Product.engine_id == ServiceId.TICKETS_TO_EVENTS,
            mapper.Product.service_code == "YANDEX_SERVICE_WO_VAT",
        )
        .one()
    )
    return product_wo_vat.id


def base_netting(session, netting_tpts, expected_qty):
    begin_dt = datetime.datetime(2020, 1, 5)
    finish_dt = datetime.datetime(2020, 1, 20)
    act_dt = datetime.datetime(2020, 1, 30)
    tpts = TicketToEventOverrides.get_multiple_tpts_data(begin_dt, finish_dt, act_dt)
    return base(
        session,
        service_ids,
        con_func,
        begin_dt,
        finish_dt,
        act_dt,
        tpts + TicketToEventOverrides.fill_service_id(netting_tpts),
        expected_qty,
    )


def check_wo_nds_act(acts, wo_nds_qty):
    hm.assert_that(
        acts,
        hm.has_item(
            hm.all_of(
                hm.has_property("amount_nds", 0),
                hm.has_properties(
                    {"rows": hm.has_items(hm.has_properties({"act_qty": wo_nds_qty}))}
                ),
            )
        ),
    )


def test_netting_refund(session, product_wo_vat_id):
    # добавим строку refund netting_wo_nds
    netting_tpts = [
        {
            "dt": datetime.datetime(2020, 1, 6),
            "amount": 32,
            "paysys_type_cc": "netting_wo_nds",
            "transaction_type": "refund",
            "service_code": "YANDEX_SERVICE_WO_VAT",
            "product_id": product_wo_vat_id,
        }
    ]
    acts = base_netting(session, netting_tpts, [D(2 + 16), D(32)])
    check_wo_nds_act(acts, D(32))


def test_netting_payment(session, product_wo_vat_id):
    netting_tpts = [
        # добавим строку payment netting_wo_nds - она должна уменьшить сумму в безндсном акте
        {
            "dt": datetime.datetime(2020, 1, 6),
            "amount": 4,
            "paysys_type_cc": "netting_wo_nds",
            "transaction_type": "payment",
            "service_code": "YANDEX_SERVICE_WO_VAT",
            "product_id": product_wo_vat_id,
        },
        # добавим строку refund netting_wo_nds что бы общая сумма была положительной и акт появился
        {
            "dt": datetime.datetime(2020, 1, 6),
            "amount": 7,
            "paysys_type_cc": "netting_wo_nds",
            "transaction_type": "refund",
            "service_code": "YANDEX_SERVICE_WO_VAT",
            "product_id": product_wo_vat_id,
        },
    ]
    base_netting(session, netting_tpts, [D(2 + 16), D(7 - 4)])


def test_netting_ignore(session, product_wo_vat_id):
    # проверим что они не учитываются если вне рассматриваемого отрезка
    netting_tpts = [
        {
            "dt": datetime.datetime(2020, 1, 4),
            "amount": 3,
            "paysys_type_cc": "netting_wo_nds",
            "transaction_type": "payment",
            "service_code": "YANDEX_SERVICE_WO_VAT",
            "product_id": product_wo_vat_id,
        },
        {
            "dt": datetime.datetime(2020, 1, 21),
            "amount": 7,
            "paysys_type_cc": "netting_wo_nds",
            "transaction_type": "refund",
            "service_code": "YANDEX_SERVICE_WO_VAT",
            "product_id": product_wo_vat_id,
        },
    ]
    base_netting(session, netting_tpts, [D(2 + 16)])
