# -*- coding: utf-8 -*-

from decimal import Decimal as D

from balance import mapper
from balance import reverse_partners as rp
from balance.constants import *
from tests import object_builder as ob
from tests.balance_tests.rev_partners.common import (
    gen_acts,
    gen_contract,
)


def travel_attributes(contract):
    contract.col0.services = {ServiceId.TRAVEL}
    contract.col0.currency = CurrencyNumCode.USD
    contract.col0.partner_credit = 1
    contract.col0.personal_account = 1


def create_money_product(session, product_unit_id=853, currency_iso_code=None):
    unit = ob.Getter(mapper.ProductUnit, product_unit_id).build(session).obj
    product = (
        ob.ProductBuilder(price=1, tax=1, unit=unit, engine_id=ServiceId.TRAVEL)
        .build(session)
        .obj
    )
    partner_product = (
        ob.PartnerProductBuilder(
            product=product, currency_iso_code=currency_iso_code, order_type="main"
        )
        .build(session)
        .obj
    )


def test_acts(session):
    AMOUNT = D("666.66")
    SERVICE_ID = ServiceId.TRAVEL
    CURRENCY_ISO_CODE = "USD"

    contract = gen_contract(
        session, personal_account=True, postpay=True, con_func=travel_attributes
    )

    # create_money_product(currency_iso_code=CURRENCY_ISO_CODE)
    rp.compl_map[ServiceId.TRAVEL] = lambda contract, on_dt: [(None, AMOUNT)]

    a_m = mapper.ActMonth(for_month=contract.col0.dt)
    rpc = rp.ReversePartnerCalc(contract, [SERVICE_ID], a_m)
    acts = gen_acts(rpc)

    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == AMOUNT
    assert acts[0].rows[0].act_sum == AMOUNT
