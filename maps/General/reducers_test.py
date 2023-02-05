from maps.poi.streetview_poi.sign_hypotheses.lib.ut.reducers_rows import (
    add_oid_ti_ft_poi_attr_rows,
    add_orgnames_rows,
    remove_duplicates_rows,
    pretty_res_without_verified_pos_rows
)

from maps.poi.streetview_poi.sign_hypotheses.lib.general import (
    add_oid_to_ft_poi_attr_reducer,
    add_columns_and_filter_with_orginfo_reducer,
)

from maps.poi.streetview_poi.sign_hypotheses.lib.one_sign import (
    remove_duplicates_reducer
)

from maps.poi.streetview_poi.sign_hypotheses.lib.filters import (
    get_pretty_results_without_verified_position_orgs_reducer
)


def reducer_test(reducer, rows):
    for row in rows:
        result = list(reducer(row['input']['keys'], row['input']['rows']))
        assert result == row['output']


def test_add_oid_to_ft_poi_attr_reducer():
    reducer_test(
        add_oid_to_ft_poi_attr_reducer,
        add_oid_ti_ft_poi_attr_rows
    )


def test_add_org_names_reducer():
    reducer_test(
        add_columns_and_filter_with_orginfo_reducer,
        add_orgnames_rows
    )


def test_remove_oid_duplicates():
    reducer_test(
        remove_duplicates_reducer,
        remove_duplicates_rows
        )


def test_get_sign_area_without_verified_position_orgs():
    reducer_test(
        get_pretty_results_without_verified_position_orgs_reducer,
        pretty_res_without_verified_pos_rows
    )
