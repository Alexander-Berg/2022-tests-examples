# coding: utf-8

from hamcrest import (
    assert_that,
    equal_to,
    has_key,
)

from testutils import (
    TestCase,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.users import responsible
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import OrganizationServiceModel


class TestResponsibleCase(TestCase):
    def test_when_users_are_not_responsible(self):
        # пользователи не ответственные - ответ пустой
        result = responsible._get_services_user_responsible_for_on_this_shard(self.main_connection, self.user['id'])
        assert_that(result, has_key(self.user['id']))
        assert_that(
            result[self.user['id']],
            equal_to([])
        )

    def test_when_users_are_responsible(self):
        # пользователи ответственные - ответ содержит их OrganizationServiceModel
        enable_service(
            self.meta_connection, self.main_connection,
            org_id=self.user['org_id'],
            service_slug=self.service['slug']
        )
        OrganizationServiceModel(self.main_connection).change_responsible(
            org_id=self.user['org_id'],
            service_id=self.service['id'],
            responsible_id=self.user['id']
        )
        result = responsible._get_services_user_responsible_for_on_this_shard(self.main_connection, self.user['id'])
        assert_that(result, has_key(self.user['id']))
        assert_that(
            result[self.user['id']],
            equal_to([(self.service['id'], self.user['org_id'])])
        )
