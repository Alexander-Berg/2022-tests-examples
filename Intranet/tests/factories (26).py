import factory

from datetime import datetime

from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    OfficeFactory,
)


class DismissalFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'dismissal.Dismissal'

    staff = factory.SubFactory(StaffFactory)
    department = factory.SubFactory(DepartmentFactory)
    office = factory.SubFactory(OfficeFactory)
    created_by = factory.SubFactory(StaffFactory)

    created_at = factory.LazyAttribute(lambda x: datetime.now())
    modified_at = factory.LazyAttribute(lambda x: datetime.now())


class CheckPointFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'dismissal.CheckPoint'

    dismissal = factory.SubFactory(DismissalFactory)


class CheckPointTemplateFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'dismissal.CheckPointTemplate'


class ClearanceChitTemplateFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'dismissal.ClearanceChitTemplate'
