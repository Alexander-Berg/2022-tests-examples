import pytest

from django.db import connections, router

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.departments.models import Department

from staff.headcounts.headcounts_summary.query_builder import QueryBuilder
from staff.headcounts.headcounts_summary.query_builder.query_params import (
    HierarchyValues,
    QueryParams,
    RelatedEntity,
)


def test_params_uses_department_table():
    # given
    hv = [HierarchyValues(tree_id=1, lft=1, rght=2, id=1)]
    with_department_filters = QueryParams.from_default_args(department_filters=hv)
    with_department_grouping = QueryParams.from_default_args(groupings=[RelatedEntity.department])
    without_department = QueryParams.from_default_args()
    without_department_with_restrictions = QueryParams.from_default_args(department_permission_filters=hv)

    # then
    assert with_department_filters.params_uses_department_table is True
    assert with_department_grouping.params_uses_department_table is True
    assert without_department.params_uses_department_table is False
    assert without_department_with_restrictions.params_uses_department_table is True


def test_params_uses_value_stream_table():
    # given
    hv = [HierarchyValues(tree_id=1, lft=1, rght=2, id=1)]
    with_value_stream_filters = QueryParams.from_default_args(value_stream_filters=hv)
    with_value_stream_grouping = QueryParams.from_default_args(groupings=[RelatedEntity.value_stream])
    without_value_stream = QueryParams.from_default_args()
    without_value_stream_with_restrictions = QueryParams.from_default_args(value_stream_permission_filters=hv)

    # then
    assert with_value_stream_filters.params_uses_value_stream_table is True
    assert with_value_stream_grouping.params_uses_value_stream_table is True
    assert without_value_stream.params_uses_value_stream_table is False
    assert without_value_stream_with_restrictions.params_uses_value_stream_table is True


def test_builder_joins_zero_info_on_empty_grouping():
    # given
    query_params = QueryParams.from_default_args()
    query_builder = QueryBuilder(query_params)

    # when
    result = query_builder._join_related_entities_info()

    # then
    assert result == ''


def test_builder_single_join_department_info():
    # given
    query_params = QueryParams.from_default_args(groupings=[
        RelatedEntity.department,
    ])
    query_builder = QueryBuilder(query_params)

    # when
    result = query_builder._join_related_entities_info()

    # then
    assert result == (
        'JOIN intranet_department AS department_join ON (department_join.id = summary_select.department_id)'
    )


def test_builder_double_join_department_and_vs_info():
    # given
    query_params = QueryParams.from_default_args(groupings=[
        RelatedEntity.department,
        RelatedEntity.value_stream
    ])
    query_builder = QueryBuilder(query_params)

    # when
    result = query_builder._join_related_entities_info()

    # then
    assert result == (
        'JOIN intranet_department AS department_join ON (department_join.id = summary_select.department_id)'
        ' JOIN intranet_department AS valuestream_join ON (valuestream_join.id = summary_select.value_stream_id)'
    )


def test_builder_selects_only_summary_on_absent_grouping():
    # given
    query_params = QueryParams.from_default_args()
    query_builder = QueryBuilder(query_params)

    # when
    result = query_builder._select()

    # then
    assert result == f'SELECT {query_builder._summary_alias}.*'


@pytest.mark.django_db
def test_builder_gives_query_that_returns_something(company):
    # given
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep1, value_stream=company.vs_root, geography=company.geo_russia)
    BudgetPositionAssignmentFactory(department=company.dep2, value_stream=company.vs_root, geography=company.geo_russia)
    query = QueryBuilder(QueryParams.from_default_args(
        groupings=[RelatedEntity.department],
        department_filters=[
            HierarchyValues(company.yandex.tree_id, company.yandex.lft, company.yandex.rght, company.yandex.id)
        ],
    )).build()

    cursor = connections[router.db_for_read(Department)].cursor()

    # when
    cursor.execute(query, tuple())

    # then
    result = cursor.fetchall()
    assert set([row[0] for row in result]) == {company.dep1.id, company.dep2.id}
