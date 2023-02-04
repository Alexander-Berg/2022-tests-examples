# coding=utf-8
__author__ = 'atkaya'

from dateutil.relativedelta import relativedelta
from datetime import datetime

import pytest

from balance import balance_steps as steps
from balance import balance_api as api
from btestlib.constants import PersonTypes, Services, Paysyses, Products, User, \
    Currencies, ContractCommissionType, Firms
import balance.balance_db as db
from btestlib.data.defaults import Date
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED
import btestlib.utils as utils
from temp.igogor.balance_objects import Contexts
from decimal import Decimal as D

LOGIN_INVOICE_LINKS_CHECKS = User(1153412153, 'yndx-static-balance-4')
LOGIN_INVOICE_WITH_ACT_PAGE = User(1207059764, 'yndx-static-balance-7')
LOGIN_OVERDUE_OVERDRAFT = User(1207059797, 'yndx-static-balance-8')
LOGIN_INVOICE_GENERAL_CHECKS = User(1207059849, 'yndx-static-balance-9')
LOGIN_INVOICE_DETAILS_CHECKS = User(1217923750, 'yndx-static-balance-10')
LOGIN_210_OPERATIONS_CHECKS = User(1217923768, 'yndx-static-balance-11')
LOGIN_VARIOUS_OPERATIONS_CHECKS = User(1217923778, 'yndx-static-balance-12')
LOGIN_Y_INVOICE = User(1217923792, 'yndx-static-balance-13')
LOGIN_CHARGE_NOTE = User(1217923804, 'yndx-static-balance-14')


# для кейсов на проверку реквизитов на странице счета
@pytest.mark.parametrize('i', range(1, 101))
def test_create_invoice_with_yt(i):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'yndx-tst-ytpayer-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.YT.code)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                              product_id=Products.DIRECT_FISH.id,
                                              service_id=Services.DIRECT.id)
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100,
         'BeginDT': datetime.today()}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=datetime.today()))
    invoice_id_rub, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_YT_RUB.id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    invoice_id_eur, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_YT_EUR.id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    invoice_id_usd, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_YT_USD.id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)


# для кейсов на замену ru.yandex.autotests.balance.tests.bankDetails.invoicesWithContract
@pytest.mark.parametrize('i', range(1, 11))
def test_create_invoice_with_contract_eur_test(i):
    # EurTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.YT.code)
    contract_type = ContractCommissionType.PR_AGENCY
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.EUR.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'DISCOUNT_FIXED': 12,
        'DEAL_PASSPORT': Date.TODAY_ISO,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)

    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                            product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id,
                            params={'AgencyID': client_id})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
         'AgencyID': client_id}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_eur, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_YT_EUR.id,
                                                     credit=0, contract_id=contract_id, overdraft=0,
                                                     endbuyer_id=None)


@pytest.mark.parametrize('i', range(11, 21))
def test_create_invoice_with_contract_ruring_test(i):
    # RurIngTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    contract_type = ContractCommissionType.COMMISS
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.RUB.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'COMMISSION_TYPE': 47,
        'BANK_DETAILS_ID': 21
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                              product_id=Products.DIRECT_FISH.id,
                                              service_id=Services.DIRECT.id,
                                              params={'AgencyID': client_id})
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
                    'AgencyID': client_id}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_rub, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_UR_RUB.id, credit=0,
                                                     contract_id=contract_id, overdraft=0, endbuyer_id=None)


@pytest.mark.parametrize('i', range(21, 31))
def test_create_invoice_with_contract_ruropen_test(i):
    # RurOtkritieTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    contract_type = ContractCommissionType.COMMISS
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.RUB.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'COMMISSION_TYPE': 47,
        'BANK_DETAILS_ID': 1
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                              product_id=Products.DIRECT_FISH.id,
                                              service_id=Services.DIRECT.id,
                                              params={'AgencyID': client_id})
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
                    'AgencyID': client_id}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_rub, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_UR_RUB.id, credit=0,
                                                     contract_id=contract_id, overdraft=0, endbuyer_id=None)


