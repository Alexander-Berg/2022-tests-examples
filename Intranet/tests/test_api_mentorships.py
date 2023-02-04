from django.contrib.auth import get_user_model
from django.test import TransactionTestCase

from mentor.users.tests.factories import UserFactory

from ..models import Mentorship
from .factories import MentorFactory, MentorshipFactory

User = get_user_model()


class RemoveMentorshipTestCase(TransactionTestCase):
    def setUp(self):
        self.mentor = MentorFactory()
        self.user = UserFactory()

    def build_url(self, mentorship_id):
        return "/api/v1/mentorships/{mentorship_id}/".format(
            mentorship_id=mentorship_id
        )

    def test_remove_not_mine_mentorship(self):
        mentorship = MentorshipFactory()
        url = self.build_url(mentorship_id=mentorship.id)

        self.client.force_login(self.user)

        with self.assertNumQueries(3):
            response = self.client.delete(url)

        mentorship.refresh_from_db()

        self.assertEqual(response.status_code, 404)
        self.assertEqual(mentorship.removed_by_mentee, False)
        self.assertEqual(mentorship.removed_by_mentor, False)

    def test_remove_not_declined_mentorship(self):
        mentorship = MentorshipFactory(
            mentee=self.user, status=Mentorship.Status.CREATED
        )
        url = self.build_url(mentorship_id=mentorship.id)

        self.client.force_login(self.user)

        with self.assertNumQueries(3):
            response = self.client.delete(url)

        mentorship.refresh_from_db()

        self.assertEqual(response.status_code, 400)
        self.assertEqual(mentorship.removed_by_mentee, False)
        self.assertEqual(mentorship.removed_by_mentor, False)

    def test_remove_for_mentor(self):
        mentorship = MentorshipFactory(
            mentor=self.mentor, status=Mentorship.Status.DECLINED
        )
        url = self.build_url(mentorship_id=mentorship.id)

        self.client.force_login(self.mentor.user)

        with self.assertNumQueries(7):
            response = self.client.delete(url)

        mentorship.refresh_from_db()

        self.assertEqual(response.status_code, 204)
        self.assertEqual(response.content, b"")
        self.assertEqual(mentorship.removed_by_mentor, True)

    def test_remove_for_mentee(self):
        mentorship = MentorshipFactory(
            mentee=self.user, status=Mentorship.Status.DECLINED
        )
        url = self.build_url(mentorship_id=mentorship.id)

        self.client.force_login(self.user)

        with self.assertNumQueries(7):
            response = self.client.delete(url)

        mentorship.refresh_from_db()

        self.assertEqual(response.status_code, 204)
        self.assertEqual(response.content, b"")
        self.assertEqual(mentorship.removed_by_mentee, True)


