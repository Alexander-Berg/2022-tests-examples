import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_calls_dm_with_expected_params(domain, dm):
    await domain.list_client_events(
        client_id=111,
        biz_id=123,
        datetime_from=dt("2020-01-01 00:00:00"),
        datetime_to=dt("2020-02-02 00:00:00"),
    )

    dm.list_client_events.assert_called_with(
        client_id=111,
        biz_id=123,
        datetime_from=dt("2020-01-01 00:00:00"),
        datetime_to=dt("2020-02-02 00:00:00"),
    )


async def test_returns_events_details(domain, dm):
    dm_result = dict(
        calls=[
            {
                "timestamp": dt("2020-01-01 00:00:00"),
                "source": Source.GEOADV_PHONE_CALL,
                "type": CallEvent.INITIATED,
            }
        ],
        events_before=2,
        events_after=3,
    )
    dm.list_client_events.coro.return_value = dm_result

    got = await domain.list_client_events(
        client_id=111,
        biz_id=123,
        datetime_from=dt("2020-01-01 00:00:00"),
        datetime_to=dt("2020-02-02 00:00:00"),
    )

    assert got == dm_result
