# -*- coding: utf-8 -*-
import datetime

from hamcrest import (
    assert_that,
    contains_inanyorder,
    has_entries,
    none,
    equal_to,
    not_,
    not_none,
)

from testutils import (
    TestCase,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.mail_migration import (
    CreateAccountTask,
    CreateMailBoxesTask,
    MailMigrationTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models import TaskModel
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    TASK_STATES,
    Task,
    get_default_queue,
)


class SimpleTask(Task):
    need_rollback = False
    org_id_is_required = False

    def do(self, **kwargs):
        return 'success'


class TestTaskModel(TestCase):

    def test_create(self):
        # создаем новую запись
        TaskModel(self.main_connection).delete(force_remove_all=True)

        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': 'default',
            'ttl': 60*60,
            'author_id': self.user['id'],
        }
        task = TaskModel(self.main_connection).create(**create_params)

        # в пустой таблице появилась запись
        assert_that(
            TaskModel(self.main_connection).find({'id': task['id']}, one=True),
            has_entries(**create_params)
        )

    def test_get_lock_for_worker(self):
        TaskModel(self.main_connection).delete(force_remove_all=True)

        queue = 'queue'
        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': queue,
            'ttl': 60 * 60,
        }

        # задача с истекшим ttl
        task_params = create_params.copy()
        task_params['task_name'] = 'task_name1'
        task = TaskModel(self.main_connection).create(**task_params)
        task = TaskModel(self.main_connection).update(
            update_data={
                'worker': 'some-worker-name',
                'free_lock_at': utcnow() - datetime.timedelta(days=10),
                'start_at': utcnow() - datetime.timedelta(days=10),
                'created_at': utcnow() - datetime.timedelta(days=10),
            },
            filter_data={'id': task['id']},
        )
        # обычная задача в другой очереди
        task_params = create_params.copy()
        task_params['queue'] = 'another-queue'
        TaskModel(self.main_connection).create(**task_params)

        # обычная задача в нашей очереди
        task_params = create_params.copy()
        task_params['task_name'] = 'task_name2'
        task_params['start_in'] = datetime.timedelta(seconds=-5)    # в прошлом, потому что время в базе может отставать
        TaskModel(self.main_connection).create(**task_params)

        # задача со временем старта в будущем
        task_params = create_params.copy()
        task_params['task_name'] = 'task_name3'
        task_params['start_in'] = datetime.timedelta(minutes=30)

        # завершенная задача
        task_params = create_params.copy()
        task_params['task_name'] = 'task_name4'
        task = TaskModel(self.main_connection).create(**task_params)
        TaskModel(self.main_connection).update(
            update_data={'finished_at': utcnow()},
            filter_data={'id': task['id']},
        )

        # Зарегистрируем типы тасков, чтоб их можно было выполнять
        # Значение класса тут не важно, поскольку мы лишь проверяем,
        # что таск можно залочить.
        from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import TaskType
        TaskType.task_types['task_name1'] = None
        TaskType.task_types['task_name2'] = None
        TaskType.task_types['task_name3'] = None
        TaskType.task_types['task_name4'] = None

        assert_that(
            TaskModel(self.main_connection).lock_for_worker('worker1', queue),
            has_entries(
                task_name='task_name1'
            )
        )
        assert_that(
            TaskModel(self.main_connection).lock_for_worker('worker2', queue),
            has_entries(
                task_name='task_name2'
            )
        )
        assert_that(
            TaskModel(self.main_connection).lock_for_worker('worker3', queue),
            none()
        )

    def test_lock_suspended_task(self):
        # приостановленную задачу нельзя взять на выполнение
        TaskModel(self.main_connection).delete(force_remove_all=True)
        task = SimpleTask(self.main_connection).delay(
            start_in=datetime.timedelta(seconds=-5),
        )
        SimpleTask(self.main_connection, task_id=task.task_id).suspend()

        locked_task_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())
        assert_that(
            locked_task_data,
            none()
        )

    def test_release_task(self):
        # освобождаем задачу занятую ранее обработчиком
        # она должна перейдти в состояние доступное для захвата другим обрабочиком

        queue = 'queue'
        create_params = {
            'task_name': 'task_name',
            'params': {},
            'queue': queue,
            'ttl': 60 * 60,
            'start_in': datetime.timedelta(seconds=-5),
        }
        task = TaskModel(self.main_connection).create(**create_params)

        # Зарегистрируем тип таска, чтоб его можно было выполнять
        # Значение класса тут не важно, поскольку мы лишь проверяем,
        # что таск можно залочить.
        from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import TaskType
        TaskType.task_types['task_name'] = None

        TaskModel(self.main_connection).lock_for_worker('some-worker', queue)

        assert_that(
            TaskModel(self.main_connection).get(task['id']),
            has_entries(
                state=not_(
                    equal_to('free')
                ),
                worker=not_none(),
                locked_at=not_none(),
                free_lock_at=not_none(),
            )
        )
        TaskModel(self.main_connection).release_task(task['id'])
        # теперь задача доступна для выполлнения
        assert_that(
            TaskModel(self.main_connection).get(task['id']),
            has_entries(
                state='free',
                worker=none(),
                locked_at=none(),
                free_lock_at=none(),
            )
        )

    def test_get_dependents(self):
        # Создаем таск task_1. Создаем ещё 2 таска, task_2 и task_3, которые зависят от первого.
        # Проверем, что в списке тасков, зависящих от task_1, имеются task_2 и task_3

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

        assert_that(
            TaskModel(self.main_connection)._get_dependents(task_1['id']),
            contains_inanyorder(task_2, task_3),
        )

    def test_get_dependencies(self):
        # Создаем таски task_1 и task_2. Создаем task_3, который зависит от первых двух.
        # Проверем, что в списке тасков, от которых зависит task_3, имеются task_1 и task_2

        TaskModel(self.main_connection).delete(force_remove_all=True)

        create_params = {
            'task_name': 'test_name',
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        task_1 = TaskModel(self.main_connection).create(**create_params)
        task_2 = TaskModel(self.main_connection).create(**create_params)
        task_3 = TaskModel(self.main_connection).create(
            depends_on=[task_1['id'], task_2['id']], **create_params)

        assert_that(
            TaskModel(self.main_connection).get_dependencies(task_3['id']),
            contains_inanyorder(task_1, task_2),
        )

    def test_get_by_params(self):
        TaskModel(self.main_connection).delete(force_remove_all=True)
        # Создаем таск task_1, а затем task_2 и task_3 зависящий от первого, но незаконченные
        # Проверем, что таски которые зависят от 1-го незаконченныe(force_remove_all=True)
        task = TaskModel(self.main_connection).create(
            task_name='test_name',
            params={'org_id': 123, 'other_param': 'some data'},
            queue='default',
            ttl=60 * 60,
            author_id=self.user['id'],
        )
        assert_that(
            TaskModel(self.main_connection).find(
                filter_data={'params__contains': {'org_id': 123}},
                one=True
            ),
            task,
        )

    def test_get_state(self):
        TaskModel(self.main_connection).delete(force_remove_all=True)
        task = TaskModel(self.main_connection).create(
            task_name='test_name',
            params={'org_id': 123, 'other_param': 'some data'},
            queue='default',
            ttl=60 * 60,
            author_id=self.user['id'],
        )
        state = TaskModel(self.main_connection).get_state(task['id'])
        assert(state == 'free')

    def test_get_params(self):
        TaskModel(self.main_connection).delete(force_remove_all=True)
        params = {'org_id': 123, 'other_param': 'some data'}
        task = TaskModel(self.main_connection).create(
            task_name='test_name',
            params=params,
            queue='default',
            ttl=60 * 60,
            author_id=self.user['id'],
        )
        assert(TaskModel(self.main_connection).get_params(task['id']) == params)

    def test_save_params(self):
        TaskModel(self.main_connection).delete(force_remove_all=True)
        params = {'org_id': 321, 'other_param': 'new data'}
        task = TaskModel(self.main_connection).create(
            task_name='test_name',
            params={'org_id': 123, 'other_param': 'some data'},
            queue='default',
            ttl=60 * 60,
            author_id=self.user['id'],
        )
        TaskModel(self.main_connection).save_params(task['id'], params)
        assert(TaskModel(self.main_connection).get_params(task['id']) == params)

    def test_get_mail_migration_stats(self):
        # Создадим 4 таска:
        # 1. Относящийся к миграции почты, success
        # 2. Относящийся к миграции почты, failed
        # 3. Относящийся к миграции почты, suspended
        # 4. Не относящийся к миграции почты, success
        # Проверим, что статистика посчитается только для успешного и неуспешного тасков по миграции

        task_classes = [
            CreateAccountTask,
            CreateMailBoxesTask,
            MailMigrationTask,
            SimpleTask,
        ]
        task_names = [cls.get_task_name() for cls in task_classes]
        TaskModel(self.main_connection).delete(force_remove_all=True)
        create_params = {
            'params': {},
            'queue': 'default',
            'ttl': 60 * 60,
            'author_id': self.user['id'],
        }
        tasks = []
        for task_name in task_names:
            tasks.append(TaskModel(self.main_connection).create(task_name=task_name, **create_params))

        # success
        TaskModel(self.main_connection).\
            filter(id=tasks[0]['id']).\
            update(state=TASK_STATES.success, finished_at=utcnow() + datetime.timedelta(minutes=10))
        # failed
        TaskModel(self.main_connection).\
            filter(id=tasks[1]['id']).\
            update(state=TASK_STATES.failed, finished_at=utcnow() + datetime.timedelta(minutes=10))
        # suspended
        TaskModel(self.main_connection).\
            filter(id=tasks[2]['id']).\
            update(state=TASK_STATES.suspended)
        # not mail_migration task
        TaskModel(self.main_connection).\
            filter(id=tasks[3]['id']).\
            update(state=TASK_STATES.success, finished_at=utcnow() + datetime.timedelta(minutes=10))

        stat = TaskModel(self.main_connection).get_mail_migration_stats()
        assert_that(
            stat,
            has_entries(
                success=1,
                failed=1
            ),
        )
