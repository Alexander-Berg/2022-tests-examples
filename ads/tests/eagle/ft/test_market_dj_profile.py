from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer
from ads.bsyeti.tests.eagle.ft.lib.constants import MARKET_DJ_FAIL


# dj should return one line for each profile_key
# each line - base64(serialized_proto)


def test_good_protos_from_market_dj(test_environment_with_market_dj):
    bigb_uid = test_environment_with_market_dj.new_uid()
    puid = test_environment_with_market_dj.new_uid()

    # real answer
    with test_environment_with_market_dj.market_dj_server(
        request=(
            "get_profile"
            "?profile_key=%7B%22ObjectId%22%3A%22p{puid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
            "&profile_key=%7B%22ObjectId%22%3A%22y{bigb_uid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
        ).format(puid=puid, bigb_uid=bigb_uid),
        response=(
            b"EgJwMhgAItoBJQ6u7mAq0gGTDxUBFfgBw9_asgYYARLg7723BgMSrZuzpQcIEs7B1cwBFRLil9L4AQcSrLjgqAISEt"
            b"GRkrECDxKTz_-7AhMSirSRvAIWEuy9r70CDBKl-8S9AgsS4_f9wAICEs6KycECBBLh4u7BAhQSjra31gIOEobg9tkC"
            b"DRLRnIPeAgoS-Pjm3gIaEqmkuN8CERLr64roAh0S-Lqc8AIXEu3joJEDHBKbhPShAxsSoaiHpQMGEqm6qqgDEBKYn5"
            b"q1AwES9-eKuQMJErnAlbwDGRKx2sS9AwUwATgCSgA,\nEgJ5MRgAIgIqADABOAJKAA,,"
        ),
    ):
        result = test_environment_with_market_dj.request(
            client="debug",
            ids={"puid": puid, "bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1092],
        )

    check_answer(
        {
            "dj_profiles": [
                {
                    "keyword_id": 1092,
                    "profile": {
                        "ObjectId": "p2",
                        "IsYandex": False,
                        "Counters": {
                            "MinLastUpdateTime": 1626254862,
                            "CompressedCounters": (
                                b"\223\017\025\001\025\370\001\303\337\332\262\006\030\001\022\340"
                                b"\357\275\267\006\003\022\255\233\263\245\007\010\022\316\301\325"
                                b"\314\001\025\022\342\227\322\370\001\007\022\254\270\340\250\002"
                                b"\022\022\321\221\222\261\002\017\022\223\317\377\273\002\023\022"
                                b"\212\264\221\274\002\026\022\354\275\257\275\002\014\022\245\373"
                                b"\304\275\002\013\022\343\367\375\300\002\002\022\316\212\311\301"
                                b"\002\004\022\341\342\356\301\002\024\022\216\266\267\326\002\016"
                                b"\022\206\340\366\331\002\r\022\321\234\203\336\002\n\022\370\370"
                                b"\346\336\002\032\022\251\244\270\337\002\021\022\353\353\212\350"
                                b"\002\035\022\370\272\234\360\002\027\022\355\343\240\221\003\034"
                                b"\022\233\204\364\241\003\033\022\241\250\207\245\003\006\022\251"
                                b"\272\252\250\003\020\022\230\237\232\265\003\001\022\367\347\212"
                                b"\271\003\t\022\271\300\225\274\003\031\022\261\332\304\275\003\005"
                            ),
                        },
                        "ObjectNamespace": 1,
                        "ObjectType": 2,
                        "ArchiveData": {},
                    },
                },
                {
                    "keyword_id": 1092,
                    "profile": {
                        "ObjectId": "y1",
                        "IsYandex": False,
                        "Counters": {"CompressedCounters": ""},
                        "ObjectNamespace": 1,
                        "ObjectType": 2,
                        "ArchiveData": {},
                    },
                },
            ],
            "is_full": True,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_empty_protos_from_market_dj(test_environment_with_market_dj):
    bigb_uid = test_environment_with_market_dj.new_uid()
    puid = test_environment_with_market_dj.new_uid()

    # empty protos
    with test_environment_with_market_dj.market_dj_server(
        request=(
            "get_profile"
            "?profile_key=%7B%22ObjectId%22%3A%22p{puid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
            "&profile_key=%7B%22ObjectId%22%3A%22y{bigb_uid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
        ).format(puid=puid, bigb_uid=bigb_uid),
        response=b"\n",
    ):
        result = test_environment_with_market_dj.request(
            client="debug",
            ids={"puid": puid, "bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1092],
        )

    check_answer(
        {"dj_profiles": [{"keyword_id": 1092, "profile": {}}, {"keyword_id": 1092, "profile": {}}], "is_full": True},
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_unparsable_proto_from_market_dj(test_environment_with_market_dj):
    bigb_uid = test_environment_with_market_dj.new_uid()
    puid = test_environment_with_market_dj.new_uid()

    # unparsable proto
    # we do not retry in that case
    with test_environment_with_market_dj.market_dj_server(
        request=(
            "get_profile"
            "?profile_key=%7B%22ObjectId%22%3A%22p{puid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
            "&profile_key=%7B%22ObjectId%22%3A%22y{bigb_uid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
        ).format(puid=puid, bigb_uid=bigb_uid),
        response=b"UnparsableProto\nUnparsableProto",
    ):
        result = test_environment_with_market_dj.request(
            client="debug",
            ids={"puid": puid, "bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1092],
        )

    check_answer(
        {"dj_profiles": [{"keyword_id": 1092, "profile": {}}, {"keyword_id": 1092, "profile": {}}], "is_full": True},
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_incorrect_response_from_market_dj(test_environment_with_market_dj):
    bigb_uid = test_environment_with_market_dj.new_uid()
    puid = test_environment_with_market_dj.new_uid()

    # requested uids count is not equal to lines in answer
    # we check that immediately after receiving answer
    with test_environment_with_market_dj.market_dj_server(
        request=(
            "get_profile"
            "?profile_key=%7B%22ObjectId%22%3A%22p{puid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
            "&profile_key=%7B%22ObjectId%22%3A%22y{bigb_uid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
        ).format(puid=puid, bigb_uid=bigb_uid),
        response=b"UnparsableProto",
    ):
        result = test_environment_with_market_dj.request(
            client="debug",
            ids={"puid": puid, "bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1092],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": MARKET_DJ_FAIL,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_bad_code_from_market_dj(test_environment_with_market_dj):
    bigb_uid = test_environment_with_market_dj.new_uid()

    with test_environment_with_market_dj.market_dj_server(request="random_request", response=b"SomeIncorrectResponse"):
        result = test_environment_with_market_dj.request(
            client="debug",
            ids={"bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1092],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": MARKET_DJ_FAIL,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )
