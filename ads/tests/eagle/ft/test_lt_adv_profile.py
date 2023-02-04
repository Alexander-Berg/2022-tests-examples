from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer
from ads.bsyeti.tests.eagle.ft.lib.constants import LT_ADV_FAIL


def test_lt_adv_profile(test_environment_with_kv_saas):
    uniqid = test_environment_with_kv_saas.new_uid()
    bigb_uid = uniqid
    profile_id = "y{uniqid}".format(uniqid=uniqid)

    with test_environment_with_kv_saas.kv_saas_server(
        data={profile_id: b"\x19\0\0\0\0\0\0\0(\xB5/\xFD \x19\xC9\0\0\n\7y123456\x12\x0E\n\5m\0\x10^_\x12\0055\n\x10^_"}
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1111],
        )

    check_answer(
        {
            "lt_adv_profiles": [
                {
                    "profile": {
                        "Uid": "y123456",
                        "RsyaClicks": [{"Click": {"Timestamp": 1600000000}, "Conversion": {"Timestamp": 1600000010}}],
                    },
                    "profile_id": profile_id,
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_no_response_from_kv_saas(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    fake_uniqid = test_environment_with_kv_saas.new_uid()
    fake_uniqid_key = "y{fake_uniqid}".format(fake_uniqid=fake_uniqid)

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            fake_uniqid_key: b"\x19\0\0\0\0\0\0\0(\xB5/\xFD \x19\xC9\0\0\n\7y123456\x12\x0E\n\5m\0\x10^_\x12\0055\n\x10^_"
        }
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1111],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": LT_ADV_FAIL,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_incorrect_response_from_kv_saas(test_environment_with_kv_saas):
    uniqid = test_environment_with_kv_saas.new_uid()
    bigb_uid = uniqid
    profile_id = "y{uniqid}".format(uniqid=uniqid)

    with test_environment_with_kv_saas.kv_saas_server(
        data={profile_id: b""},
        use_incorrect_response=True,
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"bigb-uid": bigb_uid},
            test_time=1500000000,
            keywords=[1111],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": LT_ADV_FAIL,
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )
