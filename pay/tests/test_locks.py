from contextlib import contextmanager

import pytest
from hamcrest import assert_that, equal_to, raises, calling
from sqlalchemy.engine import Connection
from sqlalchemy.orm import Session

from conftest import TestApplication
from yb_darkspirit import scheme
from yb_darkspirit.locks import exec_if_not_locked, LockedException
from yb_darkspirit.process.process_manager import PROCESSES, ProcessManager
from yb_darkspirit.task.task_manager import TASKS, TaskManager


@pytest.fixture
def cleanup_locks(application):
    with new_connection(application) as connection, new_session(connection, application) as session:
        with session.begin():
            session.query(scheme.Locks).delete()
        yield
        with session.begin():
            session.query(scheme.Locks).delete()



@contextmanager
def new_connection(application):
    # type: (TestApplication) -> Connection
    engine = application.get_dbhelper(
        database_id=application.database_id
    ).engines[0]
    connection = engine.connect()
    yield connection
    connection.close()


@contextmanager
def new_session(connection, application):
    # type: (Connection, TestApplication) -> Session
    sessionmaker = application.get_dbhelper(
        database_id=application.database_id
    ).sessionmakers[0]

    session = sessionmaker(bind=connection)
    session.clone = lambda: session

    yield session
    session.close()


def test_lock_race(application, cleanup_locks):
    def aux_session_func():
        return 'aux_session'

    def session_func():
        res = exec_if_not_locked(aux_session, lock_name='just_a_lock', func=aux_session_func)
        return res or 'session'

    def session_func_raise_on_lock():
        res = exec_if_not_locked(aux_session, lock_name='just_a_lock', func=aux_session_func, raise_on_lock=True)
        return res or 'session'

    with new_connection(application) as conn, new_connection(application) as aux_conn:
        with new_session(conn, application) as session, new_session(aux_conn, application) as aux_session:
            result = exec_if_not_locked(session, lock_name='just_a_lock', func=session_func)
            assert_that(result, equal_to('session'))

            assert_that(
                calling(exec_if_not_locked).with_args(session, lock_name='just_a_lock', func=session_func_raise_on_lock),
                raises(LockedException)
            )


@pytest.mark.parametrize('process_name', PROCESSES)
def test_process_lock_names(session, process_name):
    lock = scheme.Locks(lock_name=ProcessManager.construct_process_lock_name(process_name))
    session.add(lock)
    session.flush()


@pytest.mark.parametrize('task_name', TASKS)
def test_task_lock_names(session, task_name):
    lock = scheme.Locks(lock_name=TaskManager._construct_task_lock_name(task_name))
    session.add(lock)
    session.flush()
