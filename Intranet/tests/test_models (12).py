import faker

from django.test import TransactionTestCase

from lms.classrooms.models import StudentSlot
from lms.classrooms.tests.factories import ClassroomFactory, StudentSlotFactory, TimeslotFactory
from lms.courses.models import CourseStudent
from lms.courses.tests.factories import CourseFactory, CourseStudentFactory
from lms.enrollments.models import EnrolledUser
from lms.enrollments.tests.factories import EnrolledUserFactory, TrackerEnrollmentFactory

from ..models import TrackerHook, TrackerHookEvent, TrackerIssue
from .factories import (
    EnrolledUserTrackerIssueFactory, EnrollmentTrackerIssueFactory, StudentSlotTrackerIssueFactory,
    TrackerHookEventFactory, TrackerHookFactory,
)

fake = faker.Faker()


class TrackerHookEventModelTestCase(TransactionTestCase):
    def test_request_hook_not_in_pending(self):
        with self.assertNumQueries(1):
            event = TrackerHookEventFactory(status=TrackerHookEvent.STATUS_SUCCESS)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_SUCCESS)

    def test_request_body_not_dict(self):
        request_body = []
        with self.assertNumQueries(5):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_ERROR)
        self.assertEqual(event.process_status_result, 'Error: request_body is not a dict')

    def test_no_issue_key(self):
        request_body = {}
        with self.assertNumQueries(5):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_ERROR)
        self.assertEqual(event.process_status_result, 'Error: No parameter "key"')

    def test_cannot_parse_issue_key(self):
        invalid_issue_key = fake.word()
        request_body = {'key': invalid_issue_key}
        with self.assertNumQueries(5):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_ERROR)
        self.assertEqual(event.process_status_result, f'Error: Cannot parse issue key "{invalid_issue_key}"')

    def test_issue_id_not_int(self):
        queue_name = fake.word()
        invalid_issue_id = fake.word()
        request_body = {'key': f'{queue_name}-{invalid_issue_id}'}
        with self.assertNumQueries(5):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_ERROR)
        self.assertEqual(event.process_status_result, f'Error: Cannot convert issue id "{invalid_issue_id}" to int')

    def test_issue_not_found(self):
        queue_name = fake.word()
        issue_id = fake.pyint(min_value=1, max_value=100)
        request_body = {'key': f'{queue_name}-{issue_id}'}
        with self.assertNumQueries(6):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_ERROR)
        self.assertEqual(event.process_status_result, f'Error: Issue "{queue_name}-{issue_id}" not found')

    def test_queue_not_default(self):
        enrollment = TrackerEnrollmentFactory()
        issue = EnrollmentTrackerIssueFactory(
            enrolled_user__course=enrollment.course,
            enrolled_user__enrollment=enrollment,
        )
        request_body = {'key': issue.issue}
        with self.assertNumQueries(6):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_SUCCESS)
        self.assertEqual(event.process_status_result, 'Success: Queue is not default')

    def test_unknown_action(self):
        enrollment = TrackerEnrollmentFactory()
        issue = EnrollmentTrackerIssueFactory(
            enrolled_user__course=enrollment.course,
            enrolled_user__enrollment=enrollment,
            queue__is_default=True,
        )
        invalid_action = fake.word()
        request_body = {'key': issue.issue, 'action': invalid_action}
        with self.assertNumQueries(6):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_ERROR)
        self.assertEqual(event.process_status_result, f'Error: Unknown action "{invalid_action}"')

    def test_user_enrolled(self):
        enrollment = TrackerEnrollmentFactory()
        enrolled_user = EnrolledUserFactory(course=enrollment.course, enrollment=enrollment)
        issue = EnrollmentTrackerIssueFactory(enrolled_user=enrolled_user, queue__is_default=True)
        hook_action = 'user_enrolled'
        request_body = {'key': issue.issue, 'action': hook_action, 'status': 'traker_status'}
        with self.assertNumQueries(31):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_SUCCESS)
        self.assertEqual(event.process_status_result, 'Success: Processed')

        issue.refresh_from_db()
        self.assertEqual(issue.status, 'traker_status')
        self.assertTrue(issue.status_processed)

        enrolled_user.refresh_from_db()
        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.ENROLLED)
        self.assertEqual(enrolled_user.history.first().history_change_reason, f'подтверждено в трекере ({hook_action})')
        course_student = enrolled_user.course_student
        self.assertEqual(course_student.course, enrolled_user.course)
        self.assertEqual(course_student.group, enrolled_user.group)
        self.assertEqual(course_student.user, enrolled_user.user)
        self.assertEqual(course_student.status, CourseStudent.StatusChoices.ACTIVE)
        self.assertEqual(
            course_student.history.first().history_change_reason,
            'cоздано при подтверждении заявки',
        )

    def test_user_rejected(self):
        enrollment = TrackerEnrollmentFactory()
        enrolled_user = EnrolledUserFactory(course=enrollment.course, enrollment=enrollment)
        issue = EnrollmentTrackerIssueFactory(enrolled_user=enrolled_user, queue__is_default=True)
        hook_action = 'user_rejected'
        request_body = {'key': issue.issue, 'action': hook_action, 'status': 'traker_status'}
        with self.assertNumQueries(22):
            event = TrackerHookEventFactory(request_body=request_body)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHookEvent.STATUS_SUCCESS)
        self.assertEqual(event.process_status_result, 'Success: Processed')

        issue.refresh_from_db()
        self.assertEqual(issue.status, 'traker_status')
        self.assertTrue(issue.status_processed)

        enrolled_user.refresh_from_db()
        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)

        self.assertEqual(enrolled_user.history.first().history_change_reason, f'отклонено в трекере ({hook_action})')
        self.assertIsNone(enrolled_user.course_student)


