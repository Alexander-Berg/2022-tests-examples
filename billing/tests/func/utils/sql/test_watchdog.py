# -*- coding: utf-8 -*-

import functools
import threading
import contextlib

import mock
import cx_Oracle
from sqlalchemy import exc as sa_exc
from sqlalchemy import text as sa_text

from billing.dcs.tests.utils import BaseTestCase, create_patcher

from billing.dcs.dcs.utils.sql import watchdog
from billing.dcs.dcs.utils.common import Struct, log

# TODO: проверку формирования dsn отдаём на проверку регресии
# TODO: но по хорошему нужно будет написать тесты и на это

# Переопределить threading.Event.wait довольно сложно
# Поэтому мы устанавливаем время ожидания стремящимся к нулю
MINIMAL_TICK = 0.0001

CHECK_RUNS_PATCH_PATH = 'billing.dcs.dcs.utils.sql.watchdog'
patch = create_patcher(CHECK_RUNS_PATCH_PATH)

DB_CONFIGS = {
    'watchdog_connection_service_name': 'balance',
    'watchdog_connection_port': 1521,
    'watchdog_retry_max_attempts': 1,
    'watchdog_retry_wait_time': MINIMAL_TICK,
    'watchdog_check_count': 10,
    'watchdog_check_interval': MINIMAL_TICK,
    'watchdog_should_assert_rw': False,
    'watchdog_enabled': True,

    'acquire_parallels_watchdog_disable_parallel_hint_check': False,
}


class DummyWatchdogThread(watchdog.BaseWatchdogThread):
    """
    Пустой поток-наблюдатель для тестирования логики взаимодействия между
      потоком и запросом
    """

    def _do_check(self):
        raise watchdog.StopWatchdogException()


class DummyQueryWatchdog(watchdog.BaseQueryWatchdog):
    thread_cls = DummyWatchdogThread


@contextlib.contextmanager
def patch_engine(execute_function):
    """
    Патчим engines_registry на получение коннекторов-пустышек.
    Заменяем в них методы execute и connect().execute() на переданную функцию.
    Используется для подмены выполнения запроса.
    """
    engine_path = '__getitem__.return_value'
    execute_path = '.'.join((engine_path, 'execute', 'side_effect'))
    connection_execute_path = '.'.join((
        engine_path, 'connect', 'return_value', 'execute', 'side_effect'))

    with patch('engines_registry') as mock_:
        mock_.configure_mock(**{
            execute_path: execute_function,
            connection_execute_path: execute_function,
        })
        yield mock_


@contextlib.contextmanager
def patch_thread_func(thread_cls_or_path, function_name, side_effect):
    """
    Патчим функцию "function_name" у требуемого класса потока.
    Используется для переопределения методов _do_check и _kill_watched_query у
      потоков.
    """
    if isinstance(thread_cls_or_path, basestring):
        patch_func = mock.patch
        function_path = '{}.{}'.format(thread_cls_or_path, function_name)
        args = (function_path, )
    else:
        patch_func = mock.patch.object
        args = (thread_cls_or_path, function_name)

    with patch_func(*args, side_effect=side_effect, autospec=True) as mock_:
        yield mock_


