# coding: utf-8

from hamcrest import (
    assert_that,
    contains,
    equal_to,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.utils import only_ids
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    OrganizationModel,
    OrganizationRevisionCounterModel,
)


class TestActionCase(TestCase):
    def test_if_order_can_be_changed(self):
        org_id = self.organization['id']
        m = ActionModel(self.main_connection)
        a1 = m.create(org_id, 'test', 1, {}, 'user')['id']
        a2 = m.create(org_id, 'test', 1, {}, 'user')['id']
        a3 = m.create(org_id, 'test', 1, {}, 'user')['id']

        # проверяем, что порядок "по-умолчанию" по id
        response = only_ids(m.find())
        assert_that(response, contains(a1, a2, a3))

        response = only_ids(m.find(order_by='id'))
        assert_that(response, contains(a1, a2, a3))

        response = only_ids(m.find(order_by='-id'))
        assert_that(response, contains(a3, a2, a1))

    def test_find_by_revision(self):
        # ищем действие по номеру ревизии

        org_id = self.organization['id']
        m = ActionModel(self.main_connection)
        a1 = m.create(org_id, 'test', 1, {}, 'user')

        response = only_ids(m.find({'revision': a1['revision'], 'org_id': org_id}))
        assert_that(response, contains(a1['id']))

    def test_should_increment_organization_revision(self):
        # проверим, что действие увеличивает ревизию организации

        action_model = ActionModel(self.main_connection)

        # создадим несколько ActionModel
        action_model.create(self.organization['id'], 'test', 1, {}, 'user')
        for i in range(10):
            action_model.create(self.organization['id'], 'test', 1, {}, 'user')

        organization_before_action = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['revision'],
        )

        # теперь запишем действие, оно должно увеличить ревизию на 1
        action_model.create(self.organization['id'], 'test', 1, {}, 'user')

        organization_after_action = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['revision'],
        )

        assert_that(
            organization_after_action['revision'],
            equal_to(organization_before_action['revision'] + 1),
        )
