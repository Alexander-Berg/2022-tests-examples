# -*- coding: utf-8 -*-
import responses
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    has_entries,
    contains,
    has_key,
    has_item,
    has_items,
)

from testutils import (
    TestCase,
    get_auth_headers,
    create_organization,
)
from unittest.mock import (
    ANY,
    patch,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models.access_restore import (
    OrganizationAccessRestoreModel,
    RestoreTypes,
)
from intranet.yandex_directory.src.yandex_directory.access_restore.views.restore import control_answers_is_valid
from intranet.yandex_directory.src.yandex_directory.common.utils import format_datetime
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from . import control_answers


NEW_OUTER_ADMIN_UID = 11111111


class TestRestoreListView__get(TestCase):
    def setUp(self):
        super(TestRestoreListView__get, self).setUp()
        self.create_params = {
            'domain': self.organization_domain,
            'new_admin_uid': self.admin_uid,
            'old_admin_uid': 111,
            'org_id': 10500,
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**self.create_params)

    def test_get_simple(self):
        response = self.get_json('/restore/')
        assert_that(
            response,
            equal_to([{
                'restore_id': self.first_restore['id'],
                'expires_at': format_datetime(self.first_restore['expires_at']),
                'created_at': format_datetime(self.first_restore['created_at']),
                'domain': self.organization_domain,
                'state': 'in_progress',
            }])
        )

    def test_get_statuses_except_expired(self):
        # получаем все статусы, кроме expired
        other_domain = 'my.domain'
        other_create_params = self.create_params.copy()
        other_create_params['domain'] = other_domain

        other_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**other_create_params)
        response = self.get_json('/restore/')

        # пролучим 2 заявки
        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    restore_id=self.first_restore['id'],
                    domain=self.organization_domain,
                    state='in_progress',
                ),
                has_entries(
                    restore_id=other_restore['id'],
                    domain=other_domain,
                    state='in_progress',
                )
            )
        )

        # "просрочим" одну из заявок
        OrganizationAccessRestoreModel(self.meta_connection).update(
            {'state': 'expired'},
            {'domain': other_domain}
        )

        for state in RestoreTypes.valid_types:
            OrganizationAccessRestoreModel(self.meta_connection).update(
                {'state': state},
                {'domain': self.organization_domain}
            )
            response = self.get_json('/restore/')
            assert_that(
                response,
                contains(
                    has_entries(
                        restore_id=self.first_restore['id'],
                        domain=self.organization_domain,
                        state=state,
                    ),
                )
            )


