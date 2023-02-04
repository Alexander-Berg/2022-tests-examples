# -*- coding: utf-8 -*-

__author__ = 'blubimov'

from decimal import Decimal

import pytest
from hamcrest import contains_string

from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as const
from btestlib import matchers
from btestlib import reporter
from btestlib import utils
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import STATION_PAYMENTS_CONTEXT, STATION_SERVICES_CONTEXT


SERVICE = STATION_PAYMENTS_CONTEXT.service

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.STATION),
    pytest.mark.tickets('BALANCE-27187'),
    pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/thirdpartytransactions/payments'),
    pytest.mark.usefixtures("switch_to_pg")
]


class PROTOCOL(object):
    XMLRPC = 'xmlrpc'
    REST = 'rest'


AMOUNT = simpleapi_defaults.DEFAULT_PRICE

AR_SERVICE = STATION_PAYMENTS_CONTEXT.service.id
SERVICES_SERVICE = STATION_SERVICES_CONTEXT.service.id


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# проверка платежа
@pytest.mark.parametrize('data', [
    pytest.mark.smoke({'commission_category': 500}),
    {'commission_category': 0},
    {'commission_category': 10000},
    {'commission_category': None, 'exc': 'Commission category is mandatory field for 611'},
], ids=lambda data: 'commission_category={}'.format(
        data['commission_category']))
def test_payment(data):
    partner_commission = 2
    partner_id, person_id, contract_id = create_contract(partner_commission=partner_commission)

    invoices = get_invoices(contract_id)

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, _ = create_payment(partner_id, data['commission_category'])

    if data.get('exc') is None:

        # запускаем обработку платежа
        steps.CommonPartnerSteps.export_payment(payment_id)

        # проверяем данные платежа в таблице t_thirdparty_transactions
        check_payment(payment_id, trust_payment_id, contract_id,
                      partner_id, person_id, invoices, partner_commission=partner_commission,
                      commission_category=data['commission_category'])

    else:
        # проверяем, что при обработке платежа происходит ошибка
        with pytest.raises(utils.XmlRpc.XmlRpcError) as exc:
            steps.CommonPartnerSteps.export_payment(payment_id)

        utils.check_that(exc.value.response, contains_string(data['exc']))

        # проверяем, что транзакций нет в t_thirdparty_transactions
        check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, invoices,
                      partner_commission=partner_commission, commission_category=data['commission_category'],
                      payment_not_exported=True)


# проверка возврата
def test_refund():
    partner_id, person_id, contract_id = create_contract()

    invoices = get_invoices(contract_id)

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token, service_order_id = create_payment(partner_id)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleNewApi.create_refund(SERVICE, purchase_token)

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    # проверяем данные возврата в таблице t_thirdparty_transactions
    check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, invoices,
                  trust_refund_id=trust_refund_id)


# todo-blubimov сейчас postathorize у нас реализован только для xmlrpc, нужно доделать для rest
# todo-blubimov дергать нужно simpleapi.steps.payments_api_steps.Payments.Order#resize
# todo-blubimov после этого выпилить из этого модуля все про xmlrpc
# проверка обновления суммы платежа
@pytest.mark.parametrize('data', [
    {'updated_amount': 300},
    {'updated_amount': 0},
], ids=lambda data: str(data))
def test_reversal(data):
    partner_commission = 2
    partner_id, person_id, contract_id = create_contract(partner_commission=partner_commission)

    invoices = get_invoices(contract_id)

    # создаем платеж в трасте
    commission_category = 500
    trust_payment_id, payment_id, purchase_token, service_order_id = \
        create_payment(partner_id, commission_category=commission_category, amount=AMOUNT, need_clearing=False,
                       payment_protocol=PROTOCOL.XMLRPC, wait_for_export_from_bs=False)

    updated_amount = data['updated_amount']
    steps.SimpleApi.postauthorize(SERVICE, trust_payment_id, [service_order_id],
                                  amounts=[updated_amount], actions=['clear'])

    payment_id = steps.SimpleApi.get_payment_id(trust_payment_id)

    # запускаем обработку платежа
    export_result = steps.CommonPartnerSteps.export_payment(payment_id)

    if updated_amount == 0:
        utils.check_that(export_result['output'], contains_string('skipped: payment has been completely cancelled'))

    # проверяем данные платежа в таблице t_thirdparty_transactions
    check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, invoices,
                  partner_commission=partner_commission, commission_category=commission_category,
                  payment_amount=updated_amount)


# ---------- utils ----------

def create_contract(is_offer=True, partner_commission=2):
    partner_id = steps.SimpleNewApi.create_partner(SERVICE)
    params = {'partner_commission_pct2': partner_commission}
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(STATION_PAYMENTS_CONTEXT,
                                                                               client_id=partner_id, is_offer=is_offer,
                                                                               additional_params=params)
    return partner_id, person_id, contract_id


