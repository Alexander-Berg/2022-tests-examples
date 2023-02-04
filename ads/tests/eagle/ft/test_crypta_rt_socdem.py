# -*- coding: utf-8 -*-
import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer
from yabs.server.proto.keywords import keywords_data_pb2

TS = 1500000000

KEYWORDS_FILTER = [
    keywords_data_pb2.EKeyword.KW_BT_COUNTER,
    keywords_data_pb2.EKeyword.KW_CRYPTA_SHORTTERM_INTERESTS,
    keywords_data_pb2.EKeyword.KW_CRYPTA_MAX_SOCDEM,
    keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER,
    keywords_data_pb2.EKeyword.KW_CRYPTA_USER_AGE_6S,
    keywords_data_pb2.EKeyword.KW_CRYPTA_INCOME_5_SEGMENTS,
    keywords_data_pb2.EKeyword.KW_KRYPTA_USER_REVENUE,
    keywords_data_pb2.EKeyword.KW_SOURCE_UNIQS,
    keywords_data_pb2.EKeyword.KW_BIGB_IS_FULL,
    keywords_data_pb2.EKeyword.KW_AWAPS_BMCATEGORIES,
]

INTERESTS_KW = [
    {"keyword_id": 602, "update_time": TS, "uint_values": [12]},
    {"keyword_id": 602, "update_time": TS, "uint_values": [122]},
    {"keyword_id": 409, "update_time": TS, "uint_values": [200000073]},
    {"keyword_id": 409, "update_time": TS, "uint_values": [200000079]},
    {"keyword_id": 409, "update_time": TS, "uint_values": [200000244]},
    {"keyword_id": 409, "update_time": TS, "uint_values": [200000246]},
    {"keyword_id": 409, "update_time": TS, "uint_values": [200000435]},
    {"keyword_id": 602, "update_time": TS, "uint_values": [216]},
    {"keyword_id": 602, "update_time": TS, "uint_values": [226]},
    {"keyword_id": 602, "update_time": TS, "uint_values": [192]},
]


@pytest.fixture(scope="module")
def known_id(test_environment):
    result = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{}".format(result): {
                "UserItems": [
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_OFFLINE_GENDER_WEIGHT,
                        "update_time": TS,
                        "weighted_uint_values": [
                            {"first": 0, "weight": 700000, "update_time": TS},
                            {"first": 1, "weight": 300000, "update_time": TS},
                        ],
                    },
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_OFFLINE_EXACT_GENDER,
                        "update_time": TS,
                        "uint_values": [0],
                    },
                ],
            },
        }
    )
    yield result


@pytest.fixture(scope="module")
def new_id(test_environment):
    result = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{}".format(result): {
                "UserItems": [
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_RT_GENDER_WEIGHT,
                        "update_time": TS,
                        "weighted_uint_values": [
                            {"first": 0, "weight": 200000, "update_time": TS},
                            {"first": 1, "weight": 800000, "update_time": TS},
                        ],
                    },
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_RT_EXACT_GENDER,
                        "update_time": TS,
                        "uint_values": [1],
                    },
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_DEVICE_MODEL_BT,
                        "update_time": TS,
                        "string_value": "iPhone12,1",
                    },
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_DEVICE_MODEL_BT,
                        "update_time": TS,
                        "string_value": "iPad",
                    },
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_DETAILED_DEVICE_TYPE_BT,
                        "update_time": TS,
                        "uint_values": [3],
                    },
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_USER_REGION,
                        "update_time": TS,
                        "uint_values": [213],
                    },
                ],
                "Queries": [
                    {"query_id": i, "query_text": query}
                    for i, query in enumerate(
                        [
                            "сумки karl lagerfeld карл лагерфельд официальных интернет",
                            "microsoft прекратит производство xbox one x",
                            "новости футбола",
                        ]
                    )
                ],
                "Counters": [
                    {
                        "counter_id": 200,
                        "key": [200000073, 200000079, 200000244, 200000246, 200000435],
                        "value": [
                            3.46322274208,
                            8.99720954895,
                            9.37608909607,
                            13.7542047501,
                            29.181854248,
                        ],
                    },
                    {
                        "counter_id": 201,
                        "key": [200000073, 200000079, 200000244, 200000246, 200000435],
                        "value": [TS] * 5,
                    },
                ],
            },
        }
    )
    yield result


