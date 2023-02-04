# coding=utf-8
import datetime
import decimal
import json
from tenacity import retry, stop_after_attempt, wait_random

from dateutil.relativedelta import relativedelta
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_db, balance_api
from balance import balance_steps as steps
from balance.balance_steps import new_taxi_steps as taxi_steps, tarification_entity_steps
from balance.balance_db import balance
from btestlib import utils
from btestlib.constants import Firms, Currencies, PaymentType, TransactionType, TaxiOrderType, ContractSubtype, Services as Services_, DistributionContractType
from btestlib.constants import Services as btl_Services
from btestlib.constants import Products as btl_Products
from btestlib.data import person_defaults
from btestlib.data.defaults import Taxi
from check import db, yt_tables
from check.db import update_date_invoice, update_date_consume_by_invoice
from check.defaults import LAST_DAY_OF_PREVIOUS_MONTH, CLIENT_LOGIN, MEANINGLESS_DEFAULTS, Products, Services
from check.utils import relative_date, LAST_DAY_OF_MONTH, create_data_file, create_data_file_in_s3


QTY_CAMPAIGN = decimal.Decimal("23")
FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=1)
QTY = decimal.Decimal("55.7")
END_OF_MONTH = relative_date(months=-1, day=LAST_DAY_OF_MONTH)
_, _, first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()
PERSON_TYPE_UR = 'ur'
ACT_DT = utils.Date.get_last_day_of_previous_month()
CONTRACT_START_DT = utils.Date.first_day_of_month(datetime.datetime.now() - relativedelta(months=1))
TRANSACTION_LOG_DT = ACT_DT - datetime.timedelta(days=15)  # Устанавливаем TRANSACTION_LOG_DT примерно на середину месяца
DEFAULT_SERVICE_MIN_COST = decimal.Decimal('100')
COMPLETION_DT = utils.Date.first_day_of_month(ACT_DT)
DEFAULT_ADVANCE_PAYMENT_SUM = decimal.Decimal('100')


def create_all_dc_files(source_dict, file_date=None):
    # TODO: при переделывании на s3 сохранить параметр %(start_dt)s
    if not file_date:
        file_date = COMPLETION_DT

    creator_by_source = {
        1: create_data_in_bk,
        4: create_data_in_d_installs,
        8: create_data_in_dc_activations,
        11: create_data_in_tags3,
        14: create_data_in_market_cpa,
        17: create_data_in_taxi_distr,
        26: create_data_in_adapters,
        31: create_data_in_direct_rs,
    }
    for source in source_dict.keys():
        if source_dict.get(source):
            creator_by_source[source](source_dict[source], file_date)


def create_data_in_bk(data_list, file_date):
    orders = ''
    for data in data_list:
        data.update({'date': file_date.strftime("%Y%m%d000000")})
        one_order = '{date}\t{place_id}\t{page_id}\t{completion_type}\t0\t{shows}\t{clicks}\t{bucks}\t{mbucks}\t{hits}\n'.format(
            **data)
        orders += one_order
    orders += '#End'

    create_data_file_in_s3(
        content=orders,
        file_name='dc_bk_{}.csv'.format(file_date.strftime("%Y%m%d")),
        db_key='dc_stat_page_url_bk'
    )
    reporter.log(orders)


def create_data_in_market(market_data):
    if market_data:
        for row in market_data:
            market_data_dict = row['market_data']
            query = 'insert into cmp.market_orders values({0},{1},{2},{3},TO_DATE(\'{4}\', \'YYYY-MM-DD HH24:MI:SS\'))'.\
                format(
                    market_data_dict['service_order_id'],
                    market_data_dict['service_type'],         # service_type = 11
                    market_data_dict['consume_qty'],
                    0,
                    market_data_dict['date_by_order']
            )
            balance_api.test_balance().ExecuteSQL('market_dcs', query)
    return 1


def create_data_in_navi(navi_data):
    orders = 'completion_qty,consumption_qty,order_id\r\n'
    template = '{completion_qty},{consumption_qty},{order_id}\r\n'
    for line in navi_data:
        orders += template.format(**line)

    create_data_file_in_s3(
        content=orders,
        file_name='obn.csv',
        db_key='obn_navigator_importer_url',
    )
    reporter.log(orders)


def create_data_in_d_installs(data_list, file_date):
    orders = []
    for data in data_list:
        one_order = {"fielddate__ms": str(data['page_id']) + '00000', "install_new": str(data['shows']),
                     "fielddate": str(file_date.strftime("%Y-%m-%d 00:00:00")),
                     "path": "\tR\tYandex software on third party websites\tdownload-ff.yandex.ru\tUnique installation\t" + str(
                         data['place_id']) + "\t4\t",
                     "_comment_field_name_prefix": "path=^R^Yandex+software+on+third+party+websites^download-ff.yandex.ru^Unique+installation^" + str(
                         data['place_id']) + "^4^",
                     "path__lvl": 5}
        orders.append(one_order)

    date_str = file_date.strftime('%Y-%m-%d')
    create_data_file_in_s3(
        content=json.dumps({"values": orders}),
        file_name='dc_installs_{}.json'.format(date_str),
        db_key='dc_stat_page_url_d_installs',
    )
    reporter.log(orders)


def create_data_in_dc_activations(data_list, file_date):
    path = '//home/balance_reports/dcs/test/test_data/distribution-statistics/activations'
    yt_tables.dc_activations_create_data_in_yt(path, file_date, data_list)

    path = '{}/{}'.format(path, file_date.strftime('%Y-%m-%d'))
    url = 'http://hahn.yt.yandex.net/api/v3/read_table?path={}'.format(path)

    db_key = 'dc_stat_page_url_activations'
    query = 'update t_config set value_str = :value where item = :item'
    balance_api.test_balance().ExecuteSQL('cmp', query, {'value': url, 'item': db_key})

    reporter.log(data_list)


def create_data_in_taxi_distr(data_list, file_date):

    yt_data_list = []

    for data in data_list:
        yt_data_list.append({
                'utc_dt'    : data['date'],
                'clid'      : str(data['tag_id']),
                'product_id': str(data['place_id']),
                'commission': str(decimal.Decimal(data['bucks'])),
                'cost'      : '574.00',
                'quantity'  : str(data['shows']),
            })

    yt_tables.dc_taxi_distr_create_data_in_yt(yt_data_list)

    db_key = 'dc_stat_page_url_taxi_distr'
    date = datetime.datetime.now().strftime("%Y-%m-%d")
    path = '//home/balance_reports/dcs/test/test_data/taxi/taxi_distr/{}'.format(date)
    url = 'http://hahn.yt.yandex.net/api/v3/read_table?path={}'.format(path)

    query = 'update t_config set value_str = :value where item = :item'
    balance_api.test_balance().ExecuteSQL('cmp', query, {'value': url, 'item': db_key})



