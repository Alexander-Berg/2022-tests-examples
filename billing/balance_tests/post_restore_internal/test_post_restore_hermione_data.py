# -*- coding: utf-8 -*-

from datetime import timedelta, datetime
from decimal import Decimal as D
import copy
from balance import balance_api as api

import pytest

from balance import balance_steps as steps
from btestlib import utils
import balance.balance_db as db
from temp.igogor.balance_objects import Contexts
from btestlib.constants import PersonTypes, Services, Paysyses, Products, ContractPaymentType, \
    Currencies, ContractCreditType, ContractCommissionType, Firms, User
import balance.balance_db as db
from btestlib.data.defaults import Date
from balance.real_builders import common_defaults
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT
from dateutil.relativedelta import relativedelta



@pytest.mark.parametrize('num', range(30))
def test_persons_pagination(num):
    client_id = steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    ur_params = common_defaults.FIXED_UR_PARAMS
    ur_params.update({'is-partner': '0', 'inn': '2473746582', 'email': 'hermione_test_pers@ya.ru',
                      'name': 'ООО "Гермионовый плательщик"'})
    _ = steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=ur_params)


@pytest.mark.parametrize('num', range(1, 55))
def test_client_pagination(num):
    client_id = steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    steps.ClientSteps.link(client_id, 'yb-atst-herm-client-' + str(num))


def test_clients_update():
    query = "UPDATE t_client SET name = 'очень старый клиент' WHERE ID in (520570, 560854, 582661, 505590, 551135, " \
            "795390, 796342, 548016, 802857, 545609, 824743, 835036, 846061, 818686, 857518, 860208, 865217, 804047, " \
            "523106, 874361, 513288, 779618, 879676, 876819, 888962, 448585, 805652, 531618, 920688, 919287, 880054, " \
            "946909, 937606, 949543, 952156, 839539, 511321, 953806, 799821, 995695, 1034559, 1055000, 1072457, " \
            "1072887, 1086527, 1008064, 1070733, 1081864, 1101622, 1121562)"
    db.balance().execute(query)


