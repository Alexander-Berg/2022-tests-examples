# -*- coding: utf-8 -*-
from testutils import (
    TestCase,
    create_group,
    get_all_admin_permssions,
    get_outer_admin_permissions,
)

from intranet.yandex_directory.src.yandex_directory.auth.user import User
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    group_permissions,
    global_permissions,
)


class Test_has_permissions(TestCase):
    def setUp(self):
        super(Test_has_permissions, self).setUp()
        self.auth_user = User(
            passport_uid=self.user['id'],
            ip='127.0.0.1'
        )

    def test_has_permissions_global(self):
        permissions = [
            global_permissions.add_groups,
        ]
        result = self.auth_user.has_permissions(
            self.meta_connection,
            self.main_connection,
            permissions,
            org_id=self.organization['id'],
        )
        self.assertTrue(result)

    def test_permissions_not_list(self):
        # тип парметра permissions должен быть list
        with self.assertRaises(AssertionError):
            self.auth_user.has_permissions(
                self.meta_connection,
                self.main_connection,
                global_permissions.add_users,
                org_id=self.organization['id'],
            )

    def test_has_permissions_non_existed(self):
        result = self.auth_user.has_permissions(
            self.meta_connection,
            self.main_connection,
            [global_permissions.add_groups, 'non_existed_permission'],
            org_id=self.organization['id'],
        )
        self.assertFalse(result)

    def test_has_permissions_for_object(self):
        uid = self.user['id']
        group = create_group(
            self.main_connection,
            self.organization['id'], type='generic',
            admins=[uid],
        )
        result = self.auth_user.has_permissions(
            self.meta_connection,
            self.main_connection,
            [group_permissions.edit],
            'group',
            group['id'],
            org_id=self.organization['id'],
        )
        self.assertTrue(result)

    def test_has_permissions_outer_admin(self):
        outer_admin_user = User(
            passport_uid=self.outer_admin['id'],
            ip='127.0.0.1',
        )
        outer_admin_permissions = get_outer_admin_permissions()
        outer_admin_permissions.remove(global_permissions.change_logo)
        result = outer_admin_user.has_permissions(
            self.meta_connection,
            self.main_connection,
            outer_admin_permissions,
            org_id=self.organization['id'],
        )
        self.assertTrue(result)

    def test_has_permissions_with_any_param(self):
        result = self.auth_user.has_permissions(
            self.meta_connection,
            self.main_connection,
            get_all_admin_permssions(),
            any_permission=True,
            org_id=self.organization['id'],
        )
        self.assertTrue(result)
