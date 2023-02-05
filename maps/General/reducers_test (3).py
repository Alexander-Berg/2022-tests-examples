from maps.poi.static_poi.buildings import (
    BuildingOrgsReducer,
    MergeOrgsFromBuildingsReducer
)
from maps.poi.static_poi.fresh_unpublished import AddFreshNotPublishedReasonReducer
from maps.poi.static_poi.merge_disp_classes import (
    AddTweakReducer,
    JoinMainDispClassesReducer
)
from maps.poi.static_poi.nyak_export import JoinWithAddrNeighborsReducer
from maps.poi.static_poi.organizations import OrgVisibilityReducer
from maps.poi.static_poi.poi import JoinDispClassesReducer
from maps.poi.static_poi.ymapsdf import (
    AddBuildingsReducer,
    AddPositionVerifiedReducer,
    OrgBuildingsReducer,
    OrgBuildingsFormatReducer,
    OrgsPositionVerifiedReducer
)

from reducers_rows import (
    add_buildings_reducer_rows,
    add_fresh_not_published_reason_reducer_rows,
    add_position_verified_reducer_rows,
    add_tweak_reducer_rows,
    building_orgs_reducer_rows,
    join_disp_classes_reducer_rows,
    join_main_disp_classes_reducer_rows,
    join_with_addr_neighbors_reducer_rows,
    merge_orgs_from_buildings_reducer_rows,
    org_buildings_reducer_rows,
    org_buildings_format_reducer_rows,
    org_visibility_reducer_rows,
    orgs_position_verified_reducer_rows
)


def reducer_test(reducer, rows):
    for row in rows:
        result = list(reducer(row['input']['keys'], row['input']['rows']))
        assert result == row['output']


def test_add_buildings_reducer():
    reducer_test(AddBuildingsReducer(), add_buildings_reducer_rows)


def test_add_position_verified_reducer():
    reducer_test(AddPositionVerifiedReducer(), add_position_verified_reducer_rows)


def test_add_fresh_not_published_reason_reducer():
    reducer_test(AddFreshNotPublishedReasonReducer(), add_fresh_not_published_reason_reducer_rows)


def test_building_orgs_reducer():
    reducer_test(BuildingOrgsReducer(), building_orgs_reducer_rows)


def test_join_disp_classes_reducer():
    reducer_test(JoinDispClassesReducer(), join_disp_classes_reducer_rows)


def test_join_main_disp_classes_reducer():
    reducer_test(JoinMainDispClassesReducer(), join_main_disp_classes_reducer_rows)


def test_join_with_addr_neighbors_reducer():
    reducer_test(JoinWithAddrNeighborsReducer(), join_with_addr_neighbors_reducer_rows)


def test_merge_orgs_from_buildings_reducer():
    reducer_test(MergeOrgsFromBuildingsReducer(), merge_orgs_from_buildings_reducer_rows)


def test_org_buildings_reducer():
    reducer_test(OrgBuildingsReducer(), org_buildings_reducer_rows)


def test_org_buildings_format_reducer():
    reducer_test(OrgBuildingsFormatReducer(), org_buildings_format_reducer_rows)


def test_org_visibility_reducer():
    reducer_test(OrgVisibilityReducer(), org_visibility_reducer_rows)


def test_orgs_position_verified_reducer():
    reducer_test(OrgsPositionVerifiedReducer(), orgs_position_verified_reducer_rows)


def test_add_tweak_reducer():
    for row in add_tweak_reducer_rows:
        result = list(AddTweakReducer()(row['input']['keys'], row['input']['rows']))
        assert len(result) == 1
        result = result[0]
        output = row['output'][0]

        assert 'base_disp_class' in result
        dc_check = result.pop('base_disp_class')
        dc_true = output.pop('base_disp_class')
        assert result == output
        if dc_true is None:
            assert dc_check is None
        else:
            assert abs(dc_check - dc_true) < 5e-3  # in case hash depends on platform
