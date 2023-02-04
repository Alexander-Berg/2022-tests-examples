# coding=utf-8
__author__ = 'atkaya'

from datetime import timedelta, datetime

from balance import balance_steps as steps
from balance import balance_api as api
from btestlib.constants import PersonTypes, Services, Paysyses, Products, User, Firms
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT
import balance.balance_db as db
import btestlib.utils as utils

LOGIN_INVOICES_SEARCH = User(1119019053, 'yndx-static-balance-2')
LOGIN_INVOICES_ELS_AND_DEBTS = User(1119135689, 'yndx-static-balance-3')
LOGIN_INVOICES_SUM = User(1347376951, 'yndx-static-balance-20')


# подготовка данных для страницы поиска счетов invoices.xml в КИ
def test_create_objects_for_asessors_invoices_xml():
    client_id = steps.ClientSteps.create()

    steps.UserSteps.link_user_and_client(LOGIN_INVOICES_SEARCH, client_id)

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
    _, _, _ = steps.InvoiceSteps.create(request_id_1, person_ph_id, Paysyses.CC_PH_RUB.id, credit=0, overdraft=0)

    # счет 2: выставляем овердрафтный счет на Маркет, оплачен, без актов
    service_order_id_2 = steps.OrderSteps.next_id(Services.MARKET.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.MARKET.id, service_id=Services.MARKET.id)
    orders_list_2 = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id_2, 'Qty': 10}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2)
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_ur_2_id, Paysyses.BANK_UR_RUB.id, credit=0,
                                                   overdraft=1)
    steps.InvoiceSteps.pay(invoice_id_2)

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

    # создаем постоплатный договор с фиктивной схемой
    contract_fictive_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id,
                                                                                 'PERSON_ID': person_ur_1_id,
                                                                                 'EXTERNAL_ID': '2211/20-fictive',
                                                                                 'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                                 'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                                 'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
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

    # счет 8: выставляем счет-квитанцию (не должно быть видно в поиске)
    service_order_id_charge_note = api.medium().GetOrdersInfo({'ContractID': contract_old_pa_id})[0]['ServiceOrderID']
    # зато работает
    service_charge_note = \
        db.balance().execute('select service_id from t_order where service_order_id = :service_order_id',
                             {'service_order_id': service_order_id_charge_note})[0]['service_id']
    orders_list = [{'ServiceID': service_charge_note, 'ServiceOrderID': service_order_id_charge_note, 'Qty': 50,
                    'BeginDT': datetime.today()}]
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


# подготовка данных для страницы поиска счетов invoices.xml в КИ: ЕЛС и информация о задолженности по овердрафту
# ЕЛС пока не используем, закомментирован
def test_create_objects_for_asessors_invoices_xml_part2():
    client_id = steps.ClientSteps.create(single_account_activated=False,
                                         enable_single_account=True)

    steps.UserSteps.link_user_and_client(LOGIN_INVOICES_ELS_AND_DEBTS, client_id)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)

    # # создаем ЕЛС
    # steps.ElsSteps.create_els(client_id)
    #
    # service_order_id = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)
    # steps.OrderSteps.create(client_id, service_order_id, product_id=Products.DIRECT_FISH.id, service_id=Services.DIRECT.id)
    # orders_list = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 100}]
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.BANK_UR_RUB.id)
    # steps.InvoiceSteps.pay(invoice_id)
    # steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id, {'Bucks': 100}, 0)
    # steps.ActsSteps.generate(client_id, force=1)

    # задаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, Services.DIRECT.id, 1000000, firm_id=Firms.YANDEX_1.id)

    # выставляем овердрафтные счета
    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 10}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1)
    invoice_id_1, _, _ = steps.InvoiceSteps.create(request_id_1, person_id, Paysyses.BANK_UR_RUB.id, credit=0,
                                                   overdraft=1)

    service_order_id_2 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_2 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_2, 'Qty': 55}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2)
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_id, Paysyses.BANK_UR_RUB.id, credit=0,
                                                   overdraft=1)

    # делаем один из овердрафтных счетов просроченным
    db.balance().execute('update t_invoice set payment_term_dt = :dt where id = :invoice_id', {
        'dt': datetime(2020, 6, 1),
        'invoice_id': invoice_id_2})


# подготовка данных для страницы поиска счетов invoices.xml в КИ: суммы, оплата
def test_invoices_sums():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code)
    steps.UserSteps.link_user_and_client(LOGIN_INVOICES_SUM, client_id)

    # счет 1: выставляем предоплатный счет на Директ, без оплат, без актов
    service_order_id_1 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_1, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_1 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_1, 'Qty': 10}]
    request_id_1 = steps.RequestSteps.create(client_id, orders_list_1,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 1, 1, 3, 23, 46)))
    _, _, _ = steps.InvoiceSteps.create(request_id_1, person_id, Paysyses.CC_PH_RUB.id, credit=0, overdraft=0)

    # счет 2: выставляем счет на Директ, оплачен, заакчен
    service_order_id_2 = steps.OrderSteps.next_id(Services.DIRECT.id)
    steps.OrderSteps.create(client_id, service_order_id_2, product_id=Products.DIRECT_FISH.id,
                            service_id=Services.DIRECT.id)
    orders_list_2 = [{'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id_2, 'Qty': 3.48}]
    request_id_2 = steps.RequestSteps.create(client_id, orders_list_2,
                                             additional_params=dict(InvoiceDesireDT=datetime(2021, 2, 1)))
    invoice_id_2, _, _ = steps.InvoiceSteps.create(request_id_2, person_id, Paysyses.BANK_PH_RUB.id, credit=0,
                                                   overdraft=0)
    steps.InvoiceSteps.pay(invoice_id_2, payment_dt=datetime(2020, 2, 1, 23, 12, 33))
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id_2, {'Bucks': 3.48}, 0)
    steps.ActsSteps.generate(client_id, force=1)