class TestRestoreListView__post(TestCase):
    def setUp(self):
        super(TestRestoreListView__post, self).setUp()
        self.post_data = {
            'domain': self.organization_domain,
            'control_answers': {
                'admins': ['123'],
                'maillists': [],
                'users': ['email@example.com'],
                'forgot_admins': False,
                'no_users': False,
                'no_maillists': True,
                'enabled_services': [],
                'paid': False,
            }
        }

        self.new_admin_uid = 111111
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'

    def test_post_simple(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils._send_email') as send_email, \
                patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.control_answers_is_valid', return_value=True):
            response = self.post_json(
                '/restore/',
                data=self.post_data,
                headers=get_auth_headers(as_uid=self.new_admin_uid),
            )

        # письмо о попытке получении владения
        send_email.assert_called_with(
            ANY,
            ANY,
            self.organization['id'],
            ANY,
            ANY,
            self.organization_domain,
            app.config['SENDER_CAMPAIGN_SLUG']['ACCESS_RESTORE_TRY_RESTORE'],
            organization_name=ANY,
            date=ANY,
            ip=ANY,
        )

        assert_that(
            response,
            has_entries(
                restore_id=ANY,
                state='in_progress',
            ),
        )

    def test_post_with_same_domain(self):
        # если пользователь пытается создать еще одну завяку для данного домена, то возвращаем текущую
        with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.control_answers_is_valid', return_value=True):
            response_first = self.post_json(
                '/restore/',
                data=self.post_data,
                headers=get_auth_headers(as_uid=NEW_OUTER_ADMIN_UID)
            )
        response_second = self.post_json(
            '/restore/',
            data=self.post_data,
            headers=get_auth_headers(as_uid=NEW_OUTER_ADMIN_UID)
        )

        assert_that(
            response_second,
            has_entries(
                restore_id=response_first['restore_id'],
                state='in_progress',
            ),
        )

        # первая заявка завершилась со статусом fail
        OrganizationAccessRestoreModel(self.meta_connection).update(
            {'state': 'fail'},
            {'domain': self.organization_domain}
        )
        # тогда создаем новую
        with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.control_answers_is_valid', return_value=True):
            response_third = self.post_json(
                '/restore/',
                data=self.post_data,
                headers=get_auth_headers(as_uid=NEW_OUTER_ADMIN_UID)
            )
        active_record = OrganizationAccessRestoreModel(self.meta_connection)\
            .filter(domain=self.organization_domain, state='in_progress')\
            .one()

        assert response_third['restore_id'] != response_first['restore_id']

        assert_that(
            response_third,
            has_entries(
                restore_id=active_record['id'],
                state='in_progress',
            ),
        )

    def test_post_new_admin_is_owner(self):
        # self.admin_uid уже администратор организации
        response = self.post_json(
            '/restore/',
            data=self.post_data,
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='new_admin_already_owner',
            )
        )

    def test_post_new_admin_from_other_org(self):
        # новый админ - внутренняя учетка не из данной орагнизации
        innner_user_uid = 111*10**13 + 77777
        response = self.post_json(
            '/restore/',
            data=self.post_data,
            headers=get_auth_headers(as_uid=innner_user_uid),
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='new_admin_from_other_organization',
            )
        )

    @responses.activate
    def test_invalid_domain(self):
        # домена нет в коннекте
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/',
            json=[],
            status=200,
        )
        post_data = self.post_data.copy()
        post_data['domain'] = 'hello.world'
        self.post_json(
            '/restore/',
            data=post_data,
            headers=get_auth_headers(as_uid=self.admin_uid),
            expected_code=404
        )

    def test_invalid_answers(self):
        # неверные ответы на вопросы
        with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.control_answers_is_valid', return_value=False):
            response = self.post_json(
                '/restore/',
                data=self.post_data,
                headers=get_auth_headers(as_uid=NEW_OUTER_ADMIN_UID)
            )
            assert_that(
                response,
                has_entries(
                    restore_id=ANY,
                    state='invalid_answers',
                ),
            )

    def test_post_with_valid_data(self):
        post_data = {
            'domain': self.organization_domain,
            'control_answers': control_answers.all_cases['has_admin_no_users_no_maillists']
        }
        with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.get_user_id_from_passport_by_login',
                   return_value=self.admin_uid):
            response = self.post_json(
                '/restore/',
                data=post_data,
                headers=get_auth_headers(as_uid=NEW_OUTER_ADMIN_UID)
            )
            assert_that(
                response,
                has_entries(
                    restore_id=ANY,
                    state='in_progress',
                ),
            )


class TestRestoreDetailView(TestCase):
    def setUp(self):
        super(TestRestoreDetailView, self).setUp()
        self.domain = 'whereismyadmin.com'
        create_params = {
            'domain': self.domain,
            'new_admin_uid': self.admin_uid,
            'old_admin_uid': 111,
            'org_id': 10500,
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**create_params)

    def test_get(self):
        response = self.get_json('/restore/{}/'.format(self.first_restore['id']))
        assert_that(
            response,
            has_entries(
                restore_id=self.first_restore['id'],
                domain=self.domain,
                state='in_progress',
            ),
        )

        self.get_json('/restore/{}/'.format('23rhff74bergeubg854'), expected_code=404)


