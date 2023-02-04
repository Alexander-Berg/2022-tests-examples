# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    is_,
    is_not,
    contains_inanyorder,
    none,
    contains,
)
from unittest.mock import patch, Mock

from testutils import (
    create_organization,
    create_department,
    TestCase,
    has_only_entries,
)
from intranet.yandex_directory.src.yandex_directory.common.exceptions import ConstraintValidationError
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_DEPARTMENT,
    ROOT_DEPARTMENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import Ignore
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ActionModel,
    OrganizationModel,
    DepartmentModel,
    GroupModel,
    UserGroupMembership,
)
from intranet.yandex_directory.src.yandex_directory.core.models.department import (
    DepartmentNotEmptyError,
    DepartmentNotFoundError,
    RootDepartmentRemovingError,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    MAILLIST_SERVICE_SLUG,
)
from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel


class TestDepartmentModel_create(TestCase):
    def setUp(self):
        super(TestDepartmentModel_create, self).setUp()

        self.name = {
            'ru': 'Департамент',
            'en': 'Department'
        }
        self.parent_name_name = {
            'ru': 'Департамент родительский',
            'en': 'Department parent'
        }
        self.external_id = 'external_id'

    def test_simple(self):
        # return value
        department = DepartmentModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            external_id=self.external_id,
        )
        self.assertIsNotNone(department['id'])
        self.assertEqual(department['path'], str(department['id']))
        self.assertEqual(department['name'], self.name)
        self.assertEqual(department['org_id'], self.organization['id'])
        self.assertEqual(department['external_id'], self.external_id)

        # test data in database
        department_from_db = DepartmentModel(self.main_connection).get(department['id'], org_id=self.organization['id'])
        self.assertIsNotNone(department_from_db)
        self.assertEqual(department_from_db['id'], department['id'])
        self.assertEqual(department_from_db['name'], self.name)
        self.assertEqual(department_from_db['org_id'], self.organization['id'])
        self.assertEqual(department_from_db['parent_id'], None)
        self.assertEqual(department_from_db['path'], str(department_from_db['id']))
        self.assertEqual(department_from_db['external_id'], self.external_id)

    def test_deletion_shouldnt_free_id(self):
        # Проверим, что при удалении отдела его id не освободится, и не будет занят вслед созданным отделом.
        def create():
            return DepartmentModel(self.main_connection).create(
                name=self.name,
                org_id=self.organization['id'],
            )
        dep1 = create()
        DepartmentModel(self.main_connection).delete({'id': dep1['id']}, force=True)
        dep2 = create()

    def test_with_parent_department(self):
        parent_department = DepartmentModel(self.main_connection).create(
            name=self.parent_name_name,
            org_id=self.organization['id']
        )
        department = DepartmentModel(self.main_connection).create(
            name=self.name,
            parent_id=parent_department['id'],
            org_id=self.organization['id']
        )
        expected_path = '.'.join(map(str, [parent_department['id'], department['id']]))
        self.assertEqual(department['parent_id'], parent_department['id'])
        self.assertEqual(department['path'], expected_path)

        # test data in database
        department_from_db = DepartmentModel(self.main_connection).get(department['id'], org_id=self.organization['id'])
        self.assertEqual(department_from_db['parent_id'], parent_department['id'])
        self.assertEqual(department_from_db['path'], expected_path)

    def test_department_id_should_grow_incrementally_inside_every_organization(self):
        google = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        yandex_departments = []
        google_departments = []

        for i in range(3):
            yandex_departments.append(
                DepartmentModel(self.main_connection).create(
                    org_id=self.organization['id'],
                    name={
                        'ru': 'Some department'
                    }
                )['id']
            )
            google_departments.append(
                DepartmentModel(self.main_connection).create(
                    org_id=google['id'],
                    name={
                        'ru': 'Some department in google'
                    }
                )['id']
            )

        expected = [2, 3, 4]
        self.assertEqual(yandex_departments, expected)
        self.assertEqual(google_departments, expected)

    def test_special_group_heads_created_for_each_deparment(self):
        org_id = self.organization['id']
        department = DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Разработка чата',
                'en': 'Chat development'
            },
            org_id=org_id
        )

        self.assertIsNotNone(department.get('heads_group_id'))

        group = GroupModel(self.main_connection).get(
            group_id=department['heads_group_id'],
            org_id=org_id,
        )

        expected = {
            'ru': 'Руководитель отдела "Разработка чата"',
            'en': 'Head of department "Chat development"',
        }
        self.assertEqual(expected, group['name'])

    def test_special_group_heads_has_english_name_in_ru_key_if_org_lang_is_not_ru(self):
        # Сейчас Вики берёт название команды только из ключа ru, даже
        # для организации в которой установлен английский язык.
        # Поэтому, в рамках подготовки к выпиливанию перевода данных на разные
        # языки, мы делаем так, что для не русско-язычных организаций в
        # ключ ru будет записываться английский вариант названия команды.

        org_id = self.organization['id']
        # Поменяем язык организации
        OrganizationModel(self.main_connection).update(
            update_data={'language': 'en'},
            filter_data={'id': org_id},
        )

        # Теперь создадим отдел
        department = DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Chat development'
            },
            org_id=org_id
        )

        group = GroupModel(self.main_connection).get(
            group_id=department['heads_group_id'],
            org_id=org_id,
        )

        # И удостоверимся, что оба ключа содержат англоязычное название команды
        team_name = 'Head of department "Chat development"'
        assert_that(
            group['name'],
            has_entries(
                ru=team_name,
                en=team_name,
            )
        )


    def test_with_list_aliases(self):
        """ Передаем список алиасов """
        list_aliases = ['dep_alias1', 'dep_alias2', 'dep_alias3']
        department = DepartmentModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            aliases=list_aliases,
        )
        self.assertIsNotNone(department['id'])
        self.assertEqual(department['name'], self.name)
        self.assertEqual(department['aliases'], list_aliases)

        # test data in database
        department_from_db = DepartmentModel(self.main_connection).get(
            org_id=self.organization['id'],
            department_id=department['id']
        )
        self.assertIsNotNone(department_from_db)
        self.assertEqual(department_from_db['id'], department['id'])
        self.assertEqual(department_from_db['name'], self.name)
        self.assertEqual(department_from_db['aliases'], list_aliases)

    def test_without_aliases(self):
        """ Если не передаем алиасы, то в базе проставляется пустой список"""
        department = DepartmentModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            aliases=None,
        )
        self.assertIsNotNone(department['id'])
        self.assertEqual(department['name'], self.name)
        self.assertEqual(department['aliases'], [])

        # test data in database
        department_from_db = DepartmentModel(self.main_connection).get(
            org_id=self.organization['id'],
            department_id=department['id']
        )
        self.assertIsNotNone(department_from_db)
        self.assertEqual(department_from_db['id'], department['id'])
        self.assertEqual(department_from_db['name'], self.name)
        self.assertEqual(department_from_db['aliases'], [])

    def test_create_with_uppercase(self):
        label = 'UPPERCASE_LABEL'
        department = DepartmentModel(self.main_connection).create(
            name=self.name,
            org_id=self.organization['id'],
            external_id=self.external_id,
            label=label,
        )
        self.assertIsNotNone(department['id'])
        self.assertEqual(department['label'], label.lower())


class TestDepartmentModel_update_one(TestCase):
    def test_should_update_one_department(self):
        org_id = self.organization['id']

        DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Разработка чата',
                'en': 'Chat development'
            },
            org_id=org_id
        )
        new_name = {
            'ru': 'Киберчат'
        }
        DepartmentModel(self.main_connection).update_one(
            id=2,
            org_id=org_id,
            data={
                'name': new_name
            }
        )
        from_db = DepartmentModel(self.main_connection).get(
            department_id=2,
            org_id=org_id,
        )

        self.assertEqual(from_db['name'], new_name)

    def test_heads_group_name_changes_when_department_renamed(self):
        """При обновлении названия департамента, нужно и менять и название группы
        """
        org_id = self.organization['id']
        department = DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Разработка чата',
                'en': 'Chat development'
            },
            org_id=org_id
        )

        DepartmentModel(self.main_connection).update_one(
            id=2,
            org_id=org_id,
            data={
                'name': {
                    'ru': 'Киберчат'
                }
            }
        )

        group = GroupModel(self.main_connection).get(
            group_id=department['heads_group_id'],
            org_id=org_id,
        )

        expected = {
            'ru': 'Руководитель отдела "Киберчат"'
        }
        self.assertEqual(expected, group['name'])

    def test_should_raise_validation_error_if_parent_id_is_self(self):
        org_id = self.organization['id']
        department = DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Разработка чата',
                'en': 'Chat development'
            },
            org_id=org_id
        )

        try:
            DepartmentModel(self.main_connection).update_one(
                id=2,
                org_id=org_id,
                data={
                    'parent_id': department['id']
                }
            )
        except ConstraintValidationError as e:
            self.assertEqual(str(e), 'Department could not be used as parent of itself')
        else:
            self.fail('Should not be allowed to use itself as parent')

    def test_should_raise_validation_error_if_parent_id_is_one_of_departments_descendants(self):
        org_id = self.organization['id']
        parent_department = DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Разработка чата',
                'en': 'Chat development'
            },
            org_id=org_id
        )
        child_department = DepartmentModel(self.main_connection).create(
            id=3,
            name={
                'ru': 'Разработка диска',
                'en': 'Disk development'
            },
            org_id=org_id,
            parent_id=parent_department['id']
        )

        try:
            DepartmentModel(self.main_connection).update_one(
                id=parent_department['id'],
                org_id=org_id,
                data={
                    'parent_id': child_department['id']
                }
            )
        except ConstraintValidationError as e:
            expected_message = ("Department with id {child_id} is a descendant of department with id {parent_id} and "
                                "cannot be used as it's parent").format(
                child_id=child_department['id'],
                parent_id=parent_department['id'],
            )
            self.assertEqual(str(e), expected_message)
        else:
            self.fail('Should not be allowed to use itself as parent')


class TestDepartmentModel_find(TestCase):
    def test_expand_parent_for_root_department(self):
        # Проверим, что для рутового отдела поле parent будет не словарём, а None
        department = DepartmentModel(self.main_connection).find(
            {'id': 1},
            fields=['parent.*'],
            one=True,
        )
        assert_that(
            department['parent'],
            none(),
        )

    def test_with_is_outstaff(self):
        # Проверяем, что если в fields указан только is_outstaff,
        # то в результатах от find будет только это поле и id.
        # И при этом is_outstaff будет зависеть от того, в каком
        # отделе пользователь.

        org_id = self.organization['id']
        outstaff = DepartmentModel(self.main_connection) \
                   .get_or_create_outstaff(org_id)

        response = DepartmentModel(self.main_connection).fields('is_outstaff').all()

        assert_that(
            response,
            contains_inanyorder(
                has_only_entries(
                    id=self.department['id'],
                    is_outstaff=False,
                ),
                has_only_entries(
                    id=outstaff['id'],
                    is_outstaff=True,
                ),
            )
        )


class TestDepartmentModel_find_by_id(TestCase):
    def setUp(self):
        super(TestDepartmentModel_find_by_id, self).setUp()
        self.second_department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.third_department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )

    def test_find_by_one_id(self):
        response = DepartmentModel(self.main_connection).find({'id': self.department['id']})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.department['id'])

    def test_find_by_list_of_ids(self):
        expected_ids = [
            self.department['id'],
            self.second_department['id']
        ]
        response = DepartmentModel(self.main_connection).find({'id': expected_ids})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), len(expected_ids))
        self.assertEqual(
            sorted([i['id'] for i in response]),
            sorted(expected_ids)
        )


class TestDepartmentModel_find_by_alias(TestCase):
    def setUp(self):
        super(TestDepartmentModel_find_by_alias, self).setUp()
        self.second_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            aliases=['dep1', 'dep2'],
        )

    def test_me(self):
        response = DepartmentModel(self.main_connection).find({'alias': self.second_department['aliases'][0]})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['aliases'], self.second_department['aliases'])

    def test_alias_with_quota(self):
        response = DepartmentModel(self.main_connection).find({'alias': '"alias1"'})
        self.assertEqual(response, [])


class TestDepartmentModel_find_by_parent_id(TestCase):
    def setUp(self):
        super(TestDepartmentModel_find_by_parent_id, self).setUp()

        self.second_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.department['id']
        )
        self.third_department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        self.fourth_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.third_department['id']
        )

    def test_find_by_one_parent_id(self):
        response = DepartmentModel(self.main_connection).find({'parent_id': self.department['id']})
        self.assertIsNotNone(response)
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0]['id'], self.second_department['id'])

    def test_find_by_list_of_parent_ids(self):
        expected_ids = [
            self.second_department['id'],
            self.fourth_department['id']
        ]
        response = DepartmentModel(self.main_connection).find({
            'parent_id': [
                self.department['id'],
                self.third_department['id']
            ]
        })
        self.assertIsNotNone(response)
        self.assertEqual(len(response), len(expected_ids))
        self.assertEqual(
            sorted([i['id'] for i in response]),
            sorted(expected_ids)
        )


class TestDepartmentModel_get(TestCase):
    def test_simple(self):
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Департамент',
                'en': 'Department'
            },
            org_id=self.organization['id']
        )
        another_department = DepartmentModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Департамент другой',
                'en': 'Department another'
            },
            org_id=another_organization['id']
        )

        self.assertEqual(DepartmentModel(self.main_connection).get(2, another_organization['id'], fields=['*', 'email']), another_department)


class TestDepartmentModel_get_by_external_id(TestCase):
    def test_simple(self):
        # поиск отдела по внешнему идентификатору
        # создаем 2 отдела 1 из которых с внешним идентификатором

        external_id = 'external_id'
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        DepartmentModel(self.main_connection).create(
            name={
                'ru': 'Департамент',
                'en': 'Department'
            },
            org_id=self.organization['id'],
        )
        another_department = DepartmentModel(self.main_connection).create(
            name={
                'ru': 'Департамент другой',
                'en': 'Department another'
            },
            org_id=another_organization['id'],
            external_id=external_id
        )

        department_from_db = DepartmentModel(self.main_connection).get_by_external_id(
            org_id=another_organization['id'],
            id=external_id,
            fields=['*', 'email']
        )

        self.assertEqual(
            department_from_db,
            another_department
        )


class TestDepartmentModel_find_prefetch_related__parents(TestCase):
    def setUp(self):
        super(TestDepartmentModel_find_prefetch_related__parents, self).setUp()

        self.root = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )
        self.parent_of_parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.root['id']
        )
        self.parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.parent_of_parent_department['id']
        )
        self.department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=self.parent_department['id']
        )

    def test_me(self):
        departments = DepartmentModel(self.main_connection).find(
            fields=[
                'parents.email',
                'parents.*',
            ]
        )
        departments_by_id = dict([
            (d['id'], d)
            for d in departments
        ])

        self.assertEqual(departments_by_id[self.root['id']].get('parents'), [])
        self.assertEqual(
            departments_by_id[self.parent_of_parent_department['id']].get('parents'),
            [self.root]
        )
        self.assertEqual(
            departments_by_id[self.parent_department['id']].get('parents'),
            [
                self.root,
                self.parent_of_parent_department
            ]
        )
        self.assertEqual(
            departments_by_id[self.department['id']].get('parents'),
            [
                self.root,
                self.parent_of_parent_department,
                self.parent_department
            ]
        )

    def test_prefetch_related_should_not_include_departments_from_other_organizations(self):
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_organization'
        )['organization']

        root_department_from_second_organization = create_department(
            self.main_connection,
            org_id=second_organization['id'],
        )
        department_from_second_organization = create_department(
            self.main_connection,
            dep_id=self.parent_of_parent_department['id'],
            org_id=second_organization['id'],
            parent_id=root_department_from_second_organization['id'],
        )
        create_department(
            self.main_connection,
            org_id=second_organization['id'],
            parent_id=department_from_second_organization['id'],
        )
        department = DepartmentModel(self.main_connection).get(
            department_id=self.department['id'],
            org_id=self.organization['id'],
            fields=[
                '*',
                'parents.*',
            ],
        )
        parent_department_ids = [d['org_id'] for d in department['parents']]
        self.assertEqual(set(parent_department_ids), set([self.organization['id']]))

    def test_prefetch_related_for_items_with_many_org_ids_must_raise_error(self):
        second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_organization'
        )['organization']

        root_department_from_second_organization = create_department(
            self.main_connection,
            org_id=second_organization['id'],
        )
        department_from_second_organization = create_department(
            self.main_connection,
            dep_id=self.parent_of_parent_department['id'],
            org_id=second_organization['id'],
            parent_id=root_department_from_second_organization['id'],
        )
        create_department(
            self.main_connection,
            org_id=second_organization['id'],
            parent_id=department_from_second_organization['id'],
        )

        with self.assertRaises(RuntimeError):
            DepartmentModel(self.main_connection).find(
                fields=[
                    '*',
                    'parents.*',
                ],
            )


class TestDepartmentModelBehaviour_path_rebuild(TestCase):
    def check_paths(self, ordered_departments):
        expected_path = []
        for department in ordered_departments:
            expected_path.append(department['id'])
            self.assertEqual(
                department['path'],
                '.'.join(map(str, expected_path))
            )

    def test_path_should_be_self_id_if_no_parent_departments(self):
        department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )
        self.assertEqual(department['path'], str(department['id']))

    def test_path_should_be_full_path_to_self_from_root_department(self):
        root = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )
        parent_of_parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=root['id']
        )
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_of_parent_department['id']
        )
        department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_department['id']
        )

        self.check_paths([
            root,
            parent_of_parent_department,
            parent_department,
            department
        ])

    def test_should_rebuild_paths_for_all_descendants(self):
        root = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )
        parent_of_parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=root['id']
        )
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_of_parent_department['id']
        )
        department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_department['id']
        )

        # set other parent for parent_of_parent department
        another_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=root['id']
        )

        DepartmentModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            id=parent_of_parent_department['id'],
            data={'parent_id': another_department['id']}
        )

        # test, that paths have been rebuilt
        from_db = {
            'parent_of_parent_department': DepartmentModel(self.main_connection).get(
                department_id=parent_of_parent_department['id'],
                org_id=self.organization['id'],
            ),
            'parent_department': DepartmentModel(self.main_connection).get(
                department_id=parent_department['id'],
                org_id=self.organization['id'],
            ),
            'department': DepartmentModel(self.main_connection).get(
                department_id=department['id'],
                org_id=self.organization['id'],
            ),
        }

        self.check_paths([
            root,
            another_department,  # here it is
            from_db['parent_of_parent_department'],
            from_db['parent_department'],
            from_db['department']
        ])

    def test_should_not_rebuild_path_if_parent_id_has_not_been_sent_in_update_params(self):
        root = create_department(
            self.main_connection,
            org_id=self.organization['id'],
        )
        parent_of_parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=root['id']
        )
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_of_parent_department['id']
        )

        DepartmentModel(self.main_connection).update({'path': 'dont.touch.me'}, {'id': parent_department['id']})
        new_name = {
            'ru': 'new name'
        }
        DepartmentModel(self.main_connection).update_one(
            id=parent_department['id'],
            org_id=self.organization['id'],
            data={
                'name': new_name
            }
        )

        from_db = DepartmentModel(self.main_connection).get(department_id=parent_department['id'], org_id=self.organization['id'])

        self.assertEqual(from_db['name'], new_name)
        self.assertEqual(from_db['path'], 'dont.touch.me')
        self.assertEqual(from_db['name_plain'], new_name['ru'])


