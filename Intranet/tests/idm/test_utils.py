# -*- coding: utf-8 -*-
import responses

from django.conf import settings
from django.contrib.contenttypes.models import ContentType
from django.db.models import Q
from django.test import TestCase, override_settings
from guardian.shortcuts import get_perms, assign_perm
from requests.exceptions import HTTPError
from unittest.mock import patch

from events.accounts.factories import UserFactory, GroupFactory
from events.accounts.models import User, GrantTransitionInfo
from events.common_app.helpers import override_cache_settings
from events.common_app.models import PermissionLog
from events.idm import utils
from events.surveyme.factories import SurveyFactory, SurveyGroupFactory


class TestIdmUtils_user(TestCase):
    def test_get_user_args(self):
        with self.assertRaises(AssertionError):
            utils.get_user_args(None, None)

        with patch('events.idm.utils.find_users_by_login') as mock_find_by_login:
            mock_find_by_login.return_value = []
            with self.assertRaises(ValueError):
                utils.get_user_args(None, 'test-new-user')
            mock_find_by_login.assert_called_once_with('test-new-user')

        with patch('events.idm.utils.find_users_by_login') as mock_find_by_login:
            mock_find_by_login.return_value = [{'uid': '123454321', 'login': 'test-new-user'}]
            uid, login = utils.get_user_args(None, 'test-new-user')
            self.assertEqual((uid, login), ('123454321', 'test-new-user'))
            mock_find_by_login.assert_called_once_with('test-new-user')

        with patch('events.idm.utils.find_users_by_uid') as mock_find_by_uid:
            mock_find_by_uid.return_value = []
            with self.assertRaises(ValueError):
                utils.get_user_args('123454321', None)
            mock_find_by_uid.assert_called_once_with('123454321')

        with patch('events.idm.utils.find_users_by_uid') as mock_find_by_uid:
            mock_find_by_uid.return_value = [{'uid': '123454321', 'login': 'test-new-user'}]
            uid, login = utils.get_user_args('123454321', None)
            self.assertEqual((uid, login), ('123454321', 'test-new-user'))
            mock_find_by_uid.assert_called_once_with('123454321')

        with (
            patch('events.idm.utils.find_users_by_login') as mock_find_by_login,
            patch('events.idm.utils.find_users_by_uid') as mock_find_by_uid,
        ):
            uid, login = utils.get_user_args('123454321', 'test-new-user')
            self.assertEqual((uid, login), ('123454321', 'test-new-user'))
            mock_find_by_login.assert_not_called()
            mock_find_by_uid.assert_not_called()

    def test_should_find_user(self):
        user = UserFactory()

        new_user = utils.find_user(user.uid)
        self.assertEqual(new_user.pk, user.pk)
        self.assertEqual(new_user.first_name, user.first_name)

        new_user = utils.find_user(None, login=user.username)
        self.assertEqual(new_user.username, user.username)
        self.assertEqual(new_user.first_name, user.first_name)

        new_user = utils.find_user(user.uid, first_name='newuser')
        self.assertEqual(new_user.first_name, 'newuser')

    def test_shouldnt_find_user(self):
        self.assertIsNone(utils.find_user('not-a-user-uid'))
        self.assertIsNone(utils.find_user(None, login='not-a-user-uid'))

    def test_should_create_user_without_login(self):
        with patch('events.idm.utils.get_user_args') as mock_user_args:
            mock_user_args.return_value = ('123454321', 'test-new-user')
            user = utils.create_user('123454321')
        self.assertIsNotNone(user)
        self.assertEqual(user.uid, '123454321')
        self.assertEqual(user.username, 'test-new-user')
        self.assertEqual(user.email, 'test-new-user@yandex-team.ru')
        mock_user_args.assert_called_once_with('123454321', None)

    def test_should_create_user_with_login(self):
        user = utils.create_user('123454321', login='test-new-user')
        self.assertIsNotNone(user)
        self.assertEqual(user.uid, '123454321')
        self.assertEqual(user.username, 'test-new-user')
        self.assertEqual(user.email, 'test-new-user@yandex-team.ru')

    def test_shouldnt_create_user_if_exists(self):
        existing_user = UserFactory()
        user = utils.create_user(existing_user.uid, login=existing_user.username)
        self.assertIsNotNone(user)
        self.assertEqual(user, existing_user)

    def test_should_return_user_if_exists(self):
        user = UserFactory()
        self.assertEqual(utils.get_user(user.uid), user)

        self.assertIsNone(utils.get_user('123454321', create=False))

        user = utils.get_user('123454321', login='test-new-user')
        self.assertIsNotNone(user)
        self.assertEqual(user.uid, '123454321')
        self.assertEqual(user.username, 'test-new-user')
        self.assertEqual(user.email, 'test-new-user@yandex-team.ru')


class TestIdmUtils_group(TestCase):
    def test_should_return_group_id(self):
        self.assertEqual(utils.get_group_id('group:12345'), 12345)

    def test_shouldnt_return_group_id(self):
        self.assertIsNone(utils.get_group_id('name:12345'))
        self.assertIsNone(utils.get_group_id(':12345'))
        self.assertIsNone(utils.get_group_id('12345'))
        self.assertIsNone(utils.get_group_id('group:allstaff'))
        self.assertIsNone(utils.get_group_id('name:allstaff'))
        self.assertIsNone(utils.get_group_id(':allstaff'))
        self.assertIsNone(utils.get_group_id('allstaff'))

    def test_should_find_group(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertEqual(utils.find_group(group_id), group)

    def test_shouldnt_find_group(self):
        self.assertIsNone(utils.find_group(12345))

    def test_should_create_group(self):
        group = utils.create_group(12321)
        self.assertIsNotNone(group)
        self.assertEqual(group.name, 'group:12321')

    def test_shouldnt_create_group_if_exists(self):
        existing_group = GroupFactory()
        group_id = utils.get_group_id(existing_group.name)
        group = utils.create_group(group_id)
        self.assertIsNotNone(group)
        self.assertEqual(group, existing_group)

    def test_should_return_group_if_exists(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertEqual(utils.get_group(group_id), group)

        self.assertIsNone(utils.get_group(12321, create=False))

        group = utils.get_group(12321)
        self.assertIsNotNone(group)
        self.assertEqual(group.name, 'group:12321')


class TestIdmUtils_chief(TestCase):
    @responses.activate
    def test_should_return_chief_from_staff(self):
        user = UserFactory()
        chief = UserFactory()
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [{
                    'uid': user.uid,
                    'chief': {
                        'uid': chief.uid,
                        'login': chief.username,
                    },
                }],
            },
        )
        person = utils.get_chief_from_staff(user.uid)
        self.assertIsNotNone(person)
        self.assertDictEqual(person, {
            'uid': chief.uid,
            'login': chief.username,
        })
        self.assertEqual(len(responses.calls), 1)
        self.assertTrue(f'uid={user.uid}' in responses.calls[0].request.url)

    @responses.activate
    def test_shouldnt_return_chief_from_staff(self):
        user = UserFactory()
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [],
            },
        )
        person = utils.get_chief_from_staff(user.uid)
        self.assertIsNone(person)
        self.assertEqual(len(responses.calls), 1)
        self.assertTrue(f'uid={user.uid}' in responses.calls[0].request.url)

    @override_cache_settings()
    def test_should_return_user_if_exists(self):
        user = UserFactory()
        chief = UserFactory()
        with patch('events.idm.utils.get_chief_from_staff') as mock_chief:
            mock_chief.return_value = {
                'uid': chief.uid,
                'login': chief.username,
            }
            person = utils.get_chief(user.uid)
        self.assertIsNotNone(person)
        self.assertEqual(person, chief)
        mock_chief.assert_called_once_with(user.uid)

    @override_cache_settings()
    def test_should_create_new_user_if_not_exists(self):
        user = UserFactory()
        with patch('events.idm.utils.get_chief_from_staff') as mock_chief:
            mock_chief.return_value = {
                'uid': '123454321',
                'login': 'test-new-user',
            }
            person = utils.get_chief(user.uid)
        self.assertIsNotNone(person)
        self.assertEqual(person.uid, '123454321')
        self.assertEqual(person.username, 'test-new-user')
        self.assertEqual(person.email, 'test-new-user@yandex-team.ru')
        mock_chief.assert_called_once_with(user.uid)

    @override_cache_settings()
    def test_shouldnt_create_user_if_not_exists(self):
        user = UserFactory()
        with patch('events.idm.utils.get_chief_from_staff') as mock_chief:
            mock_chief.return_value = None
            person = utils.get_chief(user.uid)
        self.assertIsNone(person)
        mock_chief.assert_called_once_with(user.uid)

    @override_cache_settings()
    def test_shouldnt_create_user_volozh_patch(self):
        user = UserFactory()
        with patch('events.idm.utils.get_chief_from_staff') as mock_chief:
            mock_chief.return_value = {
                'uid': '123454321',
                'login': 'volozh',
            }
            person = utils.get_chief(user.uid)
        self.assertIsNone(person)
        mock_chief.assert_called_once_with(user.uid)


class TestIdmUtils_survey(TestCase):
    def test_should_find_survey(self):
        survey = SurveyFactory()
        self.assertEqual(utils.get_survey(survey.pk), survey)

    def test_shouldnt_find_survey(self):
        self.assertIsNone(utils.get_survey(1234321))


class TestIdmUtils_survey_group(TestCase):
    def test_should_find_survey_group(self):
        survey_group = SurveyGroupFactory()
        self.assertEqual(utils.get_survey_group(survey_group.pk), survey_group)

    def test_shouldnt_find_survey_group(self):
        self.assertIsNone(utils.get_survey_group(12321))


