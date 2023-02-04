from django.conf import settings
from django.contrib.auth.models import Permission, Group
from django.contrib.contenttypes.models import ContentType
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm
from unittest.mock import patch

from events.accounts.factories import UserFactory, GroupFactory, OrganizationToGroupFactory
from events.accounts.models import User
from events.staff.factories import StaffGroupFactory
from events.surveyme.factories import SurveyFactory, SurveyGroupFactory
from events.surveyme.models import Survey, SurveyGroup
from events.v3.errors import PermissionDenied
from events.v3.helpers import has_perm
from events.v3.perms import (
    check_perm, filter_permitted_surveys, get_permissions_out, change_permissions,
    CHANGE_SURVEY, CHANGE_SURVEYGROUP,
)
from events.v3.schemas import PermissionOut, PermissionIn, UserIn


class TestCheckPerm(TestCase):
    fixtures = ['initial_data.json']

    def test_should_check_if_user_has_permission_on_survey(self):
        user = UserFactory()
        survey = SurveyFactory()
        assign_perm(CHANGE_SURVEY, user, survey)
        check_perm(user, survey)

    def test_should_check_if_user_has_permission_on_survey_throug_survey_group(self):
        user = UserFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(CHANGE_SURVEYGROUP, user, survey_group)
        survey = SurveyFactory(group=survey_group)
        check_perm(user, survey)

    def test_should_check_if_user_has_permission_on_survey_group(self):
        user = UserFactory()
        survey_group = SurveyGroupFactory()
        assign_perm(CHANGE_SURVEYGROUP, user, survey_group)
        check_perm(user, survey_group)

    def test_shouldnt_check_if_user_has_not_permission_on_survey(self):
        user = UserFactory()
        survey = SurveyFactory()
        with self.assertRaises(PermissionDenied):
            check_perm(user, survey)

    def test_shouldnt_check_if_user_has_not_permission_on_survey_group(self):
        user = UserFactory()
        survey_group = SurveyGroupFactory()
        with self.assertRaises(PermissionDenied):
            check_perm(user, survey_group)


class TestFilterPermittedSurveys(TestCase):
    fixtures = ['initial_data.json']

    def test_should_return_survey_for_superuser(self):
        user = UserFactory(is_superuser=True)
        survey = SurveyFactory()
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    def test_shouldnt_return_survey_for_user_without_perms_intranet(self):
        user = UserFactory()
        survey = SurveyFactory()
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) == 0)
        self.assertTrue(survey.pk not in qs)

    def test_should_return_survey_for_user_with_perms_on_survey_intranet(self):
        user = UserFactory()
        survey = SurveyFactory()
        assign_perm(CHANGE_SURVEY, user, survey)
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    def test_should_return_survey_for_user_with_perms_on_surveygroup_intranet(self):
        user = UserFactory()
        survey_group = SurveyGroupFactory()
        survey = SurveyFactory(group=survey_group)
        assign_perm(CHANGE_SURVEYGROUP, user, survey_group)
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    def test_should_return_survey_for_group_with_perms_on_survey_intranet(self):
        user = UserFactory()
        group = GroupFactory()
        group.user_set.add(user)
        survey = SurveyFactory()
        assign_perm(CHANGE_SURVEY, group, survey)
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    def test_should_return_survey_for_group_with_perms_on_surveygroup_intranet(self):
        user = UserFactory()
        group = GroupFactory()
        group.user_set.add(user)
        survey_group = SurveyGroupFactory()
        survey = SurveyFactory(group=survey_group)
        assign_perm(CHANGE_SURVEYGROUP, group, survey_group)
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_return_survey_for_user_without_perms_b2c(self):
        user = UserFactory()
        survey = SurveyFactory()
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) == 0)
        self.assertTrue(survey.pk not in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_survey_for_author_b2c(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        qs = list(filter_permitted_surveys(user, None, Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) == 1)
        self.assertTrue(survey.pk in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_return_survey_for_user_without_perms_b2b(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(user)
        survey = SurveyFactory(org=o2g.org)
        qs = list(filter_permitted_surveys(user, [o2g.org.dir_id], Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) == 0)
        self.assertTrue(survey.pk not in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_survey_for_user_with_perms_on_survey_b2b(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(user)
        survey = SurveyFactory(org=o2g.org)
        assign_perm(CHANGE_SURVEY, user, survey)
        qs = list(filter_permitted_surveys(user, [o2g.org.dir_id], Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_survey_for_user_with_perms_on_surveygroup_b2b(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(user)
        survey_group = SurveyGroupFactory(org=o2g.org)
        survey = SurveyFactory(group=survey_group)
        assign_perm(CHANGE_SURVEYGROUP, user, survey_group)
        qs = list(filter_permitted_surveys(user, [o2g.org.dir_id], Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_survey_for_group_with_perms_on_survey_b2b(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(user)
        survey = SurveyFactory(org=o2g.org)
        assign_perm(CHANGE_SURVEY, o2g.group, survey)
        qs = list(filter_permitted_surveys(user, [o2g.org.dir_id], Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_return_survey_for_group_with_perms_on_surveygroup_b2b(self):
        user = UserFactory()
        o2g = OrganizationToGroupFactory()
        o2g.group.user_set.add(user)
        survey_group = SurveyGroupFactory(org=o2g.org)
        survey = SurveyFactory(group=survey_group)
        assign_perm(CHANGE_SURVEYGROUP, o2g.group, survey_group)
        qs = list(filter_permitted_surveys(user, [o2g.org.dir_id], Survey.objects.values_list('pk', flat=True)))
        self.assertTrue(len(qs) > 0)
        self.assertTrue(survey.pk in qs)


class TestGetPermissionsOut(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        Permission.objects.get_or_create(
            codename='viewfile_survey',
            content_type=ContentType.objects.get_for_model(Survey),
        )
        Permission.objects.get_or_create(
            codename='viewfile_surveygroup',
            content_type=ContentType.objects.get_for_model(SurveyGroup),
        )
        self.common_group = Group.objects.get(pk=settings.GROUP_ALLSTAFF_PK)

    def test_should_return_owner_access_for_survey(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        assign_perm('change_survey', user, survey)

        result = get_permissions_out(survey)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'owner',
            'action': 'change',
        }))

        assign_perm('viewfile_survey', user, survey)

        result = get_permissions_out(survey)
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'owner',
            'action': 'change',
        }))
        self.assertEqual(result[1], PermissionOut.parse_obj({
            'access': 'owner',
            'action': 'viewfile',
        }))

    def test_should_return_owner_access_for_surveygroup(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=user)
        assign_perm('change_surveygroup', user, surveygroup)

        result = get_permissions_out(surveygroup)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'owner',
            'action': 'change',
        }))

        assign_perm('viewfile_surveygroup', user, surveygroup)

        result = get_permissions_out(surveygroup)
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'owner',
            'action': 'change',
        }))
        self.assertEqual(result[1], PermissionOut.parse_obj({
            'access': 'owner',
            'action': 'viewfile',
        }))

    def test_should_return_common_access_for_survey(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        assign_perm('change_survey', self.common_group, survey)

        result = get_permissions_out(survey)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'common',
            'action': 'change',
        }))

        assign_perm('viewfile_survey', self.common_group, survey)

        result = get_permissions_out(survey)
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'common',
            'action': 'change',
        }))
        self.assertEqual(result[1], PermissionOut.parse_obj({
            'access': 'common',
            'action': 'viewfile',
        }))

    def test_should_return_common_access_for_surveygroup(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=user)
        assign_perm('change_surveygroup', self.common_group, surveygroup)

        result = get_permissions_out(surveygroup)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'common',
            'action': 'change',
        }))

        assign_perm('viewfile_surveygroup', self.common_group, surveygroup)

        result = get_permissions_out(surveygroup)
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'common',
            'action': 'change',
        }))
        self.assertEqual(result[1], PermissionOut.parse_obj({
            'access': 'common',
            'action': 'viewfile',
        }))

    def test_should_return_restricted_access_for_survey(self):
        survey = SurveyFactory(user=UserFactory())
        user = UserFactory()
        staff_group = StaffGroupFactory(name='Test group', url='svc_test')
        group = GroupFactory(name=f'group:{staff_group.pk}')
        assign_perm('change_survey', user, survey)
        assign_perm('change_survey', group, survey)

        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            result = get_permissions_out(survey)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        }))
        mock_get_name.assert_called_once()

        assign_perm('viewfile_survey', user, survey)
        assign_perm('viewfile_survey', group, survey)

        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            result = get_permissions_out(survey)
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        }))
        self.assertEqual(result[1], PermissionOut.parse_obj({
            'access': 'restricted',
            'action': 'viewfile',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        }))
        self.assertEqual(mock_get_name.call_count, 2)

    def test_should_return_restricted_access_for_surveygroup(self):
        surveygroup = SurveyGroupFactory(user=UserFactory())
        user = UserFactory()
        staff_group = StaffGroupFactory(name='Test group', url='svc_test')
        group = GroupFactory(name=f'group:{staff_group.pk}')
        assign_perm('change_surveygroup', user, surveygroup)
        assign_perm('change_surveygroup', group, surveygroup)

        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            result = get_permissions_out(surveygroup)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        }))
        mock_get_name.assert_called_once()

        assign_perm('viewfile_surveygroup', user, surveygroup)
        assign_perm('viewfile_surveygroup', group, surveygroup)

        with patch.object(User, 'get_name_and_surname_with_fallback') as mock_get_name:
            mock_get_name.return_value = 'John Doe'
            result = get_permissions_out(surveygroup)
        self.assertEqual(len(result), 2)
        self.assertEqual(result[0], PermissionOut.parse_obj({
            'access': 'restricted',
            'action': 'change',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        }))
        self.assertEqual(result[1], PermissionOut.parse_obj({
            'access': 'restricted',
            'action': 'viewfile',
            'users': [
                {'id': user.pk, 'login': user.username, 'display': 'John Doe', 'uid': user.uid},
            ],
            'groups': [
                {'id': staff_group.pk, 'name': staff_group.name, 'url': staff_group.get_info_url()},
            ],
        }))
        self.assertEqual(mock_get_name.call_count, 2)


