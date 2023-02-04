import pytest

from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.empty_tables import UploadEmptyTables


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_upload_empty_tables_task(test_task_executor):
    input_resources = {
        "input_resource": None,
    }

    output_resources = {
        "ad_excl": test_task_executor.create_yt_table_resource("ad_excl"),
    }

    test_task_executor.execute_task(
        task=UploadEmptyTables(),
        input_resources=input_resources,
        output_resources=output_resources,
    )

    assert list(output_resources["ad_excl"].read_table()) == []
