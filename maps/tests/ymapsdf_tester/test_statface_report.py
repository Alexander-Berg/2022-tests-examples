import pytest
import json

from itertools import product

from maps.garden.sdk.yt import utils as yt_utils
from maps.garden.sdk.core import Version

from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk.extensions import resource_namer
from maps.garden.modules.ymapsdf.lib.parent_finder import (
    parent_finder as parent_finder_yt
)
from maps.garden.modules.ymapsdf.lib.translation import (
    translate as translation
)
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import (
    row_statistics,
    ft_geom_conflicts,
    statface_report
)


YT_SERVER = "plato"

TEST_ROW_COUNT_DATA = {
    "edge": 2048,
    "ft_geom": {
        "hydro-bay": 512,
        "hydro-fountain": 256
    },
    "extra_poi": {
        "landmark": 53,
    },
    "ft_in": {
        "landmark": 53,
    },
}

TEST_CONFLICTED_POI_COUNTS = {
    ft_geom_conflicts.conflicts_column_name(zoom, prefix): 1
    for zoom, prefix in product(
        ft_geom_conflicts.CONFLICT_ZOOMS,
        ("", "export_", "indoor_", "recent_"))
}
TEST_CONFLICTED_POI_COUNTS.update({
    ft_geom_conflicts.conflicts_column_name(zoom, "yz_"): 2
    for zoom in ft_geom_conflicts.CONFLICT_ZOOMS
})

TEST_EXPECTED_CONFLICTS_REPORT_OUTPUT = {
    statface_report.report_column_name(zoom, prefix): 1
    for zoom, prefix in product(
        ft_geom_conflicts.CONFLICT_ZOOMS,
        ("", "export_", "indoor_", "recent_"))
}
TEST_EXPECTED_CONFLICTS_REPORT_OUTPUT.update({
    statface_report.report_column_name(zoom, conflict_type="yz"): 2
    for zoom in ft_geom_conflicts.CONFLICT_ZOOMS
})

TEST_EXPECTED_REPORT_OUTPUT = [
    {
        "fielddate": "2020-01-02",
        "tablepath_tree": f"\t{test_utils.ymapsdf.TEST_REGION}\t",
        "row_count": 2816,
        **TEST_EXPECTED_CONFLICTS_REPORT_OUTPUT
    }
] + [
    {
        "fielddate": "2020-01-02",
        "tablepath_tree": path,
        "row_count": row_count
    }
    for path, row_count in [
        (f"\t{test_utils.ymapsdf.TEST_REGION}\tedge\t", 2048),
        (f"\t{test_utils.ymapsdf.TEST_REGION}\tft_geom\t", 768),
        (f"\t{test_utils.ymapsdf.TEST_REGION}\tft_geom\thydro-bay\t", 512),
        (f"\t{test_utils.ymapsdf.TEST_REGION}\tft_geom\thydro-fountain\t", 256)
    ]
]


@pytest.fixture
def yt_settings(environment_settings):
    return yt_utils.get_server_settings(
        yt_utils.get_yt_settings(environment_settings),
        server=YT_SERVER)


@pytest.fixture
def yt_client(yt_settings):
    return yt_utils.get_yt_client(yt_settings)


def generate_resource(cook, resource_name):
    resource_name = resource_namer.get_full_resource_name(
        resource_name,
        test_utils.ymapsdf.TEST_REGION,
        test_utils.ymapsdf.TEST_VENDOR)

    resource = cook.create_input_resource(resource_name)
    resource.version = Version(properties={
        "shipping_date": test_utils.ymapsdf.TEST_SHIPPING_DATE,
        "region": test_utils.ymapsdf.TEST_REGION
    })
    resource.server = YT_SERVER
    resource.load_environment_settings(cook.environment_settings)

    return resource


def generate_empty_json_resource(yt_client, cook, resource_name):
    resource = generate_resource(cook, resource_name)

    yt_client.create(
        "file", path=resource.path, recursive=True, ignore_existing=True)
    yt_client.write_file(resource.path, json.dumps({}).encode())

    return resource


def generate_conflicts_count_resource(cook, resource_name):
    resource = generate_resource(cook, resource_name)

    resource.write_table(
        [TEST_CONFLICTED_POI_COUNTS])

    resource.logged_commit()
    resource.calculate_size()

    return resource


@pytest.mark.use_local_yt_yql
def test_graph(yt_client, environment_settings):
    environment_settings["statface"] = {"token": "KEY"}

    cook = test_utils.GraphCook(environment_settings)

    translation.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    parent_finder_yt.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    input_builder = mutagen.create_region_vendor_mutagen(
        cook.input_builder(),
        test_utils.ymapsdf.TEST_REGION,
        test_utils.ymapsdf.TEST_VENDOR)

    row_statistics.fill_graph(input_builder)
    ft_geom_conflicts.fill_graph(input_builder)

    statface_report.fill_graph(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR))

    generate_empty_json_resource(
        yt_client,
        cook,
        row_statistics.YMAPSDF_ROW_STATISTICS)

    generate_conflicts_count_resource(
        cook,
        ft_geom_conflicts.YMAPSDF_CONFLICTS_COUNT)

    test_utils.execute(cook)


def test_statface_report_generator():
    report = statface_report.generate_statface_report(
        test_utils.ymapsdf.TEST_SHIPPING_DATE, test_utils.ymapsdf.TEST_REGION,
        TEST_ROW_COUNT_DATA,
        TEST_CONFLICTED_POI_COUNTS)

    report = sorted(report, key=lambda x: x["row_count"], reverse=True)

    assert report == TEST_EXPECTED_REPORT_OUTPUT
