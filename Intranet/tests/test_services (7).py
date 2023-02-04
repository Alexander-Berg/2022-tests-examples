from django.test import TransactionTestCase

from mentor.users.tests.factories import UserFactory

from ..models import Mentorship
from ..services import (
    has_active_mentorship,
    update_mentor_feedback_count,
    update_mentor_mentees_count,
)
from .factories import MentorFactory, MentorFeedbackFactory, MentorshipFactory


class UpdateMentorMenteesCountTestCase(TransactionTestCase):
    def test_correct_mentees_count(self) -> None:
        mentor = MentorFactory()

        MentorshipFactory(mentor=mentor, status=Mentorship.Status.CREATED)
        MentorshipFactory(mentor=mentor, status=Mentorship.Status.ACCEPTED)
        MentorshipFactory(mentor=mentor, status=Mentorship.Status.PAUSED)
        MentorshipFactory(mentor=mentor, status=Mentorship.Status.COMPLETED)
        MentorshipFactory(mentor=mentor, status=Mentorship.Status.DECLINED)

        with self.assertNumQueries(2):
            update_mentor_mentees_count(pk=mentor.id)

        mentor.refresh_from_db()

        self.assertEqual(mentor.mentees_count, 3)


class UpdateMentorFeedbackCountTestCase(TransactionTestCase):
    def test_correct_feedback_count(self) -> None:
        mentor = MentorFactory()

        MentorFeedbackFactory(
            mentor=mentor, is_visible=True, can_publish=True, is_published=True
        )
        MentorFeedbackFactory(
            mentor=mentor, is_visible=False, can_publish=True, is_published=True
        )
        MentorFeedbackFactory(
            mentor=mentor, is_visible=True, can_publish=False, is_published=True
        )
        MentorFeedbackFactory(
            mentor=mentor, is_visible=True, can_publish=True, is_published=False
        )

        with self.assertNumQueries(2):
            update_mentor_feedback_count(pk=mentor.id)

        mentor.refresh_from_db()

        self.assertEqual(mentor.feedback_count, 1)


class HasActiveMentorshipTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.mentee = UserFactory()

        self.client.force_login(self.mentee)

    def test_has_created_mentorship(self):
        mentor = MentorFactory()
        MentorshipFactory(
            mentee=self.mentee, mentor=mentor, status=Mentorship.Status.CREATED
        )

        with self.assertNumQueries(1):
            result = has_active_mentorship(
                mentee_id=self.mentee.pk, mentor_id=mentor.pk
            )

        self.assertEqual(result, True)

    def test_has_accepted_mentorship(self):
        mentor = MentorFactory()
        MentorshipFactory(
            mentee=self.mentee, mentor=mentor, status=Mentorship.Status.ACCEPTED
        )

        with self.assertNumQueries(1):
            result = has_active_mentorship(
                mentee_id=self.mentee.pk, mentor_id=mentor.pk
            )

        self.assertEqual(result, True)

    def test_has_paused_mentorship(self):
        mentor = MentorFactory()
        MentorshipFactory(
            mentee=self.mentee, mentor=mentor, status=Mentorship.Status.ACCEPTED
        )

        with self.assertNumQueries(1):
            result = has_active_mentorship(
                mentee_id=self.mentee.pk, mentor_id=mentor.pk
            )

        self.assertEqual(result, True)

    def test_has_no_active_mentorships(self):
        mentor = MentorFactory()

        with self.assertNumQueries(1):
            result = has_active_mentorship(
                mentee_id=self.mentee.pk, mentor_id=mentor.pk
            )

        self.assertEqual(result, False)
