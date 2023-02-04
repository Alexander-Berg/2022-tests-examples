from unittest.mock import MagicMock, patch

from django.test import TestCase, TransactionTestCase

from lms.courses.models import CourseStudent
from lms.courses.tests.factories import CourseFactory, CourseGroupFactory, CourseStudentFactory
from lms.enrollments.models import EnrolledUser
from lms.enrollments.tests.factories import EnrolledUserFactory, TrackerEnrollmentFactory
from lms.staff.tests.factories import UserWithStaffProfileFactory
from lms.tracker.tests.factories import EnrollmentTrackerIssueFactory, EnrollmentTrackerQueueFactory
from lms.users.tests.factories import UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from ..models import EnrollmentTrackerIssue
from ..services import update_enrolled_user_status_by_issue, update_or_create_enrollment_ticket


class EnrollmentTrackerIssueUpdateStatusTestCase(TestCase):
    def setUp(self) -> None:
        self.course = CourseFactory(is_active=True)
        self.enrollment = TrackerEnrollmentFactory(course=self.course)
        self.user = UserFactory()

    @parameterized_expand_doc(
        [
            (EnrolledUser.StatusChoices.PENDING, 29),
            (EnrolledUser.StatusChoices.ENROLLED, 22),
        ]
    )
    def test_update_accepted_status_with_group(self, init_status: str, num_queries: int):
        group = CourseGroupFactory(course=self.course)
        enrolled_user = EnrolledUserFactory(
            user=self.user,
            enrollment=self.enrollment,
            course=self.course,
            group=group,
            status=init_status,
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=False,
            enrolled_user=enrolled_user,
            queue__is_default=True,
        )

        issue.status = issue.queue.accepted_status
        with patch('lms.tracker.services.create_tracker_enrollment_issues'):
            with self.assertNumQueries(1):
                issue.save()

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, init_status)

        with self.assertNumQueries(num_queries):
            update_enrolled_user_status_by_issue(issue=issue)

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.ENROLLED)
        self.assertIn(issue.enrolled_user.user, [s.user for s in issue.enrolled_user.group.students.all()])
        self.assertTrue(issue.status_processed)

    @parameterized_expand_doc(
        [
            (EnrolledUser.StatusChoices.PENDING, 26),
            (EnrolledUser.StatusChoices.ENROLLED, 20),
        ]
    )
    def test_update_accepted_status_no_group(self, init_status: str, num_queries: int):
        enrolled_user = EnrolledUserFactory(
            status=init_status,
            course=self.enrollment.course,
            enrollment=self.enrollment
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=False,
            enrolled_user=enrolled_user,
            queue__is_default=True,
        )

        issue.status = issue.queue.accepted_status
        with patch('lms.tracker.services.create_tracker_enrollment_issues'):
            with self.assertNumQueries(1):
                issue.save()

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, init_status)
        self.assertFalse(issue.status_processed)

        with self.assertNumQueries(num_queries):
            update_enrolled_user_status_by_issue(issue=issue)

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.ENROLLED)
        self.assertIsNone(issue.enrolled_user.group)
        self.assertTrue(issue.status_processed)

    def test_update_status_pending_to_rejected_with_group(self):
        group = CourseGroupFactory(course=self.course)
        enrolled_user = EnrolledUserFactory(
            user=self.user,
            course=self.course,
            group=group,
            status=EnrolledUser.StatusChoices.PENDING,
            enrollment=self.enrollment,
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=False,
            enrolled_user=enrolled_user,
            queue__is_default=True,
        )

        issue.status = issue.queue.rejected_status
        with patch('lms.tracker.services.create_tracker_enrollment_issues'):
            with self.assertNumQueries(1):
                issue.save()

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.PENDING)
        self.assertFalse(issue.status_processed)

        with self.assertNumQueries(22):
            update_enrolled_user_status_by_issue(issue=issue)

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)
        self.assertNotIn(issue.enrolled_user.user, issue.enrolled_user.group.students.all())
        self.assertTrue(issue.status_processed)

        course_student = CourseStudent.objects.filter(
            user=enrolled_user.user,
            course=enrolled_user.course,
            group=enrolled_user.group,
        ).first()
        self.assertIsNone(course_student)

    def test_update_rejected_status_with_group(self):
        group = CourseGroupFactory(course=self.course)
        enrolled_user = EnrolledUserFactory(
            user=self.user,
            course=self.course,
            group=group,
            status=EnrolledUser.StatusChoices.REJECTED,
            enrollment=self.enrollment,
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=False,
            enrolled_user=enrolled_user,
            queue__is_default=True,
        )

        issue.status = issue.queue.rejected_status
        with patch('lms.tracker.services.create_tracker_enrollment_issues'):
            with self.assertNumQueries(1):
                issue.save()

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)
        self.assertFalse(issue.status_processed)

        with self.assertNumQueries(17):
            update_enrolled_user_status_by_issue(issue=issue)

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)

        self.assertNotIn(issue.enrolled_user.user, issue.enrolled_user.group.students.all())
        self.assertTrue(issue.status_processed)

        course_student = CourseStudent.objects.filter(
            user=enrolled_user.user,
            course=enrolled_user.course,
            group=enrolled_user.group,
        ).first()
        self.assertIsNone(course_student)

    def test_update_rejected_status_with_group_with_student(self):
        group = CourseGroupFactory(course=self.course)
        enrolled_user = EnrolledUserFactory(
            user=self.user,
            course=self.course,
            group=group,
            status=EnrolledUser.StatusChoices.REJECTED,
            enrollment=self.enrollment,
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=False,
            enrolled_user=enrolled_user,
            queue__is_default=True,
        )
        course_student = CourseStudentFactory(
            user=enrolled_user.user,
            course=enrolled_user.course,
            group=enrolled_user.group,
            status=CourseStudent.StatusChoices.EXPELLED,
        )

        issue.status = issue.queue.rejected_status
        with patch('lms.tracker.services.create_tracker_enrollment_issues'):
            with self.assertNumQueries(1):
                issue.save()

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)
        self.assertFalse(issue.status_processed)

        with self.assertNumQueries(17):
            update_enrolled_user_status_by_issue(issue=issue)

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)

        self.assertNotIn(issue.enrolled_user.user, issue.enrolled_user.group.students.all())
        self.assertTrue(issue.status_processed)

        course_student.refresh_from_db()
        self.assertEqual(course_student.status, CourseStudent.StatusChoices.EXPELLED)

    @parameterized_expand_doc(
        [
            (EnrolledUser.StatusChoices.PENDING, 19),
            (EnrolledUser.StatusChoices.REJECTED, 15),
        ]
    )
    def test_update_rejected_status_no_group(self, init_status: str, num_queries: int):
        enrolled_user = EnrolledUserFactory(
            status=init_status,
            course=self.enrollment.course,
            enrollment=self.enrollment,
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=False,
            enrolled_user=enrolled_user,
            queue__is_default=True,
        )

        issue.status = issue.queue.rejected_status
        with patch('lms.tracker.services.create_tracker_enrollment_issues'):
            with self.assertNumQueries(1):
                issue.save()

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, init_status)
        self.assertFalse(issue.status_processed)

        with self.assertNumQueries(num_queries):
            update_enrolled_user_status_by_issue(issue=issue)

        issue.refresh_from_db()
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)
        self.assertTrue(issue.status_processed)

    def test_update_status_already_processed(self):
        enrolled_user = EnrolledUserFactory(
            status=EnrolledUser.StatusChoices.ENROLLED,
            course=self.enrollment.course,
            enrollment=self.enrollment,
        )
        issue = EnrollmentTrackerIssueFactory(
            status_processed=True,
            enrolled_user=enrolled_user,
        )

        issue.status = issue.queue.accepted_status
        with self.assertNumQueries(1):
            issue.save()

        issue.refresh_from_db()
        self.assertTrue(issue.status_processed)


class CreateTicketTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.user, self.head = UserWithStaffProfileFactory.create_batch(2)

        hr_partners = UserWithStaffProfileFactory.create_batch(2)
        hr_partner_profiles = [u.staffprofile for u in hr_partners]

        self.user_profile = self.user.staffprofile
        self.head_profile = self.head.staffprofile
        self.user_profile.hr_partners.add(*hr_partner_profiles)
        self.user_profile.head = self.head_profile
        self.user_profile.save()

        self.course = CourseFactory(is_active=True)
        self.group = CourseGroupFactory(course=self.course)
        self.enrollment = TrackerEnrollmentFactory(course=self.course)
        self.queue = EnrollmentTrackerQueueFactory(enrollment=self.enrollment)

    def test_create_ticket(self):
        mocked_issues = MagicMock()
        create = MagicMock()
        create.key = f'{self.queue.name}-42'
        create.status.key = 'opened'
        mocked_issues.create.return_value = create
        mocked_issues.find.return_value = []

        # with self.assertNumQueries(2):
        #     get_course_city_staff_city_map()
        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            with self.assertNumQueries(26):
                enrolled_user = EnrolledUser(
                    course=self.course,
                    enrollment=self.enrollment,
                    user=self.user,
                    group=self.group,
                )
                enrolled_user.save()

        issue = EnrollmentTrackerIssue.objects.get(
            enrolled_user=enrolled_user,
            queue=self.queue
        )
        self.assertEqual(issue.issue_number, 42)
        self.assertEqual(issue.status, 'opened')

        mocked_issues.find.assert_called_once_with(
            filter={
                'runId': enrolled_user.id,
                'queue': self.queue.name,
            }
        )

        mocked_issues.create.assert_called_once()

    def test_ticket_in_tracker_exists(self):
        mocked_issues = MagicMock()
        existed_issue = MagicMock()
        existed_issue.key = f'{self.queue.name}-42'
        existed_issue.status.key = 'opened'
        mocked_issues.find.return_value = [existed_issue]

        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            with self.assertNumQueries(19):
                enrolled_user = EnrolledUser(
                    course=self.course,
                    enrollment=self.enrollment,
                    user=self.user,
                    group=self.group,
                )
                enrolled_user.save()

        issue = EnrollmentTrackerIssue.objects.get(
            enrolled_user=enrolled_user,
            queue=self.queue
        )
        self.assertEqual(issue.issue_number, 42)
        self.assertEqual(issue.status, 'opened')

        mocked_issues.find.assert_called_once_with(
            filter={
                'runId': enrolled_user.id,
                'queue': self.queue.name,
            }
        )

    def test_ticket_in_db_exists(self):
        with patch('lms.tracker.receivers.process_tracker_enrolled_user_task'):
            enrolled_user = EnrolledUserFactory(
                course=self.course,
                enrollment=self.enrollment,
                user=self.user,
                group=self.group,
            )

        EnrollmentTrackerIssueFactory(
            enrolled_user=enrolled_user,
            queue=self.queue,
        )

        with self.assertNumQueries(1):
            update_or_create_enrollment_ticket(queue=self.queue, enrolled_user=enrolled_user)


class UpdateEnrolledUserStatusTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.course = CourseFactory(is_active=True)
        self.enrollment = TrackerEnrollmentFactory(course=self.course)

    def build_mocked_issues(self, queue):
        mocked_issues = MagicMock()
        create = MagicMock()
        create.key = f'{queue.name}-42'
        create.status.key = 'opened'
        mocked_issues.create.return_value = create
        mocked_issues.find.return_value = []

        return mocked_issues

    def test_not_default_queue(self):
        queue = EnrollmentTrackerQueueFactory(is_default=False, enrollment=self.enrollment)
        mocked_issues = self.build_mocked_issues(queue)

        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            enrolled_user = EnrolledUserFactory(
                status=EnrolledUser.StatusChoices.PENDING,
                course=self.enrollment.course,
                enrollment=self.enrollment,
            )

        issue = EnrollmentTrackerIssue.objects.get(enrolled_user=enrolled_user, queue=queue)

        with self.assertNumQueries(1):
            issue.save()

        issue.refresh_from_db()
        self.assertFalse(issue.status_processed)
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.PENDING)

    def test_status_not_default_queue_accepted(self):
        queue = EnrollmentTrackerQueueFactory(is_default=False, enrollment=self.enrollment)
        mocked_issues = self.build_mocked_issues(queue)

        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            enrolled_user = EnrolledUserFactory(
                status=EnrolledUser.StatusChoices.PENDING,
                course=self.enrollment.course,
                enrollment=self.enrollment,
            )

        issue = EnrollmentTrackerIssue.objects.get(enrolled_user=enrolled_user, queue=queue)
        issue.status = queue.accepted_status
        with self.assertNumQueries(1):
            issue.save()

        issue.refresh_from_db()
        self.assertFalse(issue.status_processed)
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.PENDING)

    def test_status_accepted_default_queue(self):
        queue = EnrollmentTrackerQueueFactory(is_default=True, enrollment=self.enrollment)
        mocked_issues = self.build_mocked_issues(queue)

        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            enrolled_user = EnrolledUserFactory(
                status=EnrolledUser.StatusChoices.PENDING,
                course=self.enrollment.course,
                enrollment=self.enrollment,
            )

        issue = EnrollmentTrackerIssue.objects.get(enrolled_user=enrolled_user, queue=queue)
        issue.status = queue.accepted_status
        with self.assertNumQueries(1):
            issue.save()

        issue.refresh_from_db()
        self.assertFalse(issue.status_processed)
        self.assertEqual(issue.enrolled_user.status, EnrolledUser.StatusChoices.PENDING)

    def test_status_already_processed(self):
        queue = EnrollmentTrackerQueueFactory(is_default=True, enrollment=self.enrollment)
        mocked_issues = self.build_mocked_issues(queue)

        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issues):
            enrolled_user = EnrolledUserFactory(
                status=EnrolledUser.StatusChoices.ENROLLED,
                course=self.enrollment.course,
                enrollment=self.enrollment,
            )

        self.assertFalse(EnrollmentTrackerIssue.objects.filter(enrolled_user=enrolled_user, queue=queue).exists())
