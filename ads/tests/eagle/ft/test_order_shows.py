from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_user_shows_fast_only(test_environment):
    id1 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)
    main_time = 1500000000

    user_shows = [
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 1,
            "AggregationKey": 1,
            "Count": 1,
            "MinTs": main_time,
            "MaxTs": main_time,
            "Data": "",
        },
    ]

    test_environment.user_shows._insert_rows(user_shows)
    test_environment.user_shows.ensure_replicated(user_shows)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=main_time,
        glue_type=GlueType.NO_GLUE,
        keywords=[1168, 1169],
    )

    check_answer(
        {"order_shows": [{"values": [{"order_id": 1, "shows": [main_time]}], "source_uniq_index": 0}]},
        result.answer,
        ignored_fields=["source_uniqs", "user_shows", "tsar_vectors", "frequency_events"],
    )


def test_user_shows_freq_fast_only(test_environment):
    id1 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)
    main_time = 1500000000

    user_shows = [
        {
            "UniqId": yuid,
            "KeywordId": 1225,
            "ItemId": 1,
            "AggregationKey": 1,
            "Count": 1,
            "MinTs": main_time,
            "MaxTs": main_time,
            "Data": "",
        },
    ]

    test_environment.user_shows._insert_rows(user_shows)
    test_environment.user_shows.ensure_replicated(user_shows)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=main_time,
        glue_type=GlueType.NO_GLUE,
        keywords=[1219, 1225],
    )

    check_answer(
        {"frequency_events": [{"events": [{"object_id": 1, "event_timestamps": [main_time]}], "source_uniq_index": 0, "keyword_id": 1219}]},
        result.answer,
        ignored_fields=["source_uniqs", "user_shows", "tsar_vectors"],
    )


def test_user_shows_fast_only_vulture(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)
    cid = "y{id}".format(id=id2)
    main_time = 1500000000

    user_shows = [
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 1,
            "AggregationKey": 1,
            "Count": 1,
            "MinTs": main_time,
            "MaxTs": main_time,
            "Data": "",
        },
        {
            "UniqId": cid,
            "KeywordId": 1168,
            "ItemId": 1,
            "AggregationKey": 2,
            "Count": 1,
            "MinTs": main_time,
            "MaxTs": main_time,
            "Data": "",
        },
    ]
    test_environment.user_shows._insert_rows(user_shows)
    test_environment.user_shows.ensure_replicated(user_shows)

    vulture_rows = {
        yuid: {
            "KeyRecord": {"user_id": yuid, "id_type": 1},
            "ValueRecords": [{"user_id": str(id2), "id_type": 4, "crypta_graph_distance": 1}],
        },
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    exp_json = {"EagleSettings": {"LoadSettings": {"LoadUserShowsByCryptaId": True}}}

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=main_time,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[1168, 1169],
        exp_json=exp_json,
    )

    check_answer(
        {"order_shows": [
            {"values": [{"order_id": 1, "shows": [main_time]}], "source_uniq_index": 0},
            {"values": [{"order_id": 1, "shows": [main_time]}], "source_uniq_index": 1}
        ]},
        result.answer,
        ignored_fields=["source_uniqs", "user_shows", "tsar_vectors", "frequency_events"],
    )


