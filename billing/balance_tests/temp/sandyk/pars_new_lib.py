#-*- coding: utf-8 -*-

from xlrd import open_workbook
##from MTestlib import MTestlib as mtl
from datetime import datetime
from datetime import timedelta

import igogor.balance_steps as steps
import balance.balance_db as db


sql_date_format = "%d.%m.%Y %H:%M:%S"
##inv_dt_format = '%Y-%m-%d %H:%M:%S'
##rpc = mtl.rpc
##test_rpc = mtl.test_rpc

##добавить продукты для всех нужных сервисов
service_product_mapping = {'7':'1475', '11':'2136'}

##считываем данные из файла
def read_file (filename, sheet_number):
    d={}
    q={}
    rb = open_workbook(filename) #открываем файл
    sheet = rb.sheet_by_index(sheet_number) #лист номер 0
    for ii in range(sheet.nrows):
        for i in range(sheet.ncols):
            d[sheet.cell_value(0,i)]=sheet.cell_value(ii,i)
        q[ii]=d
        d={}
    del q[0]
    return q


data= read_file('test_data.xlsx',0)


## проходим по словарю, разбиваем его на словари по договорам (БЕЗ клиентов)
##contracts={}
##contracts[data[1]['id']] = []
##contracts[data[1]['id']].append(data[1])
##
##for numb in list(data.keys())[1:len(data.keys())]:
##    if data[numb-1]['id'] == data[numb]['id']:
##        contracts[data[numb-1]['id']].append(data[numb])
##    else:
##        contracts[data[numb]['id']] = []
##        contracts[data[numb]['id']].append(data[numb])
##
##print contracts
##


## проходим по словарю, разбиваем его на словари по договорам (С клиентами)
##contracts={}
##contracts[data[1]['id']] = {data[1]['client_id']:[]}
##contracts[data[1]['id']][data[1]['client_id']].append(data[1])
##contracts[data[3]['id']] = {data[3]['client_id']:[]}
##print data[3]['client_id']
##contracts[data[3]['id']][data[3]['client_id']].append(data[3])
##print contracts

##for numb in list(data.keys())[1:len(data.keys())]:
##    if data[numb-1]['id'] == data[numb]['id']:
##        if data[numb-1]['client_id'] == data[numb]['client_id']:
##            contracts[data[numb-1]['id']][data[numb-1]['client_id']].append(data[numb])
##        else:
##            contracts[data[numb]['id']][data[numb]['client_id']] = []
##            contracts[data[numb]['id']][data[numb]['client_id']].append(data[numb])
##
##    else:
##        contracts[data[numb]['id']] = {data[numb]['client_id']:[]}
##        contracts[data[numb]['id']][data[numb]['client_id']].append(data[numb])
##print contracts

## проходим по словарю, разбиваем его на словари по договорам (С клиентами) #2
contracts = {}
for numb in data:
    contract_id = data[numb]['id']
    client_id = data[numb]['client']
    if contract_id not in contracts:
        contracts[contract_id] = {}
    if client_id not in contracts[contract_id]:
        contracts[contract_id][client_id] = []
    contracts[contract_id][client_id].append(data[numb])
pass


##собираем данные для создания договоров
c_d={}

all_d={}
i=1
j=1
for contract_id in contracts:
        agency_id = steps.ClientSteps.create({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})['client_id']

        services = []
        data_for_contract = {}

        for client_id in contracts[contract_id]:

                for x in contracts[contract_id][client_id]:
                    data_for_act={}
                    service_id = db.balance().execute(
                        "select service_id from T_SERVICE_DISCOUNT_TYPES where DISCOUNT_TYPE_ID = :disc",
                        {'disc': int(x['serv'])})[0]['service_id']
                    if int(service_id) not in services:
                        services.append ( int(service_id))
                    payment_type = x['type']
                    dt_from =  str(x['dt_from'])+'T00:00:00'
                    comm = x['comm']
                    payment = x['type']

##данные для актов
                    data_for_act['client_id'] = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Petrov3'})['client_id']
                    order_owner   = client_id
                    invoice_owner = agency_id
                    if order_owner == invoice_owner: agency_id = None
                    data_for_act['service_id'] = db.balance().execute(
                        "select service_id from T_SERVICE_DISCOUNT_TYPES where DISCOUNT_TYPE_ID = :disc",
                        {'disc': int(x['serv'])})[0]['service_id']
##api.TestBalance.execute_sql
                    data_for_act['product_id'] = int(service_product_mapping[str(service_id)])
                    data_for_act['currency'] = str(x['curr'])
                    data_for_act['inv_dt'] = datetime.strptime((x['invoice_dt']),'%Y-%m-%d')
##                    str(x['invoice_dt'])+' 00:00:00'
    ##                act_dt =  str(x['act_dt'])
                    data_for_act['act_dt'] =  datetime.strptime((x['act_dt']),'%Y-%m-%d %H:%M:%S')
                    data_for_act['payment_dt']  =  datetime.strptime(((x['comiss'])[5:15]),'%Y-%m-%d')
##                     str(x['comiss'])[5:15] +' 00:00:00'
                    data_for_act['payment_sum'] = x['payment']
                    if data_for_act['currency'] == 'RUR':
                            paysys_id = 1001
                            qty = float(x['a_sum'])/30
                    else:
                        rate = db.balance().execute(
                            "select rate from T_CURRENCY_RATE where cc = :currency and  rate_dt = to_date(:act_dt,\'DD.MM.YYYY HH24:MI:SS\')",
                            {'currency': str(data_for_act['currency']),
                             'act_dt': data_for_act['act_dt'].strftime(sql_date_format)})[0]['rate']
                            if data_for_act['currency'] == 'USD':
                                    paysys_id = 1067
                                    qty = float(x['a_sum'])/float(rate)/30
                            if data_for_act['currency'] == 'EUR':
                                    paysys_id = 1066
                                    qty = float(x['a_sum'])/float(rate)/30

                    data_for_act['paysys_id'] =paysys_id
                    data_for_act['qty'] =qty
                    data_for_act['contract_id'] =x['id']
                    all_d[i]=data_for_act
                    i+=1

                if data_for_act['currency'] == 'RUR':
                    person_id = steps.PersonSteps.create(agency_id, 'ph', {'phone':'234'})
                    is_non_rez = 0
                else:
                    person_id = steps.PersonSteps.create(agency_id, 'usp', {'phone':'234'})
                    is_non_rez = 1
                data_for_contract = {
                 'client_id': agency_id,
                 'person_id': person_id,
                 'FINISH_DT': datetime.now()+timedelta(hours = 20000),
                 'SERVICES': services,
                 'COMMISSION_TYPE':comm ,
                 'is_signed': dt_from,
                 'dt':dt_from}

        if comm <= 30:
            if payment==2:
                contract_id=steps.ContractSteps.create('opt_prem',data_for_contract)[0]
                print contract_id
            if payment==3:
                contract_id=steps.ContractSteps.create('opt_prem_post',data_for_contract )[0]
                print contract_id
        c_d[j] = {'id':x['id'],'contract_id':contract_id, 'agency_id':data_for_contract['client_id'], 'person_id':data_for_contract['person_id'], 'payment':payment}
        j+=1

for con in c_d:
    for data in all_d:
         if all_d[data]['contract_id'] == str(c_d[con]['id']):

            order_owner   = all_d[data]['client_id']
            invoice_owner = c_d[con]['agency_id']
            if order_owner == invoice_owner: agency_id = None
            service_order_id = steps.OrderSteps.next_id(all_d[data]['service_id'])
            order_id = steps.OrderSteps.create (all_d[data]['client_id'], all_d[data]['product_id'], all_d[data]['service_id'], service_order_id ,  {'TEXT':'Py_Test order'})

            order_id = steps.OrderSteps.create (order_owner, service_order_id, all_d[data]['product_id'], all_d[data]['service_id'],
            {'TEXT':'Py_Test order','AgencyID' : invoice_owner, 'ManagerUID': None})
            orders_list = [
             {'ServiceID': all_d[data]['service_id'], 'ServiceOrderID': service_order_id, 'Qty': all_d[data]['qty'], 'BeginDT': all_d[data]['inv_dt']}
##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty, 'BeginDT': begin_dt}
##        , {'ServiceID': service_id, 'ServiceOrderID': service_order_id3, 'Qty': qty, 'BeginDT': begin_dt}
             ]
            request_id = steps.RequestSteps.create (invoice_owner, orders_list, None,  all_d[data]['inv_dt'])
            if c_d[con]['payment'] == 2:
                is_credit = 0
            else:
                is_credit = 1
            invoice_id= steps.InvoiceSteps.create (request_id, c_d[con]['person_id'], all_d[data]['paysys_id'], is_credit, c_d[con]['contract_id'], overdraft = 0)['invoice_id']
            steps.InvoiceSteps.pay(invoice_id, all_d[data]['payment_sum'], all_d[data]['payment_dt'])
            steps.CampaignsSteps.do_campaigns(all_d[data]['service_id'], service_order_id, {'Bucks': all_d[data]['qty'], 'Money': 0}, 0, all_d[data]['act_dt'])
            steps.ActsSteps.generate(invoice_owner, 1, all_d[data]['act_dt'])
            print invoice_id
            print '-------------------------------------------------------------------'


##        if comm > 30:
##             print '1111'
##             if payment==2:
##                contract_id=mtl.create_contract2('comm_pre',data_for_contract )
##             if payment==3:
##                contract_id=mtl.create_contract2('comm_post',data_for_contract)



###
##



##print '-----------------------------'
##for contract_id in contracts:
##    print contracts[[contract_id]['client']]
##    if contract not in processed_contracts:
##        agency_id = mtl.create_client({'IS_AGENCY': 1, 'name': u'Иванов Иван Иванович'})
##        person_id = mtl.create_person(agency_id, 'ph', {'phone':'234'})
####    for client_id in contract_id:
##        ##создаем все для договора
##
####        for stri in contracts[contract_id][client_id]:
##        for client_id in contract_id:
##            print contracts[contract_id][client_id]
##                print stri['dt_from']
##        processed_contracts.append(contract_id)
##



##        contract_id = mtl.mtl.create_contract2('opt_prem',{'client_id': agency_id, 'person_id': person_id, 'FINISH_DT': '2018-12-31T00:00:00', 'SERVICES': [7], 'is_signed': '2015-12-31T00:00:00'})



##print processed_contracts





