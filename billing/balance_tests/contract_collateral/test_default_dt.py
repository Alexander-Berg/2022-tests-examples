# -*- coding: utf-8 -*-
from datetime import datetime

import pytest
from typing import Optional

from balance.mapper import Contract
from billing.contract_iface.contract_meta import collateral_types
from tests import object_builder as ob


def create_contract(session, set_none=None):
    # type: (object, Optional[bool]) -> Contract
    params = dict()
    if set_none is None:
        # behaviour for which we sure everything works
        params['dt'] = datetime.now()
    elif set_none:
        params['dt'] = None

    contract = ob.ContractBuilder.construct(
        session,
        **params
    )
    return contract


def create_via_contract_object_builder(session, set_none):
    contract = create_contract(session, set_none)
    return contract.col0


def create_via_collateral_builder(session, set_none):
    contract = create_contract(session)
    params = dict()
    if set_none:
        params['dt'] = None

    col = ob.CollateralBuilder.construct(
        session,
        contract=contract,
        **params
    )
    return col


def create_via_append_collateral(session, set_none):
    contract = create_contract(session)
    params = {'dt': datetime.now()}
    if set_none:
        params['dt'] = None

    col = contract.append_collateral(
        collateral_type=collateral_types['GENERAL'][1019],
        **params
    )
    return col


@pytest.mark.parametrize(
    ('set_none',),
    [
        (True,),
        (False,),
    ]
)
@pytest.mark.parametrize(
    ('creation_func',),
    [
        (create_via_contract_object_builder,),
        (create_via_collateral_builder,),
        (create_via_append_collateral,),
    ]
)
def test_collateral_dt_not_none(session, creation_func, set_none):
    collateral = creation_func(session, set_none)

    assert isinstance(collateral.dt, datetime)
