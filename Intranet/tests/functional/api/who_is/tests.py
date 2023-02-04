# -*- coding: utf-8 -*-
import datetime
import responses
from hamcrest import (
    assert_that,
    has_entries,
    equal_to,
)
from unittest.mock import patch
from testutils import (
    TestCase,
    create_organization,
    create_user,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DepartmentModel,
    GroupModel,
    UserModel,
    UserMetaModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.domain import (
    DomainModel,
)
from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client


class TestWhoIs__get_by_email(TestCase):
    def setUp(self):
        super(TestWhoIs__get_by_email, self).setUp()

        self.org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='org',
            domain_part='-domain.org',
        )
        self.org_id = self.org['organization']['id']

        self.department_name = {
            'ru': 'Тестовый Департамент',
            'en': 'Test Department'
        }

        self.department = DepartmentModel(self.main_connection).create(
            name=self.department_name,
            label='test-department',
            org_id=self.org_id,
            external_id='external_id',
        )

        self.group_name = {
            'ru': 'Группа',
            'en': 'Group',
        }
        self.group = GroupModel(self.main_connection).create(
            name=self.group_name,
            label='test-group',
            org_id=self.org_id,
        )

        self.user = create_user(
            self.meta_connection,
            self.main_connection,
            user_id=123,
            nickname='test-user',
            name={'first': {'ru': 'Username'}},
            email='test-user@org-domain.org',
            org_id=self.org_id,
            department_id=None,
        )

        app.blackbox_instance.userinfo.return_value = {}

    def test_get_department(self):
        response_data = self.get_json(r'/v4/who-is/?email=test-department@org-domain.org')
        assert_that(
            response_data,
            has_entries(
                org_id=self.org_id,
                type='department',
                object_id=self.department['id'],
            )
        )

    def test_get_group(self):
        response_data = self.get_json(r'/v4/who-is/?email=test-group@org-domain.org')
        assert_that(
            response_data,
            has_entries(
                org_id=self.org_id,
                type='group',
                object_id=self.group['id'],
            )
        )

    def test_get_user(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.who_is.get_user_data_from_blackbox_by_login') as mock_user_data:
            mock_user_data.return_value = {
                'is_maillist': False,
                'uid': self.user['id']
            }
            response_data = self.get_json(r'/v4/who-is/?email=test-user@org-domain.org')
        assert_that(
            response_data,
            has_entries(
                org_id=self.org_id,
                type='user',
                object_id=self.user['id'],
            )
        )

    def test_get_not_exist_object(self):
        self.get_json(r'/v4/who-is/?email=random@random.org', expected_code=404)
        self.get_json(r'/v4/who-is/', expected_code=422)
        self.get_json(r'/v4/who-is/?test=random@random.org', expected_code=422)
        self.get_json(r'/v4/who-is/?email=random', expected_code=422)
        self.get_json(r'/v4/who-is/?email=random@', expected_code=422)
        self.get_json(r'/v4/who-is/?email=@random.org', expected_code=422)

    def test_lazy_migration(self):
        # досоздаем пользователя, если его нет у нас, но есть в паспорте
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.who_is.get_user_data_from_blackbox_by_login') as mock_user_data:
            uid = 1130000011111111
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': 'only_passport_user@khunafin.xyz',
                'first_name': 'user',
                'is_maillist': False,
                'language': 'ru',
                'last_name': 'user',
                'login': 'only_passport_user@khunafin.xyz',
                'sex': '1',
                'uid': uid,
                'org_ids': [self.org_id],
            }
            response = self.get_json(r'/v4/who-is/?email=only_passport_user@org-domain.org')

        expected = {
            'object_id': uid,
            'org_id': self.org_id,
            'type': 'user'
        }
        assert_that(response, equal_to(expected))

        # пользователь создался
        user = UserModel(self.main_connection) \
            .filter(id=uid) \
            .fields('org_id', 'nickname', 'gender', 'birthday') \
            .one()

        assert_that(
            user,
            equal_to({
                'id': uid,
                'org_id': self.org_id,
                'nickname': 'only_passport_user',
                'gender': 'male',
                'birthday': datetime.datetime.strptime('2000-01-01', '%Y-%m-%d').date()
            })
        )

    def test_lazy_migration_when_user_is_in_main_db(self):
        # досоздаем пользователя, если его нет у нас, но есть в паспорте

        # Представим, что данные пользователя уже почему-то есть в main базе,
        # но в meta почему-то отсутствуют
        uid = 1130000011111111
        nickname = 'only_passport_user@org-domain.org'
        email = nickname
        original_first = 'vasilisa'
        original_last = 'ivanova'
        new_last = 'pupkina'

        user = UserModel(self.main_connection) \
            .create(
                uid,
                nickname,
                {'first': original_first, 'last': original_last},
                email,
                'female',
                self.org_id,
            )

        with patch('intranet.yandex_directory.src.yandex_directory.core.views.who_is.get_user_data_from_blackbox_by_login') as mock_user_data:
            mock_user_data.return_value = {
                'aliases': [],
                'birth_date': '2000-01-01',
                'default_email': email,
                'first_name': original_first,
                'is_maillist': False,
                'language': 'ru',
                # В Паспорте имя сменилось и далее мы должны его обновить и в Директории
                'last_name': new_last,
                'login': nickname,
                'sex': '2',
                'uid': uid,
                'org_ids': [self.org_id],
            }
            self.get_json(r'/v4/who-is/?email=' + email)

        # У пользователя должна обновиться фамилия
        user = UserModel(self.main_connection) \
            .filter(id=uid) \
            .fields('org_id', 'name') \
            .one()

        assert_that(
            user,
            has_entries(
                name=has_entries(
                    first=equal_to({'ru': original_first}),
                    last=equal_to({'ru': new_last}),
                )
            )
        )


