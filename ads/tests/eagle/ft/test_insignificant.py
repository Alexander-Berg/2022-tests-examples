import pytest

from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType


def test_insignificant_request_default(test_environment):
    idfa = "00000000-0000-0000-0000-000000000000"
    test_environment.profiles.add(
        {"idfa/{}".format(idfa): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]}}
    )

    with pytest.raises(AssertionError):
        test_environment.request(
            client="yabs",
            ids={"idfa": idfa},
            test_time=1500000000,
            glue_type=GlueType.NO_GLUE,
            keywords=[235, 328, 564, 725],
        )
