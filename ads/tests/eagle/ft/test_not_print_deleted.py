# -*- coding: utf-8 -*-
import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer
from yabs.server.proto.keywords import keywords_data_pb2

TS = 1500000000
VISIT_GOALS = [
    {
        "keyword_id": keywords_data_pb2.EKeyword.KW_VISIT_GOAL,
        "update_time": TS,
        "uint_values": [retargeting_id],
        "source_uniq_index": 0,
    }
    # fmt: off
    for retargeting_id in [
        1001, 1000000001, 2000000001,  # 0, 1, 2
        3000000001, 4000000001, 5000000001,  # 3, 4, 5
        6000000001, 7000000001, 8000000001,  # 6, 7, 8
        9000000001, 9100000001, 9200000001, 9300000001,  # 9, 10, 11, 12
    ]
    # fmt: on
]


def patch_visit_goals():
    for i, e in enumerate(VISIT_GOALS):
        if i % 2 == 0:
            e["deleted"] = True
        if i == 2:
            e["revision"] = 12345
        if i == 4:
            e["revision"] = 0

        if i == 3:
            e["deleted"] = False
        if i == 5:
            e["revision"] = 1337
        if i == 7:
            e["deleted"] = False
            e["revision"] = 1337
        if i == 9:
            e["deleted"] = False
            e["revision"] = 0
        if i == 11:
            e["revision"] = 0


@pytest.fixture(scope="module")
def uid(test_environment):
    patch_visit_goals()
    uniq_id = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{}".format(uniq_id): {
                "UserItems": VISIT_GOALS,
            },
        }
    )
    yield uniq_id


def test_not_print_deleted(test_environment, uid):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": uid},
        test_time=TS,
        keywords=[
            keywords_data_pb2.EKeyword.KW_VISIT_GOAL,
            keywords_data_pb2.EKeyword.KW_BIGB_IS_FULL,
        ],
    )
    filtered_vg = [i for i in VISIT_GOALS if not i.get("deleted")]
    assert len(filtered_vg) == len(VISIT_GOALS) // 2
    assert tuple([i["uint_values"][0] for i in filtered_vg]) == tuple(
        [1000000001, 3000000001, 5000000001, 7000000001, 9000000001, 9200000001]
    )

    check_answer(
        {"items": filtered_vg + [{"keyword_id": 564, "update_time": TS, "uint_values": [1]}]},
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
