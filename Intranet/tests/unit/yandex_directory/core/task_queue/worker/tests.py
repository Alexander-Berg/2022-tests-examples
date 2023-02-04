# -*- coding: utf-8 -*-
from datetime import timedelta
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    none,
    not_none,
    close_to,
)

from testutils import (
    TestCase,
    override_settings,
)

from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    get_exponential_step,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    TaskRelationsModel,
    OrganizationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    Task,
    TASK_STATES,
    get_default_queue,
    AsyncResult,
)

from intranet.yandex_directory.src.yandex_directory.core.task_queue.worker import TaskProcessor
from intranet.yandex_directory.src.yandex_directory.core.utils import only_attrs


class SimpleTask(Task):
    need_rollback = False
    org_id_is_required = False

    def do(self, **kwargs):
        return 'success'

test_metadata = {'my_metadata': 'value'}


class WithMetadataTask(Task):
    need_rollback = False

    def do(self, **kwargs):
        self.set_metadata(test_metadata)
        return 'success'


class RetryTask(SimpleTask):
    need_rollback = False

    retry_at = utcnow() + timedelta(seconds=1000)

    def do(self,  **kwargs):
        self.defer(retry_at=self.retry_at)


class ExponentialRetryTask(SimpleTask):
    def do(self,  **kwargs):
        self.exponential_defer(
            min_interval=10,
            max_interval=60,
            const_time=5 * 60,
        )


class FailedTask(SimpleTask):
    def do(self,  **kwargs):
        return 1 / 0


class FailedWithSuccessRollbackTask(FailedTask):
    need_rollback = True
    tries = 1
    rollback_tries = 1

    def rollback(self, **kwargs):
        pass


class FailedWithFailedRollbackTask(FailedWithSuccessRollbackTask):
    def rollback(self, **kwargs):
        return 1 / 0


class FailUnpickledTask(SimpleTask):
    need_rollback = False
    trie = 1

    def do(self,  **kwargs):
        raise Exception(self.main_connection)


class DependentTask(Task):
    org_id_is_required = False

    def on_dependency_success(self):
        deps = only_attrs(
            TaskRelationsModel(self.main_connection) \
                .filter(task_id=self.task_id) \
                .fields('dependency_task_id'),
            'dependency_task_id'
        )
        ready = True
        for d in deps:
            state = TaskModel(self.main_connection).get(d)['state']
            if state != TASK_STATES.success:
                ready = False
                break
        if ready:
            self.resume()

    def do(self, **kwargs):
        return "dependent success"


class ChangeOrgNameAndFailTask(Task):
    def do(self):
        OrganizationModel(self.main_connection) \
            .filter() \
            .update(name='CHANGED')
        raise RuntimeError('Some shit happened. As always.')


class TestProcess(TestCase):

    def setUp(self):
        super(TestProcess, self).setUp()
        # создаем задачу в прошлом, потому что время в базе может отставать
        self.task_id = SimpleTask(self.main_connection).delay(start_in=timedelta(seconds=-5)).task_id
        # захват задачи
        self.locked_task_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())

    def test_success_process_task(self):
        # выполнили задачу с первого раза
        TaskProcessor(SimpleTask, self.locked_task_data).process(self.main_connection)

        async_result = AsyncResult(self.main_connection, self.task_id)

        assert_that(
            async_result.get_result(),
            equal_to('success')
        )
        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.success,
                finished_at=not_none(),
                result=not_none(),
                exception=none(),
                tries=1,
                rollback_tries=0,
            )
        )

    def test_fail_process_task(self):
        # при выполении задачи возникла ошибка
        # повторим ее попозже

        TaskProcessor(FailedTask, self.locked_task_data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.in_progress,
                finished_at=none(),
                worker=none(),
                result=none(),
                exception=not_none(),
                start_at=not_none(),
                tries=1,
                rollback_tries=0,
            )
        )

    def test_all_retry_no_rollback(self):
        # все попытки неудачны у задачи нет функции отката

        # ранее было n-1 неудачных попыток выполнениея задачи
        data = self.locked_task_data.copy()
        data['tries'] = FailedTask.tries - 1
        TaskProcessor(FailedTask, data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.failed,
                finished_at=not_none(),
                result=none(),
                exception=not_none(),
                tries=FailedTask.tries,
                rollback_tries=0,
            )
        )

    def test_with_success_rollback(self):
        # удачно откатываем задачу с функцией отката измененеий
        TaskProcessor(FailedWithSuccessRollbackTask, self.locked_task_data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.in_progress,
                finished_at=none(),
                result=none(),
                exception=not_none(),
                tries=FailedWithSuccessRollbackTask.tries,
                rollback_tries=0,
            )
        )

        data = self.locked_task_data.copy()
        data['tries'] = FailedWithSuccessRollbackTask.tries

        TaskProcessor(FailedWithSuccessRollbackTask, data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.rollback,
                finished_at=not_none(),
                result=none(),
                exception=not_none(),
                tries=FailedWithSuccessRollbackTask.tries,
                rollback_tries=1,
            )
        )

    def test_with_failed_rollback(self):
        # откат задачи завершился ошибкой
        TaskProcessor(FailedWithFailedRollbackTask, self.locked_task_data).process(self.main_connection)
        data = self.locked_task_data.copy()
        data['tries'] = FailedWithSuccessRollbackTask.tries
        TaskProcessor(FailedWithFailedRollbackTask, data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.rollback_failed,
                finished_at=not_none(),
                result=none(),
                exception=not_none(),
                tries=FailedWithFailedRollbackTask.tries,
                rollback_tries=FailedWithFailedRollbackTask.rollback_tries,
            )
        )

    def test_defer_task(self):
        # обрабатываем исключение Defer, откладывая задачу на выполнение
        TaskProcessor(RetryTask, self.locked_task_data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                state=TASK_STATES.free,
                finished_at=none(),
                result=none(),
                exception=none(),
                tries=0,
                start_at=RetryTask.retry_at,
            )
        )

    def test_exponential_defer_task(self):
        # обрабатываем исключение Defer, откладывая задачу на выполнение
        TaskProcessor(ExponentialRetryTask, self.locked_task_data).process(self.main_connection)
        # таск должен снова перейти в состояние free
        self.assert_no_failed_tasks(allowed_states=['free'])

        new_state = TaskModel(self.main_connection).get(self.task_id)
        created_at = new_state['created_at']
        start_at = new_state['start_at']
        delay = (start_at - created_at).total_seconds()

        # Смотрим, что был выбран минимальный экспоненциальный интервал
        assert_that(
            delay,
            close_to(10, 0.5),
        )

    def test_exponential_steps(self):
        # Проверяем, как работает выбор экспоненциального шага

        # Минимальный интервал должен быть около минуты
        min_interval = 60
        # Максимальный час
        max_interval = 60 * 60
        # По прошествии суток - должен всегда выдаваться максимальный интервал
        const_time = 24 * 60 * 60
        # допустимая погрешность
        alpha = 0.5

        def get(time_since_start):
            return get_exponential_step(
                time_since_start,
                min_interval,
                max_interval,
                const_time,
            )

        # В самом начале, шаг должен быть минимальным
        assert_that(
            get(0),
            close_to(min_interval, alpha)
        )
        # В начале пути
        assert_that(
            get(const_time * 0.05),
            close_to(73.6, alpha)
        )
        assert_that(
            get(const_time * 0.1),
            close_to(90.3, alpha)
        )
        # ближе к концу кривой
        assert_that(
            get(const_time * 0.6),
            close_to(699.9, alpha)
        )
        assert_that(
            get(const_time * 0.9),
            close_to(2390.4, alpha)
        )
        assert_that(
            get(const_time * 0.99),
            close_to(3455.5, alpha)
        )
        # на плоском отрезке
        assert_that(
            get(const_time * 1.05),
            close_to(max_interval, alpha)
        )


    def test_task_with_metadata(self):
        # сохраним при вполнении задачи метаданные
        TaskProcessor(WithMetadataTask, self.locked_task_data).process(self.main_connection)

        assert_that(
            WithMetadataTask(self.main_connection, task_id=self.task_id).get_metadata(),
            equal_to(test_metadata)
        )

    def test_unpickled_error(self):
        TaskProcessor(FailUnpickledTask, self.locked_task_data).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.task_id),
            has_entries(
                traceback=not_none(),
                exception=none(),
            ),
        )

    def test_transaction_rollback(self):
        # Проверим, что если таск изменит данные через main_connection,
        # а потом выбросит исключение, то эти изменения откатятся, но трейс
        # в данных таска сохранится.
        TaskProcessor(ChangeOrgNameAndFailTask, self.locked_task_data).process(self.main_connection)

        org = OrganizationModel(self.main_connection).get(self.organization['id'])

        assert org['name'] != 'CHANGED'

        task = TaskModel(self.main_connection).get(self.task_id)

        assert_that(
            task,
            has_entries(
                traceback=not_none(),
                exception=not_none(),
            ),
        )


class TestProcessDependency(TestCase):

    def setUp(self):
        super(TestProcessDependency, self).setUp()

        self.dependencies = [
            SimpleTask(self.main_connection).delay(
                start_in=timedelta(seconds=-5),
                dependency_1=True).task_id,
            SimpleTask(self.main_connection).delay(
                start_in=timedelta(seconds=-5),
                dependency_2=True).task_id
        ]

        self.dependent_id = DependentTask(self.main_connection, depends_on=self.dependencies).\
            delay(
            start_in=timedelta(seconds=-5),
            dependent=True,
        ).task_id

    def test_dependent_task(self):
        # Создаем 2 таска. Создаем третий таск, зависящий от них.
        # Зависяий таск переходит в статус free, если все его зависимости завершились успешно.
        # Два раза вызываем обработчик для тасков-зависимостей.
        # Проверяем, что зависимый таск перешел в статус free.
        # Вызываем обработчки для зависимого таска. Проверяем, что таск завершился успешно.

        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        if locked_data:
            TaskProcessor(
                SimpleTask,
                locked_data,
            ).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.dependent_id),
            has_entries(
                state=TASK_STATES.suspended,
                finished_at=none(),
                result=none(),
                exception=none(),
                tries=0,
            )
        )

        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        if locked_data:
            TaskProcessor(
                SimpleTask,
                locked_data,
            ).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.dependent_id),
            has_entries(
                state=TASK_STATES.free,
                finished_at=none(),
                result=none(),
                exception=none(),
                tries=0,
            )
        )

        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        if locked_data:
            TaskProcessor(
                DependentTask,
                locked_data,
            ).process(self.main_connection)

        assert_that(
            TaskModel(self.main_connection).get(self.dependent_id),
            has_entries(
                state=TASK_STATES.success,
                finished_at=not_none(),
                result=not_none(),
                exception=none(),
                tries=1,
            )
        )
