# -*- coding: utf-8 -*-
__author__ = 'sandyk'

from datetime import datetime

import pytest
from hamcrest import equal_to

from balance.features import Features
from dateutil.relativedelta import relativedelta
from temp.igogor.balance_objects import Contexts
from btestlib import (
    reporter as reporter,
    utils as utils,
)
from btestlib.constants import (
    Firms,
    ContractPaymentType,
    Currencies,
    PersonTypes,
    Paysyses,
    ContractCommissionType,
    ContractCreditType
)
from ... import (
    balance_steps as steps,
    balance_db as db,
)

OOO = Firms.YANDEX_1.id
DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT
SERVICE_7 = DIRECT.service.id
PRODUCT = DIRECT.product.id
PAYMENT_TYPE = ContractPaymentType.POSTPAY
NO_AGENCY = ContractCommissionType.NO_AGENCY
PR_AGENCY = ContractCommissionType.PR_AGENCY
UR = PersonTypes.UR.code
YT = PersonTypes.YT.code
CREDIT_LIMIT = 1000000
CAMPAIGN_QTY = 1000
PAYMENT_TERM = 15
QTY = 10000

pytestmark = [pytest.mark.tickets('TESTBALANCE-1631'),
              reporter.feature(Features.UI, Features.INVOICE, Features.REVERSE, Features.CREDIT)
              ]

PAYSYS_MAPPING = {UR: Paysyses.BANK_UR_RUB.id,
                  YT: Paysyses.BANK_YT_USD.id,
                  }

NOW_DT = datetime.now()
FEW_MONTHS_AGO = NOW_DT - relativedelta(days=180)
START_DT = utils.Date.date_to_iso_format(NOW_DT - relativedelta(days=180))
FINISH_DT = utils.Date.date_to_iso_format(NOW_DT + relativedelta(days=180))

DEFAULT_CONTRACT_PARAMS = {'DT': START_DT,
                           'IS_SIGNED': START_DT,
                           'FIRM': OOO,
                           'SERVICES': [SERVICE_7],
                           'PAYMENT_TYPE': PAYMENT_TYPE,
                           'UNILATERAL': 1,
                           'CURRENCY': Currencies.RUB.num_code,
                           'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT,
                           'PAYMENT_TERM': PAYMENT_TERM,
                           }


def create_invoice(client_id=None, person_id=None, contract_type=NO_AGENCY, is_agency=0, person_type=UR,
                   contract_params=DEFAULT_CONTRACT_PARAMS):
    invoice_owner = client_id or steps.ClientSteps.create(params={'IS_AGENCY': is_agency})
    order_owner = steps.ClientSteps.create(params={'IS_AGENCY': 0}) if is_agency else invoice_owner
    person_id = person_id or steps.PersonSteps.create(invoice_owner, person_type)
    contract_all_params = DEFAULT_CONTRACT_PARAMS.copy()
    contract_all_params.update(contract_params)
    contract_all_params.update({'CLIENT_ID': invoice_owner, 'PERSON_ID': person_id})
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_all_params)
    service_order_id = steps.OrderSteps.next_id(SERVICE_7)
    order_id = steps.OrderSteps.create(order_owner, service_order_id, PRODUCT, SERVICE_7,
                                       {'TEXT': 'Py_Test order',
                                        'AgencyID': invoice_owner if is_agency else None,
                                        'ManagerUID': None, })

    orders_list = [{'ServiceID': SERVICE_7,
                    'ServiceOrderID': service_order_id,
                    'Qty': QTY,
                    'BeginDT': FEW_MONTHS_AGO}]
    request_id = steps.RequestSteps.create(invoice_owner,
                                           orders_list,
                                           additional_params={'InvoiceDesireDT': FEW_MONTHS_AGO})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id,
                                                 person_id=person_id,
                                                 paysys_id=PAYSYS_MAPPING[person_type],
                                                 credit=1,
                                                 contract_id=contract_id,
                                                 overdraft=0,
                                                 endbuyer_id=None)
    return invoice_owner, person_id, service_order_id, contract_id, invoice_id


def get_campaings_dates():
    start_1_month, _, _, end_2_month, _, end_3_month = utils.Date.previous_three_months_start_end_dates()
    return [start_1_month, end_2_month, end_3_month]


def check_credit_limit(contract_id, calc_act_dates, update_limit_available=1):
    if update_limit_available:
        acts = steps.ActsSteps.get_act_data_by_contract(contract_id)
        act_sum = 0
        for act in acts:
            if act['dt'] in calc_act_dates:
                currency_rate = steps.CurrencySteps.get_currency_rate(act['dt'], act['currency'], 'RUR', 1000)
                act_sum += act['act_sum'] * currency_rate
        expected_limit_val = round(act_sum / 3 * (30 + PAYMENT_TERM) / 30)
    else:
        expected_limit_val = CREDIT_LIMIT
    collaterals = db.get_collaterals_by_contract(contract_id)
    fact_limit_val = [attr['value_num'] for attr in
                      db.get_attributes_by_batch_id(collaterals[0]['attribute_batch_id']) if
                      attr['code'] == 'CREDIT_LIMIT_SINGLE'][0]
    utils.check_that(fact_limit_val, equal_to(expected_limit_val))


def generate_acts(service_order_id, invoice_id, dates):
    bucks = CAMPAIGN_QTY
    acts_list = []
    for month in dates:
        steps.CampaignsSteps.do_campaigns(SERVICE_7, service_order_id,
                                          {'Bucks': bucks, 'Days': 0, 'Money': 0}, 0,
                                          campaigns_dt=month)
        bucks += 1000
        acts_list.append(steps.ActsSteps.create(invoice_id, month)[0])
    return acts_list


