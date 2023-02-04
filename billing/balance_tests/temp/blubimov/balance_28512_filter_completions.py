# coding: utf-8

import pickle
from collections import Counter
from datetime import date

from enum import Enum

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib import utils

DT = date(2018, 7, 1)
# DT = date(2018, 10, 1)
# DT = date(2018, 11, 1)


# все источники, забор откруток у которых отрабатывает на тесте
# cluster_tools/partner_completions.py
class PartnerCompletionSources(Enum):
    blue_market = ('blue_market', 't_partner_product_completion', 'service_id = 612', date(2018, 10, 24))
    avia_product_completions = ('avia_product_completions',
                                't_partner_product_completion', 'service_id = 114 and source_id = 2', date(2018, 10, 1))

    # t_partner_payment_stat - это алиас для t_partner_zen_stat
    toloka = ('toloka', 't_partner_payment_stat', 'service_id = 42')
    boyscouts = ('boyscouts', 't_partner_payment_stat', 'service_id = 619', date(2018, 7, 30))
    zen = ('zen', 't_partner_payment_stat', 'service_id = 134')
    taxi_stand_svo = ('taxi_stand_svo', 't_partner_payment_stat', 'service_id = 626', date(2018, 7, 2))

    adfox = ('adfox', 't_partner_adfox_stat')
    buses2 = ('buses2', 't_partner_buses_stat', '1=1', date(2018, 7, 31))
    # dsp = ('dsp', 't_partner_dsp_stat') # пока не смотрим т.к. запросы к бд таймаутятся
    taxi_aggr = ('taxi_aggr', 't_partner_taxi_stat_aggr', '1=1', DT,
                 {'commission_sum': 'commission_value',
                  'promocode_sum': 'coupon_value',
                  'payment_type': 'payment_method'})

    # дистрибуция: поиски и загрузки/установки/клики/активации
    # d_installs = ('d_installs', 't_partner_completion_buffer', 'source_id = 4', DT,  # таймаутится
    #               {'dt': 'fielddate', 'shows': 'install_new'})
    activations = ('activations', 't_partner_completion_buffer', 'source_id = 8', DT,
                   {'shows': 'activations', 'place_id': 'clid'})

    # дистрибуция: разделение доходов
    # tags3 = ('tags3', 't_partner_tags_stat3', 'source_id = 11') # пока не смотрим т.к. запросы к бд таймаутятся
    taxi_medium = ('taxi_medium', 't_partner_tags_stat3', 'source_id = 12', DT,
                   {'commission_sum': 'commission_value', 'shows': 'count'})
    # rs_market_cpa = ('rs_market_cpa', 't_partner_tags_stat3', 'source_id = 14 and page_id = 10004') # export таймаутится
    avia_rs = ('avia_rs', 't_partner_tags_stat3', 'source_id = 15', date(2018, 10, 1))
    rtb_distr = ('rtb_distr', 't_partner_tags_stat3', 'source_id = 16')
    taxi_distr = ('taxi_distr', 't_partner_tags_stat3', 'source_id = 17')
    video_distr = ('video_distr', 't_partner_tags_stat3', 'source_id = 18')

    # данные только добавляются, но не удаляются/обновляются
    distr_pages = ('distr_pages', 't_distribution_pages')

    def __init__(self, source_name, table_name, where_conditions='1=1', dt=DT, mapper=None):
        self.source_name = source_name
        self.table_name = table_name
        self.where_conditions = where_conditions
        self.dt = dt
        self.mapper = mapper  # {db_field_name: parsed_field_name}

    def get_db_rows_number(self, on_date=True, extra_conditions=''):
        query = 'select count(*) as qty from {table_name} where {where_conditions}'.format(
            table_name=self.table_name,
            where_conditions=self.where_conditions
        )
        # в таблице t_distribution_pages нет столбца dt
        if on_date and self.table_name.lower() != 't_distribution_pages':
            query += " and trunc(dt) = date'{dt}'".format(dt=self.dt.isoformat())

        if extra_conditions:
            query += " and {}".format(extra_conditions)

        rows_qty = db.balance().execute(query, single_row=True)['qty']
        return rows_qty

    def get_db_rows_number_wo_source_filter(self):
        query = 'select count(*) as qty from {table_name}'.format(table_name=self.table_name)
        rows_qty = db.balance().execute(query, single_row=True)['qty']
        return rows_qty

    def delete_db_rows_on_date(self):
        if self.table_name.lower() == 't_distribution_pages':
            return
        query = "delete {table_name} where {where_conditions} and trunc(dt) = date'{dt}'".format(
            table_name=self.table_name, where_conditions=self.where_conditions, dt=self.dt.isoformat()
        )
        print 'MANUAL DELETE ROWS'
        db.balance().execute(query)

    def replace_db_fields_names(self, input_filter):
        if self.mapper:
            filter_dict = input_filter['completion_filter']
            for k in filter_dict:
                if k in self.mapper:
                    filter_dict[self.mapper[k]] = filter_dict.pop(k)
        return input_filter

    @staticmethod
    def get_by_source(source_name):
        for s in PartnerCompletionSources:
            if s.source_name == source_name:
                return s
        print "{}: DB table not defined".format(source_name)


def test_check_sources_db_rows_number():
    # pcs_list = PartnerCompletionSources
    source_list = [
        'activations',
        'adfox',
        'avia_chain',
        'avia_product_completions',
        'avia_rs',
        'blue_market',
        'boyscouts',
        'buses2',
        'd_installs',  # долго работает, может таймаутить
        'distr_pages',
        'dsp',
        'rtb_distr',
        'tags3',
        'taxi_aggr',
        'taxi_distr',
        'taxi_medium',
        'toloka',
        'video_distr',
        'zen'
    ]
    pcs_list = [PartnerCompletionSources.get_by_source(source) for source in source_list]
    pcs_list = [pcs for pcs in pcs_list if pcs is not None]

    print get_db_rows_number(pcs_list)
    # pcs.delete_db_rows_on_date()
    # print  get_db_rows_number(pcs_list)


def get_db_rows_number(pcs_list, extra_condition=None, prev_res=None):
    out = ''
    res = {}

    def print_cant_read_sources(cant_read_sources, out):
        if cant_read_sources:
            out += "\nCan't read data for sources: {}".format(utils.Presenter.pretty(cant_read_sources))

    if extra_condition:
        cant_read_sources_on_date_filtered = []
        rows_on_date_filtered = {}
        for source in pcs_list:
            try:
                rows_on_date_filtered.update(
                    {source.source_name: source.get_db_rows_number(on_date=True, extra_conditions=extra_condition)})
            except:
                cant_read_sources_on_date_filtered.append(source.source_name)
        res['rows_on_date_filtered'] = rows_on_date_filtered

        cant_read_sources_all_rows_filtered = []
        rows_all_filtered = {}
        for source in pcs_list:
            try:
                rows_all_filtered.update(
                    {source.source_name: source.get_db_rows_number(on_date=False, extra_conditions=extra_condition)})

            except:
                cant_read_sources_all_rows_filtered.append(source.source_name)
        res['rows_all_filtered'] = rows_all_filtered

    cant_read_sources_on_date = []
    rows_on_date = {}
    for source in pcs_list:
        try:
            rows_on_date.update({source.source_name: source.get_db_rows_number(on_date=True)})
        except:
            cant_read_sources_on_date.append(source.source_name)
    res['rows_on_date'] = rows_on_date

    cant_read_sources_all_rows = []
    rows_all = {}
    for source in pcs_list:
        try:
            rows_all.update({source.source_name: source.get_db_rows_number(on_date=False)})
        except:
            cant_read_sources_all_rows.append(source.source_name)
    res['rows_all'] = rows_all

    cant_read_sources_all_rows_for_table = []
    rows_all_for_table = {}
    for source in pcs_list:
        try:
            rows_all_for_table.update({source.source_name: source.get_db_rows_number_wo_source_filter()})
        except:
            cant_read_sources_all_rows_for_table.append(source.source_name)
    res['rows_all_for_table'] = rows_all_for_table

    add_diffs(res, prev_res)

    if extra_condition:
        out += 'Rows on date (with date filter and extra filter):\n'
        out += utils.Presenter.pretty(rows_on_date_filtered)
        print_cant_read_sources(cant_read_sources_on_date_filtered, out)

        out += '\nAll rows (without date filter but with extra filter):\n'
        out += utils.Presenter.pretty(rows_all_filtered)
        print_cant_read_sources(cant_read_sources_all_rows_filtered, out)

    out += '\nRows on date (with date filter):\n'
    out += utils.Presenter.pretty(rows_on_date)
    print_cant_read_sources(cant_read_sources_on_date, out)

    out += '\nAll rows for source (without date filter):\n'
    out += utils.Presenter.pretty(rows_all)
    print_cant_read_sources(cant_read_sources_all_rows, out)

    out += '\nAll rows for table:\n'
    out += utils.Presenter.pretty(rows_all_for_table)
    print_cant_read_sources(cant_read_sources_all_rows_for_table, out)

    return out, res


def add_diffs(res, prev_res):
    if prev_res is None:
        return
    for k in prev_res:
        if k in res:
            for source in prev_res[k]:
                if source in res[k]:
                    diff_key = source + '_diff'
                    res[k].update({diff_key: res[k][source] - prev_res[k][source]})


def get_all_sources():
    # raw_result = db.balance().execute("SELECT DISTINCT SOURCE_NAME FROM T_PARTNER_COMPLETIONS_RESOURCE")
    # result = [row['source_name'] for row in raw_result]
    #
    # # старые источники, которые уже не используются
    # old_sources = ['avia_order_completions', 'avia_order_completions_new', 'avia_order_shipment', 'taxi']
    #
    # # на тесте не отрабатывают
    # not_working_sources = [
    #     # Connection timed out
    #     'addapter_dev_com',
    #     'addapter_dev_ds',
    #     'addapter_ret_com',
    #     'addapter_ret_ds',
    #     'advisor_market',
    #     'api_market',
    #     'bk',
    #     'health',
    #     'multiship',
    #     'rs_market',
    #     'rs_market_cpa',
    #     'serphits',
    #     # Unable to read table
    #     'connect',
    #     'cloud',
    # ]
    #
    # result = set(result) - set(old_sources) - set(not_working_sources)
    # return sorted(result)

    # источники успешно отрабатывающие на тесте
    working_sources = [
        # 'activations',
        # 'adfox',
        'avia_chain',
        # 'avia_product_completions',
        # 'avia_rs',
        # 'blue_market',
        # 'boyscouts',
        # 'buses2',
        'd_installs',  # долго работает, может таймаутить
        'distr_pages',
        'dsp',
        'rtb_distr',
        'tags3',
        'taxi_aggr',
        'taxi_distr',
        'taxi_medium',
        'toloka',
        'video_distr',
        'zen'
    ]

    return working_sources


def get_all_sources_from_pcs():
    return [pcs.source_name for pcs in PartnerCompletionSources]


def test_all_sources():
    print utils.Presenter.pretty(get_all_sources())


