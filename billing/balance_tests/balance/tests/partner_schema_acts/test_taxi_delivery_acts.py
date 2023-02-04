# coding: utf-8

from decimal import Decimal

from hamcrest import empty
import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Products, ServiceCode
from btestlib.data.partner_contexts import *
from btestlib.matchers import contains_dicts_with_entries

AMOUNT = Decimal('100.11')
PAY_AMOUNT = Decimal('58.93')
_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
  PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()


postpay_params = [
    [0, 'prepay'],
    [1, 'postpay'],
]


contexts = [
    TAXI_RU_DELIVERY_CONTEXT,
    TAXI_DELIVERY_ISRAEL_CONTEXT,
    TAXI_DELIVERY_KZ_CONTEXT,
    TAXI_DELIVERY_BY_BYN_CONTEXT,
    TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT
]

foodtech_delivery_bv_usd_contexts = [
    TAXI_DELIVERY_GEO_USD_CONTEXT,
    TAXI_DELIVERY_UZB_USD_CONTEXT,
    TAXI_DELIVERY_ARM_USD_CONTEXT,
    TAXI_DELIVERY_KGZ_USD_CONTEXT,
    TAXI_DELIVERY_GHA_USD_CONTEXT,
    TAXI_DELIVERY_ZAM_USD_CONTEXT,
    TAXI_DELIVERY_AZ_USD_CONTEXT,
]

foodtech_delivery_bv_eur_contexts = [
    TAXI_DELIVERY_EST_EUR_CONTEXT,
    TAXI_DELIVERY_KZ_EUR_CONTEXT,
    TAXI_DELIVERY_BY_EUR_CONTEXT,
    TAXI_DELIVERY_ISR_EUR_CONTEXT,
    TAXI_DELIVERY_UZB_EUR_CONTEXT,
    TAXI_DELIVERY_CMR_EUR_CONTEXT,
    TAXI_DELIVERY_ARM_EUR_CONTEXT,
    TAXI_DELIVERY_KGZ_EUR_CONTEXT,
    TAXI_DELIVERY_GHA_EUR_CONTEXT,
    TAXI_DELIVERY_SEN_EUR_CONTEXT,
    TAXI_DELIVERY_MXC_EUR_CONTEXT,
    TAXI_DELIVERY_TR_EUR_CONTEXT,
    TAXI_DELIVERY_PER_EUR_CONTEXT,
    TAXI_DELIVERY_ZA_EUR_CONTEXT,
    TAXI_DELIVERY_UAE_EUR_CONTEXT,
    TAXI_DELIVERY_ANG_EUR_CONTEXT,
    TAXI_DELIVERY_ZAM_EUR_CONTEXT,
    TAXI_DELIVERY_CIV_EUR_CONTEXT,
    TAXI_DELIVERY_AZ_EUR_CONTEXT,
    TAXI_DELIVERY_MD_EUR_CONTEXT,
    TAXI_DELIVERY_RS_EUR_CONTEXT
]

contexts += foodtech_delivery_bv_usd_contexts
contexts += foodtech_delivery_bv_eur_contexts

products_commission = {
    TAXI_RU_DELIVERY_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_RUB,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_RUB,
        Products.TAXI_DELIVERY_CASH_CARGO_ORDER_RUB,
        Products.TAXI_DELIVERY_CARD_ORDER_RUB,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_RUB,
        Products.TAXI_DELIVERY_CARD_CARGO_ORDER_RUB
    ],
    TAXI_DELIVERY_ISRAEL_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_ILS,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_ILS,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_ILS,
        Products.TAXI_DELIVERY_CARD_ORDER_ILS,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_ILS,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_ILS,
    ],
    TAXI_DELIVERY_KZ_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_KZT,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_KZT,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_KZT,
        Products.TAXI_DELIVERY_CARD_ORDER_KZT,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_KZT,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_KZT,
    ],
    TAXI_DELIVERY_BY_BYN_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_BYN,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_BYN,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_BYN,
        Products.TAXI_DELIVERY_CARD_ORDER_BYN,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_BYN,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_BYN,
    ],
    TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_ILS,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_ILS,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_ILS,
        Products.TAXI_DELIVERY_CARD_ORDER_ILS,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_ILS,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_ILS,
    ],
}

for i in foodtech_delivery_bv_usd_contexts:
    products_commission[i.name] = [
        Products.TAXI_DELIVERY_CASH_ORDER_USD,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_USD,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_USD,
        Products.TAXI_DELIVERY_CARD_ORDER_USD,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_USD,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_USD,
    ]

for i in foodtech_delivery_bv_eur_contexts:
    products_commission[i.name] = [
        Products.TAXI_DELIVERY_CASH_ORDER_EUR,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_EUR,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_EUR,
        Products.TAXI_DELIVERY_CARD_ORDER_EUR,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_EUR,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_EUR,
    ]

