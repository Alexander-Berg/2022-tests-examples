import pytest
import json

from maps.garden.sdk.core import AutotestsFailedError, Version
from maps.garden.sdk.resources import FlagResource
from maps.garden.sdk.yt.yt_table import YtTableResource

from maps.garden.modules.osm_borders_src import defs
from maps.garden.modules.osm_borders_src.lib.validation.countries_completeness import ValidateCountriesCompleteness


TEST_COUNTRIES_GEOM = [
    {"osm_id": 1, "isocode": "RU", "shape": "some_shape"},
    {"osm_id": 2, "isocode": "GB", "shape": "some_shape"},
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
            "vendor": "osm",
            "shipping_date": "20220324"
        }
    )
    input_resource.load_environment_settings(environment_settings)
    input_resource.write_table(TEST_COUNTRIES_GEOM)

    output_resource = FlagResource(
        name=defs.OSM_BORDERS_SRC.resource_name("countries_validation_flag")
    )

    def execute_task():
        test_task_executor.execute_task(
            task=ValidateCountriesCompleteness(),
            input_resources={
                "countries_geom": input_resource,
            },
            output_resources={
                "out_flag_resource": output_resource
            }
        )

    with pytest.raises(AutotestsFailedError):
        execute_task()

    mocker.patch(
        "maps.garden.modules.osm_borders_src.lib.validation.countries_completeness.resource.find",
        lambda _: json.dumps(TEST_REGIONS)
    )
    execute_task()
