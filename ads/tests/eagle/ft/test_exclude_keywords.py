from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer


def test_exclude_keywords(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [
                    {
                        "keyword_id": 174,
                        "update_time": 1500000000,
                        "weighted_uint_values": [
                            {"first": 0, "weight": 574222, "update_time": 1500000000},
                            {"first": 1, "weight": 425777, "update_time": 1500000000},
                        ],
                    },
                    {
                        "keyword_id": 569,
                        "update_time": 1500000000,
                        "pair_values": [
                            {"first": 543, "second": 2},
                            {"first": 176, "second": 1},
                            {"first": 174, "second": 1},
                            {"first": 614, "second": 1},
                        ],
                    },
                ]
            }
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 174,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 574222, "update_time": 1500000000},
                        {"first": 1, "weight": 425777, "update_time": 1500000000},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 176,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 96427, "update_time": 1500000000},
                        {"first": 1, "weight": 618944, "update_time": 1500000000},
                        {"first": 2, "weight": 284627, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 543,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 26769, "update_time": 1500000000},
                        {"first": 1, "weight": 67792, "update_time": 1500000000},
                        {"first": 2, "weight": 397602, "update_time": 1500000000},
                        {"first": 3, "weight": 298865, "update_time": 1500000000},
                        {"first": 4, "weight": 145030, "update_time": 1500000000},
                        {"first": 5, "weight": 63940, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 569,
                    "update_time": 1500000000,
                    "pair_values": [
                        {"first": 543, "second": 2},
                        {"first": 176, "second": 1},
                        {"first": 174, "second": 1},
                        {"first": 614, "second": 1},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 614,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 96427, "update_time": 1500000000},
                        {"first": 1, "weight": 381123, "update_time": 1500000000},
                        {"first": 2, "weight": 237821, "update_time": 1500000000},
                        {"first": 3, "weight": 245325, "update_time": 1500000000},
                        {"first": 4, "weight": 39302, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 881,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 574222, "update_time": 1500000000},
                        {"first": 1, "weight": 425777, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 882,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 26769, "update_time": 1500000000},
                        {"first": 1, "weight": 67792, "update_time": 1500000000},
                        {"first": 2, "weight": 397602, "update_time": 1500000000},
                        {"first": 3, "weight": 298865, "update_time": 1500000000},
                        {"first": 4, "weight": 145030, "update_time": 1500000000},
                        {"first": 5, "weight": 63940, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 883,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 96427, "update_time": 1500000000},
                        {"first": 1, "weight": 618944, "update_time": 1500000000},
                        {"first": 2, "weight": 284627, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 884,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 96427, "update_time": 1500000000},
                        {"first": 1, "weight": 381123, "update_time": 1500000000},
                        {"first": 2, "weight": 237821, "update_time": 1500000000},
                        {"first": 3, "weight": 245325, "update_time": 1500000000},
                        {"first": 4, "weight": 39302, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 889,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },
                {
                    "keyword_id": 890,
                    "update_time": 1500000000,
                    "uint_values": [2],
                },
                {
                    "keyword_id": 891,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },
                {
                    "keyword_id": 892,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },  # full or partial profile
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        no_keywords=[176, 543, 614, 881, 882, 883, 884, 889, 890, 891, 892],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 174,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 574222, "update_time": 1500000000},
                        {"first": 1, "weight": 425777, "update_time": 1500000000},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 569,
                    "update_time": 1500000000,
                    "pair_values": [
                        {"first": 543, "second": 2},
                        {"first": 176, "second": 1},
                        {"first": 174, "second": 1},
                        {"first": 614, "second": 1},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },  # full or partial profile
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        no_keywords=[543, 569, 614, 881, 882, 883, 884, 889, 890, 891, 892],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 174,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 574222, "update_time": 1500000000},
                        {"first": 1, "weight": 425777, "update_time": 1500000000},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 176,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 96427, "update_time": 1500000000},
                        {"first": 1, "weight": 618944, "update_time": 1500000000},
                        {"first": 2, "weight": 284627, "update_time": 1500000000},
                    ],
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },  # full or partial profile
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        keywords=[174, 176, 564],
        no_keywords=[176, 543, 614, 881, 882, 883, 884, 889, 890, 891, 892],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 174,
                    "update_time": 1500000000,
                    "weighted_uint_values": [
                        {"first": 0, "weight": 574222, "update_time": 1500000000},
                        {"first": 1, "weight": 425777, "update_time": 1500000000},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },  # full or partial profile
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        no_keywords=[174, 176, 543, 569, 614, 881, 882, 883, 884],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },  # full or partial profile
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
