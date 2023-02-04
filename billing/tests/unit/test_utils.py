# coding: utf-8

import os
import time
import mock
import shutil
import random
import logging
import tempfile
import threading
from datetime import datetime
from operator import itemgetter

from billing.dcs.dcs.utils.common import Option, relative_date, LAST_DAY_OF_MONTH, \
    LazyMapping, all_sequential_subsequences, all_combinations, cut_string, \
    to_unicode, OptionParser, truncate_to_date, Period
from billing.dcs.dcs.utils.parallel import run_in_parallel, run_safe, _prepare_tasks, \
    TASK_RUN_RESULT_TYPES
from billing.dcs.dcs.utils.storage import save_file
from billing.dcs.dcs.utils.cli import report_error
from billing.dcs.dcs.exceptions import ArgumentsParsingError

from billing.dcs.tests import utils as test_utils


class UnitTestException(StandardError):
    pass


TEST_EXCEPTION = UnitTestException('Test exception')


def fail_func():
    raise TEST_EXCEPTION


def int_with_sleep(string):
    time.sleep(random.random() / 1000)
    return int(string)


class RunInParallelTestCase(test_utils.BaseTestCase):
    def prepare_thread_mock(self, thread_mock):
        """ Make thread instance's is_alive method return False. """
        thread_instance_mock = mock.Mock(spec=threading.Thread)
        thread_instance_mock.is_alive.return_value = False
        thread_mock.return_value = thread_instance_mock

    @mock.patch('billing.dcs.dcs.utils.parallel.threading.Thread', spec=threading.Thread)
    def test_no_threads_are_spawned(self, thread_mock):
        self.prepare_thread_mock(thread_mock)
        tasks = [(int, ('1', ))]
        results = run_in_parallel(tasks, pool_size=1)
        self.assertEqual(results, [(TASK_RUN_RESULT_TYPES.success, 1)])
        self.assertEqual(thread_mock.call_count, 0)

    @mock.patch('billing.dcs.dcs.utils.parallel.threading.Thread', spec=threading.Thread)
    def test_pool_spawning(self, thread_mock):
        self.prepare_thread_mock(thread_mock)
        tasks = [(int, ('1', ))] * 10
        run_in_parallel(tasks, pool_size=5)
        self.assertEqual(thread_mock.call_count, 5)

    @mock.patch('billing.dcs.dcs.utils.parallel.threading.Thread', spec=threading.Thread)
    def test_no_unnecessary_threads_spawning(self, thread_mock):
        self.prepare_thread_mock(thread_mock)
        tasks = [(int, ('1', ))] * 5
        run_in_parallel(tasks, pool_size=50)
        self.assertEqual(thread_mock.call_count, 5)

    def test_results_is_unordered(self):
        """
        Proof, that without save_ordering=True, test execution of
        run_in_parallel returns unordered results.
        """
        tasks_count = 25
        tasks = [(int_with_sleep, (str(x), )) for x in range(tasks_count)]
        results = run_in_parallel(tasks, pool_size=5, save_ordering=False)
        self.assertTrue(all((r[0] == TASK_RUN_RESULT_TYPES.success
                             for r in results)))
        result_values = [r[1] for r in results]
        self.assertNotEqual(result_values, range(tasks_count))
        self.assertEqual(sorted(result_values), range(tasks_count))

    def test_results_is_ordered(self):
        tasks_count = 25
        tasks = [(int_with_sleep, (str(x), )) for x in range(tasks_count)]
        results = run_in_parallel(tasks, pool_size=5)
        self.assertTrue(all((r[0] == TASK_RUN_RESULT_TYPES.success
                             for r in results)))
        self.assertEqual([r[1] for r in results], range(tasks_count))

    def test_fail_fast(self):
        tasks = [(int, ('1', )),
                 (fail_func, ),
                 (int, ('1', ))]

        results = run_in_parallel(tasks, pool_size=1)
        self.assertEqual(len(results), 2)

        # check first result
        self.assertEqual(results[0], (TASK_RUN_RESULT_TYPES.success, 1))

        # check second result
        result_type, result_value = results[1]
        self.assertEqual(result_type, TASK_RUN_RESULT_TYPES.failure)
        self.assertEqual(result_value[1], TEST_EXCEPTION)

    @mock.patch('billing.dcs.dcs.utils.parallel._prepare_tasks')
    def test_tasks_are_prepared(self, prepare_tasks_mock):
        tasks = [(int, ('1', ))] * 5
        error_handler = object()
        run_in_parallel(tasks, error_handler=error_handler)
        self.assertEqual(prepare_tasks_mock.call_count, 1)
        self.assertEqual(prepare_tasks_mock.call_args,
                         mock.call(tasks, error_handler))

    def test_empty_tasks_list(self):
        self.assertListEqual(run_in_parallel([]), [])


