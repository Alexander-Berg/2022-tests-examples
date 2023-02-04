import pytest

from maps.garden.sdk.test_utils.canonization import canonize_yt_tables
from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER
from maps.garden.modules.ymapsdf_osm.lib.dict_tables import UploadDictTable


@pytest.mark.use_local_yt(YT_CLUSTER)
def test_upload_dict_table_task(test_task_executor):
    input_resources = {
        "input_resource": None,
    }

    output_resources = {
        "output_resource": test_task_executor.create_yt_table_resource("source_type"),
    }

    task = UploadDictTable("source_type")

    assert task.displayed_name == "UploadSourceTypeTableTask"

    test_task_executor.execute_task(
        task=task,
        input_resources=input_resources,
        output_resources=output_resources,
    )

    return canonize_yt_tables(
        yt_table_resources=output_resources
    )
