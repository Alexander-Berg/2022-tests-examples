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

CDP_SEGMENTS = [
    {
        "keyword_id": keywords_data_pb2.EKeyword.KW_CDP_SEGMENTS,
        "update_time": TS,
        "uint_values": [retargeting_id],
    }
    for retargeting_id in [2600000000, 2600000001]
]

KEYWORDS_FILTER = [
    keywords_data_pb2.EKeyword.KW_BT_COUNTER,
    keywords_data_pb2.EKeyword.KW_SOURCE_UNIQS,
    keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
    keywords_data_pb2.EKeyword.KW_VISIT_GOAL,
    keywords_data_pb2.EKeyword.KW_CDP_SEGMENTS,
    keywords_data_pb2.EKeyword.KW_BIGB_IS_FULL,
]


@pytest.fixture(scope="module")
def uid(test_environment):
    user_id = 10005000  # need fixed id
    test_environment.profiles.add(
        {
            "y{}".format(user_id): {
                "UserItems": VISIT_GOALS,
                "AudienceSegmentsWithPriorities": {
                    "segments_with_priorities": [
                        {"priority": 500, "segments": [1500000000, 1, 2, 3, 4]},
                        {"priority": 100, "segments": [2100000000, 1, 2, 3]},
                        {"priority": 50, "segments": [2100000010, 1, 2, 3]},
                    ],
                    "timestamp": TS,
                },
                "CdpSegments": {
                    "segments": [2600000000, 1],
                    "timestamp": TS,
                },
            },
        }
    )
    return user_id


def test_audience_with_priorities(test_environment, uid):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": uid},
        test_time=TS,
        exp_json={"Crypta": {"Audience": {"UseSegmentsWithPriorities": True, "MaxPublicRecords": 20}}},
        keywords=KEYWORDS_FILTER,
    )

    check_answer(
        {
            "items": VISIT_GOALS
            + [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in [
                    1500000000,
                    1500000001,
                    1500000003,
                    1500000006,
                    1500000010,
                    2100000000,
                    2100000001,
                    2100000003,
                    2100000006,
                    2100000010,
                    2100000011,
                    2100000013,
                    2100000016,
                ]
            ]
            + CDP_SEGMENTS
            + [
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_remove_lal_from_parent(test_environment, uid):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": uid},
        test_time=TS,
        exp_json={"Crypta": {"Audience": {"UseSegmentsWithPriorities": True, "RemoveLalFromParent": True}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": VISIT_GOALS
            + [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in [
                    1500000010,
                    2100000000,
                    2100000001,
                    2100000003,
                    2100000006,
                    2100000010,
                    2100000011,
                    2100000013,
                    2100000016,
                ]
            ]
            + CDP_SEGMENTS
            + [
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_use_priorities_from_dict(test_environment, uid):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": uid},
        test_time=TS,
        exp_json={
            "Crypta": {
                "Audience": {
                    "UseSegmentsWithPriorities": True,
                    "MaxPublicRecords": 6,
                    "UsePriorityDict": True,
                }
            }
        },
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": VISIT_GOALS
            + [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in [
                    1500000001,
                    1500000003,
                    2100000003,
                    2100000006,
                    2100000010,
                    2100000016,
                ]
            ]
            + CDP_SEGMENTS
            + [
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_ignore_unknown_segments(test_environment, uid):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": uid},
        test_time=TS,
        exp_json={
            "Crypta": {
                "Audience": {
                    "UseSegmentsWithPriorities": True,
                    "MaxPublicRecords": 20,
                    "UsePriorityDict": True,
                    "IgnoreUnknownSegments": True,
                }
            }
        },
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": VISIT_GOALS
            + [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in [
                    1500000001,
                    1500000003,
                    2100000003,
                    2100000006,
                    2100000010,
                    2100000011,
                    2100000013,
                    2100000016,
                ]
            ]
            + CDP_SEGMENTS
            + [
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_per_client_settings(test_environment, uid):
    result = test_environment.request(
        client="adfox",
        ids={"bigb-uid": uid},
        test_time=TS,
        exp_json={
            "Crypta": {
                "Audience": {
                    "UseSegmentsWithPriorities": True,
                    "UsePriorityDict": True,
                    "PerClientSettings": {
                        "Values": [
                            {
                                "Client": "adfox",
                                "PriorityDictKey": "adfox",
                                "IgnoreUnknownSegments": True,
                            }
                        ]
                    },
                }
            }
        },
        keywords=[
            keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
        ],
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in [
                    2100000013,
                    2100000016,
                ]
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )


def test_glue_only_main_puid(test_environment):
    yuid = test_environment.new_uid()
    puid = test_environment.new_uid()

    def profile_with_segments(segments):
        if segments:
            segments = [segments[0]] + [b - a for a, b in zip(segments[:-1], segments[1:])]

        return {
            "AudienceSegmentsWithPriorities": {
                "segments_with_priorities": [
                    {"priority": 500, "segments": segments},
                ],
                "timestamp": TS,
            },
        }

    puid_segments = [2000000002, 2000000003]

    test_environment.profiles.add(
        {
            "y{}".format(yuid): profile_with_segments([]),
            "p{}".format(puid): profile_with_segments(puid_segments),
        }
    )

    test_environment.vulture.add(
        {
            "y{id}".format(id=yuid): {
                "KeyRecord": {"user_id": str(yuid), "id_type": 1},
                "ValueRecords": [
                    {
                        "user_id": str(puid),
                        "id_type": 6,
                        "crypta_graph_distance": 1,
                    },
                ],
            }
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": yuid},
        test_time=TS,
        keywords=[
            keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
        ],
    )

    check_answer(
        {"items": []},
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": yuid, "puid": puid},
        test_time=TS,
        keywords=[
            keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
        ],
    )

    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_AUDITORIUM_SEGMENTS,
                    "update_time": TS,
                    "uint_values": [segment],
                }
                for segment in puid_segments
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
