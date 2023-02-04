# coding=utf-8
__author__ = 'atkaya'

import pytest

from balance import balance_steps as steps
from btestlib.data.partner_contexts import *
import btestlib.utils as utils
from temp.igogor.balance_objects import Contexts
from decimal import Decimal as D
from balance.real_builders import common_defaults
from btestlib.data import defaults

MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                        firm=Firms.MARKET_111)
DIRECT_FISH_FIRM = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                        paysys=Paysyses.BANK_UR_RUB)


@pytest.mark.parametrize('context, is_offer, login',
                         [(TAXI_RU_CONTEXT, 1, 'yndx-static-yb-printforms-1'),
                          (TAXI_BV_GEO_USD_CONTEXT, 0, 'yndx-static-yb-printforms-2'),
                          (TAXI_BV_LAT_EUR_CONTEXT, 0, 'yndx-static-yb-printforms-3'),
                          (TAXI_UBER_BV_BY_BYN_CONTEXT, 1, 'yndx-static-yb-printforms-4'),
                          (TAXI_UBER_BV_AZN_USD_CONTEXT, 0, 'yndx-static-yb-printforms-5'),
                          (TAXI_ISRAEL_CONTEXT, 0, 'yndx-static-yb-printforms-6'),
                          (TAXI_GHANA_USD_CONTEXT, 0, 'yndx-static-yb-printforms-7'),
                          (TAXI_ZA_USD_CONTEXT, 0, 'yndx-static-yb-printforms-8')])
def test_taxi_printform(context, is_offer, login):
    today = utils.Date.date_to_iso_format(datetime.today())
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_offer=is_offer,
                                                                                       is_postpay=0,
                                                                                       additional_params={
                                                                                           'start_dt': today})

    steps.ClientSteps.link(client_id, login)

    cash_product = Taxi.CURRENCY_TO_PRODUCT[context.currency]['cash']

    service_order_id = steps.OrderSteps.next_id(Taxi.CASH_SERVICE_ID)
    steps.OrderSteps.create(client_id, service_order_id, product_id=cash_product,
                            service_id=Taxi.CASH_SERVICE_ID)

    orders_list = [{'ServiceID': Taxi.CASH_SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': D('50.11'),
                    'BeginDT': today}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    charge_note_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, contract_id=contract_id)


@pytest.mark.parametrize('context, person_params, paysys, login, overdraft',
                         [(DIRECT_FISH_FIRM, common_defaults.FIXED_PH_PARAMS, Paysyses.BANK_PH_RUB,
                           'yndx-static-yb-printforms-9', 0),
                          (DIRECT_FISH_FIRM, common_defaults.FIXED_UR_PARAMS, Paysyses.BANK_UR_RUB,
                           'yndx-static-yb-printforms-10', 0),
                          (MARKET_FIRM_FISH, common_defaults.FIXED_UR_PARAMS, Paysyses.BANK_UR_RUB_MARKET,
                           'yndx-static-yb-printforms-11', 0),
                          (DIRECT_FISH_FIRM, common_defaults.FIXED_PH_PARAMS, Paysyses.BANK_PH_RUB,
                           'yndx-static-yb-printforms-12', 1),
                          (DIRECT_FISH_FIRM, common_defaults.FIXED_UR_PARAMS, Paysyses.BANK_UR_RUB,
                           'yndx-static-yb-printforms-13', 1),
                          (MARKET_FIRM_FISH, common_defaults.FIXED_UR_PARAMS, Paysyses.BANK_UR_RUB_MARKET,
                           'yndx-static-yb-printforms-14', 1),
                          ],
                         ids=['Direct PH prepayment',
                              'Direct UR prepayment',
                              'Market UR prepayment',
                              'Direct PH overdraft',
                              'Direct UR overdraft',
                              'Market UR overdraft', ])
def test_offer_printform(context, person_params, paysys, login, overdraft):
    qty = 50
    today = utils.Date.date_to_iso_format(datetime.today())
    client_id = steps.ClientSteps.create(params={'NAME': ''})
    steps.ClientSteps.link(client_id, login)
    if overdraft:
        steps.ClientSteps.set_force_overdraft(client_id, context.service.id, 100000000, context.firm.id)
    person_id = steps.PersonSteps.create(client_id, person_params['type'])
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': today}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=today))

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                           paysys_id=paysys.id, overdraft=overdraft)


def test_adfox_printform():
    context = ADFOX_RU_CONTEXT
    _, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
        utils.Date.previous_three_months_start_end_dates()
    adfox_products = [
        {u'product_id': defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN, u'scale': 'adfox_mobile_test_scale', u'account': '3'},
        {u'product_id': defaults.ADFox.PRODUCT_ADFOX_MOBILE_DEFAULT, u'scale': 'adfox_mobile_default_test_scale',
         u'account': ''},
        {u'product_id': defaults.ADFox.PRODUCT_ADFOX_UNIT_PRODUCT, u'scale': 'adfox_sites_requests_test_scale',
         u'account': '4'}
    ]
    client_id, _, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params={'start_dt': first_month_start_dt,
                                                                                'adfox_products': adfox_products})
    steps.ClientSteps.link(client_id, 'yndx-static-yb-printforms-15')

    steps.PartnerSteps.create_adfox_completion(contract_id, first_month_start_dt,
                                               product_id=defaults.ADFox.PRODUCT_ADFOX_MOBILE_MAIN,
                                               requests=defaults.ADFox.DEFAULT_REQUESTS_WO_ORDER,
                                               shows=defaults.ADFox.DEFAULT_SHOWS,
                                               units=0)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, first_month_end_dt)


@pytest.mark.parametrize('context, person_params, login',
                         [
                             (Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                                   person_type=PersonTypes.SW_UR,
                                                                   paysys=Paysyses.BANK_SW_UR_CHF,
                                                                   contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                                   currency=Currencies.CHF.num_code),
                              common_defaults.FIXED_SW_UR_PARAMS,
                              'yndx-static-yb-printforms-16'),
                             (Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                                   person_type=PersonTypes.USU,
                                                                   paysys=Paysyses.BANK_US_UR_USD,
                                                                   contract_type=ContractCommissionType.USA_OPT_CLIENT,
                                                                   currency=Currencies.USD.num_code),
                              common_defaults.FIXED_USU_PARAMS,
                              'yndx-static-yb-printforms-17'),
                             (Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                                   firm=Firms.REKLAMA_BEL_27,
                                                                   person_type=PersonTypes.BYU,
                                                                   paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                                   currency=Currencies.BYN.num_code,
                                                                   contract_type=ContractCommissionType.BEL_NO_AGENCY),
                              common_defaults.FIXED_BYU_PARAMS,
                              'yndx-static-yb-printforms-18'),
                             (Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.VERTICAL_12,
                                                                   product=Products.AUTORU_505123,
                                                                   region=Regions.RU, currency=Currencies.RUB.num_code,
                                                                   service=Services.AUTORU, nds=Nds.YANDEX_RESIDENT,
                                                                   contract_type=ContractCommissionType.AUTO_NO_AGENCY),
                              common_defaults.FIXED_UR_PARAMS,
                              'yndx-static-yb-printforms-19'),
                             (Contexts.DIRECT_FISH_KZ_CONTEXT.new(firm=Firms.KZ_25, currency=Currencies.KZT.num_code,
                                                                  contract_type=ContractCommissionType.NO_AGENCY),
                              common_defaults.FIXED_KZU_PARAMS,
                              'yndx-static-yb-printforms-20')
                         ],
                         ids=[
                             'Direct Firm 7 prepayment',
                             'Direct Firm 4 prepayment',
                             'Direct Firm 27 prepayment',
                             'Direct Firm 12 prepayment',
                             'Direct Firm 25 prepayment'])
def test_printform_with_contract(context, person_params, login):
    qty = 50
    today = utils.Date.date_to_iso_format(datetime.today())

    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': today,
                       'IS_SIGNED': today,
                       'PAYMENT_TYPE': 2,
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 0,
                       'CURRENCY': str(context.currency),
                       'FIRM': context.firm.id,
                       }

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
         'BeginDT': today},
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, credit=0,
                                                 contract_id=contract_id)
