import pytest

from staff.lib.testing import DepartmentFactory

from staff.headcounts.forms import BudgetPositionAssignmentGroupingAndFiltersForm
from staff.headcounts.headcounts_summary.query_builder.query_params import RelatedEntity


def test_groupings_clean():
    # given
    data = {'groupings': [RelatedEntity.department.value, RelatedEntity.value_stream.value]}
    form = BudgetPositionAssignmentGroupingAndFiltersForm(data)

    # when
    result = form.is_valid()

    # then
    assert result
    assert form.cleaned_data['groupings'] == [RelatedEntity.department, RelatedEntity.value_stream]


def test_groupings_clean_empty():
    # given
    data = {'groupings': []}
    form = BudgetPositionAssignmentGroupingAndFiltersForm(data)

    # when
    result = form.is_valid()

    # then
    assert result


def test_groupings_clean_wrong():
    # given
    data = {'groupings': ['some']}
    form = BudgetPositionAssignmentGroupingAndFiltersForm(data)

    # when
    result = form.is_valid()

    # then
    assert not result


@pytest.mark.django_db
def test_departments_filter():
    department = DepartmentFactory()
    data = {'groupings': [RelatedEntity.department.value], 'department_filters': [department.url]}
    form = BudgetPositionAssignmentGroupingAndFiltersForm(data)

    # when
    result = form.is_valid()

    # then
    assert result


@pytest.mark.django_db
def test_departments_filter_wrong():
    data = {'groupings': [RelatedEntity.department.value], 'department_filters': ['something_wrong']}
    form = BudgetPositionAssignmentGroupingAndFiltersForm(data)

    # when
    result = form.is_valid()

    # then
    assert not result