class TrackerHookModelErrorsTestCase(TransactionTestCase):
    def test_request_hook_not_in_pending(self):
        with self.assertNumQueries(1):
            event = TrackerHookFactory(status=TrackerHook.Status.SUCCESS)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.SUCCESS)

    def test_request_body_not_dict(self):
        data = []
        with self.assertNumQueries(3):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.ERROR)
        self.assertIn('Некорректный формат data', event.result)

    def test_no_issue_key(self):
        data = {}
        with self.assertNumQueries(3):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.ERROR)
        self.assertIn('Ключ тикета не найден', event.result)

    def test_issue_not_found(self):
        queue_name = fake.word()
        issue_id = fake.pyint(min_value=1, max_value=100)
        data = {'key': f'{queue_name}-{issue_id}'}
        with self.assertNumQueries(7):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.ERROR)
        self.assertIn(f"Тикет {queue_name}-{issue_id} не найден", event.result)


class TrackerHookForEnrollmentModelTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.enrollment = TrackerEnrollmentFactory()

    def test_unknown_action(self):
        enrolled_user = EnrolledUserFactory(enrollment=self.enrollment, course=self.enrollment.course)
        issue = EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user)

        invalid_action = fake.word()
        data = {'key': issue.issue, 'action': invalid_action}
        with self.assertNumQueries(9):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.ERROR)
        self.assertIn(f'Неизвестный action "{invalid_action}" для EnrolledUser {enrolled_user.id}', event.result)

    def test_user_enrolled(self):
        enrolled_user = EnrolledUserFactory(course=self.enrollment.course, enrollment=self.enrollment)
        issue = EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user)
        hook_action = 'user_enrolled'
        data = {'key': issue.issue, 'action': hook_action, 'status': 'traker_status'}
        with self.assertNumQueries(28):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.SUCCESS)
        self.assertEqual(event.result, '')

        issue.refresh_from_db()
        self.assertEqual(issue.issue_status, 'traker_status')
        self.assertEqual(issue.status, TrackerIssue.Status.SUCCESS)

        enrolled_user.refresh_from_db()
        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.ENROLLED)
        self.assertEqual(enrolled_user.history.first().history_change_reason, f'подтверждено в трекере ({hook_action})')

        course_student = enrolled_user.course_student
        self.assertEqual(course_student.course, enrolled_user.course)
        self.assertEqual(course_student.group, enrolled_user.group)
        self.assertEqual(course_student.user, enrolled_user.user)
        self.assertEqual(course_student.status, CourseStudent.StatusChoices.ACTIVE)
        self.assertEqual(
            course_student.history.first().history_change_reason,
            'cоздано при подтверждении заявки',
        )

    def test_user_rejected(self):
        enrolled_user = EnrolledUserFactory(course=self.enrollment.course, enrollment=self.enrollment)
        issue = EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user)
        hook_action = 'user_rejected'
        data = {'key': issue.issue, 'action': hook_action, 'status': 'traker_status'}
        with self.assertNumQueries(19):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.SUCCESS)
        self.assertEqual(event.result, '')

        issue.refresh_from_db()
        self.assertEqual(issue.issue_status, 'traker_status')
        self.assertEqual(issue.status, TrackerHook.Status.SUCCESS)

        enrolled_user.refresh_from_db()
        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.REJECTED)

        self.assertEqual(enrolled_user.history.first().history_change_reason, f'отклонено в трекере ({hook_action})')
        self.assertIsNone(enrolled_user.course_student)

    def test_user_completed(self):
        enrolled_user = EnrolledUserFactory(
            course=self.enrollment.course, enrollment=self.enrollment, status=EnrolledUser.StatusChoices.ENROLLED,
        )
        issue = EnrolledUserTrackerIssueFactory(enrolled_user=enrolled_user)
        hook_action = 'user_completed'
        data = {'key': issue.issue, 'action': hook_action, 'status': 'traker_status'}
        with self.assertNumQueries(24):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.SUCCESS)
        self.assertEqual(event.result, '')

        issue.refresh_from_db()
        self.assertEqual(issue.issue_status, 'traker_status')
        self.assertEqual(issue.status, TrackerHook.Status.SUCCESS)

        enrolled_user.refresh_from_db()
        self.assertEqual(enrolled_user.status, EnrolledUser.StatusChoices.COMPLETED)
        self.assertEqual(enrolled_user.history.first().history_change_reason, f'завершено в трекере ({hook_action})')

        course_student = enrolled_user.course_student
        self.assertEqual(course_student.course, enrolled_user.course)
        self.assertEqual(course_student.group, enrolled_user.group)
        self.assertEqual(course_student.user, enrolled_user.user)
        self.assertEqual(course_student.status, CourseStudent.StatusChoices.COMPLETED)
        self.assertEqual(
            course_student.history.first().history_change_reason,
            'завершение обучения при завершении заявки',
        )


class TrackerHookForTimeslotModelTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.course = CourseFactory(is_active=True)
        self.classroom = ClassroomFactory(course=self.course)
        self.timeslot = TimeslotFactory(classroom=self.classroom)
        self.student = CourseStudentFactory(course=self.course)

    def test_unknown_action(self):
        student_slot = StudentSlotFactory(timeslot=self.timeslot, student=self.student)
        issue = StudentSlotTrackerIssueFactory(slot=student_slot)

        invalid_action = fake.word()
        data = {'key': issue.issue, 'action': invalid_action}
        with self.assertNumQueries(9):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.ERROR)
        self.assertIn(f'Неизвестный action "{invalid_action}" для StudentSlot {student_slot.id}', event.result)

    def test_student_slot_rejected(self):
        student_slot = StudentSlotFactory(
            timeslot=self.timeslot, student=self.student, status=StudentSlot.StatusChoices.ACCEPTED,
        )
        issue = StudentSlotTrackerIssueFactory(slot=student_slot)

        hook_action = 'student_slot_reject'
        data = {'key': issue.issue, 'action': hook_action, 'status': 'traker_status'}
        with self.assertNumQueries(15):
            event = TrackerHookFactory(data=data)

        event.refresh_from_db()
        self.assertEqual(event.status, TrackerHook.Status.SUCCESS)
        self.assertEqual(event.result, '')

        issue.refresh_from_db()
        self.assertEqual(issue.issue_status, 'traker_status')
        self.assertEqual(issue.status, TrackerIssue.Status.SUCCESS)

        student_slot.refresh_from_db()
        self.assertEqual(student_slot.status, StudentSlot.StatusChoices.REJECTED)
        self.assertEqual(student_slot.history.first().history_change_reason, 'Отклонено в Трекере')
