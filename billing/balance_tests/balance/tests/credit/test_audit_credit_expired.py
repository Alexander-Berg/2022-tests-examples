# coding: utf-8
__author__ = 'atkaya'

import datetime
from datetime import timedelta

import pytest

from hamcrest import equal_to

from balance import balance_steps as steps
import btestlib.utils as utils
from balance import balance_db as db
import btestlib.reporter as reporter
from btestlib.constants import Services, Currencies, \
    Firms, PersonTypes, Products, ContractCommissionType, Paysyses
from temp.igogor.balance_objects import Contexts
from balance.features import AuditFeatures

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                               contract_type=ContractCommissionType.NO_AGENCY)
DIRECT_SW_FIRM_USD = DIRECT_YANDEX_FIRM_FISH.new(firm=Firms.EUROPE_AG_7, currency=Currencies.USD,
                                                person_type=PersonTypes.SW_UR, paysys=Paysyses.BANK_SW_UR_USD,
                                                contract_type=ContractCommissionType.SW_OPT_CLIENT)
MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                        firm=Firms.MARKET_111,
                                                        contract_type=ContractCommissionType.NO_AGENCY)

@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11))
@pytest.mark.parametrize('context', [
        pytest.param(DIRECT_YANDEX_FIRM_FISH, id='Direct RU'),
        pytest.param(MARKET_FIRM_FISH, id='Market'),
        pytest.param(DIRECT_SW_FIRM_USD, id='Direct SW')
                         ])
def test_expired_payment(context):
    old_pa = False
    if context.firm == Firms.EUROPE_AG_7:
        old_pa = True
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(context, postpay=True,
                                                                                                  old_pa=old_pa)
    orders_list, act_id = create_invoice_and_act(context, client_id, person_id, contract_id)
    act_expiration(act_id)
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id)
    except Exception, exc:
        with reporter.step(u'Проверяем, что новый счет не выставился, т.к. есть просроченная оплата по кредиту.'):
            utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'CREDIT_LIMIT_EXEEDED'))


@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_11))
@pytest.mark.parametrize('context', [
        pytest.param(DIRECT_YANDEX_FIRM_FISH, id='Direct RU'),
        pytest.param(MARKET_FIRM_FISH, id='Market'),
        pytest.param(DIRECT_SW_FIRM_USD, id='Direct SW')
                         ])
def test_credit_limit_exceeded(context):
    old_pa = False
    if context.firm == Firms.EUROPE_AG_7:
        old_pa = True
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(context, postpay=True,
                                                                                                  old_pa=old_pa,
                                                                                                  additional_params={'CREDIT_LIMIT_SINGLE': 1})
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100500, 'BeginDT': datetime.datetime.now()}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id)
    except Exception, exc:
        with reporter.step(u'Проверяем, что новый счет не выставился, т.к. превышен лимит кредита.'):
            utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'CREDIT_LIMIT_EXEEDED'))


#-------------UTILS------------------------------------------------------------

def create_invoice_and_act(context, client_id, person_id, contract_id, qty=1):
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id=client_id, product_id=context.product.id, service_id=context.service.id,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': datetime.datetime.now()}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {context.product.type.code: qty}, 0)
    act_id = steps.ActsSteps.generate(client_id, force=1)[0]
    return orders_list, act_id

def act_expiration(act_id):
    with reporter.step(u'Делаем акт просроченным'):
        steps.ActsSteps.set_payment_term_dt(act_id, utils.Date.nullify_time_of_date(datetime.datetime.today()) - timedelta(days=1))