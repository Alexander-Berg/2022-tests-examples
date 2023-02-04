import base64

from dj.lib.proto import profile_pb2

from ads.bsyeti.tests.eagle.ft.lib.test_environment import check_answer


def get_based64_response_for_test():
    # !python3
    # offer_ids = [14, 11, 19]
    # models = b''.join(i.to_bytes(8, byteorder='little') for i in offer_ids)
    models = b"\024\000\000\000\000\000\000\000\x0b\x00\x00\x00\x00\x00\x00\x00\x13\x00\x00\x00\x00\x00\x00\x00"

    dj_profile = profile_pb2.TProfileProto()
    dj_profile.ObjectId = "p2"
    dj_profile.IsYandex = False

    erf = dj_profile.Erfs.add()
    erf.ErfNamespace = 1
    erf.ErfType = 670
    erf.String = b"\x01\x00\x00\x00\x00\x00\x00\x00"  # 1.to_bytes(8, byteorder='little')

    erf = dj_profile.Erfs.add()
    erf.ErfNamespace = 21
    erf.ErfType = 670
    erf.String = models

    serialized = dj_profile.SerializeToString()
    response = base64.b64encode(serialized)
    return models, response


def test_dj(test_environment_with_market_dj):
    bigb_uid = test_environment_with_market_dj.new_uid()
    puid = test_environment_with_market_dj.new_uid()

    binstr, resp = get_based64_response_for_test()

    test_environment_with_market_dj.profiles.add(
        {
            "y{id}".format(id=bigb_uid): {
                "Offers": [
                    {
                        "counter_id": 42,
                        "offer_id_md5": 225224,
                        "action_bits": 1,
                        "update_time": 1500000000,
                        "select_type": 27,
                    },
                    {
                        "counter_id": 228,
                        "offer_id_md5": 225,
                        "action_bits": 16,
                        "update_time": 1500000000,
                        "select_type": 27,
                    },
                ]
            },
        }
    )

    with test_environment_with_market_dj.market_dj_server(
        request=(
            "get_profile"
            "?profile_key=%7B%22ObjectId%22%3A%22p{puid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
            "&profile_key=%7B%22ObjectId%22%3A%22y{bigb_uid}%22%2C%22ObjectType%22%3A2%2C%22ObjectNamespace%22%3A1%7D"
        ).format(puid=puid, bigb_uid=bigb_uid),
        response=(resp + b"\nEgJ5MRgAIgIqADABOAJKAA,,"),
    ):
        result = test_environment_with_market_dj.request(
            client="debug",
            ids={"puid": puid, "bigb-uid": bigb_uid},
            exp_json={
                "EagleSettings": {"PrintErfsAsOffers": True},
                "OfferExpireSettings": [
                    {
                        "ExpireDays": 540,
                        "DesiredMinimalCountPerCounterId": 50,
                        "DesiredMinimalCountQuota": 300,
                        "SelectType": 164,
                        "MaxCount": 300,
                    }
                ],
            },
            test_time=1500000000,
            keywords=[377, 1092],
        )
    check_answer(
        {
            "offers": [
                {
                    "counter_id": 42,
                    "offer_id_md5": 225224,
                    "action_bits": 1,
                    "update_time": 1500000000,
                    "source_uniq_index": 0,
                },
                {
                    "counter_id": 228,
                    "offer_id_md5": 225,
                    "action_bits": 16,
                    "update_time": 1500000000,
                    "select_type": 27,
                    "source_uniq_index": 0,
                },
                # modelId = 14
                {
                    "counter_id": 44910898,
                    "offer_id_md5": 3561132623765523694,
                    "action_bits": 16,
                    "update_time": 1500000000,
                    "select_type": 164,
                },
                # modelId = 11
                {
                    "counter_id": 44910898,
                    "offer_id_md5": 6606652192114587209,
                    "action_bits": 16,
                    "update_time": 1500000000,
                    "select_type": 164,
                },
                # modelId = 19
                {
                    "counter_id": 44910898,
                    "offer_id_md5": 7146505081096872482,
                    "action_bits": 16,
                    "update_time": 1500000000,
                    "select_type": 164,
                },
            ],
            "dj_profiles": [
                {
                    "keyword_id": 1092,
                    "profile": {
                        "ObjectId": "p2",
                        "IsYandex": False,
                        "Erfs": [
                            {
                                "ErfNamespace": 1,
                                "ErfType": 670,
                                "String": b"\x01\x00\x00\x00\x00\x00\x00\x00",
                            },
                            {
                                "ErfNamespace": 21,
                                "ErfType": 670,
                                "String": binstr.decode("utf-8"),
                            },
                        ],
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
        },
        result.answer,
    )