class Test_control_answers_is_valid(TestCase):
    def check_answers(self, answers, org_id):
        return control_answers_is_valid(self.meta_connection, answers, org_id, self.shard, self.organization_domain)

    def assert_cases(self, happy_cases, org_id, passport_uid):
        all_cases = control_answers.all_cases

        with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.get_user_id_from_passport_by_login',
                   return_value=passport_uid):
            # явно проверим все хеппи кейсы, чтобы исключить опечатки
            for case_name in happy_cases:
                assert self.check_answers(all_cases[case_name], org_id), case_name

            for case_name, case in all_cases.items():
                if case_name not in happy_cases:
                    assert not self.check_answers(case, org_id), case_name

    def test_empty_org_with_outer_admin(self):
        outer_admin_uid = 1234
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            admin_uid=outer_admin_uid,
            root_dep_label='all',
        )['organization']

        happy_cases = [
            'has_admin_no_users_no_maillists',
        ]

        self.assert_cases(happy_cases, my_org['id'], [outer_admin_uid])

    def test_empty_org_with_outer_deputy(self):
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            admin_uid=909090,
            root_dep_label='all',
        )['organization']

        outer_admin_uid = 7070707
        self.create_deputy_admin(
            org_id=my_org['id'],
            is_outer=True,
            uid=outer_admin_uid
        )

        happy_cases = [
            'has_admin_no_users_no_maillists',
        ]
        self.assert_cases(happy_cases, my_org['id'], [outer_admin_uid])

    def test_empty_org_with_inner_admin(self):
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            root_dep_label='all',
            admin_uid=control_answers.inner_admin_uid,
        )['organization']

        happy_cases = [
            'has_admin_no_users_no_maillists',
            'forgot_admins_has_inner_admin_as_user_no_maillists',
            'has_inner_admin_as_admin_has_inner_admin_as_user_no_maillists',
        ]

        self.assert_cases(happy_cases, my_org['id'], [my_org['admin_uid']])

    def test_outer_admin_with_users_no_maillists(self):
        outer_admin_uid = 1234
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            admin_uid=outer_admin_uid,
            root_dep_label='all',
        )['organization']

        for login in control_answers.valid_users:
            self.create_user(org_id=my_org['id'], nickname=login)

        happy_cases = [
            'forgot_admins_valid_users_no_maillists',
            'has_admin_valid_users_no_maillists',
            'has_admin_valid_users_all_maillist',
            'forgot_admins_valid_users_all_maillist',
            'has_admin_valid_users_with_incorrect_no_maillists',
            'forgot_admins_one_valid_user_no_maillists',  # в оргнизации 3 пользователя, этот кейс подходит
            'has_admin_valid_users_valid_maillists',  # пока пропускаем такой вариант
            'has_admin_valid_users_valid_maillists_with_all',  # пока пропускаем такой вариант
            'has_inner_admin_valid_users_with_inner_admin_no_maillists', # из-за моков это тоже хороший кейс
        ]

        self.assert_cases(happy_cases, my_org['id'], [outer_admin_uid])

    def test_inner_admin_with_users_no_maillists(self):
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            root_dep_label='all',
            admin_uid=control_answers.inner_admin_uid,
        )['organization']

        for login in control_answers.valid_users:
            self.create_user(org_id=my_org['id'], nickname=login)

        happy_cases = [
            'forgot_admins_valid_users_all_maillist',
            'forgot_admins_valid_users_no_maillists',
            'has_admin_valid_users_no_maillists',
            'has_admin_valid_users_all_maillist',
            'has_admin_valid_users_with_incorrect_no_maillists',
            'has_inner_admin_valid_users_with_inner_admin_no_maillists',
            'has_admin_valid_users_valid_maillists',  # пока пропускаем такой вариант
            'has_admin_valid_users_valid_maillists_with_all',  # пока пропускаем такой вариант
        ]

        self.assert_cases(happy_cases, my_org['id'], [my_org['admin_uid']])

    def test_inner_admin_with_users_with_maillists(self):
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            root_dep_label='all',
            admin_uid=control_answers.inner_admin_uid,
        )['organization']

        for login in control_answers.valid_users:
            self.create_user(org_id=my_org['id'], nickname=login)

        for ml in control_answers.valid_maillists:
            self.create_group(my_org['id'], label=ml, author_id=my_org['admin_uid'])

        happy_cases = [
            'has_admin_valid_users_valid_maillists',
            'has_admin_valid_users_valid_maillists_with_all',
            'has_admin_valid_users_all_maillist',
            'has_admin_valid_users_with_incorrect_no_maillists',
            'has_inner_admin_valid_users_with_inner_admin_no_maillists',
            'forgot_admin_valid_users_valid_maillists',
            'has_admin_valid_users_no_maillists',  # пока пропускаем такой вариант
        ]

        self.assert_cases(happy_cases, my_org['id'], [my_org['admin_uid']])

    def test_non_exists_admins(self):
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            admin_uid=9999,
            root_dep_label='all',
        )['organization']

        self.assert_cases([], my_org['id'], [None, None])

    def test_inner_admin_yandex_with_users_with_maillists(self):
        # спец кейс, когда в поле admins введен логин без домена, это внутренний админ,
        # но при этом в паспорте есть такая же учетка на домене yandex.ru
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            root_dep_label='all',
            admin_uid=control_answers.inner_admin_uid,
        )['organization']

        for login in control_answers.valid_users:
            self.create_user(org_id=my_org['id'], nickname=login)

        for ml in control_answers.valid_maillists:
            self.create_group(my_org['id'], label=ml, author_id=my_org['admin_uid'])

        happy_cases = [
            'has_admin_valid_users_valid_maillists',
            'has_admin_valid_users_valid_maillists_with_all',
            'has_inner_admin_valid_users_with_inner_admin_no_maillists',
            'has_admin_valid_users_with_incorrect_no_maillists',
            'forgot_admin_valid_users_valid_maillists',
            'has_admin_valid_users_all_maillist',  # пока пропускаем такой вариант
            'has_admin_valid_users_no_maillists',  # пока пропускаем такой вариант
        ]

        return_uid = 123456  # рандомный uid, как будто в яндексе есть учетка с логин admin
        self.assert_cases(happy_cases, my_org['id'], [return_uid, my_org['admin_uid']])

    def test_inner_admin_users_with_aliases(self):
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            root_dep_label='all',
            admin_uid=control_answers.inner_admin_uid,
        )['organization']

        for login, alias in zip(
                ['u1', 'u2', control_answers.valid_users[2]],
                control_answers.valid_user_aliases
        ):
            self.create_user(org_id=my_org['id'], nickname=login, aliases=[alias])

        for ml in control_answers.valid_maillists:
            self.create_group(my_org['id'], label=ml, author_id=my_org['admin_uid'])

        happy_cases = [
            'forgot_admin_valid_aliases_valid_maillists',
        ]

        self.assert_cases(happy_cases, my_org['id'], [my_org['admin_uid']])

    def test_min_user_count(self):
        # проверяем, что работает порог на минимальное кол-во вводимых ящиков
        my_org = create_organization(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            language='ru',
            label='new_org',
            root_dep_label='all',
            admin_uid=control_answers.inner_admin_uid,
        )['organization']

        for login in control_answers.valid_users:
            self.create_user(org_id=my_org['id'], nickname=login)

        for _ in range(4):
            self.create_user(org_id=my_org['id'])

        happy_cases = [
            'forgot_admins_valid_users_all_maillist',
            'has_admin_valid_users_valid_maillists',
            'has_admin_valid_users_valid_maillists_with_all',
            'has_admin_valid_users_all_maillist',
            'has_admin_valid_users_no_maillists',
            'has_admin_valid_users_with_incorrect_no_maillists',
            'has_inner_admin_valid_users_with_inner_admin_no_maillists',
            'forgot_admins_valid_users_no_maillists',
            'has_admin_valid_users_with_incorrect_no_maillists',
        ]

        self.assert_cases(happy_cases, my_org['id'], [my_org['admin_uid']])

        for _ in range(40):
            self.create_user(org_id=my_org['id'])

        happy_cases = [
            'has_admin_valid_users_with_incorrect_no_maillists'
        ]
        self.assert_cases(happy_cases, my_org['id'], [my_org['admin_uid']])


