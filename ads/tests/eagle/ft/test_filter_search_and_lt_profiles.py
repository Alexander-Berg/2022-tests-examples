from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer


def test_filter_search_and_lt_profiles(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    icookie = test_environment_with_kv_saas.new_uid()
    icookie_id = "y{icookie}".format(icookie=icookie)

    test_environment_with_kv_saas.search_profiles.add(
        {
            icookie_id: {
                "UserHistoryState": {
                    "LastClick": {"Started": True, "Title": "qwertyytrewq"},
                    "UserHistory": {"Records": [{"Url": "http://xednay.ru"}]},
                }
            }
        }
    )

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            icookie_id: b'(\0\0\0\0\0\0\0(\xB5/\xFD (A\1\0*&\n\x0E\x08\1"\nabracdabra\x12\x14\n\x12"\x10http://yandex.ru'
        }
    ):
        result = test_environment_with_kv_saas.request(
            client="yabs",
            ids={"bigb-uid": bigb_uid, "icookie": icookie},
            test_time=1500000000,
            keywords=[950, 1078],
        )

        # By default SearchPers and LongTerm profiles are filtered.
        check_answer(
            {
                "is_full": True,
            },
            result.answer,
            ignored_fields=["source_uniqs"],
        )

        exp_json = {"EagleSettings": {"FilterSearchAndLongTermProfilesForRsya": False}}

        result = test_environment_with_kv_saas.request(
            client="yabs",
            exp_json=exp_json,
            ids={"bigb-uid": bigb_uid, "icookie": icookie},
            test_time=1500000000,
            keywords=[950, 1078],
        )

        check_answer(
            {
                "lt_search_profiles": [
                    {
                        "profile": {
                            "UserHistoryState": {
                                "LastClick": {"Started": True, "Title": "abracdabra"},
                                "UserHistory": {"Records": [{"Url": "http://yandex.ru"}]},
                            }
                        },
                        "profile_id": icookie_id,
                    }
                ],
                "search_pers_profiles": [
                    {
                        "profile": {
                            "UserHistoryState": {
                                "LastClick": {"Started": True, "Title": "qwertyytrewq"},
                                "UserHistory": {"Records": [{"Url": "http://xednay.ru"}]},
                            }
                        },
                        "profile_id": icookie_id,
                    }
                ],
            },
            result.answer,
            ignored_fields=["source_uniqs"],
        )
