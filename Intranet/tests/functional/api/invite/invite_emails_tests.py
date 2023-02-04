# coding: utf-8

from hamcrest import (
    assert_that,
    has_entries,
    contains,
    not_none)
from unittest.mock import (
    patch,
)

from testutils import (
    assert_called,
    TestCase,
    get_auth_headers,
    TestOrganizationWithoutDomainMixin,
)
from intranet.yandex_directory.src.yandex_directory.common.models.types import (
    ROOT_DEPARTMENT_ID,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    InviteModel,
    OrganizationModel,
)


class TestInviteEmailsView(TestOrganizationWithoutDomainMixin, TestCase):
    def setUp(self):
        super(TestInviteEmailsView,self).setUp()
        self.headers = get_auth_headers(as_uid=self.yandex_admin['id'])

    def test_invite_emails(self):
        # Приглашаем 3 пользователя
        # Прверяем, что сгенерится 3 инвайт кода
        # Проверяем, что мок рассылятора вызовется 3 раза
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.send') as mock_send:
            self.post_json(
                '/invites/emails/',
                data={
                    'department_id': ROOT_DEPARTMENT_ID,
                    'emails': [
                        'testmail1@my-domain.test.com',
                        'testmail2@yandex.ru',
                        'почта@домен.рф'
                    ]
                },
                headers=self.headers,
                expected_code=200,
            )
            invite = InviteModel(self.meta_connection).find(filter_data={'org_id': self.yandex_organization['id']})
            assert_that(
                invite,
                contains(
                    has_entries(counter=3)
                )
            )
            assert_called(mock_send, 3)

    def test_invite_emails_check_tlds(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.send'):
            for tld, code in [(None, 200), ('ru', 200), ('com', 200), ('alert(100)', 422)]:
                data = {
                    'department_id': ROOT_DEPARTMENT_ID,
                    'emails': [
                        'testmail1@my-domain.test.com',
                    ],
                }
                if tld is not None:
                    data['tld'] = tld
                self.post_json(
                    '/invites/emails/',
                    data=data,
                    headers=self.headers,
                    expected_code=code,
                )

    def test_invite_emails_with_custom_mail_template(self):
        # Проверяем, что можно передать параметры wait и mail_campaign_slug и они сохранятся
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.send') as mock_send:
            wait = 30
            mail_campaign_slug = 'AS3DA23SD-ASD34'

            self.post_json(
                '/invites/emails/',
                data={
                    'department_id': ROOT_DEPARTMENT_ID,
                    'emails': [
                        'testmail1@my-domain.test.com',
                        'testmail2@yandex.ru',
                        'почта@домен.рф'
                    ],
                    'wait': wait,
                    'mail_campaign_slug': mail_campaign_slug,
                },
                headers=self.headers,
                expected_code=200,
            )
            invite = InviteModel(self.meta_connection).filter(org_id=self.yandex_organization['id']).one()
            assert_that(
                invite,
                has_entries(
                    counter=3,
                    wait=wait,
                    mail_campaign_slug=mail_campaign_slug,
                    service_slug='autotest',
                )
            )
            assert_called(mock_send, 3)

    def test_limit(self):
        # Проверим, что на количество инвайтов есть лимит в 100 штук
        # и на 101 будет ошибка.

        limit = 100
        for i in range(limit):
            self.post_json(
                '/invites/emails/',
                data={
                    'department_id': ROOT_DEPARTMENT_ID,
                    'emails': [
                        'testmail1@my-domain.test.com',
                        'testmail2@yandex.ru',
                        'почта@домен.рф'
                    ]
                },
                headers=self.headers,
                expected_code=200,
            )

        self.post_json(
            '/invites/',
            data={'department_id': 1},
            headers=self.headers,
            expected_code=403,
            expected_error_code='too_many_invites',
            expected_message='Too many invites',
        )

    def test_limit_in_whitelist_organization(self):
        # На организации в белом списке лимит не распространяется
        vip_features = self.yandex_organization['vip']
        vip_features.append('whitelist')
        OrganizationModel(self.main_connection).update_vip_reasons(self.yandex_organization['id'], vip_features)

        limit = 100
        for i in range(limit):
            self.post_json(
                '/invites/emails/',
                data={
                    'department_id': ROOT_DEPARTMENT_ID,
                    'emails': [
                        'testmail1@my-domain.test.com',
                        'testmail2@yandex.ru',
                        'почта@домен.рф'
                    ]
                },
                headers=self.headers,
                expected_code=200,
            )

        self.post_json(
            '/invites/emails/',
            data={
                'department_id': ROOT_DEPARTMENT_ID,
                'emails': [
                    'testmail1@my-domain.test.com',
                    'testmail2@yandex.ru',
                    'почта@домен.рф'
                ]
            },
            headers=self.headers,
            expected_code=200,
        )