def test_search_invoices_ci(login='yb-hermione-ci-1'):
    client_id = steps.ClientSteps.create()

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'})
    person_ur_1_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                              {'name': u'тестовое юр лицо номер один',
                                               'email': 'test-ur-1@balance.ru',
                                               'inn': '7833208341'})
    person_ur_2_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                              {'name': u'тестовое юр лицо номер два',
                                               'email': 'test-ur-2@balance.ru',
                                               'inn': '7865109488'})

    # задаем овердрафты для директа и маркета
    steps.OverdraftSteps.set_force_overdraft(client_id, Services.MARKET.id, 1000000, firm_id=Firms.MARKET_111.id)
    steps.OverdraftSteps.set_force_overdraft(client_id, Services.DIRECT.id, 1000000, firm_id=Firms.YANDEX_1.id)

    # счет 1: выставляем предоплатный счет на Директ, без оплат, без актов
    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 35}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                             additional_params=dict(InvoiceDesireDT=datetime(2020, 3, 1)))
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ph_id, Paysyses.CC_PH_RUB.id, credit=0, overdraft=0)
    steps.InvoiceSteps.set_dt(invoice_id_1, datetime(2020, 3, 1))

    # счет 2: выставляем овердрафтный счет на Маркет, оплачен, без актов
    service_order_id_2 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.MARKET.id, service_id=Services.MARKET.id)
    orders_list_2 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_2, 'Qty': 10}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2)
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ur_2_id, Paysyses.BANK_UR_RUB.id, credit=0,
                                                   overdraft=1)
    steps.InvoiceSteps.pay(invoice_id_2)
    steps.InvoiceSteps.set_dt(invoice_id_2, datetime(2020, 3, 2))

    # счет 3: выставляем овердрафтный счет на Директ, оплачен, заакчен
    service_order_id_3 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_3, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_3 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_3, 'Qty': 80}]
    request_id_3 = steps.RequestSteps.create(client_id, orders_list_3)
    invoice_id_3, _, _ = steps.InvoiceSteps.create(request_id_3, person_ph_id, Paysyses.BANK_PH_RUB.id, credit=0,
                                                   overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 80}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    steps.InvoiceSteps.pay(invoice_id_3)
    steps.InvoiceSteps.set_dt(invoice_id_3, datetime(2020, 3, 3))

    # счет 4: выставляем предоплатный счет на Медийку, оплачен, без актов
    service_order_id_4 = steps.OrderSteps.next_id(Services.MEDIA_70.id)
    steps.OrderSteps.create(client_id, service_order_id_4, product_id=Products.MEDIA.id,
                            service_id=Services.MEDIA_70.id)
    orders_list_4 = [{'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id_4, 'Qty': 100}]
    request_id_4 = steps.RequestSteps.create(client_id, orders_list_4,
                                             additional_params=dict(InvoiceDesireDT=datetime(2020, 1, 1)))
    invoice_id_4, _, _ = steps.InvoiceSteps.create(request_id_4, person_ph_id, Paysyses.BANK_PH_RUB.id, credit=0,
                                                   overdraft=0)
    steps.InvoiceSteps.pay(invoice_id_4, payment_dt=datetime(2020, 2, 1))
    db.balance().execute('update t_invoice set receipt_dt = :dt where id = :invoice_id', {
        'dt': datetime(2020, 3, 4),
        'invoice_id': invoice_id_4})
    steps.InvoiceSteps.set_dt(invoice_id_4, datetime(2020, 3, 4))

    # создаем постоплатный договор с новым ЛС
    contract_type = 'no_agency_post'
    NOW = datetime.now()
    to_iso = utils.Date.date_to_iso_format
    FINISH_DT = to_iso(datetime(2023, 3, 5))
    START_DT = to_iso(datetime(2020, 3, 5))
    contract_new_pa_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id,
                                                                                'PERSON_ID': person_ur_1_id,
                                                                                'EXTERNAL_ID': '5577/20-pa-new',
                                                                                'DT': START_DT,
                                                                                'FINISH_DT': FINISH_DT,
                                                                                'IS_SIGNED': START_DT,
                                                                                'SERVICES': [Services.DIRECT.id],
                                                                                'FIRM': Firms.YANDEX_1.id,
                                                                                'PERSONAL_ACCOUNT_FICTIVE': 1})

    # создаем постоплатный договор с фиктивной схемой
    contract_fictive_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id,
                                                                                 'PERSON_ID': person_ur_1_id,
                                                                                 'EXTERNAL_ID': '2211/20-fictive',
                                                                                 'DT': START_DT,
                                                                                 'FINISH_DT': FINISH_DT,
                                                                                 'IS_SIGNED': START_DT,
                                                                                 'SERVICES': [Services.MARKET.id],
                                                                                 'FIRM': Firms.MARKET_111.id,
                                                                                 'PERSONAL_ACCOUNT_FICTIVE': 1})
    steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_fictive_id)


    # создаем договор со старым ЛС
    # счет 5, 6, 7: создаются автоматом 3 ЛС
    _, _, contract_old_pa_id, _ = steps.ContractSteps.create_partner_contract(TAXI_RU_CONTEXT, client_id=client_id,
                                                                              person_id=person_ur_2_id,
                                                                              additional_params={
                                                                                  'external_id': '123-01/1-partner'
                                                                                  # 'services': [Services.TAXI_111.id],
                                                                              })

    # steps.InvoiceSteps.set_dt(invoice_id_9, datetime(2020, 3, 9))


    # счет 8: выставляем счет-квитанцию (не должно быть видно в поиске)
    service_order_id_charge_note = api.medium().GetOrdersInfo({'ContractID': contract_old_pa_id})[0]['ServiceOrderID']
    # зато работает
    service_charge_note = \
        db.balance().execute('select service_id from t_order where service_order_id = :service_order_id',
                             {'service_order_id': service_order_id_charge_note})[0]['service_id']
    orders_list = [{'ServiceID': service_charge_note, 'ServiceOrderID': service_order_id_charge_note, 'Qty': 50,
                    'BeginDT': datetime(2020, 3, 8)}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': datetime.today(),
                                                              'InvoiceDesireType': 'charge_note'})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_ur_2_id, Paysyses.BANK_UR_RUB.id,
                                                 credit=0, contract_id=contract_old_pa_id)

    # счет 9: предоплатный на Директ по договору, включен, без актов
    service_order_id_9 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_9, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_9 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_9, 'Qty': 540}]
    request_id_9 = steps.RequestSteps.create(client_id, orders_list_9)
    invoice_id_9, _, _ = steps.InvoiceSteps.create(request_id_9, person_ur_1_id, Paysyses.BANK_UR_RUB.id, credit=0,
                                                   overdraft=0,
                                                   contract_id=contract_new_pa_id)
    steps.InvoiceSteps.turn_on(invoice_id_9)
    steps.InvoiceSteps.set_dt(invoice_id_9, datetime(2020, 3, 9))

    # счет 10: новый ЛС + ы счет по договору, , в КИ будет видно только на погашение
    service_order_id_10 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_10, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_10 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_10, 'Qty': 239}]
    request_id_10 = steps.RequestSteps.create(client_id, orders_list_10)
    invoice_id_10, _, _ = steps.InvoiceSteps.create(request_id_10, person_ur_1_id, Paysyses.BANK_UR_RUB.id, credit=1,
                                                    overdraft=0,
                                                    contract_id=contract_new_pa_id)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_10, {'Bucks': 239}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    steps.InvoiceSteps.set_dt(invoice_id_10, datetime(2020, 3, 10))

    # счет 11: фиктивный + счет на погашение по договору, в КИ будет видно только на погашение
    service_order_id_11 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_11, product_id=Products.MARKET.id,
                            service_id=Services.MARKET.id)
    orders_list_11 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_11, 'Qty': 5}]
    request_id_11 = steps.RequestSteps.create(client_id, orders_list_11)
    invoice_id_11, _, _ = steps.InvoiceSteps.create(request_id_11, person_ur_1_id, Paysyses.BANK_UR_RUB.id, credit=1,
                                                    overdraft=0,
                                                    contract_id=contract_fictive_id)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_11, {'Bucks': 5}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    steps.InvoiceSteps.set_dt(invoice_id_11, datetime(2020, 3, 11))

    # счет 12:
    service_order_id_12 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_12, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_12 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_12, 'Qty': 20}]
    request_id_12 = steps.RequestSteps.create(client_id, orders_list_12)
    invoice_id_12, _, _ = steps.InvoiceSteps.create(request_id_12, person_ph_id, Paysyses.CC_PH_RUB.id, credit=0,
                                                    overdraft=0)
    steps.InvoiceSteps.pay(invoice_id_12)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_12, {'Bucks': 14}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    steps.InvoiceSteps.set_dt(invoice_id_12, datetime(2020, 3, 12))

    invoices_new_pa = steps.InvoiceSteps.get_invoice_id_by_client_and_contract(client_id, contract_new_pa_id)
    for i in range(len(invoices_new_pa)):
        steps.InvoiceSteps.set_dt(invoices_new_pa[i]['id'], datetime(2020, 3, 13))

    invoices_fictive = steps.InvoiceSteps.get_invoice_id_by_client_and_contract(client_id, contract_fictive_id)
    for i in range(len(invoices_fictive)):
        steps.InvoiceSteps.set_dt(invoices_fictive[i]['id'], datetime(2020, 3, 14))

    invoices_old_pa = steps.InvoiceSteps.get_invoice_id_by_client_and_contract(client_id, contract_old_pa_id)
    for i in range(len(invoices_old_pa)):
        steps.InvoiceSteps.set_dt(invoices_old_pa[i]['id'], datetime(2020, 3, 15))

    data = steps.api.medium().GetPassportByLogin(0, login)
    steps.ClientSteps.link(client_id, login)


