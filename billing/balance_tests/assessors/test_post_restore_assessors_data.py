# coding=utf-8
__author__ = 'atkaya'

from datetime import timedelta, datetime

from balance import balance_steps as steps
from btestlib.constants import PersonTypes, Services, Paysyses, Products, User, Firms
import balance.balance_db as db
import btestlib.utils as utils

LOGIN_WITHOUT_CLIENT = User(1575092862, 'yb-static-balance-5')
LOGIN_ORDER_PAGE = User(1574407943, 'yb-static-balance-6')
LOGIN_REPRESENTATIVE = User(1574403204, 'yb-static-representative')
LOGIN_SETTLEMENTS = User(1347405535, 'yndx-static-balance-21')
LOGIN_EMPTY = User(1373216401, 'yndx-static-balance-22')


# кейсы можно искать по ключу "КИ"

def test_empty_client():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(LOGIN_EMPTY, client_id)


# подготовка данных для страницы представителей representatives.xml в КИ
def test_create_objects_for_asessors_representatives_xml():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(LOGIN_REPRESENTATIVE, client_id)
    db.balance().execute('UPDATE t_passport SET is_main=1 WHERE passport_id=1574403204')
    db.balance().execute(
        'UPDATE t_passport SET gecos=null, email=null, login=null, client_id=:client_id WHERE passport_id=1253955001',
        {'client_id': client_id})
    db.balance().execute(
        "UPDATE t_passport  SET gecos='Иванов Иван Иванович', email='yes@11слонят.ип', login='yes@11слонят.ип', client_id=:client_id WHERE passport_id=1253963727",
        {'client_id': client_id})
    if not db.balance().execute('SELECT id FROM t_role_client_user WHERE passport_id=1253985017 AND role_id=100'):
        db.balance().execute('INSERT INTO t_role_client_user(role_id, passport_id) VALUES (100,1253985017)')
    db.balance().execute('UPDATE t_role_client_user SET client_id=:client_id WHERE PASSPORT_ID=1253985017',
                         {'client_id': client_id})


def test_order_consumes_pagination():
    client_id = steps.ClientSteps.create({'NAME': ''})
    steps.UserSteps.link_user_and_client(LOGIN_ORDER_PAGE, client_id)

    person_id_1 = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    dt = datetime.now()

    # оплаченный и заакченный заказ по директу
    service_order_id = steps.OrderSteps.next_id(service_id=Services.DIRECT.id)

    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id,
                                       product_id=Products.DIRECT_FISH.id,
                                       service_id=Services.DIRECT.id,
                                       params={'Text': u'Тестовый заказ Директ'})

    for i in range(1, 15):
        orders_list = [
            {'ServiceID': Services.DIRECT.id, 'ServiceOrderID': service_order_id, 'Qty': 10 * i, 'BeginDT': dt}
        ]
        request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                               additional_params=dict(InvoiceDesireDT=dt))
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id_1,
                                                     paysys_id=Paysyses.BANK_UR_RUB.id,
                                                     credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
        steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(Services.DIRECT.id, service_order_id, {'Bucks': 1050}, 0)
    steps.ActsSteps.generate(client_id)


def test_settlements():
    client_id = steps.ClientSteps.create()
    steps.UserSteps.link_user_and_client(LOGIN_SETTLEMENTS, client_id)

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
