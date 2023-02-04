# -*- coding: utf-8 -*-
from datetime import timedelta

from hamcrest import (
    assert_that,
    equal_to,
    instance_of,
    none,
    has_length,
    calling,
    raises,
    contains_inanyorder,
)
from unittest.mock import (
    patch,
)

from testutils import (
    TestCase,
    override_settings,
    source_path,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks import (
    CreateMailCollectorsTask,
    CreateMailCollectorTask,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.exception import (
    MailCollectingError,
    CollectorsCreatingError,
    AllBoxesCollectingError,

)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.transfer.tasks import (
    WaitingForMigrationTask,
    WaitingForMigrationsTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    MailMigrationFileModel,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    TASK_STATES,
    get_default_queue,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import Suspend
from intranet.yandex_directory.src.yandex_directory.core.task_queue.worker import TaskProcessor


class TestWaitingForMigrationTask(TestCase):

    def test_waiting_for_migration__success(self):
        # Если сборка почты завершена и ошибок нет, таск завершается успешно
        yarm_response = {
            'folders': [
                {
                    'collected': '4',
                    'errors': '0',
                    'messages': '4',
                    'name': 'INBOX'
                },
                {
                    'collected': '1',
                    'errors': '0',
                    'messages': '1',
                    'name': 'Избранное'
                },
                {
                    'collected': '2',
                    'errors': '0',
                    'messages': '2',
                    'name': 'Спам'
                },
            ],
        }

        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.transfer.tasks.status', return_value=yarm_response):
                task = WaitingForMigrationTask(self.main_connection).delay(
                    popid=12345,
                    org_id=org_id,
                )
                self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            task.get_metadata(),
            equal_to(yarm_response)
        )

    def test_waiting_for_migration__fail(self):
        # Если сборка почты завершена и есть ошибки, таск фейлится
        yarm_response = {
            'folders': [
                {
                    'collected': '4',
                    'errors': '0',
                    'messages': '4',
                    'name': 'INBOX'
                },
                {
                    'collected': '1',
                    'errors': '1',
                    'messages': '0',
                    'name': 'Избранное'
                },
                {
                    'collected': '2',
                    'errors': '0',
                    'messages': '2',
                    'name': 'Спам'
                },
            ],
        }
        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.transfer.tasks.status', return_value=yarm_response):
                task = WaitingForMigrationTask(self.main_connection).delay(
                    popid=12345,
                    org_id=org_id,
                )
                self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )
        assert_that(
            task.exception,
            instance_of(MailCollectingError)
        )
        assert_that(
            task.get_metadata(),
            equal_to(yarm_response)
        )

    def test_waiting_for_migration__defer(self):
        # Если сборка почты не завершена, таск откладывается.
        yarm_response = {
            'folders': [
                {
                    'collected': '3',
                    'errors': '0',
                    'messages': '4',
                    'name': 'INBOX'
                },
                {
                    'collected': '1',
                    'errors': '1',
                    'messages': '0',
                    'name': 'Избранное'
                },
                {
                    'collected': '2',
                    'errors': '0',
                    'messages': '2',
                    'name': 'Спам'
                },
            ],
        }
        org_id = self.organization['id']

        # создаем задачу в прошлом, потому что время в базе может отставать
        task = WaitingForMigrationTask(self.main_connection).delay(
            popid=12345,
            start_in=timedelta(seconds=-5),
            org_id=org_id,
        )

        locked_data = TaskModel(self.main_connection).lock_for_worker('worker', get_default_queue())

        if locked_data:
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.transfer.tasks.status', return_value=yarm_response):
                TaskProcessor(WaitingForMigrationTask, locked_data).process(self.main_connection)

        assert_that(
            task.state,
            equal_to(TASK_STATES.free),
        )
        assert_that(
            task.get_metadata(),
            equal_to(yarm_response)
        )

    def test_waiting_for_migration__yarm_fail(self):
        # Если yarm вернул ошибку, таск фейлится
        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.transfer.tasks.status', side_effect=Exception):
                task = WaitingForMigrationTask(self.main_connection).delay(
                    popid=12345,
                    org_id=org_id,
                )
                self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )
        assert_that(
            task.get_metadata(),
            none(),
        )


