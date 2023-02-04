import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


@pytest.fixture(scope="module")
def user_ids(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()

    ssp_user_id1 = "10001@ABC"
    ssp_id2 = 10002
    ssp_uid2 = "def"
    ssp_user_id2 = "10002@DEF"

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
        "ssp_user_id/{_id}".format(_id=ssp_user_id1): {
            "KeyRecord": {"user_id": ssp_user_id1, "id_type": 20},
            "ValueRecords": [{"user_id": str(id1), "id_type": 1, "crypta_graph_distance": 1}],
        },
        "ssp_user_id/{_id}".format(_id=ssp_user_id2): {
            "KeyRecord": {"user_id": ssp_user_id2, "id_type": 20},
            "ValueRecords": [{"user_id": str(id2), "id_type": 1, "crypta_graph_distance": 1}],
        },
    }
    test_environment.vulture.add(vulture_rows, sync=True)
    d = {
        "id1": id1,
        "id2": id2,
        "ssp_user_id1": ssp_user_id1,
        "ssp_user_id2": ssp_user_id2,
        "ssp_id2": ssp_id2,
        "ssp_uid2": ssp_uid2,
    }
    return d


def test_ssp_user_id(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"ssp-user-id-full": user_ids["ssp_user_id1"]},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={"EagleSettings": {"LoadSettings": {"UseSspUserId": True}}},
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
                    "source_uniq_index": 0,
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_ssp_user_id_priority(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={
            "ssp-user-id-full": user_ids["ssp_user_id1"],
            "ssp-user-id": user_ids["ssp_uid2"],
            "ssp-id": user_ids["ssp_id2"],
        },
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={"EagleSettings": {"LoadSettings": {"UseSspUserId": True}}},
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [15],
                    "source_uniq_index": 0,
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_ssp_user_id_combine(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={
            "ssp-user-id": user_ids["ssp_uid2"],
            "ssp-id": user_ids["ssp_id2"],
        },
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json={"EagleSettings": {"LoadSettings": {"UseSspUserId": True}}},
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [17],
                    "source_uniq_index": 0,
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
