from datetime import date, datetime
from decimal import Decimal

import pytest
import pytz

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.build_reconciliation_report.coro.return_value = {
        1: {"completion_qty": Decimal("100.0"), "consumption_qty": Decimal("100.0")},
        15: {"completion_qty": Decimal("200.0"), "consumption_qty": Decimal("350.0")},
    }

    result = await orders_domain.build_reconciliation_report(date(2000, 1, 2))

    orders_dm.build_reconciliation_report.assert_called_with(
        due_to=pytz.timezone("Europe/Moscow").localize(datetime(2000, 1, 3)),
        service_id=100,
    )
    assert result == {
        1: {"completion_qty": Decimal("100.0"), "consumption_qty": Decimal("100.0")},
        15: {"completion_qty": Decimal("200.0"), "consumption_qty": Decimal("350.0")},
    }