class TestController(object):
    """
    Класс-помощник для управления ходом тестирования потоков-наблюдателей.
    Потребовался из-за того, что нам нужно синхронизировать начала выполнения
      потока и запроса, а так же уметь управлять их выполненим.

    Позволяет управлять поведением потока и запроса.
    Синхронизирует начало выполнения потока и запроса.
    Позволяет продолжить выполнение запроса, по требованию потока.
    Содержит небольшие обёртки для более удобного управления.
    Позволяет получить ошибки потока, после его выполнения.
    ...
    """

    context = None

    def __init__(self, timeout=60):
        self._timeout = timeout

        self._event_thread_ready = threading.Event()
        self._event_query_ready = threading.Event()

        self._event_query_allow_to_run = threading.Event()
        self._event_query_allow_to_finish = threading.Event()

        self._watchdog_thread = None

        self.context = Struct()
        self.thread_iteration = 0

    def reset(self):
        events = (
            self._event_thread_ready,
            self._event_query_ready,
            self._event_query_allow_to_run,
            self._event_query_allow_to_finish,
        )
        for event in events:
            event.clear()

        self._watchdog_thread = None
        self.thread_iteration = 0

    @property
    def thread_iteration(self):
        return self.context.thread_iteration

    @thread_iteration.setter
    def thread_iteration(self, value):
        self.context.thread_iteration = value

    @property
    def is_first_thread_iteration(self):
        return self.thread_iteration == 1

    @property
    def is_last_thread_iteration(self):
        if self._watchdog_thread is not None:
            return self.thread_iteration == self._watchdog_thread._check_count
        return False

    def inc_thread_iteration(self):
        self.thread_iteration += 1

    def _fail_if_not(self, condition, text=None):
        if not condition:
            raise RuntimeError(text or 'condition failed')

    def flag_thread_ready(self):
        if not self._event_thread_ready.is_set():
            log.debug('Thread ready')
            self._event_thread_ready.set()

    def continue_only_if_thread_ready(self):
        if not self._event_thread_ready.is_set():
            log.debug('Waiting for thread')
            self._fail_if_not(self._event_thread_ready.wait(self._timeout),
                              'Waited for thread too long')

    def flag_query_ready(self):
        if not self._event_query_ready.is_set():
            log.debug('Query ready')
            self._event_query_ready.set()

    def continue_only_if_query_ready(self):
        if not self._event_query_ready.is_set():
            log.debug('Waiting for query')
            self._fail_if_not(self._event_query_ready.wait(self._timeout),
                              'Waited for query too long')

    def allow_query_to_run(self):
        log.debug('Allow query to run')
        self._event_query_allow_to_run.set()

    def allow_query_to_finish(self):
        log.debug('Allow query to finish')
        self._event_query_allow_to_finish.set()

    def continue_only_if_query_can_run(self):
        log.debug('Query asked to run')
        self._fail_if_not(self._event_query_allow_to_run.wait(self._timeout),
                          'Query waited for run too long')

    def continue_only_if_query_can_finish(self):
        log.debug('Query asked to finish')
        self._fail_if_not(self._event_query_allow_to_finish.wait(self._timeout),
                          'Query waited for finish too long')

    def run_query(self, func=None):
        self.flag_query_ready()
        self.continue_only_if_thread_ready()

        log.debug('Run query')
        self.continue_only_if_query_can_run()

        log.debug('Query started')

        res = None
        if func:
            log.debug('Query run custom function')
            # Здесь можно добавить передачу контроллера в выполняемую функцию
            res = func()

        self.continue_only_if_query_can_finish()
        log.debug('Query finished')
        return res

    def run_thread(self, thread, func=None):
        if self._watchdog_thread is None:
            self._watchdog_thread = thread

        self.flag_thread_ready()
        self.continue_only_if_query_ready()

        self.inc_thread_iteration()
        if self.is_first_thread_iteration:
            log.debug('Thread started')
        log.debug('Thread iteration #%d', self.thread_iteration)

        res = None
        if func:
            log.debug('Thread run custom function')

            # Здесь можно добавить передачу контроллера в выполняемую функцию
            res = func(thread)
        else:
            log.debug('Allow query to finish')
            self.allow_query_to_run()
            self.allow_query_to_finish()

        log.debug('Thread finished')
        return res

    def thread_stop_watchdog(self):
        raise watchdog.StopWatchdogException

    def thread_next_interation(self):
        raise watchdog.NextIterationException

    def query_kill(self, code=666, message='session killed'):
        orig = cx_Oracle.DatabaseError(Struct(code=code, message=message))
        raise sa_exc.DatabaseError(None, None, orig=orig)

    def get_thread_exception(self):
        if self._watchdog_thread:
            return self._watchdog_thread.exception
        else:
            raise RuntimeError('TestController has no any watchdog thread')


class BaseWatchDogTestCase(BaseTestCase):
    mocked_thread_cls = None

    def setUp(self):
        self.controller = TestController(timeout=5)

        self._db_configs_patch = mock.patch(
            '{}.{}'.format(CHECK_RUNS_PATCH_PATH, 'DB_CONFIGS'),
            DB_CONFIGS
        )
        self._db_configs_patch.start()

    def tearDown(self):
        self._db_configs_patch.stop()

    @contextlib.contextmanager
    def _patch_query(self, inner_func):
        """
        Обёртка над patch_engine. Использует ThreadController.run_query
          как основную функцию выполнения запроса для синхронизации с потоком.
          Внутри также исполняет inner_func.
        """
        def connection_execute(object_):
            new_inner = functools.partial(inner_func, object_)
            return self.controller.run_query(new_inner)

        with patch_engine(connection_execute) as mock_:
            yield mock_

    @contextlib.contextmanager
    def _patch_thread(self, inner_func):
        """
        Обёртка над patch_thread_func. Использует ThreadController.run_thread
          как основную функцию потока для синхронизации с запросом. Внутри
          также исполняет inner_func.
        """
        def do_check(thread):
            return self.controller.run_thread(thread, inner_func)

        with patch_thread_func(self.mocked_thread_cls,
                               '_do_check', do_check) as mock_:
            yield mock_

    @contextlib.contextmanager
    def _patch_kill_func(self, inner_func):
        kill_func_patch = patch_thread_func(
            self.mocked_thread_cls, '_kill_watched_query',
            functools.partial(inner_func)
        )
        with kill_func_patch as mock_:
            yield mock_


class DummyWatchDogTestCase(BaseWatchDogTestCase):
    mocked_thread_cls = DummyWatchdogThread

    def test_query_do_well(self):
        def thread_inner(thread):
            self.controller.allow_query_to_run()
            self.controller.allow_query_to_finish()
            self.controller.thread_stop_watchdog()

        def query_inner(object_):
            return object_[::-1]

        with self._patch_thread(thread_inner), self._patch_query(query_inner):
            w = DummyQueryWatchdog('dummy', check_count=1)
            r = w.execute('select 1')

        self.assertEqual(r, '1 tceles')
        self.assertIsNone(self.controller.get_thread_exception())

    def test_query_was_killed(self):

        def thread_inner(thread):
            self.controller.thread_next_interation()

        def kill_func_inner(thread):
            self.controller.allow_query_to_run()
            self.controller.allow_query_to_finish()

        def query_inner(object_):
            self.controller.query_kill()

        with self._patch_query(query_inner), \
                self._patch_thread(thread_inner) as thread_mock, \
                self._patch_kill_func(kill_func_inner) as kill_func_mock:

            check_count = 3
            w = DummyQueryWatchdog('dummy', check_count=check_count)
            with self.assertRaises(watchdog.OutOfRetriesException):
                w.execute('select 1')

            self.assertEqual(thread_mock.call_count, check_count)
            self.assertEqual(kill_func_mock.call_count, 1)

    def test_query_was_killed_and_retried(self):
        def thread_inner(thread):
            self.controller.thread_next_interation()

        def kill_func_inner(thread):
            self.controller.allow_query_to_run()
            self.controller.allow_query_to_finish()

        def query_inner(object_):
            self.controller.reset()
            self.controller.query_kill()

        with self._patch_query(query_inner), \
                self._patch_thread(thread_inner) as thread_mock, \
                self._patch_kill_func(kill_func_inner) as kill_func_mock, \
                mock.patch('time.sleep') as time_sleep_mock:

            check_count = 3
            retry_count = 5

            w = DummyQueryWatchdog('dummy',
                                   retry_max_attempts=retry_count,
                                   check_count=check_count)
            with self.assertRaises(watchdog.OutOfRetriesException):
                w.execute('select 1')

            self.assertEqual(thread_mock.call_count, check_count * retry_count)
            self.assertEqual(time_sleep_mock.call_count, retry_count - 1)
            self.assertEqual(kill_func_mock.call_count, retry_count)

    def test_query_unknown_exception(self):
        def thread_inner(thread):
            self.controller.allow_query_to_run()
            self.controller.allow_query_to_finish()

            thread.shutdown_event.set()

        def query_database_error(object_):
            self.controller.query_kill(code=-1, message='unknown exception')

        def query_type_error(object_):
            # Например, если вместо текста запроса пришло что-то необычное
            raise TypeError('unknown object type')

        with self._patch_thread(thread_inner):
            w = DummyQueryWatchdog('dummy')

            with self._patch_query(query_database_error):
                with self.assertRaises(sa_exc.DatabaseError):
                    w.execute('select 1')

            self.controller.reset()
            with self._patch_query(query_type_error):
                with self.assertRaises(TypeError):
                    w.execute('select 1')

    def test_thread_unknown_exception(self):
        thread_expected_exc = RuntimeError('unknown thread exception')

        def thread_inner(thread):
            raise thread_expected_exc

        def query_inner(object_):
            return object_[::-1]

        self.controller.allow_query_to_run()
        self.controller.allow_query_to_finish()

        with self._patch_thread(thread_inner), self._patch_query(query_inner):
            w = DummyQueryWatchdog('dummy')
            r = w.execute('select 1')

        self.assertEqual(r, '1 tceles')
        self.assertIs(self.controller.get_thread_exception(),
                      thread_expected_exc)

    def test_assert_rw_raises_exception(self):
        def query_inner(object_):
            return Struct(scalar=mock.Mock(return_value='READ WRITE'))

        retry_count = 5
        with patch_engine(query_inner) as engine_mock:
            w = DummyQueryWatchdog(
                'dummy',
                retry_max_attempts=retry_count,
                should_assert_rw=True
            )
            with self.assertRaises(watchdog.ConnectedToPrimaryException):
                w.execute('select 1')
            self.assertEqual(engine_mock.__getitem__.call_count, retry_count)

    def test_assert_rw_retried(self):
        retry_count = 5
        rows = ['READ WRITE'] * (retry_count - 1) + ['OPEN READ']
        scalar_mock = mock.Mock(side_effect=rows)
        execute_mock = mock.Mock(scalar=scalar_mock)

        def query_inner(object_):
            return execute_mock

        with patch_engine(query_inner) as engine_mock:
            w = DummyQueryWatchdog(
                'dummy',
                retry_max_attempts=retry_count,
                should_assert_rw=True
            )
            r = w.execute('select 1')
            self.assertEqual(engine_mock.__getitem__.call_count, retry_count)
            self.assertIs(r, execute_mock)

    def test_db_id_supplying(self):
        def thread_inner(thread):
            self.controller.allow_query_to_run()
            self.controller.allow_query_to_finish()
            self.controller.thread_stop_watchdog()

        def query_inner(object_):
            return object_[::-1]

        with self._patch_thread(thread_inner), self._patch_query(query_inner):
            w = DummyQueryWatchdog('dummy', check_count=1)
            r = w.execute('select 1')

        self.assertEqual(r, '1 tceles')
        self.assertIsNone(self.controller.get_thread_exception())

    def test_db_id_not_supplied(self):
        with self.assertRaises(watchdog.ConnectionInfoIsNotSpecified):
            w = DummyQueryWatchdog()
            w.execute('select 1')

    def test_connection_preparing(self):
        def query_inner(object_):
            return object_[::-1]

        class NotPrepared(Exception):
            pass

        def prepare_connection_func(connection):
            raise NotPrepared('You are not prepared!')

        w = DummyQueryWatchdog('dummy', check_count=1,
                               prepare_connection_func=prepare_connection_func)

        with self._patch_query(query_inner):
            with self.assertRaises(NotPrepared):
                w.execute('select 1')

    def test_use_connection(self):
        from billing.dcs.dcs.utils.sql.connection import engines_registry

        def thread_inner(thread):
            pass

        with engines_registry['cmp'].connect() as connection:
            with self._patch_thread(thread_inner):
                w = watchdog.AcquireParallelsQueryWatchdog(use_connection=connection)
                w.execute('select 1 from dual')