class TestDepartmentModel_remove(TestCase):
    def setUp(self):
        super(TestDepartmentModel_remove, self).setUp()
        self.org_id = self.organization['id']

    def test_group_not_link_with_removed_department(self):
        """
        Проверим, что после удаления департамента, он перестает входит в группы,
        в которых раньше был (ресурсы удаляются)
        """
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        department_will_be_removed = create_department(
            self.main_connection,
            org_id=self.org_id,
            parent_id=parent_department['id']
        )
        department_will_be_leave = create_department(
            self.main_connection,
            org_id=self.org_id,
            parent_id=parent_department['id']
        )
        members = [
            {'type': TYPE_DEPARTMENT, 'id': department_will_be_removed['id']},
            {'type': TYPE_DEPARTMENT, 'id': department_will_be_leave['id']},
        ]
        group = self.create_group(members=members)

        # удаляем департамент
        DepartmentModel(self.main_connection).remove(department_will_be_removed['id'], org_id=self.org_id, author_uid=self.user['id'])
        updated_group = GroupModel(self.main_connection).get(
            group_id=group['id'],
            org_id=self.org_id,
            fields=[
                '*',
                'members.*',
            ],
        )

        # проверяем, что удалился именно 1ый департамент,
        # а 2й - остался на месте
        assert_that(
            updated_group['members'],
            contains_inanyorder(
                has_entries(
                    object=has_entries(
                        id=department_will_be_leave['id']
                    )
                )
            )
        )
        assert_that(
            updated_group['members'],
            is_not(
                contains_inanyorder(
                    has_entries(
                        object=has_entries(
                            id=department_will_be_removed['id']
                        )
                    )
                )
            )
        )

    def test_get_not_return_removed_departments(self):
        """
        Проверим, что get модели не возвращает удаленные департаменты
        """
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        dep_exist = create_department(
            self.main_connection,
            org_id=self.org_id,
            parent_id=parent_department['id']
        )
        dep_remove = create_department(
            self.main_connection,
            org_id=self.org_id,
            parent_id=parent_department['id']
        )
        DepartmentModel(self.main_connection).remove(
            org_id=self.org_id,
            department_id=dep_remove['id'],
            author_uid=self.user['id'],
        )
        dep_remove = DepartmentModel(self.main_connection).get(
            org_id=self.org_id,
            department_id=dep_remove['id']
        )
        assert_that(dep_remove, is_(None))

        dep = DepartmentModel(self.main_connection).get(
            org_id=self.org_id,
            department_id=dep_exist['id']
        )
        assert_that(dep, is_not(None))

    def test_cant_remove_department_with_users(self):
        """
        Нельзя удалить департамент в котором есть не уволенные люди
        """
        department = create_department(
            self.main_connection,
            org_id=self.organization['id']
        )
        user = self.create_user(department_id=department['id'])
        self.assertEqual(user['is_dismissed'], False)

        with self.assertRaises(DepartmentNotEmptyError):
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=department['id'],
                author_uid=self.user['id'],
                force=False,
                real_remove=False,
            )

        fresh_user = UserModel(self.main_connection).get(user_id=user['id'], org_id=user['org_id'])
        self.assertEqual(fresh_user['is_dismissed'], False)

    def test_can_remove_department_with_users_if_force_removing_enabled(self):
        # Если передано force=True, то нужно увольнять всех людей в отделе и удалять его
        department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        user = self.create_user(department_id=department['id'])
        self.assertEqual(user['is_dismissed'], False)

        try:
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=department['id'],
                author_uid=self.user['id'],
                force=True,
                real_remove=False,
            )
        except DepartmentNotEmptyError:
            self.fail('Удаление департамента с force=True не должно вызывать DepartmentNotEmptyError')

    def test_can_remove_department_with_users_if_force_removing_enabled_and_real_remove(self):
        """
        Если передан real_remove=True, а люди в отделе еще есть - нужно кинуть ошибку
        """
        department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        user = self.create_user(department_id=department['id'])
        self.assertEqual(user['is_dismissed'], False)

        experiments = [True, False]

        for exp in experiments:
            with self.assertRaises(DepartmentNotEmptyError):
                DepartmentModel(self.main_connection).remove(
                    org_id=self.organization['id'],
                    department_id=department['id'],
                    author_uid=self.user['id'],
                    force=exp,
                    real_remove=True,
                )

    def test_cant_remove_department_with_departments(self):
        # Нельзя удалить департамент в котором есть активные департаменты
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        child_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_department['id']
        )

        with self.assertRaises(DepartmentNotEmptyError):
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=parent_department['id'],
                author_uid=self.user['id'],
                force=False,
                real_remove=False,
            )

        # проверим, что департаменты не удалены
        fresh_parent_department = DepartmentModel(self.main_connection).get(
            department_id=parent_department['id'],
            org_id=parent_department['org_id'],
        )
        self.assertEqual(fresh_parent_department['removed'], False)
        fresh_child_department = DepartmentModel(self.main_connection).get(
            department_id=child_department['id'],
            org_id=child_department['org_id'],
        )
        self.assertEqual(fresh_child_department['removed'], False)

    def test_cant_remove_department_with_departments_and_real_remove(self):
        # Нельзя удалить департамент в котором есть активные департаменты
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        child_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_department['id']
        )

        with self.assertRaises(DepartmentNotEmptyError):
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=parent_department['id'],
                author_uid=self.user['id'],
                force=False,
                real_remove=True,
            )

        # проверим, что департаменты не удалены
        fresh_parent_department = DepartmentModel(self.main_connection).get(
            department_id=parent_department['id'],
            org_id=parent_department['org_id'],
        )
        self.assertEqual(fresh_parent_department['removed'], False)
        fresh_child_department = DepartmentModel(self.main_connection).get(
            department_id=child_department['id'],
            org_id=child_department['org_id'],
        )
        self.assertEqual(fresh_child_department['removed'], False)

    def test_can_remove_department_with_departments_if_force_removing_enabled(self):
        # Можно удалить департамент в котором есть активные департаменты если передан force=True
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        child_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_department['id']
        )

        try:
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=parent_department['id'],
                author_uid=self.user['id'],
                force=True,
                real_remove=False,
            )
        except DepartmentNotEmptyError:
            self.fail('Удаление департамента с force=True не должно вызывать DepartmentNotEmptyError')

        # проверим, что департаменты не удалены
        fresh_parent_department = DepartmentModel(self.main_connection).get(
            department_id=parent_department['id'],
            org_id=parent_department['org_id'],
            removed=True,
        )
        self.assertIsNotNone(fresh_parent_department)

        fresh_child_department = DepartmentModel(self.main_connection).get(
            department_id=child_department['id'],
            org_id=child_department['org_id'],
            removed=True,
        )
        self.assertIsNotNone(fresh_child_department)

    def test_can_remove_department_with_departments_if_force_removing_enabled_with_real_remove(self):
        # Можно удалить департамент в котором есть активные департаменты если передан force=True и real_remove=True
        parent_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        child_department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=parent_department['id']
        )

        try:
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=parent_department['id'],
                author_uid=self.user['id'],
                force=True,
                real_remove=True,
            )
        except DepartmentNotEmptyError:
            self.fail('Удаление департамента с force=True не должно вызывать DepartmentNotEmptyError')

        # проверим, что департаменты удалены, т.к. мы передали real_remove
        fresh_parent_department = DepartmentModel(self.main_connection).get(
            department_id=parent_department['id'],
            org_id=parent_department['org_id'],
            removed=True,
        )
        self.assertIsNone(fresh_parent_department)

        fresh_child_department = DepartmentModel(self.main_connection).get(
            department_id=child_department['id'],
            org_id=child_department['org_id'],
            removed=True,
        )
        self.assertIsNone(fresh_child_department)

    def test_cant_remove_root_department(self):
        """
        Нельзя удалить корневой департамент
        """
        experiments = [
            {'force': False, 'real_remove': False},
            {'force': True, 'real_remove': False},
            {'force': False, 'real_remove': True},
            {'force': True, 'real_remove': True},
        ]
        for experiment in experiments:
            with self.assertRaises(RootDepartmentRemovingError):
                DepartmentModel(self.main_connection).remove(
                    org_id=self.organization['id'],
                    department_id=ROOT_DEPARTMENT_ID,
                    author_uid=self.user['id'],
                    force=experiment['force'],
                    real_remove=experiment['real_remove'],
                )

    def test_cant_remove_non_existing_department(self):
        """
        При попытке удалить отсутствующий департамент надо вызвать ошибку DepartmentNotFoundError
        """
        with self.assertRaises(DepartmentNotFoundError):
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=1234567891,
                author_uid=self.user['id'],
            )

    def test_action_department_delete_should_be_called_after_deleting(self):
        department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            parent_id=ROOT_DEPARTMENT_ID
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.actions.action_department_delete', Mock()) as mocked_action:
            DepartmentModel(self.main_connection).remove(
                org_id=self.organization['id'],
                department_id=department['id'],
                author_uid=self.user['id'],
            )
            self.assertEqual(mocked_action.call_count, 1)

    def test_can_remove_departament_without_uid_for_org_with_maillist_service(self):
        # Можно удалить отдел, если в организации вклчен сервис рассылок, но у отдела почему-то нет uid
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org_id,
            MAILLIST_SERVICE_SLUG,
        )
        department = create_department(
            self.main_connection,
            org_id=self.org_id,
            parent_id=ROOT_DEPARTMENT_ID
        )
        heads_group_id =  department['heads_group_id']

        DepartmentModel(self.main_connection).remove(
            org_id=self.org_id,
            department_id=department['id'],
            author_uid=self.user['id'],
        )
        department = DepartmentModel(self.main_connection).get(
            department_id=department['id'],
            org_id=self.org_id,
            removed=True,
        )
        assert_that(
            department,
            has_entries(
                heads_group_id=None,
            )
        )
        group = GroupModel(self.main_connection).filter(
            id=heads_group_id,
            org_id=self.org_id,
            removed=Ignore,
        ).one()
        assert_that(
            group,
            has_entries(
                removed=True,
            )
        )


