# -*- coding: utf-8 -*-

from contextlib import closing
import os
import tempfile
import shutil
import zipfile
import copy
import pytest

from liquibase import Liquibase
from changelog import Changelog, Changefile, Changeset
from executors import Executor, statements
from dbtools import ChangesetNotFound, DatabaseError


db_params = dict(driver='DBOracle',
                 params=dict(user='bo',
                             password='balalancing',
                             dsn='DEV_BALANCE_YANDEX_RU'),
                 defaultschema='BO',
                 databasechangelog='DATABASECHANGELOG_TEST',
                 databasechangeloglock='DATABASECHANGELOGLOCK_TEST')


tests_zip = 'test_queue.zip'

sqlfile = 'tables/overdraft_params_history.sql'

test_changeset_string = """--changeset ufian:BALANCE-23728-1 runAlways:true runOnChange:true
alter session set ddl_lock_timeout = 600;
"""

test_changeset_string2 = """--changeset test:BALANCE-TEST runAlways:true runOnChange:true
insert into {} (ID,AUTHOR,FILENAME,DATEEXECUTED,ORDEREXECUTED,EXECTYPE,MD5SUM,DESCRIPTION,LIQUIBASE,DEPLOYMENT_ID) 
values ('XXX','test','from_string',SYSTIMESTAMP,2,'EXECUTED','7:d4074a1cde285411cb3494f7d55d821b','','3.5.1','9991154572')
""".format(db_params['databasechangelog'])


@pytest.fixture(scope="session")
def lq():
    lq = Liquibase(db_params)
    yield lq
    lq.db.free_lock()


@pytest.fixture(scope="session")
def cl_params():
    dirpath = tempfile.mkdtemp()
    zip_ref = zipfile.ZipFile(tests_zip, 'r')
    zip_ref.extractall(dirpath)
    zip_ref.close()
    cl_params = dict(changelogdir=dirpath,
                     changelogroot='changelog.xml')
    yield cl_params
    shutil.rmtree(dirpath)


def test_liquibase_process_sql_invalid_params(lq):
    """ Should fail without params """
    with pytest.raises(TypeError):
        lq.process_sql()


def test_liquibase_process_string_invalid_params(lq):
    """ Should fail without string """
    with pytest.raises(TypeError):
        lq.process_string()


def test_liquibase_invalid_db_driver():
    """ Should fail with wrong db driver """
    with pytest.raises(DatabaseError):
        wrong_db_params = db_params.copy()
        wrong_db_params.update({'driver': 'xxx'})
        lq = Liquibase(wrong_db_params)


def test_database_check_create_databasechangelog(lq):
    """ Method can be executed without exceptions and table exists afterwards """
    lq.db.check_create_databasechangelog()
    query = "SELECT COUNT(*) FROM " + db_params['databasechangelog']
    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        lq.db.execute(query)


def test_database_check_create_databasechangeloglock(lq):
    """ Method can be executed without exceptions and table exists afterwards """
    lq.db.check_create_databasechangeloglock()
    query = "SELECT COUNT(*) FROM " + db_params['databasechangeloglock']
    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        lq.db.execute(query)


def test_database_acquire_free_lock(lq):
    """ Check if lock can be acquired """
    assert lq.db.locked()
    lq.db.free_lock()
    assert not lq.db.locked()
    lq.db.acquire_lock()
    assert lq.db.locked()


def test_database_get_databasechangelog(lq):
    """ Check if databasechangelog exists, accessible and contains data """
    dbchangelog = lq.db.get_databasechangelog()
    assert len(dbchangelog) >= 1346


def test_database_check_checksum(lq):
    """ Check checksum calculating algorithm """
    test_changeset = Changeset(test_changeset_string, filename='header.sql', last=False)
    lq.db.check_checksum(test_changeset)


def test_database_register_changeset(lq):
    """ Check ran changeset can be registered """
    test_changeset = Changeset(test_changeset_string, filename='header.sql', last=False)
    query = 'SELECT COUNT(*) FROM {} WHERE md5sum = \'{}\''.format(db_params['databasechangelog'],
                                                                 test_changeset.checksum)

    try:
        lq.db.check_checksum(test_changeset)
        increment = 0
    except ChangesetNotFound:
        increment = 1

    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        count1 = lq.db.executeandfetchone(query).get('COUNT(*)')
        lq.db.connection.begin()
        lq.db.register_changeset(test_changeset)
        lq.db.connection.commit()

    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        count2 = lq.db.executeandfetchone(query).get('COUNT(*)')

    assert count1 == count2 - increment


