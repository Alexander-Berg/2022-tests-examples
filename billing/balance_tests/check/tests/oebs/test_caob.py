# coding: utf-8
__author__ = 'chihiro'

from collections import namedtuple
from tenacity import retry, stop_after_attempt, wait_random

import datetime

import pytest
from hamcrest import equal_to, contains_string, is_in, empty, has_length

from balance import balance_api as api
from balance import balance_steps as steps
from btestlib import utils as butils
import btestlib.reporter as reporter
from check import db, retrying, shared_steps
from check import steps as check_steps
from check.shared import CheckSharedBefore
from balance.balance_db import balance


"""
Осуществляет сравнение договоров (и его атрибутов) в биллинге и oebs

Дополнение от разработчика:
    Логика этой сверки примерно повторяет логинку экспорта из биллинга в OEBS,
    большую часть которой можно посмотреть в следующих файлах биллинга
    (пути указаны относительно базовой ссылки
    https://hg.yandex-team.ru/balance/tools/file/a7f718a213e3/):
    balance/processors/oebs/contract.py
    balance/processors/oebs/dao/contract.py
    Не сверяется в текущей реализации:
    general:
    commission_type (XXOKE_HISTORY_PERCENT)
    discard_nds (XXOKE_HISTORY_TAXRATE)
    bank_of_payee (XXOKE_REMIT_TO_ADDRESS)
    discount_pct (XXOKE_FIX_DISCOUNT)
    commission_charge_type (XXAR_TYPE_PER_CENT)
    commission_payback_type (XXOKE_PAYMENT_REWARD)
    discard_nds (XXOKE_NDS)
    account_type (XXOKE_ACCOUNT_TYPE)
    supercommission_bonus (XXOKE_BONUS_PROGRAM)
    non-general:
    nds (XXOKE_HISTORY_TAXRATE)
    

Забор данных
Забираем данные из Биллинга (в таблицу cmp.caob_bill), проверяем выполнение следующих условий
  
Contract.type != 'PREFERRED_DEAL'
и
    def is_comparable(self, min_finish_dt):
        return (
            not (self.type == 'DISTRIBUTION'
                 and getattr(self.col0, 'test_mode', 0) == 1
                 and not self.person_id)
                 # договор - действующий
            and (self.finish_dt is None or self.finish_dt >= min_finish_dt)
            # договора по этой фирме экспортируются в oebs
            and 'OEBS' in self.firm.exports
        )
  
Забираем данные из OEBS (в таблицу cmp.caob_oebs)
Условие забора данных повторяет забор данных из Биллинга

Типы расхождений:
    1 - Отсутствует в OEBS
    2 - Отсутствует в Биллинге
    3 - Расходится значение

"""


@pytest.fixture(scope="module")
def fixtures():
    person_category = 'ur'
    contract_type = 'no_agency_test'
    client_id = check_steps.create_client()

    steps.ExportSteps.export_oebs(client_id=client_id)

    person_id = check_steps.create_person(client_id, person_category=person_category)

    data = namedtuple('data', 'client_id person_id contract_type person_category')
    data_list = data(client_id=client_id,
                     person_id=person_id,
                     contract_type=contract_type,
                     person_category = person_category,
                     )
    return data_list


def create_contract(client_id, person_id, contract_type, services=[37],
                    start_dt=datetime.datetime.now().replace(day=1),
                    end_dt=datetime.datetime.now().replace(day=1) + datetime.timedelta(weeks=5),
                    prevent_oebs_export=True):
    start_dt = start_dt.strftime('%Y-%m-%dT00:00:00')
    end_dt = end_dt.strftime('%Y-%m-%dT00:00:00')
    contract_id, _ = steps.ContractSteps.create_contract(contract_type,
                                                         {'CLIENT_ID': client_id,
                                                          'PERSON_ID': person_id,
                                                          'DT': '{0}'.format(start_dt),
                                                          'FINISH_DT': '{0}'.format(end_dt),
                                                          'IS_SIGNED': '{0}'.format(start_dt),
                                                          'CURRRENCY': 'USD',
                                                          'SERVICES': services},
                                                         prevent_oebs_export)

    export_with_retry(lambda: steps.ExportSteps.export_oebs(person_id=person_id))

    return contract_id


# Обернул в retry, чтобы уйти от ошибки "Could not lock with nowait"
@retry(stop=stop_after_attempt(5), wait=wait_random(min=1, max=3), reraise=True)
def export_with_retry(func):
    return func()


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_without_changes(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в обеих системах
        -все данные сходятся
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_not_found_in_billing(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS
        -договор отсутствует в Биллинге
    Ожидаемый результат:
        заказ попадает в список с расхождений,
        состояние = "Отсутствует в Биллинге"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :new_dt
                      where attribute_batch_id = (
                                                    select attribute_batch_id
                                                    from bo.t_contract_collateral
                                                    where contract2_id = :contract_id
                                                 )
                      and code = 'FINISH_DT'
                    """
        query_params = {'contract_id': contract_id, 'new_dt': datetime.datetime.now().replace(year=2011)}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 2, 'CONTRACT_ID', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_not_found_in_oebs(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в Биллинге
        -договор отсутствует в OEBS
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Отсутствует в OEBS"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type, prevent_oebs_export=False)


    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that((contract_id, 1, 'CONTRACT_ID'), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_external_id(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, external_id = 118980/11
        -договор присутствует в биллинге, external_id = 218980/11
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'external_id' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        db.wait_export('Contract', 'OEBS', contract_id)
        check_steps.change_external_id(contract_id, 'contract2')

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'CONTRACT_EID', is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_start_dt(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, start_dt = 24.04.16 13:16:08
        -договор присутствует в биллинге, start_dt = 01.04.16 00:00:00
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'start_dt' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        db.wait_export('Contract', 'OEBS', contract_id)
        query = """
                      update bo.t_contract_collateral
                      set dt = :new_dt
                      where contract2_id = :contract_id
                    """
        query_params = {'contract_id': contract_id, 'new_dt': datetime.datetime.now() - datetime.timedelta(days=3)}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'START_DT', is_in(result))

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_finish_dt(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, finish_dt = 24.04.16 13:16:08
        -договор присутствует в биллинге, finish_dt = 01.04.16 00:00:00
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'finish_dt' в таблице с результатами запуска

    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_dt = :new_dt
                      where attribute_batch_id = (
                                                    select attribute_batch_id
                                                    from bo.t_contract_collateral
                                                    where contract2_id = :contract_id
                                                 )
                      and code = 'FINISH_DT'
                """
        query_params = {'contract_id': contract_id, 'new_dt': datetime.datetime.now() + datetime.timedelta(weeks=6)}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'FINISH_DT', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_currency_code(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, currency_code = USD
        -договор присутствует в биллинге, currency_code = EUR
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'currency_code' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                    select attribute_batch_id
                                                    from bo.t_contract_collateral
                                                    where contract2_id = :contract_id
                                                 )
                      and code = 'CURRENCY'
                """
        query_params = {'contract_id': contract_id, 'val': 978}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'CURRENCY_CODE', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_org_id(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, FIRM = 12
        -договор присутствует в биллинге, FIRM = 10
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'FIRM' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        db.wait_export('Contract', 'OEBS', contract_id)
        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                    select attribute_batch_id
                                                    from bo.t_contract_collateral
                                                    where contract2_id = :contract_id
                                                 )
                      and code = 'FIRM'
                    """
        query_params = {'contract_id': contract_id, 'val': 10}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'ORG_ID', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_person_id(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, PERSON_ID = 128972198
        -договор присутствует в биллинге, PERSON_ID = 102187498
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'PERSON_ID' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        person_id_new = check_steps.create_person(
            f.client_id, person_category=f.person_category
        )
        query = """
                      update bo.t_contract2
                      set person_id = :person_id
                      where id =:contract_id
                    """
        query_params = {'contract_id': contract_id, 'person_id': person_id_new}
        balance().execute(query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'PERSON_ID', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_payment_type(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, PAYMENT_TYPE = 2
        -договор присутствует в биллинге, PAYMENT_TYPE = 3
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'MANAGER_CODE' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                 )
                      and code = 'PAYMENT_TYPE'
                    """
        query_params = {'contract_id': contract_id, 'val': 3}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'PAYMENT_TYPE', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_manager_code(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, MANAGER_CODE = 20453
        -договор присутствует в биллинге, MANAGER_CODE = 666666
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'MANAGER_BO_CODE' в таблице с результатами запуска

    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                  )
                      and code = 'MANAGER_CODE'
                    """
        query_params = {'contract_id': contract_id, 'val': 666666}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'MANAGER_CODE',  is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_manager_code_without_diff(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, дата окончания действия договора - вчерашний день, MANAGER_CODE = 20453
        -договор присутствует в биллинге, дата окончания действия договора - вчерашний день, MANAGER_CODE = 666666
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type,
                                      start_dt=datetime.datetime.now() - datetime.timedelta(days=2),
                                      end_dt=datetime.datetime.now() - datetime.timedelta(days=1))
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                  )
                      and code = 'MANAGER_CODE'
                    """
        query_params = {'contract_id': contract_id, 'val': 666666}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_manager_bo_code(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, MANAGER_BO_CODE = 20453
        -договор присутствует в биллинге, MANAGER_BO_CODE = 666666
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'MANAGER_BO_CODE' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_id = create_contract(f.client_id, f.person_id, f.contract_type)
        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                  )
                      and code = 'MANAGER_BO_CODE'
                    """
        query_params = {'contract_id': contract_id, 'val': 20453}
        balance().execute(query, query_params)

        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                  )
                      and code = 'MANAGER_BO_CODE'
                    """
        query_params = {'contract_id': contract_id, 'val': 666666}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'MANAGER_BO_CODE', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_credit_type(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, CREDIT_TYPE = 1
        -договор присутствует в биллинге, CREDIT_TYPE = 2
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'CREDIT_TYPE' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_type = 'no_agency_post'
        contract_id = create_contract(f.client_id, f.person_id, contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update apps.oke_k_terms
                      set term_value_pk1 = '1'
                      where term_code = 'XXOKE_CREDIT'
                      and k_header_id in
                          (select k_header_id
                          from apps.oke_k_headers_full_v
                          where k_number = :contract_id
                          )
                    """
        query_params = {'contract_id': str(contract_id)}
        api.test_balance().ExecuteSQL('oebs_qa', query, query_params)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'CREDIT_TYPE', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_credit_limit_single(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, CREDIT_LIMIT = 10000000
        -договор присутствует в биллинге, CREDIT_LIMIT = 1111111
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'CREDIT_LIMIT' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_type = 'no_agency_post'
        contract_id = create_contract(f.client_id, f.person_id, contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                  )
                      and code = 'CREDIT_LIMIT_SINGLE'
                    """
        query_params = {'contract_id': contract_id, 'val': 1111111}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'CREDIT_LIMIT', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_contract_with_change_commission(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в OEBS, COMMISSION = 0
        -договор присутствует в биллинге, COMMISSION = 218
    Ожидаемый результат:
        договор попадает в список с расхождений,
        состояние = "Расходится значение", поле ATTRIBUTE_CODE = 'COMMISSION' в таблице с результатами запуска
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        contract_type = 'no_agency_post'
        contract_id = create_contract(f.client_id, f.person_id, contract_type)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(contract_id=contract_id,
                                                                collateral_id=collateral_id))

        query = """
                      update bo.t_contract_attributes
                      set value_num = :val
                      where attribute_batch_id = (
                                                  select attribute_batch_id
                                                  from bo.t_contract_collateral
                                                  where contract2_id = :contract_id
                                                  )
                      and code = 'COMMISSION'
                    """
        query_params = {'contract_id': contract_id, 'val': 22}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'COMMISSION', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_check_2173(shared_data):
    """
    Начальные условия:
        -договор с типом "Дистрибуция", у коротого имеется дочерний договор
        -оба договора присутствуют в OEBS, у дочернего договора finish_dt = 24.04.16 13:16:08
        -оба договора присутствуют в биллинге, у дочернего договора finish_dt = 01.04.16 00:00:00
    Ожидаемый результат:
        оба договора отсутствуют в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id', 'contract_id_child']
    ) as before:
        before.validate()

        dt = datetime.datetime(2015, 1, 1, 0, 0, 0)
        client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

        steps.ExportSteps.export_oebs(client_id=client_id)

        contract_id, external_id = steps.ContractSteps.create_contract(type_='universal_distr',
                                                                       params={'CLIENT_ID': client_id,
                                                                               'PERSON_ID': person_id,
                                                                               'DT': dt, 'DISTRIBUTION_TAG': tag_id},
                                                                       prevent_oebs_export=True)
        collateral_id = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(person_id=person_id,
                                                                contract_id=contract_id,
                                                                collateral_id=collateral_id))

        _, _, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
        contract_id_child, _ = steps.ContractSteps.create_contract(type_='universal_distr',
                                                                   params={'CLIENT_ID': client_id,
                                                                           'PERSON_ID': person_id,
                                                                           'DT': dt, 'DISTRIBUTION_TAG': tag_id,
                                                                           'PARENT_CONTRACT_ID': contract_id},
                                                                   prevent_oebs_export=True)
        collateral_id_child = db.get_collateral_id_by_contract_id(contract_id)

        export_with_retry(lambda: steps.ExportSteps.export_oebs(person_id=person_id,
                                                                contract_id=contract_id_child,
                                                                collateral_id=collateral_id_child))

        query = """
                      update bo.t_contract_collateral
                      set dt = :new_dt
                      where contract2_id = :contract_id
                    """
        query_params = {'contract_id': contract_id_child,
                        'new_dt': datetime.datetime.now() - datetime.timedelta(days=3)}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id_child)


    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    result_child = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id_child]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())
    butils.check_that(result_child, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_check_2242_acquiring_no_diffs(shared_data, fixtures):
    """
    Начальные условия:
        -договор присутствует в оебс
        -договор присутствует в биллинге
        -значение атрибута END_DT в биллинге = 2016-10-13
        -значение атрибута END_DT в оебс = 2016-11-05
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        f = fixtures

        steps.ExportSteps.export_oebs(client_id=f.client_id)

        start_dt = datetime.datetime.now().replace(day=1).strftime('%Y-%m-%dT00:00:00')
        end_dt = datetime.datetime.now().strftime('%Y-%m-%dT00:10:00')

        contract_id, _ = steps.ContractSteps.create_contract('acquiring',
                                                             {'CLIENT_ID': f.client_id,
                                                              'PERSON_ID': f.person_id,
                                                              'DT': '{0}'.format(start_dt),
                                                              'FINISH_DT': '{0}'.format(end_dt),
                                                              'IS_SIGNED': '{0}'.format(start_dt),
                                                              'SERVICES': [7],
                                                              'END_DT': '{0}'.format(end_dt),
                                                              'DISCOUNT_POLICY_TYPE': 3
                                                              }, prevent_oebs_export=True)
        collateral_0_id = db.get_collateral_id_by_contract_id(contract_id)

        query = """
                          update bo.t_contract_attributes
                          set value_dt = :dt
                          where attribute_batch_id = (
                                                      select attribute_batch_id 
                                                      from bo.t_contract_collateral 
                                                      where id = :collateral_id
                                                      )
                          and code = 'END_DT'
                        """
        query_params = {'collateral_id': collateral_0_id, 'dt': datetime.datetime.now()}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_check_2515_contract_offer_no_diffs(shared_data):
    """
    Начальные условия:
        -договор присутствует в оебс
        -договор присутствует в биллинге
        -значение атрибута IS_OFFER в биллинге = 1
        -значение атрибута IS_OFFER в оебс = 1
    Ожидаемый результат:
        договор отсутствует в списке расхождений

    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        contract_id = check_steps.create_contract_offer()

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_check_2515_contract_offer_diffs(shared_data):
    """
    Начальные условия:
        -договор присутствует в оебс
        -договор присутствует в биллинге
        -значение атрибута IS_OFFER в биллинге = 0
        -значение атрибута IS_OFFER в оебс = 1
    Ожидаемый результат:
        договор присутствует в списке расхождений
        состояние = "Расходится значение атрибута IS_OFFER"
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()

        contract_id = check_steps.create_contract_offer()

        query = """
                              update bo.t_contract_attributes
                              set value_num = :num
                              where attribute_batch_id = (select attribute_batch_id from  T_CONTRACT_COLLATERAL where CONTRACT2_ID = :contract_id)
                              and code = 'IS_OFFER'
                            """
        query_params = {'contract_id': contract_id,
                        'num': 0}
        balance().execute(query, query_params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(contract_id, 3, 'IS_OFFER', is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_check_2515_contract_offer_diffs_without_diff(shared_data):
    """
    Начальные условия:
        -договор отсутствует в оебс
        -договор присутствует в биллинге
        -значение атрибута IS_OFFER в биллинге = 0
    Ожидаемый результат:
        договор отсутствует в списке расхождений
    """
    with CheckSharedBefore(
            shared_data=shared_data, cache_vars=['contract_id']
    ) as before:
        before.validate()
        contract_id = check_steps.create_contract_offer(is_offer=True)

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    result = [(row['contract_id'], row['state'], row['attribute_code'])
              for row in cmp_data if row['contract_id'] == contract_id]
    reporter.log("RESULT = %s " % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CAOB)
def test_check_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """
    diffs_count = 15

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_caob(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s " % cmp_data)

    assert len(cmp_data) == diffs_count
