# -*- coding: utf-8 -*-
import datetime
from hamcrest import (
    assert_that,
    contains_inanyorder,
    has_entries,
    has_length,
    none,
    instance_of,
    calling,
    raises,
    is_not,
    contains,
    has_properties,
    equal_to,
)

from testutils import (
    frozen_time,
    TestCase,
    override_settings,
)

from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    SENSITIVE_DATA_PLACEHOLDER,
)
from intranet.yandex_directory.src.yandex_directory.core.models import TaskModel
from intranet.yandex_directory.src.yandex_directory.core.utils import deferred
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    Task,
    AsyncResult,
    TASK_STATES
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import DuplicatedTask
from intranet.yandex_directory.src.yandex_directory import app


class SimpleTask(Task):
    need_rollback = False
    org_id_is_required = False

    def do(self, **kwargs):
        return 'success'


test_metadata = {'test': ['metadata']}


class WithMetadataTask(Task):
    need_rollback = False
    org_id_is_required = False

    def do(self, **kwargs):
        self.set_metadata(test_metadata)
        assert test_metadata == self.get_metadata()
        return 'success'


class TaskWithSensData(Task):
    sensitive_params = ['password']
    org_id_is_required = False

    def do(self, **kwargs):
        return 'success'


class TestTask(TestCase):
    def setUp(self):
        super(TestTask, self).setUp()
        self.task_queue = 'autotest-queue'
        self.task = SimpleTask(self.main_connection, self.task_queue)

    def test_delay(self):
        # создание отложенной задачи
        kwargs = {
            'a': 'a',
            'date': str(utcnow().date()),
            'datetime': str(utcnow()),
        }
        result = self.task.delay(**kwargs)

        assert_that(
            result,
            instance_of(AsyncResult)
        )
        assert_that(
            TaskModel(self.main_connection).get(result.task_id),
            has_entries(
                params=has_entries(**kwargs),
                state=TASK_STATES.free,
                queue=self.task_queue,
                worker=none(),
                tries=0,  # не было попыток запуска
                rollback_tries=0,  # не было попыток отката
            )
        )

    def test_delay_with_time(self):
        # создание отложенной на определенное время задачи
        kwargs = {
            'a': 'a',
            'date': str(utcnow().date()),
            'datetime': str(utcnow()),
            'start_in': datetime.timedelta(minutes=10),
        }

        with frozen_time():
            result = self.task.delay(**kwargs)
            del kwargs['start_in']      # потому что json.dumps плохо обрабатывает timedelta
            assert_that(
                result,
                instance_of(AsyncResult)
            )
            assert_that(
                TaskModel(self.main_connection).get(result.task_id),
                has_entries(
                    params=has_entries(**kwargs),
                    state=TASK_STATES.free,
                    queue=self.task_queue,
                    worker=none(),
                    tries=0,  # не было попыток запуска
                    rollback_tries=0,  # не было попыток отката
                    start_at=utcnow() + datetime.timedelta(minutes=10),
                )
            )

    def test_duplicate_task(self):
        kwargs = {'a': 1, 'b': {'c': [1, 2, 3]}}

        self.task.delay(**kwargs)

        assert_that(
            calling(self.task.delay).with_args(**kwargs),
            raises(DuplicatedTask)
        )

    def test_should_not_raise_duplicate_task_with_different_queues(self):
        # если таски в разных очередях, не нужно вызывать DuplicatedTask
        kwargs = {'a': 1, 'b': {'c': [1, 2, 3]}}

        first_task = SimpleTask(self.main_connection, 'first_queue')
        second_task = SimpleTask(self.main_connection, 'second_queue')

        first_task.delay(**kwargs)

        assert_that(
            calling(second_task.delay).with_args(**kwargs),
            is_not(raises(DuplicatedTask))
        )

    def test_set_parent_task(self):
        # при создании задачи указываем id родительской задачи
        parent_task = self.task.delay(parent=True)
        child_task = SimpleTask(self.main_connection, self.task_queue, parent_task_id=parent_task.task_id).delay(child=True)

        assert_that(
            TaskModel(self.main_connection).get(child_task.task_id),
            has_entries(
                parent_task_id=parent_task.task_id
            )
        )

        # у родительской задачи получаем список ее детей
        child_tasks_results = SimpleTask(self.main_connection, task_id=parent_task.task_id).get_child_tasks_results()
        assert_that(
            child_tasks_results,
            contains(
                has_properties(task_id=child_task.task_id)
            )
        )

    def test_get_metadata(self):
        # получаем метаданные для выполненной задачи
        result = WithMetadataTask(self.main_connection).delay()
        self.process_tasks()
        assert_that(
            result.get_metadata(),
            equal_to(test_metadata)
        )

    def test_suspended(self):
        # приостановим задачу

        # ставим задачу в очередь
        self.task.delay()
        self.task.suspend()

        assert_that(
            TaskModel(self.main_connection).get(self.task.task_id),
            has_entries(
                state=TASK_STATES.suspended,
            )
        )

    def test_suspend_finished_task(self):
        # попробуем приостановить завершенную задачу
        self.task.delay()
        self.process_tasks()
        self.task.suspend()

        # завершенную задачу приостановить нельзя
        assert_that(
            TaskModel(self.main_connection).get(self.task.task_id),
            has_entries(
                state=TASK_STATES.success
            )
        )

    def test_resume(self):
        # возобновим приостановленную задачу

        # ставим задачу в очередь
        self.task.delay()

        self.task.suspend()
        self.task.resume()
        assert_that(
            TaskModel(self.main_connection).get(self.task.task_id),
            has_entries(
                state=TASK_STATES.free,
            )
        )

    def test_resume_finished_task(self):
        # попытаемся возобновить завершенную задачу
        self.task.delay()
        self.process_tasks()
        self.task.resume()
        # завершенную задачу возобновить нельзя
        assert_that(
            TaskModel(self.main_connection).get(self.task.task_id),
            has_entries(
                state=TASK_STATES.success,
            )
        )

    def test_task_with_dependencies(self):
        # Создаем task1 и task2. Создаем dependent_task, зависящий от них.
        # Проверяем, что dependent_task в находится в статусе susspended.
        # Проверяем, что функция get_dependencies для dependent_task
        # возвращает task1 и task2

        task1 = self.task.delay(dependency_1=True)
        task2 = self.task.delay(dependency_2=True)

        dependent_task = SimpleTask(
            self.main_connection,
            self.task_queue,
            depends_on=[task1.task_id, task2.task_id],
        ).delay(dependent=True)

        assert_that(
            TaskModel(self.main_connection).get(dependent_task.task_id),
            has_entries(state=TASK_STATES.suspended),
        )

        dependencies = SimpleTask(
            self.main_connection,
            self.task_queue,
        ).get_dependencies(task_id=dependent_task.task_id)

        assert_that(
            dependencies,
            has_length(2)
        )
        expected = [task1.task_id, task2.task_id]
        assert_that(
            [dep.task_id for dep in dependencies],
            contains_inanyorder(*expected)
        )

    def test_all_depends_completed_false(self):
        # Создаем таск task_1, а затем task_2 и task_3 зависящий от первого, но незаконченные
        # Проверем, что таски которые зависят от 1-го незаконченны

        TaskModel(self.main_connection).delete(force_remove_all=True)

        create_params = {
            'task_name': 'test_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        task_1 = TaskModel(self.main_connection).create(**create_params)
        task_2 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        task_3 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)

        assert(SimpleTask(self.main_connection, task_id=task_1['id']).is_all_dependents_completed() == False)

    def test_all_depends_completed_true(self):
        # Создаем таск task_1, а затем task_2 и task_3 зависящий от первого и закончены
        # Проверем, что таски которые зависят от 1-го закончены

        TaskModel(self.main_connection).delete(force_remove_all=True)

        create_params = {
            'task_name': 'test_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        task_1 = TaskModel(self.main_connection).create(**create_params)
        task_2 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        task_3 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        TaskModel(self.main_connection).update_one(
            task_2['id'], {'state':'success'})
        TaskModel(self.main_connection).update_one(
            task_3['id'], {'state': 'failed'})
        assert(SimpleTask(self.main_connection, task_id=task_1['id']).is_all_dependents_completed() == True)

    def test_all_depends_completed_not_all_false(self):
        # Создаем таск task_1, а затем task_2 и task_3 зависящий от первого, но один незаконченн
        # Проверем, что таски которые зависят от 1-го незаконченны

        TaskModel(self.main_connection).delete(force_remove_all=True)

        create_params = {
            'task_name': 'test_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        task_1 = TaskModel(self.main_connection).create(**create_params)
        task_2 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        task_3 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        TaskModel(self.main_connection).update_one(
            task_3['id'], {'state': 'failed'})
        assert(SimpleTask(self.main_connection, task_id=task_1['id']).is_all_dependents_completed() == False)

    def test_clean_private_data_in_task_on_terminate_metadata(self):

        # Создаем 4 задачи, где 1-я - зависит от 4, а 2 и 3 от первой
        # __
        # Когда завершается 4-я задача для нее запускается финальная функция on terminate
        # она не должна поменять мета данные, так как от 4-ой зависит незавершенная 1-я
        # __
        # Когда завершается 1-я задача для нее запускается финальная функция on terminate
        # она не должна поменять мета данные, так как от 1-ой зависит незавершенные 2-я и 3-я
        # __
        # Когда завершается 2-я задача для нее запускается финальная функция on terminate
        # должно скрыться значение полей с password только в ней, так от у 1-й зависит незавершенная 3-я
        # __
        # Когда завершается 3-я задача для нее запускается финальная функция on terminate
        # Должно скрыться значение поля password в 1 3 и 4 тасках

        tm = TaskModel(self.main_connection)
        tm.delete(force_remove_all=True)
        create_params = {
            'task_name': 'test_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        task_4 = tm.create(**create_params)
        task_1 = tm.create(depends_on=[task_4['id']], **create_params)
        task_2 = tm.create(depends_on=[task_1['id']], **create_params)
        task_3 = tm.create(depends_on=[task_1['id']], **create_params)
        meta_data = [{'ll': 'kek', 'ter': [{'kek': 'newpassword'}, {"oldpassword": "mem", "oldpasswordnew": 'kek', 'jin': [{"password": "kek", 6: 1, "fds": 5}]}, {}]},1]
        hidded_meta_data = ([{'ll': 'kek', 'ter': [{'kek': 'newpassword'}, {'jin': [{'fds': 5, 'password': SENSITIVE_DATA_PLACEHOLDER, 6: 1}], 'oldpassword': SENSITIVE_DATA_PLACEHOLDER, 'oldpasswordnew': SENSITIVE_DATA_PLACEHOLDER}, {}]}, 1])

        tasks = [
            task_1,
            task_2,
            task_3,
            task_4,
        ]

        def set_metadata(task, data):
            """Вспомогательная утилита, которая заодно реально применяет изменения к базе."""
            task = SimpleTask(self.main_connection, task_id=task['id'])
            task.set_metadata(data)

        def get_metadata(task):
            """Вспомогательная функция, чтобы код теста был читаемее."""

            # Тут на всякий случай убеждаемся, что данные в базу сохранились,
            # потому что сам по себе метод on_terminate этого не делает,
            # сохранение происходит в самом конце метода TaskProcessor.process.
            task = SimpleTask(self.main_connection, task_id=task['id'])
            return task.get_metadata()

        # Установим метаданные в изначальное положение
        with deferred.calls_at_the_end():
            for task in tasks:
                set_metadata(task, meta_data)

        tm.update_one(task_4['id'], {'state': 'success'})
        with deferred.calls_at_the_end():
            SimpleTask(self.main_connection, task_id=task_4['id']).on_terminate()
        assert(get_metadata(task_4) == meta_data)

        tm.update_one(task_1['id'], {'state': 'success'})

        with deferred.calls_at_the_end():
            SimpleTask(self.main_connection, task_id=task_1['id']).on_terminate()

        assert(get_metadata(task_1) == meta_data)

        tm.update_one(task_2['id'], {'state': 'success'})

        with deferred.calls_at_the_end():
            SimpleTask(self.main_connection, task_id=task_2['id']).on_terminate()

        assert (get_metadata(task_1) == meta_data)
        assert (get_metadata(task_2) == hidded_meta_data)

        tm.update_one(
            task_3['id'], {'state': 'success'})

        with deferred.calls_at_the_end():
            SimpleTask(self.main_connection, task_id=task_3['id']).on_terminate()

        for task in tasks:
            assert(get_metadata(task) == hidded_meta_data)


    def test_clean_private_data_in_task_on_terminate_params(self):
        # Создаем 4 задачи, где 1-я - зависит от 4, а 2 и 3 от первой
        # __
        # Когда завершается 4-я задача для нее запускается финальная функция on terminate
        # она не должна поменять параметры, так как от 4-ой зависят незавершенная 1-я
        # __
        # Когда завершается 1-я задача для нее запускается финальная функция on terminate
        # она не должна поменять параметры, так как от 1-ой зависят незавершенные 2-я и 3-я
        # __
        # Когда завершается 2-я задача для нее запускается финальная функция on terminate
        # должно скрыться значение полей с password только в ней, так как от 1-й зависит незавершенная 3-я
        # __
        # Когда завершается 3-я задача для нее запускается финальная функция on terminate
        # Должно скрыться значение поля password в 1 3 и 4 тасках

        TaskModel(self.main_connection).delete(force_remove_all=True)
        params = {'ll': 'kek', 'ter': [{'kek': 'newpassword'}, {"oldpassword": "mem", "oldpasswordnew": 'kek', 'jin': [{"password": "kek", '6': 1, "fds": 5}]}, {}]}
        create_params = {
            'task_name': 'test_name',
            'params': params,
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        task_4 = TaskModel(self.main_connection).create(**create_params)
        task_1 = TaskModel(self.main_connection).create(depends_on=[task_4['id']], **create_params)
        task_2 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        task_3 = TaskModel(self.main_connection).create(depends_on=[task_1['id']], **create_params)
        hided_params = ({'ll': 'kek', 'ter': [{'kek': 'newpassword'}, {'jin': [{'fds': 5, 'password': SENSITIVE_DATA_PLACEHOLDER, '6': 1}], 'oldpassword': SENSITIVE_DATA_PLACEHOLDER, 'oldpasswordnew': SENSITIVE_DATA_PLACEHOLDER}, {}]})
        TaskModel(self.main_connection).update_one(task_4['id'], {'state': 'success'})
        SimpleTask(self.main_connection, task_id=task_4['id']).on_terminate()
        assert(TaskModel(self.main_connection).get_params(task_4['id']) == params)

        TaskModel(self.main_connection).update_one(task_1['id'], {'state': 'success'})
        SimpleTask(self.main_connection, task_id=task_1['id']).on_terminate()
        assert(TaskModel(self.main_connection).get_params(task_1['id']) == params)

        TaskModel(self.main_connection).update_one(task_2['id'], {'state': 'success'})
        SimpleTask(self.main_connection, task_id=task_2['id']).on_terminate()
        assert (TaskModel(self.main_connection).get_params(task_1['id']) == params)
        assert (TaskModel(self.main_connection).get_params(task_2['id']) == hided_params)

        TaskModel(self.main_connection).update_one(
            task_3['id'], {'state': 'success'})
        SimpleTask(self.main_connection, task_id=task_3['id']).on_terminate()
        assert (TaskModel(self.main_connection).get_params(task_4['id']) == hided_params)
        assert (TaskModel(self.main_connection).get_params(task_3['id']) == hided_params)
        assert (TaskModel(self.main_connection).get_params(task_2['id']) == hided_params)
        assert (TaskModel(self.main_connection).get_params(task_1['id']) == hided_params)

class TestHideSensitiveParams(TestCase):
    def setUp(self):
        super(TestHideSensitiveParams, self).setUp()
        self.task_queue = 'autotest-queue'
        self.task = TaskWithSensData(self.main_connection, self.task_queue)

        self.password = 'pa$$word'
        self.anyparam = 'value'

        self.params = {
            'password': self.password,
            'anyparams': self.anyparam,

        }

    # TODO: Шифровать пароли
    # Пока закомментиреум этот тест, до тех пор, пока не придумаем, как шифровать пароли
    # def test_hide_sens_data_after_terminate(self):
    #     # после завершения задачи в базе скроем в параметрах секретные данные
    #
    #     result = self.task.delay(**self.params)
    #
    #     assert_that(
    #         TaskModel(self.main_connection).get(result.task_id, fields=['params']),
    #         has_entries(
    #             params=has_entries(
    #                 anyparams=self.anyparam,
    #                 password=HideSensitiveParamsMixin.placeholder,
    #             )
    #         )
    #     )

    def test_hide_sens_data_in_log(self):
        # скрываем секретные данные при логировании
        clean_data = TaskWithSensData.clean_log_data(**self.params)

        assert_that(
            clean_data,
            has_entries(
                anyparams=self.anyparam,
                password=app.config['SECRET_PLACEHOLDER'],
            )
        )
