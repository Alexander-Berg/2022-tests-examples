# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    not_,
    equal_to,
    none,
    has_item,
    calling,
    raises,
)

from testutils import (
    TestCase,
    override_settings,
    assert_called_once,
    assert_not_called,
)

from intranet.yandex_directory.src.yandex_directory.core.models import(
    OrganizationModel,
    UserMetaModel,
    UserModel,
    ActionModel,
)
from unittest.mock import patch, ANY
from intranet.yandex_directory.src.yandex_directory.core.task_queue.exceptions import DuplicatedTask
from intranet.yandex_directory.src.yandex_directory.core.utils.users.base import create_portal_user
from intranet.yandex_directory.src.yandex_directory.core.utils.tasks import (
    ChangeOrganizationOwnerTask,
    CreateRobotTask,
)
from intranet.yandex_directory.src.yandex_directory.core.task_queue import TASK_STATES


class TestChangeOrganizationOwnerTask(TestCase):

    def setUp(self):
        super(TestChangeOrganizationOwnerTask, self).setUp()
        self.task = ChangeOrganizationOwnerTask(self.main_connection)
        self.new_inner_admin_id = self.create_user(
            nickname='new-admin'
        )['id']
        self.new_outer_admin_id = 100001

    def test_is_new_owner_uid_valid(self):
        # прверяем uid новго админа на допустимость
        org_id = self.organization['id']
        assert_that(
            self.task._is_new_owner_uid_valid(org_id, self.user['id'], self.new_inner_admin_id),
            equal_to(True)
        )

        assert_that(
            self.task._is_new_owner_uid_valid(org_id, self.user['id'], self.new_outer_admin_id),
            equal_to(True)
        )

        pdd_uid = 111*10**13 + 1000
        assert_that(
            self.task._is_new_owner_uid_valid(org_id, self.user['id'], pdd_uid),
            equal_to(False)
        )

    def assert_ownership_moved(self):
        org_id = self.organization['id']

        assert_that(
            OrganizationModel(self.main_connection).filter(id=org_id).one(),
            has_entries(
                {'admin_uid': self.new_outer_admin_id}
            )
        )

        assert_that(
            UserMetaModel(self.meta_connection).filter(org_id=org_id, id=self.new_outer_admin_id).one(),
            has_entries(
                {'user_type': 'inner_user'}
            )
        )

    def test_update_connect_outer_to_outer(self):
        # передаем владение домена внешнему админа
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': self.new_outer_admin_id,
            'default_email': 'default@ya.ru',
        }

        self.task._update_connect(
            org_id=self.organization['id'],
            old_owner_uid=self.admin_uid,
            new_owner_uid=self.new_outer_admin_id,
        )
        self.assert_ownership_moved()

    def test_update_connect_outer_to_inner_portal_user(self):
        # передаем владение домена от внешнего админа учётке, которая является сотрудником, и при этом портальная

        # Сначала сделаем его портальным:
        new_owner_uid = self.new_outer_admin_id
        org_id = self.organization['id']

        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': new_owner_uid,
            'default_email': 'default@ya.ru',
        }
        create_portal_user(
            self.meta_connection,
            self.main_connection,
            uid=new_owner_uid,
            org_id=org_id,
        )
        assert_that(
            UserMetaModel(self.meta_connection).filter(id=new_owner_uid).one(),
            has_entries(user_type='inner_user')
        )

        # Делаем действие
        self.task._update_connect(
            org_id=org_id,
            old_owner_uid=self.admin_uid,
            new_owner_uid=new_owner_uid,
        )

        self.assert_ownership_moved()

    def test_update_connect_outer_to_outer_deputy(self):
        # передаем владение домена внешнему заместителю админа
        outer_deputy = self.create_deputy_admin()
        assert_that(
            UserMetaModel(self.meta_connection).get_outer_deputy_admins(org_id=self.organization['id']),
            equal_to([{
                'id': outer_deputy['id'],
                'user_type': 'deputy_admin',
                'is_outer': True,
                'org_id': self.organization['id'],
                'is_dismissed': False,
                'created': ANY,
                'cloud_uid': None,
            }])
        )
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': outer_deputy['id'],
            'default_email': 'default@ya.ru',
        }
        self.task._update_connect(
            org_id=self.organization['id'],
            old_owner_uid=self.admin_uid,
            new_owner_uid=outer_deputy['id'],
        )

        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.organization['id']).one(),
            has_entries(
                {'admin_uid': outer_deputy['id']}
            )
        )

        assert_that(
            UserMetaModel(self.meta_connection).filter(org_id=self.organization['id'], id=outer_deputy['id']).one(),
            has_entries(
                {'user_type': 'inner_user'}
            )
        )


    def test_update_connect_outer_to_inner(self):
        # передаем владение домена  сотруднику организации
        self.task._update_connect(
            org_id=self.organization['id'],
            old_owner_uid=self.user['id'],
            new_owner_uid=self.new_inner_admin_id,
        )

        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.organization['id']).one(),
            has_entries(
                {'admin_uid': self.new_inner_admin_id}
            )
        )


        assert_that(
            UserMetaModel(self.meta_connection) \
                .filter(org_id=self.organization['id'], id=self.new_inner_admin_id) \
                .one(),
            has_entries(
                {'user_type': 'inner_user'}
            )
        )

        assert_that(
            UserModel(self.main_connection).get_organization_admins(self.organization['id']),
            has_item(
                has_entries(
                    {'id': self.new_inner_admin_id}
                )
            )
        )

        assert_that(
            UserModel(self.main_connection).get_organization_admins(self.organization['id']),
            has_item(
                has_entries(
                    {'id': self.user['id']}
                )
            )
        )

    def test_not_allowed_duplicate_task(self):
        # запрещено создавать несоклько задач для одной организации
        ChangeOrganizationOwnerTask(self.main_connection).delay(org_id=123, param=1)

        assert_that(
            calling(
                ChangeOrganizationOwnerTask(self.main_connection).delay
            ).with_args(org_id=123, param=2),
            raises(DuplicatedTask)
        )

    def test_new_admin_already_owner_in_passport(self):
        # если в паспорте новый админ уже владелец, то так успешно завершается
        new_admin_login = 'new-admin@yandex.ru'
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_user_data_from_blackbox_by_uid',
                   return_value={'login': new_admin_login}), \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.tasks.get_domain_info_from_blackbox',
                      return_value={'admin_id': self.new_inner_admin_id, 'domain_id': 123}):

            ChangeOrganizationOwnerTask(self.main_connection).delay(
                org_id=self.organization['id'],
                old_owner_uid=self.user['id'],
                new_owner_uid=self.new_inner_admin_id,
            )
            self.process_tasks()

            assert_that(
                UserModel(self.main_connection).get_organization_admins(self.organization['id']),
                has_item(
                    has_entries(
                        {'id': self.new_inner_admin_id}
                    )
                )
            )

    def test_make_action(self):
        new_admin_login = 'new-admin@yandex.ru'
        with patch('intranet.yandex_directory.src.yandex_directory.common.utils.get_user_data_from_blackbox_by_uid',
                   return_value={'login': new_admin_login}), \
             patch('intranet.yandex_directory.src.yandex_directory.core.utils.tasks.get_domain_info_from_blackbox',
                   return_value={'admin_id': self.user['id'], 'domain_id': 123}):
            ChangeOrganizationOwnerTask(self.main_connection).delay(
                org_id=self.organization['id'],
                old_owner_uid=self.user['id'],
                new_owner_uid=self.new_inner_admin_id,
            )

            self.process_tasks()

        action = ActionModel(self.main_connection).filter(name='organization_owner_change').all()

        assert_that(
            len(action),
            equal_to(1)
        )

        assert_that(
            action[0],
            has_entries(
                old_object=has_entries(
                    admin_uid=self.user['id']
                ),
                object=has_entries(
                    admin_uid=self.new_inner_admin_id
                ),
                object_type='organization'
            )
        )


class TestCreateRobotTask(TestCase):
    def test_maillist_check_does_not_fail_on_nonexistent_organization(self):
        task = CreateRobotTask(self.main_connection).delay(
            org_id=100500,
            service_slug='service',
        )
        self.process_tasks()

        assert_that(
            task.state,
            equal_to(TASK_STATES.success),
        )
