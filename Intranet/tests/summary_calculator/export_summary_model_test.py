import pytest

from staff.budget_position.models import BudgetPositionAssignmentStatus
from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory

from staff.headcounts.headcounts_summary.headcounts_summary_calculator_new import HeadcountsSummaryCalculator
from staff.headcounts.headcounts_summary.export_summary_model import export_summary_model, Grouping, Name
from staff.headcounts.headcounts_summary.query_builder import QueryBuilder, QueryParams, RelatedEntity


@pytest.mark.django_db
def test_export_summary_model(company):
    # given
    BudgetPositionAssignmentFactory(
        department=company.dep1,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    BudgetPositionAssignmentFactory(
        department=company.dep2,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
    )
    params = QueryParams.from_default_args(groupings=[RelatedEntity.department])

    calculator = HeadcountsSummaryCalculator(QueryBuilder(params))

    # when
    results = list(export_summary_model(calculator.get_results(), params.groupings))

    # then
    assert len(results) == 4


@pytest.mark.django_db
def test_export_summary_model_hierarchy(company):
    # given
    BudgetPositionAssignmentFactory(
        department=company.dep1,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        value_stream=company.vs_1,
    )
    BudgetPositionAssignmentFactory(
        department=company.dep2,
        status=BudgetPositionAssignmentStatus.OCCUPIED.value,
        value_stream=company.vs_2,
    )
    params = QueryParams.from_default_args(groupings=[RelatedEntity.department, RelatedEntity.value_stream])

    calculator = HeadcountsSummaryCalculator(QueryBuilder(params))

    # when
    results = list(export_summary_model(calculator.get_results(), params.groupings))

    # then
    yandex_name = Name(name=company.yandex.name, name_en=company.yandex.name_en)
    dep1_name = Name(name=company.dep1.name, name_en=company.dep1.name_en)
    dep2_name = Name(name=company.dep2.name, name_en=company.dep2.name_en)
    vs_root_name = Name(name=company.vs_root.name, name_en=company.vs_root.name_en)
    vs_1_name = Name(name=company.vs_1.name, name_en=company.vs_1.name_en)
    vs_2_name = Name(name=company.vs_2.name, name_en=company.vs_2.name_en)

    assert results[0].hierarchy_by_groupings == []
    assert results[1].hierarchy_by_groupings == [Grouping(RelatedEntity.department, [yandex_name])]
    assert results[2].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name]),
    ]
    assert results[3].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name, vs_1_name]),
    ]
    assert results[4].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name, vs_2_name]),
    ]
    assert results[5].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name, dep1_name]),
    ]
    assert results[6].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name, dep1_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name]),
    ]
    assert results[7].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name, dep1_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name, vs_1_name]),
    ]
    assert results[8].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name, dep2_name]),
    ]
    assert results[9].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name, dep2_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name]),
    ]
    assert results[10].hierarchy_by_groupings == [
        Grouping(RelatedEntity.department, [yandex_name, dep2_name]),
        Grouping(RelatedEntity.value_stream, [vs_root_name, vs_2_name]),
    ]
