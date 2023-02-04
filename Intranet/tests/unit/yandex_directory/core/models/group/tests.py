# -*- coding: utf-8 -*-
import pytest
from testutils import TestCase
from hamcrest import (
    assert_that,
    contains,
    has_entries,
    is_,
    empty,
    has_item,
    not_,
    has_items,
)

from intranet.yandex_directory.src.yandex_directory.common.exceptions import ConstraintValidationError
from intranet.yandex_directory.src.yandex_directory.core.models import (
    GroupModel,
    UserModel,
    ActionModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    MAILLIST_SERVICE_SLUG,
)
from unittest.mock import patch
from intranet.yandex_directory.src.yandex_directory.core.models.group import (
    GROUP_TYPE_ROBOTS,
    GROUP_TYPE_DEPARTMENT_HEAD,
)
from intranet.yandex_directory.src.yandex_directory.core.models.resource import ResourceModel
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    only_ids,
    only_fields,
    build_email,
    only_attrs,
)
from testutils import (
    create_organization,
    create_group,
    assert_not_called,
)

from unittest import skip

GROUP_NAME = {
    'ru': 'Группа',
    'en': 'Group',
}
GROUP_DESCRIPTION = {
    'ru': 'Описание группы',
    'en': 'Group description',
}


class TestGroupModel_create_update(TestCase):
    def setUp(self):
        super(TestGroupModel_create_update, self).setUp()
        self.label = 'group'
        self.external_id = 'external_id'

    def test_simple(self):
        # return value
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            external_id=self.external_id,
        )
        self.assertIsNotNone(group['id'])
        self.assertEqual(group['name'], GROUP_NAME)
        self.assertEqual(group['type'], 'generic')
        self.assertEqual(group['external_id'], 'external_id')

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id']
        )
        self.assertIsNotNone(group_from_db)
        self.assertEqual(group_from_db['id'], group['id'])
        self.assertEqual(group_from_db['name'], GROUP_NAME)
        self.assertEqual(group_from_db['type'], 'generic')
        self.assertEqual(group_from_db['external_id'], self.external_id)

    def test_with_label(self):
        # return value
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label=self.label,
        )
        self.assertEqual(group['label'], self.label)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id']
        )
        self.assertEqual(group_from_db['label'], self.label)

    def test_with_author(self):
        # return value
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            author_id=self.user['id'],
        )
        self.assertEqual(group['author_id'], self.user['id'])

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
        )
        self.assertEqual(group_from_db['author_id'], self.user['id'])

    def test_without_author(self):
        # return value
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            author_id=None,
        )
        self.assertEqual(group['author_id'], None)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
        )
        self.assertEqual(group_from_db['author_id'], None)

    def test_should_set_creating_date(self):
        # return value
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
        )
        self.assertIsNotNone(group['created'])

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id']
        )
        self.assertIsNotNone(group_from_db['created'])

    @pytest.mark.skip('Now we can not change email for group.')
    def test_should_set_email_if_its_in_args(self):
        email = 'web-chib@ya.ru'
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            email=email,
        )
        self.assertEqual(group['email'], email)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=['*', 'email'],
        )
        self.assertEqual(group_from_db['email'], email)

    def test_should_set_email_automatically_if_it_is_not_in_args_and_there_is_label(self):
        label = 'support'
        group_model = GroupModel(self.main_connection)
        group = group_model.create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label=label,
        )
        expected_email = build_email(
            self.main_connection,
            label=label,
            org_id=self.organization['id'],
        )
        self.assertEqual(group['email'], expected_email)

        # test data in database
        group_from_db = group_model.get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=['*', 'email'],
        )
        self.assertEqual(group_from_db['email'], expected_email)

    @pytest.mark.skip('Now we can not change email for group.')
    def test_should_use_email_from_args_even_if_there_is_label(self):
        email = 'web-chib@ya.ru'
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            email=email,
            label='support',
        )
        self.assertEqual(group['email'], email)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=['*', 'email'],
        )
        self.assertEqual(group_from_db['email'], email)

    def test_should_not_set_email_automatically_if_there_is_no_label(self):
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
        )
        self.assertEqual(group['email'], None)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=['*', 'email'],
        )
        self.assertEqual(group_from_db['email'], None)

    def test_resource_is_created_and_deleted_with_group(self):
        num_groups = len(GroupModel(self.main_connection).find())
        num_resources = len(ResourceModel(self.main_connection).find())

        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id']
        )

        assert_that(len(GroupModel(self.main_connection).find()), is_(num_groups + 1))
        assert_that(len(ResourceModel(self.main_connection).find()), is_(num_resources + 1))

        GroupModel(self.main_connection).delete(
            filter_data={
                'id': group['id'],
                'org_id': self.organization['id'],
            })

        assert_that(len(GroupModel(self.main_connection).find()), is_(num_groups))
        # ресурс не удаляется
        assert_that(len(ResourceModel(self.main_connection).find()), is_(num_resources + 1))

    def test_create_with_description(self):
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label=self.label,
            description=GROUP_DESCRIPTION,
        )
        self.assertEqual(group['description'], GROUP_DESCRIPTION)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id']
        )
        self.assertEqual(group_from_db['description'], group['description'])

    def test_create_with_members(self):
        group_one = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label=self.label,
        )
        group_two = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label=self.label + ' another',
        )
        subgroups_ids = [
            group_one['id'],
            group_two['id'],
        ]
        self.clean_actions_and_events()
        group_three = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            members=[
                {'type': 'group', 'id': group_id}
                for group_id in subgroups_ids
            ],
            label=self.label + ' yet another',
        )

        # группа состоит из 2-х подгрупп
        assert_that(
            only_ids(
                only_attrs(
                    group_three['members'], 'object'
                )
            ),
            has_items(*subgroups_ids)
        )

        # было создано действие group_add
        assert_that(
            ActionModel(self.main_connection).find(),
            contains(
                has_entries(
                    object_type='group',
                    name='group_add',
                    org_id=self.organization['id'],
                    object=has_entries(
                        id=group_three['id'],
                    )
                )
            )
        )


    def test_create_with_department(self):
        subdepartment = self.create_department(parent_id=self.department['id'])
        worker = self.create_user(department_id=subdepartment['id'])

        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            members=[{'type': 'department', 'id': subdepartment['id']}],
        )

        # проверяем, что members теперь содержит нужный департамент

        group = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=[
                '*',
                'members.*',
            ],
        )
        assert_that(
            group,
            has_entries(
                members=contains(
                    has_entries(
                        type='department',
                        object=has_entries(id=subdepartment['id'])
                    )
                )
            )
        )

        # проверяем, что пользователь worker теперь входит в группу
        response = UserModel(self.main_connection).find(filter_data=dict(
            org_id=self.organization['id'],
            recursive_group_id=group['id']
        ))
        assert_that(response, contains(has_entries(id=worker['id'])))

    def test_membership_is_updated_on_event_when_department_was_changed(self):
        """Проверяем, что развернутый состав группы меняется, когда
        человек добавляется в отдел.
        """
        # содаем вложенный отдел

        org_id = self.organization['id']
        parent_department = self.create_department(parent_id=1)['id']
        sub_department = self.create_department(parent_id=parent_department)['id']

        # группа будет включать в себя родительский отдел
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=org_id,
            members=[{'type': 'department', 'id': parent_department}],
        )['id']

        # теперь добавляем человека во вложенный отдел
        worker = self.create_user(department_id=sub_department)

        # проверяем, что пользователь worker теперь входит в группу
        response = UserModel(self.main_connection).find(filter_data=dict(
            org_id=org_id,
            recursive_group_id=group,
        ))
        assert_that(response, contains(has_entries(id=worker['id'])))

        # теперь переводим человека в другой отдел
        UserModel(self.main_connection).update_one(
            update_data=dict(department_id=1),
            filter_data=dict(
                org_id=org_id,
                id=worker['id'],
            )
        )
        # и он должен пропасть из группы
        response = UserModel(self.main_connection).find(filter_data=dict(
            org_id=org_id,
            recursive_group_id=group,
        ))
        assert_that(response, empty())

    def test_membership_is_updated_on_event_when_subgroup_was_changed(self):
        """Проверяем, что развернутый состав группы меняется, когда
        человек добавляется в отдел.
        """
        org_id = self.organization['id']

        # создаем вложенную группу
        sub_group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=org_id,
        )['id']

        # промежуточная группа в цепочке наследования
        middle_group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=org_id,
            members=[{'type': 'group', 'id': sub_group}],
        )['id']

        # группа будет включать в себя родительский отдел
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=org_id,
            members=[{'type': 'group', 'id': middle_group}],
        )['id']

        # теперь добавляем человека во вложенную группу
        worker = self.create_user(groups=[sub_group])

        # проверяем, что пользователь worker теперь входит в группу
        response = UserModel(self.main_connection).find(filter_data=dict(
            org_id=org_id,
            recursive_group_id=group,
        ))
        assert_that(response, contains(has_entries(id=worker['id'])))

        # теперь убираем человека из группы
        GroupModel(self.main_connection).update_one(
            org_id=org_id,
            group_id=sub_group,
            data=dict(members=[])
        )

        # и проверяем, что из группы верхнего уровня он тоже пропал
        response = UserModel(self.main_connection).find(filter_data=dict(
            org_id=self.organization['id'],
            recursive_group_id=group,
        ))
        assert_that(response, empty())

    def test_cycle_update_one(self):
        """
        Пытаемся добавить к группу memberом эту же группу.
        Ожидаем исключение ConstraintValidationError
        """
        org_id = self.organization['id']
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=org_id,
        )
        members = [{'type': 'group', 'id': group['id']}]
        with self.assertRaises(ConstraintValidationError):
            GroupModel(self.main_connection).update_one(
                org_id=org_id,
                group_id=group['id'],
                data=dict(members=members)
            )

    def test_with_list_aliases(self):
        """ Передаем список алиасов """
        list_aliases = ['alias1', 'alias2', 'alias3']
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            aliases=list_aliases,
        )
        self.assertIsNotNone(group['id'])
        self.assertEqual(group['name'], GROUP_NAME)
        self.assertEqual(group['aliases'], list_aliases)

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id']
        )
        self.assertIsNotNone(group_from_db)
        self.assertEqual(group_from_db['id'], group['id'])
        self.assertEqual(group_from_db['name'], GROUP_NAME)
        self.assertEqual(group_from_db['aliases'], list_aliases)

    def test_without_aliases(self):
        """ Если не передаем алиасы, то в базе проставляется пустой список"""
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            aliases=None,
        )
        self.assertIsNotNone(group['id'])
        self.assertEqual(group['name'], GROUP_NAME)
        self.assertEqual(group['aliases'], [])

        # test data in database
        group_from_db = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id']
        )
        self.assertIsNotNone(group_from_db)
        self.assertEqual(group_from_db['id'], group['id'])
        self.assertEqual(group_from_db['name'], GROUP_NAME)
        self.assertEqual(group_from_db['aliases'], [])

    def test_create_with_uppercase(self):
        label_upper = 'LABEL'
        email_upper = build_email(
            self.main_connection,
            label_upper,
            org_id=self.organization['id'],
        )
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            external_id=self.external_id,
            label = label_upper,
        )
        self.assertIsNotNone(group['id'])
        self.assertEqual(group['label'], label_upper.lower())
        self.assertEqual(group['email'], email_upper.lower())

    def test_tech_group_not_create_maillist(self):
        # при создании технических групп рассылки в паспорте не появляются
        org_id = self.organization['id']
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            MAILLIST_SERVICE_SLUG,
        )

        group_types = [
            GROUP_TYPE_ROBOTS,
            GROUP_TYPE_DEPARTMENT_HEAD,
        ]
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.groups.create_maillist') as create_maillist:
            for group_type in group_types:
                self.clean_actions_and_events()
                tech_group = GroupModel(self.main_connection).create(
                    name={'ru': 'Type: {}'.format(group_type)},
                    org_id=org_id,
                    type=group_type,
                )
                # есть действие
                assert_that(
                    ActionModel(self.main_connection).find(),
                    contains(
                        has_entries(
                            name='group_add',
                            object_type='group',
                            org_id=org_id,
                            object=has_entries(
                                id=tech_group['id'],
                                label=None,
                            )
                        )
                    )
                )
                # нет рассылок
                assert_not_called(create_maillist)


class TestGroupModel_find_by_id(TestCase):
    def setUp(self):
        super(TestGroupModel_find_by_id, self).setUp()

        self.group_one = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.group_two = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.group_three = create_group(
            self.main_connection,
            org_id=self.organization['id']
        )

    def test_find_by_one_id(self):
        response = GroupModel(self.main_connection).find({'id': self.group_one['id']})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.group_one['id'])

    def test_find_by_list_of_ids(self):
        expected_ids = [
            self.group_one['id'],
            self.group_two['id']
        ]
        response = GroupModel(self.main_connection).find({'id': expected_ids})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), len(expected_ids))
        self.assertEqual(
            sorted([i['id'] for i in response]),
            sorted(expected_ids)
        )


class TestGroupModel_find_by_type(TestCase):
    def setUp(self):
        super(TestGroupModel_find_by_type, self).setUp()

        self.generic_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            type='generic'
        )
        self.organization_admin_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            type='organization_admin'
        )

    def test_find_by_one_type(self):
        response = GroupModel(self.main_connection).find({'type': 'generic'})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.generic_group['id'])

    def test_find_by_list_of_types(self):
        response = GroupModel(self.main_connection).find({'type': ['generic']})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.generic_group['id'])


class TestGroupModel_find_by_label(TestCase):
    def setUp(self):
        super(TestGroupModel_find_by_label, self).setUp()

        self.support_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            label='support'
        )
        self.developers_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            label='developers'
        )

    def test_find_by_label(self):
        response = GroupModel(self.main_connection).find({'label': 'support'})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.support_group['id'])

        response = GroupModel(self.main_connection).find({'label': 'developers'})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.developers_group['id'])


class TestGroupModel_find_by_alias(TestCase):
    def setUp(self):
        super(TestGroupModel_find_by_alias, self).setUp()

        self.support_group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            label='support',
            aliases=['support1', 'support2']
        )

    def test_me(self):
        response = GroupModel(self.main_connection).find({'alias': self.support_group['aliases'][0]})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['aliases'], self.support_group['aliases'])

    def test_alias_with_quota(self):
        response = GroupModel(self.main_connection).find({'alias': '"alias1"'})
        self.assertEqual(response, [])


class TestGroupModel__find__prefetch_related(TestCase):
    def test_members(self):
        group_zero = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_zero'
        )
        group_one = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_one'
        )
        group_two = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_two'
        )
        subgroups_ids = [
            group_one['id'],
            group_two['id'],
        ]
        group_three = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_three',
            members=[
                {'type': 'group', 'id': subgroup_id}
                for subgroup_id in subgroups_ids
            ],
        )

        response = GroupModel(self.main_connection).find(
            filter_data={'id': group_three['id']},
            fields=[
                '*',
                'members.*',
            ],
        )[0]
        members = response.get('members')
        self.assertEqual(len(members), len(subgroups_ids))
        self.assertEqual(
            sorted([m['object']['id'] for m in members]),
            sorted(subgroups_ids)
        )

    def test_prefetch_with_users_subgroups_and_departments(self):
        # протестируем, что prefetch_related работает правильно
        # при наличии людей, других подгрупп в группе и департаментов с пользователями

        # создадим пустую группу, которая нигде не участвует
        group_zero = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_zero'
        )
        # создадим две будущих подгруппы
        group_one = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_one'
        )
        group_two = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_two'
        )

        # создадим двух пользователей в разных департаментах
        user_one = self.create_user(department_id=self.department['id'])
        department_two = self.create_department(parent_id=self.department['id'])
        user_two = self.create_user(department_id=department_two['id'])
        department_three = self.create_department(parent_id=department_two['id'])
        user_three = self.create_user(department_id=department_three['id'])

        # составим список участников группы
        subgroups_ids = [
            group_one['id'],
            group_two['id'],
        ]
        user_ids = [
            user_one['id'],
            user_two['id'],
            user_three['id']
        ]
        department_ids = [
            department_three['id'],
        ]
        members = []
        for subgroup_id in subgroups_ids:
            members.append({'type': 'group', 'id': subgroup_id})
        for user_id in user_ids:
            members.append({'type': 'user', 'id': user_id})
        for department_id in department_ids:
            members.append({'type': 'department', 'id': department_id})

        # создадим целевую группу
        group_three = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            label='group_three',
            members=members,
        )

        response = GroupModel(self.main_connection).find(
            filter_data={'id': group_three['id']},
            fields=[
                '*',
                'members.*',
            ],
        )[0]

        members_ids = sorted([m['object']['id'] for m in response.get('members', [])])
        exp_members = sorted(user_ids + subgroups_ids + department_ids)
        self.assertEqual(len(members), len(exp_members))
        self.assertEqual(members_ids, exp_members)


