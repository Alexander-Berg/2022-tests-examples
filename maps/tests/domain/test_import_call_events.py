from datetime import datetime, timezone
from unittest import mock

import pytest

from maps_adv.common.helpers import AsyncIterator
from maps_adv.geosmb.doorman.server.lib.enums import Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


input_data = [
    [
        (
            111,
            222,  # permalink
            "7911111111",
            datetime(2020, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
            10,
            "Success",
            444,
            "http://record-url-1",
        )
    ]
]

multichunk_input_data = [
    [
        (
            111,
            222,  # permalink
            "7911111111",
            datetime(2020, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
            10,
            "Success",
            444,
            "http://record-url-1",
        ),
        (
            555,
            666,  # permalink
            "7922222222",
            datetime(2020, 2, 2, 22, 22, 22, tzinfo=timezone.utc),
            20,
            "NoAnswer",
            777,
            "http://record-url-2",
        ),
    ],
    [
        (
            888,
            999,  # permalink
            "7933333333",
            datetime(2020, 3, 3, 0, 33, 33, tzinfo=timezone.utc),
            30,
            "Error",
            999000,
            "http://record-url-3",
        )
    ],
]


@pytest.fixture(autouse=True)
def dm(dm):
    dm.find_clients.coro.return_value = []
    dm.create_client.coro.return_value = {"id": 1}
    dm.merge_client.coro.return_value = {"id": 1}

    return dm


async def test_resolves_permalinks_via_bvm(domain, bvm):
    await domain.import_call_events(AsyncIterator(multichunk_input_data))

    assert bvm.fetch_biz_id_by_permalink.call_args_list == [
        mock.call(222),
        mock.call(666),
        mock.call(999),
    ]


async def test_does_not_request_bvm_for_duplicated_permalinks(domain, bvm):
    await domain.import_call_events(
        AsyncIterator(
            [
                [
                    (
                        111,
                        2222222,  # permalink
                        "7911111111",
                        datetime(2020, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
                        10,
                        "Success",
                        444,
                        "http://record-url-1",
                    )
                    for _ in range(3)
                ]
            ]
        )
    )

    bvm.fetch_biz_id_by_permalink.assert_called_once_with(2222222)


async def test_logs_bvm_fails(caplog, domain, bvm):
    bvm.fetch_biz_id_by_permalink.coro.side_effect = Exception("bad permalink")

    await domain.import_call_events(AsyncIterator(input_data))

    assert "Failed to resolve permalink 222 to biz_id. bad permalink" in caplog.messages


async def test_creates_client_if_no_one_to_merge(domain, dm):
    dm.find_clients.coro.return_value = []

    await domain.import_call_events(AsyncIterator(input_data))

    dm.create_client.assert_called_with(
        biz_id=15,
        source=Source.GEOADV_PHONE_CALL,
        phone=7911111111,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        metadata=None,
        comment=None,
        initiator_id=None,
    )


async def test_logs_create_client_fails(caplog, domain, dm):
    dm.find_clients.coro.return_value = []
    dm.create_client.coro.side_effect = Exception("boom!")

    await domain.import_call_events(AsyncIterator(input_data))

    assert (
        "Failed to create client biz_id=15, phone=7911111111. boom!" in caplog.messages
    )


async def test_merges_client_with_existed_matched_candidate(domain, dm):
    dm.find_clients.coro.return_value = [
        {"id": 1, "phone": 7911111111, "passport_uid": None, "email": None}
    ]

    await domain.import_call_events(AsyncIterator(input_data))

    dm.merge_client.assert_called_with(
        client_id=1,
        biz_id=15,
        source=Source.GEOADV_PHONE_CALL,
        phone=7911111111,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        metadata=None,
        comment=None,
        initiator_id=None,
    )


async def test_uploads_event_for_resolved_businesses(domain, dm, bvm):
    bvm.fetch_biz_id_by_permalink.coro.side_effect = [
        Exception(),
        123,
        Exception(),
    ]

    await domain.import_call_events(AsyncIterator(multichunk_input_data))

    dm.upload_imported_call_events.assert_called_with([[666, "7922222222", 123, 1]])


async def test_uploads_event_for_resolved_clients(domain, dm, bvm):
    bvm.fetch_biz_id_by_permalink.coro.side_effect = [
        123,
        456,
        789,
    ]
    dm.find_clients.coro.return_value = []
    dm.create_client.coro.side_effect = [Exception(), {"id": 1}, Exception()]

    await domain.import_call_events(AsyncIterator(multichunk_input_data))

    dm.upload_imported_call_events.assert_called_with([[666, "7922222222", 456, 1]])


async def test_filter_duplicate_number_calls(domain, dm, bvm):
    bvm.fetch_biz_id_by_permalink.coro.return_value = 456
    input_data = [
        [
            (
                1000,
                222,  # permalink
                "7911111111",
                datetime(2020, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
                10,
                0,
                "NoAnswer",
                444,
                "http://record-url-1",
            ),
            (
                2000,
                222,  # permalink
                "7911111111",
                datetime(2020, 1, 1, 12, 12, 12, tzinfo=timezone.utc),
                10,
                20,
                "Success",
                555,
                "http://record-url-2",
            ),
        ]
    ]

    await domain.import_call_events(AsyncIterator(input_data))

    dm.upload_imported_call_events.assert_called_with([[222, "7911111111", 456, 1]])
