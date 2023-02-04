# coding: utf-8
import datetime as dt
from testutils import override_settings
from unittest.mock import patch, Mock
from dateutil.relativedelta import relativedelta
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    not_none,
    none,
    contains_inanyorder,
    contains,
)

from testutils import (
    TestCase,
    get_auth_headers,
    create_department,
    format_date,
    TestOrganizationWithoutDomainMixin,
    create_organization_without_domain,
    frozen_time,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    ROOT_DEPARTMENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    InviteModel,
    OrganizationModel,
    ServiceModel,
    UserServiceLicenses,
)
from intranet.yandex_directory.src.yandex_directory.core.views.invite import (
    InviteView,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
)


class TestInviteView__post(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestInviteView__post, self).setUp()
        self.dep_id = 5
        self.org_id = self.yandex_organization['id']
        create_department(
            self.main_connection,
            org_id=self.org_id,
            dep_id=self.dep_id,
        )
        self.headers = get_auth_headers(as_uid=self.yandex_admin['id'])
        self.default_counter = app.config['DEFAULT_INVITE_LIMIT']

    def test_post_existing_departments(self):
        # создаем новый код
        response = self.post_json('/invites/', data={'department_id': self.dep_id}, headers=self.headers)

        assert_that(
            response,
            has_entries(
                code=not_none()
            )
        )

        code = InviteModel(self.meta_connection).get(response['code'])
        assert_that(
            code,
            has_entries(
                created_at=not_none(),
                org_id=self.org_id,
                department_id=self.dep_id,
                last_use=none(),
                counter=self.default_counter,
                enabled=True,
                author_id=self.yandex_admin['id'],
            )
        )

    def test_post_not_existing_departments(self):
        # создаем новый код
        response = self.post_json('/invites/', data={'department_id': 100}, headers=self.headers)

        assert_that(
            response,
            has_entries(
                code=not_none()
            )
        )

        code = InviteModel(self.meta_connection).get(response['code'])
        assert_that(
            code,
            has_entries(
                created_at=not_none(),
                org_id=self.org_id,
                department_id=ROOT_DEPARTMENT_ID,
                last_use=none(),
                counter=self.default_counter,
                enabled=True,
                author_id=self.yandex_admin['id'],
            )
        )

    def test_limit(self):
        # Проверим, что на количество инвайтов есть лимит в 100 штук
        # и на 101 будет ошибка.

        second_org = create_organization_without_domain(
            self.meta_connection,
            self.main_connection,
        )
        limit = 100
        for i in range(limit):
            self.post_json('/invites/', data={'department_id': 1}, headers=self.headers)

        self.post_json(
            '/invites/',
            data={'department_id': 1},
            headers=self.headers,
            expected_code=403,
            expected_error_code='too_many_invites',
            expected_message='Too many invites',
        )

        # при этом на второй организации инвайт должен отправиться успешно
        headers = get_auth_headers(
            as_uid=second_org['admin_user_uid'],
        )
        self.post_json(
            '/invites/',
            data={'department_id': 1},
            headers=headers,
            expected_code=201,
        )

    def test_limit_expired(self):
        limit = 10

        with override_settings(MAX_INVITES_COUNT=limit), frozen_time():
            invite_data = {'department_id': self.dep_id, 'ttl': 1000}
            for i in range(limit - 1):
                self.post_json(
                    '/invites/',
                    data=invite_data,
                    headers=self.headers
                )

            # add expired invite
            code = InviteModel(self.meta_connection).create(
                self.org_id,
                self.dep_id,
                self.yandex_admin['id'],
            )
            InviteModel(self.meta_connection).update(
                update_data={'valid_to': utcnow() - dt.timedelta(seconds=20)},
                filter_data={'code': code},
            )

            self.post_json(
                '/invites/',
                data=invite_data,
                headers=self.headers,
                expected_code=201,
            )

            self.post_json(
                '/invites/',
                data=invite_data,
                headers=self.headers,
                expected_code=403,
                expected_error_code='too_many_invites',
                expected_message='Too many invites',
            )


    def test_internal(self):
        # проверяем что ручка internal
        assert_that(
            InviteView.post.__dict__.get('internal', False),
            equal_to(True)
        )

    def test_with_add_license(self):
        self.user_uid = 10
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': self.user_uid,
            'default_email': 'default@ya.ru',
        }
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id1',
            slug='tracker',
            name='tracker',
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org_id,
            'tracker',
        )
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.org_id,
                author_id=self.yandex_admin['id'],
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
            response = self.post_json(
                '/invites/',
                data={
                    'department_id': 1,
                    'add_license': True,
                    'service_slug': 'tracker'
                },
                headers=self.headers
            )

        assert InviteModel(self.meta_connection).get(response['code'])['add_license'] is True

        self.post_json(
            '/invites/{}/use/'.format(response['code']),
            data=None,
            headers=get_auth_headers(as_uid=self.user_uid),
        )

        licenses = UserServiceLicenses(self.main_connection).get_users_with_service_licenses(
            self.org_id,
            service['id'],
            [self.user_uid],
        )
        tracker_licenses = licenses[service['id']]
        assert len(tracker_licenses) == 1
        assert tracker_licenses[0] == self.user_uid

    def test_create_invite_with_counter_and_ttl(self):
        tll = 50
        counter = 42
        with frozen_time():
            response = self.post_json(
                '/invites/',
                data={
                    'department_id': self.dep_id,
                    'counter': counter,
                    'ttl': tll,
                },
                headers=self.headers,
            )

            code = InviteModel(self.meta_connection).get(response['code'])
            self.assertEqual(code['counter'], 42)
            self.assertEqual(code['valid_to'], utcnow() + dt.timedelta(seconds=tll))

    def test_create_invite_with_service_slug(self):
        service_slug = 'example_slug'
        with frozen_time():
            response = self.post_json(
                '/invites/',
                data={
                    'department_id': self.dep_id,
                    'service_slug': service_slug,
                },
                headers=self.headers,
            )

            code = InviteModel(self.meta_connection).get(response['code'])
            self.assertEqual(code['service_slug'], service_slug)

    def test_raise_on_expired_code(self):
        with frozen_time():
            code_1 = InviteModel(self.meta_connection).create(
                self.org_id,
                self.dep_id,
                self.yandex_admin['id'],
            )
            InviteModel(self.meta_connection).update(
                update_data={'valid_to': utcnow() - dt.timedelta(seconds=20)},
                filter_data={'code': code_1}
            )
            response = self.post_json(
                '/invites/{}/use/'.format(code_1),
                data=None,
                expected_code=409,
            )
            self.assertEqual(response['code'], 'code_expired')

    def test_success_on_not_expired_code(self):
        self.user_uid = 10
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'mr-fox',
                'firstname': 'Тест',
                'lastname': 'Тестов',
                'sex': '1',
                'birth_date': '1999-01-01',
            },
            'uid': self.user_uid,
            'default_email': 'default@ya.ru',
        }

        with frozen_time():
            code_1 = InviteModel(self.meta_connection).create(
                self.org_id,
                self.dep_id,
                self.yandex_admin['id'],
            )
            InviteModel(self.meta_connection).update(
                update_data={'valid_to': utcnow() + dt.timedelta(seconds=20)},
                filter_data={'code': code_1}
            )
            self.post_json(
                '/invites/{}/use/'.format(code_1),
                data=None,
                headers=get_auth_headers(as_uid=self.user_uid),
            )

    def test_create_service_invite(self):
        wait = 30
        mail_campaign_slug = 'AS3DA23SD-ASD34'

        response = self.post_json('/invites/', data={'department_id': self.dep_id, 'wait': wait, 'mail_campaign_slug': mail_campaign_slug}, headers=self.headers)

        assert_that(
            response,
            has_entries(
                code=not_none()
            )
        )

        code = InviteModel(self.meta_connection).get(response['code'])
        assert_that(
            code,
            has_entries(
                created_at=not_none(),
                org_id=self.org_id,
                department_id=self.dep_id,
                last_use=none(),
                counter=self.default_counter,
                enabled=True,
                author_id=self.yandex_admin['id'],
                wait=wait,
                mail_campaign_slug=mail_campaign_slug,
                service_slug='autotest',
            )
        )