def test_offline_socdem(test_environment, known_id):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": known_id},
        test_time=TS,
        exp_json={"Crypta": {"RtSocdem": {"UseRTMR": True}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_MAX_SOCDEM,
                    "update_time": TS,
                    "pair_values": [
                        {"first": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER, "second": 0},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER,
                    "update_time": TS,
                    "weighted_uint_values": [
                        {"first": 0, "update_time": TS, "weight": 700000},
                        {"first": 1, "update_time": TS, "weight": 300000},
                    ],
                    "source_uniq_index": 0,
                },
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "queries", "counters"],
    )


def test_rt_socdem(test_environment, new_id):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": new_id},
        test_time=TS,
        exp_json={"Crypta": {"RtSocdem": {"UseRTMR": False}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": INTERESTS_KW
            + [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_MAX_SOCDEM,
                    "update_time": TS,
                    "pair_values": [
                        {"first": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER, "second": 0},
                        {"first": keywords_data_pb2.EKeyword.KW_CRYPTA_USER_AGE_6S, "second": 2},
                        {"first": keywords_data_pb2.EKeyword.KW_CRYPTA_INCOME_5_SEGMENTS, "second": 3},
                        {"first": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_REVENUE, "second": 2},
                    ],
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER,
                    "update_time": TS,
                    "weighted_uint_values": [
                        {"first": 0, "update_time": TS, "weight": 847773},
                        {"first": 1, "update_time": TS, "weight": 152226},
                    ],
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_USER_AGE_6S,
                    "update_time": TS,
                    "weighted_uint_values": [
                        {"first": 0, "update_time": TS, "weight": 2465},
                        {"first": 1, "update_time": TS, "weight": 62587},
                        {"first": 2, "update_time": TS, "weight": 539611},
                        {"first": 3, "update_time": TS, "weight": 255369},
                        {"first": 4, "update_time": TS, "weight": 103765},
                        {"first": 5, "update_time": TS, "weight": 36200},
                    ],
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_INCOME_5_SEGMENTS,
                    "update_time": TS,
                    "weighted_uint_values": [
                        {"first": 0, "update_time": TS, "weight": 32833},
                        {"first": 1, "update_time": TS, "weight": 119157},
                        {"first": 2, "update_time": TS, "weight": 218252},
                        {"first": 3, "update_time": TS, "weight": 467112},
                        {"first": 4, "update_time": TS, "weight": 162644},
                    ],
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_REVENUE,
                    "update_time": TS,
                    "weighted_uint_values": [
                        {"first": 0, "update_time": TS, "weight": 32833},
                        {"first": 1, "update_time": TS, "weight": 337409},
                        {"first": 2, "update_time": TS, "weight": 629756},
                    ],
                },
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "queries", "counters"],
    )


def test_socdem_from_rtmr(test_environment, new_id):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": new_id},
        test_time=TS,
        exp_json={"Crypta": {"RtSocdem": {"UseRTMR": True}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": INTERESTS_KW
            + [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_CRYPTA_MAX_SOCDEM,
                    "update_time": TS,
                    "pair_values": [
                        {"first": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER, "second": 1},
                    ],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_KRYPTA_USER_GENDER,
                    "update_time": TS,
                    "weighted_uint_values": [
                        {"first": 0, "update_time": TS, "weight": 200000},
                        {"first": 1, "update_time": TS, "weight": 800000},
                    ],
                    "source_uniq_index": 0,
                },
                {"keyword_id": 564, "update_time": TS, "uint_values": [1]},
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "queries", "counters"],
    )
