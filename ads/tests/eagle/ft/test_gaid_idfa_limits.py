from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer
from crypta.lib.python.identifiers import identifiers as id_lib


def test_gaid_limit_request(test_environment):
    uid = test_environment.new_uid()
    first_gaid = id_lib.Gaid(str(test_environment.new_uuid()))
    second_gaid = id_lib.Gaid(str(test_environment.new_uuid()))
    third_gaid = id_lib.Gaid(str(test_environment.new_uuid()))

    test_environment.profiles.add(
        {"y{}".format(uid): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [234]}]}}
    )

    test_environment.vulture.add(
        {
            "y{}".format(uid): {
                "KeyRecord": {"user_id": "{}".format(uid), "id_type": 1, "is_main": True},
                "ValueRecords": [
                    {"user_id": first_gaid.normalize, "id_type": 8, "crypta_graph_distance": 2},
                    {"user_id": second_gaid.normalize, "id_type": 8, "crypta_graph_distance": 3},
                    {"user_id": third_gaid.normalize, "id_type": 8, "crypta_graph_distance": 4},
                ],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": uid},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={
            "EagleSettings": {
                "LoadSettings": {
                    "GaidWithoutLimitsCount": 1,
                    "ProfileIdTypeLimits": [
                        {
                            "IdType": 8,
                            "Limit": 1,
                        }
                    ],
                }
            }
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [234],
                    "source_uniq_index": 0,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(uid),
                    "is_main": True,
                    "user_id": str(uid),
                    "id_type": 1,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "y{}".format(uid),
                },
                {
                    "uniq_id": "gaid/{}".format(first_gaid),
                    "user_id": str(first_gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(uid),
                },
                {
                    "uniq_id": "gaid/{}".format(second_gaid),
                    "user_id": str(second_gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(uid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_idfa_limit_request(test_environment):
    uid = test_environment.new_uid()
    first_idfa = id_lib.Idfa(str(test_environment.new_uuid()))
    second_idfa = id_lib.Idfa(str(test_environment.new_uuid()))
    third_idfa = id_lib.Idfa(str(test_environment.new_uuid()))

    test_environment.profiles.add(
        {"y{}".format(uid): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [234]}]}}
    )

    test_environment.vulture.add(
        {
            "y{}".format(uid): {
                "KeyRecord": {"user_id": "{}".format(uid), "id_type": 1, "is_main": True},
                "ValueRecords": [
                    {"user_id": first_idfa.normalize, "id_type": 9, "crypta_graph_distance": 2},
                    {"user_id": second_idfa.normalize, "id_type": 9, "crypta_graph_distance": 3},
                    {"user_id": third_idfa.normalize, "id_type": 9, "crypta_graph_distance": 4},
                ],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": uid},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={
            "EagleSettings": {
                "LoadSettings": {
                    "IdfaWithoutLimitsCount": 1,
                    "ProfileIdTypeLimits": [
                        {
                            "IdType": 9,
                            "Limit": 1,
                        }
                    ],
                }
            }
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [234],
                    "source_uniq_index": 0,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(uid),
                    "is_main": True,
                    "user_id": str(uid),
                    "id_type": 1,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "y{}".format(uid),
                },
                {
                    "uniq_id": "idfa/{}".format(first_idfa),
                    "user_id": str(first_idfa),
                    "id_type": 9,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(uid),
                },
                {
                    "uniq_id": "idfa/{}".format(second_idfa),
                    "user_id": str(second_idfa),
                    "id_type": 9,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(uid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )
