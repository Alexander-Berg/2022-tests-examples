from django.contrib.auth import get_user_model
from django.test import TransactionTestCase

from mentor.core.validators import validate_datetime
from mentor.users.tests.factories import UserFactory

from ..models import MentorFeedback, Mentorship
from .factories import MentorFeedbackFactory, MentorshipFactory

User = get_user_model()


class CreateMentorFeedbackTestCase(TransactionTestCase):
    url = "/api/v1/mentor_feedback/"

    def build_payload(self, mentorship_id: int, **kwargs) -> dict:
        payload = {
            "mentorship_id": mentorship_id,
            "comments": "You were great!",
            "is_visible": False,
            "can_publish": False,
        }
        payload.update(**kwargs)

        return payload

    def build_expected(self, mentor_feedback: MentorFeedback) -> dict:
        return {
            "id": mentor_feedback.id,
            "mentor_id": mentor_feedback.mentor_id,
            "mentorship_id": mentor_feedback.mentorship_id,
            "comments": mentor_feedback.comments,
            "is_visible": mentor_feedback.is_visible,
            "can_publish": mentor_feedback.can_publish,
            "is_published": mentor_feedback.is_published,
            "created": validate_datetime(mentor_feedback.created),
            "modified": validate_datetime(mentor_feedback.modified),
        }

    def test_unrelated_user(self):
        mentorship = MentorshipFactory()
        unrelated_user = UserFactory()

        self.client.force_login(unrelated_user)
        with self.assertNumQueries(3):
            response = self.client.post(
                self.url,
                data=self.build_payload(mentorship.id),
                content_type="application/json",
            )

        self.assertEqual(response.status_code, 404)

    def test_not_completed_mentorship(self):
        mentorship = MentorshipFactory(status=Mentorship.Status.CREATED)

        self.client.force_login(mentorship.mentee)
        with self.assertNumQueries(3):
            response = self.client.post(
                self.url,
                data=self.build_payload(mentorship.id),
                content_type="application/json",
            )

        self.assertEqual(response.status_code, 400)

    def test_already_existed_mentor_feedback(self):
        mentorship = MentorshipFactory(status=Mentorship.Status.COMPLETED)
        MentorFeedbackFactory(mentorship=mentorship, mentor=mentorship.mentor)

        self.client.force_login(mentorship.mentee)
        with self.assertNumQueries(4):
            response = self.client.post(
                self.url,
                data=self.build_payload(mentorship.id),
                content_type="application/json",
            )

        self.assertEqual(response.status_code, 400)

    def test_create(self):
        mentorship = MentorshipFactory(status=Mentorship.Status.COMPLETED)

        self.client.force_login(mentorship.mentee)
        payload = self.build_payload(mentorship.id)
        with self.assertNumQueries(7):
            response = self.client.post(
                self.url,
                data=payload,
                content_type="application/json",
            )

        mentor_feedback = mentorship.mentorfeedback.first()
        expected = self.build_expected(mentor_feedback)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)

    def test_create_visible(self):
        mentorship = MentorshipFactory(status=Mentorship.Status.COMPLETED)

        self.client.force_login(mentorship.mentee)
        payload = self.build_payload(mentorship.id, visible=True)
        with self.assertNumQueries(7):
            response = self.client.post(
                self.url,
                data=payload,
                content_type="application/json",
            )

        mentor_feedback = mentorship.mentorfeedback.first()
        expected = self.build_expected(mentor_feedback)

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)


class PublishMentorFeedback(TransactionTestCase):
    def build_url(self, mentor_feedback_id):
        return "/api/v1/mentor_feedback/{mentor_feedback_id}/publish/".format(
            mentor_feedback_id=mentor_feedback_id
        )

    def test_unrelated_user(self):
        mentor_feedback = MentorFeedbackFactory()
        unrelated_user = UserFactory()

        self.client.force_login(unrelated_user)
        with self.assertNumQueries(3):
            response = self.client.put(self.build_url(mentor_feedback.id))

        self.assertEqual(response.status_code, 404)

    def test_forbidden_to_publish(self):
        mentor_feedback = MentorFeedbackFactory(can_publish=False)

        self.client.force_login(mentor_feedback.mentor.user)
        with self.assertNumQueries(3):
            response = self.client.put(self.build_url(mentor_feedback.id))

        self.assertEqual(response.status_code, 403)

    def test_publish(self):
        mentor_feedback = MentorFeedbackFactory(can_publish=True, is_published=False)

        self.client.force_login(mentor_feedback.mentor.user)
        with self.assertNumQueries(6):
            response = self.client.put(self.build_url(mentor_feedback.id))

        mentor_feedback.refresh_from_db()
        expected = {
            "id": mentor_feedback.id,
            "mentor_id": mentor_feedback.mentor_id,
            "mentorship_id": mentor_feedback.mentorship_id,
            "comments": mentor_feedback.comments,
            "is_visible": mentor_feedback.is_visible,
            "can_publish": True,
            "is_published": True,
            "created": validate_datetime(mentor_feedback.created),
            "modified": validate_datetime(mentor_feedback.modified),
        }

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)
        self.assertTrue(mentor_feedback.is_published)


class UnpublishMentorFeedback(TransactionTestCase):
    def build_url(self, mentor_feedback_id):
        return "/api/v1/mentor_feedback/{mentor_feedback_id}/unpublish/".format(
            mentor_feedback_id=mentor_feedback_id
        )

    def test_unrelated_user(self):
        mentor_feedback = MentorFeedbackFactory()
        unrelated_user = UserFactory()

        self.client.force_login(unrelated_user)
        with self.assertNumQueries(3):
            response = self.client.put(self.build_url(mentor_feedback.id))

        self.assertEqual(response.status_code, 404)

    def test_unpublish(self):
        mentor_feedback = MentorFeedbackFactory(is_published=True)

        self.client.force_login(mentor_feedback.mentor.user)
        with self.assertNumQueries(6):
            response = self.client.put(self.build_url(mentor_feedback.id))

        mentor_feedback.refresh_from_db()
        expected = {
            "id": mentor_feedback.id,
            "mentor_id": mentor_feedback.mentor_id,
            "mentorship_id": mentor_feedback.mentorship_id,
            "comments": mentor_feedback.comments,
            "is_visible": mentor_feedback.is_visible,
            "can_publish": mentor_feedback.can_publish,
            "is_published": False,
            "created": validate_datetime(mentor_feedback.created),
            "modified": validate_datetime(mentor_feedback.modified),
        }

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)
        self.assertFalse(mentor_feedback.is_published)
