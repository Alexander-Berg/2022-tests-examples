# -*- coding: utf-8 -*-
import datetime
from balance import balance_steps as steps
from btestlib.constants import Services, Products, Paysyses, Firms
from dateutil.relativedelta import relativedelta
from balance import balance_db as db


def get_act():
    # print steps.CommonSteps.get_host()  # Получить веточный хост для задач в PyCron

    client_id = steps.ClientSteps.create()
    # steps.ClientSteps.link(client_id, 'apopkov-test-0')
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    now = datetime.datetime.now()
    paysys_id = Paysyses.BANK_UR_RUB.id

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)  # внешний ID заказа

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': now}]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 100}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]


# Работа с базой
def db_query_type_1():
    person_id = 0
    from balance import balance_db as db
    person_db_data = db.get_person_by_id(person_id)[0]
    dt = person_db_data['dt']
    now = dt.strftime("%Y-%m-%d %H:%M:%S")


def db_query_type_2():
    from balance import balance_db as db
    table = 't_config'
    item = 0
    query = 'SELECT * FROM {} where item=:item'.format(table)
    db.balance().execute(query, {'item': item})


# Выгрузка в OEBS
def OEBS_upload():
    import balance.balance_api as api
    api.test_balance().ExportObject('OEBS', 'Person', 7139328, 0, None, None)


def ua_transfer_whenever(client_id, dt):
    import balance.balance_api as api
    api.test_balance().UATransferQueue([client_id], dt)
    api.test_balance().ExportObject('UA_TRANSFER', 'Client', client_id, 0, None, None)


def check_export_to_oebs(client_id, person_id, contract_id, act_id):
    from balance import balance_db as db
    import balance.balance_api as api
    invoice_id = db.get_act_by_id(act_id)[0]['invoice_id']

    steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id, invoice_id=invoice_id,
                                  person_id=person_id, act_id=act_id, manager_id=None)

    external_contarct_id = db.balance().execute('select EXTERNAL_ID from t_contract2 where id=:item',
                                                {'item': contract_id})[0]['external_id']
    external_invoice_id = db.balance().execute('select EXTERNAL_ID from t_invoice where id=:item',
                                               {'item': invoice_id})[0]['external_id']
    external_act_id = db.balance().execute('select EXTERNAL_ID from t_act where id=:item',
                                           {'item': act_id})[0]['external_id']
    print u'Client: ' + str(client_id)
    print u'Contract: ' + str(contract_id) + u' External: ' + str(external_contarct_id)
    print u'Invoice: ' + str(invoice_id) + u' External: ' + external_invoice_id
    print u'Act: ' + str(act_id) + u' External: ' + str(external_act_id)

    api.test_balance().ExportObject('OEBS', 'Act', act_id, 0, None, None)


def mnclose_partner():
    import balance.balance_api as api
    api.test_balance().SyncRevPartnerServices()


def get_request_choices_without_request():
    import balance.balance_api as api
    api.medium().GetRequestChoices({
        'OperatorUid': 16571028,
        'RequestID': -1,  # передаем, чтобы создать новый request
    },
        55174392,
        [{
            'Qty': 10,
            'ServiceID': 7,
            'ServiceOrderID': 40868984
        }],
        {}, )


def nirv():
    import balance.balance_api as api
    api.test_balance().ExportObject('NIRVANA_BLOCK', 'NirvanaBlock', 1100)


def get_st():
    import requests

    OAUTH = ""

    API_HOST = 'https://st-api.yandex-team.ru'

    '''
    Перевести тикет в нужный статус
    '''
    # API_PATH = '{}/v2/issues/BALANCE-32650/transitions/need_info/_execute'.format(API_HOST)
    # res = requests.post(API_PATH, headers={'Authorization': 'OAuth {}'.format(OAUTH)})

    '''
    Узнать возможные статусы для тикета
    '''
    API_PATH = '{}/v2/issues/PAYSUP-561036/transitions'.format(API_HOST)
    res = requests.get(API_PATH, headers={'Authorization': 'OAuth {}'.format(OAUTH)})

    # res.raise_for_status()
    print(res.json())
