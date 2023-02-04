import pytest

from staff.departments.models import DepartmentRoles, Department
from staff.headcounts.tests.factories import CreditManagementApplicationFactory, CreditManagementApplicationRowFactory
from staff.lib.testing import StaffFactory, BudgetPositionFactory, DepartmentFactory

from staff.headcounts.headcounts_credit_management import (
    Repository,
    CreateCreditRepaymentRequest,
    CreateCreditRepaymentRequestRow,
)
from staff.headcounts.models import CreditManagementApplication


@pytest.mark.django_db
def test_repository_correctly_saves_application():
    # given
    author = StaffFactory()
    credit_budget_position = BudgetPositionFactory()
    repayment_budget_position = BudgetPositionFactory()

    repo = Repository(author)
    request_row = CreateCreditRepaymentRequestRow(credit_budget_position.id, repayment_budget_position.id)
    credit_repayment_request = CreateCreditRepaymentRequest(
        rows=[request_row],
        comment='111',
    )

    # when
    id = repo.save(credit_repayment_request)

    # then
    application = CreditManagementApplication.objects.get(id=id)

    assert application.rows.count() == 1
    assert application.author == author
    assert application.comment == credit_repayment_request.comment

    row = application.rows.first()
    assert row.credit_budget_position.id == request_row.credit_budget_position_id
    assert row.repayment_budget_position.id == request_row.repayment_budget_position_id


@pytest.mark.django_db
def test_repository_correctly_calculates_involved_budget_positions():
    # given
    application = CreditManagementApplicationFactory()
    first_row = CreditManagementApplicationRowFactory(application=application)
    second_row = CreditManagementApplicationRowFactory(application=application)
    third_row = CreditManagementApplicationRowFactory(application=application)

    repo = Repository(application.author)

    # when
    result = repo._involved_budget_positions(CreditManagementApplication.objects.get(id=application.id))

    # then
    assert result == {
        first_row.credit_budget_position.id: first_row.credit_budget_position.code,
        first_row.repayment_budget_position.id: first_row.repayment_budget_position.code,
        second_row.credit_budget_position.id: second_row.credit_budget_position.code,
        second_row.repayment_budget_position.id: second_row.repayment_budget_position.code,
        third_row.credit_budget_position.id: third_row.credit_budget_position.code,
        third_row.repayment_budget_position.id: third_row.repayment_budget_position.code,
    }


@pytest.mark.django_db
def test_responsible_for_department_calculation(company):
    # given
    author = StaffFactory()
    repo = Repository(author)
    dep_dict = Department.objects.filter(id=company.dep111.id).values('lft', 'rght', 'tree_id').get()

    # when
    partner = repo._responsible_for_department(dep_dict, DepartmentRoles.HR_PARTNER.value)
    analyst = repo._responsible_for_department(dep_dict, DepartmentRoles.HR_ANALYST.value)

    # then
    assert partner == company.persons['dep1-hr-partner'].login
    assert analyst == company.persons['dep1-hr-analyst'].login


@pytest.mark.django_db
def test_responsible_for_department_calculation_when_he_is_missing():
    # given
    author = StaffFactory()
    repo = Repository(author)
    dep = DepartmentFactory()
    dep_dict = Department.objects.filter(id=dep.id).values('lft', 'rght', 'tree_id').get()

    # when
    result = repo._responsible_for_department(dep_dict, DepartmentRoles.HR_PARTNER.value)

    # then
    assert result is None
