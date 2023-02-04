import html
from unittest.mock import MagicMock, patch

from django.test import TestCase, TransactionTestCase

from lms.courses.tests.factories import CourseCategoryFactory, CourseFactory
from lms.enrollments.models import EnrolledUser
from lms.enrollments.tests.factories import TrackerEnrollmentFactory
from lms.staff.tests.factories import UserWithStaffProfileFactory
from lms.users.tests.factories import UserFactory

from ..models import EnrollmentTrackerIssue
from ..tickets import BaseTicket, EnrollmentFixedTicket, TicketTemplateMixin
from .factories import (
    EnrolledUserFactory, EnrollmentTrackerQueueFactory, TicketFieldTemplateFactory, TicketTemplateFactory,
    TrackerQueueFactory,
)


class FakeTicket(TicketTemplateMixin, BaseTicket):
    def __init__(self, queue, context):
        self._context = context
        super().__init__(queue)

    def get_context_data(self) -> dict:
        return self._context


class TicketTemplateTestCase(TestCase):
    def setUp(self) -> None:
        self.user = UserFactory.build()
        self.course = CourseFactory.build(is_active=True)

    def test_as_dict(self):
        summary = "{{ user.username }}"
        description = "{{ course.name }}"
        template = TicketTemplateFactory(summary=summary, description=description)
        TicketFieldTemplateFactory(
            template=template,
            field_name='city',
            value="{{ course.city.name }}",
        )
        TicketFieldTemplateFactory(
            template=template,
            field_name='first_name',
            value="{{ user.first_name }}",
        )
        TicketFieldTemplateFactory(
            template=template,
            field_name='last_name',
            value="{{ user.last_name }}",
        )

        queue = TrackerQueueFactory.build(name='TESTQUEUE', template=template)
        context = {
            'user': self.user,
            'course': self.course,
        }

        ticket = FakeTicket(queue=queue, context=context)
        expected = {
            'queue': queue.name,
            'summary': self.user.username,
            'description': self.course.name,
            'city': self.course.city.name,
        }
        self.assertEqual(ticket.as_dict(), expected)

    def test_field_allow_null(self):
        summary = "{{ course.name }}"
        description = "{{ course.description }}"
        template = TicketTemplateFactory(summary=summary, description=description)
        TicketFieldTemplateFactory(
            template=template,
            field_name='null_allowed',
            value="",
            allow_null=True,
        )
        TicketFieldTemplateFactory(
            template=template,
            field_name='not_null_field',
            value="",
        )

        queue = TrackerQueueFactory.build(name='TESTQUEUE', template=template)
        context = {
            'course': self.course,
        }
        ticket = FakeTicket(queue=queue, context=context)
        expected = {
            'queue': queue.name,
            'summary': self.course.name,
            'description': self.course.description,
            'null_allowed': '',
        }
        self.assertEqual(ticket.as_dict(), expected)

    def test_field_allow_json(self):
        summary = "{{ course.name }}"
        description = "{{ course.description }}"
        template = TicketTemplateFactory(summary=summary, description=description)
        TicketFieldTemplateFactory(
            template=template,
            field_name='json_field',
            value="{{ json_data|json }}",
            allow_json=True,
        )
        TicketFieldTemplateFactory(
            template=template,
            field_name='invalid_json_field',
            value="{{ json_data }}",
            allow_json=True,
        )
        TicketFieldTemplateFactory(
            template=template,
            field_name='invalid_json_field_null',
            value="{{ json_data }}",
            allow_json=True,
            allow_null=True,
        )
        TicketFieldTemplateFactory(
            template=template,
            field_name='json_field_str',
            value="{{ json_data }}",
        )

        queue = TrackerQueueFactory.build(name='TESTQUEUE', template=template)
        json_data = {
            "name1": {"issue": "EDU-XXXX"},
            "name2": ["one", "two", "three"],
        }
        context = {
            'course': self.course,
            'json_data': json_data,
        }
        ticket = FakeTicket(queue=queue, context=context)
        expected = {
            'queue': queue.name,
            'summary': self.course.name,
            'description': self.course.description,
            'json_field': json_data,
            'invalid_json_field_null': None,
            'json_field_str': html.escape(str(json_data)),
        }
        self.assertEqual(ticket.as_dict(), expected)


class EnrollmentTicketTestCase(TransactionTestCase):
    def setUp(self) -> None:
        self.TEST_QUEUE = 'LMSTEST'

        self.user = UserWithStaffProfileFactory()
        self.staff_profile = self.user.staffprofile

        self.categories = CourseCategoryFactory.create_batch(5)
        self.course = CourseFactory(is_active=True)
        self.course.categories.add(*self.categories[:2])
        self.enrollment = TrackerEnrollmentFactory(course=self.course)
        self.queue = EnrollmentTrackerQueueFactory(
            name=self.TEST_QUEUE,
            enrollment=self.enrollment,
            is_default=True,
        )

    def build_mocked_issue(self) -> MagicMock:
        mocked = MagicMock()
        create = MagicMock()
        create.key = f"{self.TEST_QUEUE}-42"
        create.status.key = 'opened'
        mocked.create.return_value = create
        mocked.find.return_value = []
        return mocked

    def test_ticket_dict(self):
        mocked_issue = self.build_mocked_issue()

        with patch('lms.tracker.services.startrek_api.issues', new=mocked_issue):
            enrolled_user: EnrolledUser = EnrolledUserFactory(
                course=self.course,
                enrollment=self.enrollment,
                user=self.user,
            )

        start_date = None
        end_date = None

        hr_partners = list(self.staff_profile.hr_partners.values_list('user__username', flat=True))

        with self.assertNumQueries(6):
            context_data = EnrollmentFixedTicket(enrolled_user=enrolled_user, queue=self.queue).as_dict()
        context_data['hrbp'] = list(context_data['hrbp'])

        expected = {
            'type': {'name': self.queue.issue_type},
            'employee': self.user.username,
            'department': enrolled_user.groups,
            'runId': enrolled_user.id,
            'graphTaskId': self.course.id,
            'office': str(self.staff_profile.office),
            'head': self.staff_profile.head and self.staff_profile.head.username,
            'hrbp': hr_partners,
            'edPlace': self.staff_profile.city.name_ru,
            'edGroup': enrolled_user.group_id,
            'outputGroup': enrolled_user.group_id,
            'company': self.staff_profile.organization and self.staff_profile.organization.name_ru,
            'programName': self.course.name,
            'eventCost': str(self.course.price),
            'employmentForm': self.course.study_mode_id and self.course.study_mode.name,
            'format': self.course.workflow_id and self.course.workflow.name,
            'cityOfEvent': self.course.city_id and self.course.city.name,
            'provider': self.course.provider_id and self.course.provider.name,
            'start': start_date,
            'startDateAndTime1': start_date,
            'end': end_date,
            'startDateAndTime2': end_date,
            'createdBy': self.user.username,
            'directionOfTraining': None,
            'summary': self.queue.summary,
            'description': context_data['description'],
            'access': [self.user.username, 'robot-corp-education'],
            'queue': self.queue.name,
        }
        self.maxDiff = None

        self.assertEqual(context_data, expected)

        mocked_issue.assert_not_called()
        issues = EnrollmentTrackerIssue.objects.filter(enrolled_user=enrolled_user)
        self.assertEqual(issues.count(), 1)
