# -*- coding: utf-8 -*-
from unittest.mock import (
    patch,
    Mock,
    ANY,
)

from intranet.yandex_directory.src.yandex_directory.common.db import AlreadyLockedError, get_meta_connection
from intranet.yandex_directory.src.yandex_directory.core.scheduler.utils import (
    scheduled_job,
    scheduled_job_without_lock,
)
from intranet.yandex_directory.src.yandex_directory.core.models import ScheduledTasksResultModel

from testutils import TestCase, assert_not_called


class Test__scheduled_job(TestCase):
    def test__scheduled_job_should_get_lock_with_lock_name(self):
        trigger = 'interval'

        from intranet.yandex_directory.src.yandex_directory.core.scheduler.utils import lock as original_lock

        mocked_lock = Mock(side_effect=original_lock)
        mocked_scheduler = Mock()

        with patch('intranet.yandex_directory.src.yandex_directory.core.scheduler.utils.scheduler', mocked_scheduler):
            @scheduled_job(trigger=trigger)
            def some_worker():
                pass

        # проверяем, что задача ставится в scheduler
        self.assertEqual(mocked_scheduler.add_job.call_count, 1)

        # а при запуске задачи
        with patch('intranet.yandex_directory.src.yandex_directory.core.scheduler.utils.lock', mocked_lock):
            some_worker()

        # Должна взяться блокировка с именем some_worker + имя окружения
        mocked_lock.assert_called_with(ANY,'some_worker_autotests')
        with get_meta_connection() as meta_connection:
            assert ScheduledTasksResultModel(meta_connection).get_for_stats(1 * 60 * 60) == [('some_worker', True, 1)]

    def test__scheduled_job_should_not_run_func_if_get_lock_failed(self):
        # проверяем, что в случае, если лок взять не удалось, то
        # тело функции не выполнится и не поинкрементит счетчик

        # этот мок эмулирует ситуацию, что нам не удалось взять лок
        mocked_lock = Mock(side_effect=AlreadyLockedError)

        counter = [0]

        @scheduled_job()
        def another_locked_worker():
            counter[0] += 1

        with patch('intranet.yandex_directory.src.yandex_directory.core.scheduler.utils.lock', mocked_lock):
            another_locked_worker()

            # Если не получилось взять блокировку в базе - функция не
            # должна выполняться и счетчик должен остаться равным 0
            self.assertEqual(counter[0], 0)

    def test_scheduler_should_raise_error_if_two_tasks_with_same_name_was_added(self):
        @scheduled_job()
        def func_1():
            pass

        try:
            @scheduled_job()
            def func_1():
                pass
            raised = False
        except Exception:
            raised = True

        msg = 'При попытке добавить таск с уже существующим именем для лока, надо выдать ошибку'
        self.assertTrue(raised, msg=msg)


class Test__scheduled_job_without_lock(TestCase):
    def test__scheduled_job_without_lock_should_not_get_lock(self):
        # проверяем, что при использовании декоратора
        # @scheduled_job_without_lock мы не пытаемся взять lock.

        mocked_lock = Mock(return_value=True)
        mocked_scheduler = Mock()

        with patch('intranet.yandex_directory.src.yandex_directory.core.scheduler.utils.scheduler', mocked_scheduler):
            @scheduled_job_without_lock()
            def func_which_should_not_use_lock():
                pass

        self.assertEqual(mocked_scheduler.add_job.call_count, 1)

        with patch('intranet.yandex_directory.src.yandex_directory.core.scheduler.utils.lock', mocked_lock):
            func_which_should_not_use_lock()

        # scheduled_job_without_lock не должен брать блокировку в базе
        assert_not_called(mocked_lock)

        with get_meta_connection() as meta_connection:
            assert ScheduledTasksResultModel(meta_connection).get_for_stats(1 * 60 * 60) == []
