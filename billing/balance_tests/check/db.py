# coding=utf-8
import datetime
import time

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from check import defaults
from check.defaults import Services


def get_cmp_id_list(check_list):
    cmp_ids = {}
    for type_ in check_list:
        cmp_ids[type_] = get_cmp_id(type_)
    return cmp_ids


def get_cmp_id(type_):
    name = type_[0:3] if type_[0:3] in ['aob', 'iob'] else type_
    table = 'cmp.' + str(type_) + '_cmp' if type_ in ['{0}_auto'.format(name), '{0}_sw'.format(name),
                                                      '{0}_tr'.format(name), '{0}_ua'.format(name),
                                                      '{0}_us'.format(name)] else 'cmp.{0}_cmp'.format(name)
    query = 'select id from ' + table + ' where dt = ( select max(dt) as dt from ' + table + ')'
    if type_ in ['{0}_taxi'.format(name), '{0}_vertical'.format(name), '{0}_market'.format(name),
                 'aob', 'iob']:
        query = 'select * from cmp.' + str(name) + '_cmp where  dt = (select max(dt) as dt from cmp.' + str(
            name) + '_cmp where firm_id =' + str(
            defaults.data[type_]['firm_id']) + ') and firm_id = ' + str(defaults.data[type_]['firm_id'])
    new_id = api.test_balance().ExecuteSQL('cmp', query)[0]['id']
    reporter.log(u'Сверка отработала, cmp_id = ' + str(new_id))
    return new_id


def get_max_cmp_dt(type_):
    table = 'cmp.' + str(type_) + '_cmp' if type_ not in ['aob', 'aob_taxi', 'aob_vertical', 'aob_market',
                                                          'aob_services', 'iob', 'iob_taxi', 'iob_vertical',
                                                          'iob_market', 'iob_services'] \
        else 'cmp.{0}_cmp'.format(type_[0:3])
    query_1 = 'select id from ' + table + ' where dt = ( select max(dt) as dt from ' + table + ')'
    if type_ in ['aob', 'aob_taxi', 'aob_vertical', 'aob_market', 'iob', 'iob_taxi', 'iob_vertical', 'iob_market',
                 'aob_services', 'iob_services']:
        query_1 = 'select * from ' + table + ' where  dt = (select max(dt) as dt from ' + table + ' where firm_id =' + str(
            defaults.data[type_]['firm_id']) + ') and firm_id = ' + str(
            defaults.data[type_]['firm_id'])
    new_id = api.test_balance().ExecuteSQL('cmp', query_1)[0]['id'] if api.test_balance().ExecuteSQL('cmp',
                                                                                                     query_1) else None
    query = 'select finish_dt from ' + table + ' where id = ' + str(new_id)
    if type_ in ['aob', 'aob_taxi', 'aob_vertical', 'aob_market', 'iob', 'iob_taxi', 'iob_vertical', 'iob_market',
                 'aob_services', 'iob_services']:
        query_1 += ' and firm_id = ' + str(defaults.data[type_]['firm_id'])
    return api.test_balance().ExecuteSQL('cmp', query)[0]['finish_dt'] if new_id else None


# TODO move to balance_db
def get_data_for_new_person_by_old_act(act_id):
    query = """
      select p.type as person_type,
             p.client_id as client_id,
             i.id as invoice_id
      from (t_person p
      left join t_invoice i on i.person_id=p.id)
      left join t_act a on a.invoice_id=i.id
      where a.id= :act_id"""
    query_params = {'act_id': act_id}
    return db.balance().execute(query, query_params)


# TODO move to balance_db
def get_completion_qty(order_id):
    query = 'select completion_qty from bo.V_COMPLETION_HISTORY where ORDER_ID = :order_id'
    query_params = {'order_id': order_id}
    return db.balance().execute(query, query_params)[0]['completion_qty']


def get_completion_fixed_qty_by_order_id(order_id):
    query = 'select completion_fixed_qty from bo.T_ORDER where ID = :order_id'
    query_params = {'order_id': order_id}
    return db.balance().execute(query, query_params)[0]['completion_fixed_qty']


# TODO move to balance_db
def get_order_id(service_order_id, service_id):
    query = 'select id from bo.t_order where service_order_id = :service_order_id and service_id = :service_id'
    query_params = {'service_order_id': service_order_id, 'service_id': service_id}
    return db.balance().execute(query, query_params)[0]['id']


# TODO move to balance_db
def update_date_shipment_by_service_id(service_id, date):
    query = 'update bo.t_shipment set dt = :date where service_order_id = :invoice_id'
    query_params = {'date': date, 'invoice_id': service_id}
    db.balance().execute(query, query_params)


# TODO move to balance_db
def update_date_consume_by_invoice(invoice_id, date):
    query = 'update bo.t_consume set dt = :date where invoice_id = :invoice_id'
    query_params = {'date': date, 'invoice_id': invoice_id}
    db.balance().execute(query, query_params)
    query = 'update bo.t_receipt set dt = :date where invoice_id = :invoice_id'
    query_params = {'date': date, 'invoice_id': invoice_id}
    db.balance().execute(query, query_params)
    query = 'select client_id from bo.t_invoice where id = :invoice_id'
    query_params = {'invoice_id': invoice_id}
    client_id = db.balance().execute(query, query_params)[0]['client_id']
    query = 'update bo.t_request set dt = :date where client_id = :client_id'
    query_params = {'date': date, 'client_id': client_id}
    db.balance().execute(query, query_params)
    query = 'update bo.t_request_order set dt = :date where client_id = :client_id'
    query_params = {'date': date, 'client_id': client_id}
    db.balance().execute(query, query_params)
    query = 'update bo.t_operation set dt = :date where invoice_id = :invoice_id'
    query_params = {'date': date, 'invoice_id': invoice_id}
    db.balance().execute(query, query_params)


# TODO move to balance_db, rename
def update_date(order_id, new_date):
    query = 'update (select * from bo.t_order where id = :order_id) set dt = :new_date'
    query_params = {'order_id': order_id, 'new_date': new_date}
    db.balance().execute(query, query_params)


def invoice_id_by_order(order_id):
    query = """
              select i.id
              from t_invoice i
              join t_consume c on i.id = c.invoice_id
              join t_order o on o.id = c.parent_order_id
              where o.id = :order_id
            """
    query_params = {'order_id': order_id}
    return db.balance().execute(query, query_params)[0]['id']


# TODO move to balance_db
def update_date_invoice(inv_id, new_date):
    query = 'update (select * from bo.t_invoice where id = :inv_id) set dt = :new_date, receipt_dt = :new_date, receipt_dt_1c= :new_date'
    query_params = {'inv_id': inv_id, 'new_date': new_date}
    db.balance().execute(query, query_params)


# TODO move to balance_db
def get_person_type_and_cliend_id_by_invoice(inv_id):
    query = """
            select p.type as type, p.client_id as id
            from t_person p
            left join t_invoice i on i.person_id=p.id
            where i.id= :inv_id"""
    query_params = {'inv_id': inv_id}
    return db.balance().execute(query, query_params)


# TODO move to steps.CommonSteps
def wait_export(classname, type_, object_id):
    sql = "select state as val from t_export where type = :type and object_id = :object_id and classname = :classname"
    sql_params = {'object_id': object_id, 'type': type_, 'classname': classname}
    steps.CommonSteps.wait_for(sql, sql_params, value=1)


# TODO: move to steps.CommonSteps
def flash_export(classname, type_, object_id):
    sql = "update bo.t_export set priority = -1 where type = :type and object_id = :object_id and classname = :classname"
    sql_params = {'object_id': object_id, 'type': type_, 'classname': classname}
    db.balance().execute(sql, sql_params)


# TODO move to get_invoice_by_contract; assert?
def get_invoice_by_contract_id(contract_id):
    query = 'select id from bo.t_invoice where contract_id = :contract_id'
    query_params = {'contract_id': contract_id}
    acts = db.balance().execute(query, query_params)
    assert len(acts) == 1
    return acts[0]['id']


# TODO move to balance_db; assert?
def get_act_by_contract_id(contract_id):
    query = """
        select a.id, a.external_id
        from bo.t_invoice i
        join bo.t_act a on a.invoice_id = i.id
        where contract_id = :contract_id
    """
    until = time.time() + 120
    while time.time() < until:
        acts = db.balance().execute(query, {'contract_id': contract_id})
        if acts:
            break
        time.sleep(5)

    assert len(acts) == 1
    return acts[0]['id'], acts[0]['external_id']


# TODO alreday created: use get_act_by_id
def get_invoice_by_act(act_id):
    query = 'select invoice_id, external_id from bo.t_act where id = :act_id'
    query_params = {'act_id': act_id}
    invoice = db.balance().execute(query, query_params)
    assert len(invoice) == 1
    return invoice[0]['invoice_id'], db.balance().execute('select external_id from bo.t_invoice where id = :id',
                                                          {'id': invoice[0]['invoice_id']})[0]['external_id']


def get_consue_sum_by_order(order_id):
    query = """
                select sum(consume_qty) as qty
                from bo.T_CONSUME
                where parent_order_id = :order_id
                """
    result = db.balance().execute(query, {'order_id': order_id})
    return result[0]['qty']


def create_data_in_market(market_data):
    for deskr in market_data.keys():
        query = 'insert into cmp.t_market_consumption values({0},{1},{2})'.format(Services.market,
                                                                                  market_data[deskr][1],
                                                                                  market_data[deskr][2])
        api.test_balance().ExecuteSQL('market_dcs', query)


def _create_data_in_market(market_data):
    query = 'insert into cmp.t_market_consumption values({0},{1},{2})'.format(*market_data)
    api.test_balance().ExecuteSQL('market_dcs', query)


# TODO move to balance_db;
def get_service_order_id_by_act_id(act_id):
    query = """
        select service_order_id
        from bo.t_act a
        join bo.t_invoice i on a.invoice_id=i.id
        join bo.t_consume c on c.invoice_id = i.id
        join bo.t_order o on c.parent_order_id=o.id
        where a.id= :act_id
    """
    services = db.balance().execute(query, {'act_id': act_id})
    assert len(services) == 1
    return services[0]['service_order_id']


# TODO move to get_order_by_id
def get_consume_qty(order_id):
    query = 'select consume_qty from bo.t_order where id = :order_id'
    query_params = {'order_id': order_id}
    return db.balance().execute(query, query_params)[0]['consume_qty']


# TODO move to get_order_by_id
def get_date_by_order(order_id):
    query = 'select dt from bo.t_order where id = :order_id'
    query_params = {'order_id': order_id}
    return db.balance().execute(query, query_params)[0]['dt']


def get_completion_from_partner_history(place_id, page_id, completion_type):
    query = """
                select shows, clicks, bucks, mbucks, hits
                from bo.t_partner_completion_buffer
                where place_id = :place_id and page_id = :page_id and completion_type = :completion_type
            """
    query_params = {'place_id': place_id, 'page_id': page_id, 'completion_type': completion_type}
    res = db.balance().execute(query, query_params)
    shows, clicks, bucks, mbucks, hits = res[0]['shows'], res[0]['clicks'], res[0]['bucks'], res[0]['mbucks'], res[0][
        'hits']
    return shows, clicks, bucks, mbucks, hits


