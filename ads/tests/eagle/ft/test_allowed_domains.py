from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_no_glue(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CounterPack": [
                    {
                        "counter_ids": [200, 201],
                        "keys": [100, 200],
                        "values": [
                            {"float_values": {"value": [1, 0]}},
                            {"fixed32_values": {"value": [1500000000, 1500000000]}},
                        ],
                    },
                ],
                "DelayedCounterUpdates": [
                    {
                        "counter_id": 200,
                        "key": 200,
                        "value": 1,
                        "timestamp": 1500000000,
                        "target_domain_md5": 1000,
                    }
                ],
            }
        }
    )

    test_environment.vulture.add(
        {
            "y{id1}".format(id1=id1): {
                "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        domain_ids="1000",
    )
    check_answer(
        {
            "items": [],
            "counters": [
                {
                    "counter_id": 200,
                    "key": [100, 200],
                    "value": [1, 1],
                },
                {
                    "counter_id": 201,
                    "key": [100, 200],
                    "value": [1500000000, 1500000000],
                },
            ],
            "delayed_counter_updates": [
                {
                    "counter_id": 200,
                    "key": 200,
                    "value": 0,
                    "timestamp": 1500000000,
                    "target_domain_md5": 0,
                },
            ],
        },
        result.answer,
        ignored_fields=[
            "source_uniqs",
            "tsar_vectors",
            "items",
            "search_pers_profiles",
            "glued_uniqs",
        ],
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        domain_ids="",
    )
    check_answer(
        {
            "items": [],
            "counters": [
                {
                    "counter_id": 200,
                    "key": [100, 200],
                    "value": [1, 0],
                },
                {
                    "counter_id": 201,
                    "key": [100, 200],
                    "value": [1500000000, 1500000000],
                },
            ],
            "delayed_counter_updates": [
                {
                    "counter_id": 200,
                    "key": 200,
                    "value": 1,
                    "timestamp": 1500000000,
                    "target_domain_md5": 1000,
                },
            ],
        },
        result.answer,
        ignored_fields=[
            "source_uniqs",
            "tsar_vectors",
            "items",
            "search_pers_profiles",
            "glued_uniqs",
        ],
    )
