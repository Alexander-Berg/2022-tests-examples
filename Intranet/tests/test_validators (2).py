import pytest

from django.core.exceptions import ValidationError
from django.test import SimpleTestCase

from ..enums import UserSettings
from ..validators import user_settings_validator


class UserSettingsValidatorTestCase(SimpleTestCase):
    def setUp(self) -> None:
        self.valid_settings = {
            "mentee_created_mentorship_notification": True,
            "mentee_completed_mentorship_notification": True,
            "mentee_left_feedback_notification": True,
            "mentor_accepted_mentorship_notification": True,
            "mentor_declined_mentorship_notification": True,
            "mentor_completed_mentorship_notification": True,
            "mentor_left_feedback_notification": True,
        }

    def test_invalid_settings_object(self):
        settings = "not a dict"

        with self.assertRaises(ValidationError) as cm:
            user_settings_validator(settings)

        the_exception = cm.exception
        self.assertEqual(the_exception.code, "NOT_A_DICT")

    def test_not_existing_setting_key(self):
        settings = {**self.valid_settings, "setting_that_does_not_exist": True}

        with self.assertRaises(ValidationError) as cm:
            user_settings_validator(settings)

        the_exception = cm.exception
        self.assertEqual(the_exception.code, "KEY_DOES_NOT_EXIST")

    def test_invalid_setting_value(self):
        for setting_key in UserSettings:
            settings = {**self.valid_settings, setting_key: b"invalid type"}

            with self.assertRaises(ValidationError) as cm:
                user_settings_validator(settings)

            the_exception = cm.exception
            self.assertEqual(the_exception.code, "VALUE_TYPE_IS_INVALID")

    def test_valid_settings(self):
        try:
            user_settings_validator(self.valid_settings)
        except Exception as exception:
            raise pytest.fail("DID RAISE {0}".format(exception))