@pytest.mark.parametrize('i', range(31, 41))
def test_create_invoice_with_contract_rurraif_test(i):
    # RurRaiffTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    contract_type = ContractCommissionType.COMMISS
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.RUB.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'COMMISSION_TYPE': 47,
        'BANK_DETAILS_ID': 3
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                              product_id=Products.DIRECT_FISH.id,
                                              service_id=Services.DIRECT.id,
                                              params={'AgencyID': client_id})
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
                    'AgencyID': client_id}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_rub, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_UR_RUB.id, credit=0,
                                                     contract_id=contract_id, overdraft=0, endbuyer_id=None)


@pytest.mark.parametrize('i', range(41, 51))
def test_create_invoice_with_contract_rursber_test(i):
    # RurSberTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    contract_type = ContractCommissionType.COMMISS
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.RUB.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'COMMISSION_TYPE': 47,
        'BANK_DETAILS_ID': 2
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                              product_id=Products.DIRECT_FISH.id,
                                              service_id=Services.DIRECT.id,
                                              params={'AgencyID': client_id})
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
                    'AgencyID': client_id}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_rub, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_UR_RUB.id, credit=0,
                                                     contract_id=contract_id, overdraft=0, endbuyer_id=None)


@pytest.mark.parametrize('i', range(51, 61))
def test_create_invoice_with_contract_usdopen_test(i):
    # UsdOtkritieTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.YT.code)
    contract_type = ContractCommissionType.PR_AGENCY
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.USD.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'DISCOUNT_FIXED': 12,
        'DEAL_PASSPORT': Date.TODAY_ISO,
        'BANK_DETAILS_ID': 4
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)

    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                            product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id,
                            params={'AgencyID': client_id})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
         'AgencyID': client_id}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_usd, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_YT_USD.id,
                                                     credit=0, contract_id=contract_id, overdraft=0,
                                                     endbuyer_id=None)


@pytest.mark.parametrize('i', range(61, 71))
def test_create_invoice_with_contract_usdraif_test(i):
    # UsdRaiffTest
    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(client_id, 'yndx-tst-invcontr-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.YT.code)
    contract_type = ContractCommissionType.PR_AGENCY
    SERVICES_DIRECT = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                       Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                       Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]

    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'CURRENCY': Currencies.USD.num_code,
        'PAYMENT_TYPE': 2,
        'SERVICES': SERVICES_DIRECT,
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'PAYMENT_TERM': 0,
        'DISCOUNT_FIXED': 12,
        'DEAL_PASSPORT': Date.TODAY_ISO,
        'BANK_DETAILS_ID': 5
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(contract_type, contract_params)

    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                            product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id,
                            params={'AgencyID': client_id})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
         'AgencyID': client_id}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id_usd, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                     paysys_id=Paysyses.BANK_YT_USD.id,
                                                     credit=0, contract_id=contract_id, overdraft=0,
                                                     endbuyer_id=None)


def test_create_invoice_links_checks():
    client_id = steps.ClientSteps.create({'NAME': ''})

    steps.UserSteps.link_user_and_client(LOGIN_INVOICE_LINKS_CHECKS, client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={'name': u'ООО "Плательщик"'})
    dt = datetime.now()

    # оплаченный и заакченный заказ по директу
    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    order_id_direct = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                                              product_id=Products.DIRECT_FISH.id,
                                              service_id=Services.DIRECT.id,
                                              params={'Text': u'Рекламная кампания РРС'})
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)


