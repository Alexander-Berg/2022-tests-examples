from datetime import timedelta

import factory
import faker
from factory import post_generation
from factory.django import DjangoModelFactory

from django.utils import timezone

from lms.courses.tests.factories import CourseFactory, CourseStudentFactory

from ..models import Classroom, StudentSlot, Timeslot, TimeslotExchange

fake = faker.Faker()


class ClassroomFactory(DjangoModelFactory):
    class Meta:
        model = Classroom

    course = factory.SubFactory(CourseFactory)

    name = factory.Faker('word')
    description = factory.Faker('text')
    estimated_time = fake.pyint()


class TimeslotFactory(DjangoModelFactory):
    class Meta:
        model = Timeslot

    course = factory.SubFactory(CourseFactory)
    classroom = factory.SubFactory(ClassroomFactory, course=factory.SelfAttribute('..course'))

    begin_date = factory.Faker(
        'date_time',
        tzinfo=timezone.utc,
    )
    end_date = factory.Faker(
        'date_time_between_dates',
        tzinfo=timezone.utc,
        datetime_start=factory.SelfAttribute('..begin_date'),
        datetime_end=factory.LazyAttribute(lambda t: t.datetime_start + timedelta(hours=8))
    )
    title = factory.Faker('sentence', nb_words=2)
    summary = factory.Faker('sentence')

    @post_generation
    def course_groups(self, create, extracted, **kwargs):
        if not create:
            return

        if extracted:
            self.course_groups.set(extracted)


class StudentSlotFactory(DjangoModelFactory):
    class Meta:
        model = StudentSlot

    timeslot = factory.SubFactory(TimeslotFactory)
    student = factory.SubFactory(CourseStudentFactory, course=factory.SelfAttribute('..timeslot.course'))


class TimeslotExchangeFactory(DjangoModelFactory):
    class Meta:
        model = TimeslotExchange

    course = factory.SubFactory(CourseFactory)

    target_timeslot = factory.SubFactory(
        TimeslotFactory,
        classroom=factory.SubFactory(
            ClassroomFactory,
            course=factory.SelfAttribute('...course')
        )
    )

    student_slot = factory.SubFactory(
        StudentSlotFactory,
        timeslot=factory.SubFactory(
            TimeslotFactory,
            classroom=factory.SelfAttribute('...target_timeslot.classroom')
        )
    )
