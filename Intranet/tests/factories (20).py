import factory
import faker
from factory.django import DjangoModelFactory

from mentor.users.tests.factories import UserFactory

from ..models import MenteeFeedback, Mentor, MentorFeedback, Mentorship

fake = faker.Faker()


class MentorFactory(DjangoModelFactory):
    class Meta:
        model = Mentor

    user = factory.SubFactory(UserFactory)
    description = factory.Faker("text")
    assistance = factory.Faker("sentences", nb=5)
    carrier_begin = factory.Faker("date")


class MentorshipFactory(DjangoModelFactory):
    class Meta:
        model = Mentorship

    mentor = factory.SubFactory(MentorFactory)
    mentee = factory.SubFactory(UserFactory)
    intro = factory.Faker("text")
    status_by = factory.LazyAttribute(lambda obj: obj.mentee)
    status_message = factory.Faker("text")


class MentorFeedbackFactory(DjangoModelFactory):
    class Meta:
        model = MentorFeedback

    mentor = factory.SubFactory(MentorFactory)
    mentorship = factory.SubFactory(
        MentorshipFactory, mentor=factory.SelfAttribute("..mentor")
    )
    comments = factory.Faker("text")


class MenteeFeedbackFactory(DjangoModelFactory):
    class Meta:
        model = MenteeFeedback

    mentee = factory.SubFactory(UserFactory)
    mentorship = factory.SubFactory(
        MentorshipFactory, mentee=factory.SelfAttribute("..mentee")
    )
    comments = factory.Faker("text")
