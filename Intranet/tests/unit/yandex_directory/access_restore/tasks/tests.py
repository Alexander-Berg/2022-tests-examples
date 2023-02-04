# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
)
from unittest.mock import (
    patch,
    ANY,
)

from testutils import (
    TestCase,
    override_settings,
)

from intranet.yandex_directory.src.yandex_directory import app

from intranet.yandex_directory.src.yandex_directory.core.models.access_restore import RestoreTypes
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationAccessRestoreModel,
    TaskModel,
)
from intranet.yandex_directory.src.yandex_directory.access_restore.tasks import (
    AccessRestoreTask,
    AfterAccessRestoreEmailTask,

)
from intranet.yandex_directory.src.yandex_directory.core.task_queue.base import SyncResult
from intranet.yandex_directory.src.yandex_directory.core.utils.tasks import (
    ChangeOrganizationOwnerTask,
    UpdateOrganizationMembersCountTask,
)
from intranet.yandex_directory.src.yandex_directory.core.events.tasks import UpdateMembersCountTask
from intranet.yandex_directory.src.yandex_directory.core.tasks import SyncExternalIDS


class TestAccessRestoreTask(TestCase):

    def setUp(self):
        super(TestAccessRestoreTask, self).setUp()

        TaskModel(self.main_connection).delete(force_remove_all=True)

        self.new_admin_uid = 123
        create_params = {
            'domain': self.organization_domain,
            'new_admin_uid': self.new_admin_uid,
            'old_admin_uid': self.admin_uid,
            'org_id': self.organization['id'],
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**create_params)

    def test_failed(self):
        # при передаче владения произошла ошибка
        restore_task_id = AccessRestoreTask(self.main_connection).delay(
            restore_id=self.first_restore['id'],
            org_id=self.first_restore['org_id'],
        ).task_id

        with patch.object(ChangeOrganizationOwnerTask, 'do', side_effect=Exception):
            #  выполним задачу в сихронном режиме
            SyncResult(self.main_connection, restore_task_id)

        # переведем передачу в статус failed и сохранили id задачи
        assert_that(
            OrganizationAccessRestoreModel(self.meta_connection).get(self.first_restore['id']),
            has_entries(
                state=RestoreTypes.failed,
                restore_task_id=restore_task_id,
            )
        )

    def test_success(self):
        # удачно передаем  владение
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': 123,
            'default_email': 'default@ya.ru',
        }
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.tasks.get_domain_info_from_blackbox',
                   return_value={'admin_id': 123, 'domain_id': 123}):
            org_id = self.first_restore['org_id']
            restore_id = self.first_restore['id']

            with patch.object(AfterAccessRestoreEmailTask, 'delay') as task:
                with patch.object(UpdateMembersCountTask, 'delay'):
                    with patch.object(SyncExternalIDS, 'delay'):
                        with patch.object(UpdateOrganizationMembersCountTask, 'delay'):
                            restore_task_id = AccessRestoreTask(self.main_connection).delay(
                                restore_id=restore_id,
                                org_id=org_id,
                            ).task_id
                            # выполним задачу в сихронном режиме
                            self.process_tasks()

                            self.assert_no_failed_tasks()

                            # задача на отпраку писем после получения владения
                            task.assert_called_once_with(
                                org_id=org_id,
                                restore_id=restore_id,
                            )

            # Перевели передачу в статус success  и сохранили id задачи
            assert_that(
                OrganizationAccessRestoreModel(self.meta_connection).get(restore_id),
                has_entries(
                    state=RestoreTypes.success,
                    restore_task_id=restore_task_id,
                )
            )

class TestAfterAccessRestoreEmailTask(TestCase):

    def setUp(self):
        super(TestAfterAccessRestoreEmailTask, self).setUp()

        TaskModel(self.main_connection).delete(force_remove_all=True)

        self.new_admin_uid = 123
        create_params = {
            'domain': self.organization_domain,
            'new_admin_uid': self.new_admin_uid,
            'old_admin_uid': self.admin_uid,
            'org_id': self.organization['id'],
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**create_params)

    def test_send_emails(self):
        # отправлям письма новому владельцу и админам организации
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.access_restore._send_email') as send_email, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.access_restore.get_user_data_from_blackbox_by_uid') as bb:
            bb.return_value = {'default_email': 'test.email@yandex.com'}
            # выполним на задачу на отправку писем
            org_id = self.organization['id']
            AfterAccessRestoreEmailTask(self.main_connection) \
                .delay(
                    restore_id= self.first_restore['id'],
                    org_id=org_id,
                )
            self.process_tasks()

        # письмо о получении владения
        send_email.assert_any_call(
            self.meta_connection,
            self.main_connection,
            org_id,
            self.new_admin_uid,
            ANY,
            self.first_restore['domain'],
            app.config['SENDER_CAMPAIGN_SLUG']['ACCESS_RESTORE_SUCCESS_RESTORE'],
            organization_name=ANY,
            ip=self.first_restore['ip'],
            date=self.first_restore['created_at'].isoformat(),
        )

        # письмо о смене владельца
        send_email.assert_any_call(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            ANY,
            ANY,
            self.first_restore['domain'],
            app.config['SENDER_CAMPAIGN_SLUG']['ACCESS_RESTORE_SOME_RESTORE'],
            organization_name=ANY,
            ip=self.first_restore['ip'],
            date=self.first_restore['created_at'].isoformat(),
        )
