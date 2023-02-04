# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    not_none,
    ends_with,
    calling,
    raises,
    has_entries,
    has_key,
    not_,
    contains,
    none,
    starts_with,
    all_of,
    empty,
)

from unittest.mock import patch
from datetime import date

from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    ConstraintValidationError,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    TYPE_GROUP,
    TYPE_USER,
    TYPE_DEPARTMENT,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    paginate_by_shards,
    get_common_parent,
    get_domains_by_org_id,
    is_inner_uid,
    is_org_admin,
    is_member,
    get_user_role,
    get_master_domain,
    check_objects_exists,
    flatten_paths,
    prepare_user,
    prepare_department,
    prepare_group,
    create_user,
    only_hierarchical_fields,
    lang_for_notification,
    prepare_user_with_fields,
    create_organization,
    add_existing_user,
)
from intranet.yandex_directory.src.yandex_directory.core.models.user import (
    UserModel,
    UserMetaModel,
    get_external_org_ids,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DepartmentModel,
    GroupModel,
    DomainModel,
    ServiceModel,
)
from testutils import (
    SimpleTestCase,
    TestCase,
    create_inner_uid,
    create_department,
    create_group,
    create_user as utils_create_user,
    create_organization_without_domain,
    fake_userinfo,
)
from intranet.yandex_directory.src.yandex_directory.core.features import CAN_WORK_WITHOUT_OWNED_DOMAIN
from intranet.yandex_directory.src.yandex_directory.core.features.utils import set_feature_value_for_organization
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id

from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    LoginNotavailable,
)
from intranet.yandex_directory.src.yandex_directory import app


class Test_get_common_parent(TestCase):
    def test_me(self):
        common_parent = get_common_parent('1.2.3.4', '1.2.5.6')
        assert_that(common_parent, equal_to((2, 3, 5)))

        common_parent = get_common_parent('1', '1.3')
        assert_that(common_parent, equal_to((1, 1, 3)))


class Test_create_user(TestCase):
    def setUp(self):
        super(Test_create_user, self).setUp()

        self.org_admin = self.organization_info['admin_user']
        self.user_data = {
            'id': create_inner_uid(12345),
            'name': {
                'first': {
                    'ru': 'Gena'
                },
                'last': {
                    'ru': 'Chibisov'
                }
            },
            'gender': 'male',
            'nickname': 'web-chib',
            'email': 'web-chib@ya.ru',
        }
        self.user_data_no_id = {
            'name': {
                'first': {
                    'ru': 'Gena'
                },
                'last': {
                    'ru': 'Chibisov'
                }
            },
            'gender': 'male',
            'nickname': 'web-chib',
            'email': 'web-chib@ya.ru',
        }

    def test_should_create_user_meta_instance(self):
        assert is_inner_uid(self.user_data['id'])

        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=self.user_data['nickname']
        )

        assert created_user.get('user_meta_instance')
        user_meta_instance = UserMetaModel(self.meta_connection).get(
            user_id=created_user['user_meta_instance']['id'],
            org_id=self.organization['id'],
        )
        assert user_meta_instance['org_id'] == self.organization['id']

    def test_should_create_user_instance(self):
        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=self.user_data['nickname'],
        )

        assert created_user.get('user')
        user_instance = UserModel(self.main_connection).get(user_id=created_user['user']['id'])
        assert user_instance['org_id'] == self.organization['id']
        for key, value in list(self.user_data.items()):
            assert user_instance.get(key) == value

    def test_should_create_user_if_no_id_provided(self):
        del self.user_data['id']
        nickname_from_params = 'web-chib-from'
        password = '123456789'
        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=nickname_from_params,
            password=password,
        )

        assert created_user.get('user')
        user_instance = UserModel(self.main_connection).get(user_id=created_user['user']['id'])
        assert user_instance['org_id'] == self.organization['id']
        for key, value in list(self.user_data.items()):
            assert user_instance.get(key) == value

# Если нет поля name, то должно создасться
    def test_should_create_user_if_no_name_provided(self):
        del self.user_data['name']
        nickname_from_params = 'web-chib-from'
        password = '123456789'
        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=nickname_from_params,
            password=password,
        )
        assert created_user.get('user')
        user_instance = UserModel(self.main_connection).get(user_id=created_user['user']['id'])
        assert user_instance['org_id'] == self.organization['id']
        assert user_instance.get('name')

# Если имя не задано, то у пользователя имя будет пустая строка
    def test_should_create_user_if_no_firstname_provided(self):
        del self.user_data['name']['first']
        nickname_from_params = 'web-chib-from'
        password = '123456789'
        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=nickname_from_params,
            password=password,
        )
        assert created_user.get('user')
        user_instance = UserModel(self.main_connection).get(user_id=created_user['user']['id'])
        assert user_instance['org_id'] == self.organization['id']
        assert user_instance['name']['first']['ru'] == ""

# Если фамилия не задана, то у пользователя фамиля будет пустая строка
    def test_should_create_user_if_no_lastname_provided(self):
        del self.user_data['name']['last']
        nickname_from_params = 'web-chib-from'
        password = '123456789'
        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=nickname_from_params,
            password=password,
        )

        assert created_user.get('user')
        user_instance = UserModel(self.main_connection).get(user_id=created_user['user']['id'])
        assert user_instance['org_id'] == self.organization['id']
        assert user_instance['name']['last']['ru'] == ""

    def test_should_set_domain_specific_nickname_if_not_id_provided(self):
        del self.user_data['id']
        nickname_from_params = 'web-chib-from'
        password = '123456789'

        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_data=self.user_data,
            nickname=nickname_from_params,
            password=password,
        )

        expected_nickname = nickname_from_params
        assert created_user['user']['nickname'] == expected_nickname

    def test_should_create_user_if_ignore_login_not_available_for_user(self):
        # если учетка уже есть в паспорте в директории
        # должна быть заведена учётка с таким же уидом

        self.mocked_passport.account_add.side_effect = LoginNotavailable
        expected_uid = 12312321

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_login',
                   return_value={'is_maillist': False, 'uid': expected_uid}):
            created_user = create_user(
                self.meta_connection,
                self.main_connection,
                org_id=self.organization['id'],
                user_data=self.user_data_no_id,
                nickname='nick',
                password='paswd',
                ignore_login_not_available=True,
            )

        assert_that(
            created_user,
            has_entries(
                user=has_entries(id=expected_uid)
            )
        )

    def test_should_create_user_if_ignore_login_not_available_for_ml(self):
        # create_user должен выбрасывать исключение LoginNotavailable,
        # полученное от паспорта, если учётка в паспорте - рассылка.
        # Это должно происходить независимо от флага ignore_login_not_available.

        self.mocked_passport.account_add.side_effect = LoginNotavailable

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_login',
                   return_value={'is_maillist': True}):
            assert_that(
                calling(create_user).with_args(
                    self.meta_connection,
                    self.main_connection,
                    org_id=self.organization['id'],
                    user_data=self.user_data_no_id,
                    nickname='nickname',
                    password='password',
                    ignore_login_not_available=True
                ),
                raises(LoginNotavailable)
            )


class Test_get_domains(TestCase):
    def setUp(self):
        super(Test_get_domains, self).setUp()

        self.count_no_type_domains = 5
        org_label = self.organization['label']
        org_id = self.organization['id']
        self.org_domain = self.organization_domain

        for i in range(self.count_no_type_domains):
            DomainModel(self.main_connection).create(
                org_id=org_id,
                name="%s_%s.ru" % (org_label, i)
            )

    def test_domains_info_dict(self):
        # Проверяем формат ответа функции core.utils.get_domains_by_org_id.
        #
        # Должен совпадать с таким dict-ом:
        # {
        #     "all": ["rubashki.ru", "noski.ru",
        #             "obuvka.ru", "noski.ws.yandex.ru"],
        #     "owned": ["noski.ws.yandex.ru"],
        #     "master": "noski.ws.yandex.ru",
        #     "display": "noski.ws.yandex.ru", -> https://st.yandex-team.ru/DIR-1992
        # }

        domains_info = get_domains_by_org_id(
            self.meta_connection,
            self.main_connection,
            self.organization['id']
        )
        assert_that(
            domains_info["master"], equal_to(self.org_domain)
        )
        # отдаем вместо display master
        # https://st.yandex-team.ru/DIR-1992
        assert_that(
            domains_info["display"],
            equal_to(self.org_domain)
        )

        assert_that(
            len(domains_info["all"]),
            equal_to(
                # +1 это self.org_domain
                self.count_no_type_domains + 1
            )
        )
        assert_that(
            domains_info["owned"],
            contains(self.org_domain)
        )


