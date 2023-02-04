# -*- coding: utf-8 -*-
import os
import binascii

from django.test import TestCase
from guardian import shortcuts
from unittest.mock import patch

from events.accounts.factories import (
    UserFactory, OrganizationToGroupFactory, OrganizationFactory,
)
from events.accounts.helpers import YandexClient
from events.surveyme.factories import SurveyFactory
from events.support import utils


def get_random_cloud_uid():
    return binascii.hexlify(os.urandom(10)).decode()


class TestSupportUtils(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def test__return_error(self):
        self.assertEqual(utils._return_error('testit'), {'status': 'error', 'message': 'testit'})
        self.assertEqual(utils._return_error(None), {'status': 'error', 'message': None})

    def test__return_success(self):
        self.assertEqual(utils._return_success(), {'status': 'success'})

    def test__return_inprogress(self):
        self.assertEqual(utils._return_inprogress('abcd'), {'status': 'inprogress', 'task_id': 'abcd'})
        self.assertEqual(utils._return_inprogress(None), {'status': 'inprogress', 'task_id': None})

    def test__is_user_in_org_group_1(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        self.assertFalse(utils._is_user_in_org_group(user, o2g.group))

    def test__is_user_in_org_group_2(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        user.groups.add(o2g.group)
        self.assertTrue(utils._is_user_in_org_group(user, o2g.group))

    def test__find_user_1(self):
        user = UserFactory()
        self.assertEqual(utils._find_user(email=user.email), user)

    def test__find_user_2(self):
        with self.assertRaises(utils.UserNotFound):
            utils._find_user(email='user-at-yandex.ru')

    def test__find_user_3(self):
        user = UserFactory()
        self.assertEqual(utils._find_user(uid=user.uid), user)

    def test__find_user_4(self):
        with self.assertRaises(utils.UserNotFound):
            utils._find_user(uid='not-a-user-uid')

    def test__find_user_5(self):
        user = UserFactory(cloud_uid=get_random_cloud_uid())
        self.assertEqual(utils._find_user(cloud_uid=user.cloud_uid), user)

    def test__find_user_6(self):
        with self.assertRaises(utils.UserNotFound):
            utils._find_user(cloud_uid='not-a-cloud-uid')

    def test__find_user_7(self):
        user = UserFactory(cloud_uid=get_random_cloud_uid())
        self.assertEqual(utils._find_user(uid=user.uid, cloud_uid=user.cloud_uid), user)

    def test__find_user_9(self):
        user = UserFactory(cloud_uid=get_random_cloud_uid())
        another_user = UserFactory()
        with self.assertRaises(utils.UserNotFound):
            utils._find_user(uid=another_user.uid, cloud_uid=user.cloud_uid)

    def test__find_user_10(self):
        with self.assertRaises(utils.UserNotFound):
            utils._find_user()

    def test__change_owner_1(self):
        result = utils._change_owner(99999999)
        self.assertEqual(result['status'], 'error')

    def test__change_owner_2(self):
        survey = SurveyFactory()
        result = utils._change_owner(survey.pk, uid='not-a-user-uid')
        self.assertEqual(result['status'], 'error')

    def test__change_owner_3(self):
        old_user = UserFactory()
        survey = SurveyFactory(user=old_user)
        shortcuts.assign_perm('change_survey', old_user, survey)

        user = UserFactory()
        result = utils._change_owner(survey.pk, uid=user.uid)

        survey.refresh_from_db()
        self.assertEqual(result['status'], 'success')
        self.assertTrue(user.has_perm('change_survey', survey))
        self.assertEqual(survey.user, user)

    def test__change_owner_4(self):
        old_user = UserFactory()
        o2g = OrganizationToGroupFactory()
        old_user.groups.add(o2g.group)
        survey = SurveyFactory(user=old_user, org=o2g.org)
        shortcuts.assign_perm('change_survey', old_user, survey)

        user = UserFactory()
        result = utils._change_owner(survey.pk, uid=user.uid)
        self.assertEqual(result['status'], 'error')

    def test__change_owner_5(self):
        old_user = UserFactory()
        o2g = OrganizationToGroupFactory()
        old_user.groups.add(o2g.group)
        survey = SurveyFactory(user=old_user, org=o2g.org)
        shortcuts.assign_perm('change_survey', old_user, survey)

        user = UserFactory()
        user.groups.add(o2g.group)
        result = utils._change_owner(survey.pk, uid=user.uid)

        survey.refresh_from_db()
        self.assertEqual(result['status'], 'success')
        self.assertTrue(user.has_perm('change_survey', survey))
        self.assertEqual(survey.user, user)

    def test_change_owner_1(self):
        survey = SurveyFactory(user=UserFactory())
        user = UserFactory()
        result = utils.change_owner(survey.pk, email=user.email)
        self.assertEqual(result['status'], 'inprogress')

    def test_change_owner_2(self):
        survey = SurveyFactory(user=UserFactory())
        user = UserFactory()
        result = utils.change_owner(survey.pk, uid=user.uid)
        self.assertEqual(result['status'], 'success')

    def test__find_organization_1(self):
        org = OrganizationFactory()
        self.assertEqual(utils._find_organization(org.dir_id), org)

    def test__find_organization_2(self):
        dir_id = '1234567'
        org = utils._find_organization(dir_id)
        self.assertEqual(org.dir_id, dir_id)

    def test__find_organization_3(self):
        dir_id = '1234567'
        with patch('events.support.utils.create_organization') as mock_create:
            mock_create.return_value = None
            with self.assertRaises(utils.OrganizationNotFound):
                utils._find_organization(dir_id)
        mock_create.assert_called_once_with(dir_id)

    def test_change_organization_1(self):
        org = OrganizationFactory()
        result = utils.change_organization(99999999, org.dir_id)
        self.assertEqual(result['status'], 'error')

    def test_change_organization_2(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        user.groups.add(o2g.group)
        survey = SurveyFactory(user=user, org=o2g.org)

        result = utils.change_organization(survey.pk, None)

        survey.refresh_from_db()
        self.assertIsNone(survey.org)
        self.assertEqual(result['status'], 'success')

    def test_change_organization_3(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        user.groups.add(o2g.group)
        survey = SurveyFactory(user=user, org=o2g.org)

        new_o2g = OrganizationToGroupFactory()
        user.groups.add(new_o2g.group)
        result = utils.change_organization(survey.pk, new_o2g.org.dir_id)

        survey.refresh_from_db()
        self.assertEqual(survey.org, new_o2g.org)
        self.assertEqual(result['status'], 'success')

    def test_change_organization_4(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        user.groups.add(o2g.group)
        survey = SurveyFactory(user=user, org=o2g.org)

        new_o2g = OrganizationToGroupFactory()
        result = utils.change_organization(survey.pk, new_o2g.org.dir_id)

        survey.refresh_from_db()
        self.assertEqual(survey.org, o2g.org)
        self.assertEqual(result['status'], 'error')
