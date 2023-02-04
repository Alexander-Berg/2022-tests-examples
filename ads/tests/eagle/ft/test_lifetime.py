from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer

# There is a CounterPack in every test because without any other data CreationTime will be removed


def test_no_glue(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CreationTime": 1500000000 - 3600,
                "CounterPack": [
                    {
                        "counter_ids": [7, 8],
                        "keys": [0],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    },
                ],
            }
        }
    )

    test_environment.vulture.add(
        {
            "y{id1}".format(id1=id1): {
                "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
                "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1}],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000100,
        glue_type=GlueType.NO_GLUE,
        keywords=[328, 1035],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 1035, "update_time": 1500000100, "uint_values": [3700]},
            ],
        },
        result.answer,
        ignored_fields=[
            "source_uniqs",
            "tsar_vectors",
            "counters",
            "search_pers_profiles",
            "glued_uniqs",
        ],
    )


def test_no_glue_no_creation_time(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [7, 8],
                        "keys": [0],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    },
                ],
            }
        }
    )

    test_environment.vulture.add(
        {
            "y{id1}".format(id1=id1): {
                "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
                "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1}],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000100,
        glue_type=GlueType.NO_GLUE,
        keywords=[328, 1035],
    )

    check_answer(
        {},
        result.answer,
        ignored_fields=[
            "source_uniqs",
            "tsar_vectors",
            "counters",
            "search_pers_profiles",
            "glued_uniqs",
        ],
    )


def test_glue(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CreationTime": 1500000000 - 3600,
                "CounterPack": [
                    {
                        "counter_ids": [7, 8],
                        "keys": [0],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    },
                ],
            }
        }
    )

    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id2): {
                "CreationTime": 1500000000 - 7200,
            }
        }
    )

    test_environment.vulture.add(
        {
            "y{id1}".format(id1=id1): {
                "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
                "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1}],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000100,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[328, 1035],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 1035, "update_time": 1500000100, "uint_values": [7300]},
            ],
        },
        result.answer,
        ignored_fields=[
            "source_uniqs",
            "tsar_vectors",
            "counters",
            "search_pers_profiles",
            "glued_uniqs",
        ],
    )
