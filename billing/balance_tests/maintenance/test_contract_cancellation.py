# coding: utf-8
__author__ = 'atkaya'

import allure
import balance.balance_db as db
from datetime import datetime
import itertools

from balance.balance_steps import ContractSteps

MAGIC_DAY = 0

# По просьбе аудиторов
EXCLUDED_LIST = '''1266038, 1266033, 1266065, 1266055, 1266052, 1266031, 1266030, 1266054,
                 1266049, 1266099, 1266037, 1266051, 1266048, 1266069, 1266036,
                 1267914, 1268123, 1267902, 1267781, 1266050, 1266053, 1266046,
                 1266057, 1266103, 1266072, 1266071, 1266082, 1266092, 1267381,
                 1267282, 1267308, 1266162, 1266126, 1266144, 1266133, 1266119,
                 1266131, 1266159, 1268183, 1267763, 1267762, 1267751, 1266083,
                 1266056, 1266096, 1266093, 1266066, 1266060, 1266062, 1266068,
                 1266080, 1266061, 1266079, 1268113, 1267882, 1268058, 1268324,
                 1267570, 1267617, 1267170, 1267839'''


def split_ngroup(iterable, n):
    """split_ngroup('ABC', 2) --> AB C"""
    iterable = iter(iterable)
    while True:
        res = list(itertools.islice(iterable, n))
        if not res:
            break
        yield res


def get_contract_ids(query, col_name):
    return [row[col_name] for row in db.balance().execute(query)]


def test_cancellation_dsp_contract(day=datetime.now().weekday()):
    # удаляем данные по понедельникам
    if day == MAGIC_DAY:
        with allure.step(u"Аннулируем договоры dsp."):
            contract_ids = get_contract_ids(
                """
                select distinct contract_id 
                from bo.mv_partner_dsp_contract
                where contract_id not in ({excluded_list})
                """.format(excluded_list=EXCLUDED_LIST),
                'contract_id')

            if contract_ids:
                query = "update bo.t_contract_collateral " \
                        "set is_cancelled = date'2099-01-01' " \
                        "where contract2_id in ({ids}) " \
                        "and is_cancelled is null"
                for g in split_ngroup(contract_ids, 999):
                    db.balance().execute(query.format(ids=', '.join(str(c) for c in g)))
                    ContractSteps.refresh_contracts_cache(*g)


def test_cancellation_rsya_contract(day=datetime.now().weekday()):
    # удаляем данные по понедельникам
    if day == MAGIC_DAY:
        with allure.step(u"Аннулируем договоры РСЯ."):
            contract_ids = get_contract_ids(
                """
                select distinct id 
                from bo.MV_PARTNER_CONTRACT_PUTTEE
                where id not in ({excluded_list})
                """.format(excluded_list=EXCLUDED_LIST),
                'id')

            if contract_ids:
                query = "update bo.t_contract_collateral " \
                        "set is_cancelled = date'2099-01-01' " \
                        "where contract2_id in ({ids}) " \
                        "and is_cancelled is null"
                for g in split_ngroup(contract_ids, 999):
                    db.balance().execute(query.format(ids=', '.join(str(c) for c in g)))


def test_cancellation_taxi_contract(day=datetime.now().weekday()):
    # удаляем данные по понедельникам
    if day == MAGIC_DAY:
        with allure.step(u"Аннулируем договоры Такси."):
            contract_ids = get_contract_ids(
                """
                select distinct contract_id
                from BO.MV_PARTNER_TAXI_CONTRACT
                where contract_id not in
                    (
                    select distinct contract2_id
                        from t_contract_collateral
                        where
                            (id in
                                (
                                select collateral_id
                                    from t_contract_attributes
                                    where code = 'MANAGER_CODE'
                                    and value_num in (28179, 27703)
                                )
                                or
                                passport_id in (1120000000027363, 1120000000038919, 1120000000077543, 1120000000120841)
                            )
                            and dt >= (select value_dt from t_config where item = 'TEST_DB_RESTORE_DT')
                    )
                    and contract_id not in ({excluded_list})
                """.format(excluded_list=EXCLUDED_LIST),
                'contract_id')
            if contract_ids:
                query = """
                        update t_contract_collateral
                        set is_cancelled = date'2099-01-01'
                        where contract2_id in ({ids})
                        and is_cancelled is null 
                        """
                for g in split_ngroup(contract_ids, 999):
                    db.balance().execute(query.format(ids=', '.join(str(c) for c in g)))
                    ContractSteps.refresh_contracts_cache(*g)


# больше не нужно, т.к. мы слезли с матвьюх в дистрибуции
def cancellation_distribution_contract(day=datetime.now().weekday()):
    # удаляем данные по понедельникам
    if day == MAGIC_DAY:
        with allure.step(u"Аннулируем договоры Дистрибуции."):
            contract_ids = get_contract_ids(
                """
                select distinct contract_id 
                from v_distribution_contract
                where contract_id not in ({excluded_list})
                """.format(excluded_list=EXCLUDED_LIST),
            )

            if contract_ids:
                query = "update t_contract_collateral " \
                        "set is_cancelled = date'2099-01-01' " \
                        "where contract2_id in ({ids}) " \
                        "and is_cancelled is null"
                for g in split_ngroup(contract_ids, 999):
                    db.balance().execute(query.format(ids=', '.join(str(c) for c in g)))
            ContractSteps.refresh_contracts_cache(*contract_ids)
