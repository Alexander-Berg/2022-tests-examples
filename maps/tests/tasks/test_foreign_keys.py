import pytest

from maps.garden.modules.ymapsdf_osm.defs import YT_CLUSTER, YMAPSDF_OSM
from maps.garden.modules.ymapsdf_osm.lib.validation.foreign_keys import CheckForeignKeys, ForeignKeysInfo
from maps.garden.sdk.core import AutotestsFailedError
from maps.garden.sdk.resources import FlagResource
from .utils import get_task_executor


@pytest.fixture
def task_executor(environment_settings):
    return get_task_executor(
        environment_settings=environment_settings,
        source_folder="test_foreign_keys",
    )


@pytest.mark.use_local_yt(YT_CLUSTER)
@pytest.mark.parametrize(
    ("table_name", "column_name", "reference_table_name", "reference_column_name", "exception"), [
        ("ad_center", "ad_id", "ad", "ad_id", False),
        ("ad_center", "node_id", "node", "node_id", True),
        ("ad", "p_ad_id", "ad", "ad_id", False)
    ]
)
def test_foreign_keys(task_executor, table_name, column_name, reference_table_name, reference_column_name, exception):
    input_resources = {
        table_name: task_executor.create_ymapsdf_input_yt_table_resource(table_name),
        reference_table_name: task_executor.create_ymapsdf_input_yt_table_resource(reference_table_name),
    } if table_name != reference_table_name else {
        table_name: task_executor.create_ymapsdf_input_yt_table_resource(table_name)
    }

    output_resource_name = YMAPSDF_OSM.resource_name(
        f"foreign_keys_checked_{table_name}_{column_name}__{reference_table_name}_{reference_column_name}"
    )
    output_resources = {
        output_resource_name: FlagResource(output_resource_name),
    }

    def execute():
        task_executor.execute_task(
            task=CheckForeignKeys(
                ForeignKeysInfo(
                    table_name=table_name,
                    column_name=column_name,
                    reference_table_name=reference_table_name,
                    reference_column_name=reference_column_name,
                )
            ),
            input_resources=input_resources,
            output_resources=output_resources,
        )

    if exception:
        with pytest.raises(AutotestsFailedError):
            execute()
    else:
        execute()
