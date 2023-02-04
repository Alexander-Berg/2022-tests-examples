# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime
import hamcrest as hm
import pytest

from balance import mapper
from balance import reverse_partners as rp
from tests.balance_tests.rev_partners.common import (
    gen_contract,
    gen_tpts_for_contract,
    generate_acts,
    get_multiple_tpts_data,
    SERVICES_INFO,
)
from tests.balance_tests.rev_partners.overrides import TicketToEventOverrides

get_multiple_tpts_data_func_override = {
    "TicketsToEvents": TicketToEventOverrides.get_multiple_tpts_data
}

SERVICES_INFO_FLATTEN = [
    (name, ids, contract_func)
    for name, (ids, contract_func) in SERVICES_INFO.items()
]


def check_acts(acts, expected_qty):
    assert len(acts) == len(expected_qty)
    for qty in expected_qty:
        hm.assert_that(
            acts,
            hm.has_item(
                hm.all_of(
                    hm.has_properties({"rows": hm.has_length(1)}),
                    hm.has_properties(
                        {"rows": hm.has_items(hm.has_properties({"act_qty": qty}))}
                    ),
                )
            ),
        )


def base(
    session, service_ids, con_func, begin_dt, finish_dt, act_dt, tpts, expected_qty
):
    if not con_func:
        return

    contract = gen_contract(
        session,
        postpay=True,
        personal_account=True,
        con_func=con_func,
        begin_dt=begin_dt,
        finish_dt=finish_dt,
    )
    gen_tpts_for_contract(contract, tpts)
    a_m = mapper.ActMonth(for_month=act_dt)

    rpc = rp.ReversePartnerCalc(contract, service_ids, a_m)
    res = rpc.process_and_enqueue_act()
    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    check_acts(acts, expected_qty)
    return acts


@pytest.mark.parametrize(
    ("service_name", "service_ids", "get_contract_func",),
    SERVICES_INFO_FLATTEN,
)
def test_part_month(session, service_name, service_ids, get_contract_func):
    begin_dt = datetime.datetime(2020, 6, 5)
    finish_dt = datetime.datetime(2020, 6, 20)
    act_dt = datetime.datetime(2020, 6, 30)
    get_multiple_tpts_data_func = get_multiple_tpts_data_func_override.get(service_name, get_multiple_tpts_data)
    tpts = get_multiple_tpts_data_func(begin_dt, finish_dt, act_dt)
    expected_qty = D(2 + 16)
    base(
        session,
        service_ids,
        get_contract_func,
        begin_dt,
        finish_dt,
        act_dt,
        tpts,
        [expected_qty],
    )


@pytest.mark.parametrize(
    ("service_name", "service_ids", "get_contract_func",),
    SERVICES_INFO_FLATTEN,
)
def test_with_finish_at_future(session, service_name, service_ids, get_contract_func):
    begin_dt = datetime.datetime(2020, 6, 1)
    finish_dt = datetime.datetime(2021, 1, 1)
    act_dt = datetime.datetime(2020, 6, 30)
    get_multiple_tpts_data_func = get_multiple_tpts_data_func_override.get(service_name, get_multiple_tpts_data)
    tpts = get_multiple_tpts_data_func(begin_dt, finish_dt, act_dt)
    expected_qty = D(2 + 4)
    base(
        session,
        service_ids,
        get_contract_func,
        begin_dt,
        finish_dt,
        act_dt,
        tpts,
        [expected_qty],
    )
