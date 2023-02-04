import pytest

from asgiref.sync import sync_to_async
from yt.wrapper.ypath import ypath_join

from intranet.trip.src.lib.yt import RegistryRow, YtRegistry
from intranet.trip.src.lib.yt.api import sync_read_rows, sync_connect

from .test_api import get_config, mocked_registry_row


pytestmark = pytest.mark.asyncio


@sync_to_async(thread_sensitive=True)
def _read_from_yt(config, table):
    client = sync_connect(config)
    return sync_read_rows(
        client=client,
        row_type=RegistryRow,
        table=ypath_join(config['prefix'], table),  # TODO: разобраться почему не работает config
    )


async def test_gateway_write():
    rows = [RegistryRow(**mocked_registry_row) for i in range(2)]
    rows[1].batch_line_num = 2
    table = 'test-read-write'

    config = get_config()
    await YtRegistry(
        prefix=config['prefix'],
        proxy=config['proxy']['url'],
    ).write_rows(table=table, rows=rows)
    yt_rows = await _read_from_yt(config=config, table=table)

    assert len(rows) == len(yt_rows)
    assert rows[0] in yt_rows
    assert rows[1] in yt_rows
