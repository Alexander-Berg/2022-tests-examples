# -*- coding: utf-8 -*-
import datetime
import unittest.mock
from unittest.mock import (
    Mock,
    patch,
)

from testutils import (
    TestCase,
    override_settings,
    create_organization,
)

from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ServiceModel,
    OrganizationServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service
from intranet.yandex_directory.src.yandex_directory.core.commands.notify_about_trial_expiration import Command as NotifyAboutTrialExpirationCommand


class TestNotifyAboutTrialExpirationCommand(TestCase):
    def setUp(self, *args, **kwargs):
        super(TestNotifyAboutTrialExpirationCommand, self).setUp(*args, **kwargs)
        self.command = NotifyAboutTrialExpirationCommand()
        self.command.main_connection = self.main_connection
        self.command.meta_connection = self.meta_connection

    def test_should_run_command_for_one_org_id(self):
        # проверяем что команда запустится если указать определнный org_id
        mocked_send_mail_about_trial_expiration_if_needed = Mock()

        service_with_trial = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service_with_trial['slug'],
        )
        org_service_with_trial = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': service_with_trial['id'],
            },
            one=True,
        )

        email_id = 'some-id'
        mail_config = {
            self.service['slug']: {
                30: 'another-email-id',
            },
            service_with_trial['slug']: {
                (org_service_with_trial['trial_expires'] - utcnow().date()).days: email_id,
            }
        }

        with override_settings(MAIL_IDS_BEFORE_TRIAL_END=mail_config):
            with patch(
                'intranet.yandex_directory.src.yandex_directory.core.commands.notify_about_trial_expiration.OrganizationServiceModel.send_mail_about_trial_expiration_if_needed',
                mocked_send_mail_about_trial_expiration_if_needed,
            ):
                self.command.try_run(org_id=self.organization['id'])

        mocked_send_mail_about_trial_expiration_if_needed.assert_called_once_with(
            org_id=self.organization['id'],
        )

    def test_should_run_command_for_all_org_id_with_trial_expiration(self):
        # проверяем что без указания org_id команда запускается для всех нужных организаций
        mocked_send_mail_about_trial_expiration_if_needed = Mock()

        new_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='some-org'
        )['organization']

        service_with_trial = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='new_service2',
            name='Service2',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        for org_id in [self.organization['id'], new_organization['id']]:
            enable_service(
                self.meta_connection,
                self.main_connection,
                org_id,
                service_with_trial['slug'],
            )
        org_service_with_trial = OrganizationServiceModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
                'service_id': service_with_trial['id'],
            },
            one=True,
        )

        email_id = 'some-id'
        mail_config = {
            self.service['slug']: {
                30: 'another-email-id',
            },
            service_with_trial['slug']: {
                (org_service_with_trial['trial_expires'] - utcnow().date()).days: email_id,
            }
        }

        with override_settings(MAIL_IDS_BEFORE_TRIAL_END=mail_config):
            with patch(
                'intranet.yandex_directory.src.yandex_directory.core.commands.notify_about_trial_expiration.OrganizationServiceModel.send_mail_about_trial_expiration_if_needed',
                mocked_send_mail_about_trial_expiration_if_needed,
            ):
                self.command.try_run(org_id=None)

        mocked_send_mail_about_trial_expiration_if_needed.assert_has_calls(
            [
                unittest.mock.call(org_id=self.organization['id']),
                unittest.mock.call(org_id=new_organization['id']),
            ],
            any_order=True,
        )
