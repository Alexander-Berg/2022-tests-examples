# -*- coding: utf-8 -*-
import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer
from ads.bsyeti.libs.primitives.counter_proto import counter_ids_pb2
from yabs.server.proto.keywords import keywords_data_pb2

TS = 1500000000

KEYWORDS_FILTER = [
    keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT,
    keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_2,
    keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_GENERIC,
    keywords_data_pb2.EKeyword.KW_PRISMA_INCOME_GRADE,
]


def generate_profile(user_items):
    return {
        "UserItems": [
            {
                "keyword_id": keywords_data_pb2.EKeyword.KW_DEVICE_MODEL_BT,
                "update_time": TS,
                "string_value": "Galaxy A51",
            },
            {
                "keyword_id": keywords_data_pb2.EKeyword.KW_DEVICE_MODEL_BT,
                "update_time": TS,
                "string_value": "SM-A307FN",
            },
            {
                "keyword_id": keywords_data_pb2.EKeyword.KW_DETAILED_DEVICE_TYPE_BT,
                "update_time": TS,
                "uint_values": [2],
            },
            {
                "keyword_id": keywords_data_pb2.EKeyword.KW_USER_REGION,
                "update_time": TS,
                "uint_values": [2],
            },
        ]
        + user_items,
        "Queries": [
            {"query_id": i, "query_text": query}
            for i, query in enumerate(
                [
                    "попутные грузоперевозки по россии",
                    "терка чеснок и целлофан",
                    "виды бетоносмесителей",
                    "бетоносмеситель бетоносмеситель",
                    "саженцы клубники оптом по снг",
                    "игра аркада кухонная лихорадка",
                    "доставляем груз от 50 кг",
                    "цветочный магазин в ереване",
                    "маникюр стразами фото дизайн 2020 2021 видео",
                    "пищевая соль оптом от производителя",
                ]
            )
        ],
        "Counters": [
            {
                "counter_id": counter_ids_pb2.ECounterId.CI_QUERY_CATEGORIES_INTEREST,
                "key": [
                    200006283,
                    200004279,
                    200000645,
                    200027720,
                    200002554,
                    200001097,
                    200000379,
                    200051912,
                    200002899,
                ],
                "value": [
                    1,
                    2,
                    2.557514190673828,
                    1,
                    7.480255603790283,
                    1.9999966621398926,
                    2.557514190673828,
                    2.999915599822998,
                    1,
                ],
            },
            {
                "counter_id": counter_ids_pb2.ECounterId.CI_QUERY_CATEGORIES_INTEREST_LAST_TIME,
                "key": [
                    200006283,
                    200004279,
                    200000645,
                    200027720,
                    200002554,
                    200001097,
                    200000379,
                    200051912,
                    200002899,
                ],
                "value": [TS] * 9,
            },
        ],
    }


@pytest.fixture(scope="module")
def known_id(test_environment):
    result = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{}".format(result): generate_profile(
                [
                    {
                        "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT,
                        "update_time": TS,
                        "uint_values": [1000000, 97, 4],
                    },
                ]
            ),
        }
    )
    yield result


@pytest.fixture(scope="module")
def new_id(test_environment):
    result = test_environment.new_uid()
    test_environment.profiles.add({"y{}".format(result): generate_profile([])})
    yield result


def test_offline_prism(test_environment, known_id):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": known_id},
        test_time=TS,
        exp_json={"Crypta": {"Prism": {"TestRT": True}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT,
                    "update_time": TS,
                    "uint_values": [1000000, 97, 4],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_GENERIC,
                    "update_time": TS,
                    "uint_values": [1000000, 97, 4],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISMA_INCOME_GRADE,
                    "update_time": TS,
                    "uint_values": [1],
                    "source_uniq_index": 0,
                },
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "queries", "counters"],
    )


def test_rt_prism(test_environment, new_id):
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": new_id},
        test_time=TS,
        exp_json={"Crypta": {"Prism": {"TestRT": True}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_2,
                    "update_time": TS,
                    "uint_values": [1368532, 72, 3],
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_GENERIC,
                    "update_time": TS,
                    "uint_values": [1368532, 72, 3],
                },
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "queries", "counters"],
    )


def test_offline_and_rt_prism(test_environment, known_id):
    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": known_id},
        test_time=TS,
        exp_json={"Crypta": {"Prism": {"TestRT": True, "AlwaysComputeRT": True}}},
        keywords=KEYWORDS_FILTER,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT,
                    "update_time": TS,
                    "uint_values": [1000000, 97, 4],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_2,
                    "update_time": TS,
                    "uint_values": [1368532, 72, 3],
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISM_WEIGHT_GENERIC,
                    "update_time": TS,
                    "uint_values": [1000000, 97, 4],
                    "source_uniq_index": 0,
                },
                {
                    "keyword_id": keywords_data_pb2.EKeyword.KW_PRISMA_INCOME_GRADE,
                    "update_time": TS,
                    "uint_values": [1],
                    "source_uniq_index": 0,
                },
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs", "tsar_vectors", "queries", "counters"],
    )