def get_invoices(contract_id):
    invoice_eid_ar, _ = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(contract_id,
                                                                                              const.ServiceCode.AGENT_REWARD)
    invoice_eid_ser, _ = steps.InvoiceSteps.get_personal_account_external_id_with_service_code(contract_id,
                                                                                               const.ServiceCode.YANDEX_SERVICE)
    return {AR_SERVICE: invoice_eid_ar, SERVICES_SERVICE: invoice_eid_ser}


def calc_rewards(payment_amount, partner_commission, commission_category):
    amount = utils.dround2(payment_amount)
    return {AR_SERVICE: utils.dround2(utils.pct_sum(amount, partner_commission)),
            SERVICES_SERVICE: utils.dround2(
                    utils.pct_sum(amount, commission_category / Decimal('100')))}


def create_payment(partner_id, commission_category=500, amount=AMOUNT, need_clearing=True,
                   payment_protocol=PROTOCOL.REST, wait_for_export_from_bs=True):
    if payment_protocol == PROTOCOL.REST:
        trust_payment_id, payment_id, purchase_token = create_payment_rest(partner_id, commission_category, amount,
                                                                           wait_for_export_from_bs=wait_for_export_from_bs)
        service_order_id = None
    else:
        service_order_id, trust_payment_id, purchase_token, payment_id = create_payment_xmlrpc(partner_id,
                                                                                               commission_category,
                                                                                               amount, need_clearing,
                                                                                               wait_for_export_from_bs=wait_for_export_from_bs)
    return trust_payment_id, payment_id, purchase_token, service_order_id


def create_payment_rest(partner_id, commission_category, amount, wait_for_export_from_bs=True):
    product_id = steps.SimpleNewApi.create_product(SERVICE, partner_id)
    orders = steps.SimpleNewApi.create_orders_for_payment(SERVICE, product_id,
                                                          commission_category=commission_category,
                                                          amount=amount)
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(SERVICE,
                                                                                     orders=orders,
                                                                                     wait_for_export_from_bs=wait_for_export_from_bs)
    return trust_payment_id, payment_id, purchase_token


def create_payment_xmlrpc(partner_id, commission_category, amount,
                          need_clearing, wait_for_export_from_bs=True):
    service_product_id = steps.SimpleApi.create_service_product(SERVICE, partner_id)

    # создаем платеж в трасте
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE, service_product_id,
                                             commission_category=commission_category,
                                             price=amount,
                                             need_postauthorize=need_clearing,
                                             wait_for_export_from_bs=wait_for_export_from_bs)

    return service_order_id, trust_payment_id, purchase_token, payment_id


def check_payment(payment_id, trust_payment_id, contract_id, partner_id, person_id, invoices,
                  trust_refund_id=None, partner_commission=0, commission_category=0, payment_amount=AMOUNT,
                  payment_not_exported=False):
    amount = utils.dround2(payment_amount)

    if amount == 0 or payment_not_exported:
        expected_payment_lines = []
    else:
        rewards = calc_rewards(amount, partner_commission, commission_category)

        expected_payment_lines = [
            steps.SimpleApi.create_expected_tpt_row(STATION_PAYMENTS_CONTEXT, partner_id, contract_id,
                                                    person_id, trust_payment_id,
                                                    payment_id,
                                                    trust_refund_id,
                                                    amount=amount,
                                                    yandex_reward=None if trust_refund_id else rewards[AR_SERVICE]),
            # https://st.yandex-team.ru/BALANCE-27187#1524859717000
            ]
        if not trust_refund_id:
            expected_payment_lines.append(
                steps.SimpleApi.create_expected_tpt_row(STATION_PAYMENTS_CONTEXT, partner_id, contract_id,
                                                        person_id, trust_payment_id,
                                                        payment_id,
                                                        trust_refund_id,
                                                        amount=rewards[SERVICES_SERVICE],
                                                        invoice_eid=invoices[SERVICES_SERVICE],
                                                        transaction_type=const.TransactionType.REFUND.name,
                                                        paysys_type_cc=const.PaysysType.YANDEX,
                                                        payment_type=const.PaymentType.CORRECTION_COMMISSION))

    # получаем данные по платежу
    actual_payment_lines = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id,
                                                                                             transaction_type=None,
                                                                                             trust_id=trust_refund_id or trust_payment_id)

    # Проверяем данные платежа
    utils.check_that(actual_payment_lines,
                     matchers.contains_dicts_with_entries(expected_payment_lines),
                     u'Проверяем данные платежа')
