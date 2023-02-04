import factory
import faker
from factory.django import DjangoModelFactory

from lms.courses.tests.factories import CourseTeamFactory
from lms.users.tests.factories import GroupFactory

from ..models import FirewallRule, GroupFirewallRule, TeamFirewallRule

fake = faker.Faker()


class FirewallRuleFactory(DjangoModelFactory):
    class Meta:
        model = FirewallRule

    slug = factory.Sequence(lambda n: f"{fake.word().lower()}-{n}-rule")
    summary = factory.Faker('sentences', nb=2)


class GroupFirewallRuleFactory(DjangoModelFactory):
    class Meta:
        model = GroupFirewallRule

    group = factory.SubFactory(GroupFactory)
    rule = factory.SubFactory(FirewallRuleFactory)


class TeamFirewallRuleFactory(DjangoModelFactory):
    class Meta:
        model = TeamFirewallRule

    team = factory.SubFactory(CourseTeamFactory)
    rule = factory.SubFactory(FirewallRuleFactory)