class RunSafeTestCase(test_utils.BaseTestCase):
    def test_success(self):
        def func():
            return 1
        self.assertEqual(run_safe(func)(), (TASK_RUN_RESULT_TYPES.success, 1))

    def test_failure(self):
        result_type, result_value = run_safe(fail_func)()
        self.assertEqual(result_type, TASK_RUN_RESULT_TYPES.failure)
        self.assertEqual(result_value[1], TEST_EXCEPTION)

    def test_low_level_failure(self):
        def func():
            raise SystemExit()
        self.assertRaises(SystemExit, run_safe(func))

    @mock.patch('billing.dcs.dcs.utils.parallel.sys.exc_info')
    def test_error_handler_is_called(self, exc_info_mock):
        exc_info = object()
        exc_info_mock.return_value = exc_info
        error_handler = mock.Mock()
        run_safe(fail_func, error_handler)()
        self.assertEqual(error_handler.call_count, 1)
        self.assertEqual(error_handler.call_args, mock.call(exc_info))

    def test_error_handler_exception_ignored(self):
        error_handler = mock.Mock(side_effect=TEST_EXCEPTION)
        try:
            run_safe(fail_func, error_handler)()
        except UnitTestException:
            self.fail('Exception must have been caught in run_safe.')


class PrepareTasksTestCase(test_utils.BaseTestCase):
    @mock.patch('billing.dcs.dcs.utils.parallel.run_safe')
    def test_wrapping(self, run_safe_mock):
        expected = object()
        run_safe_mock.return_value = expected
        tasks = [(int, )]
        error_handler = object()
        prepared_tasks = _prepare_tasks(tasks, error_handler)
        task_num, func, args, kwargs = prepared_tasks[0]
        self.assertEqual(run_safe_mock.call_count, 1)
        self.assertEqual(run_safe_mock.call_args, mock.call(int, error_handler))
        self.assertEqual(func, expected)

    def test_passed_args(self):
        passed_args = (1, )
        tasks = [(int, passed_args)]
        prepared_tasks = _prepare_tasks(tasks)
        task_num, func, args, kwargs = prepared_tasks[0]
        self.assertEqual(args, passed_args)

    def test_passed_kwargs(self):
        passed_kwargs = {1: 1}
        tasks = [(int, tuple(), passed_kwargs)]
        prepared_tasks = _prepare_tasks(tasks)
        task_num, func, args, kwargs = prepared_tasks[0]
        self.assertEqual(kwargs, passed_kwargs)

    def test_no_args(self):
        tasks = [(int, )]
        prepared_tasks = _prepare_tasks(tasks)
        task_num, func, args, kwargs = prepared_tasks[0]
        self.assertEqual(args, tuple())

    def test_no_kwargs(self):
        tasks = [(int, (1, ))]
        prepared_tasks = _prepare_tasks(tasks)
        task_num, func, args, kwargs = prepared_tasks[0]
        self.assertEqual(kwargs, dict())

    def test_tasks_enumeration(self):
        tasks = [(int, )] * 10
        prepared_tasks = _prepare_tasks(tasks)
        self.assertEqual(map(itemgetter(0), prepared_tasks),
                         range(len(tasks)))


class CutStringTestCase(test_utils.BaseTestCase):
    def test_cut(self):
        test_string = 'head' + 'a' * 100 + 'tail'
        self.assertEqual(
            cut_string(test_string, head=4, tail=4, glue='.'),
            'head' + '.' + 'tail'
        )

    def test_no_cut(self):
        self.assertEqual(
            cut_string('a' * 20, head=10, tail=10, glue='.'),
            'a' * 20
        )

    def test_empty_string(self):
        self.assertEqual(cut_string('', glue='.'), '')


