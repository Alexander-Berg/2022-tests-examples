# coding: utf-8

from decimal import Decimal

from hamcrest import empty
import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Products, ServiceCode, Regions
from btestlib.data.partner_contexts import TAXI_RU_DELIVERY_CONTEXT, TAXI_DELIVERY_ISRAEL_CONTEXT, \
    TAXI_DELIVERY_KZ_CONTEXT, TAXI_DELIVERY_GEO_USD_CONTEXT, TAXI_DELIVERY_EST_EUR_CONTEXT, TAXI_DELIVERY_BY_BYN_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

AMOUNT = Decimal('100.11')
PAY_AMOUNT = Decimal('58.93')
_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
  PREVIOUS_MONTH_START_DT, PREVIOUS_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()


postpay_params = [
    [0, 'prepay'],
    # [1, 'postpay'],
]


# contexts = [
#     TAXI_RU_DELIVERY_CONTEXT,
#     TAXI_DELIVERY_ISRAEL_CONTEXT,
#     TAXI_DELIVERY_KZ_CONTEXT,
#     TAXI_DELIVERY_GEO_USD_CONTEXT,
#     TAXI_DELIVERY_EST_EUR_CONTEXT,
#     TAXI_DELIVERY_BY_BYN_CONTEXT,
# ]

contexts = [
    # TAXI_DELIVERY_GEO_USD_CONTEXT.new(
    #     special_contract_params={'personal_account': 1, 'country': Regions.ARM.id,
    #                              'partner_commission_pct2': Decimal('10.2'), },
    #     region=Regions.ARM,
    # ),
    # TAXI_DELIVERY_GEO_USD_CONTEXT.new(
    #     special_contract_params={'personal_account': 1, 'country': Regions.AZ.id,
    #                              'partner_commission_pct2': Decimal('10.2'), },
    #     region=Regions.AZ,
    # ),
    # TAXI_DELIVERY_GEO_USD_CONTEXT.new(
    #     special_contract_params={'personal_account': 1, 'country': Regions.UZB.id,
    #                              'partner_commission_pct2': Decimal('10.2'), },
    #     region=Regions.UZB,
    # ),
    # TAXI_DELIVERY_GEO_USD_CONTEXT,
    # TAXI_DELIVERY_GEO_USD_CONTEXT.new(
    #     special_contract_params={'personal_account': 1, 'country': Regions.KGZ.id,
    #                              'partner_commission_pct2': Decimal('10.2'), },
    #     region=Regions.KGZ,
    # ),
    TAXI_DELIVERY_EST_EUR_CONTEXT,
    TAXI_DELIVERY_EST_EUR_CONTEXT.new(
        special_contract_params={'personal_account': 1, 'country': Regions.LAT.id,
                                'partner_commission_pct2': Decimal('10.2'),},
        region=Regions.LAT,
    ),
    TAXI_DELIVERY_EST_EUR_CONTEXT.new(
        special_contract_params={'personal_account': 1, 'country': Regions.FIN.id,
                                'partner_commission_pct2': Decimal('10.2'),},
        region=Regions.FIN,
    ),
    TAXI_DELIVERY_EST_EUR_CONTEXT.new(
        special_contract_params={'personal_account': 1, 'country': Regions.MD.id,
                                 'partner_commission_pct2': Decimal('10.2'), },
        region=Regions.MD,
    ),
    TAXI_DELIVERY_EST_EUR_CONTEXT.new(
        special_contract_params={'personal_account': 1, 'country': Regions.CIV.id,
                                 'partner_commission_pct2': Decimal('10.2'), },
        region=Regions.CIV,
    ),
]


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
    TAXI_DELIVERY_GEO_USD_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_USD,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_USD,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_USD,
        Products.TAXI_DELIVERY_CARD_ORDER_USD,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_USD,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_USD,
    ],
    TAXI_DELIVERY_EST_EUR_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_EUR,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_EUR,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_EUR,
        Products.TAXI_DELIVERY_CARD_ORDER_EUR,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_EUR,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_EUR,
    ],
    TAXI_DELIVERY_BY_BYN_CONTEXT.name: [
        Products.TAXI_DELIVERY_CASH_ORDER_BYN,
        Products.TAXI_DELIVERY_CASH_DRIVER_WORKSHIFT_BYN,
        Products.TAXI_DELIVERY_CASH_DELIVERY_ORDER_BYN,
        Products.TAXI_DELIVERY_CARD_ORDER_BYN,
        Products.TAXI_DELIVERY_CARD_HIRING_WITH_CAR_BYN,
        Products.TAXI_DELIVERY_CARD_DRIVER_WORKSHIFT_BYN,
    ],
}

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
    TAXI_DELIVERY_GEO_USD_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_USD
    ],
    TAXI_DELIVERY_EST_EUR_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_EUR
    ],
    TAXI_DELIVERY_BY_BYN_CONTEXT.name: [
        Products.TAXI_DELIVERY_PAYMENT_MAIN_BYN
    ],
}


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

    utils.check_that(invoice_data, contains_dicts_with_entries(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), u'Проверяем, что актов нет')

    consume_data = steps.ConsumeSteps.get_consumes_sum_by_client_id(client_id)
    utils.check_that(consume_data, empty(), u'Проверяем, что косьюмов нет')


