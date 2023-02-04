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
    for retargeting_id in [1001, 1000000001, 3000000001, 4000000001]
]


@pytest.fixture(scope="module")
def uid(test_environment):
    uniq_id = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{}".format(uniq_id): {
                "UserItems": VISIT_GOALS,
                "CdpSegments": {
                    "segments": [2600000000, 1, 2, 3],
                    "timestamp": TS,
                },
            },
        }
    )
    yield uniq_id


def test_cdp_segments(test_environment, uid):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": uid},
        test_time=TS,
        keywords=[
            keywords_data_pb2.EKeyword.KW_BT_COUNTER,
            keywords_data_pb2.EKeyword.KW_CDP_SEGMENTS,
            keywords_data_pb2.EKeyword.KW_VISIT_GOAL,
            keywords_data_pb2.EKeyword.KW_BIGB_IS_FULL,
        ],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_CDP_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in [2600000000, 2600000001, 2600000003, 2600000006]
            ]
            + VISIT_GOALS
            + [
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
