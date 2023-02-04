# coding: utf-8

import datetime
import json
from tenacity import retry, stop_after_attempt, wait_random

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import equal_to, is_in

import balance.balance_api as api
import balance.balance_db as balance_db
from balance import balance_steps as steps
from balance.balance_db import balance
from btestlib import utils as b_utils
from btestlib.data import defaults
import btestlib.reporter as reporter
from check import steps as check_steps
from check.shared import CheckSharedBefore
from check import db, shared_steps



"""
Логика этой сверки примерно повторяет логинку экспорта доп. соглашений
из биллинга в OEBS, большую часть которой можно посмотреть в следующих файлах
биллинга (пути указаны относительно базовой ссылки
https://hg.yandex-team.ru/balance/tools/file/da30a97abe26/):
balance/processors/oebs/init.py#l66
balance/processors/oebs/dao/contract.py


ЗАБОР ДАННЫХ:
Поля, сверяемые в ccaob
Соотношение полей атрибутов Биллинг-ОЕБС

ТИПЫ РАСХОЖДЕНИЙ:
* 1 - Отсутствует в OEBS
* 2 - Отсутствует в Биллинге
* 3 - Расходится значение  поле
"""

DIFFS_COUNT = 50


def create_distr_tag(client_id, passport_uid=defaults.PASSPORT_UID):
    with reporter.step(u"Создаем тег для заданного клиента: client_id: {}".format(client_id)):
        tag_id = balance_db.balance().execute("SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual")[0]['tag_id']
        reporter.attach(u"Tag ID", b_utils.Presenter.pretty(tag_id))

        api.medium().CreateOrUpdateDistributionTag(passport_uid,
                                                   {'TagID': tag_id, 'TagName': 'CreatedByScript',
                                                    'ClientID': client_id})

        return tag_id



@retry(stop=stop_after_attempt(5), wait=wait_random(min=1, max=3), reraise=True)
def export_with_retry(func):
    return func()



def create_contract(client_id, person_id, contract_type, services=None,
                    start_dt=datetime.datetime.now().replace(day=1),
                    end_dt=datetime.datetime.now().replace(day=1) + datetime.timedelta(weeks=5),
                    additional_params=None):
    last_day_of_month = (start_dt + relativedelta(months=1) - datetime.timedelta(days=1)).strftime(
        '%Y-%m-%dT00:00:00')
    start_dt = start_dt.strftime('%Y-%m-%dT00:00:00')
    end_dt = end_dt.strftime('%Y-%m-%dT00:00:00')
    if contract_type in ['universal_distr', 'revshare_distr_full']:
        tag_id = create_distr_tag(client_id)
        reporter.attach(u'', u'Client ID: {} Person ID: {} Tag ID: {}'.format(client_id, person_id, tag_id))
    else:
        tag_id = None

    if services is None:
        services = [37]

    params = {'CLIENT_ID': client_id,
              'PERSON_ID': person_id,
              'DT': '{0}'.format(start_dt),
              'FINISH_DT': '{0}'.format(end_dt),
              'IS_SIGNED': '{0}'.format(start_dt),
              'SERVICES': services,
              'END_DT': '{0}'.format(last_day_of_month),

              'DISCOUNT_POLICY_TYPE': 3,
              'DISTRIBUTION_TAG': tag_id
              }
    if additional_params is not None:
        params.update(additional_params)
    contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                         params)
    collateral_0_id = db.get_collateral_id_by_contract_id(contract_id)

   # Если договор выгрузился через OEBS_API, то допник не нужно выгружать
    export_with_retry(lambda: steps.ExportSteps.export_oebs(person_id=person_id,
                                                            contract_id=contract_id))

    return contract_id, collateral_0_id


def create_collateral(client_id, person_id, contract_type, services=None,
                      start_dt=datetime.datetime.now().replace(day=2),
                      end_dt=datetime.datetime.now().replace(day=1) + datetime.timedelta(weeks=5),
                      collateral_type=80, contract_id=None):
    start_dt = start_dt.strftime('%Y-%m-%dT00:00:00')
    end_dt = end_dt.strftime('%Y-%m-%dT00:00:00')

    if services is None:
        services = [37]

    if contract_id is None:
        contract_id, _ = create_contract(client_id, person_id, contract_type, services)
        steps.ExportSteps.export_oebs(person_id=person_id, contract_id=contract_id)
    steps.ContractSteps.create_collateral(collateral_type,
                                          {'CONTRACT2_ID': contract_id,
                                           'DT': start_dt,
                                           'FINISH_DT': end_dt,
                                           'IS_SIGNED': start_dt,
                                           'END_DT': end_dt,
                                           'LOYAL_CLIENTS': json.dumps([{"id": "1",
                                                                         "num": '6372690',
                                                                         "client": "6372690",
                                                                         "turnover": "668743.73",
                                                                         "todate": "2015-07-20"}])
                                           })
    collateral1_id = db.get_zero_collateral_id_by_contract_id(contract_id)

    # Если договор выгрузился через OEBS_API, то допник не нужно выгружать
    export_with_retry(lambda: steps.ExportSteps.export_oebs(person_id=person_id,
                                                            contract_id=contract_id))

    return contract_id, collateral1_id


def create_new_client_and_person(person_category='ur'):
    client_id = check_steps.create_client()
    steps.ExportSteps.export_oebs(client_id=client_id)

    person_id = check_steps.create_person(
        client_id, person_category=person_category
    )
    return client_id, person_id


# ----------------------------------------------------------------------------
# --------------------------------GENERAL-------------------------------------
# ----------------------------------------------------------------------------
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_without_diffs_general(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -все атрибуты сходятся
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'taxi_pre', services=[37, 111])
        

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_not_found_in_billing_general(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение отсутствует в биллинге
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в биллинге атрибут COLLATERAL_ID"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'taxi_pre', services=[37, 111])

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_dt = :new_dt
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'FINISH_DT'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id, 'new_dt': datetime.datetime.now().replace(year=2011)}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 2, 'COLLATERAL_ID', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_bonuses_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута SUPERCOMMISSION_BONUS в биллинге = null
        -значение атрибута SUPERCOMMISSION_BONUS в оебс = 123
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в биллинге атрибут BONUSES"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'market_cpa_post')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_num = null
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'SUPERCOMMISSION_BONUS'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 2, 'BONUSES', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_credit_limit_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута CREDIT_LIMIT_SINGLE в биллинге = 13
        -значение атрибута CREDIT_LIMIT_SINGLE в оебс = 2081
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута CREDIT_LIMIT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'no_agency_post')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_num = 13
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'CREDIT_LIMIT_SINGLE'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'CREDIT_LIMIT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_supercommission_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута SUPERCOMMISSION в биллинге = 13
        -значение атрибута SUPERCOMMISSION в оебс = 2081
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута SUPERCOMMISSION"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'comm')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_num = 13
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'SUPERCOMMISSION'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'SUPERCOMMISSION', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_account_type_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут ACCOUNT_TYPE отсутствует в ОЕБС
        -значение атрибута ACCOUNT_TYPE в биллинге = 0
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут ACCOUNT_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'comm')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              insert into bo.t_contract_attributes
                              (id, collateral_id, code, value_num, passport_id, attribute_batch_id)
                              VALUES
                              (s_contract_attributes_id.nextval, :col_id, 'ACCOUNT_TYPE', 0, 16571028, :attribute_batch_id)
                            """
        query_params = {'col_id': collateral0_id, 'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'ACCOUNT_TYPE', collateral0_id)]
    b_utils.check_that(set(expected_result), equal_to(set(result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_loyal_clients_to_date_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута LOYAL_CLIENTS в биллинге = null
        -значение атрибута LOYAL_CLIENTS в оебс = [{"id":"1","num":'6372690',"client":"6372690","turnover":"668743.73","todate":"2015-07-20"}]
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в биллинге атрибуты LOYAL_CLIENT_TO_DATE и LOYAL_CLIENT_TURNOVER"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'comm', collateral_type=1024)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]
        query = """
                                  update bo.t_contract_attributes
                                  set value_str = null
                                  where attribute_batch_id = :attribute_batch_id
                                  and code = 'LOYAL_CLIENTS'
                                """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 2, 'LOYAL_CLIENT_TO_DATE', collateral1_id),
                       (contract_id, 2, 'LOYAL_CLIENT_TURNOVER', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_commission_declared_sum_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут COMMISSION_DECLARED_SUM отсутствует в ОЕБС
        -значение атрибута COMMISSION_DECLARED_SUM в биллинге = 1010
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут COMMISSION_DECLARED_SUM"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'comm')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              insert into bo.t_contract_attributes
                              (id, collateral_id, code, value_num, passport_id, attribute_batch_id)
                              VALUES
                              (s_contract_attributes_id.nextval, :col_id, 'COMMISSION_DECLARED_SUM', 1010, 16571028, :attribute_batch_id)
                            """
        query_params = {'col_id': collateral0_id, 'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'COMMISSION_DECLARED_SUM', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_fixed_market_discount_pct_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут FIXED_MARKET_DISCOUNT_PCT отсутствует в ОЕБС
        -значение атрибута FIXED_MARKET_DISCOUNT_PCT в биллинге = 1010
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут FIXED_MARKET_DISCOUNT_PCT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'comm')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                  insert into bo.t_contract_attributes
                  (id, collateral_id, code, value_num, passport_id, attribute_batch_id)
                  VALUES
                  (s_contract_attributes_id.nextval, :col_id, 'FIXED_MARKET_DISCOUNT_PCT', 1010, 16571028, :attribute_batch_id)
                """
        query_params = {'col_id': collateral0_id, 'attribute_batch_id': attribute_batch_id}

        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'FIXED_MARKET_DISCOUNT_PCT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_finish_dt_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута FINISH_DT в биллинге = 2016-10-13
        -значение атрибута FINISH_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута FINISH_DT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'no_agency_test')

        

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :new_dt
                      where attribute_batch_id =
                      (select attribute_batch_id
                      from bo.t_contract_collateral
                      where contract2_id = :contract_id
                      and num is not null)
                      and code = 'FINISH_DT'
                    """
        query_params = {'contract_id': contract_id, 'new_dt': datetime.datetime.now() + datetime.timedelta(weeks=6)}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'FINISH_DT', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_services_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута SERVICES в биллинге = null
        -значение атрибута SERVICES в оебс = [7]
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в биллинге атрибут SERVICES"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'no_agency_test')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'SERVICES'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 2, 'SERVICES', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_payment_type_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута PAYMENT_TYPE в биллинге = 1
        -значение атрибута PAYMENT_TYPE в оебс = 3
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута PAYMENT_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'no_agency_test')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 1
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'PAYMENT_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'PAYMENT_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_discount_policy_type_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута DISCOUNT_POLICY_TYPE в биллинге = 2
        -значение атрибута DISCOUNT_POLICY_TYPE в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в оебс атрибуты DISCOUNT_POLICY и FIX_DISCOUNT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'no_agency_test')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 2
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'DISCOUNT_POLICY_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = (contract_id, 1, 'DISCOUNT_POLICY', collateral0_id)
    b_utils.check_that(expected_result, is_in(result),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")

    expected_result = (contract_id, 1, 'FIX_DISCOUNT', collateral0_id)
    b_utils.check_that(expected_result, is_in(result),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_discard_nds_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута DISCARD_NDS в биллинге = 1
        -значение атрибута DISCARD_NDS в оебс = 3
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISCARD_NDS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'market_cpa_post')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 1
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'DISCARD_NDS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)


    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DISCARD_NDS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_commission_charge_type_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута COMMISSION_CHARGE_TYPE в биллинге = 2
        -значение атрибута COMMISSION_CHARGE_TYPE в оебс = 3
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута COMMISSION_CHARGE_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'market_cpa_post')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 2
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'COMMISSION_CHARGE_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'COMMISSION_CHARGE_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_commission_payback_type_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута COMMISSION_PAYBACK_TYPE в биллинге = 1
        -значение атрибута COMMISSION_PAYBACK_TYPE в оебс = 3
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута COMMISSION_PAYBACK_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'market_cpa_post')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 1
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'COMMISSION_PAYBACK_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'COMMISSION_PAYBACK_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_commission_percent_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута COMMISSION_TYPE в биллинге = 1
        -значение атрибута COMMISSION_TYPE в оебс = 2
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в оебс атрибуты COMMISSION_PERCENT и COMMISSION_SCALE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'market_cpa_post')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 1
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'COMMISSION_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = (contract_id, 3, 'COMMISSION_PERCENT', collateral0_id)
    b_utils.check_that(expected_result, is_in(result),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")

    expected_result = (contract_id, 3, 'COMMISSION_SCALE', collateral0_id)
    b_utils.check_that(expected_result, is_in(result),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_contract_type_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута COMMISSION в биллинге = 2
        -значение атрибута COMMISSION в оебс = 3
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута CONTRACT_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'taxi_pre')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 2
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'COMMISSION'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'CONTRACT_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_country_not_converge(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута COUNTRY в биллинге = 187
        -значение атрибута COUNTRY в оебс = 17
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута COUNTRY"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'taxi_pre', services=[111])

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 187
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'COUNTRY'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'COUNTRY', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


# ----------------------------------------------------------------------------
# ---------------------------PARTNERS-----------------------------------------
# ----------------------------------------------------------------------------

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_without_diffs_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -все атрибуты сходятся
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        person_id = check_steps.create_person(client_id, person_category='ur')

        contract_id, collateral0_id = create_contract(client_id, person_id, 'rsya_ssp')

        

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_end_reason_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута END_REASON в биллинге = 2
        -значение атрибута END_REASON в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута END_REASON"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'rsya_universal', collateral_type=2090)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_num = 2
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'END_REASON'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'END_REASON', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_nds_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута NDS в биллинге = null
        -значение атрибута NDS в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISCARD_NDS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'rsya_ssp')

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'NDS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DISCARD_NDS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_bm_direct_pct_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут BM_DIRECT_PCT отсутствует в ОЕБС
        -значение атрибута BM_DIRECT_PCT в биллинге = 11
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут BM_DIRECT_PCT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'rsya_universal', collateral_type=2090)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]

        query = """
                              insert into bo.t_contract_attributes
                              (id, collateral_id, code, value_num, passport_id, attribute_batch_id)
                              VALUES
                              (s_contract_attributes_id.nextval, :col_id, 'BM_DIRECT_PCT', 11, 16571028, :attribute_batch_id)
                            """
        query_params = {'col_id': collateral1_id, 'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'BM_DIRECT_PCT', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_bm_market_pct_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут BM_MARKET_PCT отсутствует в ОЕБС
        -значение атрибута BM_MARKET_PCT в биллинге = 11
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут BM_MARKET_PCT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'rsya_universal', collateral_type=2090)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]

        query = """
                              insert into bo.t_contract_attributes
                              (id, collateral_id, code, value_num, passport_id, attribute_batch_id)
                              VALUES
                              (s_contract_attributes_id.nextval, :col_id, 'BM_MARKET_PCT', 11, 16571028, :attribute_batch_id)
                            """
        query_params = {'col_id': collateral1_id, 'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'BM_MARKET_PCT', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_bm_domains_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут BM_DOMAINS отсутствует в ОЕБС
        -значение атрибута BM_DOMAINS в биллинге = 'test.ru'
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут BM_DOMAINS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'rsya_universal', collateral_type=2090)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]
        query = """
                              insert into bo.t_contract_attributes
                              (id, collateral_id, code, value_str, passport_id, attribute_batch_id)
                              VALUES
                              (s_contract_attributes_id.nextval, :col_id, 'BM_DOMAINS', 'test.ru', 16571028, :attribute_batch_id)
                            """
        query_params = {'col_id': collateral1_id, 'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'BM_DOMAINS', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_domains_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -атрибут DOMAINS отсутствует в ОЕБС
        -значение атрибута DOMAINS в биллинге = 'test.ru'
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут DOMAINS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'rsya_universal', collateral_type=2090)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]
        query = """
                              insert into bo.t_contract_attributes
                              (id, collateral_id, code, value_str, passport_id, attribute_batch_id)
                              VALUES
                              (s_contract_attributes_id.nextval, :col_id, 'DOMAINS', 'test.ru', 16571028, :attribute_batch_id)
                            """
        query_params = {'col_id': collateral1_id, 'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 1, 'DOMAINS', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_end_dt_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута FINISH_DT в биллинге = 2016-10-13
        -значение атрибута FINISH_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута FINISH_DT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral1_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral1_id = create_collateral(client_id, person_id, 'rsya_universal', collateral_type=2090)

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral1_id})[0]["attribute_batch_id"]
        query = """
                      update bo.t_contract_attributes
                      set value_dt = :dt
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'END_DT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id,
                        'dt': datetime.datetime.now() + datetime.timedelta(weeks=5)}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'FINISH_DT', collateral1_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_pay_to_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута PAY_TO в биллинге = 2
        -значение атрибута PAY_TO в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута PAY_TO"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'rsya_ssp')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 2
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'PAY_TO'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'PAY_TO', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_contract_type_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута CONTRACT_TYPE в биллинге = 6
        -значение атрибута CONTRACT_TYPE в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута CONTRACT_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'rsya_ssp')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 6
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'CONTRACT_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'CONTRACT_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_search_forms_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута SEARCH_FORMS в биллинге = 1
        -значение атрибута SEARCH_FORMS в оебс = 5
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута SEARCH_FORMS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'rsya_ssp')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 1
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'SEARCH_FORMS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'SEARCH_FORMS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_percent_not_converge_partner(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута PARTNER_PERCENT в биллинге = 11
        -значение атрибута PARTNER_PERCENT в оебс = 5
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута PARTNER_PERCENT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'rsya_ssp')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 11
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'PARTNER_PCT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'PARTNER_PERCENT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")

# ----------------------------------------------------------------------------
# ---------------------------DISTRIBUTION-------------------------------------
# ----------------------------------------------------------------------------

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_without_diffs_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -все атрибуты сходятся
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_nds_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута NDS в биллинге = null
        -значение атрибута NDS в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISCARD_NDS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'NDS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DISCARD_NDS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_partner_ctype_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута CONTRACT_TYPE в биллинге = 6
        -значение атрибута CONTRACT_TYPE в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута CONTRACT_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_num = 2
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'CONTRACT_TYPE'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'CONTRACT_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_distr_types_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута DISTRIBUTION_PLACES в биллинге = null
        -значение атрибута DISTRIBUTION_PLACES в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISTR_TYPES"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                              update bo.t_contract_attributes
                              set value_num = null
                              where attribute_batch_id = :attribute_batch_id
                              and code = 'DISTRIBUTION_PLACES'
                            """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 2, 'DISTR_TYPES', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_end_dt_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута END_DT в биллинге = 2016-10-13
        -значение атрибута END_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута FINISH_DT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :dt
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'END_DT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id,
                        'dt': (datetime.datetime.now() + datetime.timedelta(weeks=5))}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'FINISH_DT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_install_soft_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута INSTALL_SOFT в биллинге = 'ccaob'
        -значение атрибута INSTALL_SOFT в оебс = 'test'
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута INSTALL_SOFT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_str = 'ccaob'
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'INSTALL_SOFT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'INSTALL_SOFT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_install_price_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута INSTALL_PRICE в биллинге = 666
        -значение атрибута INSTALL_PRICE в оебс = 542
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута INSTALL_PRICE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 666
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'INSTALL_PRICE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'INSTALL_PRICE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_supplements_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута SUPPLEMENTS в биллинге = null
        -значение атрибута SUPPLEMENTS в оебс = 1
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Отсутствует в ОЕБС атрибут SUPPLEMENTS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'SUPPLEMENTS'
                      and key_num = 1
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 2, 'SUPPLEMENTS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_download_domains_not_converge_distr(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута DOWNLOAD_DOMAINS в биллинге = 'ccaob'
        -значение атрибута DOWNLOAD_DOMAINS в оебс = 'test'
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DOWNLOAD_DOMAINS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'universal_distr')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_str = 'ccaob'
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'DOWNLOAD_DOMAINS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DOWNLOAD_DOMAINS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


