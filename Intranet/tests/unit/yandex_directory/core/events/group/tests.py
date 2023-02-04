# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    contains_inanyorder,
)

from testutils import (
    TestCase,
    datefield_to_isoformat,
)
from copy import deepcopy

from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.models.group import GroupModel
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.models.resource import ResourceModel
from intranet.yandex_directory.src.yandex_directory.core.actions import (
    action_group_modify,
    action_group_add,
)

from intranet.yandex_directory.src.yandex_directory.core.events.group import (
    _get_add_remove_members,
    on_group_membership_changed,
)

from intranet.yandex_directory.src.yandex_directory.core.events.group import(
    _get_all_resources_for_group,
    _get_all_resources_for_group_with_tech,
    _get_all_resources_for_parents,
)
from intranet.yandex_directory.src.yandex_directory.core.events.utils import (
    get_content_object,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_GROUP,
    TYPE_USER,
    TYPE_DEPARTMENT,
    TYPE_RESOURCE,
)


class TestGroupEvents(TestCase):
    def test_on_group_membership_changed(self):
        """
        Проверяет, что действие изменения состава группы(
        on_group_membership_changed) порождает нужные события
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
        ]
        assert_that(events, contains_inanyorder(*expected))


class TestGroupActions(TestCase):
    def test_add_empty_group(self):
        group_new = self.create_group()
        EventModel(self.main_connection).delete(force_remove_all=True)
        author_id = 1
        revision = action_group_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=group_new,
        )
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action['name'], 'group_add')
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'group_added',
        ]
        self.assertEqual(events, expected)

    def test_add_group_with_members(self):
        user = self.create_user()
        department = self.create_department()
        group = self.create_group()
        members = [
            {'type': TYPE_USER, 'object': user},
            {'type': TYPE_GROUP, 'object': group},
            {'type': TYPE_DEPARTMENT, 'object': department},
        ]

        group_new = self.create_group(members=members)
        EventModel(self.main_connection).delete(force_remove_all=True)
        author_id = 1
        revision = action_group_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=group_new,
        )
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action['name'], 'group_add')
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'group_added',
            'group_group_added',
            'user_group_added',
            'department_group_added',
        ])
        self.assertEqual(events, expected)
        resource_events = EventModel(self.main_connection).find(filter_data={'name': 'department_group_added'})
        e = resource_events[0]
        department = datefield_to_isoformat(department, 'created')
        self.assertEqual(e['object_type'], TYPE_DEPARTMENT)

        # Проверка временной поддержки старого поля.
        department['sub_members_count'] = department['members_count']

        self.assertEqual(e['object'], department)
        self.assertEqual(e['content']['subject']['id'], group_new['id'])
        self.assertEqual(e['content']['subject']['type'], group_new['type'])
        self.assertEqual(e['content']['directly'], False)

    def test_save_action_group_modify_change_property(self):
        # проверяем формат content для события group_property_changed
        group_old = self.create_group()
        # изменяем группу
        group_new = deepcopy(group_old)
        author_id = 1
        group_new['email'] = 'group_new@yandex-team.ru'
        group_new['type'] = 'not_generic'
        group_new['author_id'] = author_id

        EventModel(self.main_connection).delete(force_remove_all=True)
        revision = action_group_modify(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=group_new,
            old_object=group_old,
        )
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action['name'], 'group_modify')
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'group_property_changed',
        ])
        self.assertEqual(events, expected)
        resource_events = EventModel(self.main_connection).find(filter_data={'name': 'group_property_changed'})
        e = resource_events[0]
        self.assertEqual(len(resource_events), 1)
        self.assertEqual(e['object_type'], TYPE_GROUP)
        self.assertEqual(e['object']['id'], group_new['id'])
        assert_that(
            e['content']['diff'],
            has_entries(
                email=[group_old['email'], group_new['email']],
                author_id=[group_old['author_id'], group_new['author_id']],
                type=[group_old['type'], group_new['type']],
            )
        )

    def test_save_action_group_modify_add_members_without_relation(self):
        # добавляем в пустую группу разные типы member-ов без relations
        group_old = self.create_group()
        group_new = deepcopy(group_old)
        user = self.create_user()
        department = self.create_department()
        group = self.create_group()
        members = [
            {'type': TYPE_USER, 'id': user['id']},
            {'type': TYPE_GROUP, 'id': group['id']},
            {'type': TYPE_DEPARTMENT, 'id': department['id']},
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group_new['id'],
            data=dict(members=members)
        )
        group_new = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group_new['id'],
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
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        group_new_members = sorted([m['object']['id'] for m in group_new['members']])
        action_id_members = sorted([m['object']['id'] for m in action['object']['members']])

        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action_id_members, group_new_members)
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'group_modify')

        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'group_membership_changed',
            'user_group_added',
            'group_group_added',
            'department_group_added',
            'resource_grant_changed',  # изменился технический ресурс группы
        ])
        self.assertEqual(events, expected)

    def test_save_action_group_modify_add_members_with_relation(self):
        # добавляем в пустую группу разные типы member-ов c relations
        group_old = self.create_group()
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group_old['id']
                }
            ]
        )
        group_new = deepcopy(group_old)
        user = self.create_user()
        department = self.create_department()
        group = self.create_group()
        members = [
            {'type': TYPE_USER, 'id': user['id']},
            {'type': TYPE_GROUP, 'id': group['id']},
            {'type': TYPE_DEPARTMENT, 'id': department['id']},
        ]
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group_new['id'],
            data=dict(members=members)
        )
        group_new = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group_new['id'],
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
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        group_new_members = sorted([m['object']['id'] for m in group_new['members']])
        action_id_members = sorted([m['object']['id'] for m in action['object']['members']])

        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action_id_members, group_new_members)
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'group_modify')

        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'resource_grant_changed',  # изменился пользовательский ресурс
            'resource_grant_changed',  # изменился технический ресурс группы
            'group_membership_changed',
            'user_group_added',
            'group_group_added',
            'department_group_added',
        ])
        self.assertEqual(events, expected)

    def test_add_members_with_relation_to_embedded_group(self):
        """ Группа 1 входит в группу 2 и 3.
            Группа 2 и группа 3 входит в группу 4.
            Есть ресурс связанный с группой 4.
                       4 - R
                     /  \
                    2   3
                    \  /
                     1
            Добавляем пользователя в группу 1
        """
        group1 = self.create_group()
        group3 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group2 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group4 = self.create_group(members=[{'type': 'group',
                                             'object': group2},
                                            {'type': 'group',
                                             'object': group3},
                                            ])
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group4['id']
                }
            ]
        )

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
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        group_new_members = sorted([m['object']['id'] for m in group_new['members']])
        action_id_members = sorted([m['object']['id'] for m in action['object']['members']])

        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action_id_members, group_new_members)
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'group_modify')

        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'group_membership_changed',
            'resource_grant_changed',  # для технического ресурса группы 1
            'resource_grant_changed',  # для технического ресурса группы 2
            'resource_grant_changed',  # для технического ресурса группы 3
            'resource_grant_changed',  # для технического ресурса группы 4
            'resource_grant_changed',  # для пользовательского ресурса
            'user_group_added',
        ])

        self.assertEqual(events, expected)

    def test_save_action_group_modify_delete_members_with_relation(self):
        # удаляем разные типы member-ов c relations полностью из группы
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
        department_parent = self.create_department()
        department = self.create_department(parent_id=department_parent['id'])
        user0 = self.create_user(department_id=department_parent['id'])
        group1 = self.create_group()
        group2 = self.create_group()
        user1 = self.create_user(department_id=department['id'])
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
        action = ActionModel(self.main_connection).get_by_revision(revision, self.organization['id'])
        group_new_members = sorted([m['object']['id'] for m in group_new['members']])
        action_object_members = sorted([m['object']['id'] for m in action['object']['members']])

        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action_object_members, group_new_members)
        self.assertEqual(action_object_members, [])
        self.assertEqual(group_new_members, [])
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'group_modify')
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'resource_grant_changed',  # изменился пользовательский ресурс
            'resource_grant_changed',  # изменился технический ресурс группы
            'department_group_deleted',
            'group_group_deleted',
            'group_group_deleted',
            'group_membership_changed',
        ]
        assert_that(events, contains_inanyorder(*expected))

        resource_events = EventModel(self.main_connection).find(filter_data={'name': 'resource_grant_changed'})
        e = resource_events[0]
        e['content']['relations']['remove']['groups'] = sorted(e['content']['relations']['remove']['groups'])
        content_relation = {
            'relations': {
                'add': {
                    'users': [],
                    'departments': [],
                    'groups': [],
                },
                'remove': {
                    'users': [user0['id']],
                    'departments': [department_parent['id']],
                    'groups': [group1['id'], group2['id']],
                }
            },
            'uids': [user0['id'], user1['id']]
        }
        # Получим данные о ресурсе заново, чтобы в нём были
        # все нужные поля.
        resource = ResourceModel(self.main_connection) \
            .filter(id=resource['id']) \
            .fields('*', 'relations.*') \
            .one()
        self.assertEqual(e['object_type'], TYPE_RESOURCE)
        self.assertEqual(e['object'], resource)
        self.assertEqual(e['content'], content_relation)

    def test_save_action_group_modify_bulk_members_and_relations(self):
        # операции удаляения и добавления member-ов c relation у группы
        group_old = self.create_group()
        department1 = self.create_department()
        user0 = self.create_user()
        group1 = self.create_group(
            members=[{'type': TYPE_USER, 'object': user0}])
        user1, user2, user3 = (self.create_user(), self.create_user(),
                               self.create_user())
        old_members = [
            {'type': TYPE_USER, 'object': user1},
            {'type': TYPE_USER, 'object': user2},
            {'type': TYPE_USER, 'object': user3},
            {'type': TYPE_GROUP, 'object': group1},
            {'type': TYPE_DEPARTMENT, 'object': department1},
        ]
        resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {'name': 'write', 'group_id': group_old['id']},
                {'name': 'read', 'group_id': group_old['id']},
                {'name': 'admin', 'group_id': group_old['id']},
            ]
        )
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group_old['id'],
            data=dict(members=old_members)
        )
        group_old = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group_old['id'],
            fields=[
                '*',
                'members.*',
            ],
        )

        group_new = deepcopy(group_old)
        user4 = self.create_user()
        department2 = self.create_department()
        user5 = self.create_user(department_id=department2['id'])
        new_members = [
            {'type': TYPE_USER, 'object': user4},
            {'type': TYPE_USER, 'object': user2},
            {'type': TYPE_USER, 'object': user3},
            {'type': TYPE_DEPARTMENT, 'object': department1},
            {'type': TYPE_DEPARTMENT, 'object': department2},
        ]
        # Изначально, в команде состояли:
        # - user1
        # - user2
        # - user3
        # - group1
        # - department1

        # А теперь мы удаляем из команды:
        # - group1 (с user0);
        # - user1;
        # добавляем в команду:
        # - department2 (с user5);
        # - user4
        GroupModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            group_id=group_new['id'],
            data=dict(members=new_members)
        )
        group_new = GroupModel(self.main_connection).get(
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
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        self.assertTrue(action['id'])
        self.assertEqual(action['object']['id'], group_new['id'])
        self.assertEqual(action['object']['type'], group_new['type'])
        self.assertEqual(action['object']['label'], group_new['label'])
        group_new_members = sorted([m['object']['id'] for m in group_new['members']])
        action_object_members = sorted([m['object']['id'] for m in action['object']['members']])
        self.assertEqual(action_object_members, group_new_members)
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'group_modify')
        events = EventModel(self.main_connection).find(distinct=True)
        event_names = [x['name'] for x in events]

        expected = ['group_membership_changed', # изменился состав команды
                    'user_group_added',         # пользователь 4 был добавлен в команду
                    'department_group_added',   # отдел 2 был добавлен в команду
                    'user_group_deleted',       # пользователь 0 был удален из команды
                    'group_group_deleted',      # команда 1 была удалена из команды
                    'resource_grant_changed',   # ресурс изменился
                    'resource_grant_changed']   # тех-ресурс команды изменился

        self.assertEqual(event_names, expected)

        member_changed_events = EventModel(self.main_connection).find(filter_data={'name': 'group_membership_changed'})
        e = member_changed_events[0]
        content_members = {
            'diff': {
                'members': {
                    'add': {
                        'users': [user4['id']],
                        'departments': [department2['id']],
                        'groups': [],
                    },
                    'remove': {
                        'users': [user1['id']],
                        'departments': [],
                        'groups': [group1['id']],
                    }
                }
            },
            'subject': group_new,
            'directly': True,
        }
        self.assertEqual(e['object_type'], TYPE_GROUP)
        self.assertEqual(e['object']['id'], group_new['id'])
        self.assertEqual(e['content']['diff'], content_members['diff'])

        resource_grant_changed_events = EventModel(self.main_connection).find(
            filter_data={
                'name': 'resource_grant_changed',
                'object.service': 'autotest',
            })
        self.assertEqual(len(resource_grant_changed_events), 1)
        resource_event = resource_grant_changed_events[0]
        self.assertEqual(resource_event['object_type'], TYPE_RESOURCE)
        self.assertEqual(resource_event['object']['id'], resource['id'])
        assert_that(resource_event['content']['uids'],
                    contains_inanyorder(
                        user2['id'], user3['id'], user4['id'], user5['id']
                    ))
        # Событие для технического ресурса группы
        resource_grant_changed_events = EventModel(self.main_connection).find(
            filter_data={
                'name': 'resource_grant_changed',
                'object.service': 'directory',
            })
        self.assertEqual(len(resource_grant_changed_events), 1)


class TestEventsHelper(TestCase):
    def test_get_all_resources_for_group(self):
        # Группа 1 входит в группу 2 и 3.
        # Группа 2 и группа 3 входит в группу 4.
        # Есть ресурс связанный с группой 4.
        #            4 - R
        #          /  \
        #         2   3
        #         \  /
        #          1

        group1 = self.create_group()
        group3 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group2 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group4 = self.create_group(members=[{'type': 'group',
                                             'object': group2},
                                            {'type': 'group',
                                             'object': group3},
                                            ])
        resource_test = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group4['id']
                }
            ]
        )
        resources = _get_all_resources_for_group(
            self.main_connection,
            self.organization['id'],
            group1
        )
        # ожидаем получить resource_test, технический ресурс групп 4, 3, 2, 1
        self.assertEqual(len(resources), 4)
        resource_ids = [x['id'] for x in resources]
        assert_that(resource_ids, contains_inanyorder(
            resource_test['id'],
            group4['resource_id'],
            group3['resource_id'],
            group2['resource_id'],
        ))

    def test_get_all_resources_for_group_with_tech(self):
        """ Группа 1 входит в группу 2 и 3.
            Группа 2 и группа 3 входит в группу 4.
            Есть ресурс связанный с группой 4.
                       4 - R
                     /  \
                    2   3
                    \  /
                     1
        """
        group1 = self.create_group()
        group3 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group2 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group4 = self.create_group(members=[{'type': 'group',
                                             'object': group2},
                                            {'type': 'group',
                                             'object': group3},
                                            ])
        resource_test = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group4['id']
                }
            ]
        )
        resources = _get_all_resources_for_group_with_tech(
            self.main_connection,
            self.organization['id'],
            group1
        )
        # ожидаем получить resource_test, технический ресурс групп 4, 3, 2, 1
        self.assertEqual(len(resources), 5)
        resource_ids = [x['id'] for x in resources]
        assert_that(resource_ids, contains_inanyorder(
            resource_test['id'],
            group4['resource_id'],
            group3['resource_id'],
            group2['resource_id'],
            group1['resource_id'],
        ))

    def test_get_all_resources_for_parents_empty_list(self):
        """ В group1 входит group2
        """
        group2 = self.create_group()
        self.create_group(members=[{'type': 'group',
                                    'object': group2}])
        resources = _get_all_resources_for_parents(
            self.main_connection,
            self.organization['id'], group2['id']
        )
        self.assertEqual(resources, {})

    def test_get_all_resources_for_parents(self):
        """ Группа 1 входит в группу 2 и 3.
            Группа 2 и группа 3 входит в группу 4.
            Есть ресурс связанный с группой 4.
        """
        group1 = self.create_group()
        group3 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group2 = self.create_group(members=[{'type': 'group',
                                             'object': group1}])
        group4 = self.create_group(members=[{'type': 'group',
                                             'object': group2},
                                            {'type': 'group',
                                             'object': group3},
                                            ])
        resource_test = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'group_id': group4['id']
                }
            ]
        )
        resources = _get_all_resources_for_parents(
            self.main_connection,
            self.organization['id'], group1['id']
        )
        # ожидаем получить resource_test и технический ресурс группы 4
        self.assertEqual(len(list(resources.keys())), 2)
        assert_that(list(resources.keys()), contains_inanyorder(
            resource_test['id'],
            group4['resource_id']
        ))
