from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_unknown_duid_set(test_environment):
    id1 = test_environment.new_uid()
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1, "duid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[328, 564, 725, 1002],
    )
    check_answer(
        {
            "items": [
                {"keyword_id": 1002, "update_time": 1500000000, "uint_values": [1]},
                {"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_not_unknown_duid(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1, "duid": id2},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[328, 564, 725],
    )
    check_answer(
        {"items": [{"keyword_id": 564, "update_time": 1500000000, "uint_values": [1]}]},
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