# подготовка данных для страницы поиска заказов orders.xml в КИ
def test_settlements_ci(login='yb-hermione-ci-3'):
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, login)

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru',
                                             'inn': '7865109488'})
    person_ur_1_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                              {'name': u'тестовое юр лицо номер один',
                                               'email': 'test-ur-1@balance.ru',
                                               'inn': '7833208341'})

    # задаем овердрафт для директа
    steps.OverdraftSteps.set_force_overdraft(client_id, Services.DIRECT.id, 1000000, firm_id=Firms.YANDEX_1.id)

    # выставляем предоплатный счет на Директ
    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 35}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                             additional_params=dict(InvoiceDesireDT=datetime(2020, 3, 1)))
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ph_id, Paysyses.CC_PH_RUB.id, credit=0,
                                                   overdraft=0)
    steps.InvoiceSteps.pay_fair(invoice_id_1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_1, {'Bucks': 35}, 0)
    steps.ActsSteps.generate(client_id, force=1)

    # выставляем овердрафтный счет на Директ, оплачен, заакчен (два акта, две оплаты)
    service_order_id_2 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_2 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_2, 'Qty': 80}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2)
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ph_id, Paysyses.BANK_PH_RUB.id, credit=0,
                                                   overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_2, {'Bucks': 50.12}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_2, {'Bucks': 80}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    steps.InvoiceSteps.pay_fair(invoice_id_2, payment_sum=2000.3)
    steps.InvoiceSteps.pay_fair(invoice_id_2, payment_sum=399.7)

    # выставляем предоплатный счет на Медийку, оплачен, без актов
    service_order_id_4 = steps.OrderSteps.next_id(Services.MEDIA_70.id)
    steps.OrderSteps.create(client_id, service_order_id_4, product_id=Products.MEDIA.id,
                            service_id=Services.MEDIA_70.id)
    orders_list_4 = [{'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id_4, 'Qty': 100}]
    request_id_4 = steps.RequestSteps.create(client_id, orders_list_4,
                                             additional_params=dict(InvoiceDesireDT=datetime(2020, 1, 1)))
    invoice_id_4, _, _ = steps.InvoiceSteps.create(request_id_4, person_ph_id, Paysyses.BANK_PH_RUB.id, credit=0,
                                                   overdraft=0)
    steps.InvoiceSteps.pay_fair(invoice_id_4, payment_dt=datetime(2020, 2, 1), payment_sum=-16.2)
    db.balance().execute('update t_invoice set receipt_dt = :dt where id = :invoice_id', {
        'dt': datetime(2020, 2, 1),
        'invoice_id': invoice_id_4})

    # создаем постоплатный договор с новым ЛС
    contract_type = 'no_agency_post'
    NOW = datetime.now()
    to_iso = utils.Date.date_to_iso_format
    HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
    HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))
    contract_new_pa_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id,
                                                                                'PERSON_ID': person_ur_1_id,
                                                                                'EXTERNAL_ID': '5577/20-pa-new',
                                                                                'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                                'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                'SERVICES': [Services.DIRECT.id],
                                                                                'FIRM': Firms.YANDEX_1.id,
                                                                                'PERSONAL_ACCOUNT_FICTIVE': 1})

    # новый ЛС + ы счет по договору
    service_order_id_10 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_10, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_10 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_10, 'Qty': 239}]
    request_id_10 = steps.RequestSteps.create(client_id, orders_list_10)
    invoice_id_10, _, _ = steps.InvoiceSteps.create(request_id_10, person_ur_1_id, Paysyses.BANK_UR_RUB.id, credit=1,
                                                    overdraft=0,
                                                    contract_id=contract_new_pa_id)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_10, {'Bucks': 239}, 0)
    steps.ActsSteps.generate(client_id, force=1)


