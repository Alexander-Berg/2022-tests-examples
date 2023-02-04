from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer
from crypta.lib.python.identifiers import identifiers as id_lib


def test_ifv_request(test_environment):
    ifv = id_lib.Ifv(str(test_environment.new_uuid())).normalize
    test_environment.profiles.add(
        {
            "ifv/{}".format(ifv): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]},
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"ifv": ifv},
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
                    "uint_values": [123],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "ifv/{}".format(ifv),
                    "is_main": True,
                    "user_id": ifv,
                    "id_type": 19,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                }
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_ifv_glue_request(test_environment):
    ifv = id_lib.Ifv(str(test_environment.new_uuid())).normalize
    test_environment.profiles.add(
        {
            "ifv/{}".format(ifv): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]},
        }
    )

    yuid = str(test_environment.new_uid())
    test_environment.vulture.add(
        {
            "y{}".format(yuid): {
                "KeyRecord": {"user_id": yuid, "id_type": 1, "is_main": True},
                "ValueRecords": [{"user_id": ifv, "id_type": 19, "crypta_graph_distance": 2}],
            }
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid},
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
                    "uint_values": [123],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 1,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "y{}".format(yuid),
                },
                {
                    "uniq_id": "ifv/{}".format(ifv),
                    "is_main": False,
                    "user_id": ifv,
                    "id_type": 19,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(yuid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )
