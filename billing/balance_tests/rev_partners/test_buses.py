# -*- coding: utf-8 -*-

from decimal import Decimal as D

import datetime

from balance import constants as const
from balance import mapper
from balance import reverse_partners as rp
from balance.constants import *
from tests.balance_tests.rev_partners.common import (
    gen_contract,
    generate_acts,
    compose_get_contract_func,
)


def test_buses(session):
    con_func = compose_get_contract_func(const.ServiceId.BUSES)
    contract = gen_contract(
        session, postpay=True, personal_account=True, con_func=con_func
    )
    rp.compl_map[ServiceId.BUSES] = lambda contract, on_dt: [(None, D("6.66"))]

    a_m = mapper.ActMonth(for_month=datetime.datetime(2017, 1, 1))

    rpc = rp.ReversePartnerCalc(contract, [ServiceId.BUSES], a_m)
    res = rpc.process_and_enqueue_act()
    assert len(res) == 2
    acts = generate_acts(contract, a_m, dps=res[0], invoices=res[1])
    assert len(acts) == 1
    assert len(acts[0].rows) == 1
    assert acts[0].rows[0].act_qty == D("6.66")
