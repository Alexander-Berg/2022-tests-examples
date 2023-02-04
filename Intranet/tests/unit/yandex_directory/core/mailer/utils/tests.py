# -*- coding: utf-8 -*-
import unittest.mock
from unittest.mock import (
    patch,
)
from testutils import (
    TestCase,
    override_settings,
    assert_not_called,
)

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.mailer.utils import (
    # send_welcome_email,
    send_email_to_admins,
    send_invite_email,
)
from intranet.yandex_directory.src.yandex_directory.core.utils import build_email
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id


class TestUtils(TestCase):
    language = 'en'

    # @override_settings(MAIL_SEND_FUNCTION='intranet.yandex_directory.src.yandex_directory.core.mailer.mailer.send')
    # def test_send_welcome_email(self):
    #     # отправляем приветственное письмо
    #     org_id = self.organization['id']
    #     uid = self.user['id']
    #
    #     expected_email = build_email(
    #         self.main_connection,
    #         self.user['nickname'],
    #         org_id
    #     )
    #     passport_lang = 'en'
    #     tld = self.organization['tld']
    #
    #     with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.get_user_data_from_blackbox_by_uid') as mock_user_data, \
    #         patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.lang_for_notification', return_value='en'), \
    #         patch('intranet.yandex_directory.src.yandex_directory.core.mailer.send') as mock_send:
    #
    #         mock_user_data.return_value = {'language': passport_lang, 'login': self.user['nickname']}
    #
    #         send_welcome_email(
    #             meta_connection=self.meta_connection,
    #             main_connection=self.main_connection,
    #             org_id=org_id,
    #             uid=uid,  # только 1 сотруднику
    #         )
    #         mail_args = {
    #             'lang': passport_lang,
    #             'tld': tld,
    #         }
    #         # main_connection, campaign_slug, org_id, from_name, from_email, to_email, subject, mail_args
    #         mock_send.assert_called_once_with(
    #             self.main_connection,
    #             app.config['SENDER_CAMPAIGN_SLUG']['WELCOME_EMAIL'],
    #             org_id,
    #             expected_email,
    #             mail_args
    #         )

    # def test_dont_send_welcome_email_to_robot(self):
    #     # Попробуем добавить робота и убедимся, что
    #     # не попытались отправить ему приветственное письмо.
    #     # Потому что роботам они не нужны.
    #     with patch('intranet.yandex_directory.src.yandex_directory.core.views.users.send_welcome_email') as mocked_send:
    #         create_robot_for_service_and_org_id(
    #             meta_connection=self.meta_connection,
    #             main_connection=self.main_connection,
    #             service_slug=self.service['slug'],
    #             org_id=self.organization['id']
    #         )
    #
    #         assert_not_called(mocked_send)

    def test_send_email_to_admins(self):
        # отправка письма всем админам

        outer_admin_email = 'admin@mail.com'
        campaign_slug = 'campaign_slug'
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.get_user_data_from_blackbox_by_uid') as mock_user_data, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils._send_email') as mock_send_email:
            mock_user_data.return_value = {
                'default_email': outer_admin_email,
            }

            send_email_to_admins(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                campaign_slug,
            )

        # отправили письма внешнему админу и админу организации
        mock_send_email.assert_has_calls(
            [
                unittest.mock.call(
                    self.meta_connection,
                    self.main_connection,
                    self.organization['id'],
                    self.admin_uid,
                    build_email(
                        self.main_connection,
                        self.organization_info['admin_user']['nickname'],
                        self.organization['id'],
                    ),
                    self.organization_domain,
                    campaign_slug,
                ),
                unittest.mock.call(
                    self.meta_connection,
                    self.main_connection,
                    self.organization['id'],
                    self.outer_admin['id'],
                    outer_admin_email,
                    self.organization_domain,
                    campaign_slug,
                ),
            ]
        )

    def test_email_to_admins_without_outer(self):
        # если внешнего админа нет в паспорте, идем дальше

        campaign_slug = 'campaign_slug'
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.get_user_data_from_blackbox_by_uid') as mock_user_data, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils._send_email') as mock_send_email:
            # паспорт ничего не вернул для внешнего админа
            mock_user_data.return_value = None

            send_email_to_admins(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                campaign_slug,
            )

        # отправили админу организации
        mock_send_email.assert_has_calls(
            [
                unittest.mock.call(
                    self.meta_connection,
                    self.main_connection,
                    self.organization['id'],
                    self.admin_uid,
                    build_email(
                        self.main_connection,
                        self.organization_info['admin_user']['nickname'],
                        self.organization['id'],
                    ),
                    self.organization_domain,
                    campaign_slug,
                ),
            ]
        )

    @override_settings(MAIL_SEND_FUNCTION='intranet.yandex_directory.src.yandex_directory.core.mailer.mailer.send')
    def test_send_invite_email(self):
        # отправляем письмо с приглашением присоединиться к организации
        org_id = 123
        org_name = 'test_org_name'
        lang = 'ru'
        invite_link = 'www.test.ru/invite?code=test123'
        to_email = 'test@yandex.ru'
        tld = 'ru'

        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.send') as mock_send:
            send_invite_email(
                main_connection=self.main_connection,
                to_email=to_email,
                invite_link=invite_link,
                org_id=org_id,
                org_name=org_name,
                lang=lang,
                tld=tld,
            )
            mail_args = {
                'lang': lang,
                'org_name': org_name,
                'invite_link': invite_link,
                'tld': tld,
            }
            mock_send.assert_called_once_with(
                self.main_connection,
                app.config['SENDER_CAMPAIGN_SLUG']['INVITE_USER'],
                org_id,
                to_email,
                mail_args,
            )
