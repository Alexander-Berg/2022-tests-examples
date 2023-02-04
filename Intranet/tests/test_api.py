from django.contrib.auth import get_user_model
from django.test import TransactionTestCase

from ..constants import DEFAULT_USER_SETTINGS
from .factories import UserFactory

User = get_user_model()


class RetrieveUserSettingsTestCase(TransactionTestCase):
    url = "/api/v1/users/settings/"

    def test_default_settings(self):
        self.user = UserFactory()
        self.client.force_login(self.user)

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), DEFAULT_USER_SETTINGS)

    def test_specified_settings(self):
        settings = {
            setting_key: not DEFAULT_USER_SETTINGS[setting_key]
            for setting_key in DEFAULT_USER_SETTINGS
        }
        self.user = UserFactory(settings=settings)
        self.client.force_login(self.user)

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), settings)


class UpdateUserSettingsTestCase(TransactionTestCase):
    url = "/api/v1/users/settings/"

    def setUp(self) -> None:
        self.user = UserFactory(
            settings={
                "mentee_created_mentorship_notification": True,
                "mentee_completed_mentorship_notification": True,
                "mentee_left_feedback_notification": True,
                "mentor_accepted_mentorship_notification": True,
                "mentor_declined_mentorship_notification": True,
                "mentor_completed_mentorship_notification": True,
                "mentor_left_feedback_notification": True,
            }
        )

    def test_settings_update(self):
        self.client.force_login(self.user)

        new_settings = {
            "mentee_created_mentorship_notification": False,
            "mentee_completed_mentorship_notification": False,
            "mentee_left_feedback_notification": False,
            "mentor_accepted_mentorship_notification": False,
            "mentor_declined_mentorship_notification": False,
            "mentor_completed_mentorship_notification": False,
            "mentor_left_feedback_notification": False,
        }

        with self.assertNumQueries(5):
            response = self.client.patch(
                self.url,
                data=new_settings,
                content_type="application/json",
            )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), new_settings)
