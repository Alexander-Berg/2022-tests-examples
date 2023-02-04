import binascii

from hashlib import md5
from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_mac_ext_md5(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]
            },
            "y{id2}".format(id2=id2): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [17]}]
            },
        }
    )
    mac = "11:22:33:44:55:66"
    mac_str = mac.replace(":", "").replace("\n", "").upper()
    mac_bin = binascii.unhexlify(mac_str)
    mac_ext_md5 = md5(mac_bin).hexdigest().upper()
    vulture_rows = {
        "mem/{_id}".format(_id=mac_ext_md5): {
            "KeyRecord": {"user_id": mac_ext_md5, "id_type": 16},
            "ValueRecords": [
                {"user_id": "{id1}".format(id1=id1), "id_type": 1, "crypta_graph_distance": 1},
                {"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1},
            ],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    result = test_environment.request(
        client="yabs",
        ids={"mac-ext-md5": mac_ext_md5},
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
                    "uint_values": [15],
                    "main": False,
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [17],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{id1}".format(id1=id1),
                    "is_main": False,
                    "user_id": "{id1}".format(id1=id1),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "mem/{_id}".format(_id=mac_ext_md5),
                },
                {
                    "uniq_id": "y{id2}".format(id2=id2),
                    "is_main": False,
                    "user_id": "{id2}".format(id2=id2),
                    "id_type": 1,
                    "crypta_graph_distance": 1,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "mem/{_id}".format(_id=mac_ext_md5),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )
