import gzip
import shutil

import pytest

import yt.wrapper as yt

from maps.garden.sdk.core import Version
from maps.garden.sdk import yt as yt_plugin

from maps.garden.modules.ymapsdf.lib.common import gz_to_yt

from . import data

CSV_FILENAME = "data.csv"
GZIP_FILENAME = "data.gz"


@pytest.mark.use_local_yt("hahn")
def test_task(environment_settings):
    yt_table = yt_plugin.YtTableResource(
        name="test_gz_to_yt_table",
        path_template="/test_table",
        server="hahn")
    yt_table.version = Version(properties={"region": "test_region"})
    yt_table.load_environment_settings(environment_settings)

    data.dump_table_to_csv(data.TABLE_DATA, CSV_FILENAME)
    with open(CSV_FILENAME, "rb") as csv_file, gzip.open(GZIP_FILENAME, "wb") as gzip_file:
        shutil.copyfileobj(csv_file, gzip_file)

    gz_to_yt.copy_gz_to_yt(
        yt_table.path,
        GZIP_FILENAME,
        data.EXTENDED_TABLE_SCHEMA)

    yt_table_data = list(yt_table.read_table(format=yt.YsonFormat(encoding=None)))

    # expecting auxilliary fields x and y to be added automatically by the task
    expected_data = data.convert_to_yt_format(data.TABLE_DATA)
    for expected_row in expected_data:
        expected_row[b"x"] = data.CENTER_X
        expected_row[b"y"] = data.CENTER_Y
        expected_row[b"xmin"] = data.MIN_X
        expected_row[b"ymin"] = data.MIN_Y
        expected_row[b"xmax"] = data.MAX_X
        expected_row[b"ymax"] = data.MAX_Y

    assert yt_table_data == expected_data