class TestInviteView__get(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestInviteView__get, self).setUp()
        self.org_id = self.yandex_organization['id']
        self.headers = get_auth_headers(as_uid=self.yandex_admin['id'])

        code_1 = InviteModel(self.meta_connection).create(
            self.org_id,
            ROOT_DEPARTMENT_ID,
            self.yandex_admin['id'],
        )
        self.enabled_invite_1 = _prepare_invite(InviteModel(self.meta_connection).get(code_1))

        code_2 = InviteModel(self.meta_connection).create(
            self.org_id,
            ROOT_DEPARTMENT_ID,
            self.yandex_admin['id'],
        )
        self.enabled_invite_2 = _prepare_invite(InviteModel(self.meta_connection).get(code_2))

        code_3 = InviteModel(self.meta_connection).create(
            self.org_id,
            ROOT_DEPARTMENT_ID,
            self.yandex_admin['id'],
        )
        InviteModel(self.meta_connection).filter(code=code_3).update(enabled=False)
        self.disabled_invite = _prepare_invite(InviteModel(self.meta_connection).get(code_3))

        # Инвайт-код в другой организации
        InviteModel(self.meta_connection).create(
            self.organization['id'],
            ROOT_DEPARTMENT_ID,
            self.admin_uid,
        )

    def test_get_all(self):
        # Возвращаются все коды, активные и неактивные
        response = self.get_json('/invites/', headers=self.headers)
        assert_that(
            response['result'],
            contains_inanyorder(
                self.enabled_invite_1,
                self.enabled_invite_2,
                self.disabled_invite,
            )
        )

    def test_get_enabled(self):
        # Возвращаются только активные коды
        response = self.get_json('/invites/?enabled=1', headers=self.headers)
        assert_that(
            response['result'],
            contains_inanyorder(
                self.enabled_invite_1,
                self.enabled_invite_2,
            )
        )

    def test_get_disabled(self):
        # Возвращаются только неактивные коды
        response = self.get_json('/invites/?enabled=0', headers=self.headers)
        assert_that(
            response['result'],
            contains_inanyorder(
                self.disabled_invite,
            )
        )

    def test_get_ordering_created_at(self):
        # Сортировка по дате создания
        response = self.get_json('/invites/?ordering=created_at', headers=self.headers)
        assert_that(
            response['result'],
            contains(
                self.enabled_invite_1,
                self.enabled_invite_2,
                self.disabled_invite,
            )
        )
        response = self.get_json('/invites/?ordering=-created_at', headers=self.headers)
        assert_that(
            response['result'],
            contains(
                self.disabled_invite,
                self.enabled_invite_2,
                self.enabled_invite_1,
            )
        )

    def test_get_ordering_last_use(self):
        # Сортировка по дате изменения
        InviteModel(self.meta_connection).\
            filter(code=self.enabled_invite_1['code']).\
            update(last_use=utcnow() - relativedelta(days=1))
        InviteModel(self.meta_connection).\
            filter(code=self.enabled_invite_2['code']).\
            update(last_use=utcnow() - relativedelta(days=3))
        InviteModel(self.meta_connection).\
            filter(code=self.disabled_invite['code']).\
            update(last_use=utcnow() - relativedelta(days=2))
        self.enabled_invite_1 = _prepare_invite(InviteModel(self.meta_connection).get(self.enabled_invite_1['code']))
        self.enabled_invite_2 = _prepare_invite(InviteModel(self.meta_connection).get(self.enabled_invite_2['code']))
        self.disabled_invite = _prepare_invite(InviteModel(self.meta_connection).get(self.disabled_invite['code']))

        response = self.get_json('/invites/?ordering=last_use', headers=self.headers)
        assert_that(
            response['result'],
            contains(
                self.enabled_invite_2,
                self.disabled_invite,
                self.enabled_invite_1,
            )
        )
        response = self.get_json('/invites/?ordering=-last_use', headers=self.headers)
        assert_that(
            response['result'],
            contains(
                self.enabled_invite_1,
                self.disabled_invite,
                self.enabled_invite_2,
            )
        )


def _prepare_invite(invite):
    del invite['org_id']
    del invite['wait']
    del invite['mail_campaign_slug']
    del invite['service_slug']
    del invite['add_license']
    invite['created_at'] = format_date(invite['created_at'])
    invite['last_use'] = format_date(invite['last_use'])
    return invite
