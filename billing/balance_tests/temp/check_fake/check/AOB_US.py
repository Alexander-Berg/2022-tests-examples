#!/usr/bin/python
# -- coding: utf-8 --

import pprint
import os
import datetime
import pickle
from datetime import timedelta

import MTestlib_cmp as MTestlib
import Compares
import cmp_parameters
from temp.MTestlib import proxy_provider
from checksbase import TestBase


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


host = 'greed-ts1f'
tm = proxy_provider.GetServiceProxy(host, 0)
test = proxy_provider.GetServiceProxy(host, 1)

dt = MTestlib.some_month_ago(1)
paysys_id, person_type, contract_type, cmp_name, cmp_type, code_name = cmp_parameters.param('aob_us')

compare_type = 'aob_us'

service_id = 7;
product_id = 1475;
date_format = "%d.%m.%Y"


class AOB_US(TestBase):
    act_id = {}
    cmp_id = 0

    print paysys_id, person_type, contract_type, cmp_name, cmp_type, code_name, dt

    @classmethod
    def run_check(cls):
        Compares.pycron_command(cmp_name, code_name, 3090622)
        Compares.start_cmp(cmp_name, 'greed-ts1f')
        AOB_US.cmp_id = Compares.get_cmp_id(compare_type)
        print AOB_US.act_id
        ##            file_name = datetime.datetime.now().strftime(date_format)
        ##            F = open('tom.pkl', 'wb')
        ##            import pickle
        ##            pickle.dump(AOB_US.act_id, F)
        ##            F.close()
        print AOB_US.cmp_id

    #####забираем данные за вчера для запуска сверки, удаляем дату
    base_path = os.path.dirname(__file__)
    filename_tod = os.path.join(base_path, '..', 'data', 'AOB_US_tod.pkl')
    tod = open(filename_tod, 'rb')
    tod_data = pickle.load(tod)
    print 'tom_data for check:'
    print tod_data
    del tod_data['date']
    print 'del date:'
    print tod_data
    tod.close()


    #######################  1. Акт есть в Балансе, но нет в OEBS(без договора)
    @TestBase.prepare('Case_1_0')
    def prepare_Case_1_0():
        tm, test = MTestlib.proxy()
        client_id_1, person_id_1, order_id_1, contract_id_1, invoice_id_1, act_id_1, service_order_id_1 = Compares.com_3(
            paysys_id, person_type, dt, service_id, product_id)
        AOB_US.act_id['a1'] = act_id_1
        Compares.insert_into_t_act_check(act_id_1)

        ##### в последнем кейсе добавляем дату "завтра" и вставляем данные в файлик:
        filename_tom = os.path.join(base_path, '..', 'data', 'AOB_US_tom.pkl')
        insert_to_tom = open(filename_tom, 'wb')
        file_date = (datetime.datetime.now() + timedelta(days=1)).strftime(date_format)
        AOB_US.act_id['date'] = file_date
        pickle.dump(AOB_US.act_id, insert_to_tom)
        insert_to_tom.close()

        print u'Case_1_0 prepared'

    def Case_1_0(self):
        ##### для сверки берем данные из файла tod
        act_id = AOB_US.tod_data['a1']

        external_id = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0][
            'external_id']
        results = Compares.check(compare_type, external_id, AOB_US.cmp_id, 1)
        print 'Case_1_0 ' + str(results)
        print 'check:'
        print  act_id


