# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime

import pytest
import balance.balance_db as db
from hamcrest import equal_to
from decimal import Decimal
from dateutil.relativedelta import relativedelta
from btestlib.data.defaults import Date

import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance import balance_web as web
from balance.features import Features
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, ContractCommissionType, \
    Currencies

MAIN_DT = datetime.datetime.now()

SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                   Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                   Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]

DIRECT_CONTEXT_FIRM_4 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_INC_4,
                                                             person_type=PersonTypes.USU,
                                                             paysys=Paysyses.BANK_US_UR_USD,
                                                             contract_type=ContractCommissionType.USA_OPT_AGENCY,
                                                             contract_services=SERVICES_DIRECT,
                                                             currency=Currencies.USD.num_code,
                                                             is_agent=1,
                                                             additional_params={
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                             },
                                                             )
DIRECT_CONTEXT_FIRM_7 = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                             person_type=PersonTypes.SW_UR,
                                                             paysys=Paysyses.BANK_SW_UR_CHF,
                                                             contract_type=ContractCommissionType.SW_OPT_AGENCY,
                                                             contract_services=SERVICES_DIRECT,
                                                             currency=Currencies.CHF.num_code,
                                                             is_agent=1,
                                                             additional_params={
                                                                 'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0
                                                             }
                                                             )
MARKET_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.MARKET_111,
                                                               person_type=PersonTypes.UR,
                                                               paysys=Paysyses.BANK_UR_RUB_MARKET,
                                                               contract_services=[Services.MARKET.id],
                                                               contract_type=ContractCommissionType.OPT_CLIENT,
                                                               currency=Currencies.RUB.num_code,
                                                               is_agent=0,
                                                               additional_params={})

TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))
CONTRACT_END_DT = utils.Date.date_to_iso_format(TODAY + relativedelta(months=3))

CREDIT_LIMIT = Decimal('1000')
QTY = Decimal('50')


@pytest.mark.parametrize('context', [
    DIRECT_CONTEXT_FIRM_7,
    DIRECT_CONTEXT_FIRM_4,
    MARKET_MARKET_FIRM_FISH
],
                         ids=[
                             'Firm 7',
                             'Firm 4',
                             'Firm 111'])
def test_paystep_overdue_warning_personal_acc(context, get_free_user):
    client_id = steps.ClientSteps.create({'IS_AGENCY': context.is_agent})
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT,
                       'SERVICES': context.contract_services,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(context.currency),
                       'FIRM': context.firm.id,
                       }
    contract_params.update(context.additional_params)

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type, contract_params)

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': MAIN_DT}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)

    # проверяем под админом, что нет кнопки
    with web.Driver() as driver:
        paypreview_page = web.ClientInterface.PaypreviewPage.open(driver, request_id=request_id, person_id=person_id,
                                                                  paysys_id=context.paysys.id, contract_id=contract_id)
        utils.check_that(paypreview_page.is_generate_invoice_button_present(), equal_to(False),
                         u'Проверяем, что на странице нет кнопки "Выставить счет" (под админом)')

    # проверяем под клиентом, что нет кнопки
    # раскомментировать, когда либо научимся обходить капчу, либо будем ходить в тестовый паспорт
    # with web.Driver(user=user) as driver:
    #     paypreview_page = web.ClientInterface.PaypreviewPage.open(driver, request_id=request_id, person_id=person_id,
    #                                                               paysys_id=context.paysys.id, contract_id=contract_id)
    #     utils.check_that(paypreview_page.is_generate_invoice_button_present(), equal_to(False),
    #                      u'Проверяем, что на странице нет кнопки "Выставить счет" (под клиентом)')
