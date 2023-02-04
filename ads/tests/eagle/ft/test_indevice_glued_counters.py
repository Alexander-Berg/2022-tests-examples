from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_in_device_glued_counters(test_environment):
    id = test_environment.new_uid()

    test_environment.profiles.add(
        {
            "y{_id}".format(_id=id): {
                "CounterPack": [
                    {
                        "counter_ids": [977, 978],
                        "keys": [100, 200],
                        "values": [
                            {"float_values": {"value": [1, 0]}},
                            {"fixed32_values": {"value": [1500000000, 1500000000]}},
                        ],
                    },
                ]
            },
        }
    )

    test_environment.vulture.add(
        {
            "y{id}".format(id=id): {
                "KeyRecord": {"user_id": "{_id}".format(_id=id), "id_type": 1},
                "ValueRecords": [
                    {
                        "user_id": "00000000-0000-0000-0000-000000000001",
                        "id_type": 9,
                        "crypta_graph_distance": 2,
                        "is_indevice": True,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000002",
                        "id_type": 8,
                        "crypta_graph_distance": 1,
                        "is_indevice": True,
                    },
                    {"user_id": "{id}".format(id=id), "id_type": 1, "crypta_graph_distance": 1},
                    {"user_id": "y777777", "id_type": 2, "crypta_graph_distance": 123},
                ],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        domain_ids="",
    )

    check_answer(
        {
            "items": [],
            "counters": [
                {
                    "counter_id": 977,
                    "key": [100],
                    "value": [1],
                },
                {
                    "counter_id": 978,
                    "key": [100],
                    "value": [1500000000],
                },
                {
                    "counter_id": 991,
                    "key": [100, 200],
                    "value": [1, 0],
                },
                {
                    "counter_id": 992,
                    "key": [100, 200],
                    "value": [1500000000, 1500000000],
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
            "user_identifiers",
        ],
    )