######################### 2. Акт есть в Балансе, но нет в OEBS(по договору)
##        @TestBase.prepare('Case_1_1')
##        def prepare_Case_1_1():
##             tm, test = MTestlib.proxy()
##             client_id_2, person_id_2, order_id_2, contract_id_2, invoice_id_2, act_id_2 ,service_order_id_2= Compares.com_3(paysys_id, person_type, dt , service_id,product_id,  contract_type)
##             AOB_US.act_id['a2'] = act_id_2
##             Compares.insert_into_t_act_check(act_id_2)
##             print u'Case_1_1 prepared'
##
##        def Case_1_1(self):
##            act_id = AOB_US.act_id['a2']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,1 )
##            print 'Case_1_1 ' + str(results)
##            print 'ok'
############################### 3. Акт выгружен в OEBS, но потом захайжен в Балансе (без договора)
##        @TestBase.prepare('Case_2_0')
##        def prepare_Case_2_0():
##            tm, test = MTestlib.proxy()
##            client_id_3, person_id_3, order_id_3, contract_id_3, invoice_id_3, act_id_3,service_order_id_3 = Compares.com_4(paysys_id, person_type, dt, service_id,product_id)
##            test.ExecuteSQL('balance', 'update (select  * from t_act where  id = :act_id ) set hidden = 4', {'act_id': act_id_3})
##            AOB_US.act_id['a3'] = act_id_3
##            Compares.insert_into_t_act_check(act_id_3)
##            print u'Case_2_0 prepared'
##
##        def Case_2_0(self):
##            act_id = AOB_US.act_id['a3']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,2 )
##            print 'Case_2_0 ' + str(results)
########################### 4. Акт выгружен в OEBS, но потом захайжен в Балансе (по договору)
##        @TestBase.prepare('Case_2_1')
##        def prepare_Case_2_1():
##            tm, test = MTestlib.proxy()
##            client_id_4, person_id_4, order_id_4, contract_id_4, invoice_id_4, act_id_4,service_order_id_4 = Compares.com_4(paysys_id, person_type, dt, service_id,product_id, contract_type)
##            test.ExecuteSQL('balance', 'update (select  * from t_act where  id = :act_id ) set hidden = 4', {'act_id': act_id_4})
##            AOB_US.act_id['a4'] = act_id_4
##            Compares.insert_into_t_act_check(act_id_4)
##            print u'Case_2_1 prepared'
##
##        def Case_2_1(self):
##            act_id = AOB_US.act_id['a4']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,2 )
##            print 'Case_2_1 ' + str(results)
########################### 5. Акт выгружен в OEBS, но затем изменен эмаунт (без договора)
##        @TestBase.prepare('Case_3_0')
##        def prepare_Case_3_0():
##             tm, test = MTestlib.proxy()
##             client_id_5, person_id_5, order_id_5, contract_id_5, invoice_id_5, act_id_5,service_order_id_5 = Compares.com_4(paysys_id, person_type, dt,service_id,product_id)
##             amount_5 = test.ExecuteSQL('balance', 'select amount from t_act where  id = :act_id', {'act_id': act_id_5})[0]['amount'] - 100
##             test.ExecuteSQL('balance', 'update (select  * from t_act where  id = :act_id ) set amount = :amount ', {'act_id': act_id_5, 'amount':amount_5})
##             AOB_US.act_id['a5'] = act_id_5
##             Compares.insert_into_t_act_check(act_id_5)
##             print u'Case_3_0 prepared'
##
##        def Case_3_0(self):
##            act_id = AOB_US.act_id['a5']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,3 )
##            print 'Case_3_0 ' + str(results)
########################### 6. Акт выгружен в OEBS, но затем изменена эмаунт (по договору)
####        @TestBase.prepare('Case_3_1')
##        def prepare_Case_3_1():
##             tm, test = MTestlib.proxy()
##             client_id_6, person_id_6, order_id_6, contract_id_6, invoice_id_6, act_id_6,service_order_id_6 = Compares.com_4(paysys_id, person_type, dt,service_id,product_id, contract_type)
##             amount_6 = test.ExecuteSQL('balance', 'select amount from t_act where  id = :act_id', {'act_id': act_id_6})[0]['amount'] - 100
##             test.ExecuteSQL('balance', 'update (select  * from t_act where  id = :act_id ) set amount = :amount ', {'act_id': act_id_6, 'amount':amount_6})
##             AOB_US.act_id['a6'] = act_id_6
##             Compares.insert_into_t_act_check(act_id_6)
##             print u'Case_3_1 prepared'
##
##        def Case_3_1(self):
##            act_id = AOB_US.act_id['a6']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,3 )
##            print 'Case_3_1 ' + str(results)
########################### 7. Акт выгружен в OEBS, но затем изменена его дата (без договора)
##        @TestBase.prepare('Case_4_0')
##        def prepare_Case_4_0():
##             tm, test = MTestlib.proxy()
##             client_id_7, person_id_7  , order_id_7, contract_id_7, invoice_id_7, act_id_7,service_order_id_7 = Compares.com_4(paysys_id, person_type, dt,service_id,product_id)
##             dt_7 = test.ExecuteSQL('balance', 'select dt from t_act where  id = :act_id', {'act_id': act_id_7})[0]['dt']
##             dt_changed = MTestlib.some_days_ago(dt_7,1)
##             test.ExecuteSQL('balance', 'update (select  * from t_act where  id = :act_id ) set dt = :dt_changed ', {'act_id': act_id_7, 'dt_changed':dt_changed})
##             AOB_US.act_id['a7'] = act_id_7
##             Compares.insert_into_t_act_check(act_id_7)
##             print u'Case_4_0 prepared'
##
##        def Case_4_0(self):
##            act_id = AOB_US.act_id['a7']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,4 )
##            print 'Case_4_0 ' + str(results)
########################### 8. Акт выгружен в OEBS, но затем изменена его дата (по договору)
##        @TestBase.prepare('Case_4_1')
##        def prepare_Case_4_1():
##             tm, test = MTestlib.proxy()
##             client_id_8, person_id_8, order_id_8, contract_id_8, invoice_id_8, act_id_8,service_order_id_8 = Compares.com_4(paysys_id, person_type, dt ,service_id,product_id, contract_type)
##             dt_8 = test.ExecuteSQL('balance', 'select dt from t_act where  id = :act_id', {'act_id': act_id_8})[0]['dt']
##             dt_changed = MTestlib.some_days_ago(dt_8,1)
##             test.ExecuteSQL('balance', 'update (select  * from t_act where  id = :act_id ) set dt = :dt_changed ', {'act_id': act_id_8, 'dt_changed':dt_changed})
##             AOB_US.act_id['a8'] = act_id_8
##             Compares.insert_into_t_act_check(act_id_8)
##             print u'Case_4_1 prepared'
##
##        def Case_4_1(self):
##            act_id = AOB_US.act_id['a8']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,5 )
##            print 'Case_4_1 ' + str(results)
########################### 9. Акт выгружен в OEBS, но затем изменен плательщик (без договора)
##        @TestBase.prepare('Case_5_0')
##        def prepare_Case_5_0():
##             tm, test = MTestlib.proxy()
##             client_id_9, person_id_9, order_id_9, contract_id_9, invoice_id_9, act_id_9,service_order_id_9 = Compares.com_4(paysys_id, person_type, dt,service_id,product_id)
##             person_id_9 = MTestlib.create_person(client_id_9, person_type)
##             test.ExecuteSQL('balance', 'update (select  * from t_invoice where  id = :invoice_id ) set person_id = :person_id ', {'invoice_id': invoice_id_9, 'person_id':person_id_9})
##             AOB_US.act_id['a9'] = act_id_9
##             Compares.insert_into_t_act_check(act_id_9)
##             print u'Case_5_0 prepared'
##
##        def Case_5_0(self):
##            act_id = AOB_US.act_id['a9']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,5 )
##            print 'Case_5_0 ' + str(results)
########################### 10. Акт выгружен в OEBS, но затем изменен плательщик (по договору)
##        @TestBase.prepare('Case_5_1')
##        def prepare_Case_5_1():
##             tm, test = MTestlib.proxy()
##             client_id_10, person_id_10, order_id_10, contract_id_10, invoice_id_10, act_id_10,service_order_id_10 = Compares.com_4(paysys_id, person_type, dt,service_id,product_id, contract_type)
##             person_id_10 = MTestlib.create_person(client_id_10, person_type)
##             test.ExecuteSQL('balance', 'update (select  * from t_invoice where  id = :invoice_id ) set person_id = :person_id ', {'invoice_id': invoice_id_10, 'person_id':person_id_10})
##             AOB_US.act_id['a10'] = act_id_10
##             Compares.insert_into_t_act_check(act_id_10)
##             print u'Case_5_1 prepared'
##
##        def Case_5_1(self):
##            act_id = AOB_US.act_id['a10']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,5 )
##            print 'Case_5_1 ' + str(results)
########################### 11. Нет расхождений (без договора)
##        @TestBase.prepare('Case_0_0')
##        def prepare_Case_0_0():
##             tm, test = MTestlib.proxy()
##             client_id_11, person_id_11, order_id_11, contract_id_11, invoice_id_11, act_id_11 ,service_order_id_11= Compares.com_4(paysys_id, person_type, dt,service_id,product_id)
##             AOB_US.act_id['a11'] = act_id_11
##             Compares.insert_into_t_act_check(act_id_11)
##             print u'Case_0_0 prepared'
##
##        def Case_0_0(self):
##            act_id = AOB_US.act_id['a11']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,0 )
##            print 'Case_0_0 ' + str(results)
########################### 12. Нет расхождений (по договору)
##        @TestBase.prepare('Case_0_1')
##        def prepare_Case_0_1():
##             tm, test = MTestlib.proxy()
##             client_id_12, person_id_12, order_id_12, contract_id_12, invoice_id_12, act_id_12,service_order_id_12 = Compares.com_4(paysys_id, person_type, dt,service_id,product_id, contract_type)
##             AOB_US.act_id['a12'] = act_id_12
##             Compares.insert_into_t_act_check(act_id_12)
##             print u'Case_0_1 prepared'
##
##        def Case_0_1(self):
##            act_id = AOB_US.act_id['a12']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,0 )
##            print 'Case_0_1 ' + str(results)









