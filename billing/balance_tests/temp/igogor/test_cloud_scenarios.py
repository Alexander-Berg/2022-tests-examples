# coding: utf-8

import datetime as dt
import json
from decimal import Decimal as D

from dateutil.relativedelta import relativedelta

import btestlib.constants as c
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.defaults import GeneralPartnerContractDefaults as GenDefParams

'''
Для физика
Надо создать договор-оферту (договор-оферта это договор который создается сразу подписанным, а далее в него добавляются условия?)
   лучше создавать через CreateOffer
Вместе с договором создается лицевой счет (надо проверить что в нем правильная фирма)
   Также в договор передается набор проектов но про это никто ничего не знает (узнать у Вовы?)
Если предоплата, то клиенту создается счет-квитанция (передать в реквест 'InvoiceDesireType': 'charge_note')
   Он ее оплачивает и на эти деньги ему начинают оказываться услуги
   ВАЖНО: в счете-квитанции и его печатной форме указывается external_id лицевого счета клиента. И зачисления по счету квитанции тоже зачисляются на лицевой счет. Из оебса оплата приходит сразу на лицевой счет.
Сервис сам следит за тем чтобы перестать откручивать когда деньги кончатся для этого есть ручка GetPartnerBalance
   Баланс партнера может быть отрицательный если Сервис протупил и переоказал услуг
Открутки из ручки сервиса грузим в YT раз в полчаса, агрегируем (еслиб я еще знал что это такое) и перекладываем в таблицу T_PARTNER_CLOUD_STAT
Если постоплата, то мы просто в конце месяца собираем окрутки и генерим акты. А счета?

Для юрика
Создавать неагентский договор с теми же параметрами

Надо проверить, что для новой фирмы договоры, лицевые счета, счета-квитанции, акты создаются с нужной фирмой и грузятся в оебс



'''

START_DT = utils.Date.first_day_of_month(dt.datetime.now())

'''14:23:13 -- TestBalance.GeneratePartnerAct(1147372, datetime.datetime(2018, 9, 1, 0, 0))
            Ошибка:
            <error><wo-rollback>0</wo-rollback><delay>1</delay><state>0</state><client-id>82839272</client-id><output /><msg>Act for client_id = 82839272 already enqueued into MONTH_PROC queue with state = 0</msg><input /><method>TestBalance.GeneratePartnerAct</method><code>DEFER_ACT_ENQUEUE</code><parent-codes><code>DEFERRED_ERROR</code><code>EXCEPTION</code></parent-codes><contents>Act for client_id = 82839272 already enqueued into MONTH_PROC queue with state = 0</contents></error>
'''
# AMOUNT = D('99.94444')
AMOUNT = D('99.38543')


def test_temp():
    client_id = 83800820
    contract_id = 1217987

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)


def test_get_completions():
    start_dt = dt.datetime.now().replace(hour=0) + relativedelta(days=-1)
    end_dt = dt.datetime.now().replace(hour=23) + relativedelta(days=1)
    source = 'cloud'
    api.test_balance().GetPartnerCompletions(
        {'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': source})


def test_get_balance():
    api.medium().GetPartnerBalance(143, [1217987])


def test_ur_full_chain():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, type_=c.PersonTypes.UR.code)
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()

    contract_id, _ = steps.ContractSteps.create_contract_new(type_=c.ContractCommissionType.NO_AGENCY,
                                                             params={
                                                                 'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                 'DT': utils.Date.date_to_iso_format(
                                                                     dt.datetime.now().replace(day=1) + relativedelta(
                                                                         months=-1)),
                                                                 'FIRM': c.Firms.CLOUD_123.id,
                                                                 'PAYMENT_TYPE': c.ContractPaymentType.POSTPAY,
                                                                 'SERVICES': [c.Services.CLOUD_143.id],
                                                                 'IS_SIGNED': utils.Date.date_to_iso_format(
                                                                     dt.datetime.now() + relativedelta(days=-1)),
                                                                 'PARTNER_CREDIT': 1,
                                                                 'PERSONAL_ACCOUNT': 1,
                                                                 'CONTRACT_PROJECTS': json.dumps([{'id': 1,
                                                                                                   'value': project_uuid}]),
                                                                 'MANAGER_CODE': '20453'})

    invoice_id = create_invoice(c.Services.CLOUD_143.id, client_id, person_id, contract_id,
                                c.Products.CLOUD.id, c.Paysyses.BANK_UR_RUB.id)

    personal_invoice_id = \
    db.balance().execute("select id from t_invoice where CONTRACT_ID = :contract_id and type = 'personal_account'",
                         dict(contract_id=contract_id), single_row=True)['id']

    # steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id,
    #                               invoice_id=personal_invoice_id)

    # steps.PartnerSteps.create_cloud_completion(contract_id, START_DT, AMOUNT, project_uuid)
    steps.PartnerSteps.create_cloud_completion(contract_id, START_DT, D('0.5'))

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

    act_id = \
    db.balance().execute("select id from BO.T_ACT where INVOICE_ID = :invoice_id", dict(invoice_id=personal_invoice_id),
                         single_row=True)['id']
    # steps.ExportSteps.export_oebs(act_id=act_id)

    pass


def test_ph_full_chain():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, type_=c.PersonTypes.PH.code)
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()

    params = GenDefParams.CLOUD
    params['CONTRACT_PARAMS'].update(dict(MANAGER_CODE='20453'))
    params['PERSON_TYPE'] = c.PersonTypes.PH.code

    additional_params = {'start_dt': dt.datetime.now().replace(day=1),
                         'projects': [project_uuid]}
    # 'projects': ['time-2018-08-29T15:29:08.228262']}
    contract_id, _, person_id = steps.ContractSteps. \
        create_person_and_offer_with_additional_params(client_id,
                                                       params,
                                                       additional_params=additional_params,
                                                       is_offer=True,
                                                       person_id=person_id
                                                       )

    invoice_id = create_invoice(c.Services.CLOUD_143.id, client_id, person_id, contract_id,
                                c.Products.CLOUD.id, c.Paysyses.CC_PH_RUB.id)

    personal_invoice_id = \
    db.balance().execute("select id from t_invoice where CONTRACT_ID = :contract_id and type = 'personal_account'",
                         dict(contract_id=contract_id), single_row=True)['id']
    #
    # steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id,
    #                               invoice_id=personal_invoice_id)

    # steps.PartnerSteps.create_cloud_completion(contract_id, START_DT.replace(day=3), AMOUNT, steps.PartnerSteps.create_cloud_project_uuid())
    steps.PartnerSteps.create_cloud_completion(contract_id, START_DT.replace(day=6), AMOUNT)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

    act_id = \
    db.balance().execute("select id from BO.T_ACT where INVOICE_ID = :invoice_id", dict(invoice_id=personal_invoice_id),
                         single_row=True)['id']
    # steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id,
    #                               invoice_id=personal_invoice_id, act_id=act_id)

    pass



def create_invoice(service, client_id, person_id, contract_id, product_id, paysys_id):
    service_order_id = steps.OrderSteps.next_id(service)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service)

    orders_list = [{'ServiceID': service, 'ServiceOrderID': service_order_id, 'Qty': 100.1,
                    'BeginDT': dt.datetime.now()}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': dt.datetime.now(),
                                                              'InvoiceDesireType': 'charge_note'})

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                 credit=0, contract_id=contract_id)

    # invoice_type = db.balance().execute("select type from t_invoice where id = " + str(invoice_id))[0]['type']
    return invoice_id
