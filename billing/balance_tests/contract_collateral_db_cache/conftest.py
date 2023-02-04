# -*- coding: utf-8 -*-

from tests import object_builder as ob
from datetime import datetime


def set_collateral_attribute_links(session, collateral):
    for raw_attr in collateral.attributes:
        raw_attr.passport_id = session.oper_id
        raw_attr.collateral_id = collateral.id


def create_collateral(session, contract, collateral_params):
    """
    collateral_params: dict of collateral parameters
    """
    # num - обязательное поле в collateral
    if 'num' not in collateral_params:
        cur_collateral_number = getattr(contract.collaterals[-1], 'num', 0)
        next_collateral_number = int(cur_collateral_number) + 1 if cur_collateral_number is not None else 1
        collateral_params['num'] = next_collateral_number

    col_obj = ob.CollateralBuilder.construct(
        session,
        contract=contract,
        **collateral_params
    )
    # CollateralBuilder does not set any ids to attrs
    set_collateral_attribute_links(session, col_obj)
    session.flush()
    return col_obj


def truncate_dt(dt):  # type: (datetime) -> datetime
    return dt.replace(hour=0, minute=0, second=0, microsecond=0)
