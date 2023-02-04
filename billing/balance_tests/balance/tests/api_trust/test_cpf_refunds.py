# -*- coding: utf-8 -*-

import datetime
import pytest
import hamcrest
import functools

from balance import balance_steps as steps
from balance import balance_db as db
from temp.igogor.balance_objects import (
    Contexts,
    Services,
    Products,
    PersonTypes,
    Currencies,
    Firms,
    Paysyses
)

import btestlib.utils as utils
from btestlib.constants import (
    Export,
    Permissions,
)

from simpleapi.steps import web_steps as web
from simpleapi.steps import payments_api_steps as payments_api
from simpleapi.data.cards_pool import get_card

from balance.features import Features
import btestlib.reporter as reporter

pytestmark = [
    pytest.mark.usefixtures('switch_to_pg'),
    reporter.feature(Features.TRUST_API)
]

INVOICE_QTY = 666


@pytest.fixture
def context():
    return Contexts.DIRECT_FISH_RUB_CONTEXT.new(
        service=Services.AUTORU,
        product=Products.AUTORU,
        currency=Currencies.RUB,
        firm=Firms.VERTICAL_12,
        person_type=PersonTypes.UR,
        paysys=Paysyses.CC_RUB_UR_TRUST_AUTORU,
        additional_contract_params={}
    )


def get_bound_card(context, user):
    payment_methods = steps.TrustApiSteps.get_bound_payment_methods(
        service_id=context.service.id,
        passport_id=user.id_
    )
    if payment_methods:
        return payment_methods[0]['id']

    url_info = steps.TrustApiSteps.get_card_binding_url(
        service_id=context.service.id,
        currency=context.currency.iso_code,
        passport_id=user.id_
    )

    card = get_card()
    web.Cloud.bind_card(card, url_info['binding_url'])

    def _check_binding():
        check_res = steps.TrustApiSteps.check_binding(
            passport_id=user.id_,
            service_id=context.service.id,
            token=url_info['purchase_token']
        )
        return check_res

    utils.wait_until(lambda: _check_binding()['payment_resp_desc'], success_condition=hamcrest.equal_to('paid ok'))
    res = _check_binding()
    return res['payment_method_id']


def create_pay_invoice(context, user):
    payment_method_id = get_bound_card(context, user)

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(
        client_id=client_id,
        product_id=context.product.id,
        service_id=context.service.id,
        service_order_id=service_order_id,
    )
    request_id = steps.RequestSteps.create(
        client_id=client_id,
        orders_list=[{
            'ServiceID': context.service.id,
            'ServiceOrderID': service_order_id,
            'Qty': INVOICE_QTY,
        }],
    )

    payment_response = steps.TrustApiSteps.pay_request(
        passport_id=user.id_,
        payment_method_id=payment_method_id,
        currency_code='RUB',
        request_id=request_id,
        person_id=person_id,
    )

    utils.wait_until(
        lambda: steps.TrustApiSteps.check_request_payment(
            passport_id=user.id_,
            service_id=context.service.id,
            transaction_id=payment_response['transaction_id']
        )['resp_desc'],
        success_condition=hamcrest.equal_to('success')
    )

    return {
        'client_id': client_id,
        'person_id': person_id,
        'order_id': order_id,
        'invoice_id': payment_response['invoice_id'],
        'transaction_id': payment_response['transaction_id']
    }


def clear_payment(context, user, invoice_info):
    def _check_payment():
        return payments_api.Payments.get(context.service, user, invoice_info['transaction_id'])['payment_status']

    utils.wait_until(_check_payment, success_condition=hamcrest.not_(hamcrest.equal_to('started')))
    payments_api.Payments.clear(context.service, user, invoice_info['transaction_id'])
    utils.wait_until(_check_payment, success_condition=hamcrest.equal_to('cleared'))


def create_cpf(invoice_info, invoice_state):
    payment_res, = db.balance().execute(
        "SELECT id FROM bo.t_payment where paysys_code = 'TRUST_API' AND transaction_id = :id",
        {'id': invoice_info['transaction_id']}
    )

    cash_fact_id, _ = steps.InvoiceSteps.create_cash_payment_fact(
        invoice_state['external_id'],
        invoice_state['total_sum'],
        datetime.datetime.now(),
        'ACTIVITY',
        payment_res['id'],
        invoice_info['invoice_id']
    )
    return cash_fact_id


def _wait_for_export(refund_id):
    try:
        return steps.CommonSteps.export(Export.Type.TRUST_API, Export.Classname.INVOICE_REFUND, refund_id)
    except utils.XmlRpc.XmlRpcError as e:
        if e.response != 'Retrying TRUST_API processing: wait_for_notification':
            raise


def test_partial(context, get_free_user):
    user = get_free_user()

    # Добавляем нужные права
    steps.UserSteps.set_role_with_permissions_strict(
        user,
        [
            Permissions.DO_INVOICE_REFUND,
            Permissions.WITHDRAW_CONSUMES_PREPAY,
            Permissions.DO_INVOICE_REFUND_TRUST,
        ]
    )

    invoice_info = create_pay_invoice(context, user)
    clear_payment(context, user, invoice_info)
    init_invoice_state, = db.get_invoice_by_id(invoice_info['invoice_id'])
    cpf_id = create_cpf(invoice_info, init_invoice_state)

    refund_id = steps.RefundSteps.create_refund(cpf_id, 100, user_uid=user.id_)
    steps.ExportSteps.prevent_auto_export(refund_id, Export.Classname.INVOICE_REFUND, Export.Type.TRUST_API)
    refund_info = steps.RefundSteps.get(refund_id)
    hamcrest.assert_that(
        refund_info,
        hamcrest.has_entries(
            status_code='uninitialized'
        )
    )

    export_res = utils.wait_until(
        functools.partial(_wait_for_export, refund_id),
        success_condition=hamcrest.is_not(None)
    )
    hamcrest.assert_that(
        export_res,
        hamcrest.has_entries(
            state='1'
        )
    )

    res_invoice_state, = db.get_invoice_by_id(invoice_info['invoice_id'])
    hamcrest.assert_that(
        res_invoice_state,
        hamcrest.has_entries(
            receipt_sum=init_invoice_state['receipt_sum'] - 100
        )
    )