products_payments = {
    TAXI_RU_DELIVERY_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_RUB
    ],
    TAXI_DELIVERY_ISRAEL_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_ILS
    ],
    TAXI_DELIVERY_KZ_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_KZT
    ],
    TAXI_DELIVERY_BY_BYN_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_BYN
    ],
    TAXI_YANGO_DELIVERY_ISRAEL_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_ILS
    ],
}

for i in foodtech_delivery_bv_usd_contexts:
    products_payments[i.name] = [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_USD
    ]

for i in foodtech_delivery_bv_eur_contexts:
    products_payments[i.name] = [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_EUR
    ]

@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_taxi_delivery_acts_wo_data(context, is_postpay, _postpay_id):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_postpay=0, is_offer=1, additional_params={'start_dt': PREVIOUS_MONTH_START_DT}
    )

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT,
                                                                   manual_export=False)
    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal('0'),
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.AGENT_REWARD),
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal(0),
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.YANDEX_SERVICE),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=False),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), u'Проверяем, что актов нет')

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    utils.check_that(consume_data, empty(), u'Проверяем, что косьюмов нет')


@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_taxi_delivery_acts_two_months(context, is_postpay, _postpay_id):
    expected_act_data = []
    expected_invoice_amount = Decimal('0')
    total_pay_sum = Decimal('0')
    total_compls_sum = Decimal('0')

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_postpay=is_postpay, is_offer=1, additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT}
    )

    pa_data = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code=ServiceCode.YANDEX_SERVICE)
    pa_id, pa_eid, service_code = pa_data

    check_balance(u'Проверяем баланс в первом месяце до платежа', context, contract_id, client_id, pa_eid,
                  is_postpay, personal_account_pay_sum=Decimal('0'), total_compls_sum=Decimal('0'),
                  cur_month_charge=Decimal('0'), act_sum=Decimal('0'))

    steps.InvoiceSteps.pay(pa_id, payment_sum=PAY_AMOUNT, payment_dt=MONTH_BEFORE_PREV_START_DT)
    total_pay_sum += PAY_AMOUNT

    check_balance(u'Проверяем баланс в первом месяце после платежа', context, contract_id, client_id, pa_eid,
                  is_postpay, personal_account_pay_sum=total_pay_sum, total_compls_sum=Decimal('0'),
                  cur_month_charge=Decimal('0'), act_sum=Decimal('0'))

    for product in products_commission[context.name]:
        create_oebs_completions(context, product.service.id, product.id, contract_id, client_id,
                                MONTH_BEFORE_PREV_END_DT, AMOUNT, coef=1)
    cur_month_charge = AMOUNT * len(products_commission[context.name])
    total_compls_sum += cur_month_charge

    check_balance(u'Проверяем баланс в первом месяце с открутками до акта', context, contract_id, client_id, pa_eid,
                  is_postpay, personal_account_pay_sum=total_pay_sum, total_compls_sum=total_compls_sum,
                  cur_month_charge=cur_month_charge, act_sum=Decimal('0'))

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, MONTH_BEFORE_PREV_END_DT)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_amount = AMOUNT * len(products_commission[context.name])
    expected_invoice_amount += expected_act_amount
    expected_act_data.append(
        steps.CommonData.create_expected_act_data(expected_act_amount, MONTH_BEFORE_PREV_END_DT),
    )
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')

    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal('0'),
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.AGENT_REWARD),
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id,
                                                                 expected_invoice_amount,
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.YANDEX_SERVICE,
                                                                 total_act_sum=expected_invoice_amount),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=False),
                     u'Сравниваем данные из счета с шаблоном')

    check_balance(u'Проверяем баланс во втором месяце до актов и откруток', context, contract_id, client_id, pa_eid,
                  is_postpay, personal_account_pay_sum=total_pay_sum, total_compls_sum=total_compls_sum,
                  cur_month_charge=Decimal('0'), act_sum=expected_act_amount)

    for product in products_commission[context.name]:
        create_oebs_completions(context, product.service.id, product.id, contract_id, client_id,
                                PREVIOUS_MONTH_END_DT, AMOUNT, coef=2)

    cur_month_charge = 2 * AMOUNT * len(products_commission[context.name])
    total_compls_sum += cur_month_charge

    check_balance(u'Проверяем баланс во втором месяце с открутками до актов', context, contract_id, client_id, pa_eid,
                  is_postpay, personal_account_pay_sum=total_pay_sum, total_compls_sum=total_compls_sum,
                  cur_month_charge=cur_month_charge, act_sum=expected_invoice_amount)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_amount = AMOUNT * len(products_commission[context.name]) * 2
    expected_invoice_amount += expected_act_amount
    expected_act_data.append(
        steps.CommonData.create_expected_act_data(expected_act_amount, PREVIOUS_MONTH_END_DT),
    )
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')

    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal('0'),
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.AGENT_REWARD),
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, expected_invoice_amount,
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.YANDEX_SERVICE,
                                                                 total_act_sum=expected_invoice_amount),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=False),
                     u'Сравниваем данные из счета с шаблоном')

    check_balance(u'Проверяем баланс во втором месяце после актов', context, contract_id, client_id, pa_eid,
                  is_postpay, personal_account_pay_sum=total_pay_sum, total_compls_sum=total_compls_sum,
                  cur_month_charge=Decimal('0'), act_sum=expected_invoice_amount)


