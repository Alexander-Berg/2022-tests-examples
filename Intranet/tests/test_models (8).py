from django.core.exceptions import ValidationError
from django.test import TestCase

from lms.users.tests.factories import UserFactory

from .factories import MentorshipFactory


class MentorshipModelTestCase(TestCase):
    def test_same_mentor_and_mentee(self):
        user = UserFactory()
        with self.assertRaises(ValidationError):
            MentorshipFactory(mentor=user, mentee=user)
