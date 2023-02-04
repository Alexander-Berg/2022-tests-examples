import pytest

from django.db import connections, router

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.departments.models import Department

from staff.headcounts.headcounts_summary.query_builder import (
    Aliases,
    AssignmentPartQueryBuilder,
    HierarchyValues,
    QueryParams,
    RelatedEntity,
    SummaryQueryBuilder,
)


@pytest.mark.django_db
def test_rows_for_groupings(company):
    # given
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep2, value_stream=company.vs_root, geography=company.geo_russia)
    params = QueryParams.from_default_args(
        groupings=[RelatedEntity.department],
        department_filters=[
            HierarchyValues(company.yandex.tree_id, company.yandex.lft, company.yandex.rght, company.yandex.id),
        ],
    )
    query = SummaryQueryBuilder(
        aliases=Aliases(),
        query_params=params,
        assignement_query_builder=AssignmentPartQueryBuilder(),
    ).build_query_for_rows_without_grouping()

    cursor = connections[router.db_for_read(Department)].cursor()

    # when
    cursor.execute(query, tuple())

    # then
    result = cursor.fetchall()
    assert {row[0] for row in result} == {
        company.dep1.id,
        company.dep2.id,
    }


def test_hierarchy_joins_returns_non_empty_join_on_user_filter_but_empty_grouping(company):
    # given
    hierarchy_filter = HierarchyValues(id=1, lft=5, rght=6, tree_id=7)
    query_params = QueryParams.from_default_args(value_stream_filters=[hierarchy_filter])
    query_builder = SummaryQueryBuilder(query_params, Aliases(), AssignmentPartQueryBuilder())
    # when
    result = query_builder._joins()

    # then
    assert len(result) > 0
