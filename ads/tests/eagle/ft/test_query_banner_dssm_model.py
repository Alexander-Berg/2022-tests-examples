# -*- coding: utf-8 -*-

import time
from itertools import groupby

from ads.bsyeti.tests.eagle.ft.lib.constants import TSAR_SEARCH_VECTORS, TSAR_BOTH_SEARCH_RSYA_VECTORS
from ads.bsyeti.tests.eagle.ft.lib.test_environment import parse_profile

_QUERY_TEXT = "текст запроса"
_QUERY_REGION = 80


def _validate_response_and_get_vectors(response, search_query, region):
    data = parse_profile(response).to_dict()
    assert "current_search_query" in data
    assert search_query == data["current_search_query"].get("query_text", "")
    assert region == data["current_search_query"].get("region")
    assert "tsar_vectors" in data
    return sorted(
        [x for x in data["tsar_vectors"] if x["vector_id"] in TSAR_SEARCH_VECTORS],
        key=lambda x: x["vector_id"],
    )


def _validate_query_banner_vectors(tsar_vectors):
    expected_vector_ids = set(TSAR_SEARCH_VECTORS)
    for vector_id, grp in groupby(tsar_vectors, key=lambda x: x["vector_id"]):
        grp = list(grp)
        assert len(grp) == 1, "Expecting a single vector with tsar version id: {}".format(vector_id)
        result = grp[0]
        assert result["vector_size"] == TSAR_SEARCH_VECTORS[vector_id], "Invalid vector length"
        assert result["element_size"] > 0, "Invalid element size"
        expected_size = result["element_size"] * result["vector_size"]
        assert expected_size == len(result["value"]), "Invalid vector data size"
        expected_vector_ids.remove(vector_id)
    assert len(expected_vector_ids) == 0, "Expecting vectors with tsar version id: {}".format(
        ",".join(map(str, expected_vector_ids))
    )


def _count_both_rsya_and_search_vectors(tsar_vectors):
    return len([x for x in tsar_vectors if x["vector_id"] in TSAR_BOTH_SEARCH_RSYA_VECTORS])


def test_query_banner_dssm_model(test_environment):
    now = int(time.time())
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id1}".format(id1=id1): {
                "UserItems": [{"keyword_id": 235, "update_time": now - 10, "uint_values": [15]}]
            },
        }
    )

    empty_result = test_environment.apphost_request(client="debug", ids={"bigb-uid": id1}, test_time=now)
    vectors = _validate_response_and_get_vectors(empty_result.answer, "", 0)
    assert len(vectors) == 2 and _count_both_rsya_and_search_vectors(vectors) == len(
        vectors
    ), "Only models, that do not require search query should be evaluated."

    result = test_environment.apphost_request(
        client="debug",
        test_time=now,
        ids={"bigb-uid": id1, "search-query": _QUERY_TEXT, "region": _QUERY_REGION},
    )
    vectors = _validate_response_and_get_vectors(result.answer, _QUERY_TEXT, _QUERY_REGION)
    _validate_query_banner_vectors(vectors)
    values = dict((v["vector_id"], v["value"]) for v in vectors)

    modified_query_result = test_environment.apphost_request(
        client="debug",
        test_time=now,
        ids={"bigb-uid": id1, "search-query": _QUERY_TEXT + "bla", "region": _QUERY_REGION},
    )
    vectors = _validate_response_and_get_vectors(modified_query_result.answer, _QUERY_TEXT + "bla", _QUERY_REGION)
    _validate_query_banner_vectors(vectors)
    modified_query_values = dict((v["vector_id"], v["value"]) for v in vectors)
    assert values != modified_query_values, "Model returns equal vectors for different queries"

    modified_region_result = test_environment.apphost_request(
        client="debug",
        test_time=now,
        ids={"bigb-uid": id1, "search-query": _QUERY_TEXT, "region": _QUERY_REGION + 1},
    )
    vectors = _validate_response_and_get_vectors(modified_region_result.answer, _QUERY_TEXT, _QUERY_REGION + 1)
    _validate_query_banner_vectors(vectors)
    modified_region_values = dict((v["vector_id"], v["value"]) for v in vectors)
    assert values != modified_region_values, "Model returns equal vectors for different regions"

    # normalize search query
    normalized_search_query_result = test_environment.apphost_request(
        client="debug",
        test_time=now,
        ids={
            "bigb-uid": id1,
            "search-query": " экологический проект «Утилизация исторического наследия» ",
            "region": _QUERY_REGION,
        },
    )
    vectors = _validate_response_and_get_vectors(
        normalized_search_query_result.answer,
        "экологический проект утилизация исторического наследия",
        _QUERY_REGION,
    )
    _validate_query_banner_vectors(vectors)

    # search vectors via HTTP interface
    http_search_query_result = test_environment.request(
        client="debug",
        test_time=now,
        ids={
            "bigb-uid": id1,
            "search-query": "купить щётки для электродвигателя",
            "region": _QUERY_REGION,
        },
    )
    vectors = _validate_response_and_get_vectors(
        http_search_query_result.answer, "купить щетки для электродвигателя", _QUERY_REGION
    )
    _validate_query_banner_vectors(vectors)


def test_bad_search_query(test_environment):
    now = int(time.time())
    uid = test_environment.new_uid()
    test_environment.profiles.add(
        {
            f"y{uid}": {"UserItems": [{"keyword_id": 235, "update_time": now - 10, "uint_values": [15]}]},
        }
    )
    http_search_query_result = test_environment.request(
        client="debug",
        test_time=now,
        ids={
            "bigb-uid": uid,
            "search-query": b"\xff\xfe?\x04@\x048\x042\x045",
            "region": _QUERY_REGION,
        },
    )
    vectors = _validate_response_and_get_vectors(http_search_query_result.answer, "", _QUERY_REGION)
    _validate_query_banner_vectors(vectors)
