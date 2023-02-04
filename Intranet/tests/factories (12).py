import factory
import faker
from factory.django import DjangoModelFactory

from django.contrib.auth import get_user_model

from lms.users.tests.factories import UserFactory

from ..models import Mentorship

User = get_user_model()
fake = faker.Faker()


class MentorshipFactory(DjangoModelFactory):
    mentor = factory.SubFactory(UserFactory)
    mentee = factory.SubFactory(UserFactory)

    class Meta:
        model = Mentorship
