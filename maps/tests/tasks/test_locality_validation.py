import pytest
import json

from maps.libs.ymapsdf.py.schema import YmapsdfSchema

from maps.garden.sdk.core import DataValidationWarning, Version
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.yt.yt_table import YtTableResource

from maps.garden.modules.ymapsdf_osm import defs
from maps.garden.modules.ymapsdf_osm.lib.constants import YmapsdfTable
from maps.garden.modules.ymapsdf_osm.lib.validation.locality import ValidateLocality


TEST_LOCALITY_GOOD = [
    {
        "ad_id": 1, "capital": 1,
        "town": False, "population_is_approximated": False, "municipality": False, "informal": False
    },
    {
        "ad_id": 10, "capital": 2,  # will be ignored, because not the capital
        "town": False, "population_is_approximated": False, "municipality": False, "informal": False
    },
]

TEST_LOCALITY_EXTRA = [
    {
        "ad_id": 1, "capital": 1,
        "town": False, "population_is_approximated": False, "municipality": False, "informal": False
    },
    {
        "ad_id": 2, "capital": 1,  # extra country capital for cis1 region
        "town": False, "population_is_approximated": False, "municipality": False, "informal": False
    },
    {
        "ad_id": 10, "capital": 2,  # will be ignored, because not the capital
        "town": False, "population_is_approximated": False, "municipality": False, "informal": False
    },
]

TEST_AD = [
    {"ad_id": 1, "level_kind": 2, "disp_class": 5, "isocode": "RU"},
    {"ad_id": 2, "level_kind": 2, "disp_class": 5, "isocode": "GB"},  # extra country for cis1 region
    {"ad_id": 10, "level_kind": 2, "disp_class": 5, "isocode": "RU"},
]

TEST_REGIONS = {
    "cis1": {
        "included_countries": ["RU"],
    },
    "eu3": {
        "included_countries": ["GB"],
    },
}


@pytest.mark.use_local_yt("hahn")
def test_capitals_completeness(mocker, test_task_executor, environment_settings):
    input_ad = YtTableResource(
        name="ad",
        path_template="",
        server="hahn",
        key_columns=["ad_id"],
        schema=YmapsdfSchema.for_table(YmapsdfTable.AD).make_yt_schema(sorted=True)
    )
    input_locality = YtTableResource(
        name="locality",
        path_template="",
        server="hahn",
        key_columns=["ad_id"],
        schema=YmapsdfSchema.for_table(YmapsdfTable.LOCALITY).make_yt_schema(sorted=True)
    )
    input_ad.version = Version(
        properties={
            "region": "cis1",
            "vendor": "osm",
            "shipping_date": "20220324"
        }
    )
    input_locality.version = input_ad.version

    input_ad.load_environment_settings(environment_settings)
    input_locality.load_environment_settings(environment_settings)

    input_ad.write_table(TEST_AD)
    input_locality.write_table(TEST_LOCALITY_GOOD)

    output_resource = FlagResource(
        name=defs.YMAPSDF_OSM.resource_name("locality_validated")
    )

    def execute_task():
        test_task_executor.execute_task(
            task=ValidateLocality(),
            input_resources={
                "ad": input_ad,
                "locality": input_locality,
            },
            output_resources={
                "output_flag": output_resource
            }
        )

    with pytest.raises(DataValidationWarning):
        execute_task()

    mocker.patch(
        "maps.garden.modules.ymapsdf_osm.lib.validation.locality.resource.find",
        lambda _: json.dumps(TEST_REGIONS)
    )
    execute_task()

    input_locality.write_table(TEST_LOCALITY_EXTRA)
    with pytest.raises(DataValidationWarning):
        execute_task()
