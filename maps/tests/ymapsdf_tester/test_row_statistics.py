import json
import logging
import pytest
from yt.wrapper import ypath_join
import yatest

from maps.pylibs.utils.lib import json_utils

from maps.garden.sdk.core import DataValidationWarning
from maps.garden.sdk import test_utils
from maps.garden.sdk.extensions import mutagen
from maps.garden.sdk.yt import utils as yt_utils

from maps.garden.modules.ymapsdf.lib.merge_poi import (
    constants as merge_poi_constants,
    graph as merge_poi,
)
from maps.garden.modules.ymapsdf.lib.filter_flats import filter_flats
from maps.garden.modules.ymapsdf.lib.parent_finder import parent_finder as parent_finder_yt
from maps.garden.modules.ymapsdf.lib.translation import translate as translation
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import row_statistics
from maps.garden.modules.ymapsdf.lib.ymapsdf_load import ymapsdf_load
from maps.garden.modules.ymapsdf.lib.ymapsdf_tester import constants

logger = logging.getLogger('garden.tasks.ymapsdf_tester')


_YMAPSDF_OLD_SHIPPING_DATE = '20160908'
_YT_SERVER = 'plato'

_ROW_STATISTICS = {
    "ft": {
        "landmark": 1,
        "landmark-milestone": 1,
        "indoor-level": 1,
        "urban-service-barbershop": 1
    },
    "ft_in": {
        "landmark": 1,
        "landmark-milestone": 1,
        "indoor-level": 1,
        "urban-service-barbershop": 1
    },
    "ft_face": {
        "landmark-milestone": 1
    },
    "ft_type": 4,
    "ft_addr": {
        "landmark": 1,
        "landmark-milestone": 1,
        "indoor-level": 1
    },
    "ft_source": {
        "landmark": 2,
        "landmark-milestone": 1,
        "indoor-level": 2
    },
    "extra_poi": {
        "urban-service-barbershop": 1
    },
    "bld_addr": 6,
}


def _row_statistics_path(garden_prefix, shipping_date):
    output_resource_path = row_statistics._STAT_OUTPUT_RESOURCE_FILEPATH.format(
        shipping_date=shipping_date, region=test_utils.ymapsdf.TEST_REGION)
    return ypath_join(garden_prefix, output_resource_path)


def _write_old_row_statistics(test_yt_path_for_old_data, yt_client, old_data):
    yt_client.create('file', path=test_yt_path_for_old_data, recursive=True, ignore_existing=True)
    yt_client.write_file(test_yt_path_for_old_data, json_utils.pretty_format(old_data).encode())


def _make_table_path(prefix: str, table_name: str, shipping_date: str, stage: str) -> str:
    return ypath_join(
        prefix,
        f"ymapsdf/{shipping_date}/{test_utils.ymapsdf.TEST_REGION}_yandex/{stage}/{table_name}"
    )


@pytest.fixture
def garden_prefix(environment_settings):
    yt_settings = yt_utils.get_server_settings(
        yt_utils.get_yt_settings(environment_settings),
        server=_YT_SERVER)
    return yt_utils.get_garden_prefix(yt_settings)


@pytest.fixture
def graph_cook(environment_settings, yt_client, garden_prefix):
    test_yt_path = ypath_join(garden_prefix, constants.BASE_STATISTICS_DIR)
    yt_client.remove(test_yt_path, recursive=True, force=True)

    cook = test_utils.GraphCook(environment_settings)

    ymapsdf_load.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    merge_poi.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    translation.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    parent_finder_yt.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    filter_flats.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)

    row_statistics.fill_graph(
        mutagen.create_region_vendor_mutagen(
            cook.target_builder(),
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR))

    test_utils.ymapsdf.create_resources(
        cook,
        stage=constants.INPUT_STAGE,
        data_dir=yatest.common.test_source_path("data_row_statistics"))

    test_utils.data.create_yt_resource(
        cook,
        constants.ALPHA_STAGE.resource_name(
            "ft",
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR),
        properties=test_utils.ymapsdf.TEST_PROPERTIES,
        schema=test_utils.ymapsdf_schema.YmapsdfSchemaManager().schema_for_table("ft").make_yt_schema(),
        filepath=yatest.common.test_source_path("data_row_statistics/alpha_ft.jsonl"),
    )

    test_utils.data.create_yt_resource(
        cook,
        constants.YT_MERGE_POI_STAGE.resource_name(
            "extra_poi_tmp",
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR),
        properties=test_utils.ymapsdf.TEST_PROPERTIES,
        schema=merge_poi_constants.EXTRA_POI_TMP_SCHEMA,
        filepath=yatest.common.test_source_path("data_row_statistics/extra_poi_tmp.jsonl"),
    )

    return cook


@pytest.fixture
def upload_to_sandbox_mock(mocker):
    return mocker.patch(
        "maps.garden.modules.ymapsdf.lib.ymapsdf_tester.row_statistics.upload_report_to_sandbox",
        return_value="https://sandbox/12345")


@pytest.mark.use_local_yt_yql
def test_fail_expectation(upload_to_sandbox_mock, graph_cook, yt_client, garden_prefix):
    test_yt_path_for_old_data = _row_statistics_path(garden_prefix, _YMAPSDF_OLD_SHIPPING_DATE)

    old_row_statistics = {
        "ft_type": 123456
    }
    _write_old_row_statistics(test_yt_path_for_old_data, yt_client, old_row_statistics)

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    assert isinstance(exc_info.value.original_exception, DataValidationWarning)
    assert 1 == upload_to_sandbox_mock.call_count
    (report, module_name) = upload_to_sandbox_mock.call_args.args[:2]
    assert "ymapsdf" == module_name
    return report


