from unittest.mock import patch

from django.test import TestCase, TransactionTestCase

from lms.users.signals import user_is_present
from lms.users.tests.factories import UserFactory

from ..models import StaffProfile


class UserPostSaveHandlerTestCase(TransactionTestCase):
    def test_user_post_save(self):
        with patch('lms.staff.receivers.get_staff_profile.delay') as mock:
            user = UserFactory()
            is_staff_profile_created = StaffProfile.objects.filter(user=user).exists()

            self.assertTrue(is_staff_profile_created)
            mock.assert_called_once_with(user.pk, True)

            user_is_present.send(sender=user.__class__, user=user)
            mock.assert_called_once()


class UserLoggedInHandlerTestCase(TestCase):
    def setUp(self) -> None:
        self.user = UserFactory()

    def test_user_logged_in(self):
        with patch('lms.staff.receivers.get_staff_profile.delay') as mock:
            user_is_present.send(sender=self.user.__class__, user=self.user)

            mock.assert_called_once_with(self.user.pk, False)

            user_is_present.send(sender=self.user.__class__, user=self.user)
            mock.assert_called_once()