def create_data_in_tags3(data_list, file_date):
    template = '{date}\t{place_id}\t333\t{page_id}\t1\t{shows}\t{clicks}\t{bucks}\t\t1\n'
    orders = ''
    for data in data_list:
        orders += template.format(**data)
    orders += '#End\n'

    create_data_file_in_s3(
        content=orders,
        file_name='dc_tags3_{}.csv'.format(file_date.strftime("%Y%m%d")),
        db_key='dc_stat_page_url_tags3',
    )
    reporter.log(orders)


def create_data_in_direct_rs(data_list, file_date):
    template = '{date}\t2\t{tag_id}\t542\t1\t{shows}\t{clicks}\t{bucks}\t\t1\n'
    orders = ''
    for data in data_list:
        orders += template.format(**data)
    orders += '#End\n'

    create_data_file_in_s3(
        content=orders,
        file_name='dc_direct_rs_{}.csv'.format(file_date.strftime("%Y-%m-%d")),
        db_key='dc_stat_page_url_direct_rs',
    )
    reporter.log(orders)


def create_data_in_market_cpa(data_list, file_date):
    orders = ''
    template = '{tag_id};256;{date};{clicks};{orders};{bucks};{bucks_rs}\n'
    for data in data_list:
        orders += template.format(**data)

    create_data_file_in_s3(
        content=orders,
        file_name='dc_rs_market_cpa_{}.csv'.format(file_date.strftime("%Y-%m-%d")),
        db_key='dc_stat_page_url_rs_market_cpa',
    )
    reporter.log(orders)


def create_data_in_adapters(data_list, file_date):
    orders = ''
    template = '{dt};{clid_d};{clid_r};{money_d};{money_r};{installs}\r\n'
    for line in data_list:
        orders += template.format(**line)

    db_keys = [
        'dc_stat_page_url_addapter_ret_ds',
        'dc_stat_page_url_addapter_ret_com',
        'dc_stat_page_url_addapter_dev_ds',
        'dc_stat_page_url_addapter_dev_com',
    ]
    for db_key in db_keys:
        create_data_file_in_s3(
            content=orders,
            file_name='dc_addapter_{}.csv'.format(file_date.strftime("%Y-%m-%d")),
            db_key=db_key,
        )
    reporter.log(orders)


def create_ado_ado_awaps_data(data_list, db_key):
    columns = ['order_nmb', 'shows_accepted', 'shows_realized', 'budget_plan', 'budget_realized',
               'date_begin', 'date_end', 'product_type_nmb']

    lines = '\n'.join(['\t'.join([str(order[column]) for column in columns]) for order in data_list])
    create_data_file_in_s3(
        content=lines,
        file_name='ado_ado_awaps_2.csv',
        db_key=db_key,
    )
    reporter.log(lines)


def create_data_in_ado(data_list, db_key):
    orders = ''
    if data_list:
        for key in data_list:
            one_order = str(key['service_order_id']) + '	' + str(key['volume_accepted']) + '	' + \
                        str(key['billing_realized']) + '	' + str(key['billing_realized']) + '	' + \
                        str(key['billing_realized']) + '	' + str(key['date_begin']) + '	' + \
                        str(key['date_end']) + '	1	0\n'
            orders += one_order

        create_data_file_in_s3(
            content=orders,
            file_name='ado_ado_2.csv',
            db_key=db_key,
        )
    reporter.log(orders)


def create_awaps_data(data_list, db_key):
    columns = ['order_nmb', 'shows_accepted', 'shows_realized', 'budget_plan', 'budget_realized',
               'date_begin', 'date_end', 'product_type_nmb']

    lines = '\n'.join(['\t'.join([str(order[column]) for column in columns]) for order in data_list])
    create_data_file_in_s3(
        content=lines,
        file_name='ado_awaps_2.csv',
        db_key=db_key,
    )
    reporter.log(lines)


def create_prcbb_data(data_list):
    data_file = ''
    template = '{date}\t{place_id}\t{block_id}\t5\t{dsp_id}\t5\t{dsp_charge}\t{partner_reward}\t{shows}\t0\t0\t0\n'
    for data in data_list:
        data_file += template.format(**data)
    data_file += '#end\n'

    if len(data_list) > 1:
        data_file += '{date}\t1234567899999\t{block_id}\t5\t{dsp_id}\t5\t{dsp_charge}\t{partner_reward}\t{shows}\t0\t0\t0\n'.format(
            **data_list[0])

    create_data_file_in_s3(
        content=data_file,
        file_name='dsp.csv',
        db_key='prcbb_bk_importer_url',
    )
    reporter.log(data_file)



def create_taxi_prepay_orders(client_id, order_dt, prepaid=False, promocode_sum=0,
                              currency=Currencies.RUB, subsidy_sum=0):
    # добавляем открутки с типом order
    if subsidy_sum:
        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CASH,
                                     commission_sum=Taxi.order_commission_cash,
                                     currency=currency.iso_code, order_type=TaxiOrderType.commission,
                                     promocode_sum=promocode_sum)

        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CASH,
                                     commission_sum=Taxi.hiring_with_car_cash,
                                     currency=currency.iso_code, order_type=TaxiOrderType.hiring_with_car,
                                     promocode_sum=promocode_sum)

        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CASH,
                                     commission_sum=Taxi.childchair_cash,
                                     currency=currency.iso_code, order_type=TaxiOrderType.childchair,
                                     promocode_sum=promocode_sum)

        # Добавляем субсидию
        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CASH,
                                     commission_sum=0, currency=currency.iso_code,
                                     order_type=TaxiOrderType.subsidy, subsidy_sum=subsidy_sum)


    elif prepaid:
        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.PREPAID,
                                     commission_sum=Taxi.order_commission_prepaid,
                                     currency=currency.iso_code, order_type=TaxiOrderType.commission,
                                     promocode_sum=promocode_sum)
    else:
        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.PREPAID,
                                     commission_sum=Taxi.order_commission_prepaid,
                                     currency=currency.iso_code, order_type=TaxiOrderType.commission,
                                     promocode_sum=promocode_sum)
        steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CASH,
                                     commission_sum=Taxi.order_commission_cash,
                                     currency=currency.iso_code, order_type=TaxiOrderType.commission,
                                     promocode_sum=promocode_sum)
        if currency == Currencies.RUB:
            # добавляем открутки с типом childchair
            steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CARD,
                                         commission_sum=Taxi.childchair_card,
                                         currency=currency.iso_code, order_type=TaxiOrderType.childchair,
                                         promocode_sum=promocode_sum)


