import factory
import faker
from factory.django import DjangoModelFactory

from lms.assignments.models import Assignment, AssignmentStudentResult
from lms.courses.tests.factories import CourseFactory, CourseStudentFactory

fake = faker.Faker()


class AssignmentFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('word')
    description = factory.Faker('text')
    estimated_time = fake.pyint()

    class Meta:
        model = Assignment


class AssignmentStudentResultFactory(DjangoModelFactory):
    class Meta:
        model = AssignmentStudentResult

    assignment = factory.SubFactory(AssignmentFactory)
    student = factory.SubFactory(CourseStudentFactory, course=factory.SelfAttribute('..assignment.course'))
