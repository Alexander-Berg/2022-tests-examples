from unittest.mock import MagicMock, patch

import faker

from django.test import TestCase
from django.utils.timezone import utc

from lms.courses.tests.factories import CourseFactory
from lms.enrollments.models import EnrolledUser
from lms.enrollments.tests.factories import EnrolledUserFactory
from lms.staff.tests.factories import UserWithStaffProfileFactory
from lms.users.tests.factories import UserFactory

from ..models import CourseFollower
from .factories import CourseFollowerFactory

fake = faker.Faker()


class UnsubscribeModelTestCase(TestCase):
    def test_unsubscribe_on_create_enrolled_user(self):
        self.course = CourseFactory(is_active=True, enable_followers=True)
        self.user = UserFactory()
        self.follower = CourseFollowerFactory(course=self.course, user=self.user, is_active=True)

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment
        with patch('lms.mailing.services.timezone.now', new=mocked_now):
            self.enrolled_user = EnrolledUserFactory(
                course=self.course, user=self.user, status=EnrolledUser.StatusChoices.PENDING,
            )

        self.follower.refresh_from_db()

        self.assertFalse(self.follower.is_active)
        self.assertEqual(self.follower.unsubscribed_date, now_moment)
        self.assertEqual(self.follower.unsubscription_reason, CourseFollower.UnsubscriptionReasonChoice.ENROLLED)

    def test_unsubscribe_on_dismiss(self):
        self.courses = CourseFactory.create_batch(2)
        self.user, self.other_user = UserWithStaffProfileFactory.create_batch(2)
        self.staff_profile = self.user.staffprofile
        self.followers = [
            CourseFollowerFactory(course=course, user=self.user, is_active=True) for course in self.courses
        ]
        self.other_followers = [
            CourseFollowerFactory(course=course, user=self.other_user, is_active=True) for course in self.courses
        ]

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.mailing.services.timezone.now', new=mocked_now):
            with self.assertNumQueries(5):
                self.staff_profile.is_dismissed = True
                self.staff_profile.save()

        for follower in self.followers:
            follower.refresh_from_db()
            self.assertFalse(follower.is_active)
            self.assertEqual(follower.unsubscribed_date, now_moment)
            self.assertEqual(follower.unsubscription_reason, CourseFollower.UnsubscriptionReasonChoice.DISMISSED)

        for follower in self.other_followers:
            follower.refresh_from_db()
            self.assertTrue(follower.is_active)