def create_taxi_postpay_orders(client_id, order_dt, promocode_sum=0,
                               currency=Currencies.RUB):
    # добавляем открутки с типом order
    steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CARD,
                                 commission_sum=Taxi.order_commission_card,
                                 currency=currency.iso_code, order_type=TaxiOrderType.commission,
                                 promocode_sum=promocode_sum)
    steps.TaxiSteps.create_order(client_id, order_dt, PaymentType.CORPORATE,
                                 commission_sum=Taxi.order_commission_corp,
                                 currency=currency.iso_code, order_type=TaxiOrderType.commission,
                                 promocode_sum=promocode_sum)


def create_partner_completions_postpay(context, client_id, contract_id, with_act=True, completion_dt=COMPLETION_DT):


    create_taxi_postpay_orders(client_id, completion_dt, currency=context.currency)
    if with_act:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, ACT_DT)


def create_partner_completions_postpay_tlog(context, client_id, contract_id, with_act=True, completion_dt=COMPLETION_DT):

    orders_data = taxi_steps.TaxiData.generate_default_orders_data_tlog(
        completion_dt, context.currency.iso_code, transaction_ids_range=[1, 999])
    taxi_steps.TaxiSteps.create_orders_tlog(client_id, orders_data)

    if with_act:
        taxi_steps.TaxiSteps.generate_acts(client_id, contract_id, ACT_DT)


def create_partner_completions_prepay(context, client_id, contract_id, subsidy_sum=0, promocode_sum=0, with_act=True, completion_dt=COMPLETION_DT, prepaid=False):

    create_taxi_prepay_orders(client_id, completion_dt, prepaid, currency=context.currency, subsidy_sum=subsidy_sum,
                              promocode_sum=promocode_sum)

    if with_act:
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, ACT_DT)


def create_partner_completions_prepay_tlog(context, client_id, contract_id, with_act=True, completion_dt=COMPLETION_DT):

    taxi_steps.TaxiSteps.pay_to_personal_account(decimal.Decimal('100000'), contract_id)

    orders_data = taxi_steps.TaxiData.generate_default_orders_data_tlog(
        completion_dt, context.currency.iso_code, transaction_ids_range=[1, 999])
    taxi_steps.TaxiSteps.create_orders_tlog(client_id, orders_data)

    if with_act:
        taxi_steps.TaxiSteps.generate_acts(client_id, contract_id, ACT_DT)



def delete_order_from_pct(client_id):
    balance().execute('delete from T_PARTNER_TAXI_STAT_AGGR where client_id = :client_id', {'client_id': client_id},
                      descr='Удаляем заказ из t_partner_taxi_stat_aggr')


def update_commission_sum(client_id):
    orders = balance().execute('select * from t_order where client_id = :client_id', {'client_id': client_id})
    for order in orders:
        balance().execute(
            'update t_shipment set consumption = 100 where service_id = :service_id and service_order_id = :service_order_id',
            {'service_id': order['service_id'],
             'service_order_id': order['service_order_id']},
            descr='Изменяем consumption в t_shipment')


def update_shipment_date(client_id, shipment_dt, update_dt=None):
    if update_dt is None:
        update_dt = shipment_dt

    orders = balance().execute('select * from t_order where client_id = :client_id', {'client_id': client_id})
    for order in orders:
        balance().execute(
            '''
                update t_shipment
                set dt = to_date('{dt}','{dt_format}'),
                  update_dt = to_date('{update_dt}', '{dt_format}')
                where service_id = :service_id
                  and service_order_id = :service_order_id
            '''.format(dt=shipment_dt.strftime('%d.%m.%y %H:%M:%S'),
                       update_dt=update_dt.strftime('%d.%m.%y %H:%M:%S'),
                       dt_format='DD.MM.YY HH24:MI:SS'),
            {'service_id': order['service_id'],
             'service_order_id': order['service_order_id']},
            descr='Изменяем дату открутки в t_shipment')
        balance().execute(
            '''update t_completion_history set start_dt = to_date('{dt}','DD.MM.YY HH24:MI:SS'), shipment_dt =  to_date('{dt}','DD.MM.YY HH24:MI:SS') where order_id = :order_id'''.format(
                dt=shipment_dt.strftime('%d.%m.%y %H:%M:%S')),
            {'order_id': order['id']},
            descr='Изменяем дату открутки в t_shipment')


@retry(stop=stop_after_attempt(5), wait=wait_random(min=1, max=3), reraise=True)
def export_with_retry(func):
    return func()


def create_contract_offer(is_offer=True):
    from btestlib.data.partner_contexts import ZEN_SPENDABLE_CONTEXT
    with reporter.step(u'Создаем расходный договор'):
        client_id = steps.ClientSteps.create(prevent_oebs_export=True)
        _, person_id, partner_contract_id, _ = steps.ContractSteps.create_partner_contract(ZEN_SPENDABLE_CONTEXT,
                                                                                           client_id=client_id,
                                                                                           is_offer=is_offer)

    # Обернул в retry, чтобы уйти от ошибки "Could not lock with nowait"
    export_with_retry(lambda: steps.ExportSteps.export_oebs(person_id=person_id,
                                                            client_id=client_id,
                                                            contract_id=partner_contract_id))

    return partner_contract_id


def create_client_and_contract(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                                                 {'is-partner': str(
                                                                     1 if context.contract_type == ContractSubtype.SPENDABLE else 0)},
                                                                 inn_type=person_defaults.InnType.RANDOM)
    services = context.contract_services or [context.service.id]

    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': FIRST_MONTH,
        'IS_SIGNED': FIRST_MONTH.isoformat(),
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [service.id for service in context.services],
        'FIRM': context.firm.id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id
    })

    return client_id, person_id, contract_id


PAYMENT_SERVICES = [
    Services_.TAXI.id,
    Services_.UBER.id,
    Services_.UBER_ROAMING.id
]
P_AMOUNT = decimal.Decimal('100.1')