class TestWaitingForMigrationsTask(TestCase):

    def setUp(self):
        super(TestWaitingForMigrationsTask, self).setUp()
        self.nickname = 'tester'
        # кладем в базу файл с данными для миграции
        # как это сделала бы ручка /mail-migration/
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/core/mail_migration/data/migration_file.csv'
        )
        with open(file_path, 'rb') as f:
            self.file = MailMigrationFileModel(self.main_connection).create(
                org_id=self.organization['id'],
                file=f.read(),
            )

    def test_waiting_for_migrations__success(self):
        # Создаем 2 таска CreateMailCollectorTask
        # Создаем таск CreateMailCollectorsTask, у которого в зависимостях эти 2 таска
        # Создаем таск WaitingForMigrationsTask, зависящий от CreateMailCollectorsTask
        # Проверяем, что создались 2 таска WaitingForMigrationTask
        # и что у них верные параметры
        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={'popid': 123}), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
                collector_task_id_1 = CreateMailCollectorTask(self.main_connection).delay(
                    user_id='',
                    email='',
                    password='',
                    user='',
                    org_id=org_id,
                    host='',
                    port=993,
                ).task_id
                self.process_tasks()

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={'popid': 456}), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
                collector_task_id_2 = CreateMailCollectorTask(self.main_connection).delay(
                        user_id='',
                        email='',
                        password='',
                        user='',
                        org_id=org_id,
                        host='',
                        port=993,
                    ).task_id
                self.process_tasks()

        mail_collectors_task = TaskModel(self.main_connection).create(
            task_name=CreateMailCollectorsTask.get_task_name(),
            params={},
            queue=get_default_queue(),
            ttl=None,
            depends_on=[collector_task_id_1, collector_task_id_2],
        )

        TaskModel(self.main_connection).update_one(
            task_id=mail_collectors_task['id'],
            update_data={'state': TASK_STATES.success},
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.transfer.tasks.status', return_value={}), \
             patch.object(WaitingForMigrationsTask, '_is_ready', return_value=False):
                waiting_for_migrations_task = WaitingForMigrationsTask(
                    self.main_connection,
                    depends_on=[mail_collectors_task['id']],
                ).delay(
                    org_id=org_id,
                )
                self.process_tasks()
        dependencies = TaskModel(self.main_connection).get_dependencies(waiting_for_migrations_task.task_id,)

        # 1 подтаск CreateMailCollectorsTask
        # 2 - WaitingForMigrationTask

        assert_that(
            dependencies,
            has_length(3),
        )
        dependencies_params = [d['params'] for d in dependencies
                               if d['task_name'] == WaitingForMigrationTask.get_task_name()]
        expected_params = [
            {'popid': 123, 'org_id': org_id},
            {'popid': 456, 'org_id': org_id},
        ]
        assert_that(
            dependencies_params,
            contains_inanyorder(*expected_params)
        )

    def test_is_ready(self):
        # Проверяем, что функция _is_ready
        # бросает исключения или возвращает True/False
        org_id = self.organization['id']

        task = CreateMailCollectorsTask(
            self.main_connection,
        ).delay(
            host='test.host',
            port=993,
            org_id=org_id,
        )
        is_ready = WaitingForMigrationsTask(self.main_connection, task_id=task.task_id)._is_ready

        # общее количество зависимостей совпадает с количеством завершенных с ошибкой зависимостей (1 таск)
        # главный таск завершается с ошибкой
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'failed_dependencies_count': 1,
                'dependencies_count': 1,
            },
        )
        assert_that(calling(is_ready), raises(CollectorsCreatingError))

        # общее количество зависимостей совпадает с количеством завершенных с ошибкой зависимостей (много тасков)
        # главный таск завершается с ошибкой
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'failed_dependencies_count': 9,
                'dependencies_count': 10,
            },
        )
        assert_that(calling(is_ready), raises(AllBoxesCollectingError))

        # общее количество зависимостей 0
        # главный таск уходит в режим ожидания
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'dependencies_count': 0,
            },
        )
        assert_that(calling(is_ready), raises(Suspend))

        # общее количество зависимостей больше, чем сумма успешно и неуспешно завершенных зависимостей
        # главный таск уходит в режим ожидания
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'dependencies_count': 10,
                'failed_dependencies_count': 2,
                'successful_dependencies_count': 7,
            },
        )
        assert_that(calling(is_ready), raises(Suspend))

        # общее количество зависимостей равно сумме успешно и неуспешно завершенных зависимостей
        # ф-я возвращает True, главный таск должен завершиться успешно
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'dependencies_count': 10,
                'failed_dependencies_count': 3,
                'successful_dependencies_count': 7,
            },
        )

        assert_that(
            is_ready(),
            equal_to(True)
        )

        # имеется одна зависимость и она завершилась успешно
        # функция возвращает False, а главный таск должен начать выполнять оснвную работу
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'dependencies_count': 1,
                'failed_dependencies_count': 0,
                'successful_dependencies_count': 1,
            },
        )

        assert_that(
            is_ready(),
            equal_to(False)
        )
