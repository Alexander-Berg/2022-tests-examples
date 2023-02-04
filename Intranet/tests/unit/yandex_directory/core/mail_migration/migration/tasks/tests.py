# -*- coding: utf-8 -*-
import uuid
from hamcrest import (
    assert_that,
    has_length,
    calling,
    raises,
    has_items,
    contains_inanyorder,
)
from hamcrest import (
    equal_to,
)
from unittest.mock import patch, Mock

from testutils import (
    TestCase,
    source_path,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration import (
    MailMigrationTask,
    CreateMailBoxesTask,
    CreateMailCollectorsTask,
    CreateMailCollectorTask,
    DeleteCollectorsTask,
    DeleteCollectorTask,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.exception import (
    MailMigrationError,
    CollectorDeletingError,
)
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.utils import (
    MailMigration,
    MAIL_MIGRATION_STATES,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    MailMigrationFileModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models import TaskModel
from intranet.yandex_directory.src.yandex_directory.core.task_queue import TASK_STATES, Task
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import Suspend
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    only_fields,
)
from intranet.yandex_directory.src.yandex_directory.passport import PassportApiClient


class TestMailMigrationTask(TestCase):

    def setUp(self):
        super(TestMailMigrationTask, self).setUp()
        # кладем в базу файл с данными для миграции
        # как это сделала бы ручка /mail-migration/
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/core/mail_migration/data/migration_file.csv'
        )
        with open(file_path, 'rb') as f:
            self.file_id = MailMigrationFileModel(self.main_connection).create(
                org_id=self.organization['id'],
                file=f.read(),
            )['id']

    def test_mail_migration_with_file_rollback(self):
        migration_data = MailMigrationFileModel(self.main_connection).get(
            self.file_id,
        )
        self.assertTrue(CreateMailBoxesTask.need_rollback)
        self.assertTrue(migration_data is not None)
        CreateMailBoxesTask(self.main_connection).rollback(migration_file_id=self.file_id)
        migration_data = MailMigrationFileModel(self.main_connection).get(
            self.file_id,
        )
        self.assertTrue(migration_data is None)

    def test_mail_migration_create(self):
        # создаем задачу MailMigrationTask
        # проверяем, что у неё в зависимостях появились таски
        # CreateMailBoxesTask, CreateMailCollectorsTask, WaitingForMigrationsTask
        # с нужными параметрами
        user_uids = [1, 2]
        org_id = self.organization['id']

        m = Mock(side_effect=user_uids)
        with patch.object(PassportApiClient, 'account_add', side_effect=m), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
            migration_task = MailMigrationTask(self.main_connection).delay(
                migration_file_id=str(self.file_id),
                org_id=org_id,
                host='test@host',
                port=993,
                imap=False,
            )
            self.process_tasks()

        dependencies = TaskModel(self.main_connection).get_dependencies(migration_task.task_id)
        assert_that(
            dependencies,
            has_length(2),
        )
        expected = [
            {
                'task_name': CreateMailBoxesTask.get_task_name(),
                'params': {
                    'migration_file_id': str(self.file_id),
                    'org_id': org_id,
                },
            },
            {
                'task_name': CreateMailCollectorsTask.get_task_name(),
                'params': {
                    'org_id': org_id,
                    'host': 'test@host',
                    'port': 993,
                    'imap': False,
                    'ssl': True,
                    'no_delete_msgs': True,
                    'sync_abook': True,
                    'mark_archive_read': True,
                },
            },
            # {
            #     'task_name': WaitingForMigrationsTask.get_task_name(),
            #     'params': {},
            # },
        ]

        actual = [only_fields(d, 'task_name', 'params') for d in dependencies]

        assert_that(
            actual,
            has_items(*expected),
        )

    def test_is_ready(self):
        # Проверяем, что функция _is_ready
        # бросает исключения или возвращает True/False
        user_uids = [1, 2]
        m = Mock(side_effect=user_uids)
        with patch.object(PassportApiClient, 'account_add', side_effect=m), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
            task = MailMigrationTask(self.main_connection).delay(
                migration_file_id=str(self.file_id),
                org_id=self.organization['id'],
                host='test@host',
                port=993,
            )
        is_ready = MailMigrationTask(self.main_connection, task_id=task.task_id)._is_ready

        # все подтаски завершились, некоторые с ошибками
        # таск бросает исключение MailMigrationError
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'successful_dependencies_count': 2,
                'failed_dependencies_count': 1,
                'dependencies_count': 3,
            },
        )
        assert_that(calling(is_ready), raises(MailMigrationError))

        # все подтаски завершились, без ошибок
        # таск должен завершить. is_ready = True
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'successful_dependencies_count': 3,
                'failed_dependencies_count': 0,
                'dependencies_count': 3,
            },
        )
        assert_that(
            is_ready(),
            equal_to(True)
        )

        # не все подтаски завершились
        # таск бросает исключение Suspend
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'successful_dependencies_count': 1,
                'failed_dependencies_count': 1,
                'dependencies_count': 3,
            },
        )
        assert_that(calling(is_ready), raises(Suspend))

        # подтасок ещё нет
        # таск должен начать основную работу. is_ready = False
        TaskModel(self.main_connection).update_one(
            task_id=task.task_id,
            update_data={
                'successful_dependencies_count': 0,
                'failed_dependencies_count': 0,
                'dependencies_count': 0,
            },
        )
        assert_that(
            is_ready(),
            equal_to(False)
        )


class TestDeleteMailCollectorsTask(TestCase):
    # Создаем 2 таска CreateMailCollectorTask
    # Создаем таск CreateMailCollectorsTask, который зависит от первых двух
    # Создаем таск MailMigrationTask, который зависит от CreateMailCollectorsTask
    # Создаем таск DeleteCollectorsTask, который зависит от MailMigrationTask
    # Проверяем, что созадлись подтаски DeleteCollectorTask с нужными параметрами
    def delete_collectors_task_test(self):
        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={'popid': 123}), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
            collector_task_id_1 = CreateMailCollectorTask(self.main_connection).delay(
                user_id=111,
                email='',
                password='',
                user='',
                org_id=self.organization['id'],
                host='',
                port=993,
            ).task_id
            self.process_tasks()

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.create', return_value={'popid': 456}), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.collector.tasks.check_server', return_value={}):
            collector_task_id_2 = CreateMailCollectorTask(self.main_connection).delay(
                user_id=222,
                email='',
                password='',
                user='',
                org_id=org_id,
                host='',
                port=993,
            ).task_id
            self.process_tasks()

        TaskModel(self.main_connection).update(
            filter_data={'id': [collector_task_id_1, collector_task_id_2]},
            update_data={'state': TASK_STATES.success},
        )

        mail_collectors_task = TaskModel(self.main_connection).create(
            task_name=CreateMailCollectorsTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            depends_on=[collector_task_id_1, collector_task_id_2],
            state='success',
        )

        migration_task_id = TaskModel(self.main_connection).create(
            task_name=MailMigrationTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            depends_on=[mail_collectors_task['id']],
            state='success',
        )['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.migration.tasks.delete', side_effect=None), \
             patch.object(DeleteCollectorsTask, '_is_ready', return_value=False):
            delete_collectors_task_id = DeleteCollectorsTask(
                self.main_connection,
                depends_on=[migration_task_id],
            ).delay(org_id=org_id).task_id
            self.process_tasks()

        delete_collectors_subtasks = TaskModel(self.main_connection).get_dependencies(
            delete_collectors_task_id,
            DeleteCollectorTask,
        )
        assert_that(
            delete_collectors_subtasks,
            has_length(2),
        )
        expected_params = [
            {'uid': 111, 'popid': 123, 'org_id': org_id},
            {'uid': 222, 'popid': 456, 'org_id': org_id},
        ]
        actual_params = [d['params'] for d in delete_collectors_subtasks]
        assert_that(
            actual_params,
            contains_inanyorder(*expected_params),
        )

    def test_is_ready(self):
        # Проверяем, что функция _is_ready
        # бросает исключения или возвращает True/False
        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.migration.tasks.delete', side_effect=None), \
             patch.object(DeleteCollectorsTask, '_is_ready', return_value=False):
            task_id = DeleteCollectorsTask(
                self.main_connection,
            ).delay(org_id=org_id).task_id
        is_ready = DeleteCollectorsTask(self.main_connection, task_id=task_id)._is_ready

        # подтасок ещё нет
        # таск бросает исключение Suspend
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 0,
                'failed_dependencies_count': 0,
                'dependencies_count': 0,
            },
        )
        assert_that(calling(is_ready), raises(Suspend))

        # есть одна подтаска и она не завершилась
        # таск бросает исключение Suspend
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 0,
                'failed_dependencies_count': 0,
                'dependencies_count': 1,
            },
        )
        assert_that(calling(is_ready), raises(Suspend))

        # есть одна подтаска и она завершилась c ошибкой
        # таск начинает основную работу. Ready=False
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 0,
                'failed_dependencies_count': 1,
                'dependencies_count': 1,
            },
        )
        assert_that(
            is_ready(),
            equal_to(False),
        )

        # есть одна подтаска и она завершилась без ошибок
        # таск начинает основную работу. Ready=False
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 1,
                'failed_dependencies_count': 0,
                'dependencies_count': 1,
            },
        )
        assert_that(
            is_ready(),
            equal_to(False),
        )

        # все подтаски завершились, хотя бы один с ошибкой
        # таск бросает исключение MailMigrationError
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 1,
                'failed_dependencies_count': 2,
                'dependencies_count': 3,
            },
        )
        assert_that(calling(is_ready), raises(CollectorDeletingError))

        # все подтаски завершились, без ошибок
        # таск должен завершить. is_ready = True
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 3,
                'failed_dependencies_count': 0,
                'dependencies_count': 3,
            },
        )
        assert_that(
            is_ready(),
            equal_to(True)
        )

        # все подтаски завершились, без ошибок
        # таск должен завершить. is_ready = True
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 2,
                'failed_dependencies_count': 1,
                'dependencies_count': 3,
            },
        )
        assert_that(
            is_ready(),
            equal_to(True)
        )

        # не все подтаски завершились
        # таск бросает исключение Suspend
        TaskModel(self.main_connection).update_one(
            task_id=task_id,
            update_data={
                'successful_dependencies_count': 1,
                'failed_dependencies_count': 1,
                'dependencies_count': 3,
            },
        )
        assert_that(calling(is_ready), raises(Suspend))

    def test_no_collectors_task(self):
        # Создаем таск CreateMailCollectorsTask без подзадач
        # Создаем таск MailMigrationTask, который зависит от CreateMailCollectorsTask
        # Создаем таск DeleteCollectorsTask, который зависит от MailMigrationTask
        # Проверяем, что DeleteCollectorsTask завершился
        mail_collectors_task = TaskModel(self.main_connection).create(
            task_name=CreateMailCollectorsTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            state='success',
        )

        migration_task_id = TaskModel(self.main_connection).create(
            task_name=MailMigrationTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            depends_on=[mail_collectors_task['id']],
            state='success',
        )['id']

        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.migration.tasks.delete', side_effect=None), \
             patch.object(DeleteCollectorsTask, '_is_ready', return_value=False):
            delete_collectors_task_id = DeleteCollectorsTask(
                self.main_connection,
                depends_on=[migration_task_id],
            ).delay(org_id=org_id).task_id
            self.process_tasks()

        task = TaskModel(self.main_connection).get(delete_collectors_task_id)
        assert_that(
            task['state'],
            equal_to(TASK_STATES.success)
        )
        subtasks = TaskModel(self.main_connection).get_dependencies(
            delete_collectors_task_id,
        )
        assert_that(
            subtasks,
            has_length(1),
        )


