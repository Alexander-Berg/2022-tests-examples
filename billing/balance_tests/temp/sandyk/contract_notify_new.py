#-*- coding: utf-8 -*-

from MTestlib import MTestlib as mtl
import xmlrpclib
import pprint
import time
import urlparse
import os
import subprocess
import datetime
from datetime import date,timedelta
import calendar


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')

rpc = mtl.rpc
test_rpc = mtl.test_rpc

sql_date_format = "%d.%m.%Y %H:%M:%S"

##Заказ  / Реквест

def some_days_ago (date, number):
    delta = datetime.timedelta(days=number)
    dt = date - delta
    return (dt)

def contract_notify(contracts):
    test_rpc.ExecuteSQL('balance', 'update t_job set next_dt=sysdate-1 where id=\'contract_notify_enqueue\'')
    notify = test_rpc.ExecuteSQL('balance', 'select state_id from bo.v_pycron where name  = \'balance-contract-notify\'')[0]['state_id']
    print notify
    notify_enqueue = test_rpc.ExecuteSQL('balance', 'select state_id from v_pycron where name  = \'balance-contract-notify-enqueue\'')[0]['state_id']
    print notify_enqueue
    test_rpc.ExecuteSQL('balance', 'update (select * from T_PYCRON_STATE where id in ( :notify, :notify_enqueue )) set started  = null', {'notify': notify, 'notify_enqueue': notify_enqueue})
    print 'pycron has been restarted'
    while True:
            state = test_rpc.ExecuteSQL('balance', 'select sum(state) from t_export where object_id in ( :contract_id) and type  = \'CONTRACT_NOTIFY\' and classname = \'Contract\'',{'contract_id': contracts})[0]['state']
            if state == len(contracts):
                time.sleep(2)
            else:
                print 'contract is in enqueue'
                test_rpc.ExecuteSQL('balance', 'update (select * from t_export where object_id in ( :contract_id) and type  = \'CONTRACT_NOTIFY\' and classname = \'Contract\') set priority = -1',{'contract_id': contracts})
                break
    while True:
            state = test_rpc.ExecuteSQL('balance', 'select sum(state) from t_export where object_id in ( :contract_id) and type  = \'CONTRACT_NOTIFY\' and classname = \'Contract\'',{'contract_id': contracts})[0]['state']
            if state == 0:
                time.sleep(2)
            else:
                print 'contract has been processed'
                print 'Time ' + str ( datetime.datetime.now())
                print ''
                break



def flipping_dates (contracts, is_collateral, dt_type):

    dt_1 = some_days_ago (datetime.datetime.now(), 30)
##    mtl.add_months_to_date(datetime.datetime.now(),-1)
    dt_1_5 = some_days_ago (dt_1, 15)
    dt_2 = some_days_ago (dt_1_5, 15)
    dt_2_5 = some_days_ago (dt_2, 15)
    dt_3 = some_days_ago (dt_2_5, 17)
    dt_4 = some_days_ago (dt_3, 15)
##
##    dt_5 = some_days_ago (dt_4, 15)
##    dt_6 = some_days_ago (dt_5, 15)

##    mtl.add_months_to_date(datetime.datetime.now(),-3)

    dates = {}
    dates[1] = dt_1.strftime(sql_date_format)
    dates[2] = dt_1_5.strftime(sql_date_format)
    dates[3] = dt_2.strftime(sql_date_format)
    dates[4] = dt_2_5.strftime(sql_date_format)
    dates[5] = dt_3.strftime(sql_date_format)
    dates[6] = dt_4.strftime(sql_date_format)

##    dates[7] = dt_5.strftime(sql_date_format)
##    dates[8] = dt_6.strftime(sql_date_format)

    print dates
    for k in dates.values():
        update_dt = k
        print update_dt

##Договор
        if is_collateral == 0:
            if dt_type == 'is_faxed':
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num is null) set is_faxed =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
                print 'ert'
            if dt_type == 'not_signed':
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num is null) set dt =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
            if dt_type == 'is_booked':
                print '!!!!'
                contracts = str(contracts[0])+','+str(contracts[1])
                q= 'update (select * from t_contract_collateral c join t_contract_attributes a on c.id = a.collateral_id where c.contract2_id in ('+str(contracts)+' ) and code = \'IS_BOOKED_DT\' and num is null) set value_dt  = ''+ update_dt+'';'

                print q
                test_rpc.ExecuteSQL('balance', '\''+str(q)+'\'')
##            if dt_type == 'not_signed':
##                 test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  = :contract_id and num is null) set create_dt =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})

##ДС
        if is_collateral == 1:
            if dt_type == 'is_faxed':
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num =\'01\') set is_faxed =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
            if dt_type == 'not_signed':
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num=\'01\') set dt =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
            if dt_type == 'is_booked':
                test_rpc.ExecuteSQL('balance', 'update (select * from t_contract_collateral c join t_contract_attributes a on c.id = a.collateral_id where c.contract2_id in (:contract_id) and code = \'IS_BOOKED_DT\' and num=\'01\') set value_dt  =  to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contracts,'update_dt':update_dt})

##ДС и договор
        if is_collateral == 2:
            if dt_type == 'is_faxed':
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num is null) set is_faxed =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num =\'01\') set is_faxed =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
                print 'ert'
            if dt_type == 'not_signed':
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num is null) set dt =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
                test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  in (:contract_id) and num=\'01\') set dt =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})
            if dt_type == 'is_booked':
                test_rpc.ExecuteSQL('balance', 'update (select * from t_contract_collateral c join t_contract_attributes a on c.id = a.collateral_id where c.contract2_id in (:contract_id) and code = \'IS_BOOKED_DT\' and num is null) set value_dt  =  to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contracts,'update_dt':update_dt})
                test_rpc.ExecuteSQL('balance', 'update (select * from t_contract_collateral c join t_contract_attributes a on c.id = a.collateral_id where c.contract2_id in (:contract_id) and code = \'IS_BOOKED_DT\' and num=\'01\') set value_dt  =  to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contracts,'update_dt':update_dt})


##            if dt_type == 'not_signed':
##                 test_rpc.ExecuteSQL('balance', 'update (select * from T_CONTRACT_COLLATERAL where CONTRACT2_ID  = :contract_id and num=01) set create_dt =to_date(:update_dt,\'DD.MM.YYYY HH24:MI:SS\')',{'contract_id':contract_id,'update_dt':update_dt})

        contract_notify(contracts)

def test_client():
##
    contracts=[]
    client_id = mtl.create_client({'IS_AGENCY': 0})
##    manager_id = 20008
    person_id = mtl.create_person(client_id, 'ph',{'email': 'test-balance-notify@yandex-team.ru'})
##
###   Украина
##    client_id = 29289003
##    person_id = 4468962
##бронь
##    contract_id = mtl.create_contract2('no_agency_7',{'client_id': client_id, 'person_id': person_id,'is_faxed': '2015-05-14T00:00:00','IS_BOOKED':1})
##подписан
##    contract_id = mtl.create_contract2('no_agency_114',{'client_id': client_id, 'person_id': person_id,'is_signed': '2015-05-15T00:00:00'})
##факс

    contract_id = mtl.create_contract2('comm',{'client_id': client_id, 'person_id': person_id,'is_faxed':'2015-09-22T00:00:00','dt': '2015-09-22T00:00:00', 'IS_BOOKED':1},'Contract')
    contract_id2 = mtl.create_contract2('comm',{'client_id': client_id, 'person_id': person_id,'is_faxed':'2015-09-22T00:00:00','dt': '2015-09-22T00:00:00', 'IS_BOOKED':1},'Contract')

##    contract_id = mtl.create_contract2('multiship_post',{'client_id': client_id, 'person_id': person_id,'is_faxed': '2015-09-16T00:00:00','dt': '2015-09-16T00:00:00', 'SERVICES': [101,120]},'Contract')
    contracts.append(contract_id)
    contracts.append(contract_id2)
    print contracts
    print len(contracts)

## создаем ДС
##    mtl.create_contract2(80,{'contract2_id': contract_id},'Collateral')
## убирам менеджера
##    test_rpc.ExecuteSQL('balance', 'update (select a.* from t_contract_collateral c join T_CONTRACT_ATTRIBUTES a on c.id = A.COLLATERAL_ID where c.CONTRACT2_ID = :contract_id and a.code = \'MANAGER_CODE\') set value_num = -1',{'contract_id':contract_id});
##, 'SERVICES':[111,124]

##    contract_id=278098
##    test_rpc.ExecuteSQL('balance', 'update (select a.* from t_contract_collateral c join T_CONTRACT_ATTRIBUTES a on c.id = A.COLLATERAL_ID where c.CONTRACT2_ID = :contract_id and a.code = \'MANAGER_CODE\') set value_num = -1',{'contract_id':contract_id});

##is_faxed  is_booked
    flipping_dates(contracts,0,'is_booked')




##ukr_opt_client
##ukr_agent
##ukr_agent_premiya
##ukr_comm

###### Авто.ру
##    client_id = 29288805
##    person_id = 4468849

###### Швейцария
##    client_id = 29289008
##    person_id = 4468965
##shv_agent  shv_client
##    contract_id = mtl.create_contract2('auto_ru',{'client_id': client_id, 'person_id': person_id})
##бронь
##    contract_id = mtl.create_contract2('shv_client',{'client_id': client_id, 'person_id': person_id,'is_faxed': '2015-05-15T00:00:00','IS_BOOKED':1})
##подписан
##    contract_id = mtl.create_contract2('no_agency_111',{'client_id': client_id, 'person_id': person_id,'is_signed': '2015-05-15T00:00:00'})
##факс
##    contract_id = mtl.create_contract2('no_agency_111',{'client_id': client_id, 'person_id': person_id,'is_faxed': '2015-05-15T00:00:00'})
##
##
##    contract_id = mtl.create_contract2('auto_ru',{'client_id': client_id, 'person_id': person_id,'is_signed': '2015-05-15T00:00:00'})
##'is_signed': '2015-03-31T00:00:00'
## 'IS_BOOKED':1
##    contract_id = mtl.create_contract2('comm',{'client_id': client_id, 'person_id': person_id})
##,'IS_BOOKED':1
##    print contract_id
##    contract_id=274079
##    flipping_dates(contract_id,0,'is_faxed')
## not_signed    is_booked    is_faxed
##    contracts=[]
##    contracts = contract_id_1,contract_id_2
##    print contracts
######### Бронь подписи
######## dt = dt
######print dt_1.strftime(sql_date_format), dt_1_5.strftime(sql_date_format), dt_2.strftime(sql_date_format), dt_2_5.strftime(sql_date_format), dt_3.strftime(sql_date_format)



##    print 'Contract: ' + str(contract_id['EXTERNAL_ID']) + ' (' + str(contract_id['ID']) + ')'
##
##    col = urlparse.parse_qsl('''col-new-num=01&col-new-collateral-type=1033&col-new-print-form-type=0&col-new-dt=2014-12-05T00%3A00%3A00&col-new-memo=&col-new-group02-grp-10-pp-1137-checkpassed=1&col-new-group02-grp-1000-commission-payback-pct=8+%25&col-new-group02-grp-1000-commission-type=48&col-new-group02-grp-1000-commission-declared-sum=&col-new-group02-grp-1000-supercommission=0&col-new-group02-grp-1014-supercommission-bonus=1&col-new-group02-grp-50-supercommission=0&col-new-group02-grp-80-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1022-fixed-market-discount-pct=&
##    col-new-group02-grp-1021-commission-charge-type=1&col-new-group02-grp-1021-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1021-commission-payback-type=2&col-new-group02-grp-1021-commission-type=48&col-new-group02-grp-1021-supercommission-bonus=1&col-new-group02-grp-1021-supercommission=0&
##    col-new-group02-grp-1021-commission-declared-sum=&col-new-group02-grp-1021-named-client-declared-sum=&col-new-group02-grp-1021-linked-contracts=1&limitcol-new-group02-grp-1021-linked-contracts=&col-new-group02-grp-1021-payment-type=3&col-new-group02-grp-1021-credit-type=2&col-new-group02-grp-1021-payment-term=25&
##    col-new-group02-grp-1021-payment-term-max=&col-new-group02-grp-1021-credit-limit=17&limitcol-new-group02-grp-1021-credit-limit=&col-new-group02-grp-1021-credit-limit-single=500000&col-new-group02-grp-1021-turnover-forecast=17&limitcol-new-group02-grp-1021-turnover-forecast=&col-new-group02-grp-1021-services=1&col-new-group02-grp-1021-services-7=7&
##    col-new-group02-grp-1021-services-11=11&col-new-group02-grp-1021-services-70=70&col-new-group02-grp-1006-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1008-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1008-commission-type=48&col-new-group02-grp-1008-supercommission=0&col-new-group02-grp-1008-commission-declared-sum=&col-new-group02-grp-1009-finish-dt=2015-12-31T00%3A00%3A00&
##    col-new-group02-grp-1009-commission-type=48&col-new-group02-grp-1009-supercommission=0&col-new-group02-grp-1009-commission-declared-sum=&col-new-group02-grp-1009-payment-term=25&col-new-group02-grp-1009-credit-limit=17&limitcol-new-group02-grp-1009-credit-limit=&col-new-group02-grp-1009-turnover-forecast=17&limitcol-new-group02-grp-1009-turnover-forecast=&col-new-group02-grp-1009-credit-limit-single=500000&
##    col-new-group02-grp-1010-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1010-payment-term=25&col-new-group02-grp-1010-credit-limit=17&limitcol-new-group02-grp-1010-credit-limit=&col-new-group02-grp-1010-turnover-forecast=17&limitcol-new-group02-grp-1010-turnover-forecast=&col-new-group02-grp-1010-credit-limit-single=500000&col-new-group02-grp-1013-commission-charge-type=1&col-new-group02-grp-1013-finish-dt=2015-12-31T00%3A00%3A00&
##    col-new-group02-grp-1013-commission-payback-type=2&col-new-group02-grp-1013-commission-type=48&col-new-group02-grp-1013-commission-declared-sum=&col-new-group02-grp-1013-supercommission=0&col-new-group02-grp-1019-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1019-discount-policy-type=0&col-new-group02-grp-1019-discount-fixed=12&col-new-group02-grp-1019-declared-sum=&col-new-group02-grp-1019-fixed-discount-pct=&col-new-group02-grp-1019-budget-discount-pct=&
##    col-new-group02-grp-1019-discount-pct=&col-new-group02-grp-1019-discount-findt=&col-new-group02-grp-1032-commission-payback-pct=8+%25&col-new-group02-grp-1020-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1020-credit-type=2&col-new-group02-grp-1020-payment-term=25&col-new-group02-grp-1020-payment-term-max=&col-new-group02-grp-1020-repayment-on-consume-checkpassed=1&col-new-group02-grp-1020-credit-limit=17&limitcol-new-group02-grp-1020-credit-limit=&col-new-group02-grp-1020-turnover-forecast=17&limitcol-new-group02-grp-1020-turnover-forecast=&col-new-group02-grp-1020-credit-limit-single=500000&col-new-group02-grp-90-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-100-personal-account-checkpassed=1&col-new-group02-grp-100-credit-limit=17&limitcol-new-group02-grp-100-credit-limit=&col-new-group02-grp-100-turnover-forecast=17&limitcol-new-group02-grp-100-turnover-forecast=&col-new-group02-grp-100-credit-limit-single=500000&col-new-group02-grp-110-commission-payback-pct=8+%25&col-new-group02-grp-2110-credit-type=2&col-new-group02-grp-110-payment-term=5&col-new-group02-grp-110-payment-term-max=&col-new-group02-grp-110-calc-defermant=0&col-new-group02-grp-110-repayment-on-consume-checkpassed=1&col-new-group02-grp-110-credit-limit=17&limitcol-new-group02-grp-110-credit-limit=&col-new-group02-grp-110-credit-limit-single=500000&col-new-group02-grp-110-turnover-forecast=17&limitcol-new-group02-grp-110-turnover-forecast=&col-new-group02-grp-1031-commission-payback-pct=8+%25&col-new-group02-grp-1031-supercommission-bonus=1&col-new-group02-grp-1031-credit-type=2&col-new-group02-grp-1031-payment-term=25&col-new-group02-grp-1031-payment-term-max=&col-new-group02-grp-1031-calc-defermant=0&col-new-group02-grp-1031-repayment-on-consume-checkpassed=1&col-new-group02-grp-1031-credit-limit=17&limitcol-new-group02-grp-1031-credit-limit=&col-new-group02-grp-1031-credit-limit-single=500000&col-new-group02-grp-1031-turnover-forecast=17&limitcol-new-group02-grp-1031-turnover-forecast=&col-new-group02-grp-1034-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1034-supercommission-bonus=1&col-new-group02-grp-1036-calc-termination=&col-new-group02-grp-1033-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1033-supercommission-bonus=1&col-new-group02-grp-1033-payment-term=25&col-new-group02-grp-1033-credit-limit=17&limitcol-new-group02-grp-1033-credit-limit=&col-new-group02-grp-1033-turnover-forecast=17&limitcol-new-group02-grp-1033-turnover-forecast=&col-new-group02-grp-1033-credit-limit-single=500000&col-new-group02-grp-1035-client-limits=%5B%5D&col-new-group02-grp-1037-commission-categories=%5B%5D&col-new-group02-grp-1039-adfox-products=%5B%7B%22id%22%3A%221%22%2C%22num%22%3A504400%2C%22name%22%3A%22ADFOX.Sites1+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%222%22%2C%22num%22%3A504401%2C%22name%22%3A%22ADFOX.Nets+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%223%22%2C%22num%22%3A504402%2C%22name%22%3A%22ADFOX.Mobile+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%224%22%2C%22num%22%3A504403%2C%22name%22%3A%22ADFOX.Exchange+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%225%22%2C%22num%22%3A504404%2C%22name%22%3A%22ADFOX.Adv+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%226%22%2C%22num%22%3A504405%2C%22name%22%3A%22ADFOX.Sites1+%28shows%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%227%22%2C%22num%22%3A504406%2C%22name%22%3A%22ADFOX.Sites1+%28requests%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%228%22%2C%22num%22%3A504407%2C%22name%22%3A%22Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%229%22%2C%22num%22%3A504408%2C%22name%22%3A%22ADFOX.Nets%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2210%22%2C%22num%22%3A504409%2C%22name%22%3A%22ADFOX.Mobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2211%22%2C%22num%22%3A504410%2C%22name%22%3A%22ADFOX.Exchange%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2212%22%2C%22num%22%3A504411%2C%22name%22%3A%22ADFOX.Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2213%22%2C%22num%22%3A504412%2C%22name%22%3A%22%D0%92%D1%8B%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B0+%D0%BB%D0%BE%D0%B3%D0%BE%D0%B2+%D0%B8%D0%B7+%D0%9F%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D1%8B%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2214%22%2C%22num%22%3A504413%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2215%22%2C%22num%22%3A504414%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2216%22%2C%22num%22%3A504415%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Nets%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2217%22%2C%22num%22%3A504416%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Mobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2218%22%2C%22num%22%3A504417%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2219%22%2C%22num%22%3A504418%2C%22name%22%3A%22%D0%A0%D0%B0%D0%B7%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D0%BA%D0%B0+%D0%BD%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%BE%D0%B9+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B9+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D0%B8%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2220%22%2C%22num%22%3A504419%2C%22name%22%3A%22%D0%9D%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%B0%D1%8F+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B0%D1%8F+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D1%8C+%28%D0%BF%D0%BE%D0%B4%D0%B4%D0%B5%D1%80%D0%B6%D0%BA%D0%B0%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2221%22%2C%22num%22%3A504420%2C%22name%22%3A%22%D0%97%D0%B0%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5+%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%BE%D0%B9+%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D0%B8%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2222%22%2C%22num%22%3A504421%2C%22name%22%3A%22%D0%97%D0%B0%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5+%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%BE%D0%B9+%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D0%B8+Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2223%22%2C%22num%22%3A504422%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%C2%AB%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%C2%BB+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2224%22%2C%22num%22%3A504423%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%C2%AB%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%C2%BB+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2225%22%2C%22num%22%3A504424%2C%22name%22%3A%22%D0%9A%D0%B0%D1%81%D1%82%D0%BE%D0%BC%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8F+%D0%B0%D0%BA%D0%BA%D0%B0%D1%83%D0%BD%D1%82%D0%B0%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%5D&col-new-group02-grp-1004-credit-type=2&col-new-group02-grp-1004-payment-term=25&col-new-group02-grp-1004-payment-term-max=&col-new-group02-grp-1004-credit-limit=17&limitcol-new-group02-grp-1004-credit-limit=&col-new-group02-grp-1004-credit-limit-single=500000&col-new-group02-grp-1004-turnover-forecast=17&limitcol-new-group02-grp-1004-turnover-forecast=&col-new-group02-grp-1017-payment-term-max=&col-new-group02-grp-1005-credit-type=2&col-new-group02-grp-1005-payment-term=25&col-new-group02-grp-1005-payment-term-max=&col-new-group02-grp-1005-repayment-on-consume-checkpassed=1&col-new-group02-grp-1005-credit-limit=17&limitcol-new-group02-grp-1005-credit-limit=&col-new-group02-grp-1005-turnover-forecast=17&limitcol-new-group02-grp-1005-turnover-forecast=&col-new-group02-grp-1005-credit-limit-single=500000&col-new-group02-grp-1012-calc-defermant=0&col-new-group02-grp-115-declared-sum=&col-new-group02-grp-115-discount-pct=&col-new-group02-grp-115-discount-findt=&col-new-group02-grp-1011-discount-policy-type=0&col-new-group02-grp-1011-discount-fixed=12&col-new-group02-grp-1011-declared-sum=&col-new-group02-grp-1011-fixed-discount-pct=&col-new-group02-grp-1011-budget-discount-pct=&col-new-group02-grp-1011-discount-pct=&col-new-group02-grp-1011-discount-findt=&col-new-group02-grp-1015-federal-budget=&col-new-group02-grp-1015-ukr-budget=&col-new-group02-grp-1015-federal-declared-budget=&col-new-group02-grp-1015-federal-annual-program-budget=&col-new-group02-grp-1015-belarus-budget=&col-new-group02-grp-1015-year-planning-discount=&col-new-group02-grp-1015-year-product-discount=&col-new-group02-grp-1015-consolidated-discount=&col-new-group02-grp-1015-use-consolidated-discount-checkpassed=1&col-new-group02-grp-1015-use-ua-cons-discount-checkpassed=1&col-new-group02-grp-1015-regional-budget=&col-new-group02-grp-1015-use-regional-cons-discount-checkpassed=1&col-new-group02-grp-1015-pda-budget=&col-new-group02-grp-1015-autoru-budget=&col-new-group02-grp-1001-supercommission-bonus=1&col-new-group02-grp-1001-services=1&col-new-group02-grp-1001-services-7=7&col-new-group02-grp-1001-services-11=11&col-new-group02-grp-1001-services-70=70&col-new-group02-grp-1023-currency=810&col-new-group02-grp-1023-bank-details-id=21&col-new-group02-grp-1024-loyal-clients=%5B%5D&col-new-group02-grp-1025-pp-1137-checkpassed=1&col-new-group02-grp-1026-brand-clients=%5B%5D&col-new-group02-grp-1027-retro-discount=&col-new-group02-grp-1028-partner-min-commission-sum=&col-new-group02-grp-2222-advance-payment-sum=&col-new-group02-grp-1030-partner-commission-type=1&col-new-group02-grp-1030-partner-commission-pct=&col-new-group02-grp-1030-partner-commission-sum=&col-new-group02-grp-1030-partner-min-commission-sum=&col-new-group02-grp-1038-service-min-cost=&col-new-is-booked-checkpassed=1&col-new-is-faxed-checkpassed=1&col-new-is-signed=&col-new-is-signed-checkpassed=1&col-new-is-signed-date=26+%D1%8F%D0%BD%D0%B2+2015+%D0%B3.&col-new-is-signed-dt=2015-01-26T00%3A00%3A00&col-new-sent-dt-checkpassed=1&
##    col-new-collateral-form=&id='''+str(contract_id['ID']))
##    collateral_id = rpc.Balance.CreateContract(16571028, {k: v.decode('utf-8') for k,v in col})
##    print 'Collateral: ' + str(collateral_id) + ')'
##
##    sql = "update t_contract_collateral set is_signed = date'2014-12-05' where contract2_id = %s and collateral_type_id = 1033" % str(contract_id['ID'])
##    test_rpc.ExecuteSQL('balance', sql);


test_client()

