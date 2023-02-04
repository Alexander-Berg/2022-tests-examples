import factory
import faker
from factory.django import DjangoModelFactory
from factory.fuzzy import FuzzyChoice

from lms.courses.tests.factories import CourseFactory
from lms.enrollments.models import EnrolledUser, Enrollment, EnrollSurvey, EnrollSurveyField
from lms.users.tests.factories import UserFactory

fake = faker.Faker()


class EnrollmentFactory(DjangoModelFactory):
    class Meta:
        model = Enrollment

    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('word')
    summary = factory.Faker('sentence', nb_words=3)


class TrackerEnrollmentFactory(EnrollmentFactory):
    enroll_type = Enrollment.TYPE_TRACKER


class NotTrackerEnrollmentFactory(EnrollmentFactory):
    enroll_type = FuzzyChoice(
        set(Enrollment.TYPE_CHOICES_KEYS) - {Enrollment.TYPE_TRACKER},
    )


class EnrolledUserFactory(DjangoModelFactory):
    class Meta:
        model = EnrolledUser

    course = factory.SubFactory(CourseFactory)
    enrollment = factory.SubFactory(EnrollmentFactory, course=factory.SelfAttribute('..course'))
    user = factory.SubFactory(UserFactory)


class EnrollSurveyFactory(DjangoModelFactory):
    class Meta:
        model = EnrollSurvey

    slug = factory.Faker('slug')
    name = factory.Sequence(lambda n: f"{fake.word().lower()}-{n}")
    summary = factory.Faker('sentence')
    description = factory.Faker('sentence')
    created_by = factory.SubFactory(UserFactory)


class EnrollSurveyFieldFactory(DjangoModelFactory):
    class Meta:
        model = EnrollSurveyField

    survey = factory.SubFactory(EnrollSurveyFactory)
    name = factory.Sequence(lambda n: f"{fake.word().lower()}-{n}")
    title = factory.Faker('word')
    description = factory.Faker('sentence')
    placeholder = factory.Faker('sentence')
    field_type = EnrollSurveyField.TYPE_TEXT
    # parameters = {'max_length': 255}
    # default_value = factory.Faker('word')