#######################
##        @TestBase.prepare('test_Case13')
##        def prepare_Case13():
##             tm, test = MTestlib.proxy()
##             print u'13. Акт выгружен в OEBS, дата = 2014.10.01 (без договора)'
##             dt_changed = datetime.datetime(2014,10,1)
##             client_id_13, person_id_13  , order_id_13, contract_id_13, invoice_id_13, act_id_13,service_order_id_13 = Compares.com_4(paysys_id, person_type, dt_changed,service_id,product_id)
##             AOB_US.act_id['a13'] = act_id_13
##             Compares.insert_into_t_act_check(act_id_13)
##
##        def test_Case13(self):
##            act_id = AOB_US.act_id['a13']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,0 )
##            print 'case13 ' + str(results)
###########################
##        @TestBase.prepare('test_Case14')
##        def prepare_Case14():
##             tm, test = MTestlib.proxy()
##             print u'14. Акт выгружен в OEBS, дата  = 2014.01.01 (по договору)'
##             dt_changed = datetime.datetime(2014,10,1)
##             client_id_14, person_id_14, order_id_14, contract_id_14, invoice_id_14, act_id_14,service_order_id_14 = Compares.com_4(paysys_id, person_type, dt_changed,service_id,product_id, contract_type)
##             AOB_US.act_id['a14'] = act_id_14
##             Compares.insert_into_t_act_check(act_id_14)
##        def test_Case14(self):
##            act_id = AOB_US.act_id['a14']
##            external_id  = test.ExecuteSQL('balance', 'select external_id from t_act where id = :act_id ', {'act_id': act_id})[0]['external_id']
##            results = Compares.check(compare_type, external_id, AOB_US.cmp_id,0 )
##            print 'case14 ' + str(results)

