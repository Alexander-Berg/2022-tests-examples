# coding=utf-8

import datetime

import pytest

import balance.balance_db as db
from balance import balance_steps as steps
from check import check_defaults

QTY = 55.7
BASE_DT = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
# NEW_CAMPAIGNS_DT = BASE_DT + datetime.timedelta(days=1)
# BASE_DT=datetime.datetime.now()
check_list = ['aob_sw', 'aob_tr', 'aob_us', 'aob', 'aob_ua', 'aob_vertical', 'aob_taxi']


# check_list = ['aob_tr']


# def update_command(cmp_name):
#     # TODO: use query_params!
#     command = 'update (select * from bo.t_pycron_descr where name  =\'' + str(
#         cmp_name) + '\') set command = \'/usr/lib/pymodules/python2.7/dcs/bin/compare.sh ' + str(
#         cmp_name[4:]) + '\''
#     db.balance().execute(str(command))

def check_date(act_list):
    if not act_list:
        return True
    descr = act_list.keys()[0]
    act_id = act_list[str(descr)]['id']
    query = 'select dt from bo.T_ACT where id= :act_id'
    query_params = {'act_id': act_id}
    dt = db.balance().execute(query, query_params)[0]['dt']
    if dt.date() < BASE_DT.date():
        return True
    return False


def act_taxi_creator():
    order_sum = 300
    order_text = 250
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '234567890'})
    steps.ClientSteps.link(client_id, 'clientuid32')
    # contract_start_dt= datetime.datetime.now().replace(day=1).replace(month=1)-datetime.timedelta(days=1)
    taxi_order_dt = BASE_DT
    payment_type = 'cash'
    # создаем договор с Такси
    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay',
                                                                   {'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    # обновляем матвьюшку MV_PARTNER_TAXI_CONTRACT с договорами такси
    db.balance().execute("begin dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); end;")
    # создаем заказ в такси
    steps.CommonSteps.log(steps.TaxiSteps.create_order)(client_id, taxi_order_dt, payment_type, order_sum, order_text)
    steps.TaxiSteps.generate_acts(contract_id, BASE_DT, 111)
    query = "select ID from t_act where client_id=:client_id"
    query_params = {'client_id': client_id}
    act_id = db.balance().execute(query, query_params)[0]['id']
    return act_id


def act_creator(type):
    client_id = None or steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, check_defaults.data[type]['person_category'])
    steps.ClientSteps.link(client_id, 'chihiro-test-0')

    contract_id = None

    service_id = check_defaults.data[type]['service_id_direct']
    product_id = check_defaults.data[type]['product_id_direct']

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=service_id, product_id=product_id)

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': BASE_DT}]
    request_id = steps.RequestSteps.create(client_id, orders_list, invoice_dt=BASE_DT)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, check_defaults.data[type]['paysys_id'],
                                                 credit=0,
                                                 contract_id=contract_id,
                                                 overdraft=0, endbuyer_id=None)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=BASE_DT)

    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 23, 'Money': 0}, 0, BASE_DT)
    steps.ActsSteps.generate(client_id, force=1, date=BASE_DT)
    query = "select ID from t_act where client_id=:client_id"
    query_params = {'client_id': client_id}
    act_id = db.balance().execute(query, query_params)[0]['id']
    return act_id


