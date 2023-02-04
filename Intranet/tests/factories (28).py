import factory
from datetime import datetime, timedelta

from staff.lib.testing import StaffFactory


class WorkflowFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'gap.Workflow'

    rank = factory.Sequence(lambda x: x)


class GapFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'gap.Gap'

    gap_type = factory.SubFactory(WorkflowFactory)
    created_by = factory.SubFactory(StaffFactory)
    staff = factory.SubFactory(StaffFactory)
    left_edge = factory.LazyAttribute(lambda x: datetime.now())
    right_edge = factory.LazyAttribute(lambda x: datetime.now() + timedelta(days=1))
