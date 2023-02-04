from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_source_uniq_create_time(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    id3 = test_environment.new_uid()

    create_time_first_profile = 1500000000 - 3600
    create_time_second_profile = 1500000000 - 7200

    test_environment.profiles.add(
        {
            "y{id}".format(id=id1): {
                "CreationTime": create_time_first_profile,
            },
            "y{id}".format(id=id2): {
                "CreationTime": create_time_second_profile,
            },
        },
    )

    test_environment.vulture.add(
        {
            "y{id1}".format(id1=id1): {
                "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
                "ValueRecords": [
                    {"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1},
                    {"user_id": "{id3}".format(id3=id3), "id_type": 1, "crypta_graph_distance": 1},
                ],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000100,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[725],
    )
    check_answer(
        {
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(id1),
                    "is_main": True,
                    "user_id": str(id1),
                    "id_type": 1,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "y{}".format(id1),
                    "create_time": create_time_first_profile,
                },
                {
                    "uniq_id": "y{}".format(id2),
                    "is_main": False,
                    "user_id": str(id2),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(id1),
                    "create_time": create_time_second_profile,
                },
                {
                    "uniq_id": "y{}".format(id3),
                    "is_main": False,
                    "user_id": str(id3),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(id1),
                },
            ],
        },
        result.answer,
        ignored_fields=[
            "items",
            "tsar_vectors",
            "counters",
            "search_pers_profiles",
            "glued_uniqs",
        ],
    )