class TestIdmUtils_add_user_role(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_should_make_superuser(self):
        user = UserFactory(is_superuser=False)
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_SUPERUSER, user.uid))
        user.refresh_from_db()
        self.assertTrue(user.is_superuser)

    def test_should_create_user_and_make_superuser(self):
        uid = '123454321'
        login = 'test-new-user'
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_SUPERUSER, uid, login=login))
        user = utils.find_user(uid)
        self.assertIsNotNone(user)
        self.assertTrue(user.is_superuser)

    def test_should_make_support(self):
        user = UserFactory(is_staff=False)
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_SUPPORT, user.uid))
        user.refresh_from_db()
        self.assertTrue(user.is_staff)

    def test_should_create_user_and_make_support(self):
        uid = '123454321'
        login = 'test-new-user'
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_SUPPORT, uid, login=login))
        user = utils.find_user(uid)
        self.assertIsNotNone(user)
        self.assertTrue(user.is_staff)

    def test_should_make_form_manager(self):
        user = UserFactory()
        survey = SurveyFactory()
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_FORM_MANAGER, user.uid, object_pk=survey.pk))
        self.assertTrue(settings.ROLE_FORM_MANAGER in get_perms(user, survey))

    def test_should_create_user_and_make_form_manager(self):
        uid = '123454321'
        login = 'test-new-user'
        survey = SurveyFactory()
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_FORM_MANAGER, uid, login=login, object_pk=survey.pk))
        user = utils.find_user(uid)
        self.assertIsNotNone(user)
        self.assertTrue(settings.ROLE_FORM_MANAGER in get_perms(user, survey))

    def test_shouldn_make_form_manager(self):
        user = UserFactory()
        self.assertFalse(utils.add_user_role(self.request_user, settings.ROLE_FORM_MANAGER, user.uid, object_pk=1234321))

    def test_should_make_group_manager(self):
        user = UserFactory()
        survey_group = SurveyGroupFactory()
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_GROUP_MANAGER, user.uid, object_pk=survey_group.pk))
        self.assertTrue(settings.ROLE_GROUP_MANAGER in get_perms(user, survey_group))

    def test_should_create_user_and_make_group_manager(self):
        uid = '123454321'
        login = 'test-new-user'
        survey_group = SurveyGroupFactory()
        self.assertTrue(utils.add_user_role(self.request_user, settings.ROLE_GROUP_MANAGER, uid, login=login, object_pk=survey_group.pk))
        user = utils.find_user(uid)
        self.assertIsNotNone(user)
        self.assertTrue(settings.ROLE_GROUP_MANAGER in get_perms(user, survey_group))

    def test_shouldn_make_group_manager(self):
        user = UserFactory()
        self.assertFalse(utils.add_user_role(self.request_user, settings.ROLE_GROUP_MANAGER, user.uid, object_pk=12321))