def test_executor_queue(lq, cl_params):
    """ Check if executor can be created and queue built correctly """
    cl = Changelog(**cl_params).flatten()
    ex = Executor(cl, lq.db, contexts=['testing'])
    queue = ex.queue
    assert len(queue) == 2


def test_executor_run(lq):
    """ Check if executor can run one changeset """
    query = 'SELECT COUNT(*) FROM {} WHERE id = \'XXX\''.format(db_params['databasechangelog'])
    del_query = 'DELETE FROM {} WHERE id = \'XXX\''.format(db_params['databasechangelog'])
    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        count1 = lq.db.executeandfetchone(query).get('COUNT(*)')

    changeset = Changeset(test_changeset_string2, filename='from_string', last=True)
    cl = [changeset]
    ex = Executor(cl, lq.db)
    ex.run(changeset)

    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        count2 = lq.db.executeandfetchone(query).get('COUNT(*)')
        lq.db.connection.begin()
        lq.db.execute(del_query)
        lq.db.connection.commit()

    assert count1 == count2 - 1


def test_executor_process_queue(lq, cl_params):
    """ Check if executor is able to process test queue """
    cl = Changelog(**cl_params).flatten()
    ex = Executor(cl, lq.db, contexts=['testing'])
    try:
        ex.process_queue()
    except SyntaxError: # compilation error is not actually error
        pass


def test_executor_context(lq, cl_params):
    """ Check if executor is able to deal with context """
    cl = Changelog(**cl_params).flatten()
    ex = Executor(cl, lq.db, contexts=['test'])
    assert len(ex.queue) == 3


def test_executor_marknextran(lq, cl_params):
    """ Check markNextChangesetRan command """
    cl = Changelog(**cl_params)

    for changeset in cl.flatten():
        if not changeset.runAlways:
            break

    query = 'SELECT DATEEXECUTED FROM {} WHERE MD5SUM = \'{}\''.format(db_params['databasechangelog'],
                                                                       changeset.checksum)
    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        dateexecuted1 = lq.db.executeandfetchone(query).get('DATEEXECUTED')
        del_query = 'DELETE FROM {} WHERE MD5SUM = \'{}\''.format(db_params['databasechangelog'],
                                                                       changeset.checksum)
        lq.db.execute(del_query)
        lq.db.connection.commit()

    lq.db.databasechangelog = lq.db.get_databasechangelog()

    ex = Executor(cl.flatten(), lq.db, contexts=['testing'])
    ex.marknextran()

    with closing(lq.db.connection.cursor()) as lq.db.cursor:
        dateexecuted2 = lq.db.executeandfetchone(query).get('DATEEXECUTED')

    assert dateexecuted1 != dateexecuted2


def test_executor_statements():
    """ Check statements generator """
    changeset = Changeset(test_changeset_string, filename='from_string', last=True)
    sts = list(statements(changeset))
    assert len(sts) == 1
    assert sts[0] == u'alter session set ddl_lock_timeout = 600'


def test_changelog_invalid_changelogdir(cl_params):
    """ Should fail with wrong db driver """
    with pytest.raises(RuntimeError):
        wrong_cl_params = cl_params.copy()
        wrong_cl_params.update({'changelogdir': '/'})
        cl = Changelog(**wrong_cl_params)


def test_changelog_flatten(cl_params):
    """ Check if changelog can be built and correct """
    cl = list(Changelog(**cl_params).flatten())
    assert len(cl) == 1330
    assert cl[0].id == 'BALANCE-23728-1'


def test_changefile(cl_params):
    """ Check if changelog can be built from one sql file """

    cl = Changefile('', os.path.join(cl_params['changelogdir'], sqlfile)).content
    assert len(cl) == 2
    assert cl[0].id == 'BALANCE-28633-t_overdraft_params_history-create-sequence'
    assert cl[1].id == 'BALANCE-28633-t_overdraft_params_history-create-table'

