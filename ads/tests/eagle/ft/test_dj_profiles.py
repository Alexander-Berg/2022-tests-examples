from ads.bsyeti.tests.eagle.ft.lib.constants import MARKET_KVSAAS_FAIL
from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


KVSAAS_PREFIX = "1#2/"
ENCODED_KVSAAS_PREFIX = "1%232/"
KVSAAS_KEY_FORMAT = "{prefix}{id}"


def test_market_dj_kvsaas_with_profile(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    puid = "p{id}".format(id=bigb_uid)

    test_environment_with_kv_saas.profiles.add(
        {
            "p{id}".format(id=bigb_uid): {
                "DjProfiles": [
                    {
                        "keyword_id": 1200,
                        "profile": {
                            "ObjectId": "p{id}".format(id=bigb_uid),
                            "IsYandex": True,
                            "LastUpdateTime": 1500000000,  # without LastUpdateTime profile will be deleted in CleanUp
                        },
                    },
                ]
            }
        }
    )

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            KVSAAS_KEY_FORMAT.format(prefix=ENCODED_KVSAAS_PREFIX, id=puid): (
                b'%\x00\x00\x00\x00\x00\x00\x00(\xb5/\xfd %)\x01\x00\x12\x00\x18\x00"\x02*\x000\x008\x00J\x00R\x0f\x08\x02\x10\x01\x18\xbc\xd3\x8c\x95\x06%\x00\x00\x00@`\xbc\xd3\x8c\x95\x06'
            )
        },
        keys={
            KVSAAS_KEY_FORMAT.format(prefix=ENCODED_KVSAAS_PREFIX, id=puid): KVSAAS_KEY_FORMAT.format(prefix=KVSAAS_PREFIX, id=puid)
        },
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"puid": bigb_uid},
            test_time=1500000000,
            keywords=[1197, 1200, 1212],
        )

    check_answer(
        {
            "dj_profiles": [
                {
                    "keyword_id": 1200,
                    "profile": {
                        "ObjectId": puid,
                        "ObjectNamespace": 1,
                        "ObjectType": 2,
                        "IsYandex": True,
                        "Counters": {
                            "CompressedCounters": ""
                        },
                        "ArchiveData": {},
                        "Erfs": [
                            {
                                "ErfNamespace": 2,
                                "ErfType": 1,
                                "LastUpdateTime": 1654860220,
                                "Float": 2
                            },
                        ],
                        "LastUpdateTime": 1654860220
                    }
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_market_dj_kvsaas_without_profile(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    puid = "p{id}".format(id=bigb_uid)

    with test_environment_with_kv_saas.kv_saas_server(
        data={
            KVSAAS_KEY_FORMAT.format(prefix=ENCODED_KVSAAS_PREFIX, id=puid): (
                b'%\x00\x00\x00\x00\x00\x00\x00(\xb5/\xfd %)\x01\x00\x12\x00\x18\x00"\x02*\x000\x008\x00J\x00R\x0f\x08\x02\x10\x01\x18\xbc\xd3\x8c\x95\x06%\x00\x00\x00@`\xbc\xd3\x8c\x95\x06'
            )
        },
        keys={
            KVSAAS_KEY_FORMAT.format(prefix=ENCODED_KVSAAS_PREFIX, id=puid): KVSAAS_KEY_FORMAT.format(prefix=KVSAAS_PREFIX, id=puid)
        },
    ):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"puid": bigb_uid},
            test_time=1500000000,
            keywords=[1197, 1200, 1212],
        )

    check_answer(
        {},
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_market_dj_kvsaas_with_bad_kvsaas_profile(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    puid = "p{id}".format(id=bigb_uid)

    test_environment_with_kv_saas.profiles.add(
        {
            "p{id}".format(id=bigb_uid): {
                "DjProfiles": [
                    {
                        "keyword_id": 1200,
                        "profile": {
                            "ObjectId": "p{id}".format(id=bigb_uid),
                            "IsYandex": True,
                            "LastUpdateTime": 1500000000,  # without LastUpdateTime profile will be deleted in CleanUp
                        },
                    },
                ]
            }
        }
    )

    with test_environment_with_kv_saas.kv_saas_server(send_empty_response=True):
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"puid": bigb_uid},
            test_time=1500000000,
            keywords=[1197, 1200, 1212],
        )

    check_answer(
        {
            "dj_profiles": [
                {
                    "keyword_id": 1200,
                    "profile": {
                        "ObjectId": puid,
                        "ObjectNamespace": 1,
                        "ObjectType": 2,
                        "IsYandex": True,
                        "Counters": {
                            "CompressedCounters": ""
                        },
                        "ArchiveData": {},
                        "Erfs": [],
                        "LastUpdateTime": 1500000000
                    }
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_market_dj_kvsaas_without_kvsaas_profile(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()
    puid = "p{id}".format(id=bigb_uid)

    test_environment_with_kv_saas.profiles.add(
        {
            "p{id}".format(id=bigb_uid): {
                "DjProfiles": [
                    {
                        "keyword_id": 1200,
                        "profile": {
                            "ObjectId": "p{id}".format(id=bigb_uid),
                            "IsYandex": True,
                            "LastUpdateTime": 1500000000,  # without LastUpdateTime profile will be deleted in CleanUp
                        },
                    },
                ]
            }
        }
    )

    with test_environment_with_kv_saas.kv_saas_server():
        result = test_environment_with_kv_saas.request(
            client="debug",
            ids={"puid": bigb_uid},
            test_time=1500000000,
            keywords=[1197, 1200, 1212],
        )

    check_answer(
        {
            "is_full": False,
            "incompleted_bigb_requests": MARKET_KVSAAS_FAIL,
            "dj_profiles": [
                {
                    "keyword_id": 1200,
                    "profile": {
                        "ObjectId": puid,
                        "ObjectNamespace": 1,
                        "ObjectType": 2,
                        "IsYandex": True,
                        "Counters": {
                            "CompressedCounters": ""
                        },
                        "ArchiveData": {},
                        "Erfs": [],
                        "LastUpdateTime": 1500000000,
                    }
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )


def test_market_dj_filtering_secondary_profiles(test_environment):
    bigb_uid = "b44fca396a99538584c7e886c605e168"
    uuid = "uuid/{id}".format(id=bigb_uid)

    test_environment.profiles.add(
        {
            "y123450000606001": {
                "DjProfiles": [
                    {
                        "keyword_id": 1200,
                        "profile": {
                            "ObjectId": "y123450000606001",
                            "Erfs": [
                                {
                                    "LastUpdateTime": 1499999997  # without LastUpdateTime profile will be deleted in CleanUp
                                }
                            ],
                        },
                    },
                ]
            },
            "y123450000606002": {
                "DjProfiles": [
                    {
                        "keyword_id": 1200,
                        "profile": {
                            "ObjectId": "y123450000606002",
                            "Erfs": [
                                {
                                    "LastUpdateTime": 1499999998  # without LastUpdateTime profile will be deleted in CleanUp
                                }
                            ],
                        },
                    },
                ]
            },
            "gaid/777001": {
                "DjProfiles": [
                    {
                        "keyword_id": 1200,
                        "profile": {
                            "ObjectId": "gaid/777001",
                            "Erfs": [
                                {
                                    "LastUpdateTime": 1499999999  # without LastUpdateTime profile will be deleted in CleanUp
                                }
                            ],
                        },
                    },
                ]
            },
        }
    )

    test_environment.vulture.add({
        uuid: {
            "KeyRecord": {"user_id": "{_id}".format(_id=bigb_uid), "id_type": 7},
            "ValueRecords": [
                {"user_id": "123450000606001", "id_type": 1, "crypta_graph_distance": 1},
                {"user_id": "123450000606002", "id_type": 1, "crypta_graph_distance": 2},
                {"user_id": "777001", "id_type": 8, "crypta_graph_distance": 1},
            ],
        }
    }, sync=True)

    result = test_environment.request(
        client="debug",
        ids={"uuid": bigb_uid},
        test_time=1500000000,
        keywords=[1197, 1200, 1212],
        glue_type=GlueType.VULTURE_CRYPTA,
    )

    check_answer(
        {
            "dj_profiles": [
                {
                    "keyword_id": 1200,
                    "profile": {
                        "ObjectId": uuid,
                        "ObjectNamespace": 1,
                        "ObjectType": 2,
                        "Counters": {
                            "CompressedCounters": ""
                        },
                        "ArchiveData": {},
                        "Erfs": [
                            {
                                "ErfNamespace": 0,
                                "ErfType": 0,
                                "LastUpdateTime": 1499999999
                            },
                        ]
                    }
                }
            ]
        },
        result.answer,
        ignored_fields=["source_uniqs"],
    )
