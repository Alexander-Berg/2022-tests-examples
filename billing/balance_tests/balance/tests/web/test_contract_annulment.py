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
    Currencies, Collateral


MAIN_DT = datetime.datetime.now()

SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                   Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                   Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]

MAIN_DT = datetime.datetime.now()

DIRECT_CONTEXT_AGENCY = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                             contract_type=ContractCommissionType.OPT_AGENCY,
                                                             special_contract_params={'DISCOUNT_POLICY_TYPE': 7,},
                                                             is_agency=1)
DIRECT_CONTEXT_CLIENT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                             contract_type=ContractCommissionType.PARTNER,
                                                             special_contract_params={},
                                                             is_agency=0)
QTY = Decimal('50')
TODAY = utils.Date.nullify_time_of_date(datetime.datetime.today())
CONTRACT_START_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=4))
CONTRACT_END_DT = utils.Date.date_to_iso_format(TODAY + relativedelta(months=3))
ALMOST_OVERDUE_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=10))
OVERDUE_DT = utils.Date.date_to_iso_format(TODAY - relativedelta(months=3) - relativedelta(days=15))


@pytest.mark.parametrize('context', [
    DIRECT_CONTEXT_AGENCY,
    DIRECT_CONTEXT_CLIENT,
                        ],)
def test_contract_annulment(context, get_free_user):
    client_id = steps.ClientSteps.create({'IS_AGENCY': context.is_agency})
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 2,
                       'CREDIT_TYPE': 2,
                       'SERVICES': SERVICES_DIRECT,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       }
    contract_params.update(context.special_contract_params)

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(context.contract_type,
                                                                        contract_params)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': MAIN_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id, contract_id=contract_id)
    with web.Driver() as driver:
        contract_page = web.AdminInterface.ContractEditPage.open(driver, contract_id=contract_id)
        utils.check_that(contract_page.get_cancelled_checkbox_attributes().get_attribute('style'), 'display: none;',
                         u'Проверяем, что галка "Аннулирован" скрыта')


def test_contract_and_collateral_annulment(get_free_user):
    context = DIRECT_CONTEXT_CLIENT
    client_id = steps.ClientSteps.create()
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 2,
                       'CREDIT_TYPE': 2,
                       'SERVICES': SERVICES_DIRECT,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       }
    contract_params.update(context.special_contract_params)

    contract_id, contract_eid = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY,
                                                                        contract_params)
    collateral_params = {'CONTRACT2_ID': contract_id,
                         'DT': CONTRACT_START_DT,
                         'FINISH_DT': CONTRACT_END_DT,
                         'IS_SIGNED': CONTRACT_START_DT}
    steps.ContractSteps.create_collateral(Collateral.PROLONG, collateral_params)

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': MAIN_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id, contract_id=contract_id)
    with web.Driver() as driver:
        contract_page = web.AdminInterface.ContractEditPage.open(driver, contract_id=contract_id)
        utils.check_that(contract_page.get_cancelled_checkbox_attributes().get_attribute('style'), 'display: none;',
                         u'Проверяем, что галка "Аннулирован" скрыта')


def test_several_contracts(get_free_user):
    context = DIRECT_CONTEXT_AGENCY
    client_id = steps.ClientSteps.create({'IS_AGENCY': context.is_agency})
    user = get_free_user()
    steps.ClientSteps.link(client_id, user.login)
    person_id_1 = steps.PersonSteps.create(client_id, context.person_type.code)
    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id_1,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 2,
                       'CREDIT_TYPE': 2,
                       'SERVICES': SERVICES_DIRECT,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'COMMISSION': 1,
                       'COMMISSION_TYPE': 47,
                       'DISCARD_NDS': 0,
                       'NEW_COMMISSIONER_REPORT': 0,
                       }
    contract_id_1, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.COMMISS,
                                                                        contract_params)
    person_id_2 = steps.PersonSteps.create(client_id, context.person_type.code)
    contract_params = {'CLIENT_ID': client_id,
                       'PERSON_ID': person_id_2,
                       'DT': CONTRACT_START_DT,
                       'FINISH_DT': CONTRACT_END_DT,
                       'IS_SIGNED': CONTRACT_START_DT,
                       'PAYMENT_TYPE': 2,
                       'CREDIT_TYPE': 2,
                       'SERVICES': SERVICES_DIRECT,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'COMMISSION': 1,
                       'COMMISSION_TYPE': 47,
                       'DISCARD_NDS': 0,
                       'NEW_COMMISSIONER_REPORT': 0,
                       }
    contract_id_2, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.COMMISS,
                                                                        contract_params)

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id)
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
         'BeginDT': MAIN_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id, contract_id=contract_id_1)
    with web.Driver() as driver:
        contract_page = web.AdminInterface.ContractEditPage.open(driver, contract_id=contract_id_1)
        utils.check_that(contract_page.get_cancelled_checkbox_attributes().get_attribute('style'), 'display: none;',
                         u'Проверяем, что галка "Аннулирован" скрыта')

    with web.Driver() as driver:
        contract_page = web.AdminInterface.ContractEditPage.open(driver, contract_id=contract_id_2)
        utils.check_that(contract_page.get_cancelled_checkbox_attributes().get_attribute('style'), equal_to(''),
                         u'Проверяем, что галка "Аннулирован" скрыта')