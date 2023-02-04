from typing import Any, Sequence

import factory
import faker
from factory import post_generation
from factory.django import DjangoModelFactory
from factory.fuzzy import FuzzyChoice

from django.contrib.auth import get_user_model

from lms.moduletypes.models import ModuleType
from lms.preferences.tests.factories import ColorThemeFactory
from lms.users.tests.factories import GroupFactory, PermissionPresetFactory, ServiceAccountFactory, UserFactory

from ..models import (
    Cohort, Course, CourseBlock, CourseCategory, CourseCity, CourseFile, CourseGroup, CourseModule, CourseStudent,
    CourseTeam, CourseVisibility, CourseWorkflow, LinkedCourse, Provider, ServiceAccountCourse, StudentCourseProgress,
    StudentModuleProgress, StudyMode, Tutor,
)

User = get_user_model()
fake = faker.Faker()


class CourseWorkflowFactory(DjangoModelFactory):
    class Meta:
        model = CourseWorkflow

    name = factory.Sequence(lambda n: f"{fake.word()}-{n}")


class StudyModeFactory(DjangoModelFactory):
    class Meta:
        model = StudyMode

    slug = factory.Sequence(lambda n: f'study-mode-{n}')
    name = factory.Iterator(["Online", "Offline"])
    description = factory.Faker('sentences', nb=3)


class ProviderFactory(DjangoModelFactory):
    class Meta:
        model = Provider

    name = factory.Sequence(lambda n: f"{fake.company()}-{n}")
    description = factory.Faker('sentences', nb=5)
    created_by = factory.SubFactory(UserFactory, is_staff=True)


class CourseCategoryFactory(DjangoModelFactory):
    class Meta:
        model = CourseCategory

    name = factory.Sequence(lambda n: f'{fake.sentence(nb_words=3)}-{n}')
    slug = factory.Sequence(lambda n: f'TST-{n}')
    description = factory.Faker('sentences', nb=5)
    color_theme = factory.SubFactory(ColorThemeFactory)
    created_by = factory.SubFactory(UserFactory, is_staff=True)


class CourseCityFactory(DjangoModelFactory):
    class Meta:
        model = CourseCity

    name = factory.Faker('city')
    slug = factory.Sequence(lambda n: f'city-{n}')


class CourseFactory(DjangoModelFactory):
    class Meta:
        model = Course

    city = factory.SubFactory(CourseCityFactory)
    study_mode = factory.SubFactory(StudyModeFactory)
    author = factory.SubFactory(UserFactory)
    provider = factory.SubFactory(ProviderFactory)
    workflow = factory.SubFactory(CourseWorkflowFactory)

    slug = factory.Sequence(lambda n: f'course-{n}')
    name = factory.Sequence(lambda n: f'{fake.sentence(nb_words=3, variable_nb_words=False)}-{n}')
    summary = factory.Faker('sentence', nb_words=6)
    description = factory.Faker('text')
    payment_method = Course.PaymentMethodChoices.FREE


class CourseFileFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    filename = factory.Sequence(lambda n: f'filename-{n}')

    class Meta:
        model = CourseFile


class CourseGroupFactory(DjangoModelFactory):
    class Meta:
        model = CourseGroup

    slug = factory.Sequence(lambda n: f'group-{n}')
    name = factory.Sequence(lambda n: f'{fake.sentence(nb_words=2, variable_nb_words=False)}-{n}')
    course = factory.SubFactory(CourseFactory)

    # @factory.post_generation
    # def members(self, create: bool, members: Iterable[User], **kwargs):
    #     if not create:
    #         return
    #
    #     if members:
    #         self.members.add(*members)


class CourseVisibilityFactory(DjangoModelFactory):
    class Meta:
        model = CourseVisibility

    course = factory.SubFactory(CourseFactory)
    rules = {
        "eq": ["staff_is_head", True]
    }

    @post_generation
    def post(self, create: bool, extracted: Sequence[Any], **kwargs):
        self.clean()


class CourseTeamFactory(GroupFactory):
    permission_preset = factory.SubFactory(PermissionPresetFactory)
    name = factory.Sequence(lambda n: f"{fake.sentence(nb_words=2)}-{n}")

    class Meta:
        model = CourseTeam


class CourseStudentFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    user = factory.SubFactory(UserFactory)

    class Meta:
        model = CourseStudent


class ModuleTypeFactory(DjangoModelFactory):
    class Meta:
        model = ModuleType


class FakeCourseModule(CourseModule):
    DETECT_MODULE_TYPE = False

    class Meta:
        proxy = True


class CourseModuleFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('sentence', nb_words=3)
    description = factory.Faker('text')
    estimated_time = fake.pyint()

    module_type = factory.Iterator(ModuleType.objects.all())

    class Meta:
        model = FakeCourseModule


class CourseBlockFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    name = factory.Faker('sentence', nb_words=3)
    summary = factory.Faker('sentence', nb_words=6)

    class Meta:
        model = CourseBlock


class TutorAbstractFactory(DjangoModelFactory):
    is_active = True
    position = FuzzyChoice(Tutor.PositionChoices.values)

    class Meta:
        model = Tutor
        abstract = True


class TutorInternalFactory(TutorAbstractFactory):
    is_internal = True
    user = factory.SubFactory(UserFactory)


class TutorExternalFactory(TutorAbstractFactory):
    is_internal = False
    name = factory.Faker('name')


class CohortFactory(DjangoModelFactory):
    name = factory.Faker('sentence', nb_words=3)

    class Meta:
        model = Cohort

    @post_generation
    def users(self, create, extracted, **kwargs):
        if not create:
            return

        if extracted:
            self.users.set(extracted)


class StudentCourseProgressFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    student = factory.SubFactory(CourseStudentFactory, course=factory.SelfAttribute('..course'))
    score = fake.pyint(max_value=100)

    class Meta:
        model = StudentCourseProgress


class StudentModuleProgressFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    student = factory.SubFactory(CourseStudentFactory, course=factory.SelfAttribute('..course'))
    module = factory.SubFactory(CourseModuleFactory, course=factory.SelfAttribute('..course'))
    score = fake.pyint(max_value=100)

    class Meta:
        model = StudentModuleProgress


class LinkedCourseFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory, course_type=Course.TypeChoices.TRACK.value)
    linked_course = factory.SubFactory(CourseFactory, course_type=Course.TypeChoices.COURSE.value)
    name = factory.Faker('word')
    description = factory.Faker('text')
    estimated_time = fake.pyint()

    class Meta:
        model = LinkedCourse


class ServiceAccountCourseFactory(DjangoModelFactory):
    course = factory.SubFactory(CourseFactory)
    service_account = factory.SubFactory(ServiceAccountFactory)

    class Meta:
        model = ServiceAccountCourse