# регрессионная проверка забора откруток
def test_completions():
    # sources_list = get_all_sources()
    # pcs_list = [pcs for pcs in PartnerCompletionSources]
    pcs_list = [PartnerCompletionSources.taxi_stand_svo]
    # sources_list = ['toloka', 'zen']

    ok_sources = []
    fail_sources = []

    for pcs in pcs_list:

        # if pcs:
        #     pcs.delete_db_rows_on_date()

        db.balance().execute(
            "UPDATE T_PARTNER_COMPLETIONS_RESOURCE SET DT=date'{dt}', SOURCE_NAME=:source_name WHERE ID=1".format(
                dt=pcs.dt.isoformat()),
            {'source_name': pcs.source_name})
        try:
            steps.CommonSteps.export('PARTNER_COMPL', 'PartnerCompletionsResource', 1)
            ok_sources.append(pcs.source_name)
        except:
            fail_sources.append(pcs.source_name)

    print "\nok_sources:\n {}".format(utils.Presenter.pretty(ok_sources))
    print "\nfail_sources:\n {}".format(utils.Presenter.pretty(fail_sources))

    # pcs_list = []
    # for source_name in sources_list:
    #     pcs = PartnerCompletionSources.get_by_source(source_name)
    #     if pcs:
    #         pcs_list.append(pcs)

    print get_db_rows_number(pcs_list)


def test_completions_with_filters():
    out = ''
    # pcs = PartnerCompletionSources.activations
    # pcs = PartnerCompletionSources.distr_pages
    # pcs = PartnerCompletionSources.avia_rs
    # pcs = PartnerCompletionSources.d_installs
    # pcs = PartnerCompletionSources.toloka
    # pcs = PartnerCompletionSources.boyscouts
    # pcs = PartnerCompletionSources.blue_market
    # pcs = PartnerCompletionSources.adfox
    # pcs = PartnerCompletionSources.buses2
    # pcs = PartnerCompletionSources.taxi_aggr
    # pcs = PartnerCompletionSources.taxi_medium
    # pcs = PartnerCompletionSources.taxi_distr
    # pcs = PartnerCompletionSources.rtb_distr
    # pcs = PartnerCompletionSources.video_distr
    # pcs = PartnerCompletionSources.zen
    # pcs = PartnerCompletionSources.taxi_stand_svo
    pcs = PartnerCompletionSources.avia_product_completions

    source_name = pcs.source_name

    input_filter, db_filter = None, None
    # input_filter, db_filter = get_filters_toloka(pcs)
    # input_filter, db_filter = get_filters_boyscouts(pcs)
    # input_filter, db_filter = get_filters_blue_market(pcs)
    # input_filter, db_filter = get_filters_adfox(pcs)
    # input_filter, db_filter = get_filters_buses(pcs)
    # input_filter, db_filter = get_filters_taxi_aggr(pcs)
    # input_filter, db_filter = get_filters_taxi_medium(pcs)
    # input_filter, db_filter = get_filters_taxi_distr(pcs)
    # input_filter, db_filter = get_filters_video_distr(pcs)
    # input_filter, db_filter = get_filters_distr_pages(pcs)
    # input_filter, db_filter = get_filters_avia_product_completions(pcs)

    # ручное удаление
    # включить при необходимости проверки что данные вообще загружаются и не загружается лишних
    # pcs.delete_db_rows_on_date();out += 'manual delete rows'

    db_rows_before, prev_res = get_db_rows_number([pcs], db_filter)

    if input_filter is None:
        # при вызове ExportObject без input - используется input из t_export, поэтому нужно очищать input вручную
        db.balance().execute("UPDATE T_EXPORT SET INPUT=null WHERE OBJECT_ID=1 AND TYPE='PARTNER_COMPL'")

    ok_sources = []
    fail_sources = []
    db_rows_after = None

    db.balance().execute(
        # "UPDATE T_PARTNER_COMPLETIONS_RESOURCE SET DT=DATE'2018-07-01', SOURCE_NAME=:source_name WHERE ID=1",
        "UPDATE T_PARTNER_COMPLETIONS_RESOURCE SET DT=date'{dt}', SOURCE_NAME=:source_name WHERE ID=1".format(
            dt=pcs.dt.isoformat()),
        {'source_name': source_name})
    try:
        steps.CommonSteps.export('PARTNER_COMPL', 'PartnerCompletionsResource', 1, input_=input_filter)
        ok_sources.append(source_name)

        db_rows_after, _ = get_db_rows_number([pcs], db_filter, prev_res)
    except:
        fail_sources.append(source_name)

    print '------------------------------------------------------------'
    print 'source_name: {} ({})'.format(source_name, pcs.dt)
    print input_filter
    if out:
        print out
    print
    print db_rows_before
    print '---'
    if ok_sources:
        print "ok sources: {}".format(utils.Presenter.pretty(ok_sources))
    if fail_sources:
        print "FAIL sources: {}".format(utils.Presenter.pretty(fail_sources))
    print '---'
    if db_rows_after:
        print db_rows_after
    print