@pytest.mark.parametrize('contract_type, is_agency, person_type, contract_params, update_limit_available',
                         [
                             (PR_AGENCY, 1, UR, DEFAULT_CONTRACT_PARAMS, 1),
                             (NO_AGENCY, 0, UR, DEFAULT_CONTRACT_PARAMS, 1),
                             (NO_AGENCY, 0, UR, {'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM}, 0),
                             ##  по договору с видом кредита по сроку и сумме лимит не перерасчитывается
                             (PR_AGENCY, 1, YT, {'CURRENCY': Currencies.USD.num_code, 'DEAL_PASSPORT': START_DT}, 1),
                         ],
                         ids=[
                             'agency_contract_rub',
                             'no_agency_contract_by_term',
                             'no_agency_contract_by_term_and sum',
                             'agency_contract_usd',
                         ]
                         )
def test_credit_limit(contract_type, is_agency, person_type, contract_params, update_limit_available):
    agency_id, person_id, service_order_id, contract_id, invoice_id = create_invoice(contract_type=contract_type,
                                                                                     is_agency=is_agency,
                                                                                     person_type=person_type,
                                                                                     contract_params=contract_params)
    dates = get_campaings_dates()
    _ = generate_acts(service_order_id, invoice_id, dates)
    steps.CloseMonth.update_limits(dates[2], 0, [agency_id])
    check_credit_limit(contract_id, dates, update_limit_available)


## лимит не пересчитывается для недействующего договора
def test_credit_limit_expired_contract():
    client_id, person_id, service_order_id, contract_id, invoice_id = create_invoice()
    dates = get_campaings_dates()
    batch_id = db.get_collaterals_by_contract(contract_id)[0]['attribute_batch_id']
    db.balance().execute(
        """update t_contract_attributes set value_dt = to_date('{}', 'YYYY-MM-DD HH24:MI:SS') where attribute_batch_id = {} and code = 'FINISH_DT'""".format(
            dates[2], batch_id))
    steps.ContractSteps.refresh_contracts_cache(contract_id)
    _ = generate_acts(service_order_id, invoice_id, dates)
    steps.CloseMonth.update_limits(dates[2], 0, [client_id])
    check_credit_limit(contract_id, dates, 0)


## захайженные акты не участвуют при расчете лимита
def test_credit_limit_hidden_acts():
    client_id, person_id, service_order_id, contract_id, invoice_id = create_invoice()
    dates = get_campaings_dates()
    acts = generate_acts(service_order_id, invoice_id, dates)
    for act in acts:
        steps.ActsSteps.hide(act)
    steps.CloseMonth.update_limits(dates[2], 0, [client_id])
    check_credit_limit(contract_id, dates, 0)


## 2 договора с лимитом у 1 клиента, лимит расчитыватся в контексте договора
def test_credit_limit_2_contracts():
    client_id, person_id, service_order_id, contract_id, invoice_id = create_invoice()
    _, _, service_order_id_2, contract_id_2, invoice_id_2 = create_invoice(client_id=client_id, person_id=person_id)
    dates = get_campaings_dates()
    _ = generate_acts(service_order_id, invoice_id, dates)
    _ = generate_acts(service_order_id_2, invoice_id_2, dates)
    steps.CloseMonth.update_limits(dates[2], 0, [client_id])
    check_credit_limit(contract_id, dates, 1)
    check_credit_limit(contract_id_2, dates, 1)


## 1 акт 4 месяца назад, второй  - в 1 день текущего месяца
def test_credit_limit_no_acts():
    client_id, person_id, service_order_id, contract_id, invoice_id = create_invoice()
    start_4_month = utils.Date.shift(months=-4)
    start_0_month = utils.Date.current_month_first_and_last_days()[0]
    _ = generate_acts(service_order_id, invoice_id, [start_4_month, start_0_month])
    upd_lim_dt, _ = utils.Date.previous_month_first_and_last_days()
    steps.CloseMonth.update_limits(upd_lim_dt, 0, [client_id])
    check_credit_limit(contract_id, [start_4_month, start_0_month], 0)


## 3 месяца назад акт не 1 числа месяца - не пересчитывается лимит
def test_credit_limit_no_act_3_month_ago():
    client_id, person_id, service_order_id, contract_id, invoice_id = create_invoice()
    dates = get_campaings_dates()
    dates[0] = utils.Date.shift(dates[0], days=1)
    _ = generate_acts(service_order_id, invoice_id, dates)
    steps.CloseMonth.update_limits(dates[2], 0, [client_id])
    check_credit_limit(contract_id, dates, 0)


## 3 акта в нужном промежутке + 1 акт 4 месяца назад, второй  - в 1 день текущего месяца - они не учитываются при расчете лимита
def test_credit_limit_outside_act_period():
    client_id, person_id, service_order_id, contract_id, invoice_id = create_invoice()
    calc_dates = get_campaings_dates()
    dates = calc_dates
    dates.insert(0, utils.Date.shift(months=-4))
    dates.append(utils.Date.current_month_first_and_last_days()[0])
    _ = generate_acts(service_order_id, invoice_id, dates)
    upd_lim_dt, _ = utils.Date.previous_month_first_and_last_days()
    steps.CloseMonth.update_limits(upd_lim_dt, 0, [client_id])
    check_credit_limit(contract_id, calc_dates, 1)