def create_completions(context, client_id, contract_id, person_id, dt):
    import balance.balance_steps as steps

    if context.service == btl_Services.KINOPOISK_PLUS:
        steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt,
                                             [{'transaction_type': TransactionType.PAYMENT,
                                               'yandex_reward': P_AMOUNT,
                                               'internal': 1,
                                               'product_id': btl_Products.KINOPOISK_WITH_NDS.id}])


    if context.service in [btl_Services.GAS_STATIONS, btl_Services.UBER, btl_Services.TAXI,
                            btl_Services.RED_MARKET_BALANCE, btl_Services.ZAXI]:
        steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt,
                                             [{'transaction_type': TransactionType.PAYMENT,
                                               'yandex_reward': P_AMOUNT,}])

    # Для корпоративного Такси нужно передавать client_id и client_amount
    if context.service in [btl_Services.TAXI_CORP, btl_Services.TAXI_CORP_CLIENTS]:
        steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt,
                                             [{'transaction_type': TransactionType.PAYMENT,
                                               'client_id': client_id,
                                               'client_amount': P_AMOUNT,}])



    if context.service is btl_Services.CLOUD_143:
        product = btl_Products.CLOUD
        steps.PartnerSteps.create_cloud_completion(contract_id, dt, P_AMOUNT, product)

    if context.service in [btl_Services.TAXI_111]:

        order_act = {
            'dt': dt,
            'payment_type': PaymentType.CASH,
            'order_type': TaxiOrderType.commission,
            'commission_sum': P_AMOUNT,
            'currency': context.currency.iso_code,
        }
        taxi_steps.TaxiSteps.create_order(client_id, **order_act)


        order_dict = {'dt': dt,
                       'transaction_dt': dt,
                       'currency': context.currency.iso_code,
                       'service_id': btl_Services.TAXI_111.id,
                       'type': TaxiOrderType.commission,
                       'amount': P_AMOUNT,}


        taxi_steps.TaxiSteps.create_order_tlog(client_id, **order_dict)


def queries_for_mock_oebs(firm_id=None, act_map=None, on_dt=None, person_id=None):
    query = 'select oebs_org_id from t_firm_export where firm_id = :firm_id'
    query_params = {'firm_id': firm_id}
    result = balance().execute(query, query_params)
    oebs_org_id = result[0]['oebs_org_id']

    query = 'select apps.s_customer_trx_id.nextval from dual'
    result = balance().execute(query)
    customer_trx_id = result[0]['nextval']

    query = 'select apps.s_cust_account_id.nextval from dual'
    result = balance().execute(query)
    cust_account_id = result[0]['nextval']

    query = 'select apps.s_batch_source_id.nextval from dual'
    result = balance().execute(query)
    batch_source_id = result[0]['nextval']

    COMPLETE_FLAG = 'Y'
    amount = 1000

    query = 'insert into apps.ra_customer_trx_all values (:customer_trx_id, :trx_date, :trx_number, :bill_to_customer_id, :cust_trx_type_id, :batch_source_id, null, :org_id, :complete_flag)'
    query_params = {'customer_trx_id': customer_trx_id,
                    'trx_number': act_map['eid'],
                    'org_id': oebs_org_id,
                    'complete_flag': COMPLETE_FLAG,
                    'trx_date': on_dt,
                    'bill_to_customer_id': cust_account_id,
                    'cust_trx_type_id': 1,
                    'batch_source_id': batch_source_id
                    }
    balance().execute(query, query_params)

    query = 'insert into apps.hz_cust_accounts values (:cust_account_id, :orig_system_reference)'
    query_params = {'cust_account_id': cust_account_id,
                    'orig_system_reference': 'P' + str(person_id)
                    }
    balance().execute(query, query_params)

    query = 'insert into apps.ra_batch_sources_all  values (:batch_source_id, :name)'
    query_params = {'batch_source_id': batch_source_id,
                    'name': u'Импорт из биллинга'
                    }
    balance().execute(query, query_params)

    query = 'insert into apps.ra_customer_trx_lines_all  values (:customer_trx_id, :extended_amount)'
    query_params = {'customer_trx_id': customer_trx_id,
                    'extended_amount': amount
                    }
    balance().execute(query, query_params)


def create_act_for_aob(service, date, firm_id):

    from collections import namedtuple
    from btestlib.data import partner_contexts
    from btestlib.constants import Firms as firm
    from btestlib.constants import Services as const_services

    TAXI_GHANA_CHECK_USD_CONTEXT = partner_contexts.TAXI_GHANA_USD_CONTEXT.new(
        service=const_services.TAXI,
        contract_services=[const_services.TAXI_111.id, const_services.TAXI_128.id, const_services.TAXI.id],
    )


    partner_service = namedtuple('partner_service', 'id context')

    partner_services = {
        'kinopoisk':     partner_service(id=firm.KINOPOISK_9, context=partner_contexts.KINOPOISK_PLUS_CONTEXT),
        'taxi':          partner_service(id=firm.TAXI_13, context=partner_contexts.TAXI_RU_CONTEXT_CLONE),
        'taxi_bv':       partner_service(id=firm.TAXI_BV_22, context=partner_contexts.TAXI_BV_GEO_USD_CONTEXT),
        'taxi_kz':       partner_service(id=firm.TAXI_KAZ_24, context=partner_contexts.TAXI_KZ_CONTEXT),
        'taxi_am':       partner_service(id=firm.TAXI_AM_26, context=partner_contexts.TAXI_ARM_CONTEXT),
        'drive':         partner_service(id=firm.DRIVE_30, context=partner_contexts.DRIVE_CONTEXT),
        'israel_go':     partner_service(id=firm.YANDEX_GO_ISRAEL_35, context=partner_contexts.TAXI_ISRAEL_CONTEXT),
        'uber_ml_bv':    partner_service(id=firm.UBER_115, context=partner_contexts.TAXI_UBER_BV_AZN_USD_CONTEXT),
        'uber_ml_bv_byn':partner_service(id=firm.UBER_1088, context=partner_contexts.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT),
        'uber_az':       partner_service(id=firm.UBER_AZ_116,  context=partner_contexts.TAXI_AZARBAYCAN_CONTEXT),    # тут используется функция steps.SimpleApi.create_fake_tpt_row(...)
        'ya_cloud':      partner_service(id=firm.CLOUD_123, context=partner_contexts.CLOUD_RU_CONTEXT),
        'gas':           partner_service(id=firm.GAS_STATIONS_124, context=partner_contexts.GAS_STATION_RU_CONTEXT),
        'gas_sales':     partner_service(id=firm.GAS_STATIONS_124, context=partner_contexts.ZAXI_RU_CONTEXT),
        'uber_kz':       partner_service(id=firm.TAXI_CORP_KZT_31,  context=partner_contexts.CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED),    # тут используется функция steps.SimpleApi.create_fake_tpt_row(...)
        'mlu_europe_bv': partner_service(id=firm.MLU_EUROPE_125,  context=partner_contexts.TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT),    # тут используется функция steps.SimpleApi.create_fake_tpt_row(...)
        'mlu_africa_bv': partner_service(id=firm.MLU_AFRICA_126,  context=TAXI_GHANA_CHECK_USD_CONTEXT),
    }

    if service in partner_services:
        context = partner_services[service].context

        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                               additional_params={
                                                                                   'start_dt': date
                                                                               })

        steps.ExportSteps.export_oebs(client_id=client_id, person_id=person_id, contract_id=contract_id)
        create_completions(context, client_id, contract_id, person_id, date)
        steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, date) # or "END_MONTH"

        act_id, external_id = db.get_act_by_contract_id(contract_id)

        act_map = {}
        act_map['id'] = act_id
        act_map['eid'] = external_id
        queries_for_mock_oebs(firm_id=firm_id, act_map=act_map, on_dt=date, person_id=person_id)

        return act_id, external_id



