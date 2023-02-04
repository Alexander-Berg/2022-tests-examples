# -*- coding: utf-8 -*-
import datetime

import pytest
from hamcrest import anything

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import Services, ContractCommissionType, Firms, \
    ContractPaymentType, Currencies
from simpleapi.matchers.deep_equals import deep_equals_to
from temp.igogor.balance_objects import Contexts

NOW = datetime.datetime.now()
DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT.new()
from btestlib.utils import Date
from dateutil.relativedelta import relativedelta
import btestlib.reporter as reporter
from balance.features import Features

PREVIOUS_MONTH_LAST_DAY = NOW.replace(day=1) - datetime.timedelta(days=1)
PREVIOUS_MONTH_FIRST_DAY_ISO = utils.Date.date_to_iso_format(PREVIOUS_MONTH_LAST_DAY.replace(day=1))
NOW_ISO = utils.Date.date_to_iso_format(NOW + datetime.timedelta(days=31))
LAST_DAY_CURRENT_MONTH = NOW.replace(day=1) + relativedelta(months=1) - datetime.timedelta(days=1)


#
# Хэш с основными параметрами:
# ContractID - id договора, обязательный;
# SubclientID - id субклиента для индивидуального лимита, необязательный;
# ProductID - id продукта, для выбора типа рекламы для старых договоров с разделением кредитов по типам, необязательный;
# ActivityTypeID - id типа рекламы, аналогично ProductID.
# Хэш с флагами, можно не передавать:
# UnpaidInvoices - выводить список неоплаченных счетов, по умолчанию 0;
# ExpiredInvoices - выводить список просроченных счетов, по умолчанию 1;

def create_contract(client_id, person_id, contract_type, contract_params):
    contract_params_default = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
    }
    contract_params_default.update(contract_params)
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params_default,
                                                             prevent_oebs_export=True)
    return contract_id, person_id


def post_pay_contract_personal_account_fictive(contract_type, firm_id, service_list, currency,
                                               additional_params={}):
    default_params = {'contract_type': contract_type,
                      'contract_params': {
                          'SERVICES': service_list,
                          'FIRM': firm_id,
                          'CURRENCY': currency,
                          'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                          'PERSONAL_ACCOUNT': 1,
                          'PERSONAL_ACCOUNT_FICTIVE': 1,
                      }}
    if additional_params:
        default_params['contract_params'].update(additional_params)
    return default_params


def prepay_contract(contract_type, firm_id, service_list, currency, additional_params={}):
    default_params = {'contract_type': contract_type,
                      'contract_params': {
                          'SERVICES': service_list,
                          'FIRM': firm_id,
                          'CURRENCY': currency,
                          'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                      }}
    if additional_params:
        default_params['contract_params'].update(additional_params)
    return default_params


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT])
def test_prepay_contract(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = {'FIRM': Firms.YANDEX_1.id,
                       'CURRENCY': Currencies.RUB.num_code,
                       'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                       'SERVICES': [Services.DIRECT.id],
                       'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'FINISH_DT': NOW_ISO,
                       'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       }
    contract_id, person_id = create_contract(agency_id, person_id, ContractCommissionType.PR_AGENCY, contract_params)
    steps.ContractSteps.get_contract_credits_detailed(contract_id)
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id)
    utils.check_that(response, deep_equals_to({'IsPresent': False, 'ErrorCode': 'prepay_contract'}))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT])
def test_inactive_contract(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = {'FIRM': Firms.YANDEX_1.id,
                       'CURRENCY': Currencies.RUB.num_code,
                       'SERVICES': [Services.DIRECT.id],
                       'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'FINISH_DT': NOW_ISO,
                       # 'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 1,
                       }
    contract_id, person_id = create_contract(agency_id, person_id, ContractCommissionType.PR_AGENCY, contract_params)
    steps.ContractSteps.get_contract_credits_detailed(contract_id)
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id)
    utils.check_that(response, deep_equals_to({'IsPresent': False, 'ErrorCode': 'inactive_contract'}))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT])
def test_partner_Credit(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = {'FIRM': Firms.AUTOBUS_113.id,
                       'CURRENCY': Currencies.RUB.num_code,
                       'SERVICES': [Services.BUSES_2.id],
                       'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'FINISH_DT': NOW_ISO,
                       'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'PERSONAL_ACCOUNT': 1,
                       'PARTNER_CREDIT': 1
                       }
    contract_id, person_id = create_contract(agency_id, person_id, ContractCommissionType.NO_AGENCY, contract_params)
    steps.ContractSteps.get_contract_credits_detailed(contract_id)
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id)
    utils.check_that(response, deep_equals_to({'AvailableSum': '9223372036854775807',
                                               'Currency': 'RUB',
                                               'ExpiredInvoices': [],
                                               'ExpiredSum': '0',
                                               'IsPresent': True,
                                               'LimitSum': '9223372036854775807',
                                               'SpentSum': '0',
                                               'UnpaidSum': '0'}))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT])
