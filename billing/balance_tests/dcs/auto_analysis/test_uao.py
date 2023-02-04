# coding: utf-8

import datetime
import datetime as dt

import pytest

from tests.balance_tests.dcs.dcs_common import (
    create_order,
    create_client,
    process_completions,
    constants
)

from balance.actions.dcs.compare.auto_analysis import uao

ISO_PY_DATE_FORMAT = '%Y-%m-%d'


@pytest.mark.parametrize(
    'parent_product_id, child_product_id,'
    'root_order_completion_qty, child_orders_overused_qty,'
    'money_before, money_after, money_type, possible_rounding_error',
    [
        pytest.param(constants.DIRECT_PRODUCT_RUB_ID, constants.DIRECT_PRODUCT_RUB_ID,
                     300, 120, 300, 120, 'Money', False, id='money'),
        pytest.param(constants.DIRECT_PRODUCT_ID, constants.DIRECT_PRODUCT_ID,
                     10, 4, 10, 4, 'Bucks', False, id='bucks'),
        pytest.param(constants.DIRECT_PRODUCT_RUB_ID, constants.DIRECT_PRODUCT_ID,
                     300, 120, 10, 4, 'Bucks', False, id='money-to-bucks'),
        pytest.param(constants.DIRECT_PRODUCT_ID, constants.DIRECT_PRODUCT_RUB_ID,
                     10, 4, 300, 120, 'Money', False, id='bucks-to-money'),
        pytest.param(constants.DIRECT_PRODUCT_RUB_ID, constants.DIRECT_PRODUCT_RUB_ID,
                     300.0001, 120, 300.0002, 120, 'Money', True, id='round_error'),
    ]

)
def test_aa(session, parent_product_id, child_product_id,
            root_order_completion_qty, child_orders_overused_qty,
            money_before, money_after, money_type, possible_rounding_error):
    client = create_client(session)

    parent_order = create_order(session, client, constants.ServiceId.DIRECT,
                                product_id=parent_product_id)
    child_order = create_order(session, client, constants.ServiceId.DIRECT,
                               product_id=child_product_id,
                               group_order_id=parent_order.id,
                               child_ua_type=constants.UAChildType.OPTIMIZED)

    # -1 день от текущей даты, чтобы не обновлять вручную update_dt в bo.t_reverse_completion
    # так заказы пройдут условия при авторазборе
    completions_dt = datetime.datetime.now() - dt.timedelta(days=1)

    rows = [{
        'root_order_id': parent_order.id,
        'root_order_completion_qty': root_order_completion_qty,
        'child_orders_overused_qty': child_orders_overused_qty,
        'completions_dt': completions_dt.strftime(ISO_PY_DATE_FORMAT),
        'possible_rounding_error': possible_rounding_error,
    }]

    process_completions(child_order, {money_type: money_before}, on_dt=completions_dt - dt.timedelta(days=1))
    process_completions(child_order, {money_type: money_after}, on_dt=completions_dt - dt.timedelta(days=1))

    result = uao.process(session, rows)
    expected = [{'order_id': parent_order.id}]

    assert result == expected


def test_convert_child_qty_to_parent_product():
    assert uao.convert_child_qty_to_parent_product(
        child_product_id=1,
        root_product_id=1,
        child_qty=666
    ) == 666

    assert uao.convert_child_qty_to_parent_product(
        child_product_id=constants.DIRECT_PRODUCT_RUB_ID,
        root_product_id=constants.DIRECT_PRODUCT_ID,
        child_qty=60
    ) == 60 / constants.DIRECT_PRODUCT_PRICE

    assert uao.convert_child_qty_to_parent_product(
        child_product_id=constants.DIRECT_PRODUCT_ID,
        root_product_id=constants.DIRECT_PRODUCT_RUB_ID,
        child_qty=2
    ) == 2 * constants.DIRECT_PRODUCT_PRICE

    with pytest.raises(AssertionError):
        uao.convert_child_qty_to_parent_product(
            child_product_id=constants.DIRECT_PRODUCT_RUB_ID + 1,
            root_product_id=constants.DIRECT_PRODUCT_ID + 2,
            child_qty=666
        )