def create_invoice_for_iob(service, date):

    from collections import namedtuple
    from btestlib.data import partner_contexts
    from btestlib.constants import Firms as firm
    from btestlib.constants import Services as const_services

    # TODO закомментировано до выезда в Мастер доделок по этим контекстам(mlu_europe_bv и mlu_africa_bv)
    TAXI_GHANA_CHECK_USD_CONTEXT = partner_contexts.TAXI_GHANA_USD_CONTEXT.new(
        service=const_services.TAXI,
        contract_services=[const_services.TAXI_111.id, const_services.TAXI_128.id, const_services.TAXI.id],
    )

    partner_service = namedtuple('partner_service', 'id context')

    partner_services = {
        'kinopoisk':     partner_service(id=firm.KINOPOISK_9, context=partner_contexts.KINOPOISK_PLUS_CONTEXT),
        'health':        partner_service(id=firm.HEALTH_114, context=partner_contexts.TELEMEDICINE_CONTEXT),
        'uber_ml_bv':    partner_service(id=firm.UBER_115, context=partner_contexts.TAXI_UBER_BV_AZN_USD_CONTEXT),
        'uber_ml_bv_byn':partner_service(id=firm.UBER_1088, context=partner_contexts.TAXI_UBER_BV_BYN_AZN_USD_CONTEXT),
        'israel_go':     partner_service(id=firm.YANDEX_GO_ISRAEL_35, context=partner_contexts.TAXI_ISRAEL_CONTEXT),
        'taxi_am':       partner_service(id=firm.TAXI_AM_26, context=partner_contexts.TAXI_ARM_CONTEXT),
        'taxi_bv':       partner_service(id=firm.TAXI_BV_22, context=partner_contexts.TAXI_BV_GEO_USD_CONTEXT),
        'taxi_kz':       partner_service(id=firm.TAXI_KAZ_24, context=partner_contexts.TAXI_KZ_CONTEXT),
        'drive':         partner_service(id=firm.DRIVE_30, context=partner_contexts.DRIVE_CONTEXT),
        'taxi':          partner_service(id=firm.TAXI_13, context=partner_contexts.TAXI_RU_CONTEXT_CLONE),
        'ya_cloud':      partner_service(id=firm.CLOUD_123, context=partner_contexts.CLOUD_RU_CONTEXT),
        'gas':           partner_service(id=firm.GAS_STATIONS_124, context=partner_contexts.GAS_STATION_RU_CONTEXT),
        'uber_az':       partner_service(id=firm.UBER_AZ_116,  context=partner_contexts.TAXI_AZARBAYCAN_CONTEXT),    # if broken change to CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED
        'uber_kz':       partner_service(id=firm.TAXI_CORP_KZT_31,  context=partner_contexts.CORP_TAXI_KZ_CONTEXT_GENERAL_MIGRATED),    # тут используется функция steps.SimpleApi.create_fake_tpt_row(...)
        'mlu_europe_bv': partner_service(id=firm.MLU_EUROPE_125,  context=partner_contexts.TAXI_MLU_EUROPE_ROMANIA_EUR_CONTEXT),    # тут используется функция steps.SimpleApi.create_fake_tpt_row(...)
        'mlu_africa_bv': partner_service(id=firm.MLU_AFRICA_126,  context=TAXI_GHANA_CHECK_USD_CONTEXT),
    }

    if service in partner_services:
        context = partner_services[service].context

        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context,
                                                                               additional_params={
                                                                                   'start_dt': date
                                                                               })

        steps.ExportSteps.export_oebs(person_id=person_id, client_id=client_id, contract_id=contract_id)

        invoice_id, external_id = steps.InvoiceSteps.get_invoice_ids(client_id)

        return invoice_id, external_id



