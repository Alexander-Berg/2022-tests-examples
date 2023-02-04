from unittest.mock import call

import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm_and_cdp_client_appropriately(domain, dm, cdp):
    dm.list_biz_ids.return_value = [123, 345]
    dm.list_business_segments_data.side_effect = [
        {
            "biz_id": 123,
            "permalink": 56789,
            "counter_id": 1111,
            "segments": [
                {"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200},
                {"segment_name": "LOST", "cdp_id": 78, "cdp_size": 300},
            ],
        },
        {
            "biz_id": 345,
            "permalink": 98765,
            "counter_id": 2222,
            "segments": [
                {"segment_name": "ACTIVE", "cdp_id": 88, "cdp_size": 400},
            ],
        },
    ]
    cdp.get_segment_size.side_effect = [500, 600, 700]

    await domain.sync_segments_sizes()

    dm.list_biz_ids.assert_awaited_once_with()
    assert dm.list_business_segments_data.await_args_list == [
        call(biz_id=123),
        call(biz_id=345),
    ]
    assert cdp.get_segment_size.await_args_list == [
        call(counter_id=1111, segment_id=77),
        call(counter_id=1111, segment_id=78),
        call(counter_id=2222, segment_id=88),
    ]
    dm.update_segments_sizes.assert_awaited_once_with(sizes={77: 500, 78: 600, 88: 700})
