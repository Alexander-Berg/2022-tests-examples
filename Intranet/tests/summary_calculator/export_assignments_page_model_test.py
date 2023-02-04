import pytest

from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory

from staff.headcounts.headcounts_summary.assignments_page_model import (
    AssignmentsPageModel,
    Department,
    Name,
)

from staff.headcounts.headcounts_summary.query_builder import HierarchyValues, QueryParams, RelatedEntity


@pytest.mark.django_db
def test_assignments_page_model(company):
    # given
    yandex_dep = company.yandex
    dep1 = company.dep1
    dep2 = company.dep2
    bpa1 = BudgetPositionAssignmentFactory(
        department=dep1,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        value_stream=company.vs_1,
    )
    bpa2 = BudgetPositionAssignmentFactory(
        department=dep2,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        value_stream=company.vs_2,
    )
    params = QueryParams.from_default_args(groupings=[RelatedEntity.department])
    page_model = AssignmentsPageModel(params)

    # when
    result = list(page_model.get_result())

    # then
    assert len(result) == 2
    assert result[0].budget_position_assignment_id == bpa1.id
    assert result[0].department_chain == [
        Department(yandex_dep.id, yandex_dep.intranet_status, Name(yandex_dep.name_en, yandex_dep.name)),
        Department(dep1.id, dep1.intranet_status, Name(dep1.name_en, dep1.name)),
    ]
    assert result[1].budget_position_assignment_id == bpa2.id
    assert result[1].department_chain == [
        Department(yandex_dep.id, yandex_dep.intranet_status, Name(yandex_dep.name_en, yandex_dep.name)),
        Department(dep2.id, dep2.intranet_status, Name(dep2.name_en, dep2.name)),
    ]


@pytest.mark.django_db
def test_assignments_qs_uses_filters(company):
    # given
    BudgetPositionAssignmentFactory(
        department=company.dep1,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        value_stream=company.vs_1,
    )
    bpa2 = BudgetPositionAssignmentFactory(
        department=company.dep2,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        value_stream=company.vs_2,
    )
    params = QueryParams.from_default_args(
        groupings=[RelatedEntity.department],
        department_permission_filters=[HierarchyValues.none()],
        value_stream_permission_filters=[
            HierarchyValues(company.vs_2.tree_id, company.vs_2.lft, company.vs_2.rght, company.vs_2.id),
        ],
        geography_permission_filters=[HierarchyValues.none()],
    )
    page_model = AssignmentsPageModel(params)

    # when
    result = list(page_model.get_result())

    # then
    assert len(result) == 1
    assert result[0].budget_position_assignment_id == bpa2.id