def create_act(client_id, person_id, paysys_code, service_id, product_id, QTY_BUCKS=0, QTY_MONEY=0, QTY_DAYS=0,
               QTY_SHOWS=0, endbuyer_id=None, contract_id=None, agency_id=None):
    order_id, service_order_id = create_order(client_id, service_id,
                                              product_id,
                                              agency_id=agency_id)
    invoice_id, _ = create_invoice(
        client_id, person_id,
        orders_list=[{'ServiceID': service_id,
                      'ServiceOrderID': service_order_id,
                      'Qty': QTY,
                      'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}],
        paysys_id=paysys_code,
        endbuyer_id=endbuyer_id,
        contract_id=contract_id,
        agency_id=agency_id
    )
    steps.InvoiceSteps.pay(invoice_id, payment_dt=LAST_DAY_OF_PREVIOUS_MONTH)

    # Создаем открутки
    steps.CampaignsSteps.do_campaigns(
        service_id, service_order_id,
        campaigns_params={'Bucks': QTY_BUCKS, 'Money': QTY_MONEY, 'Days': QTY_DAYS, 'Shows': QTY_SHOWS},
        do_stop=0,
        campaigns_dt=LAST_DAY_OF_PREVIOUS_MONTH
    )
    if agency_id is not None:
        client_id = agency_id
    steps.ActsSteps.generate(client_id, force=1,
                             date=LAST_DAY_OF_PREVIOUS_MONTH)
    acts = balance_db.get_acts_by_invoice(invoice_id)
    assert len(acts) == 1
    return acts[0]['id']


def change_external_id(id, object_):
    query = 'select external_id from bo.t_{0} where id= :id'.format(object_.lower())
    query_params = {'id': id}
    eid = balance().execute(query, query_params)[0]['external_id']
    EXTERNAL_ID_SHIFT = 20000000
    if object_.lower() == 'invoice':
        prefix, middle, suffix = eid.split('-')
        middle = str(int(middle) + EXTERNAL_ID_SHIFT)
        eid_2 = '-'.join((prefix, middle, suffix))
    elif object_.lower() == 'contract2':
        prefix, suffix = eid.split('/')
        prefix = str(int(prefix) + EXTERNAL_ID_SHIFT)
        eid_2 = '/'.join((prefix, suffix))
    else:
        eid_2 = str(int(eid) + EXTERNAL_ID_SHIFT)
    balance().execute('update (select  * from bo.t_{0} where id = :id) set external_id= :eid'.format(object_.lower()),
                      {'id': id, 'eid': eid_2})
    return eid


def create_client():
    client_id = steps.ClientSteps.create()
    steps.ClientSteps.link(client_id, CLIENT_LOGIN)
    return client_id


def create_person(client_id, person_category='ur', additional_params=None):
    return steps.PersonSteps.create(client_id, person_category,
                                    params=additional_params, inn_type=person_defaults.InnType.UNIQUE)


def create_order(client_id, service_id, product_id, agency_id=None):
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id,
                                       service_id=service_id,
                                       product_id=product_id,
                                       params={'AgencyID': agency_id})
    return order_id, service_order_id


def create_invoice(client_id, person_id, orders_list, paysys_id=1003, endbuyer_id=None, contract_id=None,
                   agency_id=None, invoice_dt=LAST_DAY_OF_PREVIOUS_MONTH):
    if agency_id is not None:
        client_id = agency_id
    request_id = steps.RequestSteps.create(
        client_id, orders_list,
        additional_params=dict(InvoiceDesireDT=invoice_dt)
    )

    invoice_id, external_id, _ = steps.InvoiceSteps.create(
        request_id, person_id, paysys_id,
        credit=0, contract_id=contract_id, overdraft=0, endbuyer_id=endbuyer_id
    )

    return invoice_id, external_id


def make_serializable(dict_):
    for key, value in dict_.items():
        if isinstance(value, dict):
            dict_[key] = make_serializable(value)
        elif isinstance(value, decimal.Decimal):
            dict_[key] = str(value)
    return dict_


def do_campaign(order_info, on_dt=END_OF_MONTH):
    steps.CampaignsSteps.do_campaigns(
        order_info['service_id'], order_info['service_order_id'],
        order_info['shipment_info'],
        campaigns_dt=on_dt
    )


def create_acted_orders(orders_map, client_id=None, person_id=None, on_dt=END_OF_MONTH, contract_id=None):
    if client_id is None:
        client_id = create_client()
    if person_id is None:
        person_id = create_person(client_id, person_category='ph')
    order_list = orders_map.values()

    for order_info in order_list:
        # Set defaults
        order_info.setdefault('service_id', MEANINGLESS_DEFAULTS['service_id'])
        order_info.setdefault('product_id', MEANINGLESS_DEFAULTS['product_id'])
        order_info.setdefault('consume_qty', MEANINGLESS_DEFAULTS['qty'])
        order_info.setdefault('invoice_ids', [])
        order_info.setdefault('shipment_info', {})
        for shipment_type in ('Bucks', 'Money', 'Days', 'Shows'):
            order_info['shipment_info'].setdefault(shipment_type, 0)

        order_info['id'], order_info['service_order_id'] = \
            create_order(client_id, order_info['service_id'],
                         order_info['product_id'])
        order_info['client_id'] = client_id
        order_info['person_id'] = person_id

    # Создаем открутки в биллинге
    campaigns_info = []
    for order_info in order_list:
        campaign_info = order_info['shipment_info'].copy()
        campaign_info.update({
            'ServiceID': order_info['service_id'],
            'ServiceOrderID': order_info['service_order_id'],
            'dt': on_dt, 'stop': 0
        })
        campaigns_info.append(campaign_info)

    balance_api.medium().UpdateCampaigns(campaigns_info)

    # Если ничего зачислять не нужно, то мы закончили
    if sum(order_info['consume_qty'] for order_info in order_list) == 0:
        return orders_map

    invoice_id, _ = create_invoice(
        client_id, person_id,
        orders_list=[
            {'ServiceID': order_info['service_id'],
             'ServiceOrderID': order_info['service_order_id'],
             'Qty': order_info['consume_qty'], 'BeginDT': on_dt}
            for order_info in order_list
            if order_info['consume_qty'] > 0
        ],
        paysys_id=orders_map.values()[0].setdefault('paysys_id', 1001),
        contract_id=contract_id,
        invoice_dt=on_dt,
    )
    steps.InvoiceSteps.pay(invoice_id, payment_dt=on_dt)

    for order_info in order_list:
        order_info['invoice_ids'].append(invoice_id)
        # Раскладываем открутки на конзюмы
        do_campaign(order_info, on_dt=on_dt)

    act_ids = steps.ActsSteps.create(invoice_id, act_dt=on_dt)
    if act_ids != -1:
        for order_info in order_list:
            order_info['act_ids'] = act_ids

    return orders_map


def get_diff_orders(cmp_id, check_code_name):
    query = """
        select order_id
        from cmp.{0}_cmp_data
        where cmp_id = {1}
    """.format(check_code_name, cmp_id)
    return {
        row['order_id']
        for row in balance_api.test_balance().ExecuteSQL('cmp', query)
    }


def check_order(order_id, diff_orders, expected_state):
    if order_id in diff_orders:
        state = 1
    else:
        state = 0

    print(order_id)
    print('state = ' + str(state) + ';   expected - ' + str(expected_state))
    utils.check_that(state, equal_to(expected_state),
                     u'Проверяем, что ожидаемый результат соответствует действительному')


def create_order_map(orders_map, client_id=None):
    order_list = orders_map.values()

    for order_info in order_list:
        # Set defaults
        order_info.setdefault('service_id', MEANINGLESS_DEFAULTS['service_id'])
        order_info.setdefault('product_id', MEANINGLESS_DEFAULTS['product_id'])
        order_info.setdefault('consume_qty', MEANINGLESS_DEFAULTS['qty'])
        order_info.setdefault('shipment_info', {})
        order_info.setdefault('invoice_ids', [])
        order_info['client_id'] = client_id if client_id is not None else create_client()
        for shipment_type in ('Bucks', 'Money', 'Days', 'Shows'):
            order_info['shipment_info'].setdefault(shipment_type, 0)

        order_info['id'], order_info['service_order_id'] = \
            create_order(order_info['client_id'], order_info['service_id'],
                         order_info['product_id'])

    return orders_map


def create_invoice_map(orders_map, client_id=None, person_id=None, on_dt=END_OF_MONTH):
    orders_map = create_order_map(orders_map, client_id)
    invoice_map = {}
    order_list = orders_map.values()
    if person_id is None:
        person_id = create_person(order_list[0]['client_id'], person_category='ph')

    # Если ничего зачислять не нужно, то мы закончили
    if sum(order_info['consume_qty'] for order_info in order_list) == 0:
        return order_list

    invoice_id, inv_external_id = create_invoice(
        order_list[0]['client_id'], person_id,
        orders_list=[
            {'ServiceID': order_info['service_id'],
             'ServiceOrderID': order_info['service_order_id'],
             'Qty': order_info['consume_qty'], 'BeginDT': on_dt}
            for order_info in order_list
            if order_info['consume_qty'] > 0
        ],
        paysys_id=orders_map.values()[0].setdefault('paysys_id', 1001)
    )
    for order_info in order_list:
        order_info['invoice_ids'].append(invoice_id)

    update_date_invoice(invoice_id, on_dt)

    invoice_map['id'] = invoice_id
    invoice_map['eid'] = inv_external_id
    invoice_map['person_id'] = person_id
    invoice_map['total_sum'] = sum(order_info['consume_qty'] for order_info in order_list)
    invoice_map['orders'] = orders_map

    return invoice_map


def create_act_map(orders_map, client_id=None, person_id=None, on_dt=END_OF_MONTH, firm_id=None, act_needed=False):
    invoice_map = create_invoice_map(orders_map, client_id, person_id, on_dt)
    steps.InvoiceSteps.pay(invoice_map['id'], payment_dt=on_dt)
    act_map = {}

    order_list = invoice_map['orders'].values()

    # Создаем открутки в биллинге
    campaigns_info = []
    for order_info in order_list:
        campaign_info = order_info['shipment_info'].copy()
        campaign_info.update({
            'ServiceID': order_info['service_id'],
            'ServiceOrderID': order_info['service_order_id'],
            'dt': on_dt, 'stop': 0
        })
        campaigns_info.append(campaign_info)

    balance_api.medium().UpdateCampaigns(campaigns_info)

    for order_info in order_list:
        # Раскладываем открутки на конзюмы
        do_campaign(order_info, on_dt=on_dt)

    if act_needed:
        acts = steps.ActsSteps.generate(client_id, force=1, date=on_dt)
        assert len(acts) == 1
        act_map['id'] = acts[0]
        act_map['eid'] = balance().execute(
            'select external_id from bo.T_ACT where id= :act_id',
            {'act_id': act_map['id']})[0]['external_id']
        act_map['invoice'] = invoice_map


    # -------------------------------------------------------------
    if firm_id:
        queries_for_mock_oebs(firm_id=firm_id, act_map=act_map, on_dt=on_dt, person_id=person_id)

    return order_list if not act_needed else act_map
    # return act_map


def create_invoice_for_check_endbuyer(service_id, product_id, paysys_id, person_additional_params, firm):
    agency_id = steps.ClientSteps.create_agency()
    client_id = steps.ClientSteps.create()
    steps.ExportSteps.export_oebs(client_id=client_id)
    steps.ExportSteps.export_oebs(client_id=agency_id)
    finish_dt = (datetime.datetime.now() + datetime.timedelta(weeks=5)).strftime('%Y-%m-%dT%H:%M:%S')
    person_id = create_person(agency_id, person_category='ur', additional_params=person_additional_params)
    contract_id, _ = steps.ContractSteps.create_contract('comm_post', {'CLIENT_ID': agency_id, 'PERSON_ID': person_id,
                                                                       'DT': '2015-04-30T00:00:00',
                                                                       'FINISH_DT': finish_dt,
                                                                       'IS_SIGNED': '2015-01-01T00:00:00',
                                                                       'SERVICES': [11],
                                                                       'COMMISSION_TYPE': 48,
                                                                       'NON_RESIDENT_CLIENTS': 0,
                                                                       'FIRM': firm
                                                                       })
    steps.ExportSteps.export_oebs(contract_id=contract_id)
    endbuyer_id = create_person(
        agency_id, person_category='endbuyer_ph', additional_params={'email': 'chihiro-test-0@yandex.ru'}
    )
    order_id, service_order_id = create_order(client_id, service_id,
                                              product_id,
                                              agency_id=agency_id)
    invoice_id, _ = create_invoice(
        agency_id, person_id,
        orders_list=[{'ServiceID': service_id,
                      'ServiceOrderID': service_order_id,
                      'Qty': 55,
                      'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}],
        paysys_id=paysys_id,
        endbuyer_id=endbuyer_id,
        contract_id=contract_id,
        agency_id=agency_id
    )
    return invoice_id, service_order_id, agency_id


def create_act_for_check_endbuyer(service_id, product_id, paysys_id, person_additional_params):
    invoice_id, service_order_id, agency_id = create_invoice_for_check_endbuyer(service_id, product_id, paysys_id,
                                                                                person_additional_params)

    steps.InvoiceSteps.pay(invoice_id, payment_dt=LAST_DAY_OF_PREVIOUS_MONTH)

    # Создаем открутки
    steps.CampaignsSteps.do_campaigns(
        service_id, service_order_id,
        campaigns_params={'Bucks': 23},
        do_stop=0,
        campaigns_dt=LAST_DAY_OF_PREVIOUS_MONTH
    )

    acts = steps.ActsSteps.generate(agency_id, force=1,
                                    date=LAST_DAY_OF_PREVIOUS_MONTH)
    assert len(acts) == 1
    act_id = acts[0]
    steps.ExportSteps.export_oebs(invoice_id=invoice_id)
    steps.ExportSteps.export_oebs(act_id=act_id)
    return invoice_id, act_id


def create_vertical_invoice(client_id, agency_id, person_id):
    contract_type = 'vertical_comm_post'
    order_id, service_order_id = create_order(client_id, Services.vertical, Products.vertical, agency_id)
    contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                         {'PERSON_ID': person_id, 'CLIENT_ID': agency_id,
                                                          'SERVICES': [Services.vertical],
                                                          'FINISH_DT': datetime.datetime.now() + datetime.timedelta(
                                                              weeks=5)})
    invoice_id, inv_external_id = create_invoice(client_id, person_id, [{'ServiceID': Services.vertical,
                                                                         'ServiceOrderID': service_order_id,
                                                                         'Qty': '55',
                                                                         'BeginDT': LAST_DAY_OF_PREVIOUS_MONTH}],
                                                 paysys_id=1201003, endbuyer_id=None, contract_id=contract_id,
                                                 agency_id=agency_id)
    steps.ExportSteps.export_oebs(person_id=person_id, contract_id=contract_id)

    return service_order_id, invoice_id, inv_external_id


def create_vertical_act(client_id, agency_id, person_id, firm_id=None):
    service_order_id, invoice_id, inv_external_id = create_vertical_invoice(client_id, agency_id, person_id)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(Services.vertical, service_order_id, {'Bucks': 10, 'Days': 0, 'Money': 0}, 0,
                                      LAST_DAY_OF_PREVIOUS_MONTH)
    act_id = steps.ActsSteps.create(invoice_id, LAST_DAY_OF_PREVIOUS_MONTH)[0]
    act_eid = balance().execute(
                       'select external_id from bo.T_ACT where id= :act_id',
                       {'act_id': act_id})[0]['external_id']

    act_map={}
    act_map['id'] = act_id
    act_map['eid'] = act_eid

    queries_for_mock_oebs(firm_id=firm_id, act_map=act_map, on_dt=LAST_DAY_OF_PREVIOUS_MONTH, person_id=person_id)

    return act_id, act_eid


def generate_act_and_get_data(contract_id, client_id, generation_dt):
    # запускаем генерацию актов
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, generation_dt)

    # генерим счет на погашение и акт
    steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

    # берем данные по заказам и сортируем список по id продукта
    order_data = steps.OrderSteps.get_order_data_by_client(client_id)
    order_data.sort(key=lambda k: k['service_code'])

    # берем данные по счетам и сортируем список по типу счета
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client_and_dt(client_id, generation_dt)
    invoice_data.sort(key=lambda k: k['type'])

    # берем данные по актам
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    return order_data, invoice_data, act_data


def create_contract(person_type, start_dt=None, is_vip_needed=0, product_id=None):
    # создаем клиента
    client_id = steps.ClientSteps.create()
    # создаем плательщика
    person_id = steps.PersonSteps.create(client_id, person_type, {'kpp': '234567890'})
    # создаем договор на ADFox
    params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'DT': start_dt or first_month_start_dt}
    if person_type <> PERSON_TYPE_UR:
        params.update({'DEAL_PASSPORT': '2016-01-01T00:00:00'})
    if is_vip_needed:
        params.update({'VIP_CLIENT': 1, 'DISCOUNT_PRODUCT_ID': product_id})
    contract_id, _ = steps.ContractSteps.create_contract('adfox_all_products', params)
    return client_id, person_id, contract_id


def create_distribution_addapter_completions(places_ids, act_date, completion_type=1, bucks_rs=None, vid=None):
    """
    Временная функция, до появления на тесте BALANCE-31493.
    Копирует balance.balance_steps.distribution_steps.DistributionSteps#create_places.
    Разница только в том, что мы сами составляем мапим поля.
    """

    from balance.balance_steps.distribution_steps import \
        DistributionSubtype, Decimal, defaults

    def get_first(dct, fields):
        for f in fields:
            if f in dct:
                return dct[f]
        return None

    import json

    metadata = [{
        'page_id': page_id,
        'product_metadata': json.dumps({
            # 23, 24, 25, 26
            'sources': [23 + page_id - 4009],
            'fields': {
                'tag_id': 'key_num_1',

                'bucks': 'val_num_1',
                'hits': 'val_num_2',
            }
        })
    } for page_id in xrange(4009, 4012 + 1)]

    field_mapping = {
        row['page_id']: json.loads(row['product_metadata'])['fields']
        for row in metadata
    }
    product_to_src_map = {
        row['page_id']: json.loads(row['product_metadata'])['sources']
        for row in metadata
    }

    with reporter.step(u"Создаем открутки в новой схеме для всех возможных типов дистрибуции"):
        for distribution_type, place_id in places_ids.iteritems():
            product_id = distribution_type.result_page_id
            mapping = field_mapping[product_id]
            key = {}

            if 'place_id' in mapping:
                key['key_num_1'] = place_id
            else:
                import balance.balance_db as db
                search_id = db.balance().execute('select search_id from t_place where id = :place_id',
                                                 dict(place_id=place_id)
                                                 )[0]['search_id']
                key['key_num_1'] = search_id

            if 'completion_type' in mapping:
                column = mapping['completion_type']
                key[column] = completion_type

            if 'vid' in mapping:
                column = mapping['vid']
                key[column] = vid if vid is not None else -1

            eid = tarification_entity_steps.get_tarification_entity_id(product_id, **key)

            values = {('val_num_{}'.format(num)): 0 for num in range(1, 4+1)}
            shows_column = get_first(mapping,
                                     ['shows', 'hits', 'activations', 'count', 'orders', 'install_new', 'quantity'])
            if shows_column:
                shows_value = (distribution_type.default_amount
                               if distribution_type.subtype in (DistributionSubtype.FIXED, DistributionSubtype.ADDAPTER)
                               else defaults.Distribution.DEFAULT_REVSHARE_SHOWS
                               )
                values[shows_column] = shows_value
            clicks_column = mapping.get('clicks')  # Для clicks альтернативных названий пока нет
            if clicks_column:
                clicks_values = (0 if distribution_type.subtype == DistributionSubtype.FIXED
                                 else defaults.Distribution.DEFAULT_REVSHARE_CLICKS
                                 )
                values[clicks_column] = clicks_values
            if distribution_type.subtype == DistributionSubtype.ADDAPTER:
                bucks_column = get_first(mapping,
                                         ['bucks', 'commission_value', 'dsp_charge', 'money', 'commission'])
                values[bucks_column] = (Decimal(distribution_type.default_amount) /
                                        Decimal(distribution_type.units_type_rate) *
                                        distribution_type.default_price)
                if bucks_rs is not None:
                    values[bucks_column] = 0


            if bucks_rs is not None:
                bucks_rs_column = mapping.get('bucks_rs')
                if bucks_rs_column:
                    values[bucks_rs_column] = (Decimal(bucks_rs) / Decimal(distribution_type.units_type_rate))

            src_id = product_to_src_map[product_id][0]

            db.balance().execute('''
                insert into bo.t_entity_completion
                (dt, product_id, entity_id, src_id, val_num_1, val_num_2, val_num_3, val_num_4)
                values (:dt, :pid, :eid, :src_id, :val_num_1, :val_num_2, :val_num_3, :val_num_4) 
            ''',
            dict(dt=act_date, pid=product_id, eid=eid, src_id=src_id, **values))


def create_distribution_completions(distribution_type, contract_dt=CONTRACT_START_DT, completion_dt=COMPLETION_DT):
    firm = Firms.YANDEX_1
    currency = product_currency = Currencies.RUB
    contract_type = DistributionContractType.OFFER

    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    # создаем договор дистрибуции
    steps.DistributionSteps.create_full_contract(
        contract_type, client_id, person_id, tag_id, contract_dt, contract_dt, firm=firm,
        contract_currency=currency, product_currency=product_currency
    )
    places_ids, clids = steps.DistributionSteps.create_places(client_id, tag_id, [distribution_type, ])

    # TODO: remove after BALANCE-31493
    from balance.distribution import distribution_types
    if distribution_type.subtype is distribution_types.DistributionSubtype.ADDAPTER:
        create_distribution_addapter_completions(places_ids, completion_dt)
    else:
        # добавляем открутки
        steps.DistributionSteps.create_entity_completions(places_ids, completion_dt)

    place_id = places_ids.get(distribution_type)
    clid_id = clids.get(distribution_type)

    if distribution_type == steps.DistributionType.DOWNLOADS:
        return place_id
    else:
        return clid_id


def update_partner_transaction_log_dt(transaction_log_dt=None):
    """
    Сдвигает дату перехода на транзакционный лог.
    Открутки, субсидии и т.д. до этой даты будут забираться
      сверкой по старой схеме.
    """
    query = """
        update t_config 
        set value_dt = :value_dt 
        where item = 'partner_transaction_log_dt' 
    """.strip()
    params = {'value_dt': transaction_log_dt or TRANSACTION_LOG_DT}
    balance_api.test_balance().ExecuteSQL('cmp', query, params)
