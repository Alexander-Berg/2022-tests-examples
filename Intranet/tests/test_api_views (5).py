from unittest.mock import MagicMock, patch

import faker

from django.conf import settings
from django.utils.timezone import utc

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import CourseFactory
from lms.users.tests.factories import UserFactory

from ..models import CourseFollower
from .factories import CourseFollowerFactory

fake = faker.Faker()


class CourseFollowTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-follow'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/follow/', args=(self.course.id,), base_url=settings.API_BASE_URL)

    def test_follow(self):
        self.client.force_login(user=self.user)

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.mailing.views.api.timezone.now', new=mocked_now):
            with self.assertNumQueries(11):
                response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.assertEqual(CourseFollower.objects.filter(course=self.course, user=self.user).count(), 1)
        follower = CourseFollower.objects.filter(course=self.course, user=self.user).first()
        self.assertTrue(follower.is_active)
        self.assertEqual(follower.subscribed_date, now_moment)
        self.assertIsNone(follower.unsubscribed_date)
        self.assertIsNone(follower.unsubscription_reason)

    def test_follow_unfollowed(self):
        self.course_follower = CourseFollowerFactory(
            course=self.course,
            user=self.user,
            is_active=False,
            unsubscription_reason=CourseFollower.UnsubscriptionReasonChoice.USER,
        )

        self.client.force_login(user=self.user)

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.mailing.views.api.timezone.now', new=mocked_now):
            with self.assertNumQueries(10):
                response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.course_follower.refresh_from_db()
        self.assertTrue(self.course_follower.is_active)
        self.assertEqual(self.course_follower.subscribed_date, now_moment)
        self.assertIsNone(self.course_follower.unsubscribed_date)
        self.assertIsNone(self.course_follower.unsubscription_reason)

    def test_follow_already_followed(self):
        subscribed_date = fake.date_time(tzinfo=utc)
        unsubscribed_date = fake.date_time(tzinfo=utc)
        self.course_follower = CourseFollowerFactory(
            course=self.course,
            user=self.user,
            is_active=True,
            subscribed_date=subscribed_date,
            unsubscribed_date=unsubscribed_date,
            unsubscription_reason=CourseFollower.UnsubscriptionReasonChoice.USER,
        )

        self.client.force_login(user=self.user)

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.mailing.views.api.timezone.now', new=mocked_now):
            with self.assertNumQueries(8):
                response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.course_follower.refresh_from_db()
        self.assertTrue(self.course_follower.is_active)
        self.assertEqual(self.course_follower.subscribed_date, now_moment)
        self.assertIsNone(self.course_follower.unsubscribed_date)
        self.assertIsNone(self.course_follower.unsubscription_reason)


class CourseUnFollowTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-unfollow'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/unfollow/', args=(self.course.id,), base_url=settings.API_BASE_URL)

    def test_unfollow(self):
        subscribed_date = fake.date_time(tzinfo=utc)
        self.course_follower = CourseFollowerFactory(
            course=self.course, user=self.user, is_active=True, subscribed_date=subscribed_date,
        )

        self.client.force_login(user=self.user)

        now_moment = fake.date_time(tzinfo=utc)
        mocked_now = MagicMock()
        mocked_now.return_value = now_moment

        with patch('lms.mailing.views.api.timezone.now', new=mocked_now):
            with self.assertNumQueries(6):
                response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.course_follower.refresh_from_db()
        self.assertFalse(self.course_follower.is_active)
        self.assertEqual(self.course_follower.subscribed_date, subscribed_date)
        self.assertEqual(self.course_follower.unsubscribed_date, now_moment)
        self.assertEqual(self.course_follower.unsubscription_reason, CourseFollower.UnsubscriptionReasonChoice.USER)

    def test_unfollow_not_followed(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.assertFalse(CourseFollower.objects.filter(course=self.course, user=self.user).exists())

    def test_unfollow_already_unfollowed(self):
        subscribed_date = fake.date_time(tzinfo=utc)
        unsubscribed_date = fake.date_time(tzinfo=utc)
        self.course_follower = CourseFollowerFactory(
            course=self.course, user=self.user, is_active=False,
            subscribed_date=subscribed_date, unsubscribed_date=unsubscribed_date,
        )

        self.client.force_login(user=self.user)

        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(self.course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        self.course_follower.refresh_from_db()
        self.assertFalse(self.course_follower.is_active)
        self.assertEqual(self.course_follower.subscribed_date, subscribed_date)
        self.assertEqual(self.course_follower.unsubscribed_date, unsubscribed_date)


class MyCourseFollowsListViewSetTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:user-course-follows'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.followed_courses = CourseFactory.create_batch(2, is_active=True)
        self.not_followed_courses = CourseFactory.create_batch(2, is_active=True)
        self.followers = [
            CourseFollowerFactory(course=followed_course, user=self.user, is_active=True)
            for followed_course in self.followed_courses
        ]
        self.other_followers = [
            CourseFollowerFactory(course=followed_course, is_active=True)
            for followed_course in self.followed_courses
        ]

    def test_url(self):
        self.assertURLNameEqual('my/course_follows/', base_url=settings.API_BASE_URL)

    def test_list(self):
        self.client.force_login(user=self.user)
        url = self.get_url()

        expected = [
            {
                'course': {
                    'id': course.id,
                    'name': course.name,
                    'type': course.course_type,
                },
            } for course in reversed(self.followed_courses)
        ]
        self.list_request(url=url, expected=expected, num_queries=3, pagination=False, check_ids=False)

    def test_filtered_list(self):
        self.client.force_login(user=self.user)
        url = f'{self.get_url()}?course_id={self.followed_courses[0].id}'

        expected = [
            {
                'course': {
                    'id': course.id,
                    'name': course.name,
                    'type': course.course_type,
                },
            } for course in [self.followed_courses[0]]
        ]
        self.list_request(url=url, expected=expected, num_queries=4, pagination=False, check_ids=False)
