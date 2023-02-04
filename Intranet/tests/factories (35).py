from datetime import date
from uuid import uuid1

import factory

from staff.lib.testing import OrganizationFactory, OfficeFactory, StaffFactory
from staff.workspace_management.models import BusinessUnit, OfficeArea, RoomSharePie, Share


class BusinessUnitFactory(factory.DjangoModelFactory):
    class Meta:
        model = BusinessUnit

    name = factory.LazyAttribute(lambda x: factory.Faker('company', locale='ru_RU').generate()[:50])
    name_en = factory.LazyAttribute(lambda x: factory.Faker('company').generate()[:50])
    organization = factory.SubFactory(OrganizationFactory)


class OfficeAreaFactory(factory.DjangoModelFactory):
    class Meta:
        model = OfficeArea

    id = factory.LazyFunction(uuid1)
    office = factory.SubFactory(OfficeFactory)
    from_date = date.today()
    to_date = date.max


class RoomSharePieFactory(factory.DjangoModelFactory):
    class Meta:
        model = RoomSharePie

    id = factory.LazyFunction(uuid1)
    author = factory.SubFactory(StaffFactory)
    from_date = date.today()
    to_date = date.max


class ShareFactory(factory.DjangoModelFactory):
    class Meta:
        model = Share
