from ads.bsyeti.tests.eagle.ft.lib.constants import TSAR_BOTH_SEARCH_RSYA_VECTORS
from ads.bsyeti.tests.eagle.ft.lib.test_environment import parse_profile

_QUERY_TEXT = "текст запроса"
_QUERY_REGION = 80


def test_do_not_skip_tsar_calculation_for_search(test_environment):
    user_id = test_environment.new_uid()
    test_environment.profiles.add({"y{user_id}".format(user_id=user_id): dict()})

    result = test_environment.apphost_request(
        client="search",
        test_time=1500000000,
        ids={"bigb-uid": user_id, "search-query": _QUERY_TEXT, "region": _QUERY_REGION},
    )

    data = parse_profile(result.answer).to_dict()
    result_vectors = [x for x in data["tsar_vectors"] if x["vector_id"] in TSAR_BOTH_SEARCH_RSYA_VECTORS]
    # This check fails, when bannedModelsMask is incorrect and skips models that are both for rsya and for search.
    assert len(result_vectors) > 0