def test_new_pa_for_consumes_history(login='yb-hermione-ci-4'):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    steps.ClientSteps.link(agency_id, login)

    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    params = {'CREDIT_TYPE': 1,
              'LIFT_CREDIT_ON_PAYMENT': 1,
              'COMMISSION_TYPE': 47,
              }
    _, person_id, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=agency_id,
                                           start_dt=datetime(2021, 1, 1),
                                           services=[Services.DIRECT.id, Services.MEDIA_70.id],
                                           contract_type=ContractCommissionType.COMMISS.id,
                                           finish_dt=datetime(2099, 1, 1),
                                           additional_params=params)
    subclient_id_1 = steps.ClientSteps.create({'NAME': 'Субклиент первый'})
    subclient_id_2 = steps.ClientSteps.create({'NAME': 'Субклиент второй'})

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(DIRECT_YANDEX.service.id)
    steps.OrderSteps.create(subclient_id_1, service_order_id,
                            service_id=DIRECT_YANDEX.service.id,
                            product_id=DIRECT_YANDEX.product.id,
                            params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': DIRECT_YANDEX.service.id, 'ServiceOrderID': service_order_id,
         'Qty': 10.23, 'BeginDT': datetime.now()})
    request_id = steps.RequestSteps.create(agency_id, orders_list)
    invoice_id_pa, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                    DIRECT_YANDEX.paysys.id,
                                                    contract_id=contract_id,
                                                    credit=1)

    db.balance().execute("update t_operation set dt = :dt where invoice_id = :invoice_id and type_id = 10002",
                         {'invoice_id': invoice_id_pa, 'dt': datetime(2021, 3, 4, 5, 0, 0)})

    steps.CampaignsSteps.do_campaigns(DIRECT_YANDEX.service.id, service_order_id, {'Bucks': 9.34}, 0)
    steps.ActsSteps.generate(agency_id, force=1)
    invoice_y_id, _ = steps.InvoiceSteps.get_invoice_ids(agency_id, type='y_invoice')
    steps.InvoiceSteps.pay_fair(invoice_y_id)
    # делаем возврат
    steps.InvoiceSteps.pay_fair(invoice_y_id, payment_sum=-5.23)

    db.balance().execute(
        "update t_operation set dt = :dt where invoice_id = :invoice_id",
        {'invoice_id': invoice_y_id, 'dt': datetime(2021, 3, 14, 9, 0, 0)})

    orders_list = []
    service_order_id = steps.OrderSteps.next_id(Services.MEDIA_70.id)
    steps.OrderSteps.create(subclient_id_2, service_order_id,
                            service_id=Services.MEDIA_70.id,
                            product_id=Products.MEDIA_2.id,
                            params={'AgencyID': agency_id})
    orders_list.append(
        {'ServiceID': Services.MEDIA_70.id, 'ServiceOrderID': service_order_id,
         'Qty': 1.44, 'BeginDT': datetime.now()})
    request_id = steps.RequestSteps.create(agency_id, orders_list,
                                           additional_params={
                                               'InvoiceDesireDT': datetime(2021, 3, 1, 3, 4, 5)})
    invoice_id_prepayment, _, _ = steps.InvoiceSteps.create(request_id, person_id,
                                                            DIRECT_YANDEX.paysys.id,
                                                            contract_id=contract_id,
                                                            credit=0)
    steps.InvoiceSteps.pay_fair(invoice_id_prepayment)
    db.balance().execute(
        "update t_operation set dt = :dt where invoice_id = :invoice_id",
        {'invoice_id': invoice_id_prepayment, 'dt': datetime(2021, 3, 16, 9, 0, 1)})


def test_acts_xml():
    client_id = steps.ClientSteps.create()

    steps.ClientSteps.link(client_id, 'yndx-static-balance-acts-1')

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'
                                             })
    person_ur_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                            {'name': u'тестовое юр лицо 1',
                                             'email': 'test-ur-2@balance.ru',
                                             'inn': '7865109488',
                                             'postaddress': u'Льва Толстого, 16',
                                             'postcode': '119021'
                                             })

    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    _, _, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=client_id,
                                           person_id=person_ur_id,
                                           start_dt=datetime(2021, 1, 1),
                                           additional_params={'EXTERNAL_ID': 'test_acts-1'})

    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 35}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1)))
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id)
    steps.InvoiceSteps.pay(invoice_id_1)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_1, {'Bucks': 20}, 0,
                                      campaigns_dt=datetime(2021, 1, 1))

    steps.OverdraftSteps.set_force_overdraft(client_id, Services.MARKET.id, 1000000, firm_id=Firms.MARKET_111.id)
    service_order_id_2 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.MARKET.id, service_id=Services.MARKET.id)
    orders_list_2 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_2, 'Qty': 10}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1)))
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ur_id, Paysyses.BANK_UR_RUB.id, overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_2, {'Bucks': 8}, 0,
                                      campaigns_dt=datetime(2021, 1, 1))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 1, 31))

    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_2, {'Bucks': 10}, 0,
                                      campaigns_dt=datetime(2021, 2, 28))
    act_id = steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 2, 28))[0]
    steps.ActsSteps.set_payment_term_dt(act_id, datetime(2099, 2, 1))

    service_order_id_3 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_3, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_3 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_3, 'Qty': 15}]
    request_id_3 = steps.RequestSteps.create(client_id, orders_list_3,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 3, 1)))
    invoice_id_3, _, _ = steps.InvoiceSteps.create(request_id_3, person_ph_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_3)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 1}, 0,
                                      campaigns_dt=datetime(2021, 3, 1))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 2}, 0,
                                      campaigns_dt=datetime(2021, 3, 2))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 3}, 0,
                                      campaigns_dt=datetime(2021, 3, 3))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 4}, 0,
                                      campaigns_dt=datetime(2021, 3, 4))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 5}, 0,
                                      campaigns_dt=datetime(2021, 3, 5))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 6}, 0,
                                      campaigns_dt=datetime(2021, 3, 6))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 7}, 0,
                                      campaigns_dt=datetime(2021, 3, 7))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 8}, 0,
                                      campaigns_dt=datetime(2021, 3, 8))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 9}, 0,
                                      campaigns_dt=datetime(2021, 3, 9))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 10}, 0,
                                      campaigns_dt=datetime(2021, 3, 10))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))

    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_3, {'Bucks': 11}, 0,
                                      campaigns_dt=datetime(2021, 3, 11))
    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))