def test_invoice_with_act_page():
    client_id = steps.ClientSteps.create({'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_INVOICE_WITH_ACT_PAGE, client_id)

    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.EUROPE_AG_7,
                                                   person_type=PersonTypes.SW_UR,
                                                   paysys=Paysyses.BANK_SW_UR_CHF,
                                                   contract_type=ContractCommissionType.SW_OPT_CLIENT,
                                                   currency=Currencies.CHF.num_code)

    today = utils.Date.date_to_iso_format(datetime.today())
    credit_limit_usd = D('1000')
    qty = D('50')

    contract_params = {'DT': today,
                       'IS_SIGNED': today,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': credit_limit_usd,
                       'SERVICES': [context.service.id],
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(context.currency),
                       'FIRM': context.firm.id,
                       }
    person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_UR.code, params={u'name': u'Swiss legal Payer',
                                                                                    u'account': u'58605',
                                                                                    u'inn': u'535904'})
    contract_params.update({'CLIENT_ID': client_id,
                            'PERSON_ID': person_id})
    contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_CLIENT,
                                                                                contract_params)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'ManagerUID': context.manager.uid})
    request_id = steps.RequestSteps.create(client_id, [{'ServiceID': context.service.id,
                                                        'ServiceOrderID': service_order_id, 'Qty': qty,
                                                        'BeginDT': today}],
                                           additional_params=dict(InvoiceDesireDT=today))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, contract_id=contract_id,
                                                 credit=2)
    steps.InvoiceSteps.turn_on(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_id,
                                      {context.product.type.code: qty}, 0, today)
    steps.ActsSteps.generate(client_id, force=1, date=today)


def test_overdue_overdraft_invoice():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT
    today = utils.Date.date_to_iso_format(datetime.today())
    qty = D('50')

    client_id = steps.ClientSteps.create({'NAME': 'Клиент Клиентович'})
    steps.UserSteps.link_user_and_client(LOGIN_OVERDUE_OVERDRAFT, client_id)

    steps.ClientSteps.set_force_overdraft(client_id, context.service.id, 100000000)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id, product_id=context.product.id,
                            params={'ManagerUID': context.manager.uid})
    request_id = steps.RequestSteps.create(client_id, [{'ServiceID': context.service.id,
                                                        'ServiceOrderID': service_order_id, 'Qty': qty,
                                                        'BeginDT': today}],
                                           additional_params=dict(InvoiceDesireDT=today))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id, overdraft=1)
    steps.InvoiceSteps.set_dt(invoice_id, datetime(2020, 10, 1))
    steps.InvoiceSteps.set_payment_term_dt(invoice_id, datetime(2020, 10, 15))
    steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_id,
                                      {'Bucks': qty, 'Money': 0}, 0, today)
    steps.ActsSteps.generate(client_id, force=1, date=today)


def test_general_checks_invoice():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT
    today = utils.Date.date_to_iso_format(datetime.today())
    qty = D('50')

    client_id = steps.ClientSteps.create({'NAME': 'Клиент Клиентович'})
    steps.UserSteps.link_user_and_client(LOGIN_INVOICE_GENERAL_CHECKS, client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={u'name': u'ООО "Плательщик"',
                                                                                 u'inn': u'7838298476',
                                                                                 u'account': u'40702810982208554168',
                                                                                 u'fname': u'',
                                                                                 u'lname': u'',
                                                                                 u'mname': u''})
    order_list = []
    service_order_list = []
    for i in range(5):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=context.product.id, params={'ManagerUID': context.manager.uid})
        service_order_list.append(service_order_id)
        order_list.append({'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty - 2 * i,
                           'BeginDT': today})
    request_id = steps.RequestSteps.create(client_id, order_list, additional_params=dict(InvoiceDesireDT=today))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
    steps.InvoiceSteps.pay(invoice_id)
    for i in range(len(service_order_list)):
        steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_list[i],
                                          {'Bucks': qty - 3 * i, 'Money': 0}, 0, today)
    steps.ActsSteps.generate(client_id, force=1, date=today)


def test_details_checks_invoice():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT
    today = utils.Date.date_to_iso_format(datetime.today())
    qty = D('50')

    client_id = steps.ClientSteps.create({'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_INVOICE_DETAILS_CHECKS, client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={u'name': u'ООО "Плательщик"',
                                                                                 u'inn': u'7838298476',
                                                                                 u'account': u'40702810982208554168',
                                                                                 u'fname': u'',
                                                                                 u'lname': u'',
                                                                                 u'mname': u''})
    order_list = []
    service_order_list = []
    for i in range(14):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=context.product.id, params={'ManagerUID': context.manager.uid})
        service_order_list.append(service_order_id)
        order_list.append({'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty - 2 * i,
                           'BeginDT': today})
    request_id = steps.RequestSteps.create(client_id, order_list, additional_params=dict(InvoiceDesireDT=today))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
    steps.InvoiceSteps.pay(invoice_id)
    for i in range(3):
        steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_list[i],
                                          {'Bucks': qty - 10 * i, 'Money': 0}, 0, today)
    steps.ActsSteps.generate(client_id, force=1, date=today)
    steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_list[4],
                                      {'Bucks': 25, 'Money': 0}, 0, today)
    consumes = steps.ConsumeSteps.get_consumes_by_client_id_sorted_by_sum(client_id)
    # апдейтим даты консьюмов. чтобы зафиксировать порядок
    for i in range(14):
        query = 'UPDATE T_CONSUME SET DT = :consume_dt WHERE ID = :consume_id'
        params = {'consume_dt': datetime.today() - relativedelta(days=i), 'consume_id': consumes[i]['id']}
        db.balance().execute(query, params)


@pytest.mark.parametrize('i', range(1, 11))
def test_create_repayment(i):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0})

    steps.ClientSteps.link(client_id, 'yndx-static-yb-repayment-' + str(i))
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    SERVICES = [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
    CREDIT_LIMIT_RUB = D('5700')

    contract_params = {'DT': Date.TODAY_ISO,
                       'IS_SIGNED': Date.TODAY_ISO,
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': CREDIT_LIMIT_RUB,
                       'SERVICES': SERVICES,
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'CLIENT_ID': client_id,
                       'PERSON_ID': person_id,
                       }

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY, contract_params)
    steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)

    service_order_id_direct = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_direct,
                            product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list = [
        {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_direct, 'Qty': 50,
         'AgencyID': client_id}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    fictive_invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                         paysys_id=context.paysys.id,
                                                         credit=2, contract_id=contract_id, overdraft=0,
                                                         endbuyer_id=None)
    repayment_invoice_id_1 = steps.InvoiceSteps.make_repayment_invoice(fictive_invoice_id,
                                                                       with_confirmation=False)


@pytest.mark.parametrize('i', range(1, 11))
def test_free_funds(i):
    context = Contexts.DIRECT_FISH_RUB_CONTEXT
    today = utils.Date.date_to_iso_format(datetime.today())
    qty = D('50')
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': ''})
    steps.ClientSteps.link(client_id, 'yndx-static-yb-free-funds-' + str(i))

    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={u'name': u'ООО "Плательщик"',
                                                                                 u'inn': u'7838298476',
                                                                                 u'account': u'40702810982208554168',
                                                                                 u'fname': u'',
                                                                                 u'lname': u'',
                                                                                 u'mname': u''})

    order_list = []
    service_order_list = []
    for i in range(5):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                product_id=context.product.id, params={'ManagerUID': context.manager.uid})
        service_order_list.append(service_order_id)
        order_list.append({'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty - 2 * i,
                           'BeginDT': today})
    request_id = steps.RequestSteps.create(client_id, order_list, additional_params=dict(InvoiceDesireDT=today))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
    steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=10000)
    for i in range(len(service_order_list)):
        steps.CampaignsSteps.do_campaigns(context.product.service.id, service_order_list[i],
                                          {'Bucks': qty - 3 * i, 'Money': 0}, 0, today)
    steps.ActsSteps.generate(client_id, force=1, date=today)


def test_many_operations():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT
    today = utils.Date.date_to_iso_format(datetime.today())
    qty = D('50')
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_210_OPERATIONS_CHECKS, client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={u'name': u'ООО "Плательщик"',
                                                                                 u'inn': u'7838298476',
                                                                                 u'account': u'40702810982208554168',
                                                                                 u'fname': u'',
                                                                                 u'lname': u'',
                                                                                 u'mname': u''})

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id, params={'ManagerUID': context.manager.uid})
    request_id = steps.RequestSteps.create(client_id, [{'ServiceID': context.service.id,
                                                        'ServiceOrderID': service_order_id, 'Qty': qty,
                                                        'BeginDT': today}],
                                           additional_params=dict(InvoiceDesireDT=today))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
    for i in range(211):
        steps.InvoiceSteps.pay(invoice_id, payment_sum=i)