def get_filters_toloka(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        # 'client_id': 44609507,
        # 'currency': 'RUB',
        # 'transaction_type': 'payment',
        # 'payment_type': 'wallet',
        # 'payment_type': 'email'
        # 'service_id': 42,
    }})

    # db_filter = "client_id = 44609507" # 1
    # db_filter = "CURRENCY = 'RUB'" # 22
    # db_filter = "TRANSACTION_TYPE = 'payment'" # 22
    # db_filter = "PAYMENT_TYPE = 'wallet'" # 22
    # db_filter = "SERVICE_ID = 42" # 22
    db_filter = "PAYMENT_TYPE = 'email'"  # 01-09-2018  107

    return input_filter, db_filter


def get_filters_boyscouts(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        'client_id': 42706149,
        'transaction_type': 'payment',
        # 'transaction_type': 'refund',
        'currency': 'RUB',
        'service_id': '619',
        # 'client_id': [42661693, 42702649]
    }})

    # db_filter = "transaction_type = 'refund'"  # 0
    # db_filter = "client_id = 42706149"  # - - 271
    # db_filter = "client_id = 42706149 and transaction_type = 'payment'"  # - - 271
    # db_filter = "client_id = 42706149 and transaction_type = 'payment' and currency = 'RUB'"  # - -271
    db_filter = "client_id = 42706149 and transaction_type = 'payment' and currency = 'RUB' and service_id = 619"  # - - 271
    # db_filter = "client_id in (42661693, 42702649)"  # - - 628

    return input_filter, db_filter


def get_filters_blue_market(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        'product_id': 508942,
        # 'client_id': 103848522,
        # 'amount': '231.03',
        'contract_id': None,
    }})

    # db_filter = "product_id = 508942"  # - - 4
    # db_filter = "product_id = 508942 and client_id = 103848522" # - - 1
    # db_filter = "amount = 231.03" # - - 1
    db_filter = "product_id = 508942 and contract_id is null"  # - - 4

    return input_filter, db_filter


def get_filters_adfox(pcs):
    import datetime
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        # 'contract_id': 271423,
        # 'product_id': 505170,
        # 'bill': 0,
        # 'contract_id': [270793, 270794, 272370, 273470], 'product_id': [505170], 'bill': [0,1],
        'price_dt': datetime.datetime(2018, 5, 21),
    }})

    # db_filter = "CONTRACT_ID = 271423"  # - - 1
    # db_filter = "product_id = 505170"  # - - 407
    # db_filter = "bill = 0"  # - - 161
    # db_filter = "product_id = 505170 and contract_id in (270793, 270794, 272370, 273470) and bill in (0, 1)"  # - - 4
    db_filter = "price_dt = date'2018-05-21'"  # - - 1

    return input_filter, db_filter


def get_filters_buses(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        'client_id': 37038312,
    }})

    db_filter = "client_id = 37038312"  # - - 4

    return input_filter, db_filter


# 28777
def get_filters_taxi_aggr(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        'client_id': {44442030, 44442035, 44442030},
        # 'commission_currency': ['RUB', 'USD'],
        # 'commission_currency': '',
        # 'payment_type': ['corporate', 'cash'],
        # 'type': ['hiring_with_car', 'childchair'],
    }})
    input_filter = {'completion_filter': {'client_id': '{44442030, 44442035, 44442030}'}}

    db_filter = "client_id in (44442030, 44442035)"  # - - 4
    # db_filter = "commission_currency in ('RUB', 'USD')"  # - - 28654
    # db_filter = "commission_currency in ('RUB', 'USD') and payment_type in ('corporate', 'cash')"  # - - 17321
    # db_filter = "commission_currency in ('RUB', 'USD') and payment_type in ('corporate', 'cash') and type in ('hiring_with_car', 'childchair')"  # - - 367
    # db_filter = "commission_currency = ''"  # - - 0

    return input_filter, db_filter


