import factory

from datetime import date

from staff.oebs.models import PaySys
from staff.lib.testing import (
    BudgetPositionFactory,
    DepartmentFactory,
    GeographyDepartmentFactory,
    OccupationFactory,
    StaffFactory,
    ValueStreamFactory,
)

from staff.budget_position import models, const


class ChangeFactory(factory.DjangoModelFactory):
    class Meta:
        model = models.ChangeRegistry

    budget_position = factory.SubFactory(BudgetPositionFactory)
    position_type = const.PositionType.OFFER.value
    ticket = 'SALARY-123'
    staff = factory.SubFactory(StaffFactory)


class PaysysFactory(factory.DjangoModelFactory):
    class Meta:
        model = PaySys


class GradeFactory(factory.DjangoModelFactory):
    class Meta:
        model = models.OEBSGrade

    grade_id = factory.Sequence(lambda n: n + 1)
    occupation = factory.SubFactory(OccupationFactory)
    level = factory.Sequence(lambda n: n + 1)


class RewardFactory(factory.DjangoModelFactory):
    class Meta:
        model = models.Reward

    name = factory.Sequence(lambda n: f'reward {n}')
    start_date = date.today()
    scheme_id = factory.Sequence(lambda n: n)


class BudgetPositionAssignmentFactory(factory.DjangoModelFactory):
    budget_position = factory.SubFactory(BudgetPositionFactory)
    status = models.BudgetPositionAssignmentStatus.OCCUPIED.value
    creates_new_position = True
    replacement_type = models.ReplacementType.BUSY.value

    department = factory.SubFactory(DepartmentFactory)
    value_stream = factory.SubFactory(ValueStreamFactory)
    geography = factory.SubFactory(GeographyDepartmentFactory)

    bonus_id = 0
    reward = factory.SubFactory(RewardFactory)
    review_id = 0

    class Meta:
        model = models.BudgetPositionAssignment
