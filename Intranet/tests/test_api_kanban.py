from typing import List

from django.test import TransactionTestCase

from mentor.core.validators import validate_datetime
from mentor.users.tests.factories import UserFactory

from ..models import MenteeFeedback, MentorFeedback, Mentorship
from .factories import (
    MenteeFeedbackFactory,
    MentorFactory,
    MentorFeedbackFactory,
    MentorshipFactory,
)


class ListKanbanMenteesTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.mentor = MentorFactory()

        self.client.force_login(self.mentor.user)

        self.mentorships = MentorshipFactory.create_batch(3, mentor=self.mentor)

        self.url = self.build_url()
        self.expected = self.build_expected(self.mentorships)

    def build_url(self):
        return "/api/v1/kanban/mentees/"

    def build_expected(self, mentorships: List[Mentorship]):
        return [
            {
                "id": mentorship.id,
                "mentee": {
                    "id": mentorship.mentee.id,
                    "username": mentorship.mentee.username,
                    "staff_profile": {
                        "id": mentorship.mentee.staff_profile.id,
                        "full_name": mentorship.mentee.staff_profile.full_name,
                        "position": mentorship.mentee.staff_profile.position,
                    },
                },
                "intro": mentorship.intro,
                "status": mentorship.status,
                "status_by_id": mentorship.status_by_id,
                "status_message": mentorship.status_message,
                "created": validate_datetime(mentorship.created),
                "modified": validate_datetime(mentorship.modified),
            }
            for mentorship in mentorships
        ]

    def test_list(self):
        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_for_current_user(self):
        MentorshipFactory()

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_not_removed(self):
        MentorshipFactory(mentor=self.mentor, removed_by_mentor=True)

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)


class ListKanbanMentorsTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.user = UserFactory()

        self.client.force_login(self.user)

        self.mentorships = MentorshipFactory.create_batch(3, mentee=self.user)

        self.url = self.build_url()
        self.expected = self.build_expected(self.mentorships)

    def build_url(self):
        return "/api/v1/kanban/mentors/"

    def build_expected(self, mentorships: List[Mentorship]):
        return [
            {
                "id": mentorship.id,
                "mentor": {
                    "id": mentorship.mentor.id,
                    "user": {
                        "id": mentorship.mentor.user.id,
                        "username": mentorship.mentor.user.username,
                        "staff_profile": {
                            "id": mentorship.mentor.user.staff_profile.id,
                            "full_name": mentorship.mentor.user.staff_profile.full_name,
                            "position": mentorship.mentor.user.staff_profile.position,
                        },
                    },
                },
                "intro": mentorship.intro,
                "status": mentorship.status,
                "status_by_id": mentorship.status_by_id,
                "status_message": mentorship.status_message,
                "created": validate_datetime(mentorship.created),
                "modified": validate_datetime(mentorship.modified),
            }
            for mentorship in mentorships
        ]

    def test_list(self):
        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_for_current_user(self):
        MentorshipFactory()

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_not_removed(self):
        MentorshipFactory(mentee=self.user, removed_by_mentee=True)

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)


class ListKanbanMenteesFeedbackTestCase(TransactionTestCase):
    url = "/api/v1/kanban/mentees/feedback/"

    def setUp(self) -> None:
        self.mentor = MentorFactory()

        self.client.force_login(self.mentor.user)

        self.mentor_feedback = MentorFeedbackFactory.create_batch(
            3,
            mentor=self.mentor,
            is_visible=True,
        )

        self.expected = self.build_expected(self.mentor_feedback)

    def build_expected(self, mentor_feedback: List[MentorFeedback]):
        return [
            {
                "id": feedback.id,
                "mentor_id": feedback.mentor_id,
                "mentorship_id": feedback.mentorship_id,
                "comments": feedback.comments,
                "can_publish": feedback.can_publish,
                "is_published": feedback.is_published,
                "created": validate_datetime(feedback.created),
                "modified": validate_datetime(feedback.modified),
            }
            for feedback in mentor_feedback
        ]

    def test_list(self):
        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_for_current_user(self):
        another_mentor = MentorFactory()
        MentorFeedbackFactory(
            mentor=another_mentor,
            is_visible=True,
        )

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_visible(self):
        MentorFeedbackFactory(
            mentor=self.mentor,
            is_visible=False,
        )

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)


class ListKanbanMentorsFeedbackTestCase(TransactionTestCase):
    url = "/api/v1/kanban/mentors/feedback/"

    def setUp(self) -> None:
        self.mentee = UserFactory()

        self.client.force_login(self.mentee)

        self.mentee_feedback = MenteeFeedbackFactory.create_batch(
            3,
            mentee=self.mentee,
            is_visible=True,
        )

        self.expected = self.build_expected(self.mentee_feedback)

    def build_expected(self, mentee_feedback: List[MenteeFeedback]):
        return [
            {
                "id": feedback.id,
                "mentee_id": feedback.mentee_id,
                "mentorship_id": feedback.mentorship_id,
                "comments": feedback.comments,
                "can_publish": feedback.can_publish,
                "is_published": feedback.is_published,
                "created": validate_datetime(feedback.created),
                "modified": validate_datetime(feedback.modified),
            }
            for feedback in mentee_feedback
        ]

    def test_list(self):
        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_for_current_user(self):
        another_mentee = UserFactory()
        MenteeFeedbackFactory(
            mentee=another_mentee,
            is_visible=True,
        )

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)

    def test_list_only_visible(self):
        MenteeFeedbackFactory(
            mentee=self.mentee,
            is_visible=False,
        )

        with self.assertNumQueries(3):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), self.expected)
