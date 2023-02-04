import factory

from staff.lib.testing import GroupFactory, StaffFactory
from staff.achievery.models import Achievement, GivenAchievement, Icon, Event


class AchievementFactory(factory.DjangoModelFactory):
    class Meta:
        model = Achievement

    owner_group = factory.SubFactory(GroupFactory)
    title = factory.Sequence(lambda x: 'title_%s' % x)
    title_en = factory.Sequence(lambda x: 'title_%s' % x)
    native_lang = 'ru'


class GivenAchievementFactory(factory.DjangoModelFactory):
    class Meta:
        model = GivenAchievement

    person = factory.SubFactory(StaffFactory)
    level = -1
    achievement = factory.SubFactory(AchievementFactory)


class IconFactory(factory.DjangoModelFactory):
    class Meta:
        model = Icon

    level = 1
    achievement = factory.SubFactory(AchievementFactory)


class EventFactory(factory.DjangoModelFactory):
    class Meta:
        model = Event

    initiator = factory.SubFactory(StaffFactory)
    level = factory.LazyAttribute(lambda x: -1)
    revision = factory.LazyAttribute(lambda x: 0)
