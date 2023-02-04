import gzip
from unittest import mock
import os
import pytest
import shutil
import tarfile

import yatest

from maps.garden.sdk.core import Version

from maps.libs.ymapsdf.py import ymapsdf
from maps.garden.sdk import test_utils
from maps.garden.sdk.test_utils import ymapsdf_schema
from maps.garden.sdk.extensions import resource_namer

from maps.garden.modules.ymapsdf.lib.ymapsdf_load import constants
from maps.garden.modules.ymapsdf.lib.ymapsdf_load import ymapsdf_load
from maps.garden.modules.ymapsdf_src.lib import ymapsdf_src

ARCHIVE_ITEM_MOCK_URL = "http://example.com/" + constants.ARCHIVE_ITEM_NAME
SCHEMA_ITEM_MOCK_URL = "http://example.com/" + constants.SCHEMA_ITEM_NAME

INPUT_RESOURCE_PROPERTIES = {
    "region": test_utils.ymapsdf.TEST_REGION,
    "vendor": test_utils.ymapsdf.TEST_VENDOR,
    "shipping_date": test_utils.ymapsdf.TEST_SHIPPING_DATE,
    "file_list": [
        {
            "name": constants.ARCHIVE_ITEM_NAME,
            "url": ARCHIVE_ITEM_MOCK_URL
        },
        {
            "name": constants.SCHEMA_ITEM_NAME,
            "url": SCHEMA_ITEM_MOCK_URL
        }
    ]
}

ORIGINAL_TABLES = ymapsdf.all_tables()

TABLES_WITH_NO_SCHEMA = [
    "new_table"
]


def make_data_archive():
    archive_filename = constants.ARCHIVE_ITEM_NAME

    data_dir = yatest.common.test_source_path("data")

    with tarfile.open(archive_filename, "w") as tar_archive:
        for csv_filename in os.listdir(data_dir):
            csv_filepath = os.path.join(data_dir, csv_filename)
            table_name, _ = os.path.splitext(csv_filename)
            gz_filename = table_name + ".gz"
            with open(csv_filepath, "rb") as f_in, gzip.open(gz_filename, 'wb') as f_out:
                shutil.copyfileobj(f_in, f_out)
            tar_archive.add(name=gz_filename)
    return archive_filename


@pytest.mark.use_local_yt(constants.YT_SERVER)
@mock.patch("maps.libs.ymapsdf.py.ymapsdf.all_tables", return_value=ORIGINAL_TABLES+TABLES_WITH_NO_SCHEMA)
def test_graph_execution_ymapsdf_load_yt(all_tables_mock, requests_mock, environment_settings):
    # creating tar archive with ymapsdf schema
    # and mocking corresponding request with it
    schemas_filename = ymapsdf_schema.make_schemas_archive()

    def schema_file_callback(request, context):
        return open(schemas_filename, "rb")

    requests_mock.get(SCHEMA_ITEM_MOCK_URL, body=schema_file_callback)

    archive_filename = make_data_archive()
    archive_file = open(archive_filename, "rb")
    requests_mock.get(ARCHIVE_ITEM_MOCK_URL, body=archive_file)

    cook = test_utils.GraphCook(environment_settings)

    ymapsdf_src.fill_graph(cook.input_builder(), test_utils.ymapsdf.TEST_REGIONS)
    ymapsdf_load.fill_graph(cook.target_builder(), test_utils.ymapsdf.TEST_REGIONS)

    res = cook.create_input_resource(
        resource_namer.get_full_resource_name(
            constants.SOURCE_RESOURCE_NAME,
            test_utils.ymapsdf.TEST_REGION,
            test_utils.ymapsdf.TEST_VENDOR)
    )
    res.version = Version(properties=INPUT_RESOURCE_PROPERTIES)
    res.logged_commit()

    test_utils.execute(cook)

    test_utils.ymapsdf.validate_data(
        environment_settings,
        constants.OUTPUT_STAGE,
        yatest.common.test_source_path("data_output"),
        ORIGINAL_TABLES
    )

    # Only original tables have schemas.
    # There is no sense to validate TABLES_WITH_NO_SCHEMA
    ymapsdf_schema.validate(
        environment_settings,
        constants.YT_SERVER,
        constants.OUTPUT_STAGE,
        INPUT_RESOURCE_PROPERTIES,
        ORIGINAL_TABLES
    )