if __name__ == '__main__':
    import nose

    case_list = [
        '__main__:AOB_US.Case_1_0'
        ##        , '__main__:AOB_US.Case_1_1'
        ##         '__main__:AOB_US.Case_2_0'
        ##        , '__main__:AOB_US.Case_2_1'
        ##        , '__main__:AOB_US.Case_3_0'
        ##        , '__main__:AOB_US.Case_3_1'
        ##        , '__main__:AOB_US.Case_4_0'
        ##        , '__main__:AOB_US.Case_4_1'
        ##        , '__main__:AOB_US.Case_5_0'
        ##        , '__main__:AOB_US.Case_5_1'
        ##        , '__main__:AOB_US.Case_0_0'
        ##        , '__main__:AOB_US.Case_0_1'
        ###        , '__main__:AOB_US.test_Case13'
        #####       , '__main__:AOB_US.test_Case14'
    ]
    nose.core.runmodule(case_list, argv=['-v', '-s'])
    # nose.core.runmodule([], argv = ['-v', '-s'])
##    #nose.core.runmodule(argv = ['-v', '-s'])
##    nose.core.runmodule(['__main__:AOB_US.test_Case3', '__main__:AOB_US.test_Case4'], argv = ['-v', '-s'])







##    dt_end = datetime.datetime.now()
##    print u'Врем вполнения:' + str(dt_begin) + '   '+ str(dt_end)
##    print u'Общее время: ' + str(dt_end - dt_begin)


##    + ',' + person_id_2 + ',' + person_id_3 + ',' + person_id_4 + ',' + person_id_5 + ',' + person_id_6 + ',' + person_id_7 + ',' + person_id_8 + ',' + person_id_9 + ',' + person_id_10 + ',' + person_id_11 + ',' + person_id_12
######### апдейтим команду в пайкроне для запуска

##    return act_id_1
##    acts  = []
##    acts.append (act_id_1)


########  return act_id_1,act_id_2,act_id_3,act_id_4,act_id_5,act_id_6,act_id_7,act_id_8,act_id_9,act_id_10
