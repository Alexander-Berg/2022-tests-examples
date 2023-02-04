import os
import pytest

from asgiref.sync import sync_to_async
from yt.wrapper.default_config import get_config_from_env
from yt.wrapper.ypath import ypath_join

from intranet.trip.src.config import settings
from intranet.trip.src.lib.yt import RegistryRow
from intranet.trip.src.lib.yt.api import sync_write_rows, sync_read_rows, sync_connect


pytestmark = pytest.mark.asyncio


mocked_registry_row = {
    'BATCH_ID': 1,
    'BATCH_DATE': '09.02.2022 00:00:00',
    'BATCH_LINE_NUM': 1,
    'OPERATION_UNIT': '',
    'INVOICE_NUM': '',
    'VENDOR': '',
    'VENDOR_SITE': '',
    'INVOICE_DATE': '',
    'INVOICE_AMOUNT': 3.14,
    'CURRENCY': '',
    'DIST_LINE_AMOUNT': 2.78,
    'TAX_CODE': '',
    'FINE': '',
    'LINE_DESC': '',
    'USER_NAME': '',
    'TICKET_VENDOR': '',
    'TAX_INV_NUM': '',
    'TAX_INV_DATE': '',
    'TICKET': '',
    'DESTINATION': '',
    'COUNTRY_FROM': '',
    'COUNTRY_TO': '',
    'TRAVEL_START_DATE': '',
    'TRAVEL_END_DATE': '',
    'TRIP_PURPOSE1': '',
    'TRIP_PURPOSE2': '',
    'ASSIGNEMENT_ID': 1,
    'TYPE_EXPENSES': 'Ticket',
}


@sync_to_async(thread_sensitive=True)
def _rw_to_yt(config, table, rows):
    client = sync_connect(config)
    sync_write_rows(
        client=client,
        row_type=RegistryRow,
        table=table,
        rows=rows,
    )
    return sync_read_rows(
        client=client,
        row_type=RegistryRow,
        table=ypath_join(config['prefix'], table),  # TODO: разобраться почему не работает config
    )


def get_config():
    config = get_config_from_env()
    config['prefix'] = '//tmp/trip/'
    config['proxy']['url'] = settings.YT_PROXY
    config['yamr_mode']['create_recursive'] = True
    if 'YT_PROXY' in os.environ:
        config['proxy']['url'] = os.environ['YT_PROXY']
    return config


async def test_yt_api_rw():
    rows = [RegistryRow(**mocked_registry_row) for i in range(2)]
    rows[1].batch_line_num = 2

    config = get_config()

    yt_rows = await _rw_to_yt(
        config=config,
        table='test-read-write',
        rows=rows,
    )

    assert len(rows) == len(yt_rows)
    assert rows[0] in yt_rows
    assert rows[1] in yt_rows
