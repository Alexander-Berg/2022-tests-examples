from django.contrib.auth import get_user_model
from django.test import TransactionTestCase

from mentor.core.validators import validate_datetime
from mentor.users.tests.factories import UserFactory

from ..models import Mentorship
from .factories import MenteeFeedbackFactory, MentorshipFactory

User = get_user_model()


class CreateMenteeFeedbackTestCase(TransactionTestCase):
    url = "/api/v1/mentee_feedback/"

    def build_payload(self, mentorship_id: int):
        return {
            "mentorship_id": mentorship_id,
            "comments": "You were great!",
            "is_visible": False,
            "can_publish": False,
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

        self.client.force_login(mentorship.mentor.user)
        with self.assertNumQueries(3):
            response = self.client.post(
                self.url,
                data=self.build_payload(mentorship.id),
                content_type="application/json",
            )

        self.assertEqual(response.status_code, 400)

    def test_already_existed_mentee_feedback(self):
        mentorship = MentorshipFactory(status=Mentorship.Status.COMPLETED)
        MenteeFeedbackFactory(mentorship=mentorship, mentee=mentorship.mentee)

        self.client.force_login(mentorship.mentor.user)
        with self.assertNumQueries(4):
            response = self.client.post(
                self.url,
                data=self.build_payload(mentorship.id),
                content_type="application/json",
            )

        self.assertEqual(response.status_code, 400)

    def test_create(self):
        mentorship = MentorshipFactory(status=Mentorship.Status.COMPLETED)

        self.client.force_login(mentorship.mentor.user)
        payload = self.build_payload(mentorship.id)
        with self.assertNumQueries(5):
            response = self.client.post(
                self.url,
                data=payload,
                content_type="application/json",
            )

        mentee_feedback = mentorship.menteefeedback.first()
        expected = {
            "id": mentee_feedback.id,
            "mentee_id": mentee_feedback.mentee_id,
            "mentorship_id": mentee_feedback.mentorship_id,
            "comments": mentee_feedback.comments,
            "is_visible": mentee_feedback.is_visible,
            "can_publish": mentee_feedback.can_publish,
            "is_published": mentee_feedback.is_published,
            "created": validate_datetime(mentee_feedback.created),
            "modified": validate_datetime(mentee_feedback.modified),
        }

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)


class PublishMenteeFeedback(TransactionTestCase):
    def build_url(self, mentee_feedback_id):
        return "/api/v1/mentee_feedback/{mentee_feedback_id}/publish/".format(
            mentee_feedback_id=mentee_feedback_id
        )

    def test_unrelated_user(self):
        mentee_feedback = MenteeFeedbackFactory()
        unrelated_user = UserFactory()

        self.client.force_login(unrelated_user)
        with self.assertNumQueries(3):
            response = self.client.put(self.build_url(mentee_feedback.id))

        self.assertEqual(response.status_code, 404)

    def test_forbidden_to_publish(self):
        mentee_feedback = MenteeFeedbackFactory(can_publish=False)

        self.client.force_login(mentee_feedback.mentee)
        with self.assertNumQueries(3):
            response = self.client.put(self.build_url(mentee_feedback.id))

        self.assertEqual(response.status_code, 403)

    def test_publish(self):
        mentee_feedback = MenteeFeedbackFactory(can_publish=True, is_published=False)

        self.client.force_login(mentee_feedback.mentee)
        with self.assertNumQueries(4):
            response = self.client.put(self.build_url(mentee_feedback.id))

        mentee_feedback.refresh_from_db()
        expected = {
            "id": mentee_feedback.id,
            "mentee_id": mentee_feedback.mentee_id,
            "mentorship_id": mentee_feedback.mentorship_id,
            "comments": mentee_feedback.comments,
            "is_visible": mentee_feedback.is_visible,
            "can_publish": True,
            "is_published": True,
            "created": validate_datetime(mentee_feedback.created),
            "modified": validate_datetime(mentee_feedback.modified),
        }

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)
        self.assertTrue(mentee_feedback.is_published)


class UnpublishMenteeFeedback(TransactionTestCase):
    def build_url(self, mentee_feedback_id):
        return "/api/v1/mentee_feedback/{mentee_feedback_id}/unpublish/".format(
            mentee_feedback_id=mentee_feedback_id
        )

    def test_unrelated_user(self):
        mentee_feedback = MenteeFeedbackFactory()
        unrelated_user = UserFactory()

        self.client.force_login(unrelated_user)
        with self.assertNumQueries(3):
            response = self.client.put(self.build_url(mentee_feedback.id))

        self.assertEqual(response.status_code, 404)

    def test_unpublish(self):
        mentee_feedback = MenteeFeedbackFactory(is_published=True)

        self.client.force_login(mentee_feedback.mentee)
        with self.assertNumQueries(4):
            response = self.client.put(self.build_url(mentee_feedback.id))

        mentee_feedback.refresh_from_db()
        expected = {
            "id": mentee_feedback.id,
            "mentee_id": mentee_feedback.mentee_id,
            "mentorship_id": mentee_feedback.mentorship_id,
            "comments": mentee_feedback.comments,
            "is_visible": mentee_feedback.is_visible,
            "can_publish": mentee_feedback.can_publish,
            "is_published": False,
            "created": validate_datetime(mentee_feedback.created),
            "modified": validate_datetime(mentee_feedback.modified),
        }

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), expected)
        self.assertFalse(mentee_feedback.is_published)
