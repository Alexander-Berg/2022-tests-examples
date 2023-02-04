# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_length,
    contains_inanyorder,
    calling,
    raises,
    has_entries,
)
from hamcrest import (
    equal_to,
    instance_of,
)
from unittest.mock import patch, Mock

from testutils import (
    TestCase,
    source_path,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks import (
    CreateMailCollectorTask,
    CreateMailCollectorsTask,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.exception import (
    CollectorsCreatingError,
    NoCollectorID,
    MailBoxesCreatingError,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.mailbox.tasks import (
    CreateMailBoxesTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    MailMigrationFileModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models import TaskModel
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import (
    TASK_STATES,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import unpickle
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import Suspend
from intranet.yandex_directory.src.yandex_directory.passport import PassportApiClient

from intranet.yandex_directory.src.yandex_directory.core.yarm.exceptions import YarmDuplicateError


class TestCreateMailCollectorTask(TestCase):

    def test_create_mail_collector_success(self):
        # Создаем задачу CreateMailCollectorTask
        # Проверяем, что задача завершилась успешно и в результате - id сборщика
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={'popid':12345}):
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
                    task = CreateMailCollectorTask(self.main_connection).delay(
                        user_id='test',
                        email='test@test.com',
                        password='123',
                        user='test_passport_login@test.com',
                        org_id=self.organization['id'],
                        host='test.host',
                        port=993,
                    )
                    self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            task.get_result(),
            equal_to(12345),
        )

    def test_create_mail_collector_check_server_fail(self):
        # Создаем задачу CreateMailCollectorTask
        # Поверка настроек сборщика вернула ошибку
        # Проверям, что задача завершилась с ошибкой
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', side_effect=Exception):
                task = CreateMailCollectorTask(self.main_connection).delay(
                    user_id='test',
                    email='test@test.com',
                    password='123',
                    org_id=self.organization['id'],
                    host='test.host',
                    port=993,
                )
                self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )

    def test_create_mail_collector_create_fail(self):
        # Создаем задачу CreateMailCollectorTask
        # Ф-я создания сборщика вернула ошибку
        # Проверям, что задача завершилась с ошибкой
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', side_effect=Exception):
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
                    task = CreateMailCollectorTask(self.main_connection).delay(
                        user_id='test',
                        email='test@test.com',
                        password='123',
                        org_id=self.organization['id'],
                        host='test.host',
                        port=993,
                    )
                    self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )

    def test_create_mail_collector_no_popid_fail(self):
        # Создаем задачу CreateMailCollectorTask
        # Ф-я создания сборщика не вернула popid
        # Проверям, что задача завершилась с ошибкой
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={}):
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
                    task = CreateMailCollectorTask(self.main_connection).delay(
                        user_id='test',
                        email='test@test.com',
                        password='123',
                        user='test_passport_login@test.com',
                        org_id=self.organization['id'],
                        host='test.host',
                        port=993,
                    )
                    self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.failed),
        )
        assert_that(
            task.exception,
            instance_of(NoCollectorID),
        )

    def test_create_mail_collector_duplicate_error(self):
        # Создаем задачу CreateMailCollectorTask
        # Ф-я создания сборщика вернула YarmDuplicateError
        # Проверям, что задача завершилась успешно, а в metadata записалась информация о том,что сборщик уже существует
        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create') as yarm_create:
            with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
                    yarm_create.side_effect = YarmDuplicateError('test')
                    task = CreateMailCollectorTask(self.main_connection).delay(
                        user_id='test',
                        email='test@test.com',
                        password='123',
                        user='test_passport_login@test.com',
                        org_id=self.organization['id'],
                        host='test.host',
                        port=993,
                    )
                    self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
        assert_that(
            task.get_metadata(),
            has_entries(
                message='Collector already exists',
            )
        )


class TestCreateMailCollectorsTask(TestCase):

    def setUp(self):
        super(TestCreateMailCollectorsTask, self).setUp()
        self.nickname = 'tester'
        # кладем в базу файл с данными для миграции
        # как это сделала бы ручка /mail-migration/
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/core/mail_migration/data/migration_file.csv'
        )
        with open(file_path) as f:
            self.file = MailMigrationFileModel(self.main_connection).create(
                org_id=self.organization['id'],
                file=f.read(),
            )

    def test_create_mail_collectors_success(self):
        # Создаем таск CreateMailBoxesTask
        # Он создаёт 2 таска CreateAccountTask, которые успешно завершаются
        # Создаем таск CreateMailCollectorsTask, зависящий от CreateMailBoxesTask
        # Проверяем, что создались 2 таска CreateMailCollectorTask
        # и что у них верные параметры
        user_uids = [1, 2]
        org_id = self.organization['id']
        m = Mock(side_effect=user_uids)
        with patch.object(PassportApiClient, 'account_add', side_effect=m), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
                create_mailboxes_task = CreateMailBoxesTask(self.main_connection).delay(
                    migration_file_id=str(self.file['id']),
                    org_id=org_id,
                )
                self.process_tasks()

        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={'popid':12345}), \
            patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}), \
            patch.object(CreateMailCollectorsTask, '_is_ready', return_value=False), \
            patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.get_user_data_from_blackbox_by_uid',
                  return_value={'login': 'test@test.com'}):
                collectors_task = CreateMailCollectorsTask(
                    self.main_connection,
                    depends_on=[create_mailboxes_task.task_id]
                ).delay(
                    host='test.host',
                    port=993,
                    org_id=org_id,
                )
                self.process_tasks()

        dependencies = TaskModel(self.main_connection).get_dependencies(collectors_task.task_id,)

        # 1 подтаск CreateMailBoxesTask
        # 2 - CreateMailCollectorTask

        assert_that(
            dependencies,
            has_length(3),
        )

        actual_tasks_params = [d['params'] for d in dependencies if d['state'] == TASK_STATES.success]

        create_account_tasks = TaskModel(self.main_connection).get_dependencies(create_mailboxes_task.task_id)
        expected_tasks_params = []
        for t in create_account_tasks:
            matcher = has_entries(
                email=t['params']['email'],
                password=t['params']['old_password'],
                org_id=org_id,
                user_id=unpickle(t['result']),
                host='test.host',
                port=993,
                imap=True,
                ssl=True,
                no_delete_msgs=True,
                sync_abook=True,
                mark_archive_read=True,
                user='test@test.com',
            )
            expected_tasks_params.append(matcher)

        expected_tasks_params.append(
            has_entries(
                migration_file_id=str(self.file['id']),
                org_id=org_id,
            )
        )

        assert_that(
            actual_tasks_params,
            contains_inanyorder(*expected_tasks_params)
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
        is_ready = CreateMailCollectorsTask(self.main_connection, task_id=task.task_id)._is_ready

        # общее количество зависимостей совпадает с количеством завершенных с ошибкой зависимостей (1 таск)
        # главный таск завершается с ошибкой
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'failed_dependencies_count': 1,
                'dependencies_count': 1,
            },
        )
        assert_that(calling(is_ready), raises(MailBoxesCreatingError))

        # общее количество зависимостей совпадает с количеством завершенных с ошибкой зависимостей (много тасков)
        # главный таск завершается с ошибкой
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'failed_dependencies_count': 9,
                'dependencies_count': 10,
            },
        )
        assert_that(calling(is_ready), raises(CollectorsCreatingError))

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
