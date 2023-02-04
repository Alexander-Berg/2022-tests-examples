from staff.headcounts.headcounts_summary.hierarchy_levels_filter import HierarchyLevelsFilter
from staff.headcounts.headcounts_summary.query_builder.query_params import (
    QueryParams,
    RelatedEntity,
)
from staff.headcounts.headcounts_summary.query_builder.query_results import Result, GroupingInstanceInfo


def test_filter_departments():
    # given
    root_department = GroupingInstanceInfo.default()
    root_department.id = 1
    root_department.parent_id = None

    child_department = GroupingInstanceInfo.default()
    child_department.id = 2
    child_department.parent_id = root_department.id

    grand_child_department = GroupingInstanceInfo.default()
    grand_child_department.id = 3
    grand_child_department.parent_id = child_department.id

    result = Result.default()
    result.next_level_grouping = {
        root_department.id: Result.default(
            result_id=root_department.id,
            children={
                child_department.id: Result.default(
                    result_id=child_department.id,
                    children={
                        grand_child_department.id: Result.default(result_id=grand_child_department.id),
                    },
                ),
            },
        ),
    }
    query_params = QueryParams.from_default_args(groupings=[RelatedEntity.department])
    levels_filter = HierarchyLevelsFilter(query_params)

    # when
    levels_filter.filter(result)

    # then
    assert set(result.next_level_grouping.keys()) == {root_department.id}
    assert set(result.next_level_grouping[root_department.id].children.keys()) == {child_department.id}
    assert not result.next_level_grouping[root_department.id].children[child_department.id].children
