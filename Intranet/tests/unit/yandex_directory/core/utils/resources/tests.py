# coding: utf-8

from hamcrest import (
    assert_that,
    contains,
    has_entries,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ResourceModel,
    UserModel,
    ActionModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.resources import create_resources


class TestResourceCreation(TestCase):
    def test_resource_for_resource_creation(self):
        resource_id = 'foo bar'
        relations = [
            {'object_type': 'user', 'object_id': self.user['id'], 'name': 'admin'},
        ]
        org_id = self.organization['id']

        create_resources(
            self.main_connection,
            org_id=org_id,
            service_slug=self.service['slug'],
            external_id=resource_id,
            relations=relations,
        )

        # Проверим, что ресурс завёлся
        resource = ResourceModel(self.main_connection) \
            .filter(external_id=resource_id) \
            .one()
        assert resource is not None

        # Проверим, что связи установились как надо
        users = UserModel(self.main_connection) \
                .filter(resource=resource_id, resource_service=self.service['slug']) \
                .fields('nickname') \
                .all()
        assert_that(
            users,
            contains(
                has_entries(nickname=self.user['nickname'])
            )
        )

        # Так же, action про создание ресурса должен был быть создан
        actions = ActionModel(self.main_connection) \
                .filter(org_id=org_id) \
                .fields('name') \
                .all()
        assert_that(
            actions,
            contains(
                has_entries(name='resource_add'),
            )
        )
