# coding: utf-8
__author__ = 'pelmeshka'

from decimal import Decimal as D
from apikeys import apikeys_steps
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib import reporter as reporter
from btestlib.constants import Currencies, Firms
from btestlib.data.defaults import Date
from btestlib.matchers import equal_to_casted_dict
from btestlib.utils import aDict
from hamcrest import not_none
from pytest import param
import pytest
from temp.igogor.balance_objects import Contexts


APIKEYS_CONTEXT = Contexts.APIKEYS_CONTEXT.new(firm=Firms.YANDEX_1, currency=Currencies.RUB)
START = Date.TODAY_ISO
FINISH = Date.YEAR_AFTER_TODAY_ISO
SUM = 100
QTY = 50
TIMEOUT = 200


@reporter.feature(Features.APIKEYS, Features.NOTIFICATION)
@pytest.mark.tickets('BALANCE-28340')
@pytest.mark.parametrize('credit', [
    param(1, id="CREDIT_INVOICE"),
    param(0, id="NO_CREDIT_INVOICE")])
def test_invoice_notification_on_bank_payment_by_contract(credit):
    client_id, person_id = prepare_client_and_person()
    contract_id, contract_eid = prepare_contract(client_id, person_id)
    invoice_id, invoice_eid, request_id = prepare_invoice(client_id, person_id, credit=credit, contract_id=contract_id)
    steps.InvoiceSteps.pay_fair(invoice_id, SUM)

    with reporter.step(u'Запрашиваем нотификацию о receipt\'e'):
        notification = steps.CommonSteps.wait_and_get_notification(20, invoice_id, 1, timeout=TIMEOUT)
    utils.check_that(notification, not_none(), u'Проверяем, что нотификация пришла')

    expected_notification = get_common_notification(client_id, invoice_eid, SUM)
    expected_notification.update({'Contract': contract_eid})
    if not credit:
        expected_notification['RequestID'] = request_id
    utils.check_that(notification, equal_to_casted_dict(expected_notification),
                     u'Проверяем правильность параметров нотификации')


@reporter.feature(Features.APIKEYS, Features.NOTIFICATION)
@pytest.mark.tickets('BALANCE-28340')
def test_invoice_notification_on_bank_payment_by_contract_charge_note():
    client_id, person_id = prepare_client_and_person()
    contract_id, contract_eid = prepare_contract(client_id, person_id)
    invoice_id, invoice_eid, request_id = prepare_invoice(client_id, person_id, is_charge_note=1, contract_id=contract_id)
    steps.InvoiceSteps.pay_fair(invoice_id)

    with reporter.step(u'Запрашиваем id счёта, на который поступил receipt'):
        invoice_id_with_receipt, invoice_eid_with_receipt = steps.InvoiceSteps.get_invoice_ids(client_id,
                                                                                               type='personal_account')
    with reporter.step(u'Запрашиваем нотификацию о receipt\'e'):
        notification = steps.CommonSteps.wait_and_get_notification(20, invoice_id_with_receipt, 1, timeout=TIMEOUT)
    utils.check_that(notification, not_none(), u'Проверяем, что нотификация пришла')
    expected_notification = get_common_notification(client_id, invoice_eid_with_receipt, QTY)
    expected_notification.update({'Contract': contract_eid, 'ConsumeSum': D(0)})
    utils.check_that(notification, equal_to_casted_dict(expected_notification),
                     u'Проверяем правильность параметров нотификации')


@reporter.feature(Features.APIKEYS, Features.NOTIFICATION)
@pytest.mark.tickets('BALANCE-28340')
def test_invoice_notification_on_bank_payment_without_contract():
    client_id, person_id = prepare_client_and_person()
    invoice_id, invoice_eid, request_id = prepare_invoice(client_id, person_id)
    steps.InvoiceSteps.pay(invoice_id, SUM)

    with reporter.step(u'Запрашиваем нотификацию о receipt\'e'):
        notification = steps.CommonSteps.wait_and_get_notification(20, invoice_id, 1, timeout=TIMEOUT)
    utils.check_that(notification, not_none(), u'Проверяем, что нотификация пришла')

    expected_notification = get_common_notification(client_id, invoice_eid, SUM)
    expected_notification.update({'RequestID': request_id})
    utils.check_that(notification, equal_to_casted_dict(expected_notification),
                     u'Проверяем правильность параметров нотификации')


# ----------------------------------------------------------------------
# Utils
def prepare_client_and_person():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, APIKEYS_CONTEXT.person_type.code)
    return client_id, person_id


def prepare_contract(client_id, person_id):
    apikeys_tariffs = apikeys_steps.get_apikeys_tariffs(aDict({'tariff': 'apikeys_apimaps_10k_yearprepay_2017'}), 'apimaps')
    contract_id, contract_eid = steps.ContractSteps.create_contract('no_agency_apikeys_post',
                                                                    {
                                                                        'CLIENT_ID': client_id,
                                                                        'PERSON_ID': person_id,
                                                                        'DT': START,
                                                                        'FINISH_DT': FINISH,
                                                                        'IS_SIGNED': START,
                                                                        'APIKEYS_TARIFFS': apikeys_tariffs,
                                                                    })
    return contract_id, contract_eid


def prepare_invoice(client_id, person_id, is_charge_note=0, credit=0, contract_id=None):
    service_order_id = steps.OrderSteps.next_id(APIKEYS_CONTEXT.service.id)
    steps.OrderSteps.create(client_id, service_order_id,
                            product_id=APIKEYS_CONTEXT.product.id, service_id=APIKEYS_CONTEXT.service.id)

    orders_list = [{'ServiceID': APIKEYS_CONTEXT.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': START}]
    additional_params = {'InvoiceDesireDT': START,
                         'FirmID': APIKEYS_CONTEXT.firm.id,
                         }
    if is_charge_note:
        additional_params.update({'InvoiceDesireType': 'charge_note'})

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=additional_params)

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.create(request_id, person_id, APIKEYS_CONTEXT.paysys.id,
                                                           credit=credit, contract_id=contract_id)
    return invoice_id, invoice_eid, request_id


def get_common_notification(client_id, invoice_eid, receipt_sum):
    return {
        'ActSum': D(0),
        'ClientID': client_id,
        'ConsumeSum': D(QTY),
        'Currency': APIKEYS_CONTEXT.currency.iso_code,
        'FirmID': APIKEYS_CONTEXT.firm.id,
        'Invoice': invoice_eid,
        'ReceiptSum': D(receipt_sum),
    }
