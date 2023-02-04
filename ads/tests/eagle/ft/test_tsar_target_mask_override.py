from ads.bsyeti.tests.eagle.ft.lib.constants import TORCH_V2_VECTOR, TSAR_SEARCH_VECTORS, TSAR_BOTH_SEARCH_RSYA_VECTORS
from ads.bsyeti.tests.eagle.ft.lib.test_environment import parse_profile

_QUERY_TEXT = "текст запроса"
_QUERY_REGION = 80


def test_search_tsar_override(test_environment):
    user_id = test_environment.new_uid()
    test_environment.profiles.add({"y{user_id}".format(user_id=user_id): dict()})

    result = test_environment.apphost_request(
        client="debug",
        test_time=1500000000,
        ids={"bigb-uid": user_id, "search-query": _QUERY_TEXT, "region": _QUERY_REGION},
    )

    data = parse_profile(result.answer).to_dict()
    result_vectors = [x for x in data["tsar_vectors"] if x["vector_id"] in TSAR_SEARCH_VECTORS]

    assert len(result_vectors) > 0

    exp_json = {
        "EagleSettings": {
            "OverrideClientTsarTarget": {
                "Values": [
                    {
                        "Client": "debug",
                        "TargetMask": 1,
                    }
                ]
            }
        }
    }

    result = test_environment.apphost_request(
        client="debug",
        test_time=1500000000,
        ids={"bigb-uid": user_id, "search-query": _QUERY_TEXT, "region": _QUERY_REGION},
        exp_json=exp_json,
    )

    data = parse_profile(result.answer).to_dict()
    result_vectors = [
        x
        for x in data["tsar_vectors"]
        if x["vector_id"] in TSAR_SEARCH_VECTORS and x["vector_id"] not in TSAR_BOTH_SEARCH_RSYA_VECTORS
    ]

    assert len(result_vectors) == 0


def test_rsya_tsar_override(test_environment):
    user_id = test_environment.new_uid()
    test_environment.profiles.add({"y{user_id}".format(user_id=user_id): dict()})

    result = test_environment.request(client="debug", test_time=1500000000, ids={"bigb-uid": user_id})

    data = parse_profile(result.answer).to_dict()
    result_vectors = [x for x in data["tsar_vectors"] if x["vector_id"] in TORCH_V2_VECTOR]

    assert len(result_vectors) > 0

    exp_json = {
        "EagleSettings": {
            "OverrideClientTsarTarget": {
                "Values": [
                    {
                        "Client": "debug",
                        "TargetMask": 2,
                    }
                ]
            }
        }
    }

    result = test_environment.request(
        client="debug", test_time=1500000000, ids={"bigb-uid": user_id}, exp_json=exp_json
    )

    data = parse_profile(result.answer).to_dict()
    result_vectors = [x for x in data["tsar_vectors"] if x["vector_id"] in TORCH_V2_VECTOR]

    assert len(result_vectors) == 0