class TestDepartmentModel_delete(TestCase):
    def test_delete_simple(self):
        # Проверяем, что удаляя существующий департамент - он удаляется из базы
        # и не возвращается ни в get, ни в find

        org_id = self.organization['id']
        new_dep = create_department(
            self.main_connection,
            org_id=org_id,
        )
        DepartmentModel(self.main_connection).delete(
            filter_data={
                'org_id': org_id,
                'id': new_dep['id']
            }
        )

        empty_deps = DepartmentModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'id': new_dep['id'],
                'removed': Ignore,
            }
        )
        self.assertEqual(empty_deps, [])

        delete_none_dep = DepartmentModel(self.main_connection).get(
            org_id=org_id,
            department_id=new_dep['id'],
        )
        self.assertEqual(delete_none_dep, None)

    def test_delete_removed_department(self):
        # Убеждаемся, что обычное удаление не может удалить данные,
        # с флажком removed=True и проверяем работу delete с
        # force=True параметром

        org_id = self.organization['id']
        new_dep = create_department(
            self.main_connection,
            org_id=org_id,
        )
        DepartmentModel(self.main_connection).update_one(
            org_id=org_id,
            id=new_dep['id'],
            data={'removed': True}
        )
        DepartmentModel(self.main_connection).delete(
            filter_data={
                'org_id': org_id,
                'id': new_dep['id']
            }
        )

        removed_dep = DepartmentModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'id': new_dep['id'],
                'removed': True
            }
        )
        # все еще в таблице
        self.assertNotEqual(removed_dep, [])

        removed_dep = DepartmentModel(self.main_connection).get(
            org_id=org_id,
            department_id=new_dep['id'],
            removed=True,
        )
        # все еще в таблице
        self.assertNotEqual(removed_dep, None)

        DepartmentModel(self.main_connection).delete(
            filter_data={
                'org_id': org_id,
                'id': new_dep['id'],
            },
            force=True
        )

        delete_none_dep = DepartmentModel(self.main_connection).get(
            org_id=org_id,
            department_id=new_dep['id']
        )
        self.assertEqual(delete_none_dep, None)

        empty_deps = DepartmentModel(self.main_connection).find(
            filter_data={
                'org_id': org_id,
                'id': new_dep['id'],
                'removed': Ignore
            }
        )
        self.assertEqual(empty_deps, [])

    def test_dissmissed_head_of_department_is_removed_from_heads_group(self):
        # Если уволили руководителя отдела, он должен удаляться из команды
        # "руководители отдела".
        # Ранее не удалялся, что приводило к ошибке, описанной в DIR-2830.

        org_id = self.organization['id']
        admin_id = self.user['id']

        # Сначала создадим сотрудника
        user = self.create_user(department_id=1)
        # И сделаем его руководителем новенького отдела
        department = create_department(
            self.main_connection,
            org_id=org_id,
            # Сейчас создание руководителя отработает не полностью.
            # Так будет, пока не поправим: https://st.yandex-team.ru/DIR-2921
            head_id=user['id'],
        )
        group_id = department['heads_group_id']

        # Убедимся, что он входит в специальную группу
        def get_department_head():
            # Эта таблица хранит полный список пользователей в группе
            cached_member = UserGroupMembership(self.main_connection).find(
                filter_data={
                    'org_id': org_id,
                    'group_id': group_id,
                },
                one=True,
            )
            # А так мы получим только непосредственные вхождения
            # через связь с группой.
            user = UserModel(self.main_connection).find(
                {
                    'org_id': department['org_id'],
                    'group_id': group_id,
                },
                one=True,
            )
            return cached_member, user

        # group = GroupModel(self.main_connection).find(
        #     {
        #         'org_id': org_id,
        #         'id': group_id,
        #     },
        #     one=True,
        # )
        # resource_id = group['resource_id']
        # relations = ResourceModel(self.main_connection).get_relations(
        #     resource_id,
        #     org_id,
        # )
        result = get_department_head()

        assert_that(
            result,
            contains(
                # Руководитель должен быть в таблице с кэшом пользователей
                # (модель UserGroupMembership)
                has_entries(
                    user_id=user['id'],
                ),
                has_entries(
                    id=user['id'],
                )
            )
        )

        # Теперь уволим руководителя
        UserModel(self.main_connection).dismiss(
            org_id,
            user['id'],
            admin_id,
        )

        # И убедимся, что он больше не является главой отдела
        result = get_department_head()
        assert_that(
            result,
            contains(
                # После увольнения, пользователь должен пропасть
                # и в кэше
                none(),
                # И среди связей с группой руководителей
                none(),
            )
        )


class TestDepartmentModel__create_outstaff(TestCase):
    def test_create_success(self):
        # Проверим, что создается отдел outstaff
        # Проверим, что генерится событие
        org_id = self.organization['id']
        outstaff_department = DepartmentModel(self.main_connection).get_or_create_outstaff(
            org_id,
            'en',
        )
        assert_that(
            DepartmentModel(self.main_connection).get_or_create_outstaff(org_id),
            has_entries(
                org_id=org_id,
                label=None,
                uid=None,
                parent_id=None,
                id=outstaff_department['id'],
                name=has_entries(
                    en='Outstaff',
                    ru='Outstaff',
                )
            )
        )
        actions = ActionModel(self.main_connection).filter(org_id=org_id).all()
        assert_that(
            actions,
            contains(
                has_entries(
                    name='department_add',
                    object=has_entries(
                        name=has_entries(en='Outstaff')
                    ),
                ),
            )
        )
