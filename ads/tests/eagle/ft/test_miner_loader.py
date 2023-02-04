from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer
from ads.bsyeti.tests.eagle.ft.lib.constants import SEARCH_PERS_FAIL


def test_miner_profile(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {"y{id1}".format(id1=id1): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]}}
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
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


def test_miner_vulture(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
            "y{id2}".format(id2=id2): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]
            },
        }
    )
    vulture_rows = {
        "y{id1}".format(id1=id1): {
            "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
            "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1}],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [17],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_miner_user_shows(test_environment):
    id1 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)

    user_shows = [
        {
            "UniqId": yuid,
            "KeywordId": 1168,
            "ItemId": 1,
            "AggregationKey": 1,
            "Count": 1,
            "MinTs": 1500000000,
            "MaxTs": 1500000000,
            "Data": "",
        },
    ]

    test_environment.user_shows._insert_rows(user_shows)
    test_environment.user_shows.ensure_replicated(user_shows)

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[1168, 1169],
    )

    check_answer(
        {
            "order_shows": [{"values": [{"order_id": 1, "shows": [1500000000]}], "source_uniq_index": 0}],
            "frequency_events": [
                {"events": [{"object_id": 1, "event_timestamps": [1500000000], "ml_weights": {}}], "source_uniq_index": 0, "keyword_id": 1169},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_miner_cookies(test_environment):
    id1 = test_environment.new_uid()
    yuid = "y{id}".format(id=id1)

    test_environment.profiles.add(
        {yuid: {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]}}
    )

    cookies = [
        {
            "UniqId": yuid,
            "KeywordId": 234,
            "ValueId": 456,
            "ItemId": 789,
            "UpdateTime": 1500000000,
        }
    ]

    test_environment.cookies._insert_rows(cookies)
    test_environment.cookies.ensure_replicated(cookies)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725, 234],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },
            ],
            "server_side_cookies": [{"UpdateTime": 1500000000, "KeywordId": 234, "ValueId": 456, "Data": ""}],
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_miner_lookup_cookies(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    yuid1 = "y{id}".format(id=id1)
    yuid2 = "y{id}".format(id=id2)

    test_environment.profiles.add(
        {
            yuid1: {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]},
            yuid2: {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]},
        }
    )
    vulture_rows = {
        "y{id1}".format(id1=id1): {
            "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
            "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 4, "crypta_graph_distance": 1}],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    cookies = [
        {
            "UniqId": yuid1,
            "KeywordId": 594,
            "ValueId": 0,
            "ItemId": 0,
            "UpdateTime": 1500000000,
        },
        {
            "UniqId": yuid2,
            "KeywordId": 1126,
            "ValueId": 0,
            "ItemId": 0,
            "UpdateTime": 1400000000,
        },
    ]

    test_environment.cookies._insert_rows(cookies)
    test_environment.cookies.ensure_replicated(cookies)

    result_without_lookup = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725, 234, 594, 253],
    )
    check_answer(
        {
            "server_side_cookies": [
                {
                    "UpdateTime": 1500000000,
                    "KeywordId": 594,
                    "ValueId": 0,
                    "Data": "",
                }
            ]
        },
        result_without_lookup.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "items"],
    )

    exp_json = {"EagleSettings": {"LoadSettings": {"LoadCookiesByCryptaId": True}}}
    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725, 234, 594, 253, 1126],
        exp_json=exp_json,
    )
    check_answer(
        {
            "server_side_cookies": [
                {
                    "UpdateTime": 1500000000,
                    "KeywordId": 594,
                    "ValueId": 0,
                    "Data": "",
                },
                {
                    "UpdateTime": 1400000000,
                    "KeywordId": 1126,
                    "ValueId": 0,
                    "Data": "",
                },
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "items"],
    )


def test_miner_uuid(test_environment):
    uuid = "b44fca396a99538584c7e886c605e168"

    test_environment.profiles.add(
        {
            "y2883633673721896104": {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [19]}]
            },
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"uuid": "{id}".format(id=uuid)},
        test_time=1500000000,
        keywords=[235],
        exp_json={
            "EagleSettings": {"LoadSettings": {"LoadHashedUuids": True, "HashedDeviceidsWithouLimitsCount": 3}}
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [19],
                    "source_uniq_index": 0,
                },
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_miner_search_pers(test_environment):
    bigb_uid = test_environment.new_uid()
    icookie = test_environment.new_uid()

    test_environment.profiles.add(
        {
            "y{bigb_uid}".format(bigb_uid=bigb_uid): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
            "y{icookie}".format(icookie=icookie): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]
            },
        }
    )

    test_environment.search_profiles.add(
        {
            "y{icookie}".format(icookie=icookie): {
                "UserHistoryState": {
                    "LastClick": {"Started": True, "Title": "abracdabra"},
                    "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                }
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": bigb_uid, "icookie": icookie},
        test_time=1500000000,
        keywords=[950],
    )
    check_answer(
        {
            "search_pers_profiles": [
                {
                    "profile": {
                        "UserHistoryState": {
                            "LastClick": {"Started": True, "Title": "abracdabra"},
                            "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                        }
                    },
                    "profile_id": "y{icookie}".format(icookie=icookie),
                }
            ]
        },
        result.answer,
        ignored_fields=["items", "source_uniqs"],
    )


# TODO: Hacky workaround for is_full checking. Move to a separate FT test later.
def test_miner_partial_profile(test_environment):
    bigb_uid = test_environment.new_uid()
    icookie = test_environment.new_uid()

    test_environment.profiles.add(
        {
            "y{bigb_uid}".format(bigb_uid=bigb_uid): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
            "y{icookie}".format(icookie=icookie): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]
            },
        }
    )

    test_environment.search_profiles.add(
        {
            "y{icookie}".format(icookie=icookie): {
                "UserHistoryState": {
                    "LastClick": {"Started": True, "Title": "abracdabra"},
                    "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                }
            }
        }
    )

    exp_json = {"EagleSettings": {"LoadSettings": {"SearchPersTablePath": "//Dummy/Table"}}}

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": bigb_uid, "icookie": icookie},
        test_time=1500000000,
        keywords=[950],
        exp_json=exp_json,
    )
    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": SEARCH_PERS_FAIL,
        },
        result.answer,
        ignored_fields=["items", "source_uniqs"],
    )


