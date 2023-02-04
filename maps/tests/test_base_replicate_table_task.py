import pytest

from maps_adv.common.yt_utils import BaseReplicateYtTableTask

pytestmark = [pytest.mark.asyncio]


async def test_calls_replication_as_expected(mock_yt):
    task = BaseReplicateYtTableTask(
        src_cluster="src-cluster",
        target_cluster="target-cluster",
        token="yt-token",
        src_table="src-table",
        target_table="target-table",
    )

    await task

    assert mock_yt["run_remote_copy"].called
    assert mock_yt["run_remote_copy"].call_args[1] == {
        "cluster_name": "src-cluster",
        "destination_table": "target-table",
        "source_table": "src-table",
    }