@pytest.mark.use_local_yt_yql
def test_fail_expectation_with_nmap_links_report(upload_to_sandbox_mock, graph_cook, yt_client, garden_prefix):
    test_yt_path_for_old_data = _row_statistics_path(garden_prefix, _YMAPSDF_OLD_SHIPPING_DATE)

    old_row_statistics = {
        "bld_addr": 123654
    }
    _write_old_row_statistics(test_yt_path_for_old_data, yt_client, old_row_statistics)

    test_utils.data.create_table(
        yt_client,
        table_path=_make_table_path(
            prefix=garden_prefix,
            table_name="bld_addr",
            shipping_date=_YMAPSDF_OLD_SHIPPING_DATE,
            stage="final",
        ),
        schema=test_utils.ymapsdf_schema.YmapsdfSchemaManager().schema_for_table("bld_addr").make_yt_schema(),
        filepath=yatest.common.test_source_path("old_data_row_statistics/bld_addr.jsonl"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    assert isinstance(exc_info.value.original_exception, DataValidationWarning)
    assert 1 == upload_to_sandbox_mock.call_count
    (report, module_name) = upload_to_sandbox_mock.call_args.args[:2]
    assert "ymapsdf" == module_name
    return report


@pytest.mark.use_local_yt_yql
def test_fail_expectation_with_ft_nmaps_links_report(upload_to_sandbox_mock, graph_cook, yt_client, garden_prefix):
    test_yt_path_for_old_data = _row_statistics_path(garden_prefix, _YMAPSDF_OLD_SHIPPING_DATE)

    old_row_statistics = {
        "ft": {
            "urban-service-barbershop": 10001
        },
        "ft_in": {
            "urban-service-barbershop": 10001
        },
        "extra_poi": {
            "urban-service-barbershop": 1
        }
    }

    _write_old_row_statistics(test_yt_path_for_old_data, yt_client, old_row_statistics)

    test_utils.data.create_table(
        yt_client,
        table_path=_make_table_path(
            prefix=garden_prefix,
            table_name="ft",
            shipping_date=_YMAPSDF_OLD_SHIPPING_DATE,
            stage="alpha",
        ),
        schema=test_utils.ymapsdf_schema.YmapsdfSchemaManager().schema_for_table("ft").make_yt_schema(),
        filepath=yatest.common.test_source_path("old_data_row_statistics/alpha_ft.jsonl"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    assert isinstance(exc_info.value.original_exception, DataValidationWarning)
    assert 1 == upload_to_sandbox_mock.call_count
    (report, module_name) = upload_to_sandbox_mock.call_args.args[:2]
    assert "ymapsdf" == module_name
    return report


@pytest.mark.use_local_yt_yql
def test_fail_expectation_with_ft_altay_links_report(upload_to_sandbox_mock, graph_cook, yt_client, garden_prefix):
    test_yt_path_for_old_data = _row_statistics_path(garden_prefix, _YMAPSDF_OLD_SHIPPING_DATE)

    old_row_statistics = {
        "ft": {
            "urban-service-barbershop": 10001
        },
        "ft_in": {
            "urban-service-barbershop": 1
        },
        "extra_poi": {
            "urban-service-barbershop": 10001
        }
    }

    _write_old_row_statistics(test_yt_path_for_old_data, yt_client, old_row_statistics)

    test_utils.data.create_table(
        yt_client,
        table_path=_make_table_path(
            prefix=garden_prefix,
            table_name="extra_poi_tmp",
            shipping_date=_YMAPSDF_OLD_SHIPPING_DATE,
            stage="merge_poi",
        ),
        schema=merge_poi_constants.EXTRA_POI_TMP_SCHEMA,
        filepath=yatest.common.test_source_path("old_data_row_statistics/extra_poi_tmp.jsonl"),
    )

    with pytest.raises(test_utils.internal.task_handler.TaskError) as exc_info:
        test_utils.execute(graph_cook)

    assert isinstance(exc_info.value.original_exception, DataValidationWarning)
    assert 1 == upload_to_sandbox_mock.call_count
    (report, module_name) = upload_to_sandbox_mock.call_args.args[:2]
    assert "ymapsdf" == module_name
    return report


@pytest.mark.use_local_yt_yql
def test_success_expectation_first_launch(graph_cook, yt_client, garden_prefix):
    test_yt_path_for_new_data = _row_statistics_path(garden_prefix, test_utils.ymapsdf.TEST_SHIPPING_DATE)

    test_utils.execute(graph_cook)

    new_row_statistics = json.loads(yt_client.read_file(test_yt_path_for_new_data).read())
    new_row_statistics = {
        table_name: row_count
        for table_name, row_count in new_row_statistics.items()
        if row_count != 0 and row_count != {}  # Filter out empty tables to simplify comparison
    }
    return new_row_statistics


@pytest.mark.use_local_yt_yql
def test_success_expectation_usually(graph_cook, yt_client, garden_prefix):
    test_yt_path_for_old_data = _row_statistics_path(garden_prefix, _YMAPSDF_OLD_SHIPPING_DATE)
    test_yt_path_for_new_data = _row_statistics_path(garden_prefix, test_utils.ymapsdf.TEST_SHIPPING_DATE)

    _write_old_row_statistics(test_yt_path_for_old_data, yt_client, _ROW_STATISTICS)

    test_utils.execute(graph_cook)

    new_row_statistics = json.loads(yt_client.read_file(test_yt_path_for_new_data).read())
    new_row_statistics = {
        table_name: row_count
        for table_name, row_count in new_row_statistics.items()
        if row_count != 0 and row_count != {}  # Filter out empty tables to simplify comparison
    }
    return new_row_statistics