class TestIdmUtils_add_group_role(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_shouldnt_make_superuser(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.add_group_role(self.request_user, settings.ROLE_SUPERUSER, group_id))

    def test_shouldnt_make_support(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.add_group_role(self.request_user, settings.ROLE_SUPPORT, group_id))

    def test_should_make_form_manager(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        survey = SurveyFactory()
        self.assertTrue(utils.add_group_role(self.request_user, settings.ROLE_FORM_MANAGER, group_id, object_pk=survey.pk))
        self.assertTrue(settings.ROLE_FORM_MANAGER in get_perms(group, survey))

    def test_should_create_user_and_make_form_manager(self):
        group_id = 12321
        survey = SurveyFactory()
        self.assertTrue(utils.add_group_role(self.request_user, settings.ROLE_FORM_MANAGER, group_id, object_pk=survey.pk))
        group = utils.find_group(group_id)
        self.assertIsNotNone(group)
        self.assertTrue(settings.ROLE_FORM_MANAGER in get_perms(group, survey))

    def test_shouldn_make_form_manager(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.add_group_role(self.request_user, settings.ROLE_FORM_MANAGER, group_id, object_pk=1234321))

    def test_should_make_group_manager(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        survey_group = SurveyGroupFactory()
        self.assertTrue(utils.add_group_role(self.request_user, settings.ROLE_GROUP_MANAGER, group_id, object_pk=survey_group.pk))
        self.assertTrue(settings.ROLE_GROUP_MANAGER in get_perms(group, survey_group))

    def test_should_create_user_and_make_group_manager(self):
        group_id = 12321
        survey_group = SurveyGroupFactory()
        self.assertTrue(utils.add_group_role(self.request_user, settings.ROLE_GROUP_MANAGER, group_id, object_pk=survey_group.pk))
        group = utils.find_group(group_id)
        self.assertIsNotNone(group)
        self.assertTrue(settings.ROLE_GROUP_MANAGER in get_perms(group, survey_group))

    def test_shouldn_make_group_manager(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.add_group_role(self.request_user, settings.ROLE_GROUP_MANAGER, group_id, object_pk=12321))


class TestIdmUtils_remove_user_role(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_should_revoke_superuser(self):
        user = UserFactory(is_superuser=True)
        self.assertTrue(utils.remove_user_role(self.request_user, settings.ROLE_SUPERUSER, user.uid))
        user.refresh_from_db()
        self.assertFalse(user.is_superuser)

    def test_shouldnt_create_user_and_revoke_superuser(self):
        uid = '123454321'
        login = 'test-new-user'
        self.assertFalse(utils.remove_user_role(self.request_user, settings.ROLE_SUPERUSER, uid, login=login))
        user = utils.find_user(uid)
        self.assertIsNone(user)

    def test_should_revoke_support(self):
        user = UserFactory(is_staff=True)
        self.assertTrue(utils.remove_user_role(self.request_user, settings.ROLE_SUPPORT, user.uid))
        user.refresh_from_db()
        self.assertFalse(user.is_staff)

    def test_should_create_user_and_revoke_support(self):
        uid = '123454321'
        login = 'test-new-user'
        self.assertFalse(utils.remove_user_role(self.request_user, settings.ROLE_SUPPORT, uid, login=login))
        user = utils.find_user(uid)
        self.assertIsNone(user)

    def test_should_revoke_form_manager(self):
        user = UserFactory()
        survey = SurveyFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, user, survey)
        self.assertTrue(utils.remove_user_role(self.request_user, settings.ROLE_FORM_MANAGER, user.uid, object_pk=survey.pk))
        self.assertFalse(settings.ROLE_FORM_MANAGER in get_perms(user, survey))

    def test_shouldnt_create_user_and_revoke_form_manager(self):
        uid = '123454321'
        login = 'test-new-user'
        survey = SurveyFactory()
        self.assertFalse(utils.remove_user_role(self.request_user, settings.ROLE_FORM_MANAGER, uid, login=login, object_pk=survey.pk))
        user = utils.find_user(uid)
        self.assertIsNone(user)

    def test_should_execute_role_transition_actions(self):
        old_user = UserFactory()
        new_user = UserFactory()
        survey = SurveyFactory(user=old_user)
        with patch.object(utils.IdmUtils, 'request_roles') as mock_request_roles:
            utils.request_transition_role(
                self.request_user, settings.ROLE_FORM_MANAGER,
                new_user, old_user, survey,
            )
        new_user.has_perm(settings.ROLE_FORM_MANAGER, survey)
        mock_request_roles.assert_called_once_with(
            settings.ROLE_FORM_MANAGER,
            users=[new_user.username], object_pk=str(survey.pk),
        )
        grants_qs = (
            GrantTransitionInfo.objects.filter(
                new_user=new_user, old_user=old_user,
                content_type=ContentType.objects.get_for_model(survey),
                object_pk=str(survey.pk),
            )
        )
        self.assertEqual(grants_qs.count(), 1)

    def test_shouldnt_execute_role_transition_actions_if_user_has_role(self):
        old_user = UserFactory()
        new_user = UserFactory()
        survey = SurveyFactory(user=old_user)
        assign_perm(settings.ROLE_FORM_MANAGER, new_user, survey)
        with patch.object(utils.IdmUtils, 'request_roles') as mock_request_roles:
            utils.request_transition_role(
                self.request_user, settings.ROLE_FORM_MANAGER,
                new_user, old_user, survey,
            )
        mock_request_roles.assert_not_called()
        grants_qs = (
            GrantTransitionInfo.objects.filter(
                new_user=new_user, old_user=old_user,
                content_type=ContentType.objects.get_for_model(survey),
                object_pk=str(survey.pk),
            )
        )
        self.assertEqual(grants_qs.count(), 0)

    def test_should_revoke_form_manager_for_fired_user(self):
        user = UserFactory()
        chief = UserFactory()
        survey = SurveyFactory(user=user)
        assign_perm(settings.ROLE_FORM_MANAGER, user, survey)
        with (
            patch('events.idm.utils.get_chief') as mock_chief,
            patch('events.idm.utils.request_transition_role') as mock_request_role,
        ):
            mock_chief.return_value = chief
            result = utils.remove_user_role(
                self.request_user, settings.ROLE_FORM_MANAGER,
                user.uid, login=user.username,
                object_pk=survey.pk, fired=True,
            )
            self.assertTrue(result)
        self.assertFalse(settings.ROLE_FORM_MANAGER in get_perms(user, survey))
        mock_chief.assert_called_once_with(user.uid)
        mock_request_role.assert_called_once_with(self.request_user, settings.ROLE_FORM_MANAGER, chief, user, survey)

    def test_shouldnt_do_anything_if_form_doesnt_exist(self):
        user = UserFactory()
        self.assertFalse(utils.remove_user_role(self.request_user, settings.ROLE_FORM_MANAGER, user.uid, object_pk=123454321))

    def test_should_revoke_group_manager(self):
        user = UserFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(settings.ROLE_GROUP_MANAGER, user, survey_group)
        self.assertTrue(utils.remove_user_role(self.request_user, settings.ROLE_GROUP_MANAGER, user.uid, object_pk=survey_group.pk))
        self.assertFalse(settings.ROLE_GROUP_MANAGER in get_perms(user, survey_group))

    def test_shouldnt_create_user_and_revoke_group_manager(self):
        uid = '123454321'
        login = 'test-new-user'
        survey_group = SurveyGroupFactory()
        self.assertFalse(utils.remove_user_role(self.request_user, settings.ROLE_GROUP_MANAGER, uid, login=login, object_pk=survey_group.pk))
        user = utils.find_user(uid)
        self.assertIsNone(user)

    def test_should_revoke_group_manager_for_fired_user(self):
        user = UserFactory()
        chief = UserFactory()
        survey_group = SurveyGroupFactory(user=user)
        assign_perm(settings.ROLE_GROUP_MANAGER, user, survey_group)
        with (
            patch('events.idm.utils.get_chief') as mock_chief,
            patch('events.idm.utils.request_transition_role') as mock_request_role,
        ):
            mock_chief.return_value = chief
            result = utils.remove_user_role(
                self.request_user, settings.ROLE_GROUP_MANAGER,
                user.uid, login=user.username,
                object_pk=survey_group.pk, fired=True,
            )
            self.assertTrue(result)
        self.assertFalse(settings.ROLE_GROUP_MANAGER in get_perms(user, survey_group))
        mock_chief.assert_called_once_with(user.uid)
        mock_request_role.assert_called_once_with(self.request_user, settings.ROLE_GROUP_MANAGER, chief, user, survey_group)

    def test_shouldnt_do_anything_if_group_doesnt_exist(self):
        user = UserFactory()
        self.assertFalse(utils.remove_user_role(self.request_user, settings.ROLE_GROUP_MANAGER, user.uid, object_pk=12321))


class TestIdmUtils_remove_group_role(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_shouldnt_revoke_superuser(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_SUPERUSER, group_id))

    def test_shouldnt_create_group_and_revoke_superuser(self):
        group_id = 12321
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_SUPERUSER, group_id))
        group = utils.find_group(group_id)
        self.assertIsNone(group)

    def test_shouldnt_revoke_support(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_SUPPORT, group_id))

    def test_shouldnt_create_user_and_revoke_support(self):
        group_id = 12321
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_SUPPORT, group_id))
        group = utils.find_group(group_id)
        self.assertIsNone(group)

    def test_should_revoke_form_manager(self):
        group = GroupFactory()
        survey = SurveyFactory()
        assign_perm(settings.ROLE_FORM_MANAGER, group, survey)
        group_id = utils.get_group_id(group.name)
        self.assertTrue(utils.remove_group_role(self.request_user, settings.ROLE_FORM_MANAGER, group_id, object_pk=survey.pk))
        self.assertFalse(settings.ROLE_FORM_MANAGER in get_perms(group, survey))

    def test_shouldnt_create_group_and_revoke_form_manager(self):
        group_id = 12321
        survey = SurveyFactory()
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_FORM_MANAGER, group_id, object_pk=survey.pk))
        group = utils.find_group(group_id)
        self.assertIsNone(group)

    def test_shouldnt_do_anything_if_form_doesnt_exist(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_FORM_MANAGER, group_id, object_pk=123454321))

    def test_should_revoke_group_manager(self):
        group = GroupFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(settings.ROLE_GROUP_MANAGER, group, survey_group)
        group_id = utils.get_group_id(group.name)
        self.assertTrue(utils.remove_group_role(self.request_user, settings.ROLE_GROUP_MANAGER, group_id, object_pk=survey_group.pk))
        self.assertFalse(settings.ROLE_GROUP_MANAGER in get_perms(group, survey_group))

    def test_shouldnt_create_group_and_revoke_group_manager(self):
        group_id = 12321
        survey_group = SurveyGroupFactory()
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_GROUP_MANAGER, group_id, object_pk=survey_group.pk))
        group = utils.find_group(group_id)
        self.assertIsNone(group)

    def test_shouldnt_do_anything_if_group_doesnt_exist(self):
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)
        self.assertFalse(utils.remove_group_role(self.request_user, settings.ROLE_GROUP_MANAGER, group_id, object_pk=12321))


