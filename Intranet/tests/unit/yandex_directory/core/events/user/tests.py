# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    contains_inanyorder,
)

from testutils import (
    TestCase,
    create_robot_for_anothertest,
    datefield_to_isoformat,
    change_user_data_created_fields_to_isoformat,
)
from unittest.mock import patch
from copy import deepcopy

from intranet.yandex_directory.src.yandex_directory.core.models.action import ActionModel
from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel
from intranet.yandex_directory.src.yandex_directory.core.models.event import EventModel
from intranet.yandex_directory.src.yandex_directory.core.models.resource import (
    ResourceModel,
    ResourceRelationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import (
    action_user_add,
    action_user_modify,
    action_department_add,
)
from intranet.yandex_directory.src.yandex_directory.core.actions.user import (
    on_user_add,
    on_user_modify,
    on_user_dismiss,
)

from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_USER,
    TYPE_RESOURCE,
    TYPE_DEPARTMENT,
)


def prepare_user_created_field_(user):
    """
    Перобразуем поле created у пользователя и отдела пользователя из datetime в строку в iso формате
    :param user: данные о пользователе
    """
    u = deepcopy(user)
    u['created'] = u['created'].isoformat()
    u['department']['created'] = u['department']['created'].isoformat()
    return u


class TestUserEvents(TestCase):
    # Пока базовый тест-кейс на создаёт рассылку для рутового отдела
    root_dep_label = 'all'

    def setUp(self):
        super(TestUserEvents, self).setUp()

    def test_save_action_user_add(self):
        user = self.create_user()
        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='autotest',
            relations=[
                {
                    'name': 'read',
                    'user_id': user['id']
                }
            ]
        )
        author_id = 1
        revision = action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=user
        )
        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        self.assertTrue(action['id'])
        assert_that(
            action['object'],
            has_entries(
                **change_user_data_created_fields_to_isoformat(user)
            )
        )
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'user_add')
        revision = action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=user
        )

    def test_user_should_be_subscribed_to_department_maillist_after_creation(self):
        # Проверяем, что новый пользователь подписывается только на рассылку
        # того отдела, в который добавлен.

        department = self.create_department(label='some-department')
        user = self.create_user(department['id'])

        # Сэмитируем действия по созданию отдела и пользователя
        action_department_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=department,
        )
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )

    def test_change_maillist_when_users_department_was_changed(self):
        first_department = self.create_department(label='first')
        second_department = self.create_department(label='second')
        user = self.create_user(first_department['id'])
        moved_user = deepcopy(user)
        moved_user['department_id'] = second_department['id']

        # создаем два департамента
        for department in [first_department, second_department]:
            action_department_add(
                self.main_connection,
                org_id=self.organization['id'],
                author_id=user['id'],
                object_value=department,
            )

        # Имитируем добавление пользователя
        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=user
        )

        # Имитируем перемещение пользователя в другой департамент
        action_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=user['id'],
            object_value=moved_user,
            old_object=user,
        )

    def test_save_action_on_user_move(self):
        # проверим какие события сгенерятся при
        # переводе пользователя в другой отдел
        department_one = self.create_department()
        department_two = self.create_department()
        user = self.create_user(department_one['id'])
        new_user = deepcopy(user)
        author_id = 1

        # сначала подчистим список событий
        EventModel(self.main_connection).delete(force_remove_all=True)
        user = UserModel(self.main_connection).get(
            user['id'],
            org_id=self.organization['id'],
            fields=[
                '*',
                'department.*',
                'groups.*',
            ],
        )
        new_user['department_id'] = department_two['id']
        new_user['department']['id'] = department_two['id']
        revision = action_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=author_id,
            object_value=new_user,
            old_object=user,
        )

        new_user = change_user_data_created_fields_to_isoformat(new_user)

        action = ActionModel(self.main_connection).get_by_revision(
            revision, self.organization['id'])
        self.assertTrue(action['id'])
        self.assertEqual(action['object'], new_user)
        self.assertEqual(action['author_id'], author_id)
        self.assertEqual(action['name'], 'user_modify')
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        # проверим, что из одного департамента пользователь удалился,
        # в другой добавился, и ещё было более generic событие про то
        # что пользователя переместили
        expected = [
            'department_user_added',
            'department_user_deleted',
            'user_moved',
        ]
        self.assertEqual(events, expected)

    def test_on_user_add(self):
        user = self.create_user()
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        on_user_add(
            self.main_connection,
            self.organization['id'],
            revision,
            user
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_user_added',
            'user_added',
        ]
        assert_that(events, equal_to(expected))

    def test_resource_changed_after_user_added(self):
        department = self.create_department()
        resource = self.create_resource_with_department(
            department['id'], 'read')
        user = self.create_user(department['id'])
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        on_user_add(
            self.main_connection,
            self.organization['id'],
            revision,
            user
        )
        all_events = EventModel(self.main_connection).find()
        events = [x['name'] for x in all_events]
        events.sort()
        expected = [
            # пользователь добавился в свой отдел
            'department_user_added',
            # и в "опосредованно" в корневой
            'department_user_added',
            # получил доступ к ресурсу
            'resource_grant_changed',
            'user_added',
        ]
        assert_that(events, equal_to(expected))
        event = EventModel(self.main_connection).find({'name': 'resource_grant_changed'})[0]
        relations_add = event['content']['relations']['add']
        relations_delete = event['content']['relations']['remove']
        expected_relation = {
            'object_id': user['id'],
            'relation_name': 'read',
        }
        assert_that(
            relations_delete,
            has_entries(users=[],
                        departments=[],
                        groups=[],
                        ))
        assert_that(
            relations_add,
            has_entries(users=[expected_relation, ],
                        departments=[],
                        groups=[],
                        ))
        assert_that(event['content']['uids'], [user['id']])
        assert_that(
            event,
            has_entries(name='resource_grant_changed',
                        org_id=self.organization['id'],
                        revision=revision,
                        object=resource,
            )
        )

    def test_resource_via_group_changed_after_user_added(self):
        # Добавляем сотрудника в департамент, который входит в группу.
        # Проверяем, что сохранились события изменения состава технического
        # ресурса группы

        # TODO: Тут, видимо, надо еще генерить событие group_member_changed
        department = self.create_department()
        group = self.create_group(
            members=[{'type': 'department', 'object': department}]
        )
        resource = self.create_resource_with_group(group['id'])

        user = self.create_user(department['id'])
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        on_user_add(
            self.main_connection,
            self.organization['id'],
            revision,
            user
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = sorted([
            'department_user_added',  # добавление в department
            'department_user_added',  # добавление в root
            'resource_grant_changed',  # изменение resource
            'resource_grant_changed',  # изменение технического р-са группы
            'user_added',
        ])
        assert_that(events, equal_to(expected))
        events = EventModel(self.main_connection).find({'name': 'resource_grant_changed'})
        # События для технического ресурса группы и для ресурса resource
        assert_that(len(events), equal_to(2))
        assert_that(events, contains_inanyorder(
            has_entries(name='resource_grant_changed'),
            has_entries(name='resource_grant_changed'),
        ))
        assert_that(
            events,
            contains_inanyorder(
                has_entries(
                    name='resource_grant_changed',
                    org_id=self.organization['id'],
                    revision=revision,
                ),
                has_entries(
                    name='resource_grant_changed',
                    org_id=self.organization['id'],
                    revision=revision,
                ),
            )
        )
        event = EventModel(self.main_connection).find({
            'name': 'resource_grant_changed',
            'object.service': 'autotest',
        })[0]

        assert_that(event['content']['uids'], [user['id']])
        assert_that(event, has_entries(object=resource))

        event = EventModel(self.main_connection).find({
            'name': 'resource_grant_changed',
            'object.service': 'directory',
        })[0]

        assert_that(event['content']['uids'], [user['id']])

    def test_on_user_modify_move(self):
        # Перемещаем пользователя из department_one в department_two.
        # Первый департамент имеет права на чтение ресурса1, второй права на
        # чтение ресурса 2

        department_one = self.create_department()
        resource_one = self.create_resource_with_department(
            department_one['id'], 'read')
        department_two = self.create_department()
        resource_two = self.create_resource_with_department(
            department_two['id'], 'read')
        department_three = self.create_department()
        ResourceRelationModel(self.main_connection).create(
            self.organization['id'],
            resource_one['id'],
            'write',
            department_id=department_three['id'],
        )
        user = self.create_user(department_one['id'])
        # после создания пользователя в отделе поменялся счетчик
        # поэтому надо его обновить
        self.refresh(department_one)

        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        new_user = deepcopy(user)
        new_user['department_id'] = department_two['id']
        content = {
            'before': user,
        }
        on_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=new_user,
            object_type=TYPE_USER,
            content=content,
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_user_added',
            'department_user_deleted',
            'resource_grant_changed',
            'resource_grant_changed',
            'user_moved',
        ]
        assert_that(events, equal_to(expected))
        event = EventModel(self.main_connection).find({'name': 'user_moved'})[0]
        expected_diff = [department_one['id'], department_two['id']]

        new_user = change_user_data_created_fields_to_isoformat(new_user)
        assert_that(
            event,
            has_entries(object=new_user,
                        object_type=TYPE_USER,
                        content=has_entries(
                            diff=has_entries(department_id=expected_diff)),
                        ))

        # Проверка временной поддержки старого поля.
        department_one['sub_members_count'] = department_one['members_count']
        department_two['sub_members_count'] = department_two['members_count']

        event = EventModel(self.main_connection).find({'name': 'department_user_added'})[0]
        department_two = datefield_to_isoformat(department_two, 'created')
        assert_that(event, has_entries(object=department_two,
                                       object_type=TYPE_DEPARTMENT,
                                       content=has_entries(subject=new_user),
                                       ))

        event = EventModel(self.main_connection).find({'name': 'department_user_deleted'})[0]
        department_one = datefield_to_isoformat(department_one, 'created')
        assert_that(event, has_entries(object=department_one,
                                       object_type=TYPE_DEPARTMENT,
                                       content=has_entries(subject=new_user),
                                       ))
        event = EventModel(self.main_connection).find({'name': 'resource_grant_changed'})[0]
        assert_that(
            event,
            has_entries(
                object_type=TYPE_RESOURCE,
            ))

    def test_on_user_modify_move_tree_traversal(self):
        # Перемещаем пользователя из
        # root->department_parent_one->department_child_one
        # в root->department_parent_two->department_child_two

        department_parent_one = self.create_department()
        department_child_one = self.create_department(
            parent_id=department_parent_one['id'])
        department_parent_two = self.create_department()
        department_child_two = self.create_department(
            parent_id=department_parent_two['id'])
        user = self.create_user(department_id=department_child_one['id'])
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        new_user = deepcopy(user)
        new_user['department_id'] = department_child_two['id']
        content = {
            'before': user,
        }
        on_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=new_user,
            object_type=TYPE_USER,
            content=content,
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_user_added',
            'department_user_added',
            'department_user_deleted',
            'department_user_deleted',
            'user_moved',
        ]
        assert_that(events, equal_to(expected))
        event = EventModel(self.main_connection).find({'name': 'user_moved'})[0]
        expected_diff = [department_child_one['id'],
                         department_child_two['id']]
        new_user = change_user_data_created_fields_to_isoformat(new_user)
        assert_that(
            event,
            has_entries(object=new_user,
                        object_type=TYPE_USER,
                        content=has_entries(
                            diff=has_entries(department_id=expected_diff)),
                        ))
        event = EventModel(self.main_connection).find({'name': 'department_user_added'})
        assert_that(
            event,
            contains_inanyorder(
                has_entries(object=has_entries(id=department_child_two['id']),
                            ),
                has_entries(object=has_entries(id=department_parent_two['id']),
                            )
        ))
        assert_that(event, contains_inanyorder(
            has_entries(object_type=TYPE_DEPARTMENT),
            has_entries(object_type=TYPE_DEPARTMENT),
        ))
        assert_that(
            event,
            contains_inanyorder(
                has_entries(content=has_entries(subject=has_entries(
                    id=new_user['id'])),
                ),
                has_entries(content=has_entries(subject=has_entries(
                    id=new_user['id'])),
                )
        ))

        event = EventModel(self.main_connection).find({'name': 'department_user_deleted'})
        assert_that(
            event,
            contains_inanyorder(
                has_entries(object=has_entries(id=department_child_one['id']),
                            ),
                has_entries(object=has_entries(id=department_parent_one['id']),
                            )
        ))
        assert_that(event, contains_inanyorder(
            has_entries(object_type=TYPE_DEPARTMENT),
            has_entries(object_type=TYPE_DEPARTMENT),
        ))
        assert_that(
            event,
            contains_inanyorder(
                has_entries(content=has_entries(subject=has_entries(
                    id=new_user['id'])),
                ),
                has_entries(content=has_entries(subject=has_entries(
                    id=new_user['id'])),
                )
        ))

    def test_on_user_modify_grant_changed(self):
        # Перемещаем пользователя из department_one в department_two,
        # права на ресурс меняются с read + admin на  write

        department_one = self.create_department()
        resource_one = self.create_resource_with_department(
            department_one['id'],
            'read'
        )
        department_two = self.create_department()
        ResourceRelationModel(self.main_connection).create(
            self.organization['id'],
            resource_one['id'],
            'write',
            department_id=department_two['id'],
        )
        user = self.create_user(department_one['id'])
        # после создания пользователя в отделе поменялся счетчик
        # поэтому надо его обновить
        self.refresh(department_one)

        ResourceRelationModel(self.main_connection).create(
            self.organization['id'],
            resource_one['id'],
            'admin',
            department_id=department_one['id'],
        )
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        new_user = deepcopy(user)
        new_user['department_id'] = department_two['id']
        content = {
            'before': user,
        }
        on_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=new_user,
            object_type=TYPE_USER,
            content=content,
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_user_added',
            'department_user_deleted',
            'resource_grant_changed',
            'user_moved',
        ]
        assert_that(events, equal_to(expected))
        event = EventModel(self.main_connection).find({'name': 'user_moved'})[0]
        expected_diff = [department_one['id'], department_two['id']]
        new_user = change_user_data_created_fields_to_isoformat(new_user)
        assert_that(
            event,
            has_entries(
                object=new_user,
                object_type=TYPE_USER,
                content=has_entries(
                    diff=has_entries(department_id=expected_diff)
                )
            )
        )

        # Проверка временной поддержки старого поля.
        department_one['sub_members_count'] = department_one['members_count']
        department_two['sub_members_count'] = department_two['members_count']

        event = EventModel(self.main_connection).find({'name': 'department_user_added'})[0]
        department_two = datefield_to_isoformat(department_two, 'created')
        assert_that(
            event,
            has_entries(
                object=department_two,
                object_type=TYPE_DEPARTMENT,
                content=has_entries(subject=new_user),
            )
        )

        event = EventModel(self.main_connection).find({'name': 'department_user_deleted'})[0]
        department_one = datefield_to_isoformat(department_one, 'created')
        assert_that(event, has_entries(object=department_one,
                                       object_type=TYPE_DEPARTMENT,
                                       content=has_entries(subject=new_user),
                                       ))
        event = EventModel(self.main_connection).find({'name': 'resource_grant_changed'})[0]
        assert_that(event['content']['uids'], [user['id']])
        assert_that(event, has_entries(object=resource_one,
                                       object_type=TYPE_RESOURCE,
                                       ))
        relations_add = event['content']['relations']['add']
        relations_delete = event['content']['relations']['remove']
        expected_relation_add = [
            {
                'object_id': user['id'],
                'relation_name': 'write',
            }
        ]
        expected_relation_remove1 = {
            'object_id': user['id'],
            'relation_name': 'read',
        }
        expected_relation_remove2 = {
            'object_id': user['id'],
            'relation_name': 'admin',
        }
        assert_that(
            relations_delete,
            has_entries(users=contains_inanyorder(expected_relation_remove1,
                                                  expected_relation_remove2),
                        departments=[],
                        groups=[],
                        ))
        assert_that(
            relations_add,
            has_entries(users=expected_relation_add,
                        departments=[],
                        groups=[],
                        ))

    def test_on_user_modify_grant_not_changed(self):
        # Перемещаем пользователя из department_one в department_two,
        # у которых права на ресурсы одинаковые

        department_one = self.create_department()
        resource_one = self.create_resource_with_department(
            department_one['id'], 'read')
        department_two = self.create_department()
        ResourceRelationModel(self.main_connection).create(
            self.organization['id'],
            resource_one['id'],
            'read',
            department_id=department_two['id'],
        )
        user = self.create_user(department_one['id'])
        # после создания пользователя в отделе поменялся счетчик
        # поэтому надо его обновить
        self.refresh(department_one)

        # создадим связь с другим департаментом и проверим, что про нее не
        # приходят уведомления
        department_three = self.create_department()
        ResourceRelationModel(self.main_connection).create(
            self.organization['id'],
            resource_one['id'],
            'write',
            department_id=department_three['id'],
        )
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        new_user = deepcopy(user)
        new_user['department_id'] = department_two['id']
        content = {
            'before': user,
        }
        on_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=new_user,
            object_type=TYPE_USER,
            content=content,
        )
        events = [x['name'] for x in EventModel(self.main_connection).find()]
        events.sort()
        expected = [
            'department_user_added',
            'department_user_deleted',
            'user_moved',
        ]
        assert_that(events, equal_to(expected))
        event = EventModel(self.main_connection).find({'name': 'user_moved'})[0]
        expected_diff = [department_one['id'], department_two['id']]
        new_user = change_user_data_created_fields_to_isoformat(new_user)
        assert_that(
            event,
            has_entries(object=new_user,
                        object_type=TYPE_USER,
                        content=has_entries(
                            diff=has_entries(department_id=expected_diff)),
                        ))

        # Проверка временной поддержки старого поля.
        department_one['sub_members_count'] = department_one['members_count']
        department_two['sub_members_count'] = department_two['members_count']

        event = EventModel(self.main_connection).find({'name': 'department_user_added'})[0]
        department_two = datefield_to_isoformat(department_two, 'created')
        assert_that(event, has_entries(object=department_two,
                                       object_type=TYPE_DEPARTMENT,
                                       content=has_entries(subject=new_user),
                                       ))
        event = EventModel(self.main_connection).find({'name': 'department_user_deleted'})[0]
        department_one = datefield_to_isoformat(department_one, 'created')
        assert_that(event, has_entries(object=department_one,
                                       object_type=TYPE_DEPARTMENT,
                                       content=has_entries(subject=new_user),
                                       ))

    @patch('intranet.yandex_directory.src.yandex_directory.passport.client.PassportApiClient.account_edit',
           autospec=True)
    def test_on_user_property_changed(self, mock_func):
        # Проверяем, что при изменении полей user-a генерируется событие
        # user_property_changed.

        user = self.create_user()
        mock_func.return_value = {}
        revision = 1
        EventModel(self.main_connection).delete(force_remove_all=True)
        old_name = user['name']
        new_user = deepcopy(user)
        new_user['name'] = {
            "middle": {
                "ru": "Новое отчество",
                "en": "New Middle"
            },
            "last": {
                "ru": "Новая фамилия",
                "en": "New Last"
            },
            "first": {
                "ru": "Новое имя",
                "en": "New First"
            }
        }
        content = {
            'before': user,
        }
        on_user_modify(
            self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=new_user,
            object_type=TYPE_USER,
            content=content,
        )
        event = EventModel(self.main_connection).find()[0]
        expected = 'user_property_changed'
        expected_diff = {
            'name': [old_name, new_user['name']]
        }
        assert_that(event,
                    has_entries(
                        name=expected,
                        content=has_entries(diff=expected_diff),
                    ))

    def test_on_user_dismissed(self):
        # Проверяем, что по on_user_dismiss сгенерируется событие user_dismissed и department_user_deleted

        department_parent = self.create_department()
        department_child = self.create_department(parent_id=department_parent['id'])
        user = self.create_user(department_id=department_child['id'])

        user['groups'] = []
        content = {
            'before': user,
        }

        revision = 1

        # удаляем все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        on_user_dismiss(
            connection=self.main_connection,
            org_id=self.organization['id'],
            revision=revision,
            object_value=user,
            object_type=TYPE_USER,
            content=content,
        )

        events = EventModel(self.main_connection).find()

        names = [x['name'] for x in events]
        names.sort()
        expected = [
            'department_user_deleted',
            'department_user_deleted',
            'department_user_deleted',
            'user_dismissed',
        ]
        assert_that(names, equal_to(expected))

        # проверяем, что в каждом событии department_user_deleted будет diff с удаленным пользователем
        exp_diff = {
            'members': {
                'add': {},
                'remove': {
                    'users': [1110000000000001]
                }
            }
        }

        department_user_deleted_events = [e for e in events if e['name'] == 'department_user_deleted']
        for event in department_user_deleted_events:
            assert_that(
                event,
                has_entries(
                    content=has_entries(diff=exp_diff),
                ),
            )

    def test_on_user_add_robot(self):
        # Проверяем, что по on_user_add сгенерируeтся только событие user_added

        # получаем объект (внутри создаются события, но мы на них не смотрим, т.к. в этом тесте смотрим
        # на внутренности events)
        robot = create_robot_for_anothertest(self.meta_connection, self.main_connection,
                                             self.organization['id'], 'slug', self.post_json)

        # удаляем все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        action_user_add(
            self.main_connection,
            org_id=self.organization['id'],
            author_id=robot['id'],
            object_value=robot
        )

        events = EventModel(self.main_connection).find()

        names = [x['name'] for x in events]
        names.sort()
        expected = [
            'user_added',
        ]
        assert_that(names, equal_to(expected))
