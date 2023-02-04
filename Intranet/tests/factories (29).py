import factory

from staff.departments.models import HeadcountPosition
from staff.lib.testing import BudgetPositionFactory, StaffFactory, DepartmentFactory
from staff.oebs.constants import REPLACEMENT_TYPE, PERSON_POSITION_STATUS

from staff.headcounts.models import CreditManagementApplication, CreditManagementApplicationRow
from staff.headcounts.headcounts_credit_management import BudgetPosition, CreditRepaymentRow


class HeadcountPositionFactory(factory.DjangoModelFactory):
    class Meta:
        model = HeadcountPosition

    id = factory.Sequence(lambda x: '{}'.format(x))
    code = factory.Sequence(lambda x: x + 1)
    geo = factory.Sequence(lambda x: 'RUSc_{}'.format(x))
    bonus_id = factory.Sequence(lambda x: x + 2)
    reward_id = factory.Sequence(lambda x: x + 3)
    review_id = factory.Sequence(lambda x: x + 4)
    name = factory.Sequence(lambda n: 'position_%d' % n)
    headcount = 1
    is_crossing = False
    category_is_new = True
    prev_index = None
    index = 1
    next_index = None
    replacement_type = REPLACEMENT_TYPE.WO_REPLACEMENT
    status = PERSON_POSITION_STATUS.OCCUPIED
    department = factory.SubFactory(DepartmentFactory)
    valuestream = None


class CreditManagementApplicationFactory(factory.DjangoModelFactory):
    class Meta:
        model = CreditManagementApplication

    id = factory.Sequence(lambda x: x + 1)
    author = factory.SubFactory(StaffFactory)
    comment = factory.Sequence(lambda x: f'position_{x}')
    startrek_headcount_key = factory.Sequence(lambda x: f'HEADCOUNT-{x + 1}')
    is_active = True


class CreditManagementApplicationRowFactory(factory.DjangoModelFactory):
    class Meta:
        model = CreditManagementApplicationRow

    credit_budget_position = factory.SubFactory(BudgetPositionFactory)
    repayment_budget_position = factory.SubFactory(BudgetPositionFactory)
    application = factory.SubFactory(CreditManagementApplicationFactory)


class CreditRepaymentBudgetPositionFactory(factory.Factory):
    class Meta:
        model = BudgetPosition
    code = factory.Sequence(lambda x: x)
    department_url = None
    hr_partner_login = None
    hr_analyst_login = None
    vacancy_id = None
    valuestream = None
    geography = None


class CreditRepaymentRowFactory(factory.Factory):
    class Meta:
        model = CreditRepaymentRow

    credit_budget_position = factory.SubFactory(CreditRepaymentBudgetPositionFactory)
    repayment_budget_position = factory.SubFactory(CreditRepaymentBudgetPositionFactory)
    workflow_id = None
