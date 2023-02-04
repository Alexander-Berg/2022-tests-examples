# -*- coding: utf-8 -*-
__author__ = 'atkaya'

from decimal import Decimal

import pytest
import hamcrest as hm
from datetime import datetime, timedelta

import btestlib.reporter as reporter

from balance import balance_steps as steps, balance_api as api
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType, PaysysType
from btestlib.data.partner_contexts import SUBAGENCY_EVENTS_TICKETS_CONTEXT, SUBAGENCY_EVENTS_TICKETS2_RU_CONTEXT,\
    SUBAGENCY_EVENTS_TICKETS3_RU_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from check import db

pytestmark = [
    reporter.feature(
        Features.SIDEPAYMENT,
        Features.SUBAGENCY_EVENTS_TICKETS,
        Features.TICKETS,
    ),
]

START_DT = utils.Date.first_day_of_month()
AMOUNT = Decimal('999.66')
REWARD_VAT_PCT = Decimal('0.1')

CONTEXTS = [
    SUBAGENCY_EVENTS_TICKETS_CONTEXT,
    SUBAGENCY_EVENTS_TICKETS2_RU_CONTEXT,
    SUBAGENCY_EVENTS_TICKETS3_RU_CONTEXT,
]

PARTNER_INTEGRATION_PARAMS = steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT

parametrize_context = pytest.mark.parametrize('context', CONTEXTS, ids=lambda x: x.name)
parametrize_splitting = pytest.mark.parametrize(
    'with_reward_splitting',
    [
        pytest.param(True, id='with splitting', marks=pytest.mark.smoke),
        pytest.param(False, id='without splitting')
    ]
)
parametrize_commission_category = pytest.mark.parametrize('commission_category', [
    pytest.param('100', marks=pytest.mark.smoke),
    '10',
])


@reporter.feature(Features.PAYMENT, Features.TRUST)
@parametrize_context
@parametrize_splitting
@parametrize_commission_category
def test_payment(context, with_reward_splitting, commission_category):
    payment_type, paysys_type_cc = PaymentType.CARD, 'yamoney'
    client_id, person_id, contract_id = create_contract(context, with_reward_splitting)
    steps.SimpleApi.create_fake_service_product(context.service, client_id, service_fee=666)
    side_transaction_id, side_payment_id, _ = create_payment(context, client_id, commission_category, payment_type)
    expected_transactions = create_expected_transactions(context, client_id, person_id, contract_id,
                                                         TransactionType.PAYMENT, payment_type, paysys_type_cc,
                                                         side_transaction_id, side_payment_id, AMOUNT,
                                                         commission_category, with_reward_splitting)
    payment_data = get_thirdparty_payment_by_payment_id(side_payment_id, with_reward_splitting)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_transactions),
                     u'Сравниваем платеж с шаблоном')


@reporter.feature(Features.REFUND, Features.TRUST)
@parametrize_context
@parametrize_splitting
@parametrize_commission_category
def test_refund(context, with_reward_splitting, commission_category):
    payment_type, paysys_type_cc = PaymentType.CARD, 'yamoney'
    client_id, person_id, contract_id = create_contract(context, with_reward_splitting)
    steps.SimpleApi.create_fake_service_product(context.service, client_id, service_fee=666)
    side_payment_transaction_id, side_payment_id, service_order_id = create_payment(
        context, client_id, commission_category, payment_type)

    side_refund_id, side_refund_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          currency=context.currency,
                                                          extra_str_0=commission_category,
                                                          extra_str_1=service_order_id,
                                                          orig_transaction_id=side_payment_transaction_id,
                                                          payload="{}")

    steps.ExportSteps.create_export_record_and_export(side_refund_id,
                                                      Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_transactions = create_expected_transactions(context, client_id, person_id, contract_id,
                                                         TransactionType.REFUND, payment_type, paysys_type_cc,
                                                         side_refund_transaction_id, side_refund_id, AMOUNT,
                                                         commission_category, with_reward_splitting)
    payment_data = get_thirdparty_refund_by_payment_id(side_refund_id, with_reward_splitting)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_transactions),
                     u'Сравниваем платеж с шаблоном')


@reporter.feature(Features.REFUND, Features.TRUST)
@parametrize_context
def test_payout_ready_dt_updating(context):
    with_reward_splitting = True
    commission_category = '100'
    payment_type = PaymentType.CARD
    client_id, person_id, contract_id = create_contract(context, with_reward_splitting)
    steps.SimpleApi.create_fake_service_product(context.service, client_id, service_fee=666)
    side_payment_transaction_id, side_payment_id, service_order_id = create_payment(
        context, client_id, commission_category, payment_type)

    # проставляем платежу payout_ready_dt
    check_payout_ready_dt(side_payment_id, None)
    payout_ready_dt = datetime.now().replace(microsecond=0) - timedelta(days=1)

    with reporter.step(u'Проставляем значение payout_ready_dt '):
        api.medium().UpdatePayment({'ServiceID': context.service.id, 'TransactionID': side_payment_transaction_id},
                                   {'PayoutReady': payout_ready_dt.strftime('%Y-%m-%d %H:%M:%S')})
    with reporter.step(u'Переэкспортируем платеж'):
        steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, side_payment_id)
    check_payout_ready_dt(side_payment_id, payout_ready_dt)
    # Проверим что в платеж payout_ready_dt проставился
    payment_data = get_thirdparty_payment_by_payment_id(side_payment_id, with_reward_splitting)
    for payment in payment_data:
        hm.assert_that(payment, hm.has_entry('payout_ready_dt', payout_ready_dt))

    side_refund_id, side_refund_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          currency=context.currency,
                                                          extra_str_0=commission_category,
                                                          extra_str_1=service_order_id,
                                                          orig_transaction_id=side_payment_transaction_id,
                                                          payload="{}")

    steps.ExportSteps.create_export_record_and_export(side_refund_id,
                                                      Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    refund_data = get_thirdparty_refund_by_payment_id(side_refund_id, with_reward_splitting)
    for refund in refund_data:
        hm.assert_that(refund, hm.has_entry('payout_ready_dt', payout_ready_dt))


@reporter.feature(Features.TRUST)
@parametrize_context
def test_commission_less_than_vat_commission(context):
    payment_type = PaymentType.CARD
    with_reward_splitting = True
    commission_category = str(REWARD_VAT_PCT * 100 / 2)

    client_id, person_id, contract_id = create_contract(context, with_reward_splitting)
    steps.SimpleApi.create_fake_service_product(context.service, client_id, service_fee=666)
    with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
        create_payment(context, client_id, commission_category, payment_type)
    utils.check_that(
        error.value.response, hm.contains_string(
            "Commission percent (sum of agent reward with vat and without vat)"
            " is less then agent reward with vat percent"
        ),
        u'Проверяем текст ошибки экспорта платежа'
    )


# -------------------------------------------------------------------------------------------------------------
# Utils
def create_payment(context, client_id, commission_category, payment_type=PaymentType.CARD):
    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()

    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=context.currency,
                                                          extra_str_0=commission_category,
                                                          extra_str_1=service_order_id,
                                                          payload="{}")

    steps.ExportSteps.create_export_record_and_export(side_payment_id,
                                                      Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)
    return side_transaction_id, side_payment_id, service_order_id


def get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund):
    # собираем фактические
    if is_refund:
        main_type, reward_type = TransactionType.REFUND, TransactionType.PAYMENT
    else:
        main_type, reward_type = TransactionType.PAYMENT, TransactionType.REFUND

    rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, main_type,
                                                                             source='sidepayment')
    # если схема с разделением НДС - добавим возвраты коррекций (с типом - платеж) и уберем коррекции с типом платеж
    if with_reward_splitting:
        # (игнорируем возвраты - коррекции - они созданы (и проверяются) в платеже) для возврата и наоборот для платежа
        rows = [row for row in rows if row['paysys_type_cc'] != PaysysType.NETTING_WO_NDS]
        correction_refund_rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, reward_type, source='sidepayment')
        correction_refund_rows = [row for row in correction_refund_rows
                                  if row['paysys_type_cc'] == PaysysType.NETTING_WO_NDS]
        rows.extend(correction_refund_rows)
    return rows


def get_thirdparty_payment_by_payment_id(payment_id, with_reward_splitting):
    return get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund=False)


def get_thirdparty_refund_by_payment_id(payment_id, with_reward_splitting):
    return get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund=True)


def create_contract(context, with_reward_splitting):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        contract_additional_params = {'start_dt': START_DT}
        if with_reward_splitting:
            contract_additional_params['partner_commission_pct2'] = REWARD_VAT_PCT

        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context,
            partner_integration_params=PARTNER_INTEGRATION_PARAMS,
            additional_params=contract_additional_params)

        return client_id, person_id, contract_id


def get_expected_invoice_eid(context, contract_id, client_id, paysys_type_cc):
    if paysys_type_cc == PaysysType.NETTING_WO_NDS:
        return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 0)
    return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 1)


def get_yandex_reward(price, commission_category):
    min_payment = Decimal('0.01')
    yandex_reward = utils.dround((Decimal(price) * Decimal(commission_category)) /
                                 Decimal('10000'), 2)
    return max(yandex_reward, min_payment)


def commission_categories_and_payment_params(service, commission_category,
                                             with_reward_splitting, for_refund):
    if with_reward_splitting:  # если обычный платеж - и происходит разделение
        # для логики с разделением - переведем проценты в табличные значения (T_PARTNER_COMMISSION_CATEGORY)
        reward_vat_pct_as_commission = REWARD_VAT_PCT * 100
        payment_params = {
            # инвертировано для безндсной части
            'transaction_type': TransactionType.PAYMENT.name if for_refund else TransactionType.REFUND.name,
            'payment_type': PaymentType.CORRECTION_NETTING,
            'paysys_type_cc': PaysysType.NETTING_WO_NDS,
        }

        # для платежей и возвратов в которых возвращаем вознаграждение - разделим его
        if not for_refund or steps.SimpleApi.get_reward_refund_for_service(service):
            return [(str(Decimal(commission_category) - reward_vat_pct_as_commission), payment_params),
                    (str(reward_vat_pct_as_commission), None)]

    return [(commission_category, None)]


def create_expected_transactions(context, client_id, person_id, contract_id, transaction_type, payment_type,
                                 paysys_type_cc, transaction_id, side_payment_id, amount,
                                 commission_category, with_reward_splitting):
    commissions_with_params = commission_categories_and_payment_params(
        context.service, commission_category, with_reward_splitting,
        for_refund=transaction_type == TransactionType.REFUND)

    expected_rows = []
    for row_commission_category, payment_params in commissions_with_params:
        payment_params = payment_params or dict()
        yandex_reward = get_yandex_reward(amount, row_commission_category)
        if transaction_type == TransactionType.REFUND and \
                not steps.SimpleApi.get_reward_refund_for_service(context.service):
            yandex_reward = None

        tpt_attrs = {
            'transaction_type': transaction_type.name,
            'payment_type': payment_type,
            'amount': amount,
            'paysys_type_cc': paysys_type_cc,
            'yandex_reward': yandex_reward,
            'invoice_eid': get_expected_invoice_eid(
                context, contract_id, client_id,
                paysys_type_cc=payment_params.get('paysys_type_cc') or paysys_type_cc)
        }
        tpt_attrs.update(payment_params)

        trust_payment_id = transaction_id if tpt_attrs['transaction_type'] == TransactionType.PAYMENT.name else None
        row = steps.SimpleApi.create_expected_tpt_row(
            context, client_id, contract_id, person_id,
            trust_payment_id, side_payment_id, trust=False, **tpt_attrs)

        if with_reward_splitting and row.get('paysys_type_cc') == PaysysType.NETTING_WO_NDS:
            # для коррекции меняем местами вознаграждение и сумму.
            row['amount'] = row['yandex_reward']
            row['yandex_reward'] = row['yandex_reward_wo_nds'] = Decimal('0')
        expected_rows.append(row)
    return expected_rows


def check_payout_ready_dt(payment_id, expected_payout_ready_dt):
    payment_data = db.get_side_payment_data(payment_id)[0] or None
    payout_ready_dt = payment_data['payout_ready_dt'] or None
    if expected_payout_ready_dt:
        utils.check_that(payout_ready_dt, expected_payout_ready_dt)
    else:
        utils.check_that(payout_ready_dt, hm.none())