import datetime
MONTH_START = datetime.datetime(2022, 1, 1)
MONTH_END = datetime.datetime(2022, 1, 31)
@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_taxi_delivery_acts_two_months(context, is_postpay, _postpay_id):

    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        context, is_postpay=is_postpay, is_offer=1, additional_params={'start_dt': MONTH_START}
    )

    for product in products_commission[context.name]:
        create_oebs_completions(context, product.service.id, product.id, contract_id, client_id,
                                MONTH_START, AMOUNT, coef=1)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, MONTH_END)
    act_data = steps.ActsSteps.get_all_act_data(client_id, dt=MONTH_END)[0]
    pa_id, pa_eid, service_code = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code=ServiceCode.YANDEX_SERVICE)

    client_log = steps.ExportSteps.get_oebs_api_response('Client', client_id)
    person_log = steps.ExportSteps.get_oebs_api_response('Person', person_id)
    contract_log = steps.ExportSteps.get_oebs_api_response('Contract', contract_id)
    pa_log = steps.ExportSteps.get_oebs_api_response('Invoice', pa_id)
    act_log = steps.ExportSteps.get_oebs_api_response('Act', act_data['id'])

    report_dir = u'/Users/sfreest/Documents/reports'
    report = [
        u'{region_name} {currency}'.format(region_name=context.region.name, currency=context.currency.iso_code),
        u'Client: {client_id}, {log}'.format(client_id=client_id, log=client_log[0]),
        u'Person: {person_id}, {log}'.format(person_id=person_id, log=person_log[0]),
        u'Contract: {contract_eid}, {log}'.format(contract_eid=contract_eid, log=contract_log[0]),
        u'Invoice: {pa_eid}, {log}'.format(pa_eid=pa_eid, log=pa_log[0]),
        u'Act: {act_eid}, {log}'.format(act_eid=act_data['external_id'], log=act_log[0]),
        u'\n',
    ]
    report = [r.encode('utf8') for r in report]
    with open(u'{report_dir}/comm-{reg}-{cur}.txt'.format(report_dir=report_dir,
                                                          reg=context.region.name,
                                                          cur=context.currency.iso_code).encode('utf8'), 'w') as output_file:
        output_file.write(u'\n'.encode('utf8').join(report))


@pytest.mark.parametrize(
    'context, is_postpay, _postpay_id',
    utils.flatten_parametrization(contexts, postpay_params),
    ids=lambda _context, _is_postpay, _postpay_id: '_'.join([_context.name, _postpay_id])
)
def test_taxi_delivery_payments_acts_two_months(context, is_postpay, _postpay_id):

    client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
        context, is_postpay=is_postpay, is_offer=1, additional_params={'start_dt': MONTH_START}
    )
    for product in products_payments[context.name]:
        create_oebs_completions(context, product.service.id, product.id, contract_id, client_id,
                                MONTH_START, AMOUNT, coef=1)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, MONTH_END)
    act_data = steps.ActsSteps.get_all_act_data(client_id, dt=MONTH_END)[0]
    pa_id, pa_eid, service_code = \
        steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id, service_code=ServiceCode.AGENT_REWARD)

    client_log = steps.ExportSteps.get_oebs_api_response('Client', client_id)
    person_log = steps.ExportSteps.get_oebs_api_response('Person', person_id)
    contract_log = steps.ExportSteps.get_oebs_api_response('Contract', contract_id)
    pa_log = steps.ExportSteps.get_oebs_api_response('Invoice', pa_id)
    act_log = steps.ExportSteps.get_oebs_api_response('Act', act_data['id'])

    report_dir = u'/Users/sfreest/Documents/reports'
    report = [
        u'{region_name} {currency}'.format(region_name=context.region.name, currency=context.currency.iso_code),
        u'Client: {client_id}, {log}'.format(client_id=client_id, log=client_log[0]),
        u'Person: {person_id}, {log}'.format(person_id=person_id, log=person_log[0]),
        u'Contract: {contract_eid}, {log}'.format(contract_eid=contract_eid, log=contract_log[0]),
        u'Invoice: {pa_eid}, {log}'.format(pa_eid=pa_eid, log=pa_log[0]),
        u'Act: {act_eid}, {log}'.format(act_eid=act_data['external_id'], log=act_log[0]),
        u'\n',
    ]
    report = [r.encode('utf8') for r in report]
    with open(u'{report_dir}/agent-{reg}-{cur}.txt'.format(report_dir=report_dir,
                                                           reg=context.region.name,
                                                           cur=context.currency.iso_code).encode('utf8'), 'w') as output_file:
        output_file.write(u'\n'.encode('utf8').join(report))


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
