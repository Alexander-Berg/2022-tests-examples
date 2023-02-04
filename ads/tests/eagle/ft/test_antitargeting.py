from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_no_glue(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [663, 664],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    },
                    {
                        "counter_ids": [665, 666],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [10]}},
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
        keywords=[328, 564, 914],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 564, "update_time": 1500000100, "uint_values": [1]},
                {"keyword_id": 914, "update_time": 1500000100, "uint_values": [0]},
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


def test_no_glue_no_with_refer(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [663, 664],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    }
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
        keywords=[328, 564, 914],
    )

    check_answer(
        {
            "items": [
                {"keyword_id": 564, "update_time": 1500000100, "uint_values": [1]},
                {"keyword_id": 914, "update_time": 1500000100, "uint_values": [0]},
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


def test_no_glue_no_without_refer(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [665, 666],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    }
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
        keywords=[328, 564, 914],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 564, "update_time": 1500000100, "uint_values": [1]},
                {"keyword_id": 914, "update_time": 1500000100, "uint_values": [1]},
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


def test_no_glue_default(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [663, 664],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [10]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    },
                    {
                        "counter_ids": [665, 666],
                        "keys": [731962],
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
        keywords=[328, 564, 914],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 564, "update_time": 1500000100, "uint_values": [1]},
                {"keyword_id": 914, "update_time": 1500000100, "uint_values": [1]},
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


def test_glue(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [663, 664],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [15]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    },
                    {
                        "counter_ids": [665, 666],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [10]}},
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
                "CounterPack": [
                    {
                        "counter_ids": [665, 666],
                        "keys": [731962],
                        "values": [
                            {"float_values": {"value": [10]}},
                            {"fixed32_values": {"value": [1500000000]}},
                        ],
                    }
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
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[328, 564, 914],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 564, "update_time": 1500000100, "uint_values": [1]},
                {"keyword_id": 914, "update_time": 1500000100, "uint_values": [0]},
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
