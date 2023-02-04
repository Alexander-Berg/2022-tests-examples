# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime
from decimal import Decimal

import pytest
from hamcrest import equal_to

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Firms, ContractPaymentType, Currencies
from btestlib.data.defaults import Order
from temp.igogor.balance_objects import Contexts

DIRECT = Contexts.DIRECT_FISH_RUB_CONTEXT
DIRECT_SW_YT = Contexts.DIRECT_FISH_SW_CHF_YT_CONTEXT
DIRECT_SW = Contexts.DIRECT_FISH_SW_EUR_CONTEXT
DIRECT_US = Contexts.DIRECT_FISH_USD_CONTEXT

ORDER_QTY = Decimal('100')
DT = datetime.datetime.now()
START_DT = utils.Date.nullify_time_of_date(DT).isoformat()
HALF_YEAR_AFTER_NOW_ISO = utils.Date.date_to_iso_format(DT + datetime.timedelta(days=180))
RETRO_DSC = 10

pytestmark = [pytest.mark.tickets('TESTBALANCE-1616'),
              reporter.feature(Features.DISCOUNT, Features.CONTRACT)
              ]


def create_orders_with_campaigns(is_credit, contract_type, context=DIRECT, is_retro_dsc=0, contract_params=None):
    invoice_owner = steps.ClientSteps.create({'IS_AGENCY': 1})
    order_owner = steps.ClientSteps.create({'IS_AGENCY': 0})

    person_id = steps.PersonSteps.create(invoice_owner, context.person_type.code)
    defaullt_contract_params = {
        'CLIENT_ID': invoice_owner,
        'DT': START_DT,
        'IS_SIGNED': START_DT,
        'PERSON_ID': person_id,
        'SERVICES': [context.service.id],
        'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
    }
    defaullt_contract_params.update(contract_params)
    contract_id = steps.ContractSteps.create_contract_new(
        contract_type,
        defaullt_contract_params)[0]
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(order_owner,
                                       service_order_id,
                                       context.product.id,
                                       context.service.id,
                                       {'TEXT': 'Py_Test order',
                                        'AgencyID': invoice_owner,
                                        'ManagerUID': None})
    orders_list = Order.default_orders_list(service_order_id,
                                            service_id=context.service.id,
                                            qty=ORDER_QTY)
    request_id = steps.RequestSteps.create(invoice_owner,
                                           orders_list,
                                           additional_params={'InvoiceDesireDT': DT})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id,
                                                 person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=is_credit,
                                                 contract_id=contract_id,
                                                 )
    if is_credit == 0:
        steps.InvoiceSteps.pay(invoice_id)
    for qty in [ORDER_QTY / 2, ORDER_QTY]:
        steps.CampaignsSteps.do_campaigns(context.service.id,
                                          service_order_id,
                                          campaigns_params={'Bucks': qty},
                                          campaigns_dt=DT)
        act_id = steps.ActsSteps.generate(invoice_owner, 1, DT)[0]
        check_retro_in_act(act_id, is_retro_dsc)

    return invoice_owner, invoice_id, service_order_id, order_id


def check_retro_in_act(act_id, is_retrodsc):
    fact_retrodsc = steps.ActsSteps.get_act_retrodiscount(act_id)
    if is_retrodsc:
        act_amt = steps.ActsSteps.get_act_amount_by_act_id(act_id)[0]
        calc_retro = Decimal(act_amt['amount']) * RETRO_DSC / 100
        utils.check_that(fact_retrodsc['retrodsc'], equal_to(str(calc_retro)))
    else:
        utils.check_that(fact_retrodsc, equal_to(None))


@pytest.mark.parametrize('is_credit, contract_type, context, is_retro_dsc, contract_params',
                         [
                             ##Проверяем, что ретроскидка не расчитывается при выставлении по Комиссионным договорам
                             (0, 'commiss', DIRECT, 0, {'FIRM': Firms.YANDEX_1.id,
                                                        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                        }),
                             ##Проверяем, что ретроскидка не расчитывается при выставлении по договорам с типом Швейрцария: оптовый клиентский
                             (0, 'sw_opt_client', DIRECT_SW_YT, 0, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                    'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                    'CURRENCY': Currencies.CHF.num_code
                                                                    }),
                             ##Проверяем, что ретроскидка не расчитывается при выставлении по договорам с типом США: оптовый агентский
                             (1, 'usa_opt_agency', DIRECT_US, 0, {'FIRM': Firms.YANDEX_INC_4.id,
                                                                  'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                  'CURRENCY': Currencies.USD.num_code,
                                                                  'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                  }),
                             ##Проверяем, что ретроскидка расчитывается при выставлении по постоплатному договору
                             ## с типом Швейцария: оптовый агентский и НЕнулевой ретроскидкой
                             (1, 'sw_opt_agency', DIRECT_SW, 1, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'RETRO_DISCOUNT': 10,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                 }),
                             ##Проверяем, что ретроскидка НЕ расчитывается при выставлении по постоплатному договору
                             ## с типом Швейцария: оптовый агентский и нулевой ретроскидкой
                             (1, 'sw_opt_agency', DIRECT_SW, 0, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'RETRO_DISCOUNT': 0,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                 }),
                             ##Проверяем, что ретроскидка НЕ расчитывается при выставлении по постоплатному договору
                             ##с типом Швейцария: оптовый агентский и с незаданной ретроскидкой
                             (1, 'sw_opt_agency', DIRECT_SW, 0, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                 }),
                             ##Проверяем, что ретроскидка расчитывается при выставлении по ПРЕдоплатному договору
                             ##с типом Швейцария: оптовый агентский и НЕнулевой ретроскидкой
                             (0, 'sw_opt_agency', DIRECT_SW, 1, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'RETRO_DISCOUNT': 10,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                 }),
                             ##Проверяем, что ретроскидка НЕ расчитывается при выставлении по ПРЕдоплатному договору
                             ##с типом Швейцария: оптовый агентский и нулевой ретроскидкой
                             (0, 'sw_opt_agency', DIRECT_SW, 0, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'RETRO_DISCOUNT': 0,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                 }),
                             ##Проверяем, что ретроскидка НЕ расчитывается при выставлении по ПРЕдоплатному договору
                             ##с типом Швейцария: оптовый агентский и с незаданной ретроскидкой
                             (0, 'sw_opt_agency', DIRECT_SW, 0, {'FIRM': Firms.EUROPE_AG_7.id,
                                                                 'PAYMENT_TYPE': ContractPaymentType.PREPAY,
                                                                 'CURRENCY': Currencies.EUR.num_code,
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                                 }),
                         ])
def test_retrodiscount(is_credit, contract_type, context, is_retro_dsc, contract_params):
    create_orders_with_campaigns(is_credit, contract_type, context, is_retro_dsc, contract_params)