class TestIdmUtils_add_role(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_should_invoke_add_user_role(self):
        with patch('events.idm.utils.add_user_role') as mock_add_user_role:
            with patch('events.idm.utils.add_group_role') as mock_add_group_role:
                mock_add_user_role.return_value = True
                self.assertTrue(utils.add_role(
                    self.request_user, settings.ROLE_SUPERUSER, uid='123454321',
                    group=None, login='test-new-user', object_pk=None, passport_login=None,
                ))
        mock_add_user_role.assert_called_once_with(
            self.request_user, settings.ROLE_SUPERUSER, '123454321',
            login='test-new-user', object_pk=None, first_name=None,
        )
        mock_add_group_role.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_invoke_add_external_user_role(self):
        with (
            patch('events.idm.utils.get_external_user_uid_by_login') as mock_get_user_uid,
            patch('events.idm.utils.add_user_role') as mock_add_user_role,
            patch('events.idm.utils.add_group_role') as mock_add_group_role,
        ):
            mock_get_user_uid.return_value = '321123321'
            mock_add_user_role.return_value = True
            self.assertTrue(utils.add_role(
                self.request_user, settings.ROLE_SUPERUSER, uid='123454321',
                group=None, login='test-new-user', object_pk=None, passport_login='yndx-test-new-user',
            ))
        mock_get_user_uid.assert_called_once_with('yndx-test-new-user')
        mock_add_user_role.assert_called_once_with(
            self.request_user, settings.ROLE_SUPERUSER, '321123321',
            login='yndx-test-new-user', object_pk=None, first_name='test-new-user',
        )
        mock_add_group_role.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_invoke_add_external_user_role(self):
        with (
            patch('events.idm.utils.get_external_user_uid_by_login') as mock_get_user_uid,
            patch('events.idm.utils.add_user_role') as mock_add_user_role,
            patch('events.idm.utils.add_group_role') as mock_add_group_role,
        ):
            mock_get_user_uid.return_value = None
            self.assertFalse(utils.add_role(
                self.request_user, settings.ROLE_SUPERUSER, uid='123454321',
                group=None, login='test-new-user', object_pk=None, passport_login='yndx-test-new-user',
            ))
        mock_get_user_uid.assert_called_once_with('yndx-test-new-user')
        mock_add_user_role.assert_not_called()
        mock_add_group_role.assert_not_called()

    def test_should_invoke_add_group_role(self):
        with patch('events.idm.utils.add_user_role') as mock_add_user_role:
            with patch('events.idm.utils.add_group_role') as mock_add_group_role:
                mock_add_group_role.return_value = True
                self.assertTrue(utils.add_role(
                    self.request_user, settings.ROLE_SUPERUSER, uid=None,
                    group=12321, login=None, object_pk=None, passport_login=None,
                ))
        mock_add_user_role.assert_not_called()
        mock_add_group_role.assert_called_once_with(
            self.request_user, settings.ROLE_SUPERUSER, 12321,
            object_pk=None,
        )

    def test_shouldnt_invoke_any_method(self):
        with patch('events.idm.utils.add_user_role') as mock_add_user_role:
            with patch('events.idm.utils.add_group_role') as mock_add_group_role:
                self.assertFalse(utils.add_role(
                    self.request_user, settings.ROLE_SUPERUSER, uid=None,
                    group=None, login=None, object_pk=None, passport_login=None,
                ))
        mock_add_user_role.assert_not_called()
        mock_add_group_role.assert_not_called()


class TestIdmUtils_remove_role(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_should_invoke_remove_user_role(self):
        with patch('events.idm.utils.remove_user_role') as mock_remove_user_role:
            with patch('events.idm.utils.remove_group_role') as mock_remove_group_role:
                mock_remove_user_role.return_value = True
                self.assertTrue(utils.remove_role(
                    self.request_user, settings.ROLE_SUPERUSER, uid='123454321', login='test-new-user',
                    group=None, object_pk=None, fired=True, passport_login=None,
                ))
        mock_remove_user_role.assert_called_once_with(
            self.request_user, settings.ROLE_SUPERUSER, '123454321', login='test-new-user',
            object_pk=None, fired=True,
        )
        mock_remove_group_role.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_invoke_remove_external_user_role(self):
        with (
            patch('events.idm.utils.get_external_user_uid_by_login') as mock_get_user_uid,
            patch('events.idm.utils.remove_user_role') as mock_remove_user_role,
            patch('events.idm.utils.remove_group_role') as mock_remove_group_role,
        ):
            mock_get_user_uid.return_value = '321321321'
            mock_remove_user_role.return_value = True
            self.assertTrue(utils.remove_role(
                self.request_user, settings.ROLE_SUPERUSER, uid='123454321', login='test-new-user',
                group=None, object_pk=None, fired=False, passport_login='yndx-test-new-user',
            ))
        mock_get_user_uid.assert_called_once_with('yndx-test-new-user')
        mock_remove_user_role.assert_called_once_with(
            self.request_user, settings.ROLE_SUPERUSER, '321321321', login='yndx-test-new-user',
            object_pk=None, fired=False,
        )
        mock_remove_group_role.assert_not_called()

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_invoke_remove_external_user_role(self):
        with (
            patch('events.idm.utils.get_external_user_uid_by_login') as mock_get_user_uid,
            patch('events.idm.utils.remove_user_role') as mock_remove_user_role,
            patch('events.idm.utils.remove_group_role') as mock_remove_group_role,
        ):
            mock_get_user_uid.return_value = None
            self.assertFalse(utils.remove_role(
                self.request_user, settings.ROLE_SUPERUSER, uid='123454321', login='test-new-user',
                group=None, object_pk=None, fired=False, passport_login='yndx-test-new-user',
            ))
        mock_get_user_uid.assert_called_once_with('yndx-test-new-user')
        mock_remove_user_role.assert_not_called()
        mock_remove_group_role.assert_not_called()

    def test_should_invoke_remove_group_role(self):
        with patch('events.idm.utils.remove_user_role') as mock_remove_user_role:
            with patch('events.idm.utils.remove_group_role') as mock_remove_group_role:
                mock_remove_group_role.return_value = True
                self.assertTrue(utils.remove_role(
                    self.request_user, settings.ROLE_SUPERUSER, uid=None, login=None,
                    group=12321, object_pk=None, fired=False, passport_login=None,
                ))
        mock_remove_user_role.assert_not_called()
        mock_remove_group_role.assert_called_once_with(
            self.request_user, settings.ROLE_SUPERUSER, 12321,
            object_pk=None,
        )

    def test_shouldnt_invoke_any_method(self):
        with patch('events.idm.utils.remove_user_role') as mock_remove_user_role:
            with patch('events.idm.utils.remove_group_role') as mock_remove_group_role:
                self.assertFalse(utils.remove_role(
                    self.request_user, settings.ROLE_SUPERUSER, uid=None, login=None,
                    group=None, object_pk=None, fired=False, passport_login=None,
                ))
        mock_remove_user_role.assert_not_called()
        mock_remove_group_role.assert_not_called()


class TestIdmUtils_staff_users(TestCase):
    @responses.activate
    def test_should_find_users_by_login(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [
                    {'uid': '123454321', 'login': 'user1'},
                ],
            },
        )
        login = 'user1'
        users = {
            user['login']: user['uid']
            for user in utils.find_users_by_login(login)
        }
        self.assertEqual(len(users), 1)
        self.assertEqual(users[login], '123454321')

    @responses.activate
    def test_shouldnt_find_users_by_login(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [],
            },
        )
        login = 'user1'
        users = {
            user['login']: user['uid']
            for user in utils.find_users_by_login(login)
        }
        self.assertEqual(len(users), 0)

    @responses.activate
    def test_should_find_users_by_uid(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [
                    {'uid': '123454321', 'login': 'user1'},
                ],
            },
        )
        uid = '123454321'
        users = {
            user['uid']: user['login']
            for user in utils.find_users_by_uid(uid)
        }
        self.assertEqual(len(users), 1)
        self.assertEqual(users[uid], 'user1')

    @responses.activate
    def test_shouldnt_find_users_by_uid(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [],
            },
        )
        uid = '123454321'
        users = {
            user['uid']: user['login']
            for user in utils.find_users_by_uid(uid)
        }
        self.assertEqual(len(users), 0)

    @responses.activate
    def test_should_get_users_from_staff_create_if_not_exists(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [
                    {'uid': '123454321', 'login': 'user1'},
                    {'uid': '543212345', 'login': 'user3'},
                ],
            },
        )
        logins = ['user1' 'user2', 'user3']
        users = {
            user.uid: user.username
            for user in utils.get_users_from_staff(logins)
        }
        self.assertEqual(len(users), 2)
        self.assertEqual(users['123454321'], 'user1')
        self.assertEqual(users['543212345'], 'user3')

    @responses.activate
    def test_should_get_users_from_staff_only_existing_users(self):
        user = UserFactory()
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [
                    {'uid': user.uid, 'login': user.username},
                    {'uid': '543212345', 'login': 'user3'},
                ],
            },
        )
        logins = [user.username, 'user2', 'user3']
        users = utils.get_users_from_staff(logins, create=False)
        self.assertEqual(len(users), 1)
        self.assertEqual(users[0], user)

    def test_should_get_users_create_if_not_exists(self):
        user = UserFactory()
        logins = [user.username, 'user2', 'user3']
        with patch('events.idm.utils.get_users_from_staff') as mock_get_users_from_staff:
            mock_get_users_from_staff.return_value = []
            users = utils.get_users(logins, create=True)

        self.assertEqual(len(users), 1)
        mock_get_users_from_staff.assert_called_once_with({'user2', 'user3'}, create=True)

    def test_should_get_users_dont_create_if_not_exists(self):
        user = UserFactory()
        logins = [user.username, 'user2', 'user3']
        with patch('events.idm.utils.get_users_from_staff') as mock_get_users_from_staff:
            users = utils.get_users(logins, create=False)

        self.assertEqual(len(users), 1)
        mock_get_users_from_staff.assert_not_called()


