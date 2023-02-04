# -*- coding: utf-8 -*-
from copy import deepcopy
from hamcrest import (
    contains,
    assert_that,
    equal_to,
    has_entries,
    contains_inanyorder,
)
from testutils import (
    TestCase,
)
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.actions import (
    action_service_license_change,
    action_group_modify,
)
from intranet.yandex_directory.src.yandex_directory.core.actions.service import (
    on_service_license_change,
)
from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.models.resource import ResourceModel
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    ServiceModel,
    enable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.events.group import (
    _get_add_remove_members,
    on_group_membership_changed,
)

from intranet.yandex_directory.src.yandex_directory.core.events.utils import (
    get_content_object,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_GROUP,
    TYPE_USER,
    TYPE_DEPARTMENT,
    TYPE_SERVICE
)


class TestServiceLicenseEvents(TestCase):
    """
    Проверяем, что события создаются при вызове action
    """
    def setUp(self):
        super(TestServiceLicenseEvents, self).setUp()

    def test_on_service_license_changed(self):
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        on_service_license_change(
            self.main_connection,
            self.organization['id'],
            revision,
            object_type=TYPE_SERVICE,
            object_value=self.service,
            content={},
        )

        events = EventModel(self.main_connection).find()
        assert_that(
            len(events),
            equal_to(1)
        )
        assert_that(
            events[0],
            has_entries(
                org_id=self.organization['id'],
                name=event.service_license_changed,
                content={},
            )
        )

    def test_action_service_license_change(self):
        author_id = 123
        EventModel(self.main_connection).delete(force_remove_all=True)
        revision = action_service_license_change(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=self.service,
        )

        action = ActionModel(self.main_connection).get_by_revision(
            revision,
            self.organization['id'],
        )
        self.assertTrue(action['id'])
        self.assertEqual(action['name'], 'service_license_change')
        events = EventModel(self.main_connection).find()
        assert_that(
            len(events),
            equal_to(1)
        )
        assert_that(
            events[0],
            has_entries(
                org_id=self.organization['id'],
                name=event.service_license_changed,
                content={},
            )
        )


class TestServiceLicenseEventsForGroup(TestCase):
    def setUp(self):
        super(TestServiceLicenseEventsForGroup, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            name='service',
            slug='service',
            client_id=123,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            service_slug=self.service['slug'],
        )

    def test_on_group_direct_membership_changed(self):
        """
        Проверяет, что действие изменения состава группы(
        on_group_membership_changed), на которую была выдана лицензия порождает событие service_license_changed
        """
        # добавляем в пустую группу разные типы member-ов c relations
        group = self.create_group()
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group['id']
                }
            ]
        )
        user = self.create_user()
        members = [
            {'type': TYPE_USER, 'id': user['id']},
        ]
        self.create_licenses_for_service(self.service['id'], group_ids=[group['id']])

        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group['id'],
            data=dict(members=members)
        )
        group_new = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=[
                '*',
                'members.*',
            ],
        )

        EventModel(self.main_connection).delete(force_remove_all=True)
        diff = {
            'members': _get_add_remove_members(
                group['members'],
                group_new['members'])
        }
        content = get_content_object(diff=diff)
        on_group_membership_changed(
            self.main_connection,
            org_id=self.organization['id'],
            revision=1,
            group=group,
            content=content,
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'resource_grant_changed',
            'resource_grant_changed',
            'user_group_added',
            'service_license_changed',
        ]
        assert_that(events, contains_inanyorder(*expected))

    def test_add_members_with_relation_to_embedded_group(self):
        """ Группа 1 входит в группу 2 и 3.
            Группа 2 и группа 3 входит в группу 4.
            Есть лицензии на сервис у группы 3.
                       3 - service
                     /
                    2
                    \
                     1
            Добавляем пользователя в группу 1, ожидаем событие об изменении лицензий сервиса
        """
        group1 = self.create_group()
        group2 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group3 = self.create_group(members=[{'type': 'group',
                                             'object': group2},
                                            ])
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group3['id']
                }
            ]
        )

        # выдаем группе 3 лицензии на сервис
        self.create_licenses_for_service(self.service['id'], group_ids=[group3['id']])

        group_old = deepcopy(group1)

        user = self.create_user()
        members = [
            {'type': TYPE_USER, 'id': user['id']},
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group1['id'],
            data=dict(members=members)
        )
        group_new = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group1['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        author_id = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        revision = action_group_modify(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=group_new,
            old_object=group_old,
        )

        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'group_membership_changed',
            'resource_grant_changed',  # для технического ресурса группы 1
            'resource_grant_changed',  # для технического ресурса группы 2
            'resource_grant_changed',  # для технического ресурса группы 3
            'resource_grant_changed',  # для пользовательского ресурса
            'user_group_added',
            'service_license_changed',
        ])

        self.assertEqual(events, expected)

    def test_save_action_group_modify_delete_members_with_relation(self):
        """
        Удаляем разные типы member-ов c relations полностью из группы, у которой есть лицензии на сервис,
        проверяем, что вызывается событие service_license_changed
        """
        group_old = self.create_group()
        group_new = deepcopy(group_old)
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'write',
                    'group_id': group_old['id']
                }
            ]
        )
        # выдаем группе лицензии на сервис
        self.create_licenses_for_service(self.service['id'], group_ids=[group_old['id']])

        department_parent = self.create_department()
        department = self.create_department(parent_id=department_parent['id'])
        group1 = self.create_group()
        group2 = self.create_group()

        # создаем еще один сервис и выдаем лицензии отделу
        another_service = ServiceModel(self.meta_connection).create(
            name='another_service',
            slug='another_service',
            client_id=123456,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            service_slug=another_service['slug'],
        )
        self.create_licenses_for_service(another_service['id'], department_ids=[department['id']])

        members = [
            {'type': TYPE_GROUP, 'id': group1['id']},
            {'type': TYPE_GROUP, 'id': group2['id']},
            {'type': TYPE_DEPARTMENT, 'id': department_parent['id']},
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group_old['id'],
            data=dict(members=members)
        )
        group_old = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group_old['id'],
            fields=[
                '*',
                'members.*',
            ],
        )

        author_id = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        revision = action_group_modify(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=group_new,
            old_object=group_old,
        )

        license_events = EventModel(self.main_connection).find(filter_data={'name': 'service_license_changed'})
        # проверим, что событие изменения лицензии только одно и связано с сервисом self.service
        self.assertEqual(len(license_events), 1)
        e = license_events[0]
        self.assertEqual(e['object'], self.service)


class TestServiceWithResponsible(TestCase):
    def test_responsible_in_event(self):
        org_id = self.organization['id']
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=org_id,
            service_slug=self.service['slug'],
            responsible_id=self.user['id'],
        )
        events = EventModel(self.main_connection) \
                 .filter(org_id=org_id, name='service_enabled') \
                 .all()

        assert_that(
            events,
            contains(
                has_entries(
                    object=has_entries(
                        responsible_id=self.user['id']
                    ),
                    content=has_entries(
                        responsible_id=self.user['id']
                    ),
                )
            )
        )
