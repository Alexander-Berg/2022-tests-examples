import factory

from staff.achievery import domain, permissions
from staff.lib.testing import StaffFactory

from . import model


class AchieveryDomainObjectFactory(factory.Factory):
    class Meta:
        abstract = True

    user = factory.SubFactory(StaffFactory)
    role_registry = factory.LazyAttribute(
        lambda x: permissions.RoleRegistry(x.user)
    )


class OwnerGroupFactory(AchieveryDomainObjectFactory):
    class Meta:
        model = domain.OwnerGroup


class IconFactory(AchieveryDomainObjectFactory):
    class Meta:
        model = domain.Icon

    model = factory.SubFactory(model.IconFactory)


class PersonFactory(AchieveryDomainObjectFactory):
    class Meta:
        model = domain.Person

    model = factory.SubFactory(StaffFactory)


class AchievementFactory(AchieveryDomainObjectFactory):
    class Meta:
        model = domain.Achievement

    model = factory.SubFactory(model.AchievementFactory)


class GivenAchievementFactory(AchieveryDomainObjectFactory):
    class Meta:
        model = domain.GivenAchievement

    model = factory.SubFactory(model.GivenAchievementFactory)
