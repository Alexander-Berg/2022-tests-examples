import logging
import pytest

from maps.libs.ymapsdf.py.schema import YmapsdfSchema

from maps.garden.sdk.core import GardenError, Version
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.yt import YtTableResource
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen

from maps.garden.libs.ymapsdf.lib.ymapsdf_tester.constraints import CheckTableConstraints

from maps.garden.modules.ymapsdf.lib.filter_flats import filter_flats
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder as parent_finder_yt
from maps.garden.modules.ymapsdf.lib.translation import translate as translation
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import constraints
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import constants

logger = logging.getLogger('garden.tasks.ymapsdf_tester')

YT_SERVER = 'plato'


@pytest.mark.use_local_yt_yql
def test_all_ymapsdf_tables_constraints(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    translation.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    parent_finder_yt.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    filter_flats.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    constraints.fill_graph(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR))

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE)

    test_utils.execute(cook)


@pytest.mark.use_local_yt_yql
def test_data_errors(environment_settings):
    schema_dict = {
        "name": "ad",
        "columns": [
            {
                "name": "ad_id",
                "yt_type": "int64",
                "required": True
            },
            {
                "name": "isocode",
                "yt_type": "string",
                "required": True
            },
        ],
        "constraints": [
            {
                "type": "expression",
                "value": "ad_id >= 1 AND ad_id <= 10"
            },
            {
                "type": "regexp",
                "field": "isocode",
                "pattern": "[A-Z]{2}|[0-9]{3}"
            }
        ]
    }

    schema = YmapsdfSchema(schema_dict)

    yt_table = YtTableResource(
        "ad",
        path_template="/ad",
        server=YT_SERVER)
    yt_table.version = Version()
    yt_table.load_environment_settings(environment_settings)
    yt_table.set_schema(schema.make_yt_schema())
    yt_table.update_attributes({
        "extended_schema": schema_dict
    })
    yt_table.write_table([
        {"ad_id": 123, "isocode": "RU"},
        {"ad_id": 2, "isocode": "Hello"},
        {"ad_id": 3, "isocode": "RU"},
    ])
    yt_table.logged_commit()
    yt_table.calculate_size()

    output_resource = FlagResource("output_resource")
    output_resource.version = Version()
    output_resource.load_environment_settings(environment_settings)

    task = CheckTableConstraints(constants.make_nmaps_url)
    task.load_environment_settings(environment_settings)
    with pytest.raises(GardenError) as exc_info:
        task(yt_table, output_resource)

    return str(exc_info.value)


@pytest.mark.use_local_yt_yql
def test_no_extended_schema(environment_settings):
    """
    New ymapsdf tables may not have schema until a new export is deployed.
    """

    yt_table = YtTableResource(
        "yt_table",
        path_template="/yt_table",
        server=YT_SERVER)
    yt_table.version = Version()
    yt_table.load_environment_settings(environment_settings)
    yt_table.write_table([])
    yt_table.logged_commit()
    yt_table.calculate_size()

    output_resource = FlagResource("output_resource")
    output_resource.version = Version()
    output_resource.load_environment_settings(environment_settings)

    task = CheckTableConstraints(constants.make_nmaps_url)
    task.load_environment_settings(environment_settings)
    # Should not raise
    task(yt_table, output_resource)
