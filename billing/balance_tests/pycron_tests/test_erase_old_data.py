# coding: utf-8

import datetime
from dateutil.relativedelta import relativedelta
import pytest

from butils.application import getApplication
from cluster_tools.erase_old_data import erase_old_data

ROWS_IN_TABLE = 6


@pytest.mark.parametrize(
    'config, rows_remained, min_date',
    [
        ({'t_test_erase_old_data_table': 2, 't_export_history_3': None}, 3,
         datetime.date.today() - relativedelta(days=2)),
        ({'t_test_erase_old_data_table': 0}, 1, datetime.date.today()),
        ({'t_test_erase_old_data_table': None}, ROWS_IN_TABLE,
         datetime.date.today() - relativedelta(days=ROWS_IN_TABLE - 1)),
        ({}, ROWS_IN_TABLE, datetime.date.today() - relativedelta(days=ROWS_IN_TABLE - 1))
    ]
)
def test_erase_old_data_table(session, config, rows_remained, min_date):
    session.config.__dict__['ERASE_OLD_DATA'] = config

    for days in range(ROWS_IN_TABLE):
        query = '''
        insert into BO.T_TEST_ERASE_OLD_DATA_TABLE (DT)
        values (sysdate  - :days)
        '''
        session.execute(query, {'days': days})

    erase_old_data(session, tables=['t_test_erase_old_data_table'], tables_w_partitions=[])

    sql_query = "select dt from BO.T_TEST_ERASE_OLD_DATA_TABLE"
    answer = session.execute(sql_query).fetchall()
    dates = [date[0] for date in answer]

    assert len(dates) == rows_remained
    assert min(dates).date() == min_date


@pytest.mark.parametrize(
    'config, rows_remained, min_date',
    [
        ({'t_test_erase_old_data_table_w_parts': 2, 't_pycron_state_history': None}, 3,
         datetime.date.today() - relativedelta(days=2)),
        ({'t_test_erase_old_data_table_w_parts': 0}, 1, datetime.date.today()),
        ({'t_test_erase_old_data_table_w_parts': None},
         ROWS_IN_TABLE, datetime.date.today() - relativedelta(days=ROWS_IN_TABLE - 1)),
        ({}, ROWS_IN_TABLE, datetime.date.today() - relativedelta(days=ROWS_IN_TABLE - 1))
    ]
)
def test_erase_old_data_table_w_parts(session, config, rows_remained, min_date):
    session.oracle_namespace_lock('t_test_erase_old_data_table_w_parts_lock', lockmode='exclusive', timeout=10)
    app = getApplication()
    s = app.real_new_session()

    # Из-за коммита после удаления партиций, ставим oracle_lock для запрета одновременного запуска тестов

    with s.begin():
        s.execute("delete from BO.T_TEST_ERASE_OLD_DATA_TABLE_W_PARTS")
        for days in range(ROWS_IN_TABLE):
            query = '''
            insert
            into BO.T_TEST_ERASE_OLD_DATA_TABLE_W_PARTS (DT)
            values (sysdate  - :days)
            '''
            s.execute(query, {'days': days})

    s.config.__dict__['ERASE_OLD_DATA'] = config
    erase_old_data(s, tables=[], tables_w_partitions=['t_test_erase_old_data_table_w_parts'])

    sql_query = "select dt from BO.T_TEST_ERASE_OLD_DATA_TABLE_W_PARTS"
    answer = s.execute(sql_query).fetchall()

    dates = [date[0] for date in answer]
    assert len(dates) == rows_remained
    assert min(dates).date() == min_date
