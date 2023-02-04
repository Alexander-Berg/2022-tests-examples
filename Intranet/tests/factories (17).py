import factory
import faker
from factory.django import DjangoModelFactory

from lms.tags.models import Tag, UserTag
from lms.users.tests.factories import UserFactory

fake = faker.Faker()


class TagFactory(DjangoModelFactory):
    name = factory.Sequence(lambda n: f'{fake.word()}-{n}')

    class Meta:
        model = Tag


class UserTagFactory(DjangoModelFactory):
    tag = factory.SubFactory(TagFactory)
    user = factory.SubFactory(UserFactory)

    class Meta:
        model = UserTag
