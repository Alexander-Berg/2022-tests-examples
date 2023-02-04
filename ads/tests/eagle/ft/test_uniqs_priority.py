from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer

import binascii

from hashlib import md5


def test_uniqs_priority_distance_mode(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    id3 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
        }
    )
    mac = "11:22:33:44:55:77"
    mac_str = mac.replace(":", "").replace("\n", "").upper()
    mac_bin = binascii.unhexlify(mac_str)
    mac_ext_md5 = md5(mac_bin).hexdigest().upper()
    vulture_rows = {
        "mem/{_id}".format(_id=mac_ext_md5): {
            "KeyRecord": {"user_id": mac_ext_md5, "id_type": 16},
            "ValueRecords": [
                {"user_id": "{id1}".format(id1=id1), "id_type": 1, "crypta_graph_distance": 10},
                {"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1},
                {"user_id": "{id3}".format(id3=id3), "id_type": 1, "crypta_graph_distance": 2},
            ],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "MaxQueryCountBeforeSampling": 0,
                "MaxSecondaryProfilesWithQueriesCount": 0,
                "MaxSecondaryProfiles": 1,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"mac-ext-md5": mac_ext_md5},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "source_uniqs": [
                {
                    "uniq_id": "y{id2}".format(id2=id2),
                    "is_main": False,
                    "user_id": "{id2}".format(id2=id2),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "mem/{_id}".format(_id=mac_ext_md5),
                }
            ],
        },
        result.answer,
        ignored_fields=["items", "tsar_vectors"],
    )


def test_uniqs_priority_weight_mode(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    id3 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
        }
    )
    mac = "11:22:33:44:55:88"
    mac_str = mac.replace(":", "").replace("\n", "").upper()
    mac_bin = binascii.unhexlify(mac_str)
    mac_ext_md5 = md5(mac_bin).hexdigest().upper()
    vulture_rows = {
        "mem/{_id}".format(_id=mac_ext_md5): {
            "KeyRecord": {"user_id": mac_ext_md5, "id_type": 16},
            "ValueRecords": [
                {
                    "user_id": "{id1}".format(id1=id1),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "crypta_graph_weight": 0.1,
                },
                {
                    "user_id": "{id2}".format(id2=id2),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "crypta_graph_weight": 0,
                },
                {
                    "user_id": "{id3}".format(id3=id3),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "crypta_graph_weight": 1,
                },
            ],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "MaxQueryCountBeforeSampling": 0,
                "MaxSecondaryProfilesWithQueriesCount": 0,
                "MaxSecondaryProfiles": 1,
                "CryptaSecondaryWeightMode": 1,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"mac-ext-md5": mac_ext_md5},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "source_uniqs": [
                {
                    "uniq_id": "y{id3}".format(id3=id3),
                    "is_main": False,
                    "user_id": "{id3}".format(id3=id3),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "mem/{_id}".format(_id=mac_ext_md5),
                    "crypta_graph_weight": 1,
                },
            ],
        },
        result.answer,
        ignored_fields=["items", "tsar_vectors"],
    )