class Test__is_org_admin(TestCase):
    def setUp(self):
        super(Test__is_org_admin, self).setUp()

        self.department = self.create_department()
        self.user = self.create_user(
            nickname='test_not_admin',
            department_id=self.department['id'],
        )

    def test_is_not_admin(self):
        is_admin = is_org_admin(
            role=get_user_role(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                self.user['id']
            )
        )
        self.assertFalse(is_admin)

    def test_is_admin(self):
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=self.user['id'],
        )
        is_admin = is_org_admin(
            role=get_user_role(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                self.user['id']
            )
        )
        self.assertTrue(is_admin)

    def test_is_outer_admin(self):
        outer_admin = self.create_user(
            nickname='outer_admin',
            is_outer=True
        )
        is_inner_admin = UserModel(self.main_connection).is_admin(self.organization['id'], outer_admin['id'])
        msg = 'UserModel().is_admin должен вернуть False если пользователь не внутренний админ'
        self.assertFalse(is_inner_admin, msg=msg)

        is_admin = is_org_admin(
            role=get_user_role(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                outer_admin['id']
            )
        )
        msg = 'is_org_admin должен вернуть True если пользователь является внешним админом'
        self.assertTrue(is_admin)


class Test_check_objects_exists(TestCase):

    def test_object_no_exists(self):
        # проверяем, что для всех типов объектов кидается исключеине, если мы не нашли его в базе
        not_exists_object_id = 112123123123123
        object_types = [TYPE_USER, TYPE_GROUP, TYPE_DEPARTMENT]

        for object_type in object_types:
            pattern = 'Some objects of type "{type}" were not found in database'.format(type=object_type)
            assert_that(
                calling(check_objects_exists).with_args(
                    self.main_connection,
                    self.organization['id'],
                    [{'type': object_type, 'id': not_exists_object_id}]
                ),
                raises(ConstraintValidationError, pattern=pattern)
            )

    def test_invalid_type(self):
        # Если неизвестный тип объекта, то кидаем исключение
        not_exists_object_id = 112123123123123
        invalid_type = 'invalid_type'

        pattern = '"{type}" is invalid object type'.format(type=invalid_type)
        assert_that(
            calling(check_objects_exists).with_args(
                self.main_connection,
                self.organization['id'],
                # вариант когда ключ object_type, а не type (оба допустимы)
                [{'object_type': invalid_type, 'id': not_exists_object_id}]
            ),
            raises(ConstraintValidationError, pattern=pattern)
        )

    def test_object_type_and_type_together(self):
        # ключи типа объекта object_type и type недопустимы вместе

        pattern_re = '^Keys type and object_type are mutually exclusive, please, correct member'
        assert_that(
            calling(check_objects_exists).with_args(
                self.main_connection,
                self.organization['id'],
                [{'object_type': TYPE_USER, 'type': TYPE_USER, 'id': 123}]
            ),
            raises(ConstraintValidationError, pattern=pattern_re)
        )


class Test_flatten_paths(SimpleTestCase):

    def test_simple(self):
        # Проверим, что если список один, то вернётся такой-же

        input_data = [
            [1, 2, 3, 4],
        ]
        expected_data = [1, 2, 3, 4]

        self.assertEqual(
            flatten_paths(input_data),
            expected_data,
        )

    def test_flatten_paths(self):
        # Проверим, что функция вернёт список id
        # от вершины к листьям, по алгоритму depth-first.

        input_data = [
            [1, 2, 3, 4],
            [1, 2, 3, 5],
            [1, 6],
        ]
        expected_data = [1, 6, 2, 3, 5, 4]

        self.assertEqual(
            flatten_paths(input_data),
            expected_data,
        )

    def test_flatten_different_tries(self):
        # если цепочки не пересекаются
        # (что теоретически может быть, когда пользователя переносят из
        # "сотрудников" в "роботы").

        input_data = [
            [1, 2, 5, 6],
            [3, 4, 7, 8],
        ]
        expected_data = [3, 4, 7, 8, 1, 2, 5, 6]

        self.assertEqual(
            flatten_paths(input_data),
            expected_data,
        )


class Test_prepare_department(TestCase):
    def test_prepare_department_can_have_null_fields(self):
        # При рефакторинге и подготовке 3 версии API мы сломали выдачу
        # полей для предыдущих версий: DIR-2985
        # Тут мы проверяем, что если некоторые поля содержат None,
        # они всё равно будут сериализованы.
        assert_that(
            prepare_department(
                self.main_connection,
                {
                    'id': 1,
                    'org_id': 100500,
                    'description': None,
                    'external_id': None,
                    'label': None,
                    'members_count': 0, # Тут None быть не должно
                },
                api_version=1,
            ),
            has_entries(
                {
                    'id': 1,
                    # 'org_id': 100500 # это поле не выводится специально
                    'description': None,
                    'external_id': None,
                    'label': None,
                    'email': None, # Это поле должно генериться автоматически, если во входных данных есть label и org_id
                    'members_count': 0,
                }
            )
        )

    def test_some_fields_will_be_null_if_absent(self):
        # При рефакторинге и подготовке 3 версии API мы сломали выдачу
        # полей для предыдущих версий: DIR-2985
        # Тут мы проверяем, что даже если некоторые поля отсутствуют во
        # входном словаре, то на выходе они будут со значением None
        assert_that(
            prepare_department(
                self.main_connection,
                {
                    'id': 1,
                },
                api_version=1,
            ),
            has_entries(
                {
                    'id': 1,
                    'description': None,
                    'external_id': None,
                    'label': None,
                    'email': None, # Это поле должно генериться автоматически, если во входных данных есть label и org_id
                                   # если же их нет, то значение None
                    'members_count': None,
                }
            )
        )


class Test_prepare_group(TestCase):
    def test_prepare_group_can_have_null_fields(self):
        # При рефакторинге и подготовке 3 версии API мы сломали выдачу
        # полей для предыдущих версий: DIR-2985
        # Тут мы проверяем, что если некоторые поля содержат None,
        # они всё равно будут сериализованы.
        assert_that(
            prepare_group(
                self.main_connection,
                {
                    'id': 1,
                    'org_id': 100500,
                    'author_id': None,
                    'external_id': None,
                    'label': None,
                    'members_count': 0, # Тут None быть не должно
                },
                api_version=1,
            ),
            has_entries(
                {
                    'id': 1,
                    # 'org_id': 100500 # это поле не выводится специально
                    'author_id': None,
                    'external_id': None,
                    'label': None,
                    'email': None, # Это поле должно генериться автоматически, если во входных данных есть label и org_id
                    'members_count': 0,
                }
            )
        )


    def test_author_id_will_be_null_if_absent(self):
        # При рефакторинге и подготовке 3 версии API мы сломали выдачу
        # полей для предыдущих версий: DIR-2985
        # Тут мы проверяем, что если даже естли поля author_id
        # нет во входных данных, в API версии <3 это поле
        # будет присутствовать со значением None.
        assert_that(
            prepare_group(
                self.main_connection,
                {
                    'id': 1,
                },
                api_version=1,
            ),
            has_entries(
                {
                    'id': 1,
                    'author_id': None,
                }
            )
        )


class Test_prepare_user(TestCase):
    def setUp(self):
        super(Test_prepare_user, self).setUp()

        self.user = utils_create_user(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            user_id=create_inner_uid(12345),
            name={
                'first': {
                    'ru': 'Gena'
                },
                'last': {
                    'ru': 'Chibisov'
                }
            },
            gender='male',
            nickname='web-chib',
            email='web-chib@ya.ru',
        )
        self.group = create_group(
            self.main_connection,
            org_id=self.organization['id'],
            label='group',
        )
        self.department = create_department(
            self.main_connection,
            org_id=self.organization['id'],
            label='department',
        )

    def test_email(self):
        # email состоит из nickname/label и master домена
        org_id = self.organization['id']

        old_master_domain = DomainModel(self.main_connection).get_master(org_id)['name']

        test_data = {
            prepare_user: (self.user, 'nickname'),
        }
        for func, (data, key) in test_data.items():
            prepared = func(self.main_connection, data, api_version=1)
            # email до смены master домена
            old_email = prepared['email']
            assert_that(
                old_email,
                ends_with('@'+old_master_domain)  # доменная часть - master домен
            )

        # Для отделов и групп проверяем отдельно, так как
        # поле email лежит в prefetch_related и оно не строится в prepare_*.
        test_data_v6 = {
            prepare_department: (DepartmentModel, 'department_id', self.department['id']),
            prepare_group: (GroupModel, 'group_id', self.group['id']),
        }
        for func, (model, key, id) in test_data_v6.items():
            data = {key: id, 'org_id': org_id, 'fields': ['email']}
            prepared = func(self.main_connection, model(self.main_connection).get(**data), api_version=6)
            # email до смены master домена
            old_email = prepared['email']
            assert_that(
                old_email,
                ends_with('@'+old_master_domain)  # доменная часть - master домен
            )

        # при смене  master домена меняються ящики
        new_master_domain = 'new.master.com'

        DomainModel(self.main_connection).update_one(
            old_master_domain,
            org_id,
            {'name': new_master_domain}
        )
        # сбросим кэш текущего мастер домена
        get_master_domain(self.main_connection, org_id, force_refresh=True)

        for func, (data, _) in test_data.items():
            # email после смены master домена
            new_email = func(self.main_connection, data, api_version=1)['email']
            assert_that(
                new_email,
                ends_with('@'+new_master_domain)  # доменная часть - новый master домен
            )

        # Повторяем проверку нового адреса почты для отделов и групп.
        for func, (model, key, id) in test_data_v6.items():
            data = {key: id, 'org_id': org_id, 'fields': ['email']}
            prepared = func(self.main_connection, model(self.main_connection).get(**data), api_version=6)
            # email до смены master домена
            old_email = prepared['email']
            assert_that(
                old_email,
                ends_with('@'+new_master_domain)  # доменная часть - master домен
            )


class Test_prepare_user_with_fields(TestCase):
    def test_deprecated_login(self):
        # апи версии 5 и выше поле login убрано

        # в 1-4 версиях поля есть в выдаче
        for api_version in range(1, 4):
            assert_that(
                prepare_user_with_fields(
                    meta_connection=self.main_connection,
                    main_connection=self.main_connection,
                    user=self.user,
                    fields=['id', 'login'],
                    api_version=api_version,
                ),
                has_key('login')
            )
        # в 5 версии поля уже нет
        api_version = 5
        assert_that(
            prepare_user_with_fields(
                meta_connection=self.main_connection,
                main_connection=self.main_connection,
                user=self.user,
                fields=['id', 'login'],
                api_version=api_version,
            ),
            not_(
                has_key('login')
            )
        )


class Test__only_hierarchical_fields(TestCase):
    def setUp(self):
        self.obj = {
            'foo': {1: 'bar', 2: 'bazzz'},
            'blah': {
                'id': 42,
                'name': 'Pupkin',
            },
            'minor': [{'id': 100500, 'nickname': 'art'}],
        }

    def test_first_level_fields(self):
        # Простой вариант, когда нужно выбрать ключи первого уровня
        # со всем их содержимым
        assert_that(
            only_hierarchical_fields(
                self.obj,
                {'foo': True}
            ),
            equal_to(
                {'foo': {1: 'bar', 2: 'bazzz'}}
            )
        )

    def test_nested_dicts(self):
        # Проверим, что для вложенных списков тоже
        # можно указать желаемые поля
        assert_that(
            only_hierarchical_fields(
                self.obj,
                {'blah': {'id': True}}
            ),
            equal_to(
                {'blah': {'id': 42}}
            )
        )

    def test_nested_lists(self):
        # Проверим, что объектов вложенного списка тоже
        # можно указать список желаемых полей
        assert_that(
            only_hierarchical_fields(
                self.obj,
                {'minor': {'nickname': True}}
            ),
            equal_to(
                {'minor': [{'nickname': 'art'}]}
            )
        )

    def test_nested_lists_with_all_dependencies(self):
        # Проверим, что если у вложенного объекта все поля –
        # результаты зависимостей, то есть, для них указаны False,
        # то такой вложенный объект будет удалён целиком из результатов.
        assert_that(
            only_hierarchical_fields(
                self.obj,
                {
                    'foo': True,
                    'blah': {
                        'id': False,
                        'name': False,
                    }
                }
            ),
            equal_to(
                {'foo': {1: 'bar', 2: 'bazzz'}}
            )
        )

        # Но если одно из поле вложенного объекта имеет True, то в
        # в результатах будет только оно
        assert_that(
            only_hierarchical_fields(
                self.obj,
                {
                    'foo': True,
                    'blah': {
                        'id': True,
                        'name': False,
                    }
                }
            ),
            equal_to(
                {
                    'foo': {1: 'bar', 2: 'bazzz'},
                    'blah': {'id': 42},
                }
            )
        )


class NoShardError(RuntimeError):
    pass


class WrongOffsetError(RuntimeError):
    pass


class Test__paginate_by_shards(SimpleTestCase):
    def setUp(self, *args, **kwargs):
        super(Test__paginate_by_shards, self).setUp(*args, **kwargs)
        self.shards = [1, 2, 3]
        self.shard_sizes = {
            1: 3,
            2: 4,
            3: 10,
        }

    def get_items(self, shard, offset, limit):
        """Возвращает заданное количество элементов с определённой страницы
        какого-то шарда.

        Всего шарда 3, допустимые значения: 1, 2, 3
        В первом шарде 3 элемента, во втором - 4, а в третьем 10.

        Элементы в каждом шарде, это строки созданные из названия шарда и
        номера элемента (начиная с 1). Например 1_2 - второй элемент в
        первом шарде. Но offset при этом задаётся так же как в БД - с нуля.
        """

        if shard not in self.shard_sizes:
            raise NoShardError('Unknown shard')

        if offset < 0:
            raise WrongOffsetError('Offset should be >= 0')

        shard_size = self.shard_sizes[shard]
        # Сформируем результат из последовательности айтемов
        # в рамках выбранной страницы
        up_to = min(
            shard_size,
            offset + limit,
        )
        return [
            '{shard}_{item}'.format(shard=shard, item=item)
            for item in list(range(offset + 1, up_to + 1))
        ]

    def test_just_get_items(self):
        # Проверим, что наш get_items работает как надо и
        # отдаёт правильные айтемы из запрашиваемых шардов
        assert_that(
            self.get_items(shard=1, offset=0, limit=3),
            contains('1_1', '1_2', '1_3'),
        )
        # Во втором шарде всего 4 элемента, и
        # если запросить все, начиная с третьего (тут offset считается с 0),
        # то должны быть возвращены только два последних элемента. Даже
        # при том, что мы указываем limit=3
        assert_that(
            self.get_items(shard=2, offset=2, limit=3),
            contains('2_3', '2_4'),
        )
        assert_that(
            calling(self.get_items).with_args(shard=0, offset=1, limit=5),
            raises(NoShardError)
        )
        assert_that(
            calling(self.get_items).with_args(shard=1, offset=-1, limit=5),
            raises(WrongOffsetError)
        )

    def make_link(self, shard, next_page):
        """Это mock для формирования ссылки. В реальном коде
        тут будет код, который делает настоящую ссылку, внедряя
        в неё параметры shard и page.
        """
        return (shard, next_page)

    # Следующие тесты идут один за другим и эмулируют последовательные запросы
    # страниц из нескольких шардов с помощью paginate_by_shards.
    # Название теста содержит номер страницы, но не надо путать её со страницей
    # в рамках одного шарда.
    #
    # В paginate_by_shards передаётся именно страница
    # внутри указанного шарда.
    def test_first_page(self):
        # Проверим, что вернутся результаты из первого шарда
        # если шард и страница не указаны

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=None,
            per_page=2,
            item_getter=self.get_items,
        )
        # Всего должно быть 2 первых элемента из первого шарда
        assert_that(
            results,
            contains('1_1', '1_2')
        )
        # так как в шарде есть ещё элементы, то функция
        # должна вернуть нам первый шард
        assert_that(shard, equal_to(1))
        # и второй номер страницы
        assert_that(next_page, equal_to(2))

    def test_second_page(self):
        # Проверим, что на второй странице вернётся оставшийся элемент
        # из первого шарда, а так же номер следующего шарда. Номер страницы
        # внутри шарда должен быть сброшен в 1, поскольку шард изменился.

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=1,
            page=2,
            per_page=2,
            item_getter=self.get_items,
        )
        # В результате должен быть лишь третий элемент
        assert_that(
            results,
            contains('1_3')
        )
        # и функция должна вернуть нам следующий, второй шард
        assert_that(shard, equal_to(2))
        # а номер страницы внутри него сбросить на 1
        assert_that(next_page, equal_to(1))

    def test_third_page(self):
        # Проверим, что на третьей странице вернутся первые элементы из второго
        # шарда.

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=2,
            page=1,
            per_page=2,
            item_getter=self.get_items,
        )
        # В результате должен быть лишь третий элемент
        assert_that(
            results,
            contains('2_1', '2_2')
        )
        # и функция должна вернуть нам тот же, второй шард
        assert_that(shard, equal_to(2))
        # а и второй номер страницы
        assert_that(next_page, equal_to(2))

    def test_forth_page(self):
        # Четвертая страница должна содержать остаток элементов
        # из второго шарда. Тут мы проверяем граничное условие - то что
        # при этом функция вернёт в качестве следующего шарда - 3.

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=2,
            page=2,
            per_page=2,
            item_getter=self.get_items,
        )
        # В результате должен быть лишь третий элемент
        assert_that(
            results,
            contains('2_3', '2_4')
        )
        # и функция должна вернуть нам следущий, третий шард
        assert_that(shard, equal_to(3))
        # и первую страницу в нём
        assert_that(next_page, equal_to(1))

    def test_last_page(self):
        # Последняя страница третьего шарда должна содержать 2 элемента,
        # а следующие шард и страница должны быть None

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=3,
            page=5,
            per_page=2,
            item_getter=self.get_items,
        )
        # В результате должен быть лишь третий элемент
        assert_that(
            results,
            contains('3_9', '3_10')
        )
        # и функция должна вернуть нам следущий, третий шард
        assert_that(shard, none())
        # и первую страницу в нём
        assert_that(next_page, none())

    def test_first_shard_empty(self):
        self.shard_sizes = {
            1: 0,
            2: 4,
            3: 10,
        }

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=None,
            per_page=5,
            item_getter=self.get_items,
        )
        # Всего должно быть 4 первых элемента из второго шарда
        assert_that(
            results,
            contains('2_1', '2_2', '2_3', '2_4')
        )

        # бльше элементов в шарде нет, вернем следующий
        assert_that(shard, equal_to(3))
        assert_that(next_page, equal_to(1))

    def test_middle_shard_empty(self):
        self.shard_sizes = {
            1: 3,
            2: 0,
            3: 10,
        }

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=2,
            page=1,
            per_page=5,
            item_getter=self.get_items,
        )
        # Всего должно быть 5 первых элементов из третьего шарда
        assert_that(
            results,
            contains('3_1', '3_2', '3_3', '3_4', '3_5')
        )

        # в шарде остались элементы
        assert_that(shard, equal_to(3))
        assert_that(next_page, equal_to(2))

    def test_all_shards_empty(self):
        self.shard_sizes = {
            1: 0,
            2: 0,
            3: 0,
        }

        results, shard, next_page = paginate_by_shards(
            shards=self.shards,
            shard=None,
            page=1,
            per_page=5,
            item_getter=self.get_items,
        )

        assert_that(
            results,
            empty()
        )

        # бльше элементов в шарде нет, вернем следующий
        assert_that(shard, equal_to(None))
        assert_that(next_page, equal_to(None))


