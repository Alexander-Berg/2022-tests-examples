import pytest

import yatest

from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen
from maps.garden.modules.ymapsdf.lib.merge_poi import (
    constants as merge_poi_constants,
    graph as merge_poi,
)
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder
from maps.garden.modules.ymapsdf.lib.translation import translate as translation
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import (
    constants,
    ft_position_from_nmaps,
)

from test_helpers import (
    generate_input_table_resource,
    generate_table_resource,
)


FT_NEAR_BLD_SCHEMA = [{
    "name": "ft_id",
    "type": "int64",
    "required": True
},
{
    "name": "bld_id",
    "type": "int64",
    "required": True
}]


@pytest.mark.use_local_yt_yql
def test_all_tasks(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    translation.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    parent_finder.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    merge_poi.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    ft_position_from_nmaps.fill_graph(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR))

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        table_names=["bld_addr", "ft_addr", "ft_poi_attr"])

    test_utils.data.create_yt_resource(
        cook,
        constants.YT_MERGE_POI_STAGE.resource_name(
            "extra_poi_geoproduct_tmp",
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR),
        test_utils.ymapsdf.TEST_PROPERTIES,
        merge_poi_constants.EXTRA_POI_GEOPRODUCT_TMP_SCHEMA
    )

    test_utils.data.create_yt_resource(
        cook,
        constants.INPUT_STAGE.resource_name(
            "ft_near_bld",
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR),
        test_utils.ymapsdf.TEST_PROPERTIES,
        FT_NEAR_BLD_SCHEMA
    )

    test_utils.execute(cook)


@pytest.mark.use_local_yt_yql
def test_position_from_nmaps_calculation(environment_settings):
    data_dir = yatest.common.test_source_path("data_conflicts")
    task = ft_position_from_nmaps.ComputePoiWithPositionFromNmapsTask()
    task.load_environment_settings(environment_settings)

    output_resource = generate_table_resource(
        ft_position_from_nmaps._YMAPSDF_FT_POSITION_FROM_NMAPS, environment_settings)

    task(
        ft_near_bld=generate_input_table_resource(data_dir, "ft_near_bld", environment_settings, schema=FT_NEAR_BLD_SCHEMA),
        bld_addr=generate_input_table_resource(data_dir, "bld_addr", environment_settings),
        ft_addr=generate_input_table_resource(data_dir, "ft_addr", environment_settings),
        ft_poi_attr=generate_input_table_resource(data_dir, "ft_poi_attr", environment_settings),
        extra_poi_geoproduct_tmp=generate_input_table_resource(
            data_dir,
            "extra_poi_geoproduct_tmp",
            environment_settings,
            merge_poi_constants.EXTRA_POI_GEOPRODUCT_TMP_SCHEMA
        ),
        ft_position_from_nmaps=output_resource
    )
    output_resource.logged_commit()

    result = set([row["ft_id"] for row in output_resource.read_table()])
    ethalon_data = test_utils.data.read_table_from_file(data_dir, "ft_position_from_nmaps")
    ethalon = set([row["ft_id"] for row in ethalon_data])

    assert ethalon == result
