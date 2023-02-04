import factory
import faker
from factory.django import DjangoModelFactory
from factory.fuzzy import FuzzyChoice

from lms.courses.tests.factories import CourseFactory
from lms.users.tests.factories import UserFactory

from ..models import CourseFollower, CourseMailing, IntendedForChoice, Mailing

fake = faker.Faker()


class CourseFollowerFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    user = factory.SubFactory(UserFactory)

    class Meta:
        model = CourseFollower


class MailingFactory(DjangoModelFactory):
    mailing_task = factory.Faker('word')
    name = factory.Faker('word')
    intended_for = FuzzyChoice(IntendedForChoice.labels)

    class Meta:
        model = Mailing


class CourseMailingFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    mailing = factory.SubFactory(MailingFactory)

    class Meta:
        model = CourseMailing
