import factory
import faker
from factory.django import DjangoModelFactory

from lms.actions.models import AddAchievementEvent, CourseTrigger
from lms.courses.tests.factories import CourseFactory
from lms.users.tests.factories import UserFactory

fake = faker.Faker()


class AddAchievementEventFactory(DjangoModelFactory):
    class Meta:
        model = AddAchievementEvent

    achievement_id = fake.pyint()
    user = factory.SubFactory(UserFactory)
    course = factory.SubFactory(CourseFactory)


class CourseTriggerFactory(DjangoModelFactory):
    class Meta:
        model = CourseTrigger

    name = factory.Faker('sentence', nb_words=2)
    course = factory.SubFactory(CourseFactory)