class ToUnicodeTestCase(test_utils.BaseTestCase):
    def test_obj_is_unicode(self):
        self.assertEqual(to_unicode(u'Привет'), u'Привет')

    def test_utf8_to_unicode_conversion(self):
        self.assertEqual(to_unicode('Привет'), u'Привет')

    def test_plain_ascii_to_unicode_conversion(self):
        self.assertEqual(to_unicode('hello'), u'hello')

    def test_object_with_unicode_method(self):
        class T1(object):
            def __str__(self):
                return 'bad'

            def __unicode__(self):
                return u'good'

        self.assertEqual(to_unicode(T1()), u'good')

    def test_object_str_method_returns_unicode(self):
        """ SQLAlchemy expressions compiler returns such objects """

        class T1(object):
            def __str__(self):
                return u'ай яй яй'

        self.assertEqual(to_unicode(T1()), u'ай яй яй')

    def test_object_with_erroneous_unicode_method(self):
        self.assertEqual(to_unicode(ValueError('Привет')), u'Привет')

    def test_obj_is_none(self):
        self.assertEqual(to_unicode(None), 'None')


class RelativeDateTestCase(test_utils.BaseTestCase):
    def parse_string(self, date_string):
        return OptionParser(
            option_list=[Option('--dt', type='date')]
        ).parse_args(['--dt', date_string])[0].dt

    def test_relative_date(self):
        self.assertEqual(relative_date(datetime(2017, 3, 31), months=-1),
                         datetime(2017, 2, 28))
        self.assertEqual(relative_date(datetime(2017, 1, 31), months=+1),
                         datetime(2017, 2, 28))
        self.assertEqual(relative_date(datetime(2017, 2, 28), months=+1),
                         datetime(2017, 3, 28))
        self.assertEqual(relative_date(datetime(2017, 2, 28), months=+1,
                                       day=LAST_DAY_OF_MONTH),
                         datetime(2017, 3, 31))

    def test_option_parser(self):
        self.assertEquals(self.parse_string('relative(years=+1)'),
                          relative_date(years=+1))
        self.assertEquals(self.parse_string('relative(years=1)'),
                          relative_date(years=1))
        self.assertEquals(self.parse_string('relative(years=-1)'),
                          relative_date(years=-1))

        self.assertEquals(self.parse_string('relative(hour=10)'),
                          relative_date(hour=10))

        self.assertEquals(self.parse_string('relative(hour=10, years=-1)'),
                          relative_date(hour=10, years=-1))

        self.assertEquals(self.parse_string('relative(month=2, day=32)'),
                          relative_date(month=2, day=LAST_DAY_OF_MONTH))

        with mock.patch('billing.dcs.dcs.utils.common.add_workdays') as add_workdays_mock:
            self.parse_string('relative(day=1, months=+1)')
            self.assertEqual(add_workdays_mock.call_count, 0)

            add_workdays_mock.return_value = relative_date(days=-100)
            self.assertEquals(
                self.parse_string('relative(day=1, months=+1, workdays=-2)'),
                add_workdays_mock.return_value
            )
            self.check_mock_calls(add_workdays_mock, [mock.call(
                relative_date(day=1, months=+1), -2
            )])

    def test_exceptions(self):
        self.assertRaises(
            ArgumentsParsingError, self.parse_string, 'relative(dummy=+1)'
        )
        self.assertRaises(
            ArgumentsParsingError, self.parse_string, 'relative(year=xxx)'
        )
        self.assertRaises(
            ArgumentsParsingError, self.parse_string, 'dummy(year=+1)'
        )


class SimpleTestCase(test_utils.BaseTestCase):
    """
    Класс для сущностей, которым достаточно 1-2 простых тестов.
    """
    def test_lazy_mapping(self):
        dict_ = {str(i): i for i in range(10)}
        construct_mapping_func = mock.Mock(return_value=dict_)
        my_lazy_mapping = LazyMapping(construct_mapping_func)
        for key, value in dict_.iteritems():
            self.assertEqual(my_lazy_mapping[key], value)
            self.assertEqual(getattr(my_lazy_mapping, key), value)

        self.assertIsNone(my_lazy_mapping.get('test'))

        self.assertListEqual(dict_.keys(), my_lazy_mapping.keys())
        self.assertListEqual(dict_.values(), my_lazy_mapping.values())
        self.assertListEqual(dict_.items(), my_lazy_mapping.items())

        self.assertListEqual(dict_.keys(), list(my_lazy_mapping.iterkeys()))
        self.assertListEqual(dict_.values(), list(my_lazy_mapping.itervalues()))
        self.assertListEqual(dict_.items(), list(my_lazy_mapping.iteritems()))

        self.assertEqual(construct_mapping_func.call_count, 1)

    def test_all_sequential_subsequences(self):
        list_to_string = lambda s: ''.join(s)
        self.assertListEqual(
            sorted(map(list_to_string, all_sequential_subsequences(list('abc')))),
            sorted(['a', 'ab', 'abc', 'b', 'bc', 'c'])
        )
        self.assertListEqual(
            list(all_sequential_subsequences([])), []
        )
        self.assertListEqual(
            list(map(list, all_sequential_subsequences([1]))), [[1]]
        )

    def test_all_combinations(self):
        self.assertListEqual(all_combinations([]), [])
        self.assertListEqual(all_combinations([1]), [1])
        self.assertListEqual(
            sorted(all_combinations(['a', 'b', 'c'])),
            sorted(['a', 'ab', 'b', 'ac', 'abc', 'bc', 'c'])
        )

    @mock.patch('billing.dcs.dcs.utils.storage.settings')
    def test_storage(self, settings):
        test_dir = os.path.join(tempfile.gettempdir(),
                                'dcs_test_fake_fetched_data_dir')
        if os.path.exists(test_dir):
            shutil.rmtree(test_dir)
        os.mkdir(test_dir)
        settings.FETCHED_DATA_DIR = test_dir

        file_dt = datetime(2015, 1, 1)
        file_name = 'name'
        file_data = 'data'

        settings.STORAGE_CREATE_FILE_ATTEMPTS_COUNT = 1
        # Сохраняем первый раз, все должно быть ок
        save_file(file_dt, file_name, file_data)

        # Количество попыток - одна, должны упасть,
        # потому что файл уже существует
        self.assertRaises(AssertionError, save_file,
                          file_dt, file_name, file_data)

        # Параметр compress не должен влиять на поведение
        self.assertRaises(AssertionError, save_file,
                          file_dt, file_name, file_data, compress=False)

        # Увеличили количество попыток, ошибки быть не должно
        settings.STORAGE_CREATE_FILE_ATTEMPTS_COUNT = 2
        save_file(file_dt, file_name, file_data)

        # Пробуем записать входящие данные по частям
        settings.STORAGE_CREATE_FILE_ATTEMPTS_COUNT = 6
        file_data = '123456789'
        # Увы, мы не можем проверить, что записалось, поэтому просто проверяем
        # на возможные ошибки

        # Разбиваем на части по 2 символа
        settings.FETCHED_DATA_CHUNK_SIZE = 2
        save_file(file_dt, file_name, file_data, compress=False)
        save_file(file_dt, file_name, file_data, compress=True)

        # Не разбиваем на части
        settings.FETCHED_DATA_CHUNK_SIZE = 1 * 1024
        save_file(file_dt, file_name, file_data, compress=False)
        save_file(file_dt, file_name, file_data, compress=True)

    def test_truncate_to_date(self):
        self.assertEqual(
            truncate_to_date(datetime(2016, 1, 1, 1, 1, 1, 1)),
            datetime(2016, 1, 1)
        )

        self.assertEqual(
            truncate_to_date(datetime(2016, 1, 1)),
            datetime(2016, 1, 1)
        )

    def test_period(self):
        self.assertEqual(
            Period(datetime(2017, 8, 1), datetime(2017, 8, 3)).delta.days, 2
        )
        self.assertListEqual(
            Period(datetime(2017, 8, 1), datetime(2017, 8, 3)).days,
            [datetime(2017, 8, 1), datetime(2017, 8, 2), datetime(2017, 8, 3)]
        )


@mock.patch('billing.dcs.dcs.utils.cli.send_error_report')
class ReportErrorTestCase(test_utils.BaseTestCase):
    secret_value = 666
    subject = u'Все пропало !'

    class MyException(Exception):
        pass

    @staticmethod
    @report_error(subject)
    def failing_decorated_function():
        raise ReportErrorTestCase.MyException()

    @staticmethod
    @report_error(subject)
    def passing_decorated_function():
        return

    def test_no_error(self, send_mail_mock):
        self.passing_decorated_function()
        self.assertEqual(send_mail_mock.call_count, 0)

    def test_error_raised(self, send_mail_mock):
        with self.assertRaises(self.MyException), \
                test_utils.change_logging_level('yandex-dcs', logging.CRITICAL):
            self.failing_decorated_function()
        self.assertEqual(send_mail_mock.call_count, 1)
        call_args, call_kwargs = send_mail_mock.call_args
        subject, exc_info, args, additional_emails, executable_string = call_args
        self.assertEqual(subject, self.subject)
        self.assertEqual(exc_info[0], self.MyException)
        self.assertIsNone(args)
        self.assertIsNone(additional_emails)
        self.assertIsNone(executable_string)
