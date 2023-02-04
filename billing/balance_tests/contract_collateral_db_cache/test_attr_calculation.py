# -*- coding: utf-8 -*-
import pytest
from datetime import datetime
from dateutil.relativedelta import relativedelta

from tests import object_builder as ob
from tests.balance_tests.contract_collateral_db_cache.conftest import create_collateral

COLLATERALS_COUNT = 6


@pytest.fixture()
def signed_contract_with_collaterals(session):
    # Make half of collaterals (including col0) in past
    dt = datetime.now() - relativedelta(days=COLLATERALS_COUNT // 2)
    contract = ob.ContractBuilder(
        dt=dt,
        is_signed=dt,
        memo='col0',
    ).build(session).obj

    for i in range(1, COLLATERALS_COUNT):
        dt += relativedelta(days=1)
        create_collateral(
            session,
            contract,
            {
                "is_signed": dt,
                "dt": dt,
                "memo": 'col%s' % i
            }
        )

    return contract


@pytest.mark.parametrize(
    ('check_delta',),
    [
        pytest.param(
            relativedelta(minutes=30),
            id='Checks between collaterals',
        ),
        pytest.param(
            relativedelta(),
            id='Checks exactly at collaterals',
        ),
    ]
)
@pytest.mark.parametrize(
    ('check_at_col_num',),
    [
        pytest.param(x, id='Start from col %s' % x)
        for x in range(COLLATERALS_COUNT)
    ]
)
def test_cont(session, signed_contract_with_collaterals, check_at_col_num, check_delta):
    contract = signed_contract_with_collaterals

    starting_col = contract.collaterals[check_at_col_num]

    attributes = contract.get_raw_attributes_per_collateral_dt(
            col_from_dt=starting_col.dt + check_delta,
            check_signed=True,
            db_cache_compat_mode=True
        )

    expected_memo_attr_count = len(contract.collaterals) - check_at_col_num
    assert len(attributes) == len(contract.col0.attributes) - 1 + expected_memo_attr_count
    for attr, from_collateral, to_dt in attributes:
        if attr.code == "MEMO":
            col_idx = from_collateral.num or 0
            assert attr.value[-1] == str(col_idx)
            assert to_dt == (
                contract.collaterals[col_idx + 1].dt
                if col_idx + 1 < len(contract.collaterals)
                else datetime.max
            )
        else:
            assert from_collateral == contract.col0
            assert to_dt == datetime.max


@pytest.mark.parametrize(
    ('dt_delta', 'signed_delta',),
    [
        pytest.param(
            relativedelta(days=-1),
            relativedelta(days=0),
            id="No collaterals in future"
        ),
        pytest.param(
            relativedelta(days=0),
            relativedelta(days=0),
            id="Only collateral now"
        ),
        pytest.param(
            relativedelta(days=1),
            relativedelta(days=-1),
            id="Only collateral in future"
        ),
    ]
)
def test_signed_contract_corner_cases(
    session, dt_delta, signed_delta,
):
    now = datetime.now()
    contract = ob.ContractBuilder(
        dt=now + dt_delta,
        is_signed=now + signed_delta,
    ).build(session).obj

    attributes = contract.get_raw_attributes_per_collateral_dt(
            col_from_dt=now,
            check_signed=True,
            db_cache_compat_mode=True
        )

    assert len(attributes) > 0
    assert all([attr.collateral_id == contract.col0.id for (attr, _, __) in attributes])
    assert all([col == contract.col0 for (_, col, __) in attributes])
    assert all([to_dt == datetime.max for (_, __, to_dt) in attributes])