# ----------------------------------------------------------------------------
# ------------------------------SPENDABLE-------------------------------------
# ----------------------------------------------------------------------------

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_without_diffs_spend(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -все атрибуты сходятся
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'spendable_coroba', services=[210],
                                                      additional_params={'FIRM': 13,
                                                                         'CURRENCY': 643,
                                                                         'NDS': str(18)})

        

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_end_dt_not_converge_spend(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута END_DT в биллинге = 2016-10-13
        -значение атрибута END_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута FINISH_DT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'spendable_coroba', services=[210],
                                                      additional_params={'FIRM': 13,
                                                                         'CURRENCY': 643,
                                                                         'NDS': str(18)})

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :dt
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'END_DT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id,
                        'dt': (datetime.datetime.now() + datetime.timedelta(weeks=5))}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'FINISH_DT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_nds_not_converge_spend(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута NDS в биллинге = null
        -значение атрибута NDS в оебс = 13
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISCARD_NDS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'spendable_coroba', services=[210],
                                                      additional_params={'FIRM': 13,
                                                                         'CURRENCY': 643,
                                                                         'NDS': str(18)})

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'NDS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DISCARD_NDS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_contract_type_not_converge_spend(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута CONTRACT_TYPE в биллинге = 81
        -значение атрибута CONTRACT_TYPE в оебс = 13
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута CONTRACT_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'spendable_coroba', services=[210],
                                                      additional_params={'FIRM': 13,
                                                                         'CURRENCY': 643,
                                                                         'NDS': str(18)})

        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set key_num = 135
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'SERVICES'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'CONTRACT_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_check_2613_contract_offer_pay_to_not_converge(shared_data):
    """

    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral_id']
    ) as before:
        before.validate()

        contract_id = check_steps.create_contract_offer()
        collateral_id = balance().execute('select id from T_CONTRACT_COLLATERAL where CONTRACT2_ID = :contract_id ',
                                          {'contract_id': contract_id})[0]["id"]
        query = """
                                      update bo.t_contract_attributes
                                      set value_num = :num
                                      where attribute_batch_id = (select attribute_batch_id from  T_CONTRACT_COLLATERAL where CONTRACT2_ID = :contract_id)
                                      and code = 'PAY_TO'
                                    """
        query_params = {'contract_id': contract_id,
                        'num': 5}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'PAY_TO', collateral_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


# ----------------------------------------------------------------------------
# ------------------------------GEOCONTEXT------------------------------------
# ----------------------------------------------------------------------------

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_without_diffs_geo(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -все атрибуты сходятся
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'geo_default')

        


    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_end_dt_not_converge_geo(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута END_DT в биллинге = 2016-10-13
        -значение атрибута END_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута FINISH_DT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'geo_default')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :dt
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'END_DT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id,
                        'dt': (datetime.datetime.now() + datetime.timedelta(weeks=5))}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'FINISH_DT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_nds_not_converge_geo(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута NDS в биллинге = null
        -значение атрибута NDS в оебс = 13
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISCARD_NDS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'geo_default')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'NDS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DISCARD_NDS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_commission_payback_type_not_converge_geo(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута COMMISSION_PAYBACK_TYPE в биллинге = 5
        -значение атрибута COMMISSION_PAYBACK_TYPE в оебс = 13
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута COMMISSION_PAYBACK_TYPE"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'geo_default')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = 5
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'COMMISSION_PAYBACK_TYPE'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'COMMISSION_PAYBACK_TYPE', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


# ----------------------------------------------------------------------------
# ------------------------------AFISHA------------------------------------
# ----------------------------------------------------------------------------

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_without_diffs_afisha(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -все атрибуты сходятся
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'afisha_defaults')

        

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_end_dt_not_converge_afisha(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута END_DT в биллинге = 2016-10-13
        -значение атрибута END_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута FINISH_DT"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'afisha_defaults')

        

        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :dt
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'END_DT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id,
                        'dt': (datetime.datetime.now() + datetime.timedelta(weeks=5))}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'FINISH_DT', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_nds_not_converge_afisha(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута NDS в биллинге = null
        -значение атрибута NDS в оебс = 13
    Ожидаемый результат:
        доп.соглашение присутствует в списке расхождений
        состояние = "Расходится значение атрибута DISCARD_NDS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'collateral0_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        contract_id, collateral0_id = create_contract(client_id, person_id, 'spendable_coroba', services=[210],
                                                      additional_params={'FIRM': 13,
                                                                         'CURRENCY': 643,
                                                                         'NDS': str(18)})
        
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_num = null
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'NDS'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = [(contract_id, 3, 'DISCARD_NDS', collateral0_id)]
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


# ----------------------------------------------------------------------------
# ---------------------------------ACQUIRING----------------------------------
# ----------------------------------------------------------------------------

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_check_2242_acquiring_no_diffs(shared_data):
    """
    Начальные условия:
        -доп.соглашение присутствует в оебс
        -доп.соглашение присутствует в биллинге
        -значение атрибута END_DT в биллинге = 2016-10-13
        -значение атрибута END_DT в оебс = 2016-11-05
    Ожидаемый результат:
        доп.соглашение отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        client_id, person_id = create_new_client_and_person()
        start_dt = datetime.datetime.now().replace(day=1).strftime('%Y-%m-%dT00:00:00')
        end_dt = datetime.datetime.now().strftime('%Y-%m-%dT00:00:00')

        contract_id, _ = steps.ContractSteps.create_contract('acquiring',
                                                             {'CLIENT_ID': client_id,
                                                              'PERSON_ID': person_id,
                                                              'DT': '{0}'.format(start_dt),
                                                              'FINISH_DT': '{0}'.format(end_dt),
                                                              'END_DT': '{0}'.format(end_dt),
                                                              'IS_SIGNED': '{0}'.format(start_dt),
                                                              'SERVICES': [7],

                                                              'DISCOUNT_POLICY_TYPE': 3
                                                              })
        collateral_0_id = db.get_collateral_id_by_contract_id(contract_id)
        attribute_batch_id = \
            balance().execute('select attribute_batch_id from T_CONTRACT_COLLATERAL where ID = :contract_id ',
                              {'contract_id': collateral_0_id})[0]["attribute_batch_id"]

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :dt
                      where attribute_batch_id = :attribute_batch_id
                      and code = 'END_DT'
                    """
        query_params = {'attribute_batch_id': attribute_batch_id,
                        'dt': datetime.datetime.now()}
        balance_db.balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'], row['collateral_id'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    expected_result = []
    b_utils.check_that(set(result), equal_to(set(expected_result)),
                       u"Проверяем, что итоговый и ожидаемый результаты совпадают")


#----------------------------------------------------------------------------
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CCAOB)
def test_ccaob_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_ccaob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    b_utils.check_that(len(cmp_data), equal_to(DIFFS_COUNT),
                       u'Проверяем, что количество расхождений, выявленное сверкой, '
                       u'равно ожидаемому количеству расхождений')
