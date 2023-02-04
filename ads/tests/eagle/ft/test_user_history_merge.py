from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer, GlueType


def get_rows_for_vulture(bigb_uid, first_icookie, second_icookie):
    return {
        "y{id}".format(id=bigb_uid): {
            "KeyRecord": {"user_id": "{id}".format(id=bigb_uid), "id_type": 1},
            "ValueRecords": [
                {"user_id": "{id}".format(id=first_icookie), "id_type": 1, "crypta_graph_distance": 1},
                {"user_id": "{id}".format(id=second_icookie), "id_type": 1, "crypta_graph_distance": 1},
            ],
        }
    }


def test_search_pers_merge(test_environment):
    bigb_uid = test_environment.new_uid()
    first_icookie = test_environment.new_uid()
    second_icookie = test_environment.new_uid()

    test_environment.search_profiles.add(
        {
            "y{icookie}".format(icookie=first_icookie): {
                "UserHistoryState": {
                    "UserHistory": {
                        "FilteredRecords": [
                            {
                                "Records": [{"Url": "http://yandex.ru"}, {"Url": "http://xednay.ru"}],
                                "Description": {"ContainerId": 1},
                            },
                        ]
                    }
                }
            },
            "y{icookie}".format(icookie=second_icookie): {
                "UserHistoryState": {
                    "UserHistory": {
                        "FilteredRecords": [
                            {
                                "Records": [{"Url": "http://abc.ru"}, {"Url": "http://def.ru"}],
                                "Description": {"ContainerId": 1},
                            },
                        ]
                    }
                }
            },
        }
    )

    test_environment.vulture.add(get_rows_for_vulture(bigb_uid, first_icookie, second_icookie), sync=True)

    exp_json = {"EagleSettings": {"LoadSettings": {"MaxSecondarySearchPersProfilesCount": 3}}}

    result = test_environment.apphost_request(
        client="debug",
        ids={"bigb-uid": bigb_uid},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        exp_json=exp_json,
    )

    check_answer(
        {
            "search_pers_profiles": [
                {
                    "profile": {
                        "UserHistoryState": {
                            "UserHistory": {
                                "FilteredRecords": [
                                    {
                                        "Description": {
                                            "MaxRecords": 3,
                                            "ContainerId": 1,
                                            "StoreOptions": {"StoreUrl": True},
                                        },
                                        "Records": [
                                            {
                                                "Timestamp": 0,
                                                "Dwelltime": 0,
                                                "DocEmbedding": "",
                                                "Url": "http://xednay.ru",
                                                "Flags": {},
                                                "DocEmbeddingModel": 0,
                                                "ReqidHash": 0,
                                            },
                                            {
                                                "Timestamp": 0,
                                                "Dwelltime": 0,
                                                "DocEmbedding": "",
                                                "Url": "http://abc.ru",
                                                "DocEmbeddingModel": 0,
                                                "Flags": {},
                                                "ReqidHash": 0,
                                            },
                                            {
                                                "Timestamp": 0,
                                                "Dwelltime": 0,
                                                "DocEmbedding": "",
                                                "Url": "http://def.ru",
                                                "DocEmbeddingModel": 0,
                                                "Flags": {},
                                                "ReqidHash": 0,
                                            },
                                        ],
                                    }
                                ],
                            }
                        }
                    },
                    "merged_profiles_ids": [
                        "y{bigb_uid}".format(bigb_uid=bigb_uid),
                        "y{icookie}".format(icookie=first_icookie),
                        "y{icookie}".format(icookie=second_icookie),
                    ],
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "glued_uniqs", "tsar_vectors", "items"],
    )


def test_lt_search_merge(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    first_icookie = test_environment_with_kv_saas.new_uid()
    second_icookie = test_environment_with_kv_saas.new_uid()
    first_icookie_key = "y{id}".format(id=first_icookie)
    second_icookie_key = "y{id}".format(id=second_icookie)

    test_environment_with_kv_saas.vulture.add(get_rows_for_vulture(bigb_uid, first_icookie, second_icookie), sync=True)

    exp_json = {"EagleSettings": {"LoadSettings": {"MaxSecondaryLTSearchProfilesCount": 3}}}

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            first_icookie_key: b'2\0\0\0\0\0\0\0(\xB5/\xFD 2u\1\0t\2*0\x12.\x1A,\n\x12"\x10http://yandex.ruxednay.ru\x12\28\1\1\0\2361\xC7',
            second_icookie_key: b',\0\0\0\0\0\0\0(\xB5/\xFD ,E\1\0\x14\2**\x12(\x1A&\n\x0F"\rhttp://abc.rudef.ru\x12\28\1\1\0\xC9\x98M',
        },
        send_empty_response=True,
    ):
        result = test_environment_with_kv_saas.apphost_request(
            client="debug",
            ids={"bigb-uid": bigb_uid},
            test_time=1500000000,
            glue_type=GlueType.VULTURE_CRYPTA,
            exp_json=exp_json,
        )

    check_answer(
        {
            "lt_search_profiles": [
                {
                    "profile": {
                        "UserHistoryState": {
                            "UserHistory": {
                                "FilteredRecords": [
                                    {
                                        "Description": {
                                            "MaxRecords": 3,
                                            "ContainerId": 1,
                                            "StoreOptions": {"StoreUrl": True},
                                        },
                                        "Records": [
                                            {
                                                "Timestamp": 0,
                                                "Dwelltime": 0,
                                                "DocEmbedding": "",
                                                "Url": "http://xednay.ru",
                                                "Flags": {},
                                                "DocEmbeddingModel": 0,
                                                "ReqidHash": 0,
                                            },
                                            {
                                                "Timestamp": 0,
                                                "Dwelltime": 0,
                                                "DocEmbedding": "",
                                                "Url": "http://abc.ru",
                                                "DocEmbeddingModel": 0,
                                                "Flags": {},
                                                "ReqidHash": 0,
                                            },
                                            {
                                                "Timestamp": 0,
                                                "Dwelltime": 0,
                                                "DocEmbedding": "",
                                                "Url": "http://def.ru",
                                                "DocEmbeddingModel": 0,
                                                "Flags": {},
                                                "ReqidHash": 0,
                                            },
                                        ],
                                    }
                                ],
                            }
                        }
                    },
                    "merged_profiles_ids": [
                        "y{bigb_uid}".format(bigb_uid=bigb_uid),
                        "y{icookie}".format(icookie=first_icookie),
                        "y{icookie}".format(icookie=second_icookie),
                    ],
                }
            ]
        },
        result.answer,
        ignored_fields=[
            "source_uniqs",
            "glued_uniqs",
            "tsar_vectors",
            "items",
            "search_pers_profiles",
            "lt_adv_profiles",
        ],
    )
