from datetime import datetime

import pytest
import pytz

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.freeze_time(datetime(2000, 2, 2, 3, 4, 5, tzinfo=pytz.utc))
async def test_uses_dm(orders_domain, orders_dm):

    await orders_domain.load_geoprod_reconciliation_report()

    orders_dm.load_geoprod_reconciliation_report.assert_called_with(
        from_datetime=pytz.timezone("Europe/Moscow").localize(datetime(2000, 2, 1)),
        to_datetime=pytz.timezone("Europe/Moscow").localize(
            datetime(2000, 2, 1, 23, 59, 59, 999999)
        ),
        geoprod_service_id=200,
    )


async def test_uses_dm_with_provided_date(orders_domain, orders_dm):

    await orders_domain.load_geoprod_reconciliation_report(
        datetime(2000, 2, 22, 1, 2, 3, tzinfo=pytz.utc)
    )

    orders_dm.load_geoprod_reconciliation_report.assert_called_with(
        from_datetime=pytz.timezone("Europe/Moscow").localize(datetime(2000, 2, 22)),
        to_datetime=pytz.timezone("Europe/Moscow").localize(
            datetime(2000, 2, 22, 23, 59, 59, 999999)
        ),
        geoprod_service_id=200,
    )
