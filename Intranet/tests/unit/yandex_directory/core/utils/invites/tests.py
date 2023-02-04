# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    not_none,
)

from testutils import (
    TestCase,
    create_organization_without_domain,
    create_yandex_user,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID
from intranet.yandex_directory.src.yandex_directory.core.models import (
    InviteModel,
    UsedInviteModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.invite import use_invite


class TestInvitesUtils(TestCase):

    def setUp(self):
        super(TestInvitesUtils, self).setUp()
        self.new_org_id = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )['organization']['id']
        self.user_id = 111
        create_yandex_user(self.meta_connection, self.main_connection, self.user_id, self.new_org_id)
        self.counter = 2
        self.code = InviteModel(self.meta_connection).create(self.new_org_id, ROOT_DEPARTMENT_ID, 1, self.counter)

    def test_use(self):
        # Проверяем, что обновится инвайт и добавится новая запись в таблицу yandexuid_invites
        invite = InviteModel(self.meta_connection).get(self.code)
        use_invite(self.meta_connection, invite, self.user_id)
        assert_that(
            InviteModel(self.meta_connection).get(self.code),
            has_entries(
                counter=self.counter-1,
                last_use=not_none(),
            )
        )
        assert_that(
            UsedInviteModel(self.meta_connection).get(self.code, self.user_id),
            has_entries(
                user_id=self.user_id,
                code=self.code,
                org_id=self.new_org_id,
                created_at=not_none(),
            )
        )