def test_active_contract(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = {'FIRM': Firms.YANDEX_1.id,
                       'CURRENCY': Currencies.RUB.num_code,
                       'SERVICES': [Services.DIRECT.id],
                       'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'FINISH_DT': NOW_ISO,
                       'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 1,
                       }
    contract_id, person_id = create_contract(agency_id, person_id, ContractCommissionType.PR_AGENCY, contract_params)
    steps.ContractSteps.get_contract_credits_detailed(contract_id)
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id)
    utils.check_that(response, deep_equals_to({'UnpaidSum': '0',
                                               'AvailableSum': '133333.33',
                                               'IsPresent': True,
                                               'Currency': 'RUB',
                                               'ExpiredInvoices': [],
                                               'SpentSum': '0',
                                               'LimitSum': '133333.33',
                                               'ExpiredSum': '0'}))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.parametrize('context', [DIRECT])
def test_active_contract_extra_params_check(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = {'FIRM': Firms.YANDEX_1.id,
                       'CURRENCY': Currencies.RUB.num_code,
                       'SERVICES': [Services.DIRECT.id],
                       'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'FINISH_DT': NOW_ISO,
                       'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 1,
                       }
    contract_id, person_id = create_contract(agency_id, person_id, ContractCommissionType.PR_AGENCY, contract_params)
    extra_params = {'UnpaidInvoices': 1}
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id, extra_params=extra_params)
    utils.check_that(response, deep_equals_to(
        {'UnpaidSum': '0',
         'AvailableSum': '133333.33',
         'Currency': 'RUB',
         'IsPresent': True,
         'SpentSum': '0',
         'LimitSum': '133333.33',
         'ExpiredInvoices': [],
         'ExpiredSum': '0',
         'UnpaidInvoices': []}))
    extra_params = {'UnpaidInvoices': 0}
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id, extra_params=extra_params)
    utils.check_that(response, deep_equals_to(
        {'UnpaidSum': '0',
         'AvailableSum': '133333.33',
         'Currency': 'RUB',
         'IsPresent': True,
         'SpentSum': '0',
         'LimitSum': '133333.33',
         'ExpiredInvoices': [],
         'ExpiredSum': '0'}))
    extra_params = {'ExpiredInvoices': 0}
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id, extra_params=extra_params)
    utils.check_that(response, deep_equals_to(
        {'UnpaidSum': '0',
         'AvailableSum': '133333.33',
         'Currency': 'RUB',
         'IsPresent': True,
         'SpentSum': '0',
         'LimitSum': '133333.33',
         'ExpiredSum': '0'}))


@pytest.mark.parametrize('context', [DIRECT])
def test_active_contract_unpaid(context):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, context.person_type.code)
    contract_params = {'FIRM': Firms.YANDEX_1.id,
                       'CURRENCY': Currencies.RUB.num_code,
                       'SERVICES': [Services.DIRECT.id],
                       'DT': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'FINISH_DT': NOW_ISO,
                       'IS_SIGNED': PREVIOUS_MONTH_FIRST_DAY_ISO,
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'PERSONAL_ACCOUNT': 1,
                       'PERSONAL_ACCOUNT_FICTIVE': 1,
                       }
    contract_id, person_id = create_contract(agency_id, person_id, ContractCommissionType.PR_AGENCY, contract_params)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(agency_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id, params={'AgencyID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=1,
                                                 contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 100}, 0, NOW)
    act_id = steps.ActsSteps.generate(agency_id, force=1, date=NOW)[0]
    y_invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']
    invoice = db.get_invoice_by_id(y_invoice_id)
    external_id = invoice[0]['external_id']
    invoice_id = invoice[0]['id']
    extra_params = {'UnpaidInvoices': 1}
    response = steps.ContractSteps.get_contract_credits_detailed(contract_id, extra_params=extra_params)
    utils.check_that(response, deep_equals_to(
        {'UnpaidSum': '3000',
         'AvailableSum': '130333.33',
         'IsPresent': True,
         'UnpaidInvoices': [
             {'UnpaidSum': '3000',
              'TotalSum': '3000',
              'PaymentTermDT': steps.PaymentTermSteps.payment_term_with_holidays(10, LAST_DAY_CURRENT_MONTH),
              'ExternalID': external_id,
              'DT': Date.nullify_time_of_date(LAST_DAY_CURRENT_MONTH),
              'ID': invoice_id}],
         'Currency': 'RUB',
         'ExpiredInvoices': [],
         'SpentSum': '3000',
         'LimitSum': '133333.33',
         'ExpiredSum': '0'}))