class TestText_lang_for_notification(TestCase):
    # язык организации
    language = 'ru'

    def test_lang_from_blackbox(self):
        # проверим что мы используем язык пользователя из паспорта
        # если он в списке поддерживаемых языков для организаций

        user_lang = 'en'

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_uid', return_value={'language': user_lang}):
            # для рассылки оповещений используем язык из паспорта
            assert_that(
                lang_for_notification(
                    self.meta_connection, self.main_connection,
                    self.user['id'], self.organization['id'],
                ),
                equal_to(user_lang)
            )

    def test_fallback_to_org_lang(self):
        # если язык пользователя из паспорта не в списке
        # поддерживаемых языков для организации
        # то используем язык организации пользователя

        user_lang = 'de'

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_uid', return_value={'language': user_lang}):
            # для рассылки оповещений используем язык организации
            assert_that(
                lang_for_notification(
                    self.meta_connection, self.main_connection,
                    self.user['id'], self.organization['id'],
                ),
                equal_to(self.organization['language'])
            )


class Test_get_external_org_ids(TestCase):
    def test_get_external_org_ids(self):
        # Функция get_external_org_ids должна возвращать все org_id
        # в которых пользователь состоит и где он не уволен.

        uid = self.user['id']

        # Добавим пользователя в пару организаций
        org1 = self.create_organization(label='domain1', domain_part='.com')
        org2 = self.create_organization(label='domain2', domain_part='.com')
        org3 = self.create_organization(label='domain3', domain_part='.com')

        # Пользователь числится внутридоменным в self.organization
        # обычным сотрудником в org1
        # обычным, но уволенным сотрудником в org2
        # внешним админом в org3
        # Для внешних админов мы org_id в паспорт не проставляем,
        # для уволенных тоже, и org_id организации, которой принадлежит
        # домен, тоже не прописываем.
        # Поэтому в результате список должен содержаться только [org1['id']]
        self.add_user_by_invite(org1, uid, domain='domain1.com')
        self.add_user_by_invite(org2, uid, domain='domain2.com')
        self.dismiss(org2, uid)
        self.add_user_as_outer_admin(org3, uid)

        self.mocked_blackbox.userinfo.return_value = {
            'domain': self.organization['label'] + self.domain_part,
        }
        result = get_external_org_ids(
            self.meta_connection,
            uid,
        )
        assert_that(
            result,
            contains(org1['id'])
        )

    def test_get_external_org_ids_for_external_robot(self):
        # для внешней роботной учетки должен вернутся пустой список
        org = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        org_id = org['organization']['id']

        robot_uid = 100500
        service = ServiceModel(self.meta_connection).create(
            slug='service-with-robot-uid',
            name='Service with custom robot',
            robot_required=True,
            robot_uid=robot_uid,
        )
        robot_nickname = 'yndx-robot-electronic'

        app.blackbox_instance.userinfo.return_value = fake_userinfo(
            login=robot_nickname,
        )

        create_robot_for_service_and_org_id(self.meta_connection,
                                            self.main_connection,
                                            service['slug'],
                                            org_id=org_id)

        robots = UserModel(self.main_connection) \
            .filter(org_id=org_id, is_robot=True) \
            .fields('nickname') \
            .all()

        assert_that(
            robots,
            contains(
                has_entries(
                    nickname=robot_nickname,
                    id=robot_uid,
                )
            )
        )

        result = get_external_org_ids(
            self.meta_connection,
            robot_uid,
        )
        assert_that(
            result,
            empty()
        )


