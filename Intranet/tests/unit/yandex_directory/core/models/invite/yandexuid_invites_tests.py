# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    not_none,
    has_entries,
    has_length,
    contains_inanyorder,
)

from testutils import (
    TestCase,
    create_yandex_user,
    create_organization_without_domain,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    InviteModel,
    UsedInviteModel,
)


class TestYndexuidInviteModel(TestCase):
    def setUp(self):
        super(TestYndexuidInviteModel, self).setUp()
        self.org_id = self.organization['id']
        self.department_id = self.department['id']

        self.another_org_id = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )['organization']['id']

        self.author_id = 10
        self.invite_code_1 = InviteModel(self.meta_connection).create(
            self.org_id,
            self.department_id,
            self.author_id,
        )
        self.invite_code_2 = InviteModel(self.meta_connection).create(
            self.org_id,
            self.department_id,
            self.author_id,
        )
        self.new_user_uid_1 = 1
        create_yandex_user(
            self.meta_connection,
            self.main_connection,
            self.new_user_uid_1,
            self.organization['id'],
            email='user_1@yandex.ru',
            nickname='user_1',
        )
        self.new_user_uid_2 = 2
        create_yandex_user(
            self.meta_connection,
            self.main_connection,
            self.new_user_uid_2,
            self.organization['id'],
            email='user_2@yandex.ru',
            nickname='user_2',
        )

    def create_test_invites(self):
        UsedInviteModel(self.meta_connection).create(
            self.invite_code_1,
            self.new_user_uid_1,
            self.organization['id'],
        )
        UsedInviteModel(self.meta_connection).create(
            self.invite_code_1,
            self.new_user_uid_2,
            self.organization['id'],
        )

    def test_create(self):

        UsedInviteModel(self.meta_connection).create(
            self.invite_code_1,
            self.new_user_uid_1,
            self.organization['id'],
        )

        assert_that(
            UsedInviteModel(self.meta_connection).count(),
            equal_to(1)
        )
        assert_that(
            UsedInviteModel(self.meta_connection).find({'code': self.invite_code_1})[0],
            has_entries(
                code=self.invite_code_1,
                user_id=self.new_user_uid_1,
                org_id=self.organization['id'],
                created_at=not_none(),
            )
        )

    def test_find(self):
        # Проверяем что находятся все записи с заданными параметрами
        self.create_test_invites()
        reponse = UsedInviteModel(self.meta_connection).find({'code': self.invite_code_1})

        assert_that(
            reponse,
            has_length(2)
        )

        assert_that(
            reponse,
            contains_inanyorder(
                has_entries(
                    code=self.invite_code_1,
                    user_id=self.new_user_uid_1,
                    org_id=self.organization['id'],
                    created_at=not_none(),
                ),
                has_entries(
                    code=self.invite_code_1,
                    user_id=self.new_user_uid_2,
                    org_id=self.organization['id'],
                    created_at=not_none(),
                )
            )
        )

    def test_get(self):
        # Проверяем что находится запись по первичному ключу
        self.create_test_invites()
        assert_that(
            UsedInviteModel(self.meta_connection).get(self.invite_code_1, self.new_user_uid_1),
            has_entries(
                code=self.invite_code_1,
                user_id=self.new_user_uid_1,
                org_id=self.organization['id'],
                created_at=not_none(),
            )
        )
