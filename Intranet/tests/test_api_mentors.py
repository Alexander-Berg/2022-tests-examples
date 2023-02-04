import datetime
from typing import List

from django.test import TransactionTestCase
from django.utils import timezone

from mentor.core.validators import validate_datetime
from mentor.users.tests.factories import UserFactory

from ..models import Mentor
from .factories import MentorFactory, MentorFeedbackFactory, MentorshipFactory


class RetrieveMentorTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.user = UserFactory()
        self.mentor = MentorFactory()

        self.client.force_login(self.user)

        self.url = self.build_url(username=self.mentor.user.username)
        self.expected = self.build_expected(mentor=self.mentor)

    def build_url(self, username: str):
        return "/api/v1/mentors/{username}/".format(username=username)

    def build_expected(self, mentor: Mentor):
        return {
            "id": mentor.id,
            "user": {
                "id": mentor.user.id,
                "username": mentor.user.username,
                "is_active": mentor.user.is_active,
                "staff_profile": {
                    "id": mentor.user.staff_profile.id,
                    "joined_at": mentor.user.staff_profile.joined_at,
                    "full_name": mentor.user.staff_profile.full_name,
                    "position": mentor.user.staff_profile.position,
                    "office": mentor.user.staff_profile.office,
                    "city": mentor.user.staff_profile.city,
                    "groups": [],
                },
            },
            "description": mentor.description,
            "assistance": mentor.assistance,
            "skills": [],
            "carrier_begin": mentor.carrier_begin,
            "feedback_count": mentor.feedback_count,
            "mentees_count": mentor.mentees_count,
            "is_published": mentor.is_published,
            "is_ready_for_mentorships": mentor.is_ready_for_mentorships,
            "has_active_mentorship": False,
        }

    def test_mentor_retrieve(self):
        with self.assertNumQueries(6):
            response = self.client.get(self.url)

        self.assertEqual(response.json(), self.expected)


class ListMentorFeedbackTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.maxDiff = None
        self.mentor = MentorFactory()

        now = timezone.now()
        month_earlier = now - datetime.timedelta(days=30)

        self.mentorships = MentorshipFactory.create_batch(
            3,
            mentor=self.mentor,
            accepted_date=month_earlier,
            completed_date=now,
        )

        self.mentor_feedback = [
            MentorFeedbackFactory(
                mentor=self.mentor,
                mentorship=mentorship,
                is_visible=True,
                can_publish=True,
                is_published=True,
            )
            for mentorship in self.mentorships
        ]

        self.url = self.build_url()
        self.expected = self.build_expected(self.mentor_feedback)

    def build_url(self):
        return "/api/v1/mentors/{username}/feedback/".format(
            username=self.mentor.user.username
        )

    def build_expected(self, mentor_feedback: List[MentorFeedbackFactory]):
        return [
            {
                "id": feedback.id,
                "mentorship": {
                    "id": feedback.mentorship.id,
                    "mentee": {
                        "id": feedback.mentorship.mentee.id,
                        "username": feedback.mentorship.mentee.username,
                        "staff_profile": {
                            "id": feedback.mentorship.mentee.staff_profile.id,
                            "full_name": feedback.mentorship.mentee.staff_profile.full_name,
                        },
                    },
                },
                "comments": feedback.comments,
                "is_published": feedback.is_published,
                "created": validate_datetime(feedback.created),
                "modified": validate_datetime(feedback.modified),
            }
            for feedback in mentor_feedback
        ]

    def test_list(self):
        with self.assertNumQueries(1):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_given_mentor(self):
        MentorFeedbackFactory(is_visible=True, can_publish=True, is_published=True)

        with self.assertNumQueries(1):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_published(self):
        mentorship = MentorshipFactory(mentor=self.mentor)
        MentorFeedbackFactory(
            mentor=self.mentor,
            mentorship=mentorship,
            is_visible=True,
            can_publish=True,
            is_published=False,
        )

        with self.assertNumQueries(1):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_allowed_to_publish(self):
        mentorship = MentorshipFactory(mentor=self.mentor)
        MentorFeedbackFactory(
            mentor=self.mentor,
            mentorship=mentorship,
            is_visible=True,
            can_publish=False,
            is_published=True,
        )

        with self.assertNumQueries(1):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_visible(self):
        mentorship = MentorshipFactory(mentor=self.mentor)
        MentorFeedbackFactory(
            mentor=self.mentor,
            mentorship=mentorship,
            is_visible=False,
            can_publish=True,
            is_published=True,
        )

        with self.assertNumQueries(1):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)
