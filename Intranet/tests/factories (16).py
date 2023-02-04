import factory
from factory.django import DjangoModelFactory

from django.db.models.signals import post_save

from lms.staff.models import StaffCity, StaffCountry, StaffGroup, StaffLeadership, StaffOffice, StaffProfile
from lms.users.tests.factories import UserFactory


class StaffCountryFactory(DjangoModelFactory):
    class Meta:
        model = StaffCountry

    code = factory.Sequence(lambda n: f'country-{n}')
    name_ru = factory.Faker('country')
    name_en = factory.SelfAttribute('name_ru')


class StaffCityFactory(DjangoModelFactory):
    class Meta:
        model = StaffCity

    country = factory.SubFactory(StaffCountryFactory)
    name_ru = factory.Faker('city')
    name_en = factory.SelfAttribute('name_ru')


class StaffOfficeFactory(DjangoModelFactory):
    class Meta:
        model = StaffOffice

    city = factory.SubFactory(StaffCityFactory)
    code = factory.Sequence(lambda n: f'office-{n}')
    name_ru = factory.Faker('street_name')
    name_en = factory.SelfAttribute('name_ru')


class StaffGroupFactory(DjangoModelFactory):
    class Meta:
        model = StaffGroup

    name = factory.Sequence(lambda n: f'Department {n}')
    group_type = StaffGroup.TYPE_DEPARTMENT


@factory.django.mute_signals(post_save)
class StaffProfileFactory(DjangoModelFactory):
    class Meta:
        model = StaffProfile

    user = factory.SubFactory(UserFactory, staffprofile=None)
    language_native = factory.Iterator(["ru", "en", "de", "tr"])
    office = factory.SubFactory(StaffOfficeFactory)
    city = factory.SelfAttribute('office.city')


class StaffLeadershipFactory(DjangoModelFactory):
    class Meta:
        model = StaffLeadership

    profile = factory.SubFactory(StaffProfileFactory)
    group = factory.Sequence(lambda n: n)


@factory.django.mute_signals(post_save)
class UserWithStaffProfileFactory(UserFactory):
    staffprofile = factory.RelatedFactory(StaffProfileFactory, factory_related_name='user')
