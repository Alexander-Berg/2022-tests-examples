from typing import Any, Dict, List, Optional, Tuple

import attr

from staff.headcounts.headcounts_summary.query_builder import (
    Aliases,
    RelatedEntity,
    RelatedEntityChains,
    SummaryResults,
    ResultsMapper,
    RowPath,
)


def _make_row(groupings: List[RelatedEntity], full_path: Tuple[Optional[int], ...], **kwargs) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    for field in attr.fields(SummaryResults):
        if field.name in kwargs:
            result[field.name] = kwargs[field.name]
        else:
            result[field.name] = 0

    for idx, grouping in enumerate(groupings):
        node_id_field = grouping.pk_field_name
        result[node_id_field] = full_path[idx]

    return result


def test_row_path_for_empty_groupings() -> None:
    # given
    groupings: List[RelatedEntity] = []
    row = _make_row(groupings, tuple())
    row_path = RowPath(Aliases(), groupings, row)

    # when
    result = row_path.has_next_grouping()

    # then
    assert not result


def test_row_path_for_absent_groupings_in_row() -> None:
    # given
    groupings = [RelatedEntity.department]
    row = _make_row(groupings, (None, None))
    row_path = RowPath(Aliases(), groupings, row)

    # when
    result = row_path.has_next_grouping()

    # then
    assert not result


def test_empty_rows() -> None:
    # given
    groupings = [RelatedEntity.department, RelatedEntity.value_stream]
    mapper = ResultsMapper(groupings, Aliases(), RelatedEntityChains())

    # when
    mapper.map_to_result([])

    # then
    assert not mapper.root.next_level_grouping
    assert not mapper.root.children


def test_one_level_grouping_with_child_departments() -> None:
    # given
    groupings = [RelatedEntity.department]
    chains = RelatedEntityChains()
    chains.set_chains(RelatedEntity.department, {
        1: [1],
        2: [1, 2],
        3: [1, 3],
        4: [4],
    })
    mapper = ResultsMapper(groupings, Aliases(), chains)
    rows = [
        _make_row(groupings, (1, )),
        _make_row(groupings, (2, )),
        _make_row(groupings, (3, )),
        _make_row(groupings, (4, )),
    ]

    # when
    mapper.map_to_result(rows)

    # then
    assert mapper.root.next_level_grouping
    assert 1 in mapper.root.next_level_grouping
    assert 2 in mapper.root.next_level_grouping[1].children
    assert 3 in mapper.root.next_level_grouping[1].children
    assert 4 in mapper.root.next_level_grouping


def test_two_level_grouping_with_child_departments_and_child_geo() -> None:
    # given
    groupings = [RelatedEntity.department, RelatedEntity.geography]
    chains = RelatedEntityChains()
    chains.set_chains(RelatedEntity.department, {
        1: [1],
        7: [1, 7],
        8: [1, 8],
        9: [1, 9],
        4: [4],
        6: [6],
    })
    chains.set_chains(RelatedEntity.geography, {
        2: [2],
        3: [2, 3],
        5: [2, 5],
    })
    mapper = ResultsMapper(groupings, Aliases(), chains)
    parent_department_1 = 1
    child_department_1_7 = 7
    child_department_1_8 = 8
    child_department_1_9 = 9
    parent_department_6 = 6
    parent_geo_2 = 2
    child_geo_3 = 3
    child_geo_5 = 5

    rows = [
        _make_row(groupings, (None, None)),
        _make_row(groupings, (parent_department_1, parent_geo_2)),
        _make_row(groupings, (parent_department_1, child_geo_3)),
        _make_row(groupings, (parent_department_1, child_geo_5)),
        _make_row(groupings, (parent_department_1, None)),
        _make_row(groupings, (child_department_1_7, None)),
        _make_row(groupings, (child_department_1_8, None)),
        _make_row(groupings, (child_department_1_8, parent_geo_2)),
        _make_row(groupings, (child_department_1_9, child_geo_3)),
        _make_row(groupings, (parent_department_6, None)),
    ]

    # when
    mapper.map_to_result(rows)

    # then
    assert mapper.root.next_level_grouping
    assert parent_department_1 in mapper.root.next_level_grouping
    parent_department_1_node = mapper.root.next_level_grouping[parent_department_1]
    assert parent_geo_2 in parent_department_1_node.next_level_grouping
    assert child_geo_3 in parent_department_1_node.next_level_grouping[parent_geo_2].children
    assert child_geo_5 in parent_department_1_node.next_level_grouping[parent_geo_2].children
    assert parent_geo_2 in parent_department_1_node.children[child_department_1_8].next_level_grouping

    child_department_1_9_node = parent_department_1_node.children[child_department_1_9]
    assert child_geo_3 in child_department_1_9_node.next_level_grouping[parent_geo_2].children

    assert child_department_1_7 in mapper.root.next_level_grouping[parent_department_1].children
    assert child_department_1_9 in mapper.root.next_level_grouping[parent_department_1].children
    assert parent_department_6 in mapper.root.next_level_grouping
