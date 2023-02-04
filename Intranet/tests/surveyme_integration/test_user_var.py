# -*- coding: utf-8 -*-
from unittest.mock import patch
from django.test import TestCase, override_settings
from django.utils.translation import ugettext_lazy as _

from events.accounts.factories import (
    OrganizationFactory,
    UserFactory,
)
from events.accounts.utils import (
    PersonalData,
    PassportPersonalData,
    DirPersonalData,
    GENDER_MALE,
    GENDER_FEMALE,
)
from events.surveyme.factories import ProfileSurveyAnswerFactory
from events.surveyme_integration.variables.user import (
    UserNameVariable,
    UserUIDVariable,
    UserEmailVariable,
    UserLoginVariable,
    UserLoginB2BVariable,
    UserKarmaVariable,
    UserKarmaStatusVariable,
    UserGenderVariable,
    UserPhoneVariable,
    UserDepartmentNameVariable,
    UserHeadVariable,
    UserGroupsVariable,
    UserPublicIdVariable,
)


class TestPersonalData(TestCase):  # {{{
    fixtures = ['initial_data.json']
    var_class = UserNameVariable

    def setUp(self):
        self.user = UserFactory()
        self.org = OrganizationFactory()
        self.answer = ProfileSurveyAnswerFactory(user=self.user)

    def test_intranet_with_uid(self):
        with patch('events.surveyme.models.PassportPersonalData', spec=PassportPersonalData):
            self.assertTrue(isinstance(self.answer.personal_data, PassportPersonalData))

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        self.assertIsNone(self.answer.personal_data)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_business_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', spec=DirPersonalData):
            self.assertTrue(isinstance(self.answer.personal_data, DirPersonalData))

    @override_settings(IS_BUSINESS_SITE=True)
    def test_business_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        self.assertIsNone(self.answer.personal_data)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        self.assertIsNone(self.answer.personal_data)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        self.assertIsNone(self.answer.personal_data)
# }}}


class TestBaseUserVariable(TestCase):  # {{{
    fixtures = ['initial_data.json']
    var_class = None

    def setUp(self):
        self.user = UserFactory()
        self.org = OrganizationFactory()
        self.answer = ProfileSurveyAnswerFactory(user=self.user)
# }}}


class TestUserNameVariable(TestBaseUserVariable):  # {{{
    var_class = UserNameVariable

    def test_intranet_with_uid(self):
        name = 'vova'
        surname = 'surname'
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(name=name, surname=surname)
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), '%s %s' % (name, surname))

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()
        name = 'vova'
        surname = 'surname'

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(name=name, surname=surname)
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), '%s %s' % (name, surname))

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserUIDVariable(TestBaseUserVariable):  # {{{
    var_class = UserUIDVariable

    def test_intranet_with_uid(self):
        var = self.var_class(answer=self.answer)
        self.assertEqual(var.get_value(), self.answer.user.uid)

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        var = self.var_class(answer=self.answer)
        self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        var = self.var_class(answer=self.answer)
        self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        var = self.var_class(answer=self.answer)
        self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        var = self.var_class(answer=self.answer)
        self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        var = self.var_class(answer=self.answer)
        self.assertIsNone(var.get_value())
# }}}


class TestUserEmailVariable(TestBaseUserVariable):  # {{{
    var_class = UserEmailVariable

    def test_intranet_with_uid(self):
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(email=self.user.email)
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), self.answer.user.email)

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(email=self.user.email)
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), self.answer.user.email)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserPublicIdVariable(TestBaseUserVariable):  # {{{
    var_class = UserPublicIdVariable

    def test_intranet_with_uid(self):
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(public_id='123abc')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), '123abc')

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(public_id=None)
        ):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserLoginVariable(TestBaseUserVariable):  # {{{
    var_class = UserLoginVariable

    def test_intranet_with_uid(self):
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(login='some-username')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), 'some-username')

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(login='some-login')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), 'some-login')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserLoginB2BVariable(TestUserLoginVariable):  # {{{
    var_class = UserLoginB2BVariable
# }}}


class TestUserKarmaVariable(TestBaseUserVariable):  # {{{
    var_class = UserKarmaVariable

    def test_intranet_with_uid(self):
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(karma='1')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), '1')

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserKarmaStatusVariable(TestBaseUserVariable):  # {{{
    var_class = UserKarmaStatusVariable

    def test_intranet_with_uid(self):
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(karma_status='2')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), '2')

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserGenderVariable(TestBaseUserVariable):  # {{{
    var_class = UserGenderVariable

    def test_intranet_with_uid(self):
        with patch(
            'events.surveyme.models.PassportPersonalData',
            return_value=PersonalData(gender=GENDER_MALE)
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), _('Мужской'))

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(gender=GENDER_FEMALE)
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), _('Женский'))

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserPhoneVariable(TestBaseUserVariable):  # {{{
    var_class = UserPhoneVariable

    def test_intranet_with_uid(self):
        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(phone='9117813')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), '9117813')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserDepartmentNameVariable(TestBaseUserVariable):  # {{{
    var_class = UserDepartmentNameVariable

    def test_intranet_with_uid(self):
        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(job_place='R&D')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), 'R&D')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}


class TestUserHeadVariable(TestBaseUserVariable):  # {{{
    var_class = UserHeadVariable

    def test_intranet_with_uid(self):
        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(manager='John Doe')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), 'John Doe')

        @override_settings(IS_BUSINESS_SITE=True)
        def test_directory_without_uid(self):
            self.user.uid = None
            self.user.save()
            self.answer.survey.org = self.org
            self.answer.survey.save()

            with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
                var = self.var_class(answer=self.answer)
                self.assertIsNone(var.get_value())

        @override_settings(IS_BUSINESS_SITE=True)
        def test_personal_with_uid(self):
            with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
                var = self.var_class(answer=self.answer)
                self.assertIsNone(var.get_value())

        @override_settings(IS_BUSINESS_SITE=True)
        def test_personal_without_uid(self):
            self.user.uid = None
            self.user.save()

            with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
                var = self.var_class(answer=self.answer)
                self.assertIsNone(var.get_value())
# }}}


class TestUserGroupsVariable(TestBaseUserVariable):  # {{{
    var_class = UserGroupsVariable

    def test_intranet_with_uid(self):
        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    def test_intranet_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.PassportPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_with_uid(self):
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch(
            'events.surveyme.models.DirPersonalData',
            return_value=PersonalData(groups='Help Desk')
        ):
            var = self.var_class(answer=self.answer)
            self.assertEqual(var.get_value(), 'Help Desk')

    @override_settings(IS_BUSINESS_SITE=True)
    def test_directory_without_uid(self):
        self.user.uid = None
        self.user.save()
        self.answer.survey.org = self.org
        self.answer.survey.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_with_uid(self):
        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())

    @override_settings(IS_BUSINESS_SITE=True)
    def test_personal_without_uid(self):
        self.user.uid = None
        self.user.save()

        with patch('events.surveyme.models.DirPersonalData', return_value=PersonalData()):
            var = self.var_class(answer=self.answer)
            self.assertIsNone(var.get_value())
# }}}
