from unittest.mock import call

import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator
from maps_adv.geosmb.clients.cdp import ApiAccessDenied
from maps_adv.geosmb.marksman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_mocks(dm, cdp):
    dm.list_business_segments_data.side_effect = [
        {
            "biz_id": 11,
            "permalink": 56789,
            "counter_id": 1111,
            "segments": [
                {"segment_name": "KNOWN1", "cdp_id": 77, "cdp_size": 200},
                {"segment_name": "KNOWN2", "cdp_id": 78, "cdp_size": 300},
            ],
            "labels": [
                {"label_name": "known1", "cdp_id": 111, "cdp_size": 1000},
                {"label_name": "known2", "cdp_id": 112, "cdp_size": 2000},
            ],
        },
        {
            "biz_id": 12,
            "permalink": 98765,
            "counter_id": 2222,
            "segments": [
                {"segment_name": "KNOWN1", "cdp_id": 87, "cdp_size": 400},
                {"segment_name": "KNOWN3", "cdp_id": 88, "cdp_size": 500},
            ],
            "labels": [
                {"label_name": "known1", "cdp_id": 211, "cdp_size": 1000},
                {"label_name": "known3", "cdp_id": 212, "cdp_size": 2000},
            ],
        },
    ]

    cdp_id_counter = 0

    async def create_segment(counter_id, segment_name, filtering_params):
        nonlocal cdp_id_counter

        cdp_id_counter += 1

        return {
            "counter_id": counter_id,
            "segment_id": cdp_id_counter,
            "version": 1,
            "name": segment_name,
            "filter": str(filtering_params),
            "segment_type": "REALTIME",
            "size": cdp_id_counter * 100,
        }

    cdp.create_segment.side_effect = create_segment


async def test_creates_schema_for_each_business(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN2"],
                        "labels": ["known1", "unknown2"],
                    },
                ],
                [
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman4",
                        "phone": "322",
                        "segments": ["UNKNOWN1", "UNKNOWN2"],
                        "labels": ["unknown1", "unknown2"],
                    },
                ],
            ]
        )
    )

    cdp.create_contacts_schema.assert_any_await(
        counter_id=1111,
        attributes=[
            {"type_name": "numeric", "name": "biz_id", "multivalued": False},
            {"type_name": "text", "name": "segments", "multivalued": True},
            {"type_name": "text", "name": "labels", "multivalued": True},
        ],
    )
    cdp.create_contacts_schema.assert_any_await(
        counter_id=2222,
        attributes=[
            {"type_name": "numeric", "name": "biz_id", "multivalued": False},
            {"type_name": "text", "name": "segments", "multivalued": True},
            {"type_name": "text", "name": "labels", "multivalued": True},
        ],
    )


async def test_creates_missing_segments_for_business(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN2"],
                        "labels": ["known1", "unknown2"],
                    },
                ],
                [
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman4",
                        "phone": "322",
                        "segments": ["UNKNOWN1", "UNKNOWN2"],
                        "labels": ["unknown1", "unknown2"],
                    },
                ],
            ]
        )
    )

    cdp.create_segment.assert_any_await(
        counter_id=1111,
        segment_name="GEOSMB_SEG_UNKNOWN1",
        filtering_params={"biz_id": 11, "segments": "UNKNOWN1"},
    )
    cdp.create_segment.assert_any_await(
        counter_id=1111,
        segment_name="GEOSMB_SEG_UNKNOWN2",
        filtering_params={"biz_id": 11, "segments": "UNKNOWN2"},
    )


async def test_creates_missing_segments_for_all_businesses(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN2", "UNKNOWN1"],
                        "labels": ["known2", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN3", "UNKNOWN1"],
                        "labels": ["known3", "unknown1"],
                    },
                ]
            ]
        )
    )

    cdp.create_segment.assert_any_await(
        counter_id=1111,
        segment_name="GEOSMB_SEG_UNKNOWN1",
        filtering_params={"biz_id": 11, "segments": "UNKNOWN1"},
    )
    cdp.create_segment.assert_any_await(
        counter_id=2222,
        segment_name="GEOSMB_SEG_KNOWN2",
        filtering_params={"biz_id": 12, "segments": "KNOWN2"},
    )
    cdp.create_segment.assert_any_await(
        counter_id=2222,
        segment_name="GEOSMB_SEG_UNKNOWN1",
        filtering_params={"biz_id": 12, "segments": "UNKNOWN1"},
    )


async def test_creates_missing_labels_for_business(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN2"],
                        "labels": ["known1", "unknown2"],
                    },
                ],
                [
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman4",
                        "phone": "322",
                        "segments": ["UNKNOWN1", "UNKNOWN2"],
                        "labels": ["unknown1", "unknown2"],
                    },
                ],
            ]
        )
    )

    cdp.create_segment.assert_any_await(
        counter_id=1111,
        segment_name="GEOSMB_LABEL_unknown1",
        filtering_params={"biz_id": 11, "labels": "unknown1"},
    )
    cdp.create_segment.assert_any_await(
        counter_id=1111,
        segment_name="GEOSMB_LABEL_unknown2",
        filtering_params={"biz_id": 11, "labels": "unknown2"},
    )


async def test_creates_missing_labels_for_all_businesses(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN2", "UNKNOWN1"],
                        "labels": ["known2", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN3", "UNKNOWN1"],
                        "labels": ["known3", "unknown1"],
                    },
                ]
            ]
        )
    )

    cdp.create_segment.assert_any_await(
        counter_id=1111,
        segment_name="GEOSMB_LABEL_unknown1",
        filtering_params={"biz_id": 11, "labels": "unknown1"},
    )
    cdp.create_segment.assert_any_await(
        counter_id=2222,
        segment_name="GEOSMB_LABEL_known2",
        filtering_params={"biz_id": 12, "labels": "known2"},
    )
    cdp.create_segment.assert_any_await(
        counter_id=2222,
        segment_name="GEOSMB_LABEL_unknown1",
        filtering_params={"biz_id": 12, "labels": "unknown1"},
    )


async def test_saves_created_segments_for_business(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN2"],
                        "labels": ["known1", "unknown2"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman4",
                        "phone": "322",
                        "segments": ["UNKNOWN1", "UNKNOWN2"],
                        "labels": ["unknown1", "unknown2"],
                    },
                ]
            ]
        )
    )

    dm.add_business_segment.assert_any_await(
        biz_id=11,
        name="UNKNOWN1",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.SEGMENT,
    )
    dm.add_business_segment.assert_any_await(
        biz_id=11,
        name="UNKNOWN2",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.SEGMENT,
    )


async def test_saves_created_segments_for_all_businesses(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN2", "UNKNOWN1"],
                        "labels": ["known2", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN3", "UNKNOWN1"],
                        "labels": ["known3", "unknown1"],
                    },
                ]
            ]
        )
    )

    dm.add_business_segment.assert_any_await(
        biz_id=11,
        name="UNKNOWN1",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.SEGMENT,
    )
    dm.add_business_segment.assert_any_await(
        biz_id=12,
        name="KNOWN2",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.SEGMENT,
    )

    dm.add_business_segment.assert_any_await(
        biz_id=12,
        name="UNKNOWN1",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.SEGMENT,
    )


async def test_saves_created_labels_for_business(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN2"],
                        "labels": ["known1", "unknown2"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman4",
                        "phone": "322",
                        "segments": ["UNKNOWN1", "UNKNOWN2"],
                        "labels": ["unknown1", "unknown2"],
                    },
                ]
            ]
        )
    )

    dm.add_business_segment.assert_any_await(
        biz_id=11,
        name="unknown1",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.LABEL,
    )
    dm.add_business_segment.assert_any_await(
        biz_id=11,
        name="unknown2",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.LABEL,
    )


async def test_saves_created_labels_for_all_businesses(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN2", "UNKNOWN1"],
                        "labels": ["known2", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman2",
                        "phone": "322",
                        "segments": ["KNOWN3", "UNKNOWN1"],
                        "labels": ["known3", "unknown1"],
                    },
                ]
            ]
        )
    )

    dm.add_business_segment.assert_any_await(
        biz_id=11,
        name="unknown1",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.LABEL,
    )
    dm.add_business_segment.assert_any_await(
        biz_id=12,
        name="known2",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.LABEL,
    )
    dm.add_business_segment.assert_any_await(
        biz_id=12,
        name="unknown1",
        cdp_id=Any(int),
        cdp_size=Any(int),
        type_=SegmentType.LABEL,
    )


async def test_uploads_business_clients(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "email": "email1@ya.ru",
                        "segments": ["KNOWN2", "UNKNOWN1"],
                        "labels": ["known2", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "email": "email2@ya.ru",
                        "segments": ["KNOWN3", "UNKNOWN1"],
                        "labels": ["known3", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman3",
                        "phone": "322",
                        "email": "email2@ya.ru",
                        "client_ids": [1, 2, 3],
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                ]
            ]
        )
    )

    cdp.upload_contacts.assert_awaited_once_with(
        counter_id=1111,
        biz_id=11,
        contacts=[
            {
                "id": "doorman1",
                "phone": "322",
                "segments": ["KNOWN1", "UNKNOWN1"],
                "labels": ["known1", "unknown1"],
            },
            {
                "id": "doorman2",
                "email": "email1@ya.ru",
                "segments": ["KNOWN2", "UNKNOWN1"],
                "labels": ["known2", "unknown1"],
            },
            {
                "id": "doorman3",
                "phone": "322",
                "email": "email2@ya.ru",
                "segments": ["KNOWN3", "UNKNOWN1"],
                "labels": ["known3", "unknown1"],
            },
            {
                "id": "doorman3",
                "phone": "322",
                "email": "email2@ya.ru",
                "client_ids": [1, 2, 3],
                "segments": ["KNOWN1", "KNOWN2"],
                "labels": ["known1", "known2"],
            },
        ],
    )


async def test_uploads_business_clients_for_all_businesses(domain, dm, cdp):
    await domain.sync_businesses_segments(
        AsyncIterator(
            [
                [
                    {
                        "biz_id": 11,
                        "id": "doorman1",
                        "phone": "322",
                        "segments": ["KNOWN1", "UNKNOWN1"],
                        "labels": ["known1", "unknown1"],
                    },
                    {
                        "biz_id": 11,
                        "id": "doorman2",
                        "email": "email1@ya.ru",
                        "segments": ["KNOWN2", "UNKNOWN1"],
                        "labels": ["known2", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman3",
                        "phone": "322",
                        "email": "email2@ya.ru",
                        "segments": ["KNOWN3", "UNKNOWN1"],
                        "labels": ["known3", "unknown1"],
                    },
                    {
                        "biz_id": 12,
                        "id": "doorman3",
                        "phone": "322",
                        "email": "email2@ya.ru",
                        "client_ids": [1, 2, 3],
                        "segments": ["KNOWN1", "KNOWN2"],
                        "labels": ["known1", "known2"],
                    },
                ]
            ]
        )
    )

    cdp.upload_contacts.assert_any_await(
        counter_id=1111,
        biz_id=11,
        contacts=[
            {
                "id": "doorman1",
                "phone": "322",
                "segments": ["KNOWN1", "UNKNOWN1"],
                "labels": ["known1", "unknown1"],
            },
            {
                "id": "doorman2",
                "email": "email1@ya.ru",
                "segments": ["KNOWN2", "UNKNOWN1"],
                "labels": ["known2", "unknown1"],
            },
        ],
    )
    cdp.upload_contacts.assert_any_await(
        counter_id=2222,
        biz_id=12,
        contacts=[
            {
                "id": "doorman3",
                "phone": "322",
                "email": "email2@ya.ru",
                "segments": ["KNOWN3", "UNKNOWN1"],
                "labels": ["known3", "unknown1"],
            },
            {
                "id": "doorman3",
                "phone": "322",
                "email": "email2@ya.ru",
                "client_ids": [1, 2, 3],
                "segments": ["KNOWN1", "KNOWN2"],
                "labels": ["known1", "known2"],
            },
        ],
    )


async def test_ignores_403_errors_from_cdp_client(domain, dm, cdp):
    dm.list_business_segments_data.side_effect = [
        {
            "biz_id": 11,
            "permalink": 56789,
            "counter_id": 1111,
            "segments": [],
            "labels": [],
        },
        {
            "biz_id": 12,
            "permalink": 98765,
            "counter_id": 2222,
            "segments": [],
            "labels": [],
        },
        {
            "biz_id": 13,
            "permalink": 67676,
            "counter_id": 3333,
            "segments": [],
            "labels": [],
        },
    ]

    cdp.create_contacts_schema.side_effect = [None, ApiAccessDenied, None]

    try:
        await domain.sync_businesses_segments(
            AsyncIterator(
                [
                    [
                        {
                            "biz_id": 11,
                            "id": "doorman1",
                            "phone": "322",
                            "segments": ["KNOWN1", "UNKNOWN1"],
                            "labels": ["known1", "unknown1"],
                        },
                        {
                            "biz_id": 12,
                            "id": "doorman3",
                            "phone": "322",
                            "segments": ["KNOWN3", "UNKNOWN1"],
                            "labels": ["known3", "unknown1"],
                        },
                        {
                            "biz_id": 13,
                            "id": "doorman3",
                            "phone": "322",
                            "segments": ["KNOWN1", "KNOWN2"],
                            "labels": ["known1", "known2"],
                        },
                    ]
                ]
            )
        )
    except:
        pytest.fail("Should not raise on CDP client errors")

    assert cdp.upload_contacts.await_args_list == [
        call(counter_id=1111, biz_id=11, contacts=Any(list)),
        call(counter_id=3333, biz_id=13, contacts=Any(list)),
    ]
