import yatest.common
import yt.wrapper as yt

from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import data as data_utils
from maps.garden.sdk.test_utils import ymapsdf

from maps.garden.sdk.yt import utils as yt_utils

from maps.garden.modules.altay.lib import altay
from maps.garden.modules.ymapsdf.lib.merge_poi import graph as merge_poi
from maps.garden.modules.ymapsdf.lib.ymapsdf_load import ymapsdf_load
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester.constants import FROM_NMAPS_POSITION_POI_DIR
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import ft_position_from_nmaps
from maps.garden.modules.extra_poi_bundle.lib import graph as extra_poi_bundle


ALTAY_EXPORT_SHIPPING_DATE = '20160910'
EXTRA_POI_BUNDLE_RELEASE_NAME = "20191010"


def create_cook(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    altay.fill_graph(cook.input_builder())
    extra_poi_bundle.fill_graph(cook.input_builder())
    ymapsdf_load.fill_graph(cook.input_builder(), ymapsdf.TEST_REGIONS)
    merge_poi.fill_graph(cook.target_builder(), ymapsdf.TEST_REGIONS)

    return cook


def create_ft_position_from_nmaps_table(environment_settings, fill_table=False):
    yt_settings = yt_utils.get_server_settings(
        yt_utils.get_yt_settings(environment_settings),
        server=next(iter(environment_settings["yt_servers"].keys())))
    yt_client = yt_utils.get_yt_client(yt_settings)
    garden_prefix = yt_utils.get_garden_prefix(yt_settings)

    ft_position_from_nmaps_dir = yt.ypath_join(garden_prefix, FROM_NMAPS_POSITION_POI_DIR, "first_run")
    yt_client.create(
        "map_node",
        ft_position_from_nmaps_dir,
        recursive=True,
        ignore_existing=True)
    if fill_table:
        rows = data_utils.read_table_from_file(
            yatest.common.test_source_path("tasks_data/tmp"),
            "ft_position_from_nmaps")
    else:
        rows = []
    yt_client.write_table(
        yt.TablePath(
            yt.ypath_join(ft_position_from_nmaps_dir, ymapsdf.TEST_REGION),
            attributes={"schema": ft_position_from_nmaps.FT_POSITION_FROM_NMAPS_SCHEMA}),
        rows,
        raw=False)
