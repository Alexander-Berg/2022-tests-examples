# coding=utf-8


__author__ = 'a-vasin'

import datetime
import itertools
import json

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import equal_to, has_item, not_

import btestlib.utils as utils
import balance.balance_db as db
from balance import balance_steps as steps
from btestlib.constants import StagerProject
from btestlib.matchers import contains_dicts_with_entries
from stager_data import INPUT_DATA, EXPECTED_DATA
from stager_data_steps import DT, make_cloud_context, make_avia_context, make_taxi_tl_closed_attributes, \
    make_default_context

YT_FOLDER = "//home/balance/test/stat_aggregator/taxi"

PROJECTS_MAPPING = {
    StagerProject.EDA: {
        'input': ['billing_export_commissions'],
        'output': ['completions']
    },

    StagerProject.DRIVE: {
        'input': ['aggregations'],
        'output': ['calculations'],
        'temp': ['partner_products', 'sorted_aggregations']
    },

    StagerProject.CLOUD: {
        'input': ['marketplace_in', 'events'],
        'output': ['completions', 'errors', 'marketplace_out'],
        'temp': ['timeline', 'distinct_projects']
    },

    StagerProject.CONNECT: {
        'input': ['aggregations'],
        'output': ['calculations'],
        'temp': ['distinct_products', 'products']
    },

    StagerProject.AVIA: {
        'input': ['show_log', 'redir_log'],
        'output': ['errors', 'completions', 'distribution', 'fraud'],
        'temp': ['aggregated_shows', 'fraud_index', 'clean_completions_with_shows', 'completions_with_contract_data',
                 'contract_data', 'currency_rates', 'products', 'filtr_crr', 'filtr_rt', 'filtr_yip', 'filtr_boic_yuid',
                 'filtr_boic_uip', 'filtr_clck_yuid', 'filtr_clck_uip', 'clean_completions',
                 'distinct_clients', 'yandex_ips', 'ips']
    },

    StagerProject.TAXI: {
        'input': ['tl_revenues_in', 'order_billings', 'marketplace_billings'],
        'output': ['tl_revenues_copy', 'tl_revenues_out',
                   'tl_distrib_out',
                   'errors'],
        'temp': ['currency'],
        'closing': ['tl_revenues_copy', 'tl_revenues_out', 'tl_distrib_out']
    },
}

CONTEXT_MAPPING = {
    StagerProject.CLOUD: make_cloud_context,
    StagerProject.AVIA: make_avia_context,
}


@pytest.mark.parametrize('project',
                         [
                             StagerProject.DRIVE,
                             StagerProject.CLOUD,
                             StagerProject.CONNECT,
                             # StagerProject.AVIA,  # работает ~25 минут, разумнее раскоменчивать только при изменениях
                             StagerProject.TAXI,
                         ],
                         ids=lambda p: p.upper()
                         )
def test_stager(project):
    context = create_context(project)

    yt_client = steps.YTSteps.create_yt_client()
    all_table_names = write_tables(yt_client, project, context)

    run_aggregation(all_table_names, project)

    for table_name in PROJECTS_MAPPING[project]['output']:
        data = steps.YTSteps.read_table(yt_client, make_path(project, table_name))
        expected_data = EXPECTED_DATA[project](context)[table_name]
        utils.check_that(data, contains_dicts_with_entries(expected_data), u'Проверяем табличку {}'.format(table_name))

    for table_name in PROJECTS_MAPPING[project].get('closing', []):
        attributes = steps.YTSteps.list_table_attributes(yt_client, make_path(project, table_name))
        utils.check_that(attributes, not_(has_item('fetcher_config')), u'Проверяем отсутствие атрибута закрытия таблицы')


@pytest.mark.parametrize('dt_aggregation',
                         [0, 1],
                         ids=['BEFORE_DT_AGGREGATION',
                              'AFTER_DT_AGGREGATION', ]
                         )
def test_eda_stager(dt_aggregation):
    project = StagerProject.EDA
    # еда по разному агрегируется в зависимости от даты.
    # До даты dt_field_aggregation_start_dt в конфиге поле dt в данных еды не учитывается,
    # в выходной таблице в dt проставляется дата агрегации (таблицы)
    # После даты dt_field_aggregation_start_dt в конфиге dt в данных еды транакается и агрегируется

    dt_field_aggregation_start_dt_as_str, dt_field_aggregation_start_dt = _get_eda_dt_aggregation_start()
    dt = dt_field_aggregation_start_dt - datetime.timedelta(days=1) if not dt_aggregation \
        else dt_field_aggregation_start_dt
    context = create_context(project, dt=dt, custom_params={'dt_aggregation': dt_aggregation})
    yt_client = steps.YTSteps.create_yt_client()
    all_table_names = write_tables(yt_client, project, context, dt=dt)

    run_aggregation(all_table_names, project, dt=dt)

    for table_name in PROJECTS_MAPPING[project]['output']:
        data = steps.YTSteps.read_table(yt_client, make_path(project, table_name, dt=dt))
        expected_data = EXPECTED_DATA[project](context)[table_name]
        utils.check_that(data, contains_dicts_with_entries(expected_data), u'Проверяем табличку {}'.format(table_name))


