import pytest
import json

from maps.garden.sdk.core import DataValidationWarning, Version
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.yt.yt_table import YtTableResource

from maps.garden.modules.ymapsdf_osm import defs
from maps.garden.modules.ymapsdf_osm.lib.validation.ad import ValidateCountriesCompleteness


TEST_AD_GOOD = [
    {"ad_id": 1, "level_kind": 1, "isocode": "RU"},
    {"ad_id": 2, "level_kind": 2, "isocode": "RU"},  # will be ignored
]

TEST_AD_WITH_EXTRA = [
    {"ad_id": 1, "level_kind": 1, "isocode": "RU"},
    {"ad_id": 2, "level_kind": 1, "isocode": "GB"},  # extra country for cis1 region
    {"ad_id": 3, "level_kind": 2, "isocode": "RU"},  # will be ignored
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
def test_countries_completeness(mocker, test_task_executor, environment_settings):
    input_resource = YtTableResource(
        name="resource_name",
        path_template="",
        server="hahn"
    )
    input_resource.version = Version(
        properties={
            "region": "cis1",
            "vendor": "osm",
            "shipping_date": "20220324"
        }
    )
    input_resource.load_environment_settings(environment_settings)
    input_resource.write_table(TEST_AD_GOOD)

    output_resource = FlagResource(
        name=defs.YMAPSDF_OSM.resource_name("ad_countries_validation_flag")
    )

    def execute_task():
        test_task_executor.execute_task(
            task=ValidateCountriesCompleteness(),
            input_resources={
                "ad": input_resource,
            },
            output_resources={
                "output_flag": output_resource
            }
        )

    with pytest.raises(DataValidationWarning):
        execute_task()

    mocker.patch(
        "maps.garden.modules.ymapsdf_osm.lib.validation.ad.resource.find",
        lambda _: json.dumps(TEST_REGIONS)
    )
    execute_task()

    input_resource.write_table(TEST_AD_WITH_EXTRA)
    with pytest.raises(DataValidationWarning):
        execute_task()