class TestChangePermissionsOut(TestCase):
    fixtures = ['initial_data.json']

    def setUp(self):
        Permission.objects.get_or_create(
            codename='viewfile_survey',
            content_type=ContentType.objects.get_for_model(Survey),
        )
        Permission.objects.get_or_create(
            codename='viewfile_surveygroup',
            content_type=ContentType.objects.get_for_model(SurveyGroup),
        )
        self.common_group = Group.objects.get(pk=settings.GROUP_ALLSTAFF_PK)

    def test_should_make_owner_access_for_survey(self):
        user = UserFactory()
        survey = SurveyFactory(user=user)
        change_permissions(user, survey, [
            PermissionIn(access='owner', action='change'),
        ])
        self.assertTrue(has_perm(user, 'change_survey', survey))

        change_permissions(user, survey, [
            PermissionIn(access='owner', action='viewfile'),
        ])
        self.assertTrue(has_perm(user, 'change_survey', survey))
        self.assertTrue(has_perm(user, 'viewfile_survey', survey))

    def test_should_make_owner_access_for_surveygroup(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=user)
        change_permissions(user, surveygroup, [
            PermissionIn(access='owner', action='change'),
        ])
        self.assertTrue(has_perm(user, 'change_surveygroup', surveygroup))

        change_permissions(user, surveygroup, [
            PermissionIn(access='owner', action='viewfile'),
        ])
        self.assertTrue(has_perm(user, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(user, 'viewfile_surveygroup', surveygroup))

    def test_should_make_common_access_for_survey(self):
        user = UserFactory()
        survey = SurveyFactory(user=UserFactory())
        change_permissions(user, survey, [
            PermissionIn(access='common', action='change'),
        ])
        self.assertTrue(has_perm(self.common_group, 'change_survey', survey))

        change_permissions(user, survey, [
            PermissionIn(access='common', action='viewfile'),
        ])
        self.assertTrue(has_perm(self.common_group, 'change_survey', survey))
        self.assertTrue(has_perm(self.common_group, 'viewfile_survey', survey))

    def test_should_make_common_access_for_surveygroup(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=UserFactory())
        change_permissions(user, surveygroup, [
            PermissionIn(access='common', action='change'),
        ])
        self.assertTrue(has_perm(self.common_group, 'change_surveygroup', surveygroup))

        change_permissions(user, surveygroup, [
            PermissionIn(access='common', action='viewfile'),
        ])
        self.assertTrue(has_perm(self.common_group, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(self.common_group, 'viewfile_surveygroup', surveygroup))

    def test_should_make_restricted_access_for_survey(self):
        user = UserFactory()
        survey = SurveyFactory(user=UserFactory())
        staff_group = StaffGroupFactory()
        group = GroupFactory(name=f'group:{staff_group.pk}')
        change_permissions(user, survey, [
            PermissionIn(
                access='restricted', action='change',
                users=[user.uid], groups=[staff_group.pk],
            ),
        ])
        self.assertTrue(has_perm(user, 'change_survey', survey))
        self.assertTrue(has_perm(group, 'change_survey', survey))

        change_permissions(user, survey, [
            PermissionIn(
                access='restricted', action='viewfile',
                users=[UserIn(uid=user.uid)], groups=[staff_group.pk],
            ),
        ])
        self.assertTrue(has_perm(user, 'change_survey', survey))
        self.assertTrue(has_perm(user, 'viewfile_survey', survey))
        self.assertTrue(has_perm(group, 'change_survey', survey))
        self.assertTrue(has_perm(group, 'viewfile_survey', survey))

    def test_should_make_restricted_access_for_surveygroup(self):
        user = UserFactory()
        surveygroup = SurveyGroupFactory(user=UserFactory())
        staff_group = StaffGroupFactory()
        group = GroupFactory(name=f'group:{staff_group.pk}')
        change_permissions(user, surveygroup, [
            PermissionIn(
                access='restricted', action='change',
                users=[user.uid], groups=[staff_group.pk],
            ),
        ])
        self.assertTrue(has_perm(user, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(group, 'change_surveygroup', surveygroup))

        change_permissions(user, surveygroup, [
            PermissionIn(
                access='restricted', action='viewfile',
                users=[UserIn(uid=user.uid)], groups=[staff_group.pk],
            ),
        ])
        self.assertTrue(has_perm(user, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(user, 'viewfile_surveygroup', surveygroup))
        self.assertTrue(has_perm(group, 'change_surveygroup', surveygroup))
        self.assertTrue(has_perm(group, 'viewfile_surveygroup', surveygroup))