def get_filters_taxi_medium(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        # 'clid': 2104423, # это tag_id
        'shows': 1,
        # 'shows': 10,
    }})

    # db_filter = "tag_id = 2104423"  # 1
    db_filter = "shows = 1"  #
    # db_filter = "shows = 10"  # - - 0

    return input_filter, db_filter


def get_filters_taxi_distr(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        'clid': 2321900,  # это tag_id
        # 'place_id': 13001,
        # 'place_id': [13001, 13002],
        # 'page_id': 13002,
    }})

    # db_filter = "place_id = 13001"# - - 2
    # db_filter = "place_id in (13001, 13002)"  # - - 4
    # db_filter = "page_id = 13002"# - - 2
    # db_filter = "page_id = 13002 and place_id = 13001"# - - 0
    db_filter = "tag_id = 2321900"  # - -2

    return input_filter, db_filter


def get_filters_video_distr(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        # 'clid': [1955451, 1959249], # это tag_id
        # 'completion_type': 0,
        # 'type': 0,
        # 'vid': 165,
        'vid': 162,
    }})

    # db_filter = "tag_id in (1955451, 1959249)"  # - -2
    # db_filter = "completion_type = 0"# - - 72
    # db_filter = "type = 0"# - - 72
    # db_filter = "vid = 165"# - -2
    db_filter = "vid = 162"  # 0

    return input_filter, db_filter


def get_filters_distr_pages(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        'place_id': 3864,
        # 'page_id': 542,
    }})

    db_filter = "place_id = 3864"  # 1
    # db_filter = "page_id = 542" #-- 576

    return input_filter, db_filter

def get_filters_avia_product_completions(pcs):
    input_filter = pcs.replace_db_fields_names({'completion_filter': {
        # 'client_id': 1143179,
        # 'service_id': 114,
        # 'service_id': 115,
        # 'source_id': 2,
        # 'source_id': 1,
        # 'product_id': [508962, 508967],
        'contract_id': [190870, 191825],

    }})

    # db_filter = "client_id = 1143179"  #  - - 3
    # db_filter = "client_id = 1143179 and service_id = 114"  #  - - 3
    # db_filter = "client_id = 1143179 and service_id = 115"  #  - - 0
    # db_filter = "client_id = 1143179 and source_id = 2"  #  - - 3
    # db_filter = "client_id = 1143179 and source_id = 1"  #  - - 0
    # db_filter = "product_id in (508962, 508967)"  #  - -7
    db_filter = "contract_id in (190870, 191825)"  #  - 2

    return input_filter, db_filter


def print_rows(source_name, db_filter):
    pcs = PartnerCompletionSources.get_by_source(source_name)
    rows_filtered = pcs.get_db_rows_number(on_date=True, extra_conditions=db_filter)
    rows_on_date = pcs.get_db_rows_number(on_date=True)
    rows_all = pcs.get_db_rows_number(on_date=False)

    print '\n' + source_name
    print "rows_filtered: " + rows_filtered
    print "rows_on_date: " + rows_on_date
    print "rows_all: " + rows_all


