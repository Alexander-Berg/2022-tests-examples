from maps.poi.personalized_poi.builder.lib.reducers import (
    UpdateColumnReducer,
    UpdateJoinColumnReducer,
    JoinOldCompanyTableReducer,
    JoinYmapsdfPositionsReducer,
    AddColumnReducer,
    EcstaticNamesReducer,
    EcstaticOrgsAndFtNamesReducer,
    MergeNearRegulargeoReducer,
    MergePoiDataReducer,
    PreparePoisToUserReducer,
    RelevanceMaxCountReducer,
    CoordinatesIndexerReducer,
    CoordinatesIndexerGrouper,
    MakeRequestConcatAllReducer,
    BebrUserPoiStatisticsReducer,
)

from maps.poi.personalized_poi.builder.lib.ut.reducers_rows import (
    update_column_reducer_rows,
    update_join_column_reducer_rows,
    join_old_company_table_reducer_rows,
    join_ymapsdf_positions_reducer_rows,
    add_column_reducer_with_default_rows,
    add_column_reducer_without_default_rows,
    ecstatic_names_reducer_rows,
    ecstatic_orgs_and_ft_names_reducer_rows,
    merge_near_regulargeo_reducer_rows,
    merge_poi_data_reducer_rows,
    prepare_pois_to_user_reducer_rows,
    relevance_max_count_reducer_rows,
    coordinates_indexer_reducer_for_oid_rows,
    coordinates_indexer_reducer_for_puid_rows,
    coordinates_indexer_grouper_rows,
    make_request_concat_all_reducer_rows,
    analytics_bebr_logs_reducer_rows,
)

from maps.poi.personalized_poi.builder.lib.reducers_helpers import get_rendered_zooms


def reducer_test(reducer, rows):
    for row in rows:
        result = list(reducer(row['input']['key'], row['input']['rows']))
        assert result == row['output']


def test_update_column_reducer():
    reducer_test(UpdateColumnReducer('info'), update_column_reducer_rows)


def test_update_join_column_reducer():
    reducer_test(UpdateJoinColumnReducer('info'), update_join_column_reducer_rows)


def test_join_old_company_reducer():
    reducer_test(JoinOldCompanyTableReducer('2021-01-01'), join_old_company_table_reducer_rows)


def test_join_ymapsdf_positions_reducer():
    reducer_test(JoinYmapsdfPositionsReducer(), join_ymapsdf_positions_reducer_rows)


def test_add_column_reducer_with_default():
    reducer_test(
        AddColumnReducer('value', 'key'),
        add_column_reducer_with_default_rows
    )


def test_add_column_reducer_without_default():
    reducer_test(
        AddColumnReducer('value'),
        add_column_reducer_without_default_rows
    )


def test_ecstatic_names_reducer():
    reducer_test(EcstaticNamesReducer(), ecstatic_names_reducer_rows)


def test_ecstatic_orgs_and_ft_names_reducer():
    reducer_test(EcstaticOrgsAndFtNamesReducer(), ecstatic_orgs_and_ft_names_reducer_rows)


def test_prepare_pois_to_user_reducer():
    reducer_test(PreparePoisToUserReducer(''), prepare_pois_to_user_reducer_rows)


def test_merge_near_regulargeo_reducer():
    reducer_test(MergeNearRegulargeoReducer(100), merge_near_regulargeo_reducer_rows)


def test_merge_poi_data_reducer():
    reducer_test(MergePoiDataReducer(), merge_poi_data_reducer_rows)


def test_relevance_max_count_reducer():
    reducer_test(RelevanceMaxCountReducer(2), relevance_max_count_reducer_rows)


def test_coordinates_indexer_reducer_for_oid():
    reducer_test(
        CoordinatesIndexerReducer(10, additional_info=get_rendered_zooms),
        coordinates_indexer_reducer_for_oid_rows
    )


def test_coordinates_indexer_reducer_for_puid():
    reducer_test(
        CoordinatesIndexerReducer(10, main_id='puid', main_is_near=False),
        coordinates_indexer_reducer_for_puid_rows
    )


def test_coordinates_indexer_grouper():
    reducer_test(
        CoordinatesIndexerGrouper(),
        coordinates_indexer_grouper_rows
    )


def test_make_request_concat_all_reducer():
    reducer_test(MakeRequestConcatAllReducer(), make_request_concat_all_reducer_rows)


def test_analytics_bebr_logs_mapper():
    reducer_test(
        BebrUserPoiStatisticsReducer({'click', 'poi_show'}, {'desktop'}),
        analytics_bebr_logs_reducer_rows)
