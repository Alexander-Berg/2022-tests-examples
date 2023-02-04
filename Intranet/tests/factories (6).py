import factory
from factory.django import DjangoModelFactory

from lms.calendars.models import CalendarEvent, CalendarLayer
from lms.classrooms.tests.factories import TimeslotFactory
from lms.courses.tests.factories import CourseFactory


class CalendarLayerFactory(DjangoModelFactory):
    class Meta:
        model = CalendarLayer

    id = factory.Sequence(lambda n: n)
    course = factory.SubFactory(CourseFactory)


class CalendarEventFactory(DjangoModelFactory):
    class Meta:
        model = CalendarEvent

    id = factory.Sequence(lambda n: n)
    course = factory.SubFactory(CourseFactory)
    timeslot = factory.SubFactory(TimeslotFactory, course=factory.SelfAttribute('..course'))
