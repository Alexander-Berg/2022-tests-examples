from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer


def test_bans_because_segments(test_environment):
    keywords = [
        {"keyword_id": 547, "update_time": 1500000000, "uint_values": [1035, 1036, 1037, 1038], "source_uniq_index": 0},
    ]
    profile_id = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{profile_id}".format(profile_id=profile_id): {"UserItems": keywords},
        }
    )
    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": profile_id},
        test_time=1500000000,
        exp_json={
            "EagleSettings": {
                "BanCategoriesForProfessions": [],
            }
        },
        keywords=[323, 547],
    )
    check_answer({"items": keywords}, result.answer)

    for segment_id, bmcategory in [(1035, 100001), (1037, 100002), (1038, 100003), (1037, 100004)]:
        result = test_environment.request(
            client="yabs",
            ids={"bigb-uid": profile_id},
            test_time=1500000000,
            exp_json={
                "EagleSettings": {
                    "BanCategoriesForProfessions": [
                        {
                            "SegmentId": segment_id,
                            "BmCats": [bmcategory],
                        },
                    ],
                }
            },
            keywords=[323, 547],
        )
        bans = [
            {
                "keyword_id": 323,
                "update_time": 1500000000,
                "trio_values": [{"first": 0, "second": bmcategory, "third": 0}],
            },
        ]
        united_keywords = keywords + bans
        check_answer({"items": united_keywords}, result.answer)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": profile_id},
        test_time=1500000000,
        exp_json={
            "EagleSettings": {
                "BanCategoriesForProfessions": [
                    {
                        "SegmentId": 1037,
                        "BmCats": [101341],
                    },
                    {
                        "SegmentId": 1035,
                        "KeywordId": 547,
                        "BmCats": [101342],
                    },
                ],
            }
        },
        keywords=[323, 547],
    )
    bans = [
        {"keyword_id": 323, "update_time": 1500000000, "trio_values": [{"first": 0, "second": i, "third": 0}]}
        for i in range(101341, 101343)
    ]
    united_keywords = keywords + bans
    check_answer({"items": united_keywords}, result.answer)

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": profile_id},
        test_time=1500000000,
        exp_json={
            "EagleSettings": {
                "BanCategoriesForProfessions": [
                    {
                        "SegmentId": 1036,
                        "BmCats": [101337],
                    },
                    {
                        "SegmentId": 1038,
                        "KeywordId": 547,
                        "BmCats": [101338],
                    },
                    {
                        "SegmentId": 1037,
                        "KeywordId": 547,
                        "BmCats": [101339],
                    },
                    {
                        "SegmentId": 1035,
                        "KeywordId": 547,
                        "BmCats": [101340],
                    },
                ],
            }
        },
        keywords=[323, 547],
    )
    bans = [
        {"keyword_id": 323, "update_time": 1500000000, "trio_values": [{"first": 0, "second": i, "third": 0}]}
        for i in range(101337, 101341)
    ]
    united_keywords = keywords + bans
    check_answer({"items": united_keywords}, result.answer)
