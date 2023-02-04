# -*- coding: utf-8 -*-
import pytest

import btestlib.reporter as reporter
from balance.features import Features
from balance.snout_steps import api_steps as steps
from btestlib.data.snout_constants import Handles
from balance.balance_steps.other_steps import UserSteps

pytestmark = [reporter.feature(Features.UI, Features.INVOICE)]


@pytest.mark.smoke
def test_invoice_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/invoice?invoice_id=XXX
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    client_id, person_id, invoice_id, orders_list = steps.create_invoice()
    steps.pull_handle_and_check_result(Handles.INVOICE, invoice_id, user=user)


def test_invoice_acts_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/invoice/acts?invoice_id=XXX
    """
    client_id, invoice_id, act_id = steps.create_act()
    steps.pull_handle_and_check_result(Handles.INVOICE_ACTS, invoice_id)


def test_invoice_consumes_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/invoice/consumes?invoice_id=XXX
    """
    client_id, invoice_id = steps.do_shipment()
    steps.pull_handle_and_check_result(Handles.INVOICE_CONSUMES, invoice_id)


def test_invoice_oebs_data_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/invoice/oebs-data?invoice_id=XXX
    """
    client_id, invoice_id = steps.do_shipment()
    steps.pull_handle_and_check_result(Handles.INVOICE_OEBS_DATA, invoice_id)


def test_invoice_operations_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/invoice/operations?invoice_id=XXX
    """
    client_id, invoice_id = steps.do_shipment()
    steps.pull_handle_and_check_result(Handles.INVOICE_OPERATIONS, invoice_id)


def test_invoice_transfer_check_handle():
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/invoice/transfer/check?invoice_id=XXX&dst_service_order_id=YYY&dst_service_id=ZZZ
    """
    _, _, invoice_id, orders_list = steps.create_invoice()
    steps.pull_handle_and_check_result(Handles.INVOICE_TRANSFER_CHECK, invoice_id, {
        'dst_service_order_id': orders_list[0]['ServiceOrderID'],
        'dst_service_id': orders_list[0]['ServiceID'],
    })
