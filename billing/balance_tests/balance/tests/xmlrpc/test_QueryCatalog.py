# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest
from hamcrest import equal_to

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance.features import Features
from btestlib import utils
from btestlib.environments import BalanceHosts as hosts
from btestlib.matchers import equal_to_casted_dict
from simpleapi.matchers.deep_equals import deep_equals_to

FIRM_ID_TO_CHECK = 1
CLIENT_ID = 873982
EXPECTED_COLUMNS = ['t_firm.id',
                    't_firm.title',
                    't_firm.default_currency',
                    't_firm.default_iso_currency',
                    # 't_firm.contract_id',   удалили из схемы в BALANCE-24056
                    't_firm.unilateral',
                    # 't_firm.invoice_paysys_id',   удалили из схемы в BALANCE-24056
                    't_firm.postpay',
                    't_firm.nds_pct',
                    't_firm.email',
                    't_firm.phone',
                    't_firm.payment_invoice_email',
                    # 't_firm.alter_permition_code',  удалили из схемы в BALANCE-31845
                    't_firm.pa_prefix',
                    't_firm.region_id',
                    't_firm.config',
                    't_firm.currency_rate_src',
                    't_firm.inn',
                    't_firm.kpp',
                    't_firm.legaladdress',
                    't_firm.mdh_id',
                    't_firm.test_env',
                    't_firm.mnclose_email']

EXPECTED_COLUMNS_V_DISTR_CONTRACT = ['v_distribution_contract.client_id',
                                     'v_distribution_contract.person_id',
                                     'v_distribution_contract.id',
                                     'v_distribution_contract.parent_contract_id',
                                     'v_distribution_contract.contract_start_dt',
                                     'v_distribution_contract.dt',
                                     'v_distribution_contract.external_id',
                                     'v_distribution_contract.manager_uid',
                                     'v_distribution_contract.manager_internal_uid',
                                     'v_distribution_contract.collateral_id',
                                     'v_distribution_contract.is_signed',
                                     'v_distribution_contract.is_faxed',
                                     'v_distribution_contract.end_dt',
                                     'v_distribution_contract.tailless_end_dt',
                                     'v_distribution_contract.nds',
                                     'v_distribution_contract.contract_end_dt',
                                     'v_distribution_contract.tail_time',
                                     'v_distribution_contract.avg_discount_pct',
                                     'v_distribution_contract.contract_type',
                                     'v_distribution_contract.test_mode',
                                     'v_distribution_contract.install_price',
                                     'v_distribution_contract.advisor_price',
                                     'v_distribution_contract.activation_price',
                                     'v_distribution_contract.search_price',
                                     'v_distribution_contract.reward_type',
                                     'v_distribution_contract.currency',
                                     'v_distribution_contract.iso_currency',
                                     'v_distribution_contract.uni_has_revshare',
                                     'v_distribution_contract.uni_has_fixed',
                                     'v_distribution_contract.uni_has_searches',
                                     'v_distribution_contract.uni_has_addapter_ret',
                                     'v_distribution_contract.uni_has_addapter_dev',
                                     'v_distribution_contract.fixed_scale_id',
                                     'v_distribution_contract.currency_calculation',
                                     'v_distribution_contract.tag_id',
                                     'v_distribution_contract.products_currency',
                                     'v_distribution_contract.products_iso_currency',
                                     'v_distribution_contract.search_currency',
                                     'v_distribution_contract.search_iso_currency',
                                     'v_distribution_contract.platform_type',
                                     'v_distribution_contract.firm_id',
                                     'v_distribution_contract.payment_type',
                                     'v_distribution_contract.use_geo_filter',
                                     'v_distribution_contract.accounted_regions']


# проверяем columns в выдаче метода для таблицы t_firm
@reporter.feature(Features.XMLRPC)
@pytest.mark.tickets('BALANCE-23755')
def test_QueryCatalog_t_firm_check_columns():
    t_firm_data = api.medium().QueryCatalog(['t_firm'])

    columns = t_firm_data['columns']

    # сравниваем шаблон с полями, выданными методом
    utils.check_that(columns, deep_equals_to(EXPECTED_COLUMNS), 'Сравниваем поля')


# проверяем общее количество строк и сравниваем с количесвом строк в таблице t_firm
@reporter.feature(Features.XMLRPC)
@pytest.mark.tickets('BALANCE-23755')
def test_QueryCatalog_t_firm_check_result_wo_filter():
    t_firm_data = api.medium().QueryCatalog(['t_firm'])

    firms = t_firm_data['result']
    firm_ids = set(firm[0] for firm in firms)

    firms_from_db = db.balance().execute("SELECT id  FROM t_firm")
    firm_ids_from_db = set(firm['id'] for firm in firms_from_db)

    utils.check_that(firm_ids, equal_to(firm_ids_from_db), 'Сравниваем количество фирм')



# проверяем данные для одной из фирм и сравниваем с t_firm
@reporter.feature(Features.XMLRPC)
@pytest.mark.smoke
@pytest.mark.tickets('BALANCE-23755')
@pytest.mark.ignore_hosts(hosts.PT, hosts.PTY, hosts.PTA)
def test_QueryCatalog_t_firm_check_result_with_filter():
    filter = "t_firm.id = " + str(FIRM_ID_TO_CHECK)
    t_firm_data = api.medium().QueryCatalog(['t_firm'], filter)

    result = t_firm_data['result'][0]
    columns = t_firm_data['columns']
    i = 0

    for x in columns:
        columns[i] = columns[i].split('.')[1]
        i += 1

    firm_data = dict(zip(columns, result))

    expected_firm_data = db.balance().execute(
        "SELECT id, title, unilateral, postpay, nds_pct, "
        "email, phone, payment_invoice_email, pa_prefix, "
        "region_id, config, currency_rate_src, default_currency, default_iso_currency, inn, kpp, legaladdress, mdh_id, test_env FROM t_firm WHERE id = :firm_id",
        {'firm_id': FIRM_ID_TO_CHECK})[0]

    expected_firm_data['config'] = expected_firm_data['config'].replace("\"", "'")
    # костыль, ручка трактует 0 как False
    expected_firm_data['test_env'] = False

    # сравниваем данные по фирме
    utils.check_that(firm_data, equal_to_casted_dict(expected_firm_data), 'Сравниваем значения полей фирмы')


# проверяем columns в выдаче метода для таблицы v_distribution_contract
@reporter.feature(Features.XMLRPC)
@pytest.mark.tickets('BALANCE-24379')
def test_QueryCatalog_v_distribution_contract_check_columns():
    filter = "v_distribution_contract.client_id = " + str(CLIENT_ID)
    v_distr_contract = api.medium().QueryCatalog(['v_distribution_contract'], filter)

    columns = v_distr_contract['columns']

    # сравниваем шаблон с полями, выданными методом
    utils.check_that(columns, deep_equals_to(EXPECTED_COLUMNS_V_DISTR_CONTRACT), 'Сравниваем поля')
