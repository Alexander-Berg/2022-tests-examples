import os
import pytest
import yatest.common

import yt.wrapper as yt

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.test_utils import ymapsdf
from maps.garden.sdk.yt import utils as yt_utils

from maps.garden.modules.altay import defs as altay_defs
from maps.garden.modules.altay.lib import altay
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.merge_poi import merge_ft_experiment
from maps.garden.modules.ymapsdf.lib.ymapsdf_load import ymapsdf_load
from maps.garden.modules.extra_poi_bundle.lib import graph as extra_poi_bundle

from maps.garden.modules.ymapsdf.lib.merge_poi import constants
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester.constants import FROM_NMAPS_POSITION_POI_DIR

ALTAY_EXPORT_SHIPPING_DATE = '20160910'
EXTRA_POI_BUNDLE_RELEASE_NAME = "20191010"

# FIXME: copy-pasted from merge_ft_experiment
EXTRA_POI_EXPERIMENTS_SCHEMA = [
    {
        "name": "id",
        "type": "int64",
        "required": True
    },
    {
        "name": "experiment",
        "type": "string",
        "required": True
    },
    {
        "name": "disp_class",
        "type": "int64",
        "required": True
    },
    {
        "name": "disp_class_tweak",
        "type": "double",
        "required": True
    },
    {
        "name": "disp_class_navi",
        "type": "int64",
        "required": True
    },
    {
        "name": "disp_class_tweak_navi",
        "type": "double",
        "required": True
    },
]


def _test_data_file_path(file_name, test_name):
    return yatest.common.test_source_path(
        os.path.join('data', test_name, file_name))


def _create_cook(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    altay.fill_graph(cook.input_builder())
    extra_poi_bundle.fill_graph(cook.input_builder())
    ymapsdf_load.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    merge_poi.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    return cook


def _create_altay_resources(cook, test_name):
    properties = {
        'shipping_date': ALTAY_EXPORT_SHIPPING_DATE
    }

    altay_tables = [
        altay_defs.COMPANIES,
        altay_defs.NAMES,
        altay_defs.DUPLICATES,
        altay_defs.REFERENCES,
        altay_defs.RUBRICS,
        altay_defs.COMPANIES_UNKNOWN
    ]

    for resource_name in altay_tables:
        filepath = _test_data_file_path(resource_name + ".jsonl", test_name)
        data_utils.create_yt_resource(cook, resource_name, properties, filepath=filepath)


def _create_input_resources(cook, test_name):
    ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        data_dir=yatest.common.test_source_path(os.path.join('data', test_name)))

    _create_altay_resources(cook, test_name)

    data_utils.create_yt_resource(
        cook,
        resource_name="extra_poi_all_data",
        properties={"release_name": EXTRA_POI_BUNDLE_RELEASE_NAME},
        filepath=_test_data_file_path("extra_poi_bundle.jsonl", test_name))

    data_utils.create_yt_resource(
        cook,
        resource_name="extra_poi_experiments",
        properties={"release_name": EXTRA_POI_BUNDLE_RELEASE_NAME},
        schema=EXTRA_POI_EXPERIMENTS_SCHEMA)


def _execute_graph(environment_settings, test_name):
    cook = _create_cook(environment_settings)

    _create_input_resources(cook, test_name)

    result = test_utils.execute(cook)

    # check YMapsDF shipping_date was propagated as expected
    output_resources = [resource for resource in result.values()
                        if resource.name.count('merge_poi') > 0]
    for resource in output_resources:
        assert ('shipping_date' in resource.properties and
                resource.properties['shipping_date'] == ymapsdf.TEST_SHIPPING_DATE)


def _run_test(environment_settings, request):
    test_name = request.function.__name__
    _execute_graph(environment_settings, test_name)
    yt_server = data_utils.get_yt_server(environment_settings)

    data_utils.validate_data(
        environment_settings,
        yt_server,
        constants.OUTPUT_STAGE,
        ymapsdf.TEST_PROPERTIES,
        os.path.join(yatest.common.test_source_path("data"), test_name + "_output"),
        constants.OUTPUT_TABLES
    )

    ymapsdf_schema.validate(
        environment_settings,
        yt_server,
        constants.OUTPUT_STAGE,
        ymapsdf.TEST_PROPERTIES,
        constants.OUTPUT_TABLES
    )


@pytest.mark.use_local_yt_yql
def test_base(environment_settings, request, monkeypatch):
    """
    Test case for base POIs (not protected, indoor, etc.).

    ft_id = 1
        Base, no reference, no 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 2
        Base, no reference, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 3
        Base, reference to reliably closed company, has 'org' source.
        Removed.
    ft_id = 4
        Base, reference to unreliably closed company, has 'org' source.
        From NMaps: non-'org' sources.
        From Altay: geometry, names, rubric, permalink, icon.
    ft_id = 5
        Base, in unknown companies list, has 'org' source.
        Removed.
    ft_id = 6
        Base, reference to opened company, no 'org' source.
        From NMaps: non-'org' sources.
        From Altay: geometry, names, rubric, permalink, icon.
    ft_id = 7
        Base, reference to opened company, has 'org' source to dangling permalink.
        From NMaps: non-'org' sources.
        From Altay: geometry, names, rubric, permalink, icon.
    ft_id = 8
        Base, reference to opened company, has 'org' source to head permalink.
        From NMaps: non-'org' sources.
        From Altay: geometry, names, rubric, permalink, icon.
    ft_id = 9
        Base, reference to opened company, has 'org' source to duplicate permalink.
        From NMaps: non-'org' sources.
        From Altay: geometry, names, rubric, permalink, icon.
    ft_id = 10
        Base, reference to opened company, no any ('org' and non-'org') source.
        From Altay: geometry, names, rubric, permalink, icon.
        No non-'org' sources.
    ft_id = 11
        Base, reference to opened company, no 'org' source, no icon in Altay.
        From NMaps: non-'org' sources.
        From Altay: geometry, names, rubric, permalink.
        No icon.

    ft_id = 12
        Base, reference to opened company, has official and render names in NMaps.
        Render names are replaced by names from Altay.
    ft_id = 13
        Base, reference to opened company, has official names in NMaps.
        Render names are added from Altay.
    ft_id = 14
        Base, reference to opened company, has official names in NMaps equal to Altay.
        No names are added from Altay.
    ft_id = 15
        Base, reference to opened company, has official russian name in NMaps.
        Official english name is added from Altay.
    ft_id = 16
        Base, reference to opened company, has no names in NMaps.
        No names are added from Altay (in this case they equal to rubric name).
        Official english name is added from Altay.

    ft_id = 41
        Like ft_id = 4 but present in table ft_position_from_nmaps
        Take coordinates from NMaps
    ft_id = 50
        Dangling ft_id in references.
        Ignored.
    ft_id = 10000
        Area with parent ft_id = 1.
        Left by itself, but got p_ft_id = NULL.
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    # create ft_position_from_nmaps table
    yt_server = data_utils.get_yt_server(environment_settings)
    yt_server_settings = yt_utils.get_server_settings(
        yt_utils.get_yt_settings(environment_settings),
        server=yt_server)
    yt_client = yt_utils.get_yt_client(yt_server_settings)
    yt_garden_prefix = yt_utils.get_garden_prefix(yt_server_settings)

    from_nmaps_poi_dir = yt.ypath_join(yt_garden_prefix, FROM_NMAPS_POSITION_POI_DIR)
    yt_client.remove(from_nmaps_poi_dir, recursive=True, force=True)
    from_nmaps_poi_table_path = yt.ypath_join(from_nmaps_poi_dir, "latest", ymapsdf.TEST_REGION)
    data_utils.create_table(
        yt_client,
        from_nmaps_poi_table_path,
        [{
            "name": "ft_id",
            "type": "int64",
            "required": True
        }],
        _test_data_file_path("ft_position_from_nmaps.jsonl", request.function.__name__)
    )

    _run_test(environment_settings, request)


@pytest.mark.use_local_yt_yql
def test_protected(environment_settings, request, monkeypatch):
    """
    Test case for POIs protected by ft_type_id.

    ft_id = 1
        Protected, no reference, no 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 2
        Protected, no reference, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 3
        Protected, reference to reliably closed company, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: permalink, icon.
    ft_id = 4
        Protected, reference to unreliably closed company, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: permalink, icon.
    ft_id = 5
        Protected, in unknown companies list, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 6
        Protected, reference to opened company, no 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: permalink, icon.
    ft_id = 7
        Protected, reference to opened company, has 'org' source to dangling permalink.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        No icon.
    ft_id = 8
        Protected, reference to opened company, has 'org' source to head permalink.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: permalink, icon.
    ft_id = 9
        Protected, reference to opened company, has 'org' source to duplicate permalink.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: permalink, icon.
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    _run_test(environment_settings, request)


@pytest.mark.use_local_yt_yql
def test_keep_nmaps_permalink(environment_settings, request, monkeypatch):
    """
    Test case for POIs with ft_type_id from 'keep NMaps permalink' list.

    ft_id = 1
        Keep NMaps permalink, no reference, no 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 2
        Keep NMaps permalink, no reference, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        From Altay: icon (by permalink from NMaps).
    ft_id = 3
        Keep NMaps permalink, reference to reliably closed company, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        From Altay: icon (by permalink from NMaps).
    ft_id = 4
        Keep NMaps permalink, reference to unreliably closed company, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        From Altay: icon (by permalink from NMaps).
    ft_id = 5
        Keep NMaps permalink, in unknown companies list, has 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        From Altay: icon (by permalink from NMaps).
    ft_id = 6
        Keep NMaps permalink, reference to opened company, no 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: permalink, icon.
    ft_id = 7
        Keep NMaps permalink, reference to opened company, has 'org' source to dangling permalink.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        No icon.
    ft_id = 8
        Keep NMaps permalink, reference to opened company, has 'org' source to head permalink.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        From Altay: icon (by permalink from NMaps).
    ft_id = 9
        Keep NMaps permalink, reference to opened company, has 'org' source to duplicate permalink.
        From NMaps: geometry, names, rubric, non-'org' sources.
        From Altay: head permalink (for duplicate one in NMaps), icon.
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    _run_test(environment_settings, request)


@pytest.mark.use_local_yt_yql
def test_extra_poi(environment_settings, request, monkeypatch):
    """
    Test case for extra POIs.

    ext_poi_permalink = 100
        Head permalink, reference exists, disp_class lesser.
        Both display classes are updated.
    ext_poi_permalink = 200
        Head permalink, reference exists, disp_class greater.
        Both display classes are NOT updated.
    ext_poi_permalink = 350
        Duplicate permalink, reference exists, disp_class lesser (tweak based), disp_class_navi greater.
        Display class is updated.
    ext_poi_permalink = 450
        Duplicate permalink, reference exists, disp_class greater, disp_class_navi lesser (tweak based).
        Display class navi is updated.
    ext_poi_permalink = 500
        Head permalink, reference does not exist.
        Added new POI object.
    ext_poi_permalink = 600
        Dangling ft_type_id.
        Ignored.
    ext_poi_permalink = 700
        Head permalink, reference exists, disp_class lesser, NMaps disp_class = 10
        Ignored, set disp_class_navi = 10.
    ext_poi_permalink = 800
        Head permalink, reference does not exist, from AB-experiment (disp_class*=10, search_class=10)
        Added new POI object.
    ext_poi_permalink = 900
        Head permalink, reference exists, disp_class greater, source_type = 4, 9 (exported from extra POI)
        Both display classes are updated.
    ext_poi_permalink = 1000
        Head permalink, reference exists, disp_class = 10, source_type = 4, 9 (exported from extra POI)
        Both display classes are left unchanged despite being exported from extra POI.
    ext_poi_permalink = 1100
        Head permalink, reference exists, disp_class = 6, source_type = 4, 9 (exported from extra POI).
        Verified position NMaps but disp_class = 10 in extra POI.
        Both display classes are left unchanged.
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    _run_test(environment_settings, request)


