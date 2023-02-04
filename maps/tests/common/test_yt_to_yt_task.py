import pytest

from maps.garden.sdk.core import Version
from maps.garden.sdk import yt as yt_plugin

from maps.garden.modules.ymapsdf.lib.common import yt_to_yt_task

import yt.wrapper as yt

from . import data


@pytest.mark.use_local_yt("hahn")
def test_task(environment_settings):
    source_table = yt_plugin.YtTableResource(
        name="source_table",
        path_template="/source_table",
        server="hahn",
        schema=data.YT_TABLE_SCHEMA)
    source_table.version = Version(properties={"region": "test_region"})
    source_table.load_environment_settings(environment_settings)
    source_table.write_table(data.TABLE_DATA)
    source_table.logged_commit()
    source_table.calculate_size()

    dest_table = yt_plugin.YtTableResource(
        name="dest_table",
        path_template="/dest_table",
        server="hahn")
    dest_table.version = Version(properties={"region": "test_region"})
    dest_table.load_environment_settings(environment_settings)

    task = yt_to_yt_task.CopyYtToYtTask()
    task.load_environment_settings(environment_settings)
    task(source_table, dest_table)

    yt_table_data = list(dest_table.get_yt_client().read_table(
        dest_table.path,
        format=yt.YsonFormat(encoding=None)))
    assert yt_table_data == data.convert_to_yt_format(data.TABLE_DATA)