class TestMailMigrationProgress(TestCase):

    def setUp(self):
        super(TestMailMigrationProgress, self).setUp()
        # кладем в базу файл с данными для миграции
        # как это сделала бы ручка /mail-migration/
        file_path = source_path(
            'intranet/yandex_directory/tests/unit/yandex_directory/core/mail_migration/data/migration_file.csv'
        )
        with open(file_path) as f:
            self.file_id = MailMigrationFileModel(self.main_connection).create(
                org_id=self.organization['id'],
                file=f.read(),
            )['id']

    def test_no_subtasks_pending(self):
        # если у таска MailMigrationTask нет подтасков, прогресс возвращает 'pending'
        migrations_task_id = TaskModel(self.main_connection).create(
            task_name=MailMigrationTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )['id']

        pending_migration_progress = [
            {'stage': 'accounts-creating', 'state': MAIL_MIGRATION_STATES.pending},
            {'stage': 'collectors-creating', 'state': MAIL_MIGRATION_STATES.pending},
        ]

        assert_that(
            MailMigration(self.main_connection, migrations_task_id).get_migration_progress(),
            pending_migration_progress,
        )

    def test_no_subtasks_undefined(self):
        # если у таска MailMigrationTask нет подтасков и он завершен, прогресс возвращает 'undefined'
        migrations_task_id = TaskModel(self.main_connection).create(
            task_name=MailMigrationTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )['id']
        TaskModel(self.main_connection).set_state(migrations_task_id, 'failed')

        pending_migration_progress = [
            {'stage': 'accounts-creating', 'state': MAIL_MIGRATION_STATES.undefined},
            {'stage': 'collectors-creating', 'state': MAIL_MIGRATION_STATES.undefined},
        ]

        assert_that(
            MailMigration(self.main_connection, migrations_task_id).get_migration_progress(),
            pending_migration_progress,
        )

    def test_cancel_subtask(self):
        # Таск миграции зависит от таска создания коллекторов и таска создания ящиков
        # который зависят от таска создания аккаунтов.
        # Таким образом при отмене таска создания аккаунтов должны отменяться все предыдущие таски.
        user_uids = [1, 2]
        m = Mock(side_effect=user_uids)
        with patch.object(PassportApiClient, 'account_add', side_effect=m), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
                migration_task = MailMigrationTask(self.main_connection).delay(
                    migration_file_id=str(self.file_id),
                    org_id=self.organization['id'],
                    host='test@host',
                    port=993,
                    imap=False,
                )
                self.process_tasks()
        dependencies = TaskModel(self.main_connection).get_dependencies(migration_task.task_id)
        create_acc = TaskModel(self.main_connection).get_dependencies(dependencies[0]['id'])[0]
        if not create_acc:
            create_acc = TaskModel(self.main_connection).get_dependencies(dependencies[0]['id'])[0]
        Task(self.main_connection, task_id = create_acc['id']).cancel()
        state1 = TaskModel(self.main_connection).get_state(migration_task.task_id)
        state2 = TaskModel(self.main_connection).get_state(dependencies[0]['id'])
        state3 = TaskModel(self.main_connection).get_state(dependencies[1]['id'])
        assert(state1 == state2 and state2 == state3 and state3 == 'canceled')

    def test_match_state(self):
        # проверяем, как матчатся статусы задач на статусы миграции почты для фронта

        # Если таск по созданию аккаунтов не найден, то считаем, что он был выполнен успешно,
        # и удалён по крону.
        assert_that(
            MailMigration(self.main_connection, uuid.uuid4())._match_states(None, 'accounts-creating'),
            equal_to(MAIL_MIGRATION_STATES.success),
        )

        # у таска нет подтасков, кроме одного, статус не терминальный - статус pending
        subtask = TaskModel(self.main_connection).create(
            task_name=CreateMailBoxesTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )

        task = TaskModel(self.main_connection).create(
            task_name=CreateMailBoxesTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            depends_on=[subtask['id']]
        )
        assert_that(
            MailMigration(self.main_connection, uuid.uuid4())._match_states(task, 'accounts-creating'),
            equal_to(MAIL_MIGRATION_STATES.pending),
        )

        # у таска нет подтасков, кроме одного, статус терминальный (не success)
        # статус миграции failed
        TaskModel(self.main_connection).set_state(task['id'], TASK_STATES.failed)
        task = TaskModel(self.main_connection).get(task['id'])
        assert_that(
            MailMigration(self.main_connection, uuid.uuid4())._match_states(task, 'accounts-creating'),
            equal_to(MAIL_MIGRATION_STATES.failed),
        )
        # у таска больше одного подтаска
        # статус таска не терминальный - статус миграции in-progress
        subtask_1 = TaskModel(self.main_connection).create(
            task_name=CreateMailBoxesTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )
        subtask_2 = TaskModel(self.main_connection).create(
            task_name=CreateMailBoxesTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
        )

        task_1 = TaskModel(self.main_connection).create(
            task_name=CreateMailBoxesTask.get_task_name(),
            params={},
            queue='default',
            ttl=None,
            depends_on=[subtask_1['id'], subtask_2['id']]
        )
        assert_that(
            MailMigration(self.main_connection, uuid.uuid4())._match_states(task_1, 'accounts-creating'),
            equal_to(MAIL_MIGRATION_STATES.in_progress),
        )

        # у таска больше одного подтаска
        # статус таска success - статус миграции success
        TaskModel(self.main_connection).set_state(task_1['id'], TASK_STATES.success)
        task_1 = TaskModel(self.main_connection).get(task_1['id'])
        assert_that(
            MailMigration(self.main_connection, uuid.uuid4())._match_states(task_1, 'accounts-creating'),
            equal_to(MAIL_MIGRATION_STATES.success),
        )

        # у таска больше одного подтаска
        # статус таска не success - статус миграции failed
        TaskModel(self.main_connection).set_state(task_1['id'], TASK_STATES.rollback)
        task_1 = TaskModel(self.main_connection).get(task_1['id'])
        assert_that(
            MailMigration(self.main_connection, uuid.uuid4())._match_states(task_1, 'accounts-creating'),
            equal_to(MAIL_MIGRATION_STATES.failed),
        )

    def test_get_migration_task(self):
        # Проверяем что таск находится и по id, и по org_id
        migrations_task = TaskModel(self.main_connection).create(
            task_name=MailMigrationTask.get_task_name(),
            params={'org_id': self.organization['id']},
            queue='default',
            ttl=None,
        )
        assert_that(
            MailMigration(self.main_connection, mail_migration_task_id=migrations_task['id']).get_migration_task(),
            equal_to(migrations_task),
        )
        assert_that(
            MailMigration(self.main_connection, org_id=self.organization['id']).get_migration_task(),
            equal_to(migrations_task),
        )