@pytest.mark.use_local_yt_yql
def test_indoor(environment_settings, request, monkeypatch):
    """
    Test case for POIs used in indoor plans.

    ft_id = 1
        Indoor, no reference, no 'org' source.
        From NMaps: geometry, names, rubric, non-'org' sources.
        No permalink and icon.
    ft_id = 2
        Indoor, no reference, has 'org' source.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        From Altay: names, icon (by permalink from NMaps).
    ft_id = 3
        Indoor, reference to reliably closed company, has 'org' source.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        Removed.
    ft_id = 4
        Indoor, reference to unreliably closed company, has 'org' source.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        From Altay: names, icon (by permalink from NMaps).
    ft_id = 5
        Indoor, in unknown companies list, has 'org' source.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        Removed.
    ft_id = 6
        Indoor, reference to opened company, no 'org' source.
        From NMaps: geometry, rubric, non-'org' sources.
        From Altay: names, permalink, icon.
    ft_id = 7
        Indoor, reference to opened company, has 'org' source to dangling permalink.
        From NMaps: geometry, names, rubric, non-'org' sources, permalink.
        No icon.
    ft_id = 8
        Indoor, reference to opened company, has 'org' source to head permalink.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        From Altay: names, icon (by permalink from NMaps).
    ft_id = 9
        Indoor, reference to opened company, has 'org' source to duplicate permalink.
        From NMaps: geometry, rubric, non-'org' sources.
        From Altay: names, head permalink (for duplicate one in NMaps), icon.

    ft_id = 10, ft_id = 11
        Indoor, overground/underground level, no reference.
        Have 'org' sources to the same head permalink of extra POI with lesser disp_class.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        From extra POIs: display classes for the POI on the overground level (by permalink from NMaps).
        From Altay: names, icon (by permalink from NMaps).
    ft_id = 12, ft_id = 13
        Indoor, overground/underground level, no reference.
        Have 'org' sources to the same duplicate permalink of extra POI with lesser disp_class.
        From NMaps: geometry, rubric, non-'org' sources, permalink.
        From extra POIs: head permalink (for duplicate one in NMaps),
            display classes for the POI on the overground level.
        From Altay: names, icon (by head permalink from extra POIs).
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    _run_test(environment_settings, request)


@pytest.mark.use_local_yt_yql
def test_multiple(environment_settings, request, monkeypatch):
    """
    Test case for multiple NMaps/Altay references.
    TODO: https://st.yandex-team.ru/NMAPS-9446

    ft_id = 1, ft_id = 2  <--->  permalink = 100
        Both are unprotected - both are left.
    ft_id = 3, ft_id = 4  <--->  permalink = 300
        Protected and unprotected - both are left.
    ft_id = 5, ft_id = 6  <--->  permalink = 500
        Both are protected - both are left.

    ft_id = 7, ft_id = 8  <--->  permalink = 700
        Indoor and unprotected - both are left.
    ft_id = 9, ft_id = 10  <--->  permalink = 900
        Indoor and protected - both are left.
    ft_id = 11, ft_id = 12  <--->  permalink = 1100
        Both are indoor - both are left.

    ft_id = 13  <--->  permalink = 1200, permalink = 1300
        Only first permalink is left.
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    _run_test(environment_settings, request)


@pytest.mark.use_local_yt_yql
def test_poi_attr(environment_settings, request, monkeypatch):
    """
    Test case for attributes of POIs which have altay reference
    See details in https://st.yandex-team.ru/MAPSGARDEN-18270

    ft_id = 1  <--->  permalink = 100
        Delivery, closed for visitors.
    ft_id = 2  <--->  permalink = 200
        No altay features, opened.
    ft_id = 3  <--->  permalink = 300
        Delivery, permanently closed.
    ft_id = 4  <--->  permalink = 400
        Home visit, temporarily closed, is in extra_poi_bundle.
    ft_id = 5  <--->  permalink = 500
        Mobile service, opened.
    ft_id = 6  <--->  permalink = 600
        Delivery and pickup, opened.
    ft_id = 7  <--->  permalink = 700
        Takeaway, opened.

    ft_id = 9  <--->  permalink = 800
        Delivery and pickup, closed for visitors.
        Is in extra_poi_bundle but not in ft (reference is created).
    """
    monkeypatch.setattr(merge_ft_experiment, "POSTPROCESSING", {})
    _run_test(environment_settings, request)
