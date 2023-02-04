import factory
from datetime import datetime

from staff.lib.testing import StaffFactory, OfficeFactory


class StaffLastOfficeFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'whistlah.StaffLastOffice'

    staff = factory.SubFactory(StaffFactory)
    office = factory.SubFactory(OfficeFactory)
    office_name = factory.Sequence(lambda x: 'name{}'.format(x))
    office_name_en = factory.Sequence(lambda x: 'name_en{}'.format(x))
    updated_at = factory.LazyAttribute(lambda x: datetime.now())


class OfficeNetFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'whistlah.OfficeNet'

    office = factory.SubFactory(OfficeFactory)
    name = factory.Sequence(lambda x: 'name{}'.format(x))
    net = factory.Sequence(lambda x: 'net{}'.format(x))


class NOCOfficeFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'whistlah.NOCOffice'

    noc_office_id = factory.LazyAttribute(lambda x: x)
    office_id = factory.LazyAttribute(lambda x: x)