class TestMaskOwnerLoginView(TestCase):
    def test_get_owner_login(self):
        test_cases = [
            ({'default_email': 'admin@yandex.ru'}, 'a***n@yandex.ru'),
            ({'default_email': 'admin@domain.com'}, 'a***n@domain.com'),
            ({'default_email': 'lol@domain.com'}, 'l***l@domain.com'),
            ({'default_email': 'ab@domain.com'}, '***@domain.com'),
            ({'default_email': '', 'login': 'admin'}, 'a***n@yandex.ru'),
        ]
        for case in test_cases:
            with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.get_user_data_from_blackbox_by_uid',
                       return_value=case[0]):
                response = self.get_json(
                    '/restore/current-owner/{}/'.format(self.organization_domain),
                )
                assert response['owner_login'] == case[1], case

    @responses.activate
    def test_incorrect_domain(self):
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/',
            json=[],
            status=200,
        )
        self.get_json(
            '/restore/current-owner/{}/'.format('super_domain'),
            expected_code=404,
        )

    def test_incorrect_owner_uid(self):
        with patch('intranet.yandex_directory.src.yandex_directory.access_restore.views.restore.get_user_data_from_blackbox_by_uid',
                   return_value=None):
            self.get_json(
                '/restore/current-owner/{}/'.format(self.organization_domain),
                expected_code=404,
            )
