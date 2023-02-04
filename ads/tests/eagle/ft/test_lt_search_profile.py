from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer
from ads.bsyeti.tests.eagle.ft.lib.constants import LT_SEARCH_FAIL


def test_lt_search_profile(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    icookie = test_environment_with_kv_saas.new_uid()
    icookie_id = "y{icookie}".format(icookie=icookie)

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            icookie_id: b'(\0\0\0\0\0\0\0(\xB5/\xFD (A\1\0*&\n\x0E\x08\1"\nabracdabra\x12\x14\n\x12"\x10http://yandex.ru'
        }
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid, "icookie": icookie},
            test_time=1500000000,
            keywords=[1078],
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
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_no_response_from_kv_saas(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    icookie = test_environment_with_kv_saas.new_uid()
    fake_icookie = test_environment_with_kv_saas.new_uid()
    fake_icookie_key = "y{fake_icookie}".format(fake_icookie=fake_icookie)

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            fake_icookie_key: b'(\0\0\0\0\0\0\0(\xB5/\xFD (A\1\0*&\n\x0E\x08\1"\nabracdabra\x12\x14\n\x12"\x10http://yandex.ru'
        }
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid, "icookie": icookie},
            test_time=1500000000,
            keywords=[1078],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": LT_SEARCH_FAIL,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_incorrect_response_from_kv_saas(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    icookie = test_environment_with_kv_saas.new_uid()
    icookie_key = "y{icookie}".format(icookie=icookie)

    with test_environment_with_kv_saas.kv_saas_server(
        data={icookie_key: b""},
        use_incorrect_response=True,
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid, "icookie": icookie},
            test_time=1500000000,
            keywords=[1078],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": LT_SEARCH_FAIL,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )
