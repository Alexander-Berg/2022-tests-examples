# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    has_length,
)

from testutils import (
    TestCase,
    create_organization_without_domain,
    create_organization,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    UserMetaModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.users.dismiss import dismiss_user


class TestDismissUser(TestCase):

    def setUp(self):
        super(TestDismissUser, self).setUp()
        self.outer_admin_uid = 100500
        self.org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google',
            admin_uid=self.outer_admin_uid
        )['organization']
        self.new_org = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )['organization']
        self.add_user_by_invite(self.new_org, self.outer_admin_uid)
        self.process_tasks()

    def test_dismiss(self):
        # Проверяем, что пользватель будет уволен только в той организации, в которой его увольняют
        users_meta = UserMetaModel(self.meta_connection)\
                         .filter(id=self.outer_admin_uid, is_dismissed=False)\
                         .all()
        assert_that(
            users_meta,
            has_length(2)
        )
        users_main = UserModel(self.main_connection)\
            .filter(id=self.outer_admin_uid, is_dismissed=False)\
            .all()
        assert_that(
            users_main,
            has_length(1)
        )
        dismiss_user(
            self.meta_connection,
            self.main_connection,
            self.new_org['id'],
            self.outer_admin_uid,
            1,
            skip_disk=True,
            # skip_passport=True,
        )
        users_meta = UserMetaModel(self.meta_connection)\
                         .filter(id=self.outer_admin_uid, is_dismissed=False)\
                         .all()
        assert_that(
            users_meta,
            has_length(1)
        )
        assert_that(
            users_meta[0],
            has_entries(
                org_id=self.org['id'],
                is_outer=True,
            )
        )
        users_main = UserModel(self.main_connection)\
            .filter(id=self.outer_admin_uid, is_dismissed=False)\
            .all()
        assert_that(
            users_main,
            has_length(0)
        )
