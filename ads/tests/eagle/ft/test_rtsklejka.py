# -*- coding: utf-8 -*-
import time
import pytest

from ads.libs.py_hash import yabs_md5

from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer
from crypta.lib.python.identifiers import identifiers as id_lib
from crypta.graph.rt.events.proto.fp_pb2 import TFingerprint

IP = "ed0f:8335:1eca:cbcf:96c4:aafd:a5dc:47f5"
UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36"


def test_glue_michurin(test_environment):
    ifv = id_lib.Ifv(str(test_environment.new_uuid())).normalize
    gaid = id_lib.Gaid(str(test_environment.new_uuid())).normalize
    test_environment.profiles.add(
        {
            "ifv/{}".format(ifv): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]},
            "gaid/{}".format(gaid): {
                "UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]
            },
        }
    )

    yuid = str(test_environment.new_uid())
    test_environment.vulture.add(
        {
            "mi:y{}".format(yuid): {
                "KeyRecord": {"user_id": yuid, "id_type": 1, "is_main": True},
                "ValueRecords": [{"user_id": ifv, "id_type": 19, "crypta_graph_distance": 2}],
            },
            "y{}".format(yuid): {
                "KeyRecord": {"user_id": yuid, "id_type": 1, "is_main": True},
                "ValueRecords": [{"user_id": gaid, "id_type": 8, "crypta_graph_distance": 2}],
            },
        }
    )

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseMichurinMatching": True,
                "UseVultureMatching": False,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [123],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 1,
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1, 3],
                    "parent_profile_id": "y{}".format(yuid),
                },
                {
                    "uniq_id": "ifv/{}".format(ifv),
                    "is_main": False,
                    "user_id": ifv,
                    "id_type": 19,
                    "crypta_graph_distance": 2,
                    "link_type": 3,
                    "link_types": [3],
                    "parent_profile_id": "y{}".format(yuid),
                },
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def prepare_env(test_environment, ua=UA, ip=IP, version=0):
    gaid = id_lib.Gaid(str(test_environment.new_uuid())).normalize
    idfa = id_lib.Idfa(str(test_environment.new_uuid())).normalize
    oaid = id_lib.Oaid(str(test_environment.new_uuid())).normalize
    test_environment.profiles.add(
        {"gaid/{}".format(gaid): {"UserItems": [{"keyword_id": 235, "update_time": 1500000000, "uint_values": [123]}]}}
    )

    yuid = str(test_environment.new_uid())

    fprint = TFingerprint()
    fprint.UserIP = ip
    fprint.UAHash = yabs_md5(ua)
    fprint.Version = version

    fprintpoor = TFingerprint()
    fprintpoor.UserIP = ip
    fprintpoor.Version = version

    fprint = fprint.SerializeToString()
    fprintpoor = fprintpoor.SerializeToString()

    test_environment.vulture.add(
        {
            b"he:" + fprint: {
                "KeyRecord": {"user_id": fprint, "id_type": 21, "is_main": True},
                "ValueRecords": [
                    {
                        "user_id": gaid,
                        "id_type": 8,
                        "crypta_graph_distance": 2,
                        "link_type": 4,
                        "link_types": [4],
                    },
                    {
                        "user_id": idfa,
                        "id_type": 9,
                        "crypta_graph_distance": 3,
                        "link_type": 4,
                        "link_types": [4],
                    },
                    {
                        "user_id": oaid,
                        "id_type": 15,
                        "crypta_graph_distance": 4,
                        "link_type": 4,
                        "link_types": [4],
                    },
                ],
            },
            b"he:" + fprintpoor: {
                "KeyRecord": {"user_id": fprintpoor, "id_type": 21, "is_main": True},
                "ValueRecords": [
                    {
                        "user_id": gaid,
                        "id_type": 8,
                        "crypta_graph_distance": 5,
                        "link_type": 4,
                        "link_types": [4],
                    },
                    {
                        "user_id": idfa,
                        "id_type": 9,
                        "crypta_graph_distance": 7,
                        "link_type": 4,
                        "link_types": [4],
                    },
                ],
            },
        },
    )

    return gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor


@pytest.mark.parametrize("limit", [5, 2, 0])
def test_herschel_glue_request_limit(test_environment, limit):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": limit,
                "RtfpPoorProfileLimit": 1,
            }
        }
    }

    source_uniqs = [
        {
            "uniq_id": "y{}".format(yuid),
            "is_main": True,
            "user_id": yuid,
            "id_type": 5,  # UNKNOWN_YID
            "crypta_graph_distance": 0,
            "link_type": 1,
            "link_types": [1],
        },
        {
            "uniq_id": "gaid/{}".format(gaid),
            "is_main": False,
            "user_id": gaid,
            "id_type": 8,  # GAID
            "crypta_graph_distance": 2,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprint,
        },
        {
            "uniq_id": "idfa/{}".format(idfa),
            "is_main": False,
            "user_id": idfa,
            "id_type": 9,  # IDFA
            "crypta_graph_distance": 3,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprint,
        },
        {
            "uniq_id": "oaid/{}".format(oaid),
            "is_main": False,
            "user_id": oaid,
            "id_type": 15,  # OAID
            "crypta_graph_distance": 4,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprint,
        },
    ]

    items = [
        {
            "keyword_id": 235,
            "update_time": 1500000000,
            "uint_values": [123],
            "main": False,
            "source_uniq_index": 1,
        },
        {
            "keyword_id": 564,
            "update_time": 1500000000,
            "uint_values": [1],
            "source_uniq_index": -1,
        },
    ]

    if limit == 0:
        del items[0]

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": items,
            "source_uniqs": source_uniqs[: 1 + limit],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


@pytest.mark.parametrize("limit", [5, 2, 0])
def test_herschel_glue_request_with_glued_limit(test_environment, limit):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    test_environment.vulture.add(
        {
            "y{}".format(yuid): {
                "KeyRecord": {"user_id": yuid, "id_type": 1, "is_main": True},
                "ValueRecords": [
                    {
                        "user_id": gaid,
                        "id_type": 8,
                        "crypta_graph_distance": 3,
                        "link_type": 3,
                        "link_types": [3],
                    },
                ],
            },
        }
    )

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": limit,
                "RtfpPoorProfileLimit": 1,
            }
        }
    }

    source_uniqs = [
        {
            "uniq_id": "y{}".format(yuid),
            "is_main": True,
            "user_id": yuid,
            "id_type": 1,  # YANDEXUID
            "crypta_graph_distance": 0,
            "link_type": 1,
            "link_types": [1, 3],
            "parent_profile_id": "y{}".format(yuid),
        },
        {
            "uniq_id": "gaid/{}".format(gaid),
            "is_main": False,
            "user_id": gaid,
            "id_type": 8,  # GAID
            "crypta_graph_distance": 2,
            "link_type": 3,
            "link_types": [3, 4],
            "parent_profile_id": "y{}".format(yuid),
        },
        {
            "uniq_id": "idfa/{}".format(idfa),
            "is_main": False,
            "user_id": idfa,
            "id_type": 9,  # IDFA
            "crypta_graph_distance": 3,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprint,
        },
        {
            "uniq_id": "oaid/{}".format(oaid),
            "is_main": False,
            "user_id": oaid,
            "id_type": 15,  # OAID
            "crypta_graph_distance": 4,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprint,
        },
    ]

    items = [
        {
            "keyword_id": 235,
            "update_time": 1500000000,
            "uint_values": [123],
            "main": False,
            "source_uniq_index": 1,
        },
        {
            "keyword_id": 564,
            "update_time": 1500000000,
            "uint_values": [1],
            "source_uniq_index": -1,
        },
    ]

    if limit == 2:
        del source_uniqs[3]
    elif limit == 0:
        del source_uniqs[3]
        del source_uniqs[2]
        source_uniqs[1].update({"crypta_graph_distance": 3, "link_types": [3]})

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": items,
            "source_uniqs": source_uniqs,
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


@pytest.mark.parametrize("limit", [5, 2, 0])
def test_herschel_poor_glue_request_limit(test_environment, limit):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelPoorMatching": True,
                "RtfpWithoutLimitsCount": limit,
                "RtfpPoorProfileLimit": 1,
            }
        }
    }

    source_uniqs = [
        {
            "uniq_id": "y{}".format(yuid),
            "is_main": True,
            "user_id": yuid,
            "id_type": 5,  # UNKNOWN_YID
            "crypta_graph_distance": 0,
            "link_type": 1,
            "link_types": [1],
        },
        {
            "uniq_id": "gaid/{}".format(gaid),
            "is_main": False,
            "user_id": gaid,
            "id_type": 8,  # GAID
            "crypta_graph_distance": 5,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprintpoor,
        },
        {
            "uniq_id": "idfa/{}".format(idfa),
            "is_main": False,
            "user_id": idfa,
            "id_type": 9,  # IDFA
            "crypta_graph_distance": 7,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprintpoor,
        },
    ]

    items = [
        {
            "keyword_id": 235,
            "update_time": 1500000000,
            "uint_values": [123],
            "main": False,
            "source_uniq_index": 1,
        },
        {
            "keyword_id": 564,
            "update_time": 1500000000,
            "uint_values": [1],
            "source_uniq_index": -1,
        },
    ]

    if limit == 0:
        del items[0]

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": items,
            "source_uniqs": source_uniqs[: 1 + limit],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_herschel_glue_request_non_poor(test_environment):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpPoorProfileLimit": 0,
                "RtfpForcePoorHerschel": False,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 5,  # UNKNOWN_YID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
            ],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_herschel_glue_request_force_poor(test_environment):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpPoorProfileLimit": 0,
                "RtfpForcePoorHerschel": True,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [123],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 5,  # UNKNOWN_YID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": False,
                    "user_id": gaid,
                    "id_type": 8,  # GAID
                    "crypta_graph_distance": 2,
                    "link_type": 4,
                    "link_types": [3, 4],
                    "parent_profile_id": b"fp/" + fprint,
                },
            ],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


@pytest.mark.parametrize("ua,ip", [("", ""), (UA, ""), ("", IP)])
def test_herschel_glue_request_no_fp(test_environment, ua, ip):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment, ua, ip)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpPoorProfileLimit": 1,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    extra_info = []
    if ua:
        extra_info.append({"key": "ua", "value": ua})
    if ip:
        extra_info.append({"key": "ipv6", "value": ip})
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 5,  # UNKNOWN_YID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                }
            ],
            "extra_info": extra_info,
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


@pytest.mark.parametrize("key", ["y-zero", "y-new"])
def test_herschel_glue_request_custom_slice(test_environment, key):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)
    if key == "y-zero":
        yuid = "0"
    elif key == "y-new":
        yuid = "1234567890{}".format(int(time.time() - 1.5 * 60 * 60))

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpPoorProfileLimit": 1,
                "UseHerschelOnCustomSlice": True,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 235,
                    "update_time": 1500000000,
                    "uint_values": [123],
                    "main": False,
                    "source_uniq_index": 1,
                },
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 5,  # UNKNOWN_YID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
                {
                    "uniq_id": "gaid/{}".format(gaid),
                    "is_main": False,
                    "user_id": gaid,
                    "id_type": 8,  # GAID
                    "crypta_graph_distance": 2,
                    "link_type": 4,
                    "link_types": [3, 4],
                    "parent_profile_id": b"fp/" + fprint,
                },
            ],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_herschel_glue_request_custom_slice_skip_puid(test_environment):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)
    yuid = "1234567890{}".format(int(time.time() - 1.5 * 60 * 60))
    puid = "8728282"

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 5,
                "RtfpPoorProfileLimit": 1,
                "UseHerschelOnCustomSlice": True,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua, "puid": puid},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 5,  # UNKNOWN_YID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
                {
                    "uniq_id": "p{}".format(puid),
                    "is_main": True,
                    "user_id": puid,
                    "id_type": 6,  # PUID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                },
            ],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


@pytest.mark.parametrize("client", ["yabs", "ssp"])
def test_herschel_glue_request_no_client(test_environment, client):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpPoorProfileLimit": 1,
                "HerschelEnabledClients": {
                    "Values": ["ssp-international", "search"],
                },
            }
        }
    }

    result = test_environment.request(
        client=client,
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": [
                {
                    "keyword_id": 564,
                    "update_time": 1500000000,
                    "uint_values": [1],
                    "source_uniq_index": -1,
                },
            ],
            "source_uniqs": [
                {
                    "uniq_id": "y{}".format(yuid),
                    "is_main": True,
                    "user_id": yuid,
                    "id_type": 5,  # UNKNOWN_YID
                    "crypta_graph_distance": 0,
                    "link_type": 1,
                    "link_types": [1],
                }
            ],
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


@pytest.mark.parametrize("client", ["yabs", "ssp"])
def test_herschel_glue_request_ok_client(test_environment, client):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpPoorProfileLimit": 1,
                "HerschelEnabledClients": {
                    "Values": ["yabs", "ssp"],
                },
            }
        }
    }

    source_uniqs = [
        {
            "uniq_id": "y{}".format(yuid),
            "is_main": True,
            "user_id": yuid,
            "id_type": 5,  # UNKNOWN_YID
            "crypta_graph_distance": 0,
            "link_type": 1,
            "link_types": [1],
        },
        {
            "uniq_id": "gaid/{}".format(gaid),
            "is_main": False,
            "user_id": gaid,
            "id_type": 8,  # GAID
            "crypta_graph_distance": 2,
            "link_type": 4,
            "link_types": [3, 4],
            "parent_profile_id": b"fp/" + fprint,
        },
    ]

    items = [
        {
            "keyword_id": 235,
            "update_time": 1500000000,
            "uint_values": [123],
            "main": False,
            "source_uniq_index": 1,
        },
        {
            "keyword_id": 564,
            "update_time": 1500000000,
            "uint_values": [1],
            "source_uniq_index": -1,
        },
    ]

    result = test_environment.request(
        client=client,
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )
    check_answer(
        {
            "items": items,
            "source_uniqs": source_uniqs,
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )


def test_fp_source_uniq_nocount_limit(test_environment):
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment)

    source_uniqs = [
        {
            "uniq_id": "y{}".format(yuid),
            "is_main": True,
            "user_id": yuid,
            "id_type": 1,  # UNKNOWN_YID
            "crypta_graph_distance": 0,
            "link_type": 1,
            "link_types": [1, 3],
            "parent_profile_id": "y{}".format(yuid),
        },
    ]
    value_records = []
    for distance in range(1, 6):
        relative = str(test_environment.new_uid())
        value_records.append(
            {
                "user_id": relative,
                "id_type": 1,
                "crypta_graph_distance": distance,
            }
        )
        source_uniqs.append(
            {
                "uniq_id": "y{}".format(relative),
                "is_main": False,
                "user_id": relative,
                "id_type": 1,
                "crypta_graph_distance": distance,
                "link_type": 3,
                "link_types": [3],
                "parent_profile_id": "y{}".format(yuid),
            }
        )

    test_environment.vulture.add(
        {
            "y{}".format(yuid): {
                "KeyRecord": {"user_id": yuid, "id_type": 1, "is_main": True},
                "ValueRecords": value_records,
            },
        }
    )

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "UseHerschelPoorMatching": True,
                "MaxSecondaryProfiles": 5,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )

    check_answer(
        {
            "source_uniqs": source_uniqs,
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=[
            "items",
            "tsar_vectors",
        ],
    )


@pytest.mark.parametrize("exp_version", [0, 1])
@pytest.mark.parametrize("hfp_version", [0, 1])
def test_herschel_glue_request_versions(test_environment, exp_version, hfp_version):
    # previous parametrized test still keept in database and make bad result
    # so we need to clear vulture
    test_environment.vulture.clear()
    gaid, idfa, oaid, yuid, ua, ip, fprint, fprintpoor = prepare_env(test_environment, version=hfp_version)

    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "UseHerschelMatching": True,
                "RtfpWithoutLimitsCount": 1,
                "RtfpForcePoorHerschel": True,
                "HerschelVersion": exp_version,
            }
        }
    }

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": yuid, "ip": ip, "ua": ua},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
        keywords=[235, 328, 564, 725],
        exp_json=exp_json,
    )

    if exp_version == hfp_version:
        items = [
            {
                "keyword_id": 235,
                "update_time": 1500000000,
                "uint_values": [123],
                "main": False,
                "source_uniq_index": 1,
            },
            {
                "keyword_id": 564,
                "update_time": 1500000000,
                "uint_values": [1],
                "source_uniq_index": -1,
            },
        ]
        source_uniqs = [
            {
                "uniq_id": "y{}".format(yuid),
                "is_main": True,
                "user_id": yuid,
                "id_type": 5,  # UNKNOWN_YID
                "crypta_graph_distance": 0,
                "link_type": 1,
                "link_types": [1],
            },
            {
                "uniq_id": "gaid/{}".format(gaid),
                "is_main": False,
                "user_id": gaid,
                "id_type": 8,  # GAID
                "crypta_graph_distance": 2,
                "link_type": 4,
                "link_types": [3, 4],
                "parent_profile_id": b"fp/" + fprint,
            },
        ]
    else:
        items = [
            {
                "keyword_id": 564,
                "update_time": 1500000000,
                "uint_values": [1],
                "source_uniq_index": -1,
            },
        ]
        source_uniqs = [
            {
                "uniq_id": "y{}".format(yuid),
                "is_main": True,
                "user_id": yuid,
                "id_type": 5,  # UNKNOWN_YID
                "crypta_graph_distance": 0,
                "link_type": 1,
                "link_types": [1],
            },
        ]

    check_answer(
        {
            "items": items,
            "source_uniqs": source_uniqs,
            "extra_info": [
                {"key": "ua", "value": UA},
                {"key": "ipv6", "value": IP},
            ],
        },
        result.answer,
        ignored_fields=["tsar_vectors"],
    )
