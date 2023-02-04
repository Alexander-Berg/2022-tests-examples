import pytest

from maps_adv.adv_store.v2.tests import dt

pytestmark = [pytest.mark.asyncio, pytest.mark.freeze_time(dt("2020-02-28 07:59:30"))]

url = "/used-audience/"

EASY_TARGETING = {
    "tag": "and",
    "items": [
        {
            "tag": "or",
            "items": [
                {"not": True, "tag": "audience", "attributes": {"id": "1111"}},
                {"tag": "audience", "attributes": {"id": "2222"}},
            ],
        }
    ],
}

MEDIUM_TARGETING = {
    "tag": "and",
    "items": [
        {
            "tag": "or",
            "items": [
                {"tag": "audience", "attributes": {"id": "111100"}},
                {
                    "tag": "and",
                    "items": [
                        {
                            "tag": "or",
                            "items": [
                                {"tag": "audience", "attributes": {"id": "222200"}},
                                {"tag": "audience", "attributes": {"id": "333300"}},
                            ],
                        },
                        {
                            "tag": "or",
                            "items": [
                                {"tag": "audience", "attributes": {"id": "444400"}},
                                {
                                    "tag": "and",
                                    "items": [
                                        {
                                            "tag": "or",
                                            "items": [
                                                {
                                                    "tag": "audience",
                                                    "attributes": {"id": "555500"},
                                                },
                                                {
                                                    "tag": "audience",
                                                    "attributes": {"id": "666600"},
                                                },
                                            ],
                                        }
                                    ],
                                },
                            ],
                        },
                    ],
                },
            ],
        }
    ],
}

HARD_TARGETING = {
    "tag": "and",
    "items": [
        {
            "tag": "or",
            "items": [
                {"tag": "audience", "attributes": {"id": "1111000"}},
                {
                    "tag": "and",
                    "items": [
                        {
                            "tag": "or",
                            "items": [
                                {"tag": "audience", "attributes": {"id": "2222000"}},
                                {"tag": "audience", "attributes": {"id": "3333000"}},
                                {"tag": "audience", "attributes": {"id": "222200"}},
                                {"tag": "audience", "attributes": {"id": "333300"}},
                            ],
                        },
                        {
                            "tag": "or",
                            "items": [
                                {"tag": "audience", "attributes": {"id": "4444000"}},
                                {
                                    "tag": "and",
                                    "items": [
                                        {
                                            "tag": "or",
                                            "items": [
                                                {
                                                    "tag": "audience",
                                                    "attributes": {"id": "5555000"},
                                                },
                                                {
                                                    "tag": "audience",
                                                    "attributes": {"id": "6666000"},
                                                },
                                            ],
                                        }
                                    ],
                                },
                            ],
                        },
                    ],
                },
            ],
        },
        {
            "tag": "or",
            "items": [
                {"tag": "audience", "attributes": {"id": "111100000"}},
                {"tag": "audience", "attributes": {"id": "222200"}},
                {"tag": "audience", "attributes": {"id": "333300"}},
                {"tag": "not_audience", "attributes": {"id": "222200000"}},
                {
                    "tag": "and",
                    "items": [
                        {
                            "tag": "or",
                            "items": [
                                {"tag": "audience", "attributes": {"id": "333300000"}},
                                {"tag": "audience", "attributes": {"id": "444400000"}},
                                {
                                    "tag": "not_audience",
                                    "attributes": {"id": "555500000"},
                                },
                            ],
                        },
                        {
                            "tag": "or",
                            "items": [
                                {"tag": "audience", "attributes": {"id": "666600000"}},
                                {
                                    "tag": "and",
                                    "items": [
                                        {
                                            "tag": "or",
                                            "items": [
                                                {
                                                    "tag": "audience",
                                                    "attributes": {"id": "777700000"},
                                                },
                                                {
                                                    "tag": "not_audience",
                                                    "attributes": {"id": "888800000"},
                                                },
                                            ],
                                        },
                                        {
                                            "tag": "audience",
                                            "attributes": {"id": "999900000"},
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                },
            ],
        },
    ],
}


@pytest.mark.freeze_time(dt("2020-02-28 07:59:30"))
async def test_receives_used_audiences(api, factory):
    await factory.create_campaign(
        end_datetime=dt("2020-04-02 12:00:00"), targeting=EASY_TARGETING
    )
    await factory.create_campaign(
        end_datetime=dt("2020-03-02 12:00:00"), targeting=MEDIUM_TARGETING
    )
    await factory.create_campaign(
        end_datetime=dt("2020-04-02 12:00:00"), targeting=HARD_TARGETING
    )

    got = await api.get(url, expected_status=200)

    assert got == {
        "usedSegmentIds": [
            2000001111,
            2000002222,
            2000111100,
            2000222200,
            2000333300,
            2000444400,
            2000555500,
            2000666600,
            2001111000,
            2002222000,
            2003333000,
            2004444000,
            2005555000,
            2006666000,
            2111100000,
            2333300000,
            2444400000,
            2666600000,
            2777700000,
            2999900000,
        ]
    }


@pytest.mark.freeze_time(dt("2020-02-28 07:59:30"))
async def test_receives_used_audiences_for_future_campaigns(api, factory):
    await factory.create_campaign(
        start_datetime=dt("2020-04-02 12:00:00"),
        end_datetime=dt("2021-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )

    got = await api.get(url, expected_status=200)

    assert got == {"usedSegmentIds": [2000001111]}


@pytest.mark.freeze_time(dt("2020-02-28 07:59:30"))
async def test_ignores_ended_campaigns(api, factory):
    # campaigns ended
    await factory.create_campaign(
        end_datetime=dt("2019-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "1111"}},
    )
    await factory.create_campaign(
        end_datetime=dt("2020-02-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "2222"}},
    )
    # ongoing campaign
    await factory.create_campaign(
        end_datetime=dt("2020-03-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "3333"}},
    )

    got = await api.get(url, expected_status=200)

    assert got == {"usedSegmentIds": [2000003333]}


@pytest.mark.freeze_time(dt("2020-02-28 07:59:30"))
async def test_returns_empty_list_if_nothing_found(api, factory):
    # campaign ended
    await factory.create_campaign(
        end_datetime=dt("2019-04-02 12:00:00"),
        targeting={"tag": "audience", "attributes": {"id": "3333"}},
    )
    # ongoing campaign without targeting
    await factory.create_campaign(
        end_datetime=dt("2020-03-02 12:00:00"), targeting=None
    )

    got = await api.get(url, expected_status=200)

    assert got == {"usedSegmentIds": []}
