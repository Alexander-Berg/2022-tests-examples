import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]

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
        },
        {
            # should be ignored
            "tag": "some_other_tag",
            "items": [{"tag": "audience", "attributes": {"id": "111100"}}],
        },
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


@pytest.mark.parametrize(
    "targetings_list, expected",
    (
        ([EASY_TARGETING], [2000001111, 2000002222]),
        (
            [EASY_TARGETING, MEDIUM_TARGETING],
            [
                2000001111,
                2000002222,
                2000111100,
                2000222200,
                2000333300,
                2000444400,
                2000555500,
                2000666600,
            ],
        ),
        (
            [EASY_TARGETING, MEDIUM_TARGETING, HARD_TARGETING],
            [
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
            ],
        ),
    ),
)
async def test_returns_list_of_used_audiences(
    targetings_list, expected, campaigns_domain, campaigns_dm
):
    campaigns_dm.retrieve_targetings.coro.return_value = targetings_list

    got = await campaigns_domain.list_used_audiences()

    assert got == expected


async def test_does_not_return_duplicates(campaigns_domain, campaigns_dm):
    campaigns_dm.retrieve_targetings.coro.return_value = [
        {
            "tag": "and",
            "items": [
                {
                    "tag": "or",
                    "items": [
                        {"not": True, "tag": "audience", "attributes": {"id": "9999"}},
                        {"tag": "audience", "attributes": {"id": "9999"}},
                    ],
                }
            ],
        },
        {"tag": "audience", "attributes": {"id": "9999"}},
    ]

    got = await campaigns_domain.list_used_audiences()

    assert got == [2000009999]
