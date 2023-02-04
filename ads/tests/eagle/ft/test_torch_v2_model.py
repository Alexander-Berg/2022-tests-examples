# -*- coding: utf-8 -*-
from ads.bsyeti.tests.eagle.ft.lib.constants import TORCH_V2_VECTOR
from ads.bsyeti.tests.eagle.ft.lib.test_environment import parse_profile


def test_query_banner_dssm_model(test_environment):
    user_id = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{user_id}".format(user_id=user_id): dict(),
        }
    )
    response = test_environment.request(client="debug", ids={"bigb-uid": user_id}, test_time=1500000000)

    data = parse_profile(response.answer).to_dict()
    result_vectors = [x for x in data["tsar_vectors"] if x["vector_id"] in TORCH_V2_VECTOR]

    assert len(result_vectors) == len(TORCH_V2_VECTOR)
    result = result_vectors[0]

    assert result["vector_size"] == TORCH_V2_VECTOR[result["vector_id"]], "Invalid vector length"

    assert result["element_size"] > 0, "Invalid element size"

    expected_size = result["element_size"] * result["vector_size"]
    assert expected_size == len(result["value"]), "Invalid vector data size"
