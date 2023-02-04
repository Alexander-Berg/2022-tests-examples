from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer


def test_search_pers(test_environment):
    bigb_uid = test_environment.new_uid()
    icookie = test_environment.new_uid()

    test_environment.search_profiles.add(
        {
            "y{icookie}".format(icookie=icookie): {
                "UserHistoryState": {
                    "LastClick": {"Started": True, "Title": "abracdabra"},
                    "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                }
            }
        }
    )
    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": bigb_uid, "icookie": icookie},
        test_time=1500000000,
        keywords=[950],
    )
    check_answer(
        {
            "search_pers_profiles": [
                {
                    "profile": {
                        "UserHistoryState": {
                            "LastClick": {"Started": True, "Title": "abracdabra"},
                            "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                        }
                    },
                    "profile_id": "y{icookie}".format(icookie=icookie),
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )
