from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer
from crypta.lib.python.identifiers import identifiers as id_lib


def test_unnormalized_request(test_environment):
    invalid_idfa = "0123fde95bc96421ce5b5df0ac090ae0"
    idfa = "0123FDE9-5BC9-6421-CE5B-5DF0AC090AE0"
    test_environment.profiles.add(
        {"idfa/{}".format(idfa): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]}}
    )

    result = test_environment.request(
        client="yabs",
        ids={"idfa": invalid_idfa},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 235, "update_time": 1500000000, "uint_values": [123], "source_uniq_index": 0},
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "idfa/{}".format(idfa),
                    "is_main": True,
                    "user_id": idfa,
                    "id_type": 9,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                }
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_normalized_request(test_environment):
    idfa = id_lib.Idfa(str(test_environment.new_uuid())).normalize
    test_environment.profiles.add(
        {"idfa/{}".format(idfa): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]}}
    )

    result = test_environment.request(
        client="yabs",
        ids={"idfa": idfa},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 235, "update_time": 1500000000, "uint_values": [123], "source_uniq_index": 0},
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "idfa/{}".format(idfa),
                    "is_main": True,
                    "user_id": idfa,
                    "id_type": 9,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                }
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_kind_of_unnormalized_request(test_environment):
    oaid = id_lib.Oaid(str(test_environment.new_uuid())).normalize
    unnorm_gaid = "0123FDE95BC96421CE5B5DF0AC090AE0"
    gaid = id_lib.Gaid(unnorm_gaid).normalize
    test_environment.profiles.add(
        {"oaid/{}".format(oaid): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]}}
    )

    test_environment.vulture.add(
        {
            "gaid/{}".format(gaid): {
                "KeyRecord": {"user_id": gaid, "id_type": 8, "is_main": True},
                "ValueRecords": [{"user_id": oaid, "id_type": 15, "crypta_graph_distance": 2}],
            }
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"gaid": unnorm_gaid},
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
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": True,
                    "user_id": gaid,
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "oaid/{}".format(oaid),
                    "is_main": False,
                    "user_id": oaid,
                    "id_type": 15,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_digit_leading_zeros_request(test_environment):
    oaid = id_lib.Oaid(str(test_environment.new_uuid())).normalize
    test_environment.profiles.add(
        {"oaid/{}".format(oaid): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]}}
    )

    yuid = str(test_environment.new_uid())
    test_environment.vulture.add(
        {
            "y{}".format(yuid): {
                "KeyRecord": {"user_id": yuid, "id_type": 1, "is_main": True},
                "ValueRecords": [{"user_id": oaid, "id_type": 15, "crypta_graph_distance": 2}],
            }
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": "000000" + yuid},  # leading zeros skip
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
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
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
                    "uniq_id": "oaid/{}".format(oaid),
                    "is_main": False,
                    "user_id": oaid,
                    "id_type": 15,
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


def test_unnormalized_unknown_devid_request(test_environment):
    invalid = "9123fde95bc96421ce5b5df0ac090ae0"
    idfa = "9123FDE9-5BC9-6421-CE5B-5DF0AC090AE0"
    gaid = "9123fde9-5bc9-6421-ce5b-5df0ac090ae0"
    test_environment.profiles.add(
        {
            "gaid/{}".format(gaid): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [777]}]
            },
            "idfa/{}".format(idfa): {
                "UserItems": [{"keyword_id": 236, "update_time": 1500000000, "uint_values": [123]}]
            },
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"device-id": invalid},
        test_time=1500000000,
        glue_type=GlueType.NO_GLUE,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 235, "update_time": 1500000000, "uint_values": [777], "source_uniq_index": 0},
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": True,
                    "user_id": invalid,
                    "id_type": 12,  # UNKNOWN_DEVICE_ID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
                {
                    "uniq_id": "idfa/{}".format(idfa),
                    "is_main": True,
                    "user_id": invalid,
                    "id_type": 12,  # UNKNOWN_DEVICE_ID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )
