import functools

import mock
from celery import Task, current_app
from django.test import TestCase
from django.utils import timezone
from freezegun import freeze_time

from plan.common.utils.tasks import lock_task
from plan.unistat.models import TaskMetric

patch_lock = functools.partial(
    mock.patch,
    target='plan.common.utils.locks.lock',
    side_effect=lambda *args, **kwargs: lambda x: x,
)


def bad_function(*args, **kwargs):
    assert False


def function(*args, **kwargs):
    return args, kwargs


class TaskDecoratorTests(TestCase):

    def tearDown(self):
        current_app.tasks.clear()

    def assert_common(self, result, lock_mock, lock_mock_called=True,
                      ignore_result=True):
        self.assertEqual(lock_mock.called, lock_mock_called)
        self.assertIsInstance(result, Task)
        self.assertEqual(result.name, '.'.join((self.__module__, 'function')))
        self.assertEqual(ignore_result, result.ignore_result)
        self.assertIs(result.original_func, function)
        self.assertTupleEqual(
            result(1, 2, a=3, b=4),
            ((1, 2), {'a': 3, 'b': 4}),
        )

    def test_default_shortcut(self):
        with patch_lock() as lock_mock:
            result = lock_task(function)
            self.assert_common(result, lock_mock)

    def test_no_args(self):
        with patch_lock() as lock_mock:
            result = lock_task()(function)
            self.assert_common(result, lock_mock)

    def test_verbose_lock(self):
        with patch_lock() as lock_mock:
            result = lock_task(lock=True)(function)
            self.assert_common(result, lock_mock)

    def test_verbose_ignore_result(self):
        with patch_lock() as lock_mock:
            result = lock_task(ignore_result=True)(function)
            self.assert_common(result, lock_mock)

    def test_verbose_no_ignore_result(self):
        with patch_lock() as lock_mock:
            result = lock_task(ignore_result=False)(function)
            self.assert_common(result, lock_mock, ignore_result=False)

    def test_verbose_nolock(self):
        with patch_lock() as lock_mock:
            result = lock_task(lock=False)(function)
            self.assert_common(result, lock_mock, lock_mock_called=False)

    def test_last_finished_success(self):
        with patch_lock(), freeze_time('2018-01-01'):
            result = lock_task(last_finished_metric_name='abc')(function)
            result()
            self.assertEqual(TaskMetric.objects.get().last_success_end, timezone.now())

    def test_last_finished_error(self):
        with patch_lock(), freeze_time('2018-01-01'):
            result = lock_task(last_finished_metric_name='abc')(bad_function)
            with self.assertRaises(AssertionError):
                result()
            self.assertFalse(TaskMetric.objects.exists())