def test_user_shows_slow_only(test_environment):
    id1 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)
    main_time = 1500000000

    test_environment.profiles.add(
        {
            yuid: {
                "OrderShows": [
                    {
                        "order_id": 12,
                        "shows": [main_time - 100, main_time - 100, main_time - 10, main_time - 10, main_time - 1],
                        "ml_weights": {"alpha": 101, "beta": 2, "gamma": 1337},
                    },
                    {
                        "order_id": 14,
                        "shows": [main_time - 54, main_time - 51],
                        "ml_weights": {"alpha": 12312, "beta": 13241, "gamma": 32},
                    },
                    {
                        "order_id": 119,
                        "shows": [
                            main_time - 7200,
                            main_time - 4,
                            main_time - 3,
                            main_time - 3,
                            main_time - 3,
                            main_time - 3,
                            main_time - 2,
                        ],
                        "ml_weights": {"alpha": 23432, "beta": 123, "gamma": 4442},
                    },
                ]
            },
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=main_time,
        glue_type=GlueType.NO_GLUE,
        keywords=[1168, 1169],
    )

    check_answer(
        {
            "order_shows": [
                {
                    "values": [
                        {
                            "order_id": 12,
                            "shows": [main_time - 100, main_time - 100, main_time - 10, main_time - 10, main_time - 1],
                            "ml_weights": {"alpha": 101, "beta": 2, "gamma": 1337},
                        },
                        {
                            "order_id": 14,
                            "shows": [main_time - 54, main_time - 51],
                            "ml_weights": {"alpha": 12312, "beta": 13241, "gamma": 32},
                        },
                        {
                            "order_id": 119,
                            "shows": [
                                main_time - 7200,
                                main_time - 4,
                                main_time - 3,
                                main_time - 3,
                                main_time - 3,
                                main_time - 3,
                                main_time - 2,
                            ],
                            "ml_weights": {"alpha": 23432, "beta": 123, "gamma": 4442},
                        },
                    ],
                    "source_uniq_index": 0,
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "user_shows", "tsar_vectors", "frequency_events"],
    )


def test_user_shows_slow_only_vulture(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)
    cid = "y{id}".format(id=id2)
    main_time = 1500000000

    test_environment.profiles.add(
        {
            yuid: {
                "OrderShows": [
                    {
                        "order_id": 12,
                        "shows": [main_time - 100, main_time - 100, main_time - 10, main_time - 10, main_time - 1],
                        "ml_weights": {"alpha": 101, "beta": 2, "gamma": 1337},
                    },
                ]
            },
            cid: {
                "OrderShows": [
                    {
                        "order_id": 14,
                        "shows": [main_time - 54, main_time - 51],
                        "ml_weights": {"alpha": 12312, "beta": 13241, "gamma": 32},
                    },
                ]
            },
        }
    )

    vulture_rows = {
        yuid: {
            "KeyRecord": {"user_id": yuid, "id_type": 1},
            "ValueRecords": [{"user_id": str(id2), "id_type": 4, "crypta_graph_distance": 1}],
        },
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    exp_json = {"EagleSettings": {"LoadSettings": {"LoadUserShowsByCryptaId": True}}}

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=main_time,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[1168, 1169],
        exp_json=exp_json,
    )

    check_answer(
        {
            "order_shows": [
                {
                    "values": [
                        {
                            "order_id": 12,
                            "shows": [main_time - 100, main_time - 100, main_time - 10, main_time - 10, main_time - 1],
                            "ml_weights": {"alpha": 101, "beta": 2, "gamma": 1337},
                        },
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "values": [
                        {
                            "order_id": 14,
                            "shows": [main_time - 54, main_time - 51],
                            "ml_weights": {"alpha": 12312, "beta": 13241, "gamma": 32},
                        },
                    ],
                    "source_uniq_index": 1,
                },
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "user_shows", "tsar_vectors", "frequency_events"],
    )


def test_user_shows_mixed(test_environment):
    id1 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)
    main_time = 1500000000

    test_environment.profiles.add(
        {
            yuid: {
                "OrderShows": [
                    {
                        "order_id": 12,
                        "shows": [main_time - 100, main_time - 100, main_time - 10, main_time - 10, main_time - 1],
                        "ml_weights": {"alpha": 101, "beta": 2, "gamma": 1337},
                    },
                    {
                        "order_id": 14,
                        "shows": [main_time - 54, main_time - 51],
                        "ml_weights": {"alpha": 12312, "beta": 13241, "gamma": 32},
                    },
                    {
                        "order_id": 119,
                        "shows": [
                            main_time - 7200,
                            main_time - 4,
                            main_time - 3,
                            main_time - 3,
                            main_time - 2,
                        ],
                        "ml_weights": {"alpha": 23432, "beta": 123, "gamma": 4442},
                    },
                ]
            },
        }
    )

    user_shows = [
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 6,
            "AggregationKey": 1,
            "Count": 5,
            "MinTs": main_time - 18,
            "MaxTs": main_time - 10,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 6,
            "AggregationKey": 2,
            "Count": 2,
            "MinTs": main_time - 9,
            "MaxTs": main_time - 8,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 6,
            "AggregationKey": 3,
            "Count": 1,
            "MinTs": main_time - 7,
            "MaxTs": main_time - 7,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 6,
            "AggregationKey": 4,
            "Count": 3,
            "MinTs": main_time - 6,
            "MaxTs": main_time - 4,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 14,
            "AggregationKey": 1,
            "Count": 10000,
            "MinTs": main_time - 60,
            "MaxTs": main_time - 60,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 14,
            "AggregationKey": 2,
            "Count": 10000,
            "MinTs": main_time - 51,
            "MaxTs": main_time - 51,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 119,
            "AggregationKey": int(main_time / 3600) - 3,
            "Count": 10000,
            "MinTs": main_time - 100,
            "MaxTs": main_time - 100,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 119,
            "AggregationKey": int(main_time / 3600) - 1,
            "Count": 10000,
            "MinTs": main_time - 100,
            "MaxTs": main_time - 100,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 119,
            "AggregationKey": int(main_time / 3600),
            "Count": 3,
            "MinTs": main_time - 3,
            "MaxTs": main_time - 2,
            "Data": "",
        },
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 119,
            "AggregationKey": int(main_time / 3600) + 1,
            "Count": 10,
            "MinTs": main_time,
            "MaxTs": main_time + 9,
            "Data": "",
        },
    ]

    test_environment.user_shows._insert_rows(user_shows)
    test_environment.user_shows.ensure_replicated(user_shows)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=main_time,
        glue_type=GlueType.NO_GLUE,
        keywords=[1168, 1169],
    )

    check_answer(
        {
            "order_shows": [
                {
                    "values": [
                        {
                            "order_id": 12,
                            "shows": [main_time - 100, main_time - 100, main_time - 10, main_time - 10, main_time - 1],
                            "ml_weights": {"alpha": 101, "beta": 2, "gamma": 1337},
                        },
                        {
                            "order_id": 14,
                            "shows": [main_time - 54, main_time - 51],
                            "ml_weights": {"alpha": 12312, "beta": 13241, "gamma": 32},
                        },
                        {
                            "order_id": 119,
                            "shows": [
                                main_time + i for i in [-7200, -4, -3, -3, -2, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
                            ],
                            "ml_weights": {},
                        },
                        {
                            "order_id": 6,
                            "shows": [main_time + i for i in [-18, -16, -14, -12, -10, -9, -8, -7, -6, -5, -4]],
                            "ml_weights": {},
                        },
                    ],
                    "source_uniq_index": 0,
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "user_shows", "tsar_vectors", "frequency_events"],
    )