class TestIdmUtils_add_batch_memberships(TestCase):
    @responses.activate
    def test_should_add_memberships(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons/',
            json={
                'links': {},
                'result': [
                    {'uid': '543212345', 'login': 'user3'},
                ],
            },
        )
        user = UserFactory()
        group = GroupFactory()
        group_id = utils.get_group_id(group.name)

        data = [
            {'group': group_id, 'login': user.username},
            {'group': 12321, 'login': user.username},
            {'group': 12321, 'login': 'user3'},
            {'group': group_id, 'login': 'user3'},
        ]
        utils.add_batch_memberships(data)

        user2 = User.objects.get(username='user3')
        group2 = utils.find_group(12321)
        self.assertIsNotNone(group2)

        users = list(group.user_set.all())
        self.assertTrue(user in users)
        self.assertTrue(user2 in users)

        users = list(group2.user_set.all())
        self.assertTrue(user in users)
        self.assertTrue(user2 in users)


class TestIdmUtils_remove_batch_memberships(TestCase):
    def test_should_remove_memberships(self):
        user1 = UserFactory()
        user2 = UserFactory()
        group1 = GroupFactory()
        group2 = GroupFactory()
        group1.user_set.add(user1, user2)
        group2.user_set.add(user1)

        group1_id = utils.get_group_id(group1.name)
        group2_id = utils.get_group_id(group2.name)
        data = [
            {'group': group1_id, 'login': user2.username},
            {'group': group2_id, 'login': user1.username},
            {'group': 12321, 'login': user2.username},
            {'group': 12321, 'login': 'user3'},
        ]
        utils.remove_batch_memberships(data)

        self.assertListEqual(list(group1.user_set.all()), [user1])
        self.assertListEqual(list(group2.user_set.all()), [])
        self.assertIsNone(utils.find_group(12321))


