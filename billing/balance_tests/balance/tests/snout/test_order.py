# -*- coding: utf-8 -*-
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance.balance_steps.other_steps import UserSteps
from balance.features import Features
from balance.snout_steps import api_steps as steps
from btestlib.data.snout_constants import Handles

pytestmark = [reporter.feature(Features.UI, Features.ORDER)]


@pytest.mark.smoke
def test_order_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/order?order_id=YYY
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    client_id, _, _, _ = steps.create_invoice()
    order_id = db.get_order_by_client(client_id)[0]['id']
    steps.pull_handle_and_check_result(Handles.ORDER, order_id, user=user)


def test_order_list_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/order/list?client_id==YYY
    """
    client_id, _, _, _ = steps.create_invoice()
    steps.pull_handle_and_check_result(Handles.ORDER_LIST, client_id)


def test_order_operations_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/order/operations?order_id=YYY
    """
    client_id, _ = steps.do_shipment()
    order_id = db.get_order_by_client(client_id)[0]['id']
    steps.pull_handle_and_check_result(Handles.ORDER_OPERATIONS, order_id)


def test_order_untouched_requests_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/order/untouched-requests?order_id=YYY
    """
    client_id, _, _, _ = steps.create_invoice()
    order_id = db.get_order_by_client(client_id)[0]['id']
    steps.pull_handle_and_check_result(Handles.ORDER_UNTOUCHED_REQUESTS, order_id)


def test_order_withdraw_from_orders_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/order/withdraw/from-orders?client_id=XXX
    """
    client_id, _, _, _ = steps.create_invoice()
    steps.pull_handle_and_check_result(Handles.ORDER_WITHDRAW_FROM_ORDERS, client_id)


def test_order_withdraw_validate_amount_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/order/withdraw/validate-amount?order_id=XXX&invoice_id=YYY&amount=ZZZ
    """
    client_id, person_id, invoice_id, orders_list = steps.create_invoice()
    order_id = db.get_order_by_client(client_id)[0]['id']
    steps.pull_handle_and_check_result(
        Handles.ORDER_WITHDRAW_VALIDATE_AMOUNT,
        order_id,
        {
            'invoice_id': invoice_id,
            'amount': 1,
        },
    )