class TestGroupModel__select_related(TestCase):
    def test_author(self):
        group = GroupModel(self.main_connection).create(
            name=GROUP_NAME,
            org_id=self.organization['id'],
            author_id=self.user['id']
        )
        group = GroupModel(self.main_connection).get(
            org_id=self.organization['id'],
            group_id=group['id'],
            fields=[
                '*',
                'author.*',
            ],
        )
        self.assertEqual(
            group['author'],
            only_fields(
                self.user,
                'id',
                'org_id',
                'aliases',
                'nickname',
                'name',
                'email',
                'gender',
                'about',
                'birthday',
                'contacts',
                'position',
                'department_id',
                'is_dismissed',
                'role',
            )
        )


class TestGroupModel_get(TestCase):
    def test_simple(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        GroupModel(self.main_connection).create(
            name={
                'ru': 'Группа',
                'en': 'Group'
            },
            org_id=self.organization['id'],
            label='group',
        )
        another_group = GroupModel(self.main_connection).create(
            name={
                'ru': 'Группа другая',
                'en': 'Group another'
            },
            org_id=another_organization['id'],
            label='another group',
        )

        self.assertEqual(
            GroupModel(self.main_connection).get(
                org_id=another_organization['id'],
                group_id=another_group['id'],
            )['name']['ru'],
            another_group['name']['ru']
        )


class TestGroupModel_get_by_external_id(TestCase):
    def test_simple(self):
        # поиск команды по внешнему идентификатору
        # создаем 2 команды 1 из которых с внешним идентификатором

        external_id = 'external_id'
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        GroupModel(self.main_connection).create(
            name={
                'ru': 'Группа',
                'en': 'Group'
            },
            org_id=self.organization['id'],
            label='group',
        )
        another_group = GroupModel(self.main_connection).create(
            name={
                'ru': 'Группа другая',
                'en': 'Group another'
            },
            org_id=another_organization['id'],
            label='another group',
            external_id=external_id,
        )

        self.assertEqual(
            GroupModel(self.main_connection).get_by_external_id(org_id=another_organization['id'], id=external_id)['name']['ru'],
            another_group['name']['ru']
        )


class TestGroupModel_delete(TestCase):
    def test_delete_simple(self):
        # Проверяем, что удаляя существующую группу - она удаляется из базы
        # и не возвращается ни в get, ни в find
        org_id = self.organization['id']

        new_group = self.create_group()
        self.clean_actions_and_events()

        self.delete_group(new_group)

        # было создано действие group_delete
        assert_that(
            ActionModel(self.main_connection).find(),
            contains(
                has_entries(
                    object_type='group',
                    name='group_delete',
                    org_id=org_id,
                    object=has_entries(
                        id=new_group['id'],
                    )
                )
            )
        )

        empty_groups = GroupModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'id': new_group['id']
            }
        )
        self.assertEqual(empty_groups, [])

        delete_none_group = GroupModel(self.main_connection).get(
            org_id=org_id,
            group_id=new_group['id']
        )
        self.assertEqual(delete_none_group, None)

    def test_delete_removed_group(self):
        # Убеждаемся, что обычное удаление не может удалить данные,
        # с флажком removed=True и проверяем работу delete с
        # force=True параметром

        org_id = self.organization['id']
        new_group = self.create_group()
        GroupModel(self.main_connection).update_one(
            org_id=org_id,
            group_id=new_group['id'],
            data={'removed': True}
        )
        self.delete_group(new_group, generate_action=False)

        removed_group = GroupModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'id': new_group['id'],
                'removed': True
            }
        )
        # все еще в таблице
        self.assertNotEqual(removed_group, [])

        self.delete_group(new_group, force=True, generate_action=False)

        delete_none_group = GroupModel(self.main_connection).get(
            org_id=org_id,
            group_id=new_group['id']
        )
        self.assertEqual(delete_none_group, None)

    def test_sequential_group_ids(self):
        # Проверяем, что при удалении группы из базы,
        # её ID не должен освобождаться,
        # и у следующей группы будет id с большим номером.
        first_group = self.create_group()
        self.delete_group(first_group, force=True)
        second_group = self.create_group()

        assert_that(
            second_group['id'],
            is_(first_group['id'] + 1)
        )

    def create_group(self):
        return GroupModel(self.main_connection).create(
            name={
                'ru': 'Группа',
                'en': 'Group'
            },
            org_id=self.organization['id'],
            label='group',
        )

    def delete_group(self, group, force=False, generate_action=True):
        GroupModel(self.main_connection).delete(
            filter_data={
                'org_id': self.organization['id'],
                'id': group['id'],
            },
            force=force,
            generate_action=generate_action,
        )