def test_rows_diff():
    ts_rows = {'activations': 798936,
               'adfox': 379086,
               'avia_product_completions': 9503,
               'avia_rs': 2428121,
               'blue_market': 1043,
               'boyscouts': 133,
               'buses2': 1269,
               'd_installs': 3236941,
               'distr_pages': 559,
               'rs_market_cpa': 2612737,
               'rtb_distr': 215126,
               'taxi_aggr': 4309190,
               'taxi_distr': 14380,
               'taxi_medium': 43079,
               'toloka': 300,
               'video_distr': 4897,
               'zen': 47974}

    tm_rows = {'activations': 798936,
               'adfox': 379069,
               'avia_product_completions': 9445,
               'avia_rs': 2428153,
               'blue_market': 1039,
               'boyscouts': 133,
               'buses2': 1264,
               'd_installs': 3236941,
               'distr_pages': 559,
               'rtb_distr': 215126,
               'taxi_aggr': 4308832,
               'taxi_distr': 14210,
               'taxi_medium': 43111,
               'toloka': 297,
               'video_distr': 4913,
               'zen': 47962}

    ts_c = Counter(ts_rows)
    tm_c = Counter(tm_rows)

    tm_c.subtract(ts_c)

    diff = {}
    for source, rows_diff in tm_c.items():
        if rows_diff != 0:
            diff.update(
                {source: "ts: {} -> tm: {} (diff = {})".format(ts_rows.get(source), tm_rows.get(source), rows_diff)})

    print '\n' + utils.Presenter.pretty(diff)


def test_read_pickle_value():
    result = db.balance().execute("SELECT input FROM T_EXPORT WHERE OBJECT_ID=1 AND TYPE='PARTNER_COMPL'")[0]['input']
    print 'input: {}'.format(pickle.loads(result))


# old style filters

# -------------------- COMPLETION FILTER --------------------
# input_filter = pcs.replace_db_fields_names({'completion_filter': {
# activations
# 'place_id': 2313866
# 'place_id': [2313438, 2313866]
# 'place_id': 2313438, 'shows': 1
# 'place_id': [2313438, 2313866], 'vid': [0, 50]
# 'currency_id': 2
# 'vid': [0, 50]

# distr_pages
# 'place_id': 3864
# 'places_id': 3864

# avia_rs
# 'tag_id': 2270453,
# 'shows': 10,
# 'shows': 10, 'clicks': 10,
# 'shows': 10, 'clicks': 10, 'bucks': 3638418,
# 'shows': 10, 'clicks': 10, 'bucks': D('3638418'),
# 'shows': 10, 'clicks': 10, 'bucks': '3.638418',
# 'vid': None,
# 'bucks': 255875, 'vid': None,
# 'vid': 225,
# 'shows': [1,2], 'clicks': [1,2,3], 'bucks': [255875, 511750], 'vid': [None, 375],
# 'shows': [1,2], 'clicks': [1,2,3], 'vid': [None, 375],
# 'tag_id': 227045,
# 'dt': pcs.dt.isoformat(),

# d_installs
# 'dt': pcs.dt.isoformat(),
# 'place_id': 2313866
# }})

# -------------------- DB FILTER --------------------
# activations
# db_filter = "place_id = 2313866"
# db_filter = "place_id in (2313438, 2313866)"
# db_filter = "place_id = 2313438 and shows = 1"
# db_filter = "place_id in (2313438, 2313866) and vid in (50, 0)"
# db_filter = "vid in (0, 50)"

# distr_pages
# db_filter = "place_id = 3864"

# avia_rs
# db_filter = "tag_id = 2270453"  # 10
# db_filter = "SHOWS = 10" # 8
# db_filter = "SHOWS = 10 and CLICKS = 10" # 2
# db_filter = "SHOWS = 10 and CLICKS = 10 and bucks = 3638418" # 1
# db_filter = "SHOWS = 10 and CLICKS = 10 and bucks in (3638418,3.638418)" # 1
# db_filter = "vid is null" # 802
# db_filter = "bucks = 255875 and vid is null" # 15
# db_filter = "vid = 225" # 26
# db_filter = "SHOWS in (1, 2) and CLICKS in (1, 2, 3) and BUCKS in (255875, 511750) and (vid is null or vid = 375)" # 18
# db_filter = "SHOWS in (1, 2) and CLICKS in (1, 2, 3) and (vid is null or vid = 375)" #
# db_filter = "tag_id = 227045" # 0

# d_install
# db_filter = "PLACE_ID = 2310116" # 7
