import warnings

import factory
import faker
from factory import lazy_attribute_sequence
from factory.django import DjangoModelFactory

from django.utils import timezone

from lms.classrooms.tests.factories import ClassroomFactory, StudentSlotFactory
from lms.enrollments.models import Enrollment
from lms.enrollments.tests.factories import EnrolledUserFactory, EnrollmentFactory

from ..models import (
    ClassroomQueue, ClassroomTracker, ClassroomTrackerQueue, EnrolledUserTrackerIssue, EnrollmentQueue,
    EnrollmentTracker, EnrollmentTrackerIssue, EnrollmentTrackerQueue, StudentSlotTrackerIssue, TicketFieldTemplate,
    TicketTemplate, TrackerHook, TrackerHookEvent, TrackerIssue, TrackerQueue,
)

fake = faker.Faker()


class EnrollmentTrackerQueueFactory(DjangoModelFactory):
    class Meta:
        model = EnrollmentTrackerQueue

    name = factory.Faker('word')
    enrollment = factory.SubFactory(
        EnrollmentFactory,
        enroll_type=Enrollment.TYPE_TRACKER,
    )
    summary = factory.Faker('sentence')
    description = factory.Faker('sentence')
    issue_type = factory.Faker('word')
    accepted_status = factory.Faker('word')
    rejected_status = factory.Faker('word')


class EnrollmentTrackerIssueFactory(DjangoModelFactory):
    class Meta:
        model = EnrollmentTrackerIssue

    enrolled_user = factory.SubFactory(EnrolledUserFactory)
    queue = factory.SubFactory(
        EnrollmentTrackerQueueFactory,
        enrollment=factory.SelfAttribute('..enrolled_user.enrollment'),
    )
    issue_number = factory.Faker('pyint')
    got_status_from_startrek = factory.Faker('date_time_this_century', tzinfo=timezone.utc)
    status = factory.Faker('word')
    status_processed = factory.Faker('boolean')


class TrackerHookEventFactory(DjangoModelFactory):
    class Meta:
        model = TrackerHookEvent


class TrackerHookFactory(DjangoModelFactory):
    class Meta:
        model = TrackerHook


# new tracker models
class TrackerQueueFactory(DjangoModelFactory):
    name = factory.LazyFunction(lambda: f'{fake.word().upper()}')
    issue_type = factory.Faker('word')

    class Meta:
        model = TrackerQueue


class TrackerIssueFactory(DjangoModelFactory):
    queue = factory.SubFactory(TrackerQueueFactory)
    issue_key = factory.LazyFunction(lambda: f"{factory.Faker('word')}-{factory.Faker('pyint')}")
    issue_status = factory.Faker('word')

    class Meta:
        model = TrackerIssue

    @lazy_attribute_sequence
    def issue_key(self, n):
        return f'{self.queue}-{n}'


class ClassroomQueueFactory(TrackerQueueFactory):
    class Meta:
        model = ClassroomQueue


class ClassroomTrackerFactory(DjangoModelFactory):
    classroom = factory.SubFactory(ClassroomFactory)
    queue = factory.SubFactory(ClassroomQueueFactory)

    class Meta:
        model = ClassroomTracker


# DEPRECATED
class ClassroomTrackerQueueFactory(DjangoModelFactory):
    classroom = factory.SubFactory(ClassroomFactory)
    queue = factory.SubFactory(TrackerQueueFactory)

    class Meta:
        model = ClassroomTrackerQueue

    def __init__(self):
        warnings.warn('`ClassroomTrackerQueueFactory` factory is deprecated', DeprecationWarning)
        super().__init__()


class StudentSlotTrackerIssueFactory(TrackerIssueFactory):
    slot = factory.SubFactory(StudentSlotFactory)

    class Meta:
        model = StudentSlotTrackerIssue


class TicketTemplateFactory(DjangoModelFactory):
    name = factory.Faker('word')
    summary = factory.Faker('sentence')
    description = factory.Faker('sentence')

    class Meta:
        model = TicketTemplate


class TicketFieldTemplateFactory(DjangoModelFactory):
    template = factory.SubFactory(TicketTemplateFactory)
    field_name = factory.Faker('word')

    class Meta:
        model = TicketFieldTemplate


class EnrolledUserTrackerIssueFactory(TrackerIssueFactory):
    enrolled_user = factory.SubFactory(EnrolledUserFactory)

    class Meta:
        model = EnrolledUserTrackerIssue


class EnrollmentQueueFactory(DjangoModelFactory):
    class Meta:
        model = EnrollmentQueue


class EnrollmentTrackerFactory(DjangoModelFactory):
    enrollment = factory.SubFactory(EnrollmentFactory)
    queue = factory.SubFactory(EnrollmentQueueFactory)

    class Meta:
        model = EnrollmentTracker
