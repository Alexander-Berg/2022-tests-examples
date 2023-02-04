import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


@pytest.fixture(scope="module")
def profiles(test_environment):
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
    vulture_rows = {
        "y{id1}".format(id1=id1): {
            "KeyRecord": {"user_id": "{id1}".format(id1=id1), "id_type": 1},
            "ValueRecords": [{"user_id": "{id2}".format(id2=id2), "id_type": 1, "crypta_graph_distance": 1}],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)
    return id1, id2


def test_vulture_glue(test_environment, profiles):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": profiles[0]},
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
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_no_glue(test_environment, profiles):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": profiles[0]},
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
                    "uint_values": [15],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                },  # full or partial profile
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


protobuf_answer = {
    "items": [
        {
            "keyword_id": 235,
            "update_time": 1500000000,
            "uint_values": [15],
            "source_uniq_index": 0,
        },
        {
            "keyword_id": 235,
            "update_time": 1500000000,
            "uint_values": [17],
            "main": False,
            "source_uniq_index": 1,
        },
        {
            "keyword_id": 564,
            "update_time": 1500000000,
            "uint_values": [1],
        },
    ]
}


def test_vulture_glue_apphost(test_environment, profiles):
    result = test_environment.apphost_request(
        client="yabs",
        ids={"bigb-uid": profiles[0]},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
    )
    check_answer(
        protobuf_answer,
        result.answer,
        ignored_fields=["source_uniqs", "glued_uniqs", "tsar_vectors"],
    )


def test_vulture_glue_apphost_protobuf_json(test_environment, profiles):
    result = test_environment.apphost_request(
        client="yabs",
        ids={"bigb-uid": profiles[0]},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        resp_format="protobuf-json",
    )
    check_answer(
        protobuf_answer,
        result.answer,
        ignored_fields=["source_uniqs", "glued_uniqs", "tsar_vectors"],
    )
