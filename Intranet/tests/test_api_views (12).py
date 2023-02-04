from unittest import skip

from django.conf import settings

from rest_framework import serializers
from rest_framework.test import APITestCase

from lms.classrooms.tests.factories import ClassroomFactory, StudentSlotFactory, TimeslotFactory
from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import CourseFactory, CourseStudent, CourseStudentFactory
from lms.enrollments.tests.factories import EnrolledUserFactory, TrackerEnrollmentFactory
from lms.users.tests.factories import UserFactory

from .factories import EnrollmentTrackerIssueFactory, EnrollmentTrackerQueueFactory, StudentSlotTrackerIssueFactory


@skip("Deprecated Test Case")
class MyEnrolledUserTrackerIssueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-enroll-tracker-issues'

    def setUp(self):
        self.user = UserFactory()
        self.enrollment = TrackerEnrollmentFactory()
        self.enrolled_user = EnrolledUserFactory(user=self.user, enrollment=self.enrollment)
        self.default_queue = EnrollmentTrackerQueueFactory(enrollment=self.enrollment, is_default=True)
        self.not_default_queue = EnrollmentTrackerQueueFactory(enrollment=self.enrollment, is_default=False)

    def test_url(self):
        self.assertURLNameEqual(
            'my/enroll/{pk}/tracker_issues/',
            kwargs={'pk': self.enrolled_user.id},
            base_url=settings.API_BASE_URL,
        )

    def test_list(self):
        self.issues = [EnrollmentTrackerIssueFactory(enrolled_user=self.enrolled_user, queue=self.default_queue)]
        self.issues_not_default_queue = [
            EnrollmentTrackerIssueFactory(enrolled_user=self.enrolled_user, queue=self.not_default_queue),
        ]
        self.other_enrolled_user = EnrolledUserFactory(user=self.user, enrollment=self.enrollment)
        self.issues_other_user = [
            EnrollmentTrackerIssueFactory(enrolled_user=self.other_enrolled_user, queue=self.default_queue),
            EnrollmentTrackerIssueFactory(enrolled_user=self.other_enrolled_user, queue=self.not_default_queue)
        ]
        expected = [
            {
                'id': issue.id,
                'queue_id': issue.queue_id,
                'issue_number': issue.issue_number,
                'status': issue.status,
                'got_status_from_startrek':
                    serializers.DateTimeField().to_representation(issue.got_status_from_startrek),
                'status_processed': issue.status_processed,
                'issue': issue.issue,

            } for issue in self.issues
        ]

        self.client.force_login(user=self.user)
        self.list_request(url=self.get_url(self.enrolled_user.id), expected=expected, num_queries=3, pagination=False)


class MyStudentSlotTrackerIssueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-timeslot-tracker-issues'

    def setUp(self):
        self.course = CourseFactory()
        self.classroom = ClassroomFactory(course=self.course)
        self.timeslot = TimeslotFactory(classroom=self.classroom)
        self.user = UserFactory()

    def test_url(self):
        self.assertURLNameEqual(
            'my/timeslots/{pk}/tracker_issues/',
            kwargs={'pk': self.timeslot.id},
            base_url=settings.API_BASE_URL,
        )

    def test_list(self):
        self.user_other_student = CourseStudentFactory(
            user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE,
        )
        self.other_student = CourseStudentFactory(course=self.course, status=CourseStudent.StatusChoices.ACTIVE)
        self.other_timeslot = TimeslotFactory(classroom=self.classroom)
        self.other_student_slots = [
            StudentSlotFactory(timeslot=timeslot, student=student)
            for student in [self.user_other_student, self.other_student]
            for timeslot in [self.timeslot, self.other_timeslot]
        ]
        self.other_tracker_issues = [
            StudentSlotTrackerIssueFactory(slot=student_slot)
            for student_slot in self.other_student_slots
        ]
        self.user_other_student.status = CourseStudent.StatusChoices.EXPELLED
        self.user_other_student.save()

        self.student = CourseStudentFactory(
            user=self.user, course=self.course, status=CourseStudent.StatusChoices.ACTIVE,
        )
        self.student_slot = StudentSlotFactory(timeslot=self.timeslot, student=self.student)
        self.tracker_issues = StudentSlotTrackerIssueFactory.create_batch(2, slot=self.student_slot)

        expected = [
            {
                'id': tracker_issue.id,
                'slot_id': tracker_issue.slot_id,
                'queue': {
                    'id': tracker_issue.queue_id,
                    'name': tracker_issue.queue.name,
                    'display_name': tracker_issue.queue.display_name,
                },
                'issue_number': tracker_issue.issue_number,
                'issue': tracker_issue.issue,
            } for tracker_issue in self.tracker_issues
        ]

        self.client.force_login(user=self.user)
        self.list_request(url=self.get_url(self.timeslot.id), expected=expected, num_queries=3, pagination=False)