def test_taxi_tl_closed_table_aggregation():
    project = StagerProject.TAXI
    dt = DT - relativedelta(days=5)
    context = create_context(project, dt)

    yt_client = steps.YTSteps.create_yt_client()
    all_table_names = write_tables(yt_client, project, context, dt)

    attributes = make_taxi_tl_closed_attributes()
    table_data = INPUT_DATA[project](context)['tl_revenues_in']
    steps.YTSteps.create_data_in_yt(yt_client, make_path(project, 'tl_revenues_copy', dt), table_data, attributes)

    run_aggregation(all_table_names, project, dt)

    output_tables = [table_name for table_name in PROJECTS_MAPPING[project]['closing']
                     if table_name != 'tl_revenues_copy']
    for table_name in output_tables:
        exists = steps.YTSteps.exists_table(yt_client, make_path(project, table_name, dt))
        utils.check_that(exists, equal_to(False), u'Проверяем отсутствие таблички {}'.format(table_name))


def test_taxi_tl_table_closing():
    project = StagerProject.TAXI
    dt_closed = DT - relativedelta(days=2)
    dt_new = DT - relativedelta(days=1)

    yt_client = steps.YTSteps.create_yt_client()
    write_tables(yt_client, project, context={}, dt=dt_closed, empty=True)
    all_table_names = write_tables(yt_client, project, context={}, dt=dt_new, empty=True)

    run_aggregation(all_table_names, project, dt_closed)

    for table_name in PROJECTS_MAPPING[project]['closing']:
        attributes = steps.YTSteps.list_table_attributes(yt_client, make_path(project, table_name, dt_closed))
        utils.check_that(attributes, has_item('fetcher_config'), u'Проверяем наличие атрибута закрытия таблицы')


def create_context(project, dt=DT, custom_params=None):
    context = CONTEXT_MAPPING.get(project, lambda _: {})(dt)
    context.update(make_default_context(dt))
    if custom_params:
        context.update(custom_params)
    return context


def write_tables(yt_client, project, context, dt=DT, empty=False):
    all_table_names = list(itertools.chain.from_iterable(PROJECTS_MAPPING[project].values()))
    for table_name in all_table_names:
        steps.YTSteps.remove_table_in_yt(make_path(project, table_name, dt), yt_client)

    for table_name in PROJECTS_MAPPING[project]['input']:
        input_data = [] if empty else INPUT_DATA[project](context)[table_name]
        steps.YTSteps.create_data_in_yt(yt_client, make_path(project, table_name, dt), input_data)

    return all_table_names


def run_aggregation(all_table_names, project, dt=DT):
    custom_paths = {table_name: make_generic_path(project, table_name) for table_name in all_table_names}
    run_response = steps.StagerSteps.run_stager(project, dt, custom_paths=json.dumps(custom_paths))

    request_id = run_response['request_id']
    steps.StagerSteps.wait_completion(request_id)


def make_generic_path(project, table_name):
    return '{}/{}/{}/$date'.format(YT_FOLDER, project, table_name)


def make_path(project, table_name, dt=DT):
    return '{}/{}/{}/{}'.format(YT_FOLDER, project, table_name, dt.strftime('%Y-%m-%d'))


def _get_eda_dt_aggregation_start():
    result = db.balance().execute("select value_json from bo.t_config where item='STAGER_EDA_PARAMS'")
    dt_field_aggregation_start_dt_as_str = json.loads(result[0]['value_json'])['dt_field_aggregation_start_dt']
    dt_field_aggregation_start_dt = datetime.datetime.strptime(dt_field_aggregation_start_dt_as_str, '%Y-%m-%d')
    return dt_field_aggregation_start_dt_as_str, dt_field_aggregation_start_dt



@pytest.mark.parametrize('project',
                         [
                             # StagerProject.DRIVE,
                             # StagerProject.CLOUD,
                             # StagerProject.CONNECT,
                             # StagerProject.AVIA,  # работает ~25 минут, разумнее раскоменчивать только при изменениях
                             StagerProject.TAXI,
                         ],
                         ids=lambda p: p.upper()
                         )
def test_stager_and_compl(project):
    context = create_context(project)
    yt_client = steps.YTSteps.create_yt_client()
    BASE_DATE = utils.Date.nullify_time_of_date(datetime.datetime.now()-relativedelta(days=56))

    for i in range(0,1):
        dt = BASE_DATE+relativedelta(days=i)
        all_table_names = write_tables(yt_client, project, context,dt=dt)
        run_aggregation(all_table_names, project)
        for table_name in PROJECTS_MAPPING[project]['output']:
            data = steps.YTSteps.read_table(yt_client, make_path(project, table_name))
            expected_data = EXPECTED_DATA[project](context)[table_name]
            utils.check_that(data, contains_dicts_with_entries(expected_data), u'Проверяем табличку {}'.format(table_name))

        for table_name in PROJECTS_MAPPING[project].get('closing', []):
            attributes = steps.YTSteps.list_table_attributes(yt_client, make_path(project, table_name))
            utils.check_that(attributes, not_(has_item('fetcher_config')), u'Проверяем отсутствие атрибута закрытия таблицы')
        steps.CommonPartnerSteps.create_partner_completions_resource('taxi_aggr_tlog', dt)