def test_various_operations():
    context = Contexts.DIRECT_FISH_RUB_CONTEXT
    today = utils.Date.date_to_iso_format(datetime.today())
    qty = D('50')
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_VARIOUS_OPERATIONS_CHECKS, client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={u'name': u'ООО "Плательщик"',
                                                                                 u'inn': u'7838298476',
                                                                                 u'account': u'40702810982208554168',
                                                                                 u'fname': u'',
                                                                                 u'lname': u'',
                                                                                 u'mname': u''})
    order_list = []
    service_order_list = []
    for i in range(3):
        service_order_id = steps.OrderSteps.next_id(context.service.id)
        order_id_from = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                                product_id=context.product.id,
                                                params={'ManagerUID': context.manager.uid})
        service_order_list.append(service_order_id)
        order_list.append({'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty - 2 * i,
                           'BeginDT': today})
    request_id = steps.RequestSteps.create(client_id, order_list, additional_params=dict(InvoiceDesireDT=today))
    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
    steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=5000)

    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id_to = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                          product_id=context.product.id, params={'ManagerUID': context.manager.uid})
    service_order_list.append(service_order_id)
    request_id = steps.RequestSteps.create(client_id, order_list, additional_params=dict(InvoiceDesireDT=today))
    invoice_id_to, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id)
    steps.OrderSteps.transfer([{'order_id': order_id_from, 'qty_old': 46, 'qty_new': 36, 'all_qty': 0}],
                              [{'order_id': order_id_to, 'qty_delta': 10}])


def test_y_invoice():
    qty = 50
    context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                   paysys=Paysyses.BANK_UR_RUB)
    today = utils.Date.date_to_iso_format(datetime.today())

    client_id = steps.ClientSteps.create(params={'IS_AGENCY': 0, 'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_Y_INVOICE, client_id)
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params={u'name': u'ООО "Плательщик"',
                                                                                 u'inn': u'7838298476',
                                                                                 u'account': u'40702810982208554168',
                                                                                 u'fname': u'',
                                                                                 u'lname': u'',
                                                                                 u'mname': u''})

    contract_id = steps.ContractSteps.create_contract_new('no_agency', {'CLIENT_ID': client_id,
                                                                        'PERSON_ID': person_id,
                                                                        'IS_FIXED': today, 'DT': today,
                                                                        'IS_SIGNED': today,
                                                                        'FIRM': context.firm.id,
                                                                        'SERVICES': [context.service.id],
                                                                        'PAYMENT_TYPE': 3,
                                                                        'UNILATERAL': 1,
                                                                        'CURRENCY': 810})[0]
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, context.product.id, context.service.id,
                                       {'TEXT': 'Py_Test order', 'AgencyID': None, 'ManagerUID': None})
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': today}]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=today))

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                           paysys_id=context.paysys.id,
                                                           credit=1,
                                                           contract_id=contract_id,
                                                           overdraft=0,
                                                           endbuyer_id=None)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': qty}, 0, campaigns_dt=today)
    steps.ActsSteps.generate(client_id, 1, today)


def test_charge_note():
    qty = 50
    today = utils.Date.date_to_iso_format(datetime.today())
    context = CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED
    client_id = steps.ClientSteps.create(params={'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_CHARGE_NOTE, client_id)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, {u'name': u'ООО "Плательщик"',
                                                                               u'inn': u'7838298476',
                                                                               u'account': u'40702810982208554168',
                                                                               u'fname': u'',
                                                                               u'lname': u'',
                                                                               u'mname': u''})
    client_id, person_id, contract_id, contract_eid = \
        steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                    is_postpay=0)
    service_order_id = api.medium().GetOrdersInfo({'ContractID': contract_id})[0]['ServiceOrderID']
    # зато работает
    service_charge_note = \
        db.balance().execute('select service_id from t_order where service_order_id = :service_order_id',
                             {'service_order_id': service_order_id})[0]['service_id']
    orders_list = [{'ServiceID': service_charge_note, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': today}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': today,
                                                              'InvoiceDesireType': 'charge_note'})
    charge_note_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                     credit=0, contract_id=contract_id)
