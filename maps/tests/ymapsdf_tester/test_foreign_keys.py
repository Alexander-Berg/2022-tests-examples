from unittest import mock
import pytest

from maps.libs.ymapsdf.py.schema import YmapsdfSchema

from maps.garden.sdk.core import GardenError, Version
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.yt import YtTableResource

from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen
from maps.garden.modules.ymapsdf.lib.filter_flats import filter_flats
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder as parent_finder_yt
from maps.garden.modules.ymapsdf.lib.translation import translate as translation
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import foreign_keys
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import constants

YT_SERVER = "hahn"

AD_SCHEMA = {
    "name": "ad",
    "columns": [
        {
            "name": "ad_id",
            "yt_type": "int64",
            "required": True
        },
        {
            "name": "p_ad_id",
            "yt_type": "int64",
            "required": False,
            "reference": {
                "table": "ad",
                "column": "ad_id"
            }
        }
    ],
    "key_columns": [
        "ad_id"
    ]
}

AD_CENTER_SCHEMA = {
    "name": "ad_center",
    "columns": [
        {
            "name": "ad_id",
            "yt_type": "int64",
            "required": True
        }
    ],
    "foreign_keys": [
        {
            "reference_table": "ad",
            "columns": {"ad_id": "ad_id"},
        }
    ],
    "key_columns": [
        "ad_id"
    ]
}


@pytest.mark.use_local_yt("hahn")
def test_all_ymapsdf_tables_foreign_keys(environment_settings):
    cook = test_utils.GraphCook(environment_settings)

    translation.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    parent_finder_yt.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    filter_flats.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    foreign_keys.fill_graph(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR))

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE)

    test_utils.execute(cook)


def _create_table(table_name, schema_dict, data, environment_settings):
    schema = YmapsdfSchema(schema_dict)

    yt_table = YtTableResource(
        table_name,
        path_template="/"+table_name,
        server=YT_SERVER)
    yt_table.version = Version()
    yt_table.load_environment_settings(environment_settings)
    yt_table.set_schema(schema.make_yt_schema())
    yt_table.update_attributes({
        "extended_schema": schema_dict
    })
    yt_table.write_table(data)
    yt_table.logged_commit()
    yt_table.calculate_size()
    return yt_table


@pytest.mark.use_local_yt("hahn")
@pytest.mark.parametrize(
    ("ad_data", "ad_center_data"), [
        (
            # Case: bad ad_center foreign_key
            [
                {"ad_id": 1, "p_ad_id": None},
                {"ad_id": 2, "p_ad_id": 1},
            ],
            [
                {"ad_id": 1},
                {"ad_id": 3},
            ]
        ),
        (
            # Case: bad ad (p_ad_id) foreign_key
            [
                {"ad_id": 1, "p_ad_id": None},
                {"ad_id": 2, "p_ad_id": 3},
            ],
            [
                {"ad_id": 1},
                {"ad_id": 2},
            ]
        )
    ]
)
@mock.patch("maps.libs.ymapsdf.py.ymapsdf.all_tables", return_value=["ad", "ad_center"])
def test_foreign_keys_errors(all_tables_mock, environment_settings, ad_data, ad_center_data):
    ad_table = _create_table("ad", AD_SCHEMA, ad_data, environment_settings)
    ad_center_table = _create_table("ad_center", AD_CENTER_SCHEMA, ad_center_data, environment_settings)

    output_resource = FlagResource("output_resource")
    output_resource.version = Version()
    output_resource.load_environment_settings(environment_settings)

    task = foreign_keys.CheckTableForeignKeys()
    task.load_environment_settings(environment_settings)
    with pytest.raises(GardenError) as exc_info:
        task(ad=ad_table, ad_center=ad_center_table, output_resource=output_resource)

    return str(exc_info.value)