class TestWhoIs__get_by_domain(TestCase):
    def setUp(self):
        super(TestWhoIs__get_by_domain, self).setUp()

        self.org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='org',
            domain_part='-domain.org',
        )
        self.org_id = self.org['organization']['id']

        self.org1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='someorg1',
            domain_part='-domain1.org',
        )
        self.org_id1 = self.org1['organization']['id']

        self.org2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='someorg2',
            domain_part='-domain2.org',
        )
        self.org_id2 = self.org2['organization']['id']

        DomainModel(self.main_connection).create('org-domain_smthn.org', self.org_id1, owned=True)
        DomainModel(self.main_connection).create('org-domain_smthn.org', self.org_id2, owned=True)

        app.blackbox_instance.userinfo.return_value = {}
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []

    def test_get_by_domain(self):
        response_data = self.get_json(r'/v4/who-is/?domain=org-domain.org', expected_code=200)
        assert_that(
            response_data,
            dict(
                org_id=self.org_id,
                type='domain',
                object_id='org-domain.org',
            )
        )

    @responses.activate
    @override_settings(DOMENATOR_WHO_IS_DOMAIN_PROXY=True)
    def test_get_by_domain_proxy(self):
        domain = 'test_domenator.ru'
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/domains/who-is/?domain={domain}',
            json={'org_id': 123, 'object_id': domain, 'type': 'domain'},
        )

        response_data = self.get_json(f'/v4/who-is/?domain={domain}', expected_code=200)
        assert_that(
            response_data,
            dict(
                org_id=123,
                type='domain',
                object_id=domain,
            )
        )

    @responses.activate
    @override_settings(DOMENATOR_WHO_IS_DOMAIN_PROXY=True)
    def test_get_by_domain_proxy_fail(self):
        domain = 'test_domenator.ru'

        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/domains/who-is/?domain={domain}',
            status=404,
        )

        self.get_json(
            f'/v4/who-is/?domain={domain}',
            expected_code=404
        )

    def test_get_by_domain_conflict(self):
        response_data = self.get_json(r'/v4/who-is/?domain=org-domain_smthn.org', expected_code=409)
        assert_that(
            response_data,
            {'params': {'id_list': [108076, 108077]},
             'code': 'org_id_conflict',
             'message': 'Ownership connflict "{id_list}"'}
        )

    def test_get_not_exist_object(self):
        self.get_json(r'/v4/who-is/?email=random@random.org', expected_code=404)
        self.get_json(r'/v4/who-is/', expected_code=422)
        self.get_json(r'/v4/who-is/?test=random@random.org', expected_code=422)
        self.get_json(r'/v4/who-is/?email=random', expected_code=422)
        self.get_json(r'/v4/who-is/?email=random@', expected_code=422)
        self.get_json(r'/v4/who-is/?email=@random.org', expected_code=422)
        # дополнительные тексты после дополнения поиска по домену
        self.get_json(r'/v4/who-is/?email=@random.org&domain=random.org', expected_code=422)