def test_miner_do_not_request_main_profile(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
            "y{id2}".format(id2=id2): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]
            },
        }
    )
    vulture_rows = {
        "y{id1}".format(id1=id1): {
            "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
            "ValueRecords": [
                {"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1},
                {
                    "user_id": "AEBE52E7-03EE-455A-B3C4-E57283966239",
                    "id_type": 9,
                    "crypta_graph_distance": 1,
                    "is_indevice": True,
                },
            ],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[730],
    )

    # Since no requests were made to profiles table, empty profile is considered full.
    check_answer(
        {"is_full": True, "user_identifiers": {"InDeviceIdfa": "AEBE52E7-03EE-455A-B3C4-E57283966239"}},
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


# If icookie is not present, then use non-zero main uniq id for searchpers / lt-search requests.
# This test fails when no icookie leads to searchpers / lt-search requests skipping.
def test_do_not_skip_search_data_requests_without_icookie(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    uniq_id = "y{bigb_uid}".format(bigb_uid=bigb_uid)

    # Both search-pers and lt-search data is saved for bigb_uid
    test_environment_with_kv_saas.search_profiles.add(
        {
            uniq_id: {
                "UserHistoryState": {
                    "LastClick": {"Started": True, "Title": "abracdabra"},
                    "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                }
            }
        }
    )

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            uniq_id: b'(\0\0\0\0\0\0\0(\xB5/\xFD (A\1\0*&\n\x0E\x08\1"\nabracdabra\x12\x14\n\x12"\x10http://yandex.ru'
        }
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[950, 1078],
        )

    check_answer(
        {
            "search_pers_profiles": [
                {
                    "profile": {
                        "UserHistoryState": {
                            "LastClick": {"Started": True, "Title": "abracdabra"},
                            "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                        }
                    },
                    "profile_id": uniq_id,
                }
            ],
            "lt_search_profiles": [
                {
                    "profile": {
                        "UserHistoryState": {
                            "LastClick": {"Started": True, "Title": "abracdabra"},
                            "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                        }
                    },
                    "profile_id": uniq_id,
                }
            ],
        },
        result.answer,
        ignored_fields=["items", "source_uniqs"],
    )


# If icookie is equal to 0, then skip searchpers / lt-search requests.
# This test fails when 0 icookie leads to searchpers / lt-search requests with main uniq id.
def test_skip_search_data_requests_with_zero_icookie(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    uniq_id = "y{bigb_uid}".format(bigb_uid=bigb_uid)

    # Both search-pers and lt-search data is saved for bigb_uid
    test_environment_with_kv_saas.search_profiles.add(
        {
            uniq_id: {
                "UserHistoryState": {
                    "LastClick": {"Started": True, "Title": "abracdabra"},
                    "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                }
            }
        }
    )

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            uniq_id: b'(\0\0\0\0\0\0\0(\xB5/\xFD (A\1\0*&\n\x0E\x08\1"\nabracdabra\x12\x14\n\x12"\x10http://yandex.ru'
        }
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid, "icookie": 0},
            test_time=1500000000,
            keywords=[950, 1078],
        )

    # Since icookie: 0 is presented in the requests, it forces miner to skip searchpers / lt-search requests.
    check_answer(
        {
            "is_full": True,
        },
        result.answer,
        ignored_fields=["items", "source_uniqs"],
    )


# Eagle request doesn't return 235 keyword data, because profile loader was disabled.
# Only default 564 keyword (is full profiles) is returned.
def test_disabled_profiles_loader(test_environment_with_disabled_profiles_loader):
    id1 = test_environment_with_disabled_profiles_loader.new_uid()
    test_environment_with_disabled_profiles_loader.profiles.add(
        {"y{id1}".format(id1=id1): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]}}
    )

    result = test_environment_with_disabled_profiles_loader.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },
            ],
            "is_full": True,
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_miner_vulture_with_partial_result_enabled(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
            "y{id2}".format(id2=id2): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]
            },
        }
    )
    vulture_rows = {
        "y{id1}".format(id1=id1): {
            "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
            "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1}],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "EnablePartialResult": True,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [17],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_miner_profile_with_partial_result_enabled(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {"y{id1}".format(id1=id1): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]}}
    )

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "EnablePartialResult": True,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
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