@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_taxi_delivery_payments_acts_two_months(context, is_postpay, _postpay_id):
    expected_act_data = []
    expected_invoice_amount = Decimal('0')

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, is_postpay=0, is_offer=1, additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT}
    )
    for product in products_payments[context.name]:
        create_oebs_completions(context, product.service.id, product.id, contract_id, client_id,
                                MONTH_BEFORE_PREV_END_DT, AMOUNT, coef=1)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, MONTH_BEFORE_PREV_END_DT)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_amount = AMOUNT * len(products_payments[context.name])
    expected_invoice_amount += expected_act_amount
    expected_act_data.append(
        steps.CommonData.create_expected_act_data(expected_act_amount, MONTH_BEFORE_PREV_END_DT),
    )
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')

    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal('0'),
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.YANDEX_SERVICE),
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id,
                                                                 expected_invoice_amount,
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.AGENT_REWARD,
                                                                 total_act_sum=expected_invoice_amount),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=False),
                     u'Сравниваем данные из счета с шаблоном')

    for product in products_payments[context.name]:
        create_oebs_completions(context, product.service.id, product.id, contract_id, client_id,
                                PREVIOUS_MONTH_END_DT, AMOUNT, coef=2)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREVIOUS_MONTH_END_DT)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_amount = AMOUNT * len(products_payments[context.name]) * 2
    expected_invoice_amount += expected_act_amount
    expected_act_data.append(
        steps.CommonData.create_expected_act_data(expected_act_amount, PREVIOUS_MONTH_END_DT),
    )
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')

    invoice_data = steps.InvoiceSteps.get_personal_accounts_with_service_codes(client_id)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, Decimal('0'),
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.YANDEX_SERVICE),
        steps.CommonData.create_expected_invoice_data_by_context(context, contract_id, person_id, expected_invoice_amount,
                                                                 dt=PREVIOUS_MONTH_START_DT,
                                                                 service_code=ServiceCode.AGENT_REWARD,
                                                                 total_act_sum=expected_invoice_amount),
    ]

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data, same_length=False),
                     u'Сравниваем данные из счета с шаблоном')


# ---------------------------------------------------------------------------------------------------------------
# Utils

def create_oebs_completions(context, service_id, product_id, contract_id, client_id, dt, amount, coef):
    compls_dicts = [
        {
            'service_id': service_id,
            'last_transaction_id': 99,
            'amount': 2 * coef * amount,
            'product_id': product_id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        },
        {
            'service_id': service_id,
            'last_transaction_id': 99,
            'amount': -coef * amount,
            'product_id': product_id,
            'dt': dt,
            'transaction_dt': dt,
            'currency': context.currency.iso_code,
            'accounting_period': dt
        }
    ]
    steps.CommonPartnerSteps.create_partner_oebs_completions(contract_id, client_id, compls_dicts)


def prepare_expected_balance_data(context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                                  total_compls_sum, cur_month_charge, act_sum):
    if is_postpay:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'Currency': context.currency.iso_code,
            'CommissionToPay': cur_month_charge,
            'ActSum': act_sum,
            'BonusLeft': Decimal('0'),
            'CurrMonthBonus': Decimal('0'),
        }
    else:
        expected_balance = {
            'ClientID': client_id,
            'ContractID': contract_id,
            'PersonalAccountExternalID': invoice_eid,
            'Currency': context.currency.iso_code,
            'Balance': personal_account_pay_sum - total_compls_sum,
            'CurrMonthCharge': cur_month_charge,
            'ActSum': act_sum,
            'BonusLeft': Decimal('0'),
            'CurrMonthBonus': Decimal('0'),
        }
    return expected_balance


def check_balance(descr, context, contract_id, client_id, invoice_eid, is_postpay, personal_account_pay_sum,
                  total_compls_sum, cur_month_charge, act_sum):
    partner_balance = steps.PartnerSteps.get_partner_balance(context.partner_balance_service, [contract_id])
    expected_balance = prepare_expected_balance_data(context, contract_id, client_id, invoice_eid,
                                                     is_postpay, personal_account_pay_sum,
                                                     total_compls_sum, cur_month_charge, act_sum)
    utils.check_that(partner_balance, contains_dicts_with_entries([expected_balance]), descr)