def test_acts_ph():
    client_id = steps.ClientSteps.create()

    steps.ClientSteps.link(client_id, 'yndx-static-balance-acts-3')
    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'
                                             })

    service_order_id = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 1}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_ph_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id, {'Bucks': 1}, 0)
    steps.ActsSteps.generate(client_id, force=1)


def test_reconc_report():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, 'yndx-static-balance-acts-4')
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 1',
                              'email': 'test-ur-1@balance.ru',
                              'inn': '7865109488',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 2',
                              'email': 'test-ur-2@balance.ru',
                              'inn': '7836398764',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 3',
                              'email': 'test-ur-3@balance.ru',
                              'inn': '7884482864',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо 4',
                              'email': 'test-ur-4@balance.ru',
                              'inn': '7896345781',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-5',
                              'email': 'test-ur-5@balance.ru',
                              'inn': '7838171769',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-6',
                              'email': 'test-ur-6@balance.ru',
                              'inn': '7838317834',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-7',
                              'email': 'test-ur-7@balance.ru',
                              'inn': '7866390635',
                              })
    steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                             {'name': u'тестовое юр лицо-8',
                              'email': 'test-ur-8@balance.ru',
                              'inn': '7866390635',
                              })


def test_invoices_new_ci():
    client_id = steps.ClientSteps.create()

    steps.ClientSteps.link(client_id, 'yndx-static-balance-23')

    person_ph_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code,
                                            {'fname': u'тестовое',
                                             'lname': u'физ',
                                             'mname': u'лицо',
                                             'email': 'test-ph@balance.ru'
                                             })
    person_ur_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                            {'name': u'тестовое юр лицо 1',
                                             'email': 'test-ur-2@balance.ru',
                                             'inn': '7865109488',
                                             'postaddress': u'Льва Толстого, 16',
                                             'postcode': '119021'
                                             })

    DIRECT_YANDEX = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                         contract_type=ContractCommissionType.NO_AGENCY)
    GEO = Contexts.GEO_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                       contract_type=ContractCommissionType.NO_AGENCY)
    _, _, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(DIRECT_YANDEX, postpay=True, client_id=client_id,
                                           person_id=person_ur_id,
                                           start_dt=datetime(2021, 1, 1),
                                           additional_params={'EXTERNAL_ID': 'договор директ предоплата'})

    ### ПРЕДОПЛАТА ГЕО
    # счет 1 предоплата без договора, неоплаченный
    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 35}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1)))
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_ph_id, Paysyses.CC_PH_RUB.id)

    # счет 2 предоплата без договора, недоплаченный
    service_order_id_2 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_2 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_2, 'Qty': 40}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 2)))
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ur_id, Paysyses.BANK_UR_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_2, payment_sum=D(50), payment_dt=datetime(2021, 1, 3))

    # счет 3 предоплата без договора, оплаченный
    service_order_id_3 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_3, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_3 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_3, 'Qty': 45}]
    request_id_3 = steps.RequestSteps.create(client_id, orders_list_3,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 2)))
    invoice_id_3, _, _ = steps.InvoiceSteps.create(request_id_3, person_ph_id, Paysyses.CC_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_3, payment_dt=datetime(2021, 1, 4))

    # счет 4 предоплата без договора, переплаченный
    service_order_id_4 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_4, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_4 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_4, 'Qty': 50}]
    request_id_4 = steps.RequestSteps.create(client_id, orders_list_4,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 2)))
    invoice_id_4, _, _ = steps.InvoiceSteps.create(request_id_4, person_ph_id, Paysyses.BANK_PH_RUB.id)
    steps.InvoiceSteps.pay(invoice_id_4, payment_sum=D(100500), payment_dt=datetime(2021, 1, 4))

    # счет 5 предоплата с договором, оплаченный, частично открученный, заакченный
    service_order_id_5 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_5, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_5 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_5, 'Qty': 35}]
    request_id_5 = steps.RequestSteps.create(client_id, orders_list_5,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 6)))
    invoice_id_5, _, _ = steps.InvoiceSteps.create(request_id_5, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id)
    steps.InvoiceSteps.pay(invoice_id_5, payment_dt=datetime(2021, 1, 6))
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_5, {'Bucks': 20}, 0,
                                      campaigns_dt=datetime(2021, 1, 6))

    # счет 6 предоплата с договором, оплаченный, полностью открученный, заакченный
    service_order_id_6 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_6, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_6 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_6, 'Qty': 15}]
    request_id_6 = steps.RequestSteps.create(client_id, orders_list_6,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 6)))
    invoice_id_6, _, _ = steps.InvoiceSteps.create(request_id_6, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                   contract_id=contract_id)
    steps.InvoiceSteps.pay(invoice_id_6, payment_dt=datetime(2021, 1, 6))
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_6, {'Bucks': 15}, 0,
                                      campaigns_dt=datetime(2021, 1, 6))

    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 1, 31))

    ### ОВЕРДРАФТ И ПРЕДОПЛАТА МАРКЕТ
    steps.OverdraftSteps.set_force_overdraft(client_id, Services.MARKET.id, 1000000, firm_id=Firms.MARKET_111.id)
    # счет 7 предоплата, оплаченный, частично открученный, заакченный, Маркет
    service_order_id_7 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_7, product_id=Products.MARKET.id, service_id=Services.MARKET.id)
    orders_list_7 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_7, 'Qty': 10}]
    request_id_7 = steps.RequestSteps.create(client_id, orders_list_7,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 1)))
    invoice_id_7, _, _ = steps.InvoiceSteps.create(request_id_7, person_ur_id, Paysyses.BANK_UR_RUB.id)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_7, {'Bucks': 8}, 0,
                                      campaigns_dt=datetime(2021, 2, 1))

    # счет 8 овердрафт, оплаченный, частично открученный, заакченный, Маркет
    service_order_id_8 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_8, product_id=Products.MARKET.id,
                            service_id=Services.MARKET.id)
    orders_list_8 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_8, 'Qty': 15}]
    request_id_8 = steps.RequestSteps.create(client_id, orders_list_8,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 2)))
    invoice_id_8, _, _ = steps.InvoiceSteps.create(request_id_8, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
    steps.InvoiceSteps.pay(invoice_id_8)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_8, {'Bucks': 1}, 0,
                                      campaigns_dt=datetime(2021, 2, 2))

    # счет 10 овердрафт, почти просроченный, частично открученный, заакченный, Маркет
    service_order_id_10 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_10, product_id=Products.MARKET.id,
                            service_id=Services.MARKET.id)
    orders_list_10 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_10, 'Qty': 15}]
    request_id_10 = steps.RequestSteps.create(client_id, orders_list_10,
                                              additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 3)))
    invoice_id_10, _, _ = steps.InvoiceSteps.create(request_id_10, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_10, {'Bucks': 1}, 0,
                                      campaigns_dt=datetime(2021, 2, 3))
    steps.InvoiceSteps.set_payment_term_dt(invoice_id=invoice_id_10, dt=datetime.today() + relativedelta(days=2))

    # счет 11 овердрафт, непросроченный, частично открученный, заакченный, Маркет
    service_order_id_11 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_11, product_id=Products.MARKET.id,
                            service_id=Services.MARKET.id)
    orders_list_11 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_11, 'Qty': 15}]
    request_id_11 = steps.RequestSteps.create(client_id, orders_list_11,
                                              additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 4)))
    invoice_id_11, _, _ = steps.InvoiceSteps.create(request_id_11, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_11, {'Bucks': 1}, 0,
                                      campaigns_dt=datetime(2021, 2, 4))
    steps.InvoiceSteps.set_payment_term_dt(invoice_id=invoice_id_11, dt=datetime.today() + relativedelta(months=2))

    # счет 9 овердрафт, просроченный, частично открученный, заакченный, Маркет
    service_order_id_9 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_9, product_id=Products.MARKET.id,
                            service_id=Services.MARKET.id)
    orders_list_9 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_9, 'Qty': 15}]
    request_id_9 = steps.RequestSteps.create(client_id, orders_list_9,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 5)))
    invoice_id_9, _, _ = steps.InvoiceSteps.create(request_id_9, person_ph_id, Paysyses.BANK_PH_RUB.id, overdraft=1)
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id_9, {'Bucks': 1}, 0,
                                      campaigns_dt=datetime(2021, 2, 5))

    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 2, 28))

    ## ГЕО ПОСТОПЛАТА
    person_ur_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code,
                                            {'name': u'тестовое юр лицо 2',
                                             'email': 'test-ur-22@balance.ru',
                                             'inn': '7865109488',
                                             'postaddress': u'Льва Толстого, 16',
                                             'postcode': '119021'
                                             })

    _, _, contract_id, _ = steps.ContractSteps. \
        create_general_contract_by_context(GEO, postpay=True, client_id=client_id,
                                           person_id=person_ur_id,
                                           start_dt=datetime(2021, 3, 1),
                                           additional_params={'EXTERNAL_ID': 'договор гео постоплата'})

    service_order_id_12 = steps.OrderSteps.next_id(Services.GEO.id)
    steps.OrderSteps.create(client_id, service_order_id_12, product_id=Products.GEO.id,
                            service_id=Services.GEO.id)
    orders_list_12 = [{'ServiceID': Services.GEO.id, 'ServiceOrderID': service_order_id_12, 'Qty': 40}]
    request_id_12 = steps.RequestSteps.create(client_id, orders_list_12,
                                              additional_params=dict(InvoiceDesireDT=datetime(2021, 3, 6)))
    invoice_id_12, _, _ = steps.InvoiceSteps.create(request_id_12, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                    contract_id=contract_id, credit=True)
    steps.InvoiceSteps.pay(invoice_id_12, payment_dt=datetime(2021, 3, 6))
    steps.CampaignsSteps.do_campaigns(Services.GEO.id, service_order_id_12, {'Days': 20}, 0,
                                      campaigns_dt=datetime(2021, 3, 6))

    service_order_id_13 = steps.OrderSteps.next_id(Services.GEO.id)
    steps.OrderSteps.create(client_id, service_order_id_13, product_id=Products.GEO.id,
                            service_id=Services.GEO.id)
    orders_list_13 = [{'ServiceID': Services.GEO.id, 'ServiceOrderID': service_order_id_13, 'Qty': 4}]
    request_id_13 = steps.RequestSteps.create(client_id, orders_list_13,
                                              additional_params=dict(InvoiceDesireDT=datetime(2021, 3, 6)))
    invoice_id_13, _, _ = steps.InvoiceSteps.create(request_id_13, person_ur_id, Paysyses.BANK_UR_RUB.id,
                                                    contract_id=contract_id, credit=True)
    steps.InvoiceSteps.pay(invoice_id_13, payment_dt=datetime(2021, 3, 6))
    steps.CampaignsSteps.do_campaigns(Services.GEO.id, service_order_id_13, {'Days': 4}, 0,
                                      campaigns_dt=datetime(2021, 3, 6))

    steps.ActsSteps.generate(client_id, force=1, date=datetime(2021, 3, 31))
