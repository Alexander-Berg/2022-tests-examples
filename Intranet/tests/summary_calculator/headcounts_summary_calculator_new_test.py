import pytest

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory, BudgetPositionFactory
from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentFactory, DepartmentStaffFactory, StaffFactory

from staff.headcounts.headcounts_summary.headcounts_summary_calculator_new import (
    HeadcountsSummaryCalculator,
    ChiefsInfoUpdater,
)
from staff.headcounts.headcounts_summary.query_builder import (
    ChiefInfo,
    GroupingInstanceInfo,
    HierarchyValues,
    QueryBuilder,
    QueryParams,
    RelatedEntity,
    Result,
    SummaryResults,
)


@pytest.mark.django_db
def test_calculator_returns_empty_results_on_absent_data():
    # given
    calculator = HeadcountsSummaryCalculator(QueryBuilder(QueryParams.from_default_args()))

    # when
    results = calculator.get_results()

    # then
    assert results is None


@pytest.mark.django_db
def test_chiefs_info_updater():
    # given
    department = DepartmentFactory()
    child_department = DepartmentFactory()
    chief = StaffFactory()
    child_department_chief = StaffFactory()
    DepartmentStaffFactory(staff=chief, department=department, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(
        staff=child_department_chief,
        department=child_department,
        role_id=DepartmentRoles.CHIEF.value,
    )
    grouping_instance_info = GroupingInstanceInfo.default()
    grouping_instance_info.id = department.id
    child_grouping_instance_info = GroupingInstanceInfo.default()
    child_grouping_instance_info.id = child_department.id
    results = Result(
        current_level_grouping=None,
        summary=SummaryResults.default(),
        summary_without_children=SummaryResults.default(),
        grouping_instance_info=None,
        next_level_grouping={
            department.id: Result(
                current_level_grouping=None,
                summary=SummaryResults.default(),
                summary_without_children=SummaryResults.default(),
                grouping_instance_info=grouping_instance_info,
                next_level_grouping={},
                children={
                    child_department.id: Result(
                        current_level_grouping=None,
                        summary=SummaryResults.default(),
                        summary_without_children=SummaryResults.default(),
                        grouping_instance_info=child_grouping_instance_info,
                        next_level_grouping={},
                        children={},
                    ),
                },
            ),
        },
        children={},
    )

    # when
    ChiefsInfoUpdater().set_chiefs_into_result(results)

    # then
    department_grouping = results.next_level_grouping[department.id]
    assert department_grouping.grouping_instance_info.chief == ChiefInfo(
        first_name=chief.first_name,
        first_name_en=chief.first_name_en,
        last_name=chief.last_name,
        last_name_en=chief.last_name_en,
        login=chief.login,
    )

    assert department_grouping.children[child_department.id].grouping_instance_info.chief == ChiefInfo(
        first_name=child_department_chief.first_name,
        first_name_en=child_department_chief.first_name_en,
        last_name=child_department_chief.last_name,
        last_name_en=child_department_chief.last_name_en,
        login=child_department_chief.login,
    )


@pytest.mark.django_db
def test_occupied_crossing(company):
    # given
    bp = BudgetPositionFactory()
    first_bpa = BudgetPositionAssignmentFactory(
        department=company.dep1,
        budget_position=bp,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=company.dep1,
        budget_position=bp,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        previous_assignment=first_bpa,
    )
    params = QueryParams.from_default_args(
        groupings=[RelatedEntity.department],
        department_filters=[
            HierarchyValues(company.dep1.tree_id, company.dep1.lft, company.dep1.rght, company.dep1.id),
        ],
    )

    calculator = HeadcountsSummaryCalculator(QueryBuilder(params))

    # when
    results = calculator.get_results()

    # then
    assert results.summary.working_crossing == 1


@pytest.mark.django_db
def test_maternity_not_counted_for_crossing(company):
    # given
    bp = BudgetPositionFactory()
    first_bpa = BudgetPositionAssignmentFactory(
        department=company.dep1,
        budget_position=bp,
        status=BudgetPositionAssignmentStatus.MATERNITY.value,
    )
    BudgetPositionAssignmentFactory(
        department=company.dep1,
        budget_position=bp,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        previous_assignment=first_bpa,
    )
    params = QueryParams.from_default_args(
        groupings=[RelatedEntity.department],
        department_filters=[
            HierarchyValues(company.dep1.tree_id, company.dep1.lft, company.dep1.rght, company.dep1.id),
        ],
    )

    calculator = HeadcountsSummaryCalculator(QueryBuilder(params))

    # when
    results = calculator.get_results()

    # then
    assert results.summary.working_crossing == 0
