# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_length,
    contains_inanyorder,
    none,
)
from unittest.mock import patch, Mock

from testutils import (
    TestCase,
    source_path,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import SENSITIVE_DATA_PLACEHOLDER
from intranet.yandex_directory.src.yandex_directory.core.mail_migration.mailbox.tasks import (
    CreateMailBoxesTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    TaskModel,
    TaskRelationsModel,
    MailMigrationFileModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.csv_reader import read_csv_as_dicts
from intranet.yandex_directory.src.yandex_directory.core.views.mail_migration import (
    EMAIL,
    PASSWORD,
    FIRST_NAME,
    LAST_NAME,
    NEW_LOGIN,
    NEW_PASSWORD,
)
from intranet.yandex_directory.src.yandex_directory.passport import PassportApiClient


class TestCreateMailBoxesTask(TestCase):

    def setUp(self):
        super(TestCreateMailBoxesTask, self).setUp()
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

    def test_create_mailboxes_task__success(self):
        # Создаем таск CreateMailBoxesTask
        # Проверяем, что создались таски-зависимости типа CreateAccountTask с правильными параметрами
        # Проверяем, что миграционный файл удалился из базы
        user_uids = [1, 2]
        m = Mock(side_effect=user_uids)
        org_id = self.organization['id']

        with patch.object(PassportApiClient, 'account_add', side_effect=m), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mail_migration.account.tasks.get_user_id_from_passport', return_value=None):
                create_mailboxes_task = CreateMailBoxesTask(self.main_connection).delay(
                    migration_file_id=str(self.file['id']),
                    org_id=org_id,
                )
                self.process_tasks()

        dependencies = TaskRelationsModel(self.main_connection).filter(
            task_id=create_mailboxes_task.task_id,
        )
        assert_that(
            dependencies,
            has_length(2),
        )
        dependency_ids = [d['dependency_task_id'] for d in dependencies]
        create_account_tasks = TaskModel(self.main_connection).find(
            filter_data={'id': dependency_ids},
            order_by=['start_at'],
        )
        actual_tasks_params = [task['params'] for task in create_account_tasks]
        expected_tasks_params = []
        reader = read_csv_as_dicts(self.file['file'])
        for row in reader:
            params = {}
            params['nickname'] = (row.get(NEW_LOGIN) or row.get(EMAIL)[:row.get(EMAIL).index('@')]).lower()
            params['password'] = SENSITIVE_DATA_PLACEHOLDER
            params['first_name'] = row.get(FIRST_NAME)
            params['last_name'] = row.get(LAST_NAME)
            params['org_id'] = self.file['org_id']
            params['email'] = row.get(EMAIL)
            params['old_password'] = SENSITIVE_DATA_PLACEHOLDER
            expected_tasks_params.append(params)

        assert_that(
            actual_tasks_params,
            contains_inanyorder(*expected_tasks_params)
        )

        assert_that(
            MailMigrationFileModel(self.main_connection).get(self.file['id']),
            none(),
        )
