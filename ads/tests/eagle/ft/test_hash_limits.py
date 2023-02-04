from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer
from crypta.lib.python.identifiers import identifiers as id_lib


def prepare_env(test_environment):
    oaid = id_lib.Oaid(str(test_environment.new_uuid()))
    gaid = id_lib.Gaid(str(test_environment.new_uuid()))
    mmid = id_lib.MmDeviceId(str(test_environment.new_uuid()))

    test_environment.profiles.add(
        {
            "oaid/{}".format(oaid.normalize): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [234]}]
            }
        }
    )

    test_environment.vulture.add(
        {
            "gaid/{}".format(gaid.normalize): {
                "KeyRecord": {"user_id": gaid.normalize, "id_type": 8, "is_main": True},
                "ValueRecords": [
                    {"user_id": mmid.normalize, "id_type": 10, "crypta_graph_distance": 2},
                    {"user_id": oaid.normalize, "id_type": 15, "crypta_graph_distance": 3},
                ],
            }
        }
    )
    return gaid, oaid, mmid


def test_hash_limit_request(test_environment):
    gaid, oaid, mmid = prepare_env(test_environment)

    result = test_environment.request(
        client="debug",
        ids={"gaid": gaid.normalize},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={
            "EagleSettings": {"LoadSettings": {"LoadHashedDeviceIds": True, "HashedDeviceidsWithouLimitsCount": 3}}
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [234],
                    "main": False,
                    "source_uniq_index": 3,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "y{}".format(gaid.half),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "mm_device_id/{}".format(mmid),
                    "is_main": False,
                    "user_id": str(mmid),
                    "id_type": 10,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "oaid/{}".format(oaid),
                    "is_main": False,
                    "user_id": str(oaid),
                    "id_type": 15,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "y{}".format(oaid.half),
                    "is_main": False,
                    "user_id": str(oaid),
                    "id_type": 15,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "y{}".format(mmid.half),
                    "is_main": False,
                    "user_id": str(mmid),
                    "id_type": 10,
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


def test_hash_limit_reached_request(test_environment):
    gaid, oaid, mmid = prepare_env(test_environment)

    result = test_environment.request(
        client="debug",
        ids={"gaid": gaid.normalize},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={
            "EagleSettings": {"LoadSettings": {"LoadHashedDeviceIds": True, "HashedDeviceidsWithouLimitsCount": 1}}
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [234],
                    "main": False,
                    "source_uniq_index": 3,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "y{}".format(gaid.half),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "mm_device_id/{}".format(mmid),
                    "is_main": False,
                    "user_id": str(mmid),
                    "id_type": 10,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "oaid/{}".format(oaid),
                    "is_main": False,
                    "user_id": str(oaid),
                    "id_type": 15,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "y{}".format(mmid.half),
                    "is_main": False,
                    "user_id": str(mmid),
                    "id_type": 10,
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


def test_hash_limit_zero_request(test_environment):
    gaid, oaid, mmid = prepare_env(test_environment)

    result = test_environment.request(
        client="debug",
        ids={"gaid": gaid.normalize},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={
            "EagleSettings": {"LoadSettings": {"LoadHashedDeviceIds": True, "HashedDeviceidsWithouLimitsCount": 0}}
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [234],
                    "main": False,
                    "source_uniq_index": 3,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "y{}".format(gaid.half),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "mm_device_id/{}".format(mmid),
                    "is_main": False,
                    "user_id": str(mmid),
                    "id_type": 10,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "oaid/{}".format(oaid),
                    "is_main": False,
                    "user_id": str(oaid),
                    "id_type": 15,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_hash_disabled_request(test_environment):
    gaid, oaid, mmid = prepare_env(test_environment)

    result = test_environment.request(
        client="debug",
        ids={"gaid": gaid.normalize},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={
            "EagleSettings": {"LoadSettings": {"LoadHashedDeviceIds": False, "HashedDeviceidsWithouLimitsCount": 3}}
        },
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [234],
                    "main": False,
                    "source_uniq_index": 2,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1], "source_uniq_index": -1},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": True,
                    "user_id": str(gaid),
                    "id_type": 8,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "mm_device_id/{}".format(mmid),
                    "is_main": False,
                    "user_id": str(mmid),
                    "id_type": 10,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
                {
                    "uniq_id": "oaid/{}".format(oaid),
                    "is_main": False,
                    "user_id": str(oaid),
                    "id_type": 15,
                    "crypta_graph_distance": 3,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "gaid/{}".format(gaid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )
