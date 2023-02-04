from ads.bsyeti.tests.eagle.ft.lib.constants import TSAR_SEARCH_VECTORS
from ads.bsyeti.tests.eagle.ft.lib.test_environment import parse_profile

_QUERY_TEXT = "текст запроса"
_QUERY_REGION = 80


def test_skip_tsar_models_calculation(test_environment):
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

    result = test_environment.apphost_request(
        client="debug",
        test_time=1500000000,
        ids={"bigb-uid": user_id, "search-query": _QUERY_TEXT, "region": _QUERY_REGION},
        skip_tsar_calculation=True,
    )

    data = parse_profile(result.answer).to_dict()
    result_vectors = [x for x in data["tsar_vectors"] if x["vector_id"] in TSAR_SEARCH_VECTORS]

    assert len(result_vectors) == 0
