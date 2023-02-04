import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


@pytest.fixture(scope="module")
def user_ids(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()

    ehash1 = "2194c06f085b5f489d9956b4517f7f39"
    ehash2 = "340b88a9f589e9301af2b3b256513086"
    email2 = "hasanurhan@yandex.ru"

    phash1 = "3ac5b42a336f051a3295eb6d6266c32b"
    phash2 = "99e421d0f4183866e987eaacecee76b1"
    phone2 = "+79183938035"

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
        "emdh/{_id}".format(_id=ehash1): {
            "KeyRecord": {"user_id": ehash1, "id_type": 17},
            "ValueRecords": [{"user_id": str(id1), "id_type": 1, "crypta_graph_distance": 1}],
        },
        "emdh/{_id}".format(_id=ehash2): {
            "KeyRecord": {"user_id": ehash2, "id_type": 17},
            "ValueRecords": [{"user_id": str(id2), "id_type": 1, "crypta_graph_distance": 1}],
        },
        "pmdh/{_id}".format(_id=phash1): {
            "KeyRecord": {"user_id": phash1, "id_type": 18},
            "ValueRecords": [{"user_id": str(id1), "id_type": 1, "crypta_graph_distance": 1}],
        },
        "pmdh/{_id}".format(_id=phash2): {
            "KeyRecord": {"user_id": phash2, "id_type": 18},
            "ValueRecords": [{"user_id": str(id2), "id_type": 1, "crypta_graph_distance": 1}],
        },
    }
    test_environment.vulture.add(vulture_rows, sync=True)
    d = {
        "id1": id1,
        "id2": id2,
        "phone2": phone2,
        "email2": email2,
        "ehash1": ehash1,
        "ehash2": ehash2,
        "phash1": phash1,
        "phash2": phash2,
    }
    return d


def test_email_md5(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"email-md5": user_ids["ehash1"]},
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
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
    result = test_environment.request(
        client="yabs",
        ids={"email-md5": user_ids["ehash2"]},
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


def test_email_args_priority(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"email-md5": user_ids["ehash1"], "email": user_ids["email2"]},
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
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_email_plain(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"email": user_ids["email2"]},
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


def test_phone_md5(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"phone-md5": user_ids["phash1"]},
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
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
    result = test_environment.request(
        client="yabs",
        ids={"phone-md5": user_ids["phash2"]},
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


def test_phone_args_priority(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"phone-md5": user_ids["phash1"], "phone": user_ids["phone2"]},
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
                    "main": False,
                },
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_phone_plain(test_environment, user_ids):
    result = test_environment.request(
        client="yabs",
        ids={"phone": user_ids["phone2"]},
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
