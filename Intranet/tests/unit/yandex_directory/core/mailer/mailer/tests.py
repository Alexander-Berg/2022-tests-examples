# -*- coding: utf-8 -*-
from unittest.mock import patch, ANY
from hamcrest import (
    assert_that,
    equal_to,
)

from testutils import TestCase, assert_not_called

from intranet.yandex_directory.src.yandex_directory.core.mailer import mailer
from intranet.yandex_directory.src.yandex_directory.core.mailer.tasks import SendEmailTask, SendEmailToAllTask
from intranet.yandex_directory.src.yandex_directory.core.mailer.utils import send_email_to_all
from intranet.yandex_directory.src.yandex_directory.core.models import DomainModel
from intranet.yandex_directory.src.yandex_directory.common.exceptions import (
    UserNotFoundError,
)


class TestMailer(TestCase):

    def setUp(self):
        super(TestMailer, self).setUp()
        self.from_email = 'noreply@' + self.label + self.domain_part
        self.to = 'mail@mail.com'
        # ящик получатель не на домене организации
        self.params = {
            'campaign_slug': 'campaign_slug',
            'org_id': self.organization['id'],
            'to_email': self.to,
            'mail_args': {},
        }

    def test_send(self):
        # проверка что отправляем почту
        with patch.object(SendEmailTask, 'place_into_the_queue') as place_into_the_queue:
            mailer.send(self.main_connection, **self.params)

            expected = self.params.copy()
            place_into_the_queue.assert_called_once_with(ANY, None, **expected)

    def test_send_all(self):
        from intranet.yandex_directory.src.yandex_directory.core.mailer.utils import send_email_to_all_async

        params = {
            'org_id': self.organization['id'],
            'domain': DomainModel(self.main_connection).get_master(self.organization['id'])['name'],
            'campaign_slug': 'campaign_slug'
        }

        with patch.object(SendEmailToAllTask, 'place_into_the_queue') as place_into_the_queue:
            send_email_to_all_async(self.main_connection, **params)
            place_into_the_queue.assert_called_once_with(ANY, None, **params)


    def test__is_email_from_organization_domain__false(self):
        # ящик не находится на домене организации
        assert_that(
            mailer._is_email_from_organization_domain(self.main_connection, self.organization['id'], 'email@foreign.com'),
            equal_to(False)
        )

    def test__is_email_from_organization_domain__true(self):
        # ящик находится на домене организации
        domain = 'aliasdomain.com'
        DomainModel(self.main_connection).create(domain, self.organization['id'])

        email = 'email@' + domain
        assert_that(
            mailer._is_email_from_organization_domain(self.main_connection, self.organization['id'], email),
            equal_to(True)
        )

    def test_dummy_mailer(self):
        # проверерка что фактически отправки почты не происходит
        # проверка что отправляем почту
        with patch.object(SendEmailTask, 'place_into_the_queue') as place_into_the_queue:
            mailer.dummy_send(self.main_connection, **self.params)
            assert_not_called(place_into_the_queue)

    def test_same_domain_send(self):
        # можно отправлять письма только на ящики с домена организации
        with patch.object(SendEmailTask, 'place_into_the_queue') as place_into_the_queue,\
             patch('intranet.yandex_directory.src.yandex_directory.core.mailer.mailer._is_email_from_organization_domain', return_value=False):
            mailer.same_domain_send(self.main_connection, **self.params)
            assert_not_called(place_into_the_queue)

    def test_send_all_user_not_found(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils._send_email') as mock_send:
            mock_send.side_effect = UserNotFoundError
            send_email_to_all(
                self.meta_connection,
                self.main_connection,
                org_id=self.user['org_id'],
                domain=None,
                campaign_slug=None,
            )

    def test_log_password(self):
        # проверим, что отправка письма с паролем не падает с LoggingSecretError
        self.params['mail_args'] = {
            'password': 'password'
        }
        mailer.send(self.main_connection, **self.params)
