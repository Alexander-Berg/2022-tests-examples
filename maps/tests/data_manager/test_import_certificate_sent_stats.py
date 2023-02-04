from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator
from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName

pytestmark = [pytest.mark.asyncio]


input_data = [
    [
        {
            "biz_id": 6543,
            "coupon_id": 1000,
            "scenario": "ENGAGE_PROSPECTIVE",
            "clicked": 0,
            "opened": 2,
            "sent": 14,
            "dt": "2020-12-05",
        },
        {
            "biz_id": 7854,
            "coupon_id": 2000,
            "scenario": "DISCOUNT_FOR_LOST",
            "clicked": 2,
            "opened": 5,
            "sent": 12,
            "dt": "2020-12-05",
        },
    ],
    [
        {
            "biz_id": 1975,
            "coupon_id": 3000,
            "scenario": "THANK_THE_LOYAL",
            "clicked": 9,
            "opened": 6,
            "sent": 3,
            "dt": "2020-12-04",
        },
    ],
]


async def test_processes_all_data_from_generator(factory, con, dm):
    existing_id = await factory.create_certificate_mailing_stats(
        biz_id=11, coupon_id=111
    )

    await dm.import_certificate_mailing_stats(AsyncIterator(input_data))

    rows = await con.fetch(
        """
            SELECT *
            FROM certificate_mailing_stats
            ORDER BY id
        """
    )
    assert [dict(row) for row in rows] == [
        {
            "id": existing_id,
            "biz_id": 11,
            "coupon_id": 111,
            "scenario_name": ScenarioName.DISCOUNT_FOR_DISLOYAL,
            "clicked": 3,
            "opened": 8,
            "sent": 15,
            "sent_date": "2020-05-10",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "biz_id": 6543,
            "coupon_id": 1000,
            "scenario_name": ScenarioName.ENGAGE_PROSPECTIVE,
            "clicked": 0,
            "opened": 2,
            "sent": 14,
            "sent_date": "2020-12-05",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "biz_id": 7854,
            "coupon_id": 2000,
            "scenario_name": ScenarioName.DISCOUNT_FOR_LOST,
            "clicked": 2,
            "opened": 5,
            "sent": 12,
            "sent_date": "2020-12-05",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "biz_id": 1975,
            "coupon_id": 3000,
            "scenario_name": ScenarioName.THANK_THE_LOYAL,
            "clicked": 9,
            "opened": 6,
            "sent": 3,
            "sent_date": "2020-12-04",
            "created_at": Any(datetime),
        },
    ]


async def test_merges_stats_if_data_for_day_for_coupon_exists(factory, con, dm):
    existing_id = await factory.create_certificate_mailing_stats(
        biz_id=11, coupon_id=111, sent_date="2020-05-10", sent=5, clicked=4, opened=3
    )

    input_data = [
        [
            {
                "biz_id": 11,
                "coupon_id": 111,
                "scenario": "DISCOUNT_FOR_DISLOYAL",
                "clicked": 7,
                "opened": 2,
                "sent": 14,
                # same day
                "dt": "2020-05-10",
            },
        ]
    ]

    await dm.import_certificate_mailing_stats(AsyncIterator(input_data))

    rows = await con.fetch(
        """
            SELECT *
            FROM certificate_mailing_stats
            ORDER BY id
        """
    )
    assert [dict(row) for row in rows] == [
        {
            "id": existing_id,
            "biz_id": 11,
            "coupon_id": 111,
            "scenario_name": ScenarioName.DISCOUNT_FOR_DISLOYAL,
            "clicked": 11,
            "opened": 5,
            "sent": 19,
            "sent_date": "2020-05-10",
            "created_at": Any(datetime),
        },
    ]


async def test_does_not_merge_stats_if_data_for_day_for_coupon_does_not_exist(
    factory, con, dm
):
    existing_id = await factory.create_certificate_mailing_stats(
        biz_id=11, coupon_id=111, sent_date="2020-05-10", sent=5, clicked=4, opened=3
    )

    input_data = [
        [
            {
                "biz_id": 11,
                "coupon_id": 111,
                "scenario": "DISCOUNT_FOR_DISLOYAL",
                "clicked": 7,
                "opened": 2,
                "sent": 14,
                # another day
                "dt": "2020-05-11",
            },
        ]
    ]

    await dm.import_certificate_mailing_stats(AsyncIterator(input_data))

    rows = await con.fetch(
        """
            SELECT *
            FROM certificate_mailing_stats
            ORDER BY id
        """
    )
    assert [dict(row) for row in rows] == [
        {
            "id": existing_id,
            "biz_id": 11,
            "coupon_id": 111,
            "scenario_name": ScenarioName.DISCOUNT_FOR_DISLOYAL,
            "clicked": 4,
            "opened": 3,
            "sent": 5,
            "sent_date": "2020-05-10",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "biz_id": 11,
            "coupon_id": 111,
            "scenario_name": ScenarioName.DISCOUNT_FOR_DISLOYAL,
            "clicked": 7,
            "opened": 2,
            "sent": 14,
            "sent_date": "2020-05-11",
            "created_at": Any(datetime),
        },
    ]


async def test_does_nothing_for_empty_generator(factory, con, dm):
    existing_id = await factory.create_certificate_mailing_stats(
        biz_id=11, coupon_id=111
    )

    await dm.import_certificate_mailing_stats(AsyncIterator([]))

    rows = await con.fetch(
        """
            SELECT *
            FROM certificate_mailing_stats
            ORDER BY id
        """
    )
    assert [dict(row) for row in rows] == [
        {
            "id": existing_id,
            "biz_id": 11,
            "coupon_id": 111,
            "scenario_name": ScenarioName.DISCOUNT_FOR_DISLOYAL,
            "clicked": 3,
            "opened": 8,
            "sent": 15,
            "sent_date": "2020-05-10",
            "created_at": Any(datetime),
        }
    ]


async def test_returns_nothing(dm):
    got = await dm.import_certificate_mailing_stats(AsyncIterator(input_data))

    assert got is None