class Test_create_organization(TestCase):
    create_organization = False

    def test_duplicate_label(self):
        label = 'label'
        org1 = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            name='name1',
            domain_part='.org1.yaserv.biz',
            language='ru',
            label=label,
            admin_nickname='admin',
            admin_uid=1,
            admin_first_name='Admin',
            admin_last_name='Adminoff',
            admin_gender='male',
            admin_birthday='1990-05-03',
        )
        org2 = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            name='name2',
            domain_part='.org2.yaserv.biz',
            language='ru',
            label=label,
            admin_nickname='admin',
            admin_uid=1,
            admin_first_name='Admin',
            admin_last_name='Adminoff',
            admin_gender='male',
            admin_birthday='1990-05-03',
        )

        assert_that(
            org1['organization']['label'],
            equal_to(label)

        )
        assert_that(
            org2['organization']['label'],
            all_of(
                not_(
                    equal_to(label)
                ),
                starts_with(label)
            )
        )


class Test_membership(TestCase):
    def test_is_member(self):
        # В простейшем случае пользователь будет сотрудником и функция должна вернуть True.
        org_id = self.organization['id']
        uid = self.user['id']

        assert is_member(
            self.main_connection,
            org_id,
            uid
        ) == True

    def test_dismissed_is_not_member(self):
        # Если сотрудник уволен, то функция должна вернуть False.
        org_id = self.organization['id']
        uid = self.user['id']

        self.dismiss(self.organization, uid)

        assert is_member(
            self.main_connection,
            org_id,
            uid
        ) == False


class Test_add_existing_user(TestCase):

    def setUp(self):
        super(Test_add_existing_user, self).setUp()
        self.new_org = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )

    def test_add_user_without_name(self):
        # фолбечимся на логин для ФИО, если ФИО нет в паспорте

        uid = 1111
        with patch('intranet.yandex_directory.src.yandex_directory.app.blackbox_instance') as mock_blackbox_instance:
            login = 'user_{0}'.format(uid)
            bb_userinfo = fake_userinfo(
                uid=uid,
                login=login,
                first_name=None,
                last_name=None,
                birth_date='2010-10-10',
                sex='0',
                cloud_uid=None
            )
            mock_blackbox_instance.userinfo.return_value = bb_userinfo
            mock_blackbox_instance.batch_userinfo.return_value = [bb_userinfo]

            user = add_existing_user(
                self.meta_connection,
                self.main_connection,
                self.new_org['organization']['id'],
                uid,
            )
            print(user)
            assert_that(
                user,
                has_entries(
                    first_name=login,
                    last_name=login,
                    nickname=login,
                    birthday='2010-10-10',
                )
            )
            user = UserModel(self.main_connection).get(uid)
            assert_that(
                user,
                has_entries(
                    first_name=login,
                    last_name=login,
                    nickname=login,
                    birthday='2010-10-10',
                )
            )