@pytest.fixture(scope="module")
def act_list(request):
    acts_list_cache = request.config.cache.get("example/value", None)
    if check_date(acts_list_cache):
        global check_list
        check_list = ['aob_sw', 'aob_tr', 'aob_us', 'aob', 'aob_ua', 'aob_vertical', 'aob_taxi']
        request.config.cache.set("example/value", None)
        acts_list_cache = request.config.cache.get("example/value", None)
    acts_list = {}
    if acts_list_cache is None:
        for type in check_list:
            description = 'Act_without_changes'
            act_id = act_taxi_creator() if type == 'aob_taxi' else act_creator(type)
            # eid=db.balance().execute('select external_id from t_act where id= :id', {'id': act_id})[0]['external_id']
            # time.sleep(1)
            steps.CommonSteps.export('OEBS', 'Act', act_id)
            # check_utils.oebs_export('Act', act_id)
            acts_list['{0}_{1}'.format(description, type)] = {'id': act_id, 'expected': 0}

            description = 'Act_not_exported_to_oebs'
            act_id = act_taxi_creator() if type == 'aob_taxi' else act_creator(type)
            acts_list['{0}_{1}'.format(description, type)] = {'id': act_id, 'expected': 1}

            description = 'Act_hidden_in_billing'
            act_id = act_taxi_creator() if type == 'aob_taxi' else act_creator(type)
            # time.sleep(1)
            steps.CommonSteps.export('OEBS', 'Act', act_id)
            # check_utils.oebs_export('Act', act_id)
            # steps.ActsSteps.hide(act_id)
            db.balance().execute('update (select  * from t_act where  id = :id) set hidden=4', {'id': act_id})
            acts_list['{0}_{1}'.format(description, type)] = {'id': act_id, 'expected': 2}

            description = 'Act_with_changed_sum'
            act_id = act_taxi_creator() if type == 'aob_taxi' else act_creator(type)
            # time.sleep(1)
            steps.CommonSteps.export('OEBS', 'Act', act_id)
            # check_utils.oebs_export('Act', act_id)
            query = 'update (select  * from t_act where  id = :id) set amount = 113'
            query_params = {'id': act_id}
            db.balance().execute(query, query_params)
            acts_list['{0}_{1}'.format(description, type)] = {'id': act_id, 'expected': 3}

            description = 'Act_with_changed_date'
            act_id = act_taxi_creator() if type == 'aob_taxi' else act_creator(type)
            # time.sleep(1)
            steps.CommonSteps.export('OEBS', 'Act', act_id)
            # check_utils.oebs_export('Act', act_id)
            date2 = BASE_DT + datetime.timedelta(minutes=3)
            query = 'update (select  * from t_act where id = :id) set dt =:date2'
            query_params = {'id': act_id, 'date2': date2}
            db.balance().execute(query, query_params)
            acts_list['{0}_{1}'.format(description, type)] = {'id': act_id, 'expected': 4}

            description = 'Act_with_changed_person'
            act_id = act_taxi_creator() if type == 'aob_taxi' else act_creator(type)
            # time.sleep(1)
            steps.CommonSteps.export('OEBS', 'Act', act_id)
            # check_utils.oebs_export('Act', act_id)
            #
            query = """
              select p.type as type, p.client_id as id
              from (t_person p
              left join t_invoice i on i.person_id=p.id)
              left join t_act a on a.invoice_id=i.id
              where a.id= :act_id"""
            query_params = {'act_id': act_id}
            result = db.balance().execute(query, query_params)
            types, client_id = result[0]['type'], result[0]['id']
            person_id = steps.PersonSteps.create(client_id, types)
            #
            query = "select invoice_id as id from t_act where id= :act_id"
            query_params = {'act_id': act_id}
            invoice_id = db.balance().execute(query, query_params)[0]['id']
            query = 'update (select  * from t_invoice where  id = :invoice_id) set person_id = :person_id2'
            query_params = {'invoice_id': invoice_id, 'person_id2': person_id}
            db.balance().execute(query, query_params)
            acts_list['{0}_{1}'.format(description, type)] = {'id': act_id, 'expected': 5}


        request.config.cache.set("example/value", acts_list)

    acts_list = request.config.cache.get("example/value", None)
    return acts_list


@pytest.mark.parametrize("type", check_list)
@pytest.mark.parametrize("description", ['Act_without_changes'
    , 'Act_not_exported_to_oebs'
    , 'Act_hidden_in_billing'
    , 'Act_with_changed_sum'
    , 'Act_with_changed_date'
    , 'Act_with_changed_person'
                                         ]
    , ids=['without_changes'
        , 'not_exported_to_oebs'
        , 'hidden_in_billing'
        , 'with_changed_sum'
        , 'with_changed_date'
        , 'with_changed_person'
           ]
                         )
def test_aob(act_list, description, type):
    act_ids = act_list
    act_id = act_ids['{0}_{1}'.format(description, type)]['id']


if __name__ == "__main__":
    # test_1('aob_sw')
    # undef_test()
    pass