def insert_into_partner_completion_buffer(place_id, page_id, completion_type, source_id=1, shows=113, clicks=2,
                                          bucks=12,
                                          hits=9, mbucks=83249, date=None):
    if date is None:
        date = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime("%d.%m.%y 00:00:00")
    query = """
              insert
              into T_PARTNER_COMPLETION_BUFFER
              (place_id, page_id, dt, type, shows, clicks, bucks, mbucks, completion_type,source_id, hits)
              values( :place_id, :page_id, to_date (:date, 'DD.MM.RR HH24:MI:SS'), 0, :shows, :clicks, :bucks, :mbucks, :completion_type, :source_id, :hits)
            """
    query_params = {'place_id': place_id, 'page_id': page_id, 'date': date, 'completion_type': completion_type,
                    'source_id': source_id, 'shows': shows, 'clicks': clicks, 'bucks': bucks, 'mbucks': mbucks,
                    'hits': hits}
    db.balance().execute(query, query_params)


def get_new_search_id():
    return db.balance().execute("SELECT S_TEST_PLACE_SEARCH_ID.nextval AS search FROM dual")[0]['search']


def insert_into_partner_tags_stat3(date, place_id, tag_id, page_id, shows=0, clicks=0, bucks=0, source_id=17,
                                   bucks_rs=None, orders=0):
    if date is None:
        date = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime("%d.%m.%y 00:00:00")
    query = """
              insert
              into T_PARTNER_TAGS_STAT3
              (dt, place_id, tag_id, page_id, shows, clicks, bucks, completion_type, vid, type, source_id, orders, bucks_rs, clicksa)
              VALUES
              (to_date (:date, 'DD.MM.RR HH24:MI:SS'), :place_id, :tag_id, :page_id, :shows, :clicks, :bucks, 1, null, null, :source_id, :orders, :bucks_rs, :clicks)
            """
    query_params = {
        'place_id': place_id, 'tag_id': tag_id, 'date': date, 'shows': shows, 'clicks': clicks, 'bucks': bucks,
        'orders': orders, 'bucks_rs': bucks_rs, 'source_id': source_id, 'page_id': page_id
    }
    db.balance().execute(query, query_params)


def insert_into_partner_dsp_stat(place_id, block_id, dsp_id, date=None, dsp_charge=423897, shows=9, partner_reward=15):
    if date is None:
        date = datetime.datetime.now() - datetime.timedelta(days=1)
    query = """
                insert
                into bo.t_partner_dsp_stat
                (dt, place_id, block_id, dsp_id, hits, dsp_charge, partner_reward, shows, total_response_count,
                total_bid_sum)
                values ( :date, :place_id, :block_id, :dsp_id, 3, :dsp_charge, :partner_reward , :shows, 0, 0)

            """
    query_params = {'date': date, 'place_id': place_id, 'block_id': block_id, 'dsp_id': dsp_id,
                    'dsp_charge': dsp_charge, 'shows': shows, 'partner_reward': partner_reward}
    db.balance().execute(query, query_params)
    return shows, dsp_charge, partner_reward


def get_data_from_partner_dsp_stat(place_id, block_id, dsp_id, date=None):
    if date is None:
        date = datetime.datetime.now() - datetime.timedelta(days=2)
    query = """
                select sum(shows) AS shows, sum(dsp_charge) as dsp_charge, sum(partner_reward) as partner_reward
                from bo.t_partner_dsp_stat
                where place_id = :place_id and block_id = :block_id and dsp_id = :dsp_id and dt >= :date
            """
    query_params = {'place_id': place_id, 'block_id': block_id, 'dsp_id': dsp_id, 'date': date}
    res = db.balance().execute(query, query_params)
    shows, dsp_charge, partner_reward = res[0]['shows'], res[0]['dsp_charge'], res[0]['partner_reward']
    return shows, dsp_charge, partner_reward


def get_max_page_id_from_partner_completion_buffer():
    query = 'select max(PAGE_ID) as val from bo.T_PARTNER_COMPLETION_BUFFER'
    return db.balance().execute(query)[0]['val']


def delete_from_partner_completion_buffer(place_id, page_id, source_id):
    prev_day = (datetime.datetime.now() - datetime.timedelta(days=1)).strftime("%d.%m.%y 00:00:00")
    query = """
                delete
                from bo.T_PARTNER_COMPLETION_BUFFER
                where place_id = {0}
                and page_id = {1}
                and source_id = {2}
                and dt = to_date('{3}', 'dd.mm.yy HH24:MI:SS')
            """.format(place_id, page_id, source_id, prev_day)
    return db.balance().execute(query)


def get_completions_by_service_id_and_service_order_id(service_id, service_order_id):
    query = """
                select bucks, shows, money
                from bo.t_shipment
                where service_id = {0}
                and service_order_id = {1}
                union all
                select bucks, shows, money
                from bo.t_completion_history
                where order_id =
                (select id
                from bo.t_order
                 where service_id = {0}
                 and service_order_id = {1})
            """.format(service_id, service_order_id)
    return db.balance().execute(query)


def get_collateral_id_by_contract_id(contract_id):
    query = """
        select id
            from bo.T_CONTRACT_COLLATERAL
            where contract2_id = :contract_id
            and num is null
            """
    return db.balance().execute(query, {'contract_id': contract_id})[0]['id']


def get_zero_collateral_id_by_contract_id(contract_id):
    query = """
        select id
            from bo.T_CONTRACT_COLLATERAL
            where contract2_id = :contract_id
            and num is not null
            """
    return db.BalanceBO().execute(query, {'contract_id': contract_id})[0]['id']


def get_invoice_by_order_id(order_id):
    query = """
               select * from t_invoice
               where id = (
                   select invoice_id
                   from t_consume
                   where parent_order_id = :order_id
                   and rownum = 1
               )
            """
    res = db.balance().execute(query, {'order_id': order_id})
    assert len(res) == 1
    return res


def get_payment_data(payment_id):
    query = """
               select * from t_payment
               where id = :payment_id
            """
    res = db.balance().execute(query, {'payment_id': payment_id})
    assert len(res) == 1
    return res


def get_side_payment_data(payment_id):
    query = """
               select * from t_partner_payment_stat
               where id = :payment_id
            """
    res = db.balance().execute(query, {'payment_id': payment_id})
    assert len(res) == 1
    return res

def get_payment_service_product(external_id):
    query = """
               select * from t_service_product
               where external_id = :external_id
            """
    res = db.balance().execute(query, {'external_id': external_id})
    assert len(res) == 1
    return res


def update_adfox_completions(service_order_id, service_id, completion_qty, order_id=None):
    order_id = get_order_id(service_order_id, service_id) if not order_id else order_id
    db.balance().execute('update t_order set completion_qty = :completion_qty where id = :order_id',
                         {'order_id': order_id, 'completion_qty': completion_qty})
    db.balance().execute('update t_consume set completion_qty = :completion_qty where parent_order_id = :order_id',
                         {'order_id': order_id, 'completion_qty': completion_qty})
    db.balance().execute(
        'update T_SHIPMENT set CONSUMPTION = :completion_qty, units = :completion_qty where service_order_id = :service_order_id and service_id = :service_id',
        {'service_order_id': service_order_id, 'completion_qty': completion_qty, 'service_id': service_id})


def get_cmp_diff(cmp_id, cmp_name):
    query = """
            select *
            from cmp.{0}_cmp_data
            where cmp_id = {1}
        """.format(cmp_name, cmp_id)
    return api.test_balance().ExecuteSQL('cmp', query)


def is_check_run_finished(run_id):
    query = """
      select 1 success 
      from t_run 
      where id = :run_id 
        and finish_dt is not null
    """
    params = {'run_id': run_id}
    res = api.test_balance().ExecuteSQL('cmp', query, params)
    return bool(res and res[0]['success'])


def is_auto_analysis_running(cmp_name, cmp_id):
    query = """
    select 1 running
    from cmp.t_auto_analysis_run_info
    where check_code_name = :code_name
      and cmp_id = :cmp_id
    """
    params = {'code_name': cmp_name, 'cmp_id': cmp_id}
    res = api.test_balance().ExecuteSQL('cmp', query, params)
    return bool(res and res[0]['running'])
