# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    has_item,
)

from testutils import (
    TestCase,
    tvm2_auth_success,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationAccessRestoreModel

TVM2_HEADERS = {'X-Ya-Service-Ticket': 'qqq'}


class TestAdminOrganizationAccessRestoreListView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationAccessRestoreListView, self).setUp(*args, **kwargs)

        self.access_restore = OrganizationAccessRestoreModel(self.meta_connection).create(
            org_id=self.organization['id'],
            domain=self.organization_domain,
            new_admin_uid=123,
            old_admin_uid=self.admin_uid,
            ip='127.0.0.1',
            control_answers={
                "users": ["info@domain.com"],
                "admins": [],
                "no_users": False,
                "maillists": [],
                "no_maillists": False,
                "forgot_admins": True
            }
        )
        self.access_restore['created_at'] = self.access_restore['created_at'].isoformat()
        self.access_restore['expires_at'] = self.access_restore['expires_at'].isoformat()

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_me(self):
        response = self.get_json(
            '/admin/organizations/{org_id}/access-restores/'.format(org_id=self.organization['id']),
            headers=TVM2_HEADERS,
        )
        assert_that(
            response['result'],
            has_item(
                has_entries(
                    **self.access_restore
                )
            )
        )
