# -*- coding: utf-8 -*-
from django.test import TestCase

from events.accounts.factories import UserFactory
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables.disk import (
    DiskCarmaVariable,
    DiskIsPaidVariable,
    DiskIsPaidPsBillingVariable,
)
from events.common_app.disk_admin.mock_client import (
    VALID_DATA,
    INVALID_DATA,
    DISK_ADMIN_API_DATA,
)


class TestDiskVariables(TestCase):
    def setUp(self):
        self.valid_uid = '1'
        self.invalid_uid = '2'
        DISK_ADMIN_API_DATA.valid_uids = [self.valid_uid]
        DISK_ADMIN_API_DATA.invalid_uids = [self.invalid_uid]
        self.answer = ProfileSurveyAnswerFactory()

    def test_request_carma_for_anonymous_user_must_return_none(self):
        variable = DiskCarmaVariable(answer=self.answer)
        self.assertTrue(self.answer.user.is_anonymous)
        self.assertEqual(variable.get_value(), None)

    def test_request_paid_status_for_anonymous_user_must_return_none(self):
        variable = DiskIsPaidVariable(answer=self.answer)
        self.assertTrue(self.answer.user.is_anonymous)
        self.assertEqual(variable.get_value(), None)

    def test_auth_user_must_return_users_carma(self):
        variable = DiskCarmaVariable(answer=self.answer)
        user = UserFactory()
        self.answer.user = user
        self.answer.user.uid = self.valid_uid
        self.answer.user.save()

        msg = 'Если API админки диска вернуло данные кармы - нужно вернуть правильную цифру'
        self.assertEqual(variable.get_value(), VALID_DATA['carma']['user_carma'], msg=msg)

    def test_auth_user_must_return_paid_status(self):
        variable = DiskIsPaidVariable(answer=self.answer)
        user = UserFactory()
        self.answer.user = user
        self.answer.user.uid = self.valid_uid
        self.answer.user.save()

        msg = 'Если API админки диска вернуло данные кармы - нужно вернуть правильный статус оплаты'
        self.assertEqual(variable.get_value(), VALID_DATA['user']['is_paid'], msg=msg)

    def test_auth_user_must_return_paid_ps_billing_status(self):
        variable = DiskIsPaidPsBillingVariable(answer=self.answer)
        user = UserFactory()
        self.answer.user = user
        self.answer.user.uid = self.valid_uid
        self.answer.user.save()

        msg = 'Если API админки диска вернуло данные кармы - нужно вернуть правильный статус оплаты'
        self.assertEqual(variable.get_value(), VALID_DATA['user']['is_paid_ps_billing'], msg=msg)

    def test_with_invalid_user_must_return_error(self):
        variable = DiskCarmaVariable(answer=self.answer)
        user = UserFactory()
        self.answer.user = user
        user.uid = self.invalid_uid
        user.save()

        msg = 'Если API админки диска вернуло ошибку - нужно также вернуть её'
        self.assertEqual(variable.get_value(), '{%s}' % INVALID_DATA['error'], msg=msg)
