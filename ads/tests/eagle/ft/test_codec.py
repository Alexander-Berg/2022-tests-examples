import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer, decompress_answer


@pytest.fixture(scope="module")
def profiles(test_environment):
    id = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{id}".format(id=id): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [15]}]},
        }
    )
    answer = {
        "items": [
            {
                "keyword_id": 235,
                "update_time": 1500000000,
                "uint_values": [15],
                "source_uniq_index": 0,
                "main": True,
            }
        ]
    }
    return id, answer


@pytest.mark.parametrize(
    "client,codec,compression_type,decompress_type",
    [
        ("yabs", None, None, None),
        # default for yabs is "none"
        ("yabs", "lz4", None, "none"),
        ("yabs", "lz4", "none", "none"),
        ("yabs", "lz4", "stream", "stream"),
        # default for adfox is "stream"
        ("adfox", "lz4", None, "stream"),
        ("adfox", "lz4", "none", "none"),
    ],
)
def test_compression(test_environment, profiles, client, codec, compression_type, decompress_type):
    result = test_environment.request(
        client=client,
        ids={"bigb-uid": profiles[0]},
        keywords=[235],
        test_time=1500000000,
        compression=codec,
        compression_type=compression_type,
    )
    check_answer(
        profiles[1],
        decompress_answer(result.answer, codec, decompress_type),
        ignored_fields=["source_uniqs", "tsar_vectors"],
    )
