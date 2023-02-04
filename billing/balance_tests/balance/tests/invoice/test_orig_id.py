# coding: utf-8

__author__ = 'a-vasin'

from hamcrest import none

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.data.partner_contexts import *
from btestlib.matchers import equal_to

DEFAULT_QTY = 50
EXPECTED_INVOICE_TYPE = 'charge_note'
NOT_EXISTING_PAYMENT_ID = 1


def test_correct_orig_id():
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED

    client_id, payment_id = create_charge_note(context)

    create_and_process_payment_fact(client_id, payment_id)

    receipt_sum = get_personal_account_by_client(client_id)['receipt_sum']
    utils.check_that(receipt_sum, equal_to(DEFAULT_QTY), u'Проверяем, что зачисление произошло')

    receipt_sum_1c_payment = steps.PaymentSteps.get_payment(payment_id)['receipt_sum_1c']
    utils.check_that(receipt_sum_1c_payment, equal_to(DEFAULT_QTY), u'Проверяем, что платеж подтвержден')


def test_not_found_orig_id():
    exists = is_payment_exists(NOT_EXISTING_PAYMENT_ID)
    utils.check_that(exists, equal_to(False), u'Проверяем, что платежа не существует')

    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED

    client_id, person_id, contract_id = create_contract(context)

    create_and_process_payment_fact(client_id, NOT_EXISTING_PAYMENT_ID)

    receipt_sum = get_personal_account_by_client(client_id)['receipt_sum']
    utils.check_that(receipt_sum, equal_to(DEFAULT_QTY), u'Проверяем, что зачисление произошло')


def test_wrong_orig_id():
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED

    client_id, person_id, contract_id = create_contract(context)
    _, wrong_payment_id = create_charge_note(context)

    create_and_process_payment_fact(client_id, wrong_payment_id)

    receipt_sum = get_personal_account_by_client(client_id)['receipt_sum']
    utils.check_that(receipt_sum, equal_to(DEFAULT_QTY), u'Проверяем, что зачисление произошло')

    receipt_sum_1c_payment = steps.PaymentSteps.get_payment(wrong_payment_id)['receipt_sum_1c']
    utils.check_that(receipt_sum_1c_payment, none(), u'Проверяем, что платеж не подтвержден')


# ---------utils------------------------------------------------

def search_main_service_order_id(contract_id, service_id):
    orders_info = api.medium().GetOrdersInfo({'ContractID': contract_id, 'ServiceID': service_id})
    if len(orders_info) > 1:
        orders_info = [info for info in orders_info if info['IsGroupRoot']]
        assert len(orders_info) == 1
    return orders_info[0]['ServiceOrderID']


def create_invoice(service_id, client_id, person_id, contract_id, paysys_id):
    service_order_id = search_main_service_order_id(contract_id, service_id)
    orders_list = [{
        'ServiceID': service_id,
        'ServiceOrderID': service_order_id,
        'Qty': DEFAULT_QTY,
        'BeginDT': datetime.now()
    }]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={
                                               'InvoiceDesireDT': datetime.now(),
                                               'InvoiceDesireType': 'charge_note',
                                               'TurnOnRows': True
                                           })

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                 credit=0, contract_id=contract_id)

    return invoice_id


def create_contract(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=False)
    return client_id, person_id, contract_id


def create_charge_note(context):
    client_id, person_id, contract_id = create_contract(context)

    charge_note_id = create_invoice(context.service.id, client_id, person_id, contract_id, context.paysys.id)
    payment_id = steps.PaymentSteps.get_payment_by_invoice_id(charge_note_id)['id']

    return client_id, payment_id


def check_queue_state(invoice_id, expected_state):
    export_data = steps.ExportSteps.get_export_data(invoice_id, Export.Classname.INVOICE, Export.Type.PROCESS_PAYMENTS)
    utils.check_that(export_data['state'], equal_to(str(expected_state)), u'Проверяем состояние очереди')


def create_and_process_payment_fact(client_id, payment_id):
    invoice_id, invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)

    steps.TaxiSteps.create_cash_payment_fact(invoice_eid, DEFAULT_QTY, utils.Date.last_day_of_month(),
                                             type='INSERT', orig_id=payment_id)

    check_queue_state(invoice_id, expected_state=0)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)


def get_personal_account_by_client(client_id):
    query = "SELECT * FROM t_invoice WHERE client_id = :client_id AND type = 'personal_account'"
    params = {
        'client_id': client_id
    }
    return db.balance().execute(query, params)[0]


def is_payment_exists(payment_id):
    query = "SELECT count(*) AS cnt FROM t_payment WHERE id=:payment_id"
    params = {
        'payment_id': payment_id
    }
    return db.balance().execute(query, params)[0]['cnt'] > 0