class AcquireParallelWatchdogTestCase(BaseWatchDogTestCase):
    _mocked_thread_name = 'AcquireParallelsWatchdogThread'
    _mocked_watchdog_name = 'AcquireParallelsQueryWatchdog'
    _orig_do_check = watchdog.AcquireParallelsWatchdogThread._do_check

    mocked_thread_cls = '{}.{}'.format(CHECK_RUNS_PATCH_PATH,
                                       _mocked_thread_name)

    @contextlib.contextmanager
    def _patch_get_parallels_count(self, return_value):
        mocked_path = '{}.{}.{}'.format(
            CHECK_RUNS_PATCH_PATH, self._mocked_thread_name, '_get_parallels_count')
        with mock.patch(mocked_path, return_value=return_value) as mock_:
            yield mock_

    @contextlib.contextmanager
    def _patch_should_we_watch(self, return_value=None):
        mocked_path = '{}.{}.{}'.format(
            CHECK_RUNS_PATCH_PATH, self._mocked_watchdog_name, '_should_we_watch')
        with mock.patch(mocked_path, return_value=return_value) as mock_:
            yield mock_

    @staticmethod
    def _query_inner(object_):
        return object_[::-1]

    def test_query_has_parallels(self):
        def thread_inner(thread):
            self.controller.allow_query_to_run()
            with self.assertRaisesRegexp(watchdog.StopWatchdogException,
                                         'query is executing'):
                self._orig_do_check(thread)
            self.controller.allow_query_to_finish()

        with self._patch_thread(thread_inner), \
                self._patch_query(self._query_inner), \
                self._patch_get_parallels_count(16):

            w = watchdog.AcquireParallelsQueryWatchdog('dummy')
            r = w.execute('select /*+ parallel(16) */ 1')

        self.assertEqual(r, '1 /* )61(lellarap +*/ tceles')

    def _perform_no_parallels_test(self, parallels_count):
        def thread_inner(thread):
            self._orig_do_check(thread)

        def kill_func_inner(thread):
            self.controller.allow_query_to_run()
            self.controller.allow_query_to_finish()

        def query_inner(object_):
            self.controller.query_kill()

        with self._patch_query(query_inner), \
                self._patch_thread(thread_inner) as thread_mock, \
                self._patch_kill_func(kill_func_inner) as kill_func_mock, \
                self._patch_get_parallels_count(parallels_count):

            check_count = 3
            w = watchdog.AcquireParallelsQueryWatchdog('dummy', check_count=check_count)

            with self.assertRaises(watchdog.OutOfRetriesException):
                w.execute('select /*+ statement_queuing parallel(16) */ 1')

            self.assertEqual(thread_mock.call_count, check_count)
            self.assertEqual(kill_func_mock.call_count, 1)

    def test_query_has_no_parallels(self):
        self._perform_no_parallels_test(parallels_count=0)

    def test_query_has_one_parallel(self):
        self._perform_no_parallels_test(parallels_count=1)

    def _perform_parallel_hint_test(self, query, expected):
        w = watchdog.AcquireParallelsQueryWatchdog('dummy', check_count=1)
        r = w._should_we_watch(query)
        self.assertEqual(r, expected)

    def test_query_has_single_line_parallel_hint(self):
        queries = [
            """
              select --+ parallel(4)
                * from dual
            """,
            """
              select --+ parallel(dual 4)
                * from dual
            """,
            """
              select --+ statement_queuing parallel(4)
                * from dual
            """,
            """
              select --+ no_parallel parallel(4)
                * from dual
            """,
            """
              select --+ parallel statement_queuing
                * from dual
            """,
        ]

        for query in queries:
            self._perform_parallel_hint_test(query, expected=True)

    def test_query_has_multiline_parallel_hint(self):
        queries = [
            """
              select /*+ parallel(4) */ * from dual
            """,
            """
              select /*+ statement_queuing parallel(4) */ * from dual
            """,
            """
              select
                /*+
                  materialize
                  index(a)
                  parallel(4)
                */ *
              from dual
            """,
            sa_text('select /*+ parallel(4) */')
        ]
        for query in queries:
            self._perform_parallel_hint_test(query, expected=True)

    def test_query_has_no_parallel_hint(self):
        queries = [
            """
              select * from dual
            """,
            """
              select --+ no_parallel()
                * from dual
            """,
            """
              select --+ parallel_index(dual, index, 1)
                * from dual
            """,
            """
              select
                /*+
                  materialize
                  no_parallel
                */ *
              from dual
            """,
            """
              select -- parallel(4)
                * from dual
            """,
            """
              select /* parallel(4) */ * from dual
            """,
        ]
        for query in queries:
            self._perform_parallel_hint_test(query, expected=False)

# vim:ts=4:sts=4:sw=4:tw=79:et:

