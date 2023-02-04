# -*- coding: utf-8 -*-

from copy import deepcopy

from unittest.mock import (
    patch,
    Mock,
)
from hamcrest import (
    assert_that,
    equal_to,
    calling,
    raises,
    has_length,
    has_entries,
    has_items,
    has_entry,
    all_of,
)
from testutils import TestCase, create_organization

from intranet.yandex_directory.src.yandex_directory.core.importer import (
    BaseImporter,
    NoToplevelDepartments,
    DuplicateDepartmentId,
    InvalidParentId,
    ToManyConnectedComponent,
    DuplicateUserNickname,
    UnknownUserDepartment,
)
from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel
from intranet.yandex_directory.src.yandex_directory.core.models.department import DepartmentModel
from intranet.yandex_directory.src.yandex_directory.common.models.types import ROOT_DEPARTMENT_ID


class TestDepartmentValidator(TestCase):
    create_organization = False

    def setUp(self):
        super(TestDepartmentValidator, self).setUp()
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
        )

        self.importer = BaseImporter(
            self.meta_connection,
            self.main_connection,
            organization['organization']['id'],
        )

        self.departments_valid = [{
            'id': '1',
            'name': 'dep1',
        }, {
            'id': '2',
            'parent': '1',
            'name': 'dep11',
        }]

        self.users_valid = [{
            "id": "1",
            "name": {
                "first": "Иван",
                "last": "Иванов",
                "middle": "Иванович",
            },
        }, {
            "id": "2",
            "name": {
                "first": "Иван",
                "last": "Иванов"
            },
        }, {
            "id": "3",
            "nickname": "sosisa",
            "name": {
                "first": "Полина",
                "last": "Иванова"
            },
            "birthday": "1979-11-03",
            "contacts": [
                {
                    "type": "email",
                    "value": "polina@mail.ru"
                },
                {
                    "main": True,
                    "type": "phone",
                    "value": "+7 (123) 456-78-90"
                }
            ]
        }]

    def test_all_with_head(self):
        # ошибка если нет хоть одоного отдела без голвного отдела (циклы)
        departments_all_with_head = deepcopy(self.departments_valid)
        departments_all_with_head[0]['parent'] = '2'

        self.importer.departments = departments_all_with_head
        assert_that(
            calling(self.importer.validate_departments).with_args(),
            raises(NoToplevelDepartments)
        )

    def test_duplicate_department_id(self):
        # должна быть ошибка если есть отделы с дублированием id
        departments_duplicate_id = deepcopy(self.departments_valid)
        departments_duplicate_id.append(departments_duplicate_id[0])

        self.importer.departments = departments_duplicate_id
        assert_that(
            calling(self.importer.validate_departments).with_args(),
            raises(DuplicateDepartmentId)
        )

    def test_unknown_head_id(self):
        # должна быть ошибка если есть отдел с неизвестным головным отделом
        departments_unknown_head = deepcopy(self.departments_valid)
        departments_unknown_head[1]['parent'] = '3'

        self.importer.departments = departments_unknown_head
        assert_that(
            calling(self.importer.validate_departments).with_args(),
            raises(InvalidParentId)
        )

    def test_loop(self):
        # должна быть ошибка если есть отделы с циклами в иерархии

        departments_loop = deepcopy(self.departments_valid)
        departments_loop[0]['parent'] = '2'
        departments_loop.append({
            'id': '3',
            'name': 'dep1'
        })

        self.importer.departments = departments_loop
        assert_that(
            calling(self.importer.validate_departments).with_args(),
            raises(ToManyConnectedComponent)
        )

    def test_duplicate_nickname(self):
        # должна быть ошибка если есть пользователи с дублированием ников
        users_duplicate_nickname = deepcopy(self.users_valid)
        users_duplicate_nickname[0]['nickname'] = 'nickname'
        users_duplicate_nickname[1]['nickname'] = 'nickname'

        self.importer.users = users_duplicate_nickname
        assert_that(
            calling(self.importer.validate_users).with_args(),
            raises(DuplicateUserNickname)
        )

    def test_unknown_department(self):
        # должна быть ошибка если есть пользователи в отделе с неизвестным id

        users_unknown_department = deepcopy(self.users_valid)
        users_unknown_department[0]['department'] = '1'

        self.importer.users = users_unknown_department
        assert_that(
            calling(self.importer.validate_users).with_args(),
            raises(UnknownUserDepartment)
        )

    def test_generate_nickname_if_need(self):
        # генерируем ники если их нет

        # если Фамилия Имя совпали то гереним ники с добавлением цифры в конце
        expected = deepcopy(self.users_valid)
        expected[0]['nickname'] = 'ivan-ivanov'
        expected[1]['nickname'] = 'ivan-ivanov1'

        self.importer.users = self.users_valid
        self.importer._generate_nickname_if_need()

        assert_that(
            self.importer.users,
            equal_to(expected)
        )

    def test_import(self):
        # удачно импортируем отделы и пользователей в организацию

        # помещаем пользотвателей в отделы
        users_with_departments = deepcopy(self.users_valid)
        users_with_departments[0]['department'] = '1'
        users_with_departments[1]['department'] = '2'

        # указываем руководителей для отделов
        departments_with_head = deepcopy(self.departments_valid)
        departments_with_head[0]['head_id'] = '1'

        self.importer.users = users_with_departments
        self.importer.departments = departments_with_head

        # TODO: выглядит костыльно. Внутри importer.start_import() нужно отдавать каждый раз новый uid. Мб можно мокнуть что-то внутри.
        with patch(self.mock_passport_path) as mocked_passport_api:
            result = Mock()
            result.json = Mock()
            returns = [{'status': 'ok', 'uid': i} for i in range(6)]
            result.json.side_effect = returns
            mocked_passport_api.post = Mock(return_value=result)
            self.importer.start_import()

        users = UserModel(self.main_connection).find()
        assert_that(
            users,
            has_length(4)  # 3 импортированных пользователя + админ
        )
        departments = DepartmentModel(self.main_connection).find(
            fields=[
                '*',
                'head.*',
                'parent.*'
            ]
        )
        assert_that(
            departments,
            has_length(3)  # 2 импортированных отдела + головной отдел
        )

        assert_that(
            departments,
            all_of(
                has_items(
                    # у отдела ожидаемый руководитель
                    has_entries(
                        head=has_entry('external_id', departments_with_head[0]['head_id'])
                    ),
                    # сохранена иерархия отделов
                    has_entries(
                        parent=has_entry('external_id', '1'),
                        external_id='2',
                    ),
                    # отдел без родителя присоеденен к головному отделу
                    has_entries(
                        external_id='1',
                        parent=has_entries(
                            external_id=None,
                            id=ROOT_DEPARTMENT_ID,
                        )
                    )
                ),
            )
        )