class TestIdmUtils_idm_utils(TestCase):
    def test_should_invoke_request_roles(self):
        idm_utils = utils._IdmUtils()
        with patch.object(idm_utils.client, 'request_roles') as mock_request_roles:
            idm_utils.request_roles(
                settings.ROLE_FORM_MANAGER,
                users=['user1'],
                groups=[12321],
                object_pk=123,
            )
        mock_request_roles.assert_called_once_with(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=['user1'],
            groups=[12321],
            fields_data={'object_pk': '123'},
            silent=True,
        )

    def test_should_invoke_reject_roles(self):
        idm_utils = utils._IdmUtils()
        with patch.object(idm_utils.client, 'reject_roles') as mock_reject_roles:
            idm_utils.reject_roles(
                settings.ROLE_FORM_MANAGER,
                users=['user1'],
                groups=[12321],
                object_pk=123,
            )
        mock_reject_roles.assert_called_once_with(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=['user1'],
            groups=[12321],
            fields_data={'object_pk': '123'},
            state='granted,requested',
        )

    def test_should_invoke_request_roles_and_catch_error(self):
        idm_utils = utils._IdmUtils()
        with patch.object(idm_utils.client, 'request_roles') as mock_request_roles:
            mock_request_roles.side_effect = HTTPError
            idm_utils.request_roles(
                settings.ROLE_FORM_MANAGER,
                users=['user1'],
                groups=[12321],
                object_pk=123,
            )
        mock_request_roles.assert_called_once_with(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=['user1'],
            groups=[12321],
            fields_data={'object_pk': '123'},
            silent=True,
        )

    def test_should_invoke_reject_roles_and_catch_error(self):
        idm_utils = utils._IdmUtils()
        with patch.object(idm_utils.client, 'reject_roles') as mock_reject_roles:
            mock_reject_roles.side_effect = HTTPError
            idm_utils.reject_roles(
                settings.ROLE_FORM_MANAGER,
                users=['user1'],
                groups=[12321],
                object_pk=123,
            )
        mock_reject_roles.assert_called_once_with(
            system=settings.IDM_SYSTEM,
            path=f'/{settings.ROLE_FORM_MANAGER}/',
            users=['user1'],
            groups=[12321],
            fields_data={'object_pk': '123'},
            state='granted,requested',
        )


class TestIdmUtils_permission_log(TestCase):
    def setUp(self):
        self.request_user = UserFactory()

    def test_assign_perm_with_log_shouldnt_invoke_assign_perm(self):
        user = UserFactory()
        with patch('events.idm.utils.assign_perm') as mock_assign_perm:
            utils.assign_perm_with_log(self.request_user, settings.ROLE_SUPERUSER, user)
        qs = PermissionLog.objects.filter(
            request_user=self.request_user, role_name=settings.ROLE_SUPERUSER,
            change_type=PermissionLog.ASSIGN, user=user,
        )
        self.assertEqual(qs.count(), 1)
        mock_assign_perm.assert_not_called()

    def test_assign_perm_with_log_should_invoke_assign_perm(self):
        user = UserFactory()
        survey = SurveyFactory()
        with patch('events.idm.utils.assign_perm') as mock_assign_perm:
            utils.assign_perm_with_log(self.request_user, settings.ROLE_FORM_MANAGER, user, obj=survey)
        qs = PermissionLog.objects.filter(
            request_user=self.request_user, role_name=settings.ROLE_FORM_MANAGER,
            change_type=PermissionLog.ASSIGN, user=user,
        )
        self.assertEqual(qs.count(), 1)
        mock_assign_perm.assert_called_once_with(settings.ROLE_FORM_MANAGER, user, survey)

    def test_remove_perm_with_log_shouldnt_invoke_remove_perm(self):
        user = UserFactory()
        with patch('events.idm.utils.remove_perm') as mock_remove_perm:
            utils.remove_perm_with_log(self.request_user, settings.ROLE_SUPERUSER, user)
        qs = PermissionLog.objects.filter(
            request_user=self.request_user, role_name=settings.ROLE_SUPERUSER,
            change_type=PermissionLog.REMOVE, user=user,
        )
        self.assertEqual(qs.count(), 1)
        mock_remove_perm.assert_not_called()

    def test_remove_perm_with_log_should_invoke_remove_perm(self):
        user = UserFactory()
        survey = SurveyFactory()
        with patch('events.idm.utils.remove_perm') as mock_remove_perm:
            utils.remove_perm_with_log(self.request_user, settings.ROLE_FORM_MANAGER, user, obj=survey)
        qs = PermissionLog.objects.filter(
            request_user=self.request_user, role_name=settings.ROLE_FORM_MANAGER,
            change_type=PermissionLog.REMOVE, user=user,
        )
        self.assertEqual(qs.count(), 1)
        mock_remove_perm.assert_called_once_with(settings.ROLE_FORM_MANAGER, user, survey)

    def test_save_permission_log_should_insert_rows(self):
        users = [UserFactory(), UserFactory()]
        groups = [GroupFactory(), GroupFactory()]
        survey = SurveyFactory()
        utils.save_permission_log(
            self.request_user, settings.ROLE_FORM_MANAGER, PermissionLog.ASSIGN,
            users=users, groups=groups, object_pk=survey.pk,
        )
        qs = (
            PermissionLog.objects.filter(
                request_user=self.request_user, role_name=settings.ROLE_FORM_MANAGER,
                change_type=PermissionLog.ASSIGN,
            )
            .filter(Q(user__in=users) | Q(group__in=groups))
        )
        self.assertEqual(qs.count(), 4)

    def test_save_permission_log_shouldnt_insert_rows(self):
        survey = SurveyFactory()
        utils.save_permission_log(
            self.request_user, settings.ROLE_FORM_MANAGER, PermissionLog.REMOVE,
            users=[], groups=[], object_pk=survey.pk,
        )
        qs = PermissionLog.objects.filter(
            request_user=self.request_user, role_name=settings.ROLE_FORM_MANAGER,
            change_type=PermissionLog.REMOVE,
        )
        self.assertEqual(qs.count(), 0)
