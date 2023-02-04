import factory
from factory.django import DjangoModelFactory

from lms.courses.tests.factories import CourseFactory, CourseFileFactory, CourseStudentFactory

from ..models import Scorm, ScormFile, ScormResource, ScormStudentAttempt


class ScormFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('word')
    description = factory.Faker('text')
    is_active = False

    class Meta:
        model = Scorm


class ScormFileFactory(DjangoModelFactory):
    scorm = factory.SubFactory(ScormFactory)
    scorm_status = ScormFile.SCORM_MODULE_STATUS_READY
    public_url = factory.Faker('url')
    course_file = factory.SubFactory(CourseFileFactory)
    comment = factory.Sequence(lambda n: f'comment-{n}')

    class Meta:
        model = ScormFile


class ScormResourceFactory(DjangoModelFactory):
    scorm_file = factory.SubFactory(ScormFileFactory)
    resource_id = factory.Sequence(lambda n: f'resource-{n}')
    href = factory.Sequence(lambda n: f'/href-{n}')

    class Meta:
        model = ScormResource


class ScormStudentAttemptFactory(DjangoModelFactory):
    scorm = factory.SubFactory(ScormFactory)
    scorm_file = factory.SubFactory(
        ScormFileFactory,
        scorm=factory.SelfAttribute('..scorm'),
        course_file=factory.SubFactory(
            CourseFileFactory,
            course=factory.SelfAttribute('..scorm.course'),
        )
    )
    student = factory.SubFactory(CourseStudentFactory, course=factory.SelfAttribute('..scorm.course'))

    class Meta:
        model = ScormStudentAttempt
