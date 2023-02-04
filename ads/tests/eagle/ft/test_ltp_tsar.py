import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import parse_profile
from ads.bsyeti.libs.py_testlib.compat import break_bytes


def _get_tsar_vector(response, vector_id):
    data = parse_profile(response).to_dict()
    return [x for x in data["tsar_vectors"] if x["vector_id"] == vector_id]


def _has_source_uniq(response, user_id):
    data = parse_profile(response).to_dict()
    for source in data["source_uniqs"]:
        if source["user_id"] == str(user_id):
            return True
    return False


@pytest.mark.parametrize("cid_time_shift", [-10, 0, 10])
def test_ltp_tsar_cid(test_environment, cid_time_shift):
    yuid = test_environment.new_uid()
    cid = test_environment.new_uid()

    test_environment.profiles.add(
        {
            "y{yuid}".format(yuid=yuid): {
                "TsarVectors": [{"vector_id": 7, "value": "just some vector", "update_time": 1500000000}]
            },
            "y{cid}".format(cid=cid): {
                "TsarVectors": [
                    {
                        "vector_id": 7,
                        "value": "another vector",
                        "update_time": 1500000000 + cid_time_shift,
                    }
                ]
            },
        }
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": yuid, "crypta-id": cid},
        test_time=1500000100,
        keywords=[873],
    )
    assert _get_tsar_vector(result.answer, 7)[0]["value"] == break_bytes(b"just some vector")

    result = test_environment.request(client="debug", ids={"bigb-uid": yuid}, test_time=1500000100, keywords=[873])
    assert _get_tsar_vector(result.answer, 7)[0]["value"] == break_bytes(b"just some vector")

    result = test_environment.request(client="debug", ids={"bigb-uid": cid}, test_time=1500000100, keywords=[873])
    assert _get_tsar_vector(result.answer, 7)[0]["value"] == break_bytes(b"another vector")


@pytest.mark.parametrize("secondary_time_shift", [-10, 0, 10])
def test_ltp_tsar_glue(test_environment, secondary_time_shift):
    main = test_environment.new_uid()
    secondary = test_environment.new_uid()

    test_environment.profiles.add(
        {
            "y{main}".format(main=main): {
                "TsarVectors": [{"vector_id": 7, "value": "just some vector", "update_time": 1500000000}]
            },
            "y{secondary}".format(secondary=secondary): {
                "TsarVectors": [
                    {
                        "vector_id": 7,
                        "value": "another vector",
                        "update_time": 1500000000 + secondary_time_shift,
                    }
                ]
            },
        }
    )

    test_environment.vulture.add(
        {
            "y{}".format(main): {
                "KeyRecord": {"user_id": str(main), "id_type": 1, "is_main": True},
                "ValueRecords": [{"user_id": str(secondary), "id_type": 1, "crypta_graph_distance": 2}],
            }
        }
    )

    result = test_environment.request(client="debug", ids={"bigb-uid": main}, test_time=1500000100, keywords=[725, 873])
    assert _get_tsar_vector(result.answer, 7)[0]["value"] == break_bytes(b"just some vector")
    assert _has_source_uniq(result.answer, main)
    # assert _has_source_uniq(result.answer, secondary)

    result = test_environment.request(
        client="debug", ids={"bigb-uid": secondary}, test_time=1500000100, keywords=[725, 873]
    )
    assert _get_tsar_vector(result.answer, 7)[0]["value"] == break_bytes(b"another vector")
    # assert _has_source_uniq(result.answer, secondary)