class UpdateMentorshipStatusTestCase(TransactionTestCase):
    def setUp(self):
        self.mentor = MentorFactory()
        self.mentee = UserFactory()

    def build_url(self, mentorship_id):
        return "/api/v1/mentorships/{mentorship_id}/status/".format(
            mentorship_id=mentorship_id
        )

    def execute_transition_test(
        self,
        status: Mentorship.Status,
        status_by: User,
        new_status: Mentorship.Status,
        new_status_message: str,
        login_with: User,
        expected_status: int,
        expected_response: dict,
        expected_num_queries: int,
    ):
        mentorship = MentorshipFactory(
            mentor=self.mentor,
            mentee=self.mentee,
            status=status,
            status_by=status_by,
        )
        url = self.build_url(mentorship_id=mentorship.id)

        self.client.force_login(login_with)

        with self.assertNumQueries(expected_num_queries):
            response = self.client.patch(
                url,
                data={"status": new_status, "status_message": new_status_message},
                content_type="application/json",
            )

        self.assertEqual(response.status_code, expected_status)
        self.assertEqual(response.json(), expected_response)

    def execute_successful_transition_test(
        self,
        status: Mentorship.Status,
        status_by: User,
        new_status: Mentorship.Status,
        login_with: User,
        expected_num_queries: int,
    ):
        self.execute_transition_test(
            status=status,
            status_by=status_by,
            new_status=new_status,
            new_status_message="test",
            login_with=login_with,
            expected_num_queries=expected_num_queries,
            expected_status=200,
            expected_response={
                "status": new_status,
                "status_message": "test",
            },
        )

    def execute_not_allowed_transition_test(
        self,
        status: Mentorship.Status,
        status_by: User,
        new_status: Mentorship.Status,
        login_with: User,
        expected_num_queries: int,
    ):
        self.execute_transition_test(
            status=status,
            status_by=status_by,
            new_status=new_status,
            new_status_message="test",
            login_with=login_with,
            expected_num_queries=expected_num_queries,
            expected_status=403,
            expected_response={
                "detail": "У Вас нет прав для изменения статуса {status}".format(
                    status=status
                ),
            },
        )

    def execute_unacceptable_transition_test(
        self,
        status: Mentorship.Status,
        status_by: User,
        new_status: Mentorship.Status,
        login_with: User,
        expected_num_queries: int,
    ):
        self.execute_transition_test(
            status=status,
            status_by=status_by,
            new_status=new_status,
            new_status_message="test",
            login_with=login_with,
            expected_num_queries=expected_num_queries,
            expected_status=400,
            expected_response={
                "detail": "Переход из статуса {old_status} в {new_status} недопустим".format(
                    old_status=status, new_status=new_status
                ),
            },
        )

    def test_transition_by_another_user(self):
        another_user = UserFactory()

        self.execute_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            new_status_message="test",
            login_with=another_user,
            expected_num_queries=3,
            expected_status=404,
            expected_response={"detail": "Not Found"},
        )

    def test_transition_from_created_to_created(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_created_to_accepted(self):
        self.execute_successful_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentor.user,
            expected_num_queries=9,
        )
        self.execute_not_allowed_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_created_to_declined(self):
        self.execute_successful_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentor.user,
            expected_num_queries=9,
        )
        self.execute_not_allowed_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_created_to_paused(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_created_to_completed(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.CREATED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_accepted_to_created(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_accepted_to_accepted(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_accepted_to_declined(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_accepted_to_paused(self):
        self.execute_successful_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentor.user,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentor.user,
            expected_num_queries=7,
        )
        self.execute_successful_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentor.user,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentee,
            expected_num_queries=7,
        )

    def test_transition_from_accepted_to_completed(self):
        self.execute_successful_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentor.user,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentor.user,
            expected_num_queries=9,
        )
        self.execute_successful_transition_test(
            status=Mentorship.Status.ACCEPTED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentor.user,
            expected_num_queries=9,
        )

    def test_transition_from_declined_to_created(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_declined_to_accepted(self):
        self.execute_successful_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentor.user,
            expected_num_queries=9,
        )
        self.execute_not_allowed_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_declined_to_declined(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_declined_to_paused(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_declined_to_completed(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.DECLINED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_paused_to_created(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_paused_to_accepted(self):
        self.execute_successful_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentee,
            expected_num_queries=7,
        )
        self.execute_not_allowed_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentor.user,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentee,
            expected_num_queries=3,
        )
        self.execute_successful_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentor.user,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentor.user,
            expected_num_queries=9,
        )
        self.execute_not_allowed_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )

    def test_transition_from_paused_to_declined(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_paused_to_completed(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.PAUSED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_completed_to_created(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.CREATED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_completed_to_accepted(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.ACCEPTED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_completed_to_declined(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.DECLINED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_completed_to_paused(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.PAUSED,
            login_with=self.mentee,
            expected_num_queries=3,
        )

    def test_transition_from_completed_to_completed(self):
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentor.user,
            expected_num_queries=3,
        )
        self.execute_unacceptable_transition_test(
            status=Mentorship.Status.COMPLETED,
            status_by=self.mentee,
            new_status=Mentorship.Status.COMPLETED,
            login_with=self.mentee,
            expected_num_queries=3,
        )
