from ads.bsyeti.protos.profile_pb2 import TProfileProto
from ads.bsyeti.libs.primitives.counter_proto.counter_ids_pb2 import ECounterId

TS = 1600000000


def validate_big_profile(big_profile):
    keys = big_profile.keys()

    expected = []
    d = TProfileProto.DESCRIPTOR
    for field in d.fields:
        expected.append(field.name)

    diff = set(expected) - set(keys)
    assert len(diff) == 0, f"Добавьте в тест ниже новые поля, плиз: {diff}"


def value_type_to_dict_update(t):
    if t in [1, 2, 3, 4]:
        return {"uint_values": [1]}
    elif t in [8, 13]:
        return {
            "pair_values": [
                {"first": 1, "second": 1},
            ],
        }
    elif t in [5, 12]:
        return {
            "weighted_uint_values": [
                {"first": 1, "weight": 2, "update_time": TS},
            ]
        }
    elif t in [11, 14]:
        return {
            "trio_values": [
                {"first": 1, "second": 1, "trird": 1},
            ]
        }
    elif t in [9, 10]:
        return {
            "weighted_pair_values": [
                {"first": 1, "second": 1, "weight": 1, "update_time": TS},
            ]
        }
    elif t in [6]:
        return {
            "string_value": "apple,iphone,42,0,1",
        }
    else:
        return {"uint_values": [1]}


def get_big_bigb_profile(kw_to_type):
    return {
        "Banners": [
            {
                "banner_id": 42,
                "bid_multiplier": 874,
                "is_smart": False,
                "main": True,
                "phrase_id": 8784788,
                "rank": 1000,
                "select_type": 52,
                "source_uniq_index": 0,
                "update_time": TS,
            },
            {
                "banner_id": 44,
                "bid_multiplier": 1000,
                "is_smart": False,
                "main": True,
                "phrase_id": 43,
                "rank": 1000,
                "select_type": 61,
                "source_uniq_index": 0,
                "update_time": TS,
            },
            {
                "banner_id": 54,
                "bid_multiplier": 1000,
                "is_smart": True,
                "main": True,
                "quorum_ids": 264192,
                "rank": 1000,
                "select_type": 78,
                "source_uniq_index": 0,
                "update_time": TS,
            },
        ],
        "UserItems": [
            {"update_time": TS, "keyword_id": kw} | value_type_to_dict_update(type)
            for kw, type in kw_to_type.items()
            if kw not in [284, 328, 377, 458]
        ],
        "Interests": [
            {
                "bm_category_id": 0,
                "clicks": 112,
                "event_time": TS,
                "interest": 5240,
                "interest_update_time": TS,
                "main": True,
                "shows": 5784,
                "source_uniq_index": 0,
            },
            {
                "bm_category_id": 52570,
                "clicks": 31,
                "event_time": TS,
                "interest": 169,
                "interest_update_time": TS,
                "main": True,
                "shows": 1146,
                "source_uniq_index": 0,
            },
        ],
        "Counters": [
            {"counter_id": 142, "key": [573232094, 1397105660], "value": [3.999922513961792, 2]},
            {"counter_id": 143, "key": [573232094, 1397105660], "value": [TS, TS]},
        ]
        + [
            {
                "counter_id": id,
                "key": [TS],
                "value": [TS],
            }
            for id in range(0, max(ECounterId.values()) + 1)
            if id not in [142, 143]
        ]
        + [
            {
                "counter_id": 200,
                "key": [200000073, 200000079, 200000244, 200000246, 200000435],
                "value": [
                    3.46322274208,
                    8.99720954895,
                    9.37608909607,
                    13.7542047501,
                    29.181854248,
                ],
            },
            {
                "counter_id": 201,
                "key": [200000073, 200000079, 200000244, 200000246, 200000435],
                "value": [TS] * 5,
            },
        ],
        "Offers": [
            {
                "counter_id": 42,
                "offer_id_md5": 225224,
                "action_bits": 1,
                "update_time": TS + 10000,
                "select_type": 27,
            },
        ],
        "Queries": [
            {"query_id": i, "query_text": query}
            for i, query in enumerate(
                [
                    "buy smth",
                    "some other words",
                ]
            )
        ],
        "LegacyBanners": [{}],
        "Dmps": [{}],
        "LmFeatures": [
            {
                "counter_id": 95,
            },
            {
                "counter_id": 123,
            },
        ],
        "RegularCoords": [
            {
                "latitude": 55.57,
                "longitude": 41.99,
                "update_time": TS,
                "type": 1,
            },
            {
                "latitude": 35.3,
                "longitude": 33.2,
                "update_time": TS,
                "type": 1,
            },
        ],
        "AuditoriumSegments": {},
        "Applications": [
            {
                "active_month_frequency": 1.9083289131225425,
                "crc32_hash": 3202192353,
                "disabled_status": 2,
                "in_device": False,
                "install_time": TS,
                "last_active_time": TS,
                "main": True,
                "md5int_hash": 812506903108170596,
                "source": 3,
                "source_uniq_index": 1,
                "system_status": 2,
                "update_time": TS,
            }
        ],
        "AdSystems": [{}],
        "VisitStates": [{}],
        "DelayedCounterUpdates": [{}],
        "Carts": [{}],
        "InterestsUpdates": [{}],
        "WatchIdStates": [{}],
        "AuraRecommendations": [{}],
        "AuraVectors": [{}],
        "CounterPack": [
            {"counter_id": 589, "key": [346548009], "value": [5.502243995666504]},
        ],
        "SspFeedbacks": [{}],
        "DspFeedbacks": [{}],
        "UserSubscriptions": [{}],
        "UserEconomy": [{}],
        "UserActivity": [{}],
        "TsarVectors": [{}],
        "AudienceSegmentsWithPriorities": {
            "segments_with_priorities": [
                {"priority": 500, "segments": [TS, 1, 2, 3, 4]},
                {"priority": 100, "segments": [TS, 1, 2, 3]},
                {"priority": 50, "segments": [TS, 1, 2, 3]},
            ],
            "timestamp": TS,
        },
        "CreationTime": TS,
        "ImportantRegions": [{}],
        "CdpSegments": {
            "segments": [TS, 1],
            "timestamp": TS,
        },
        "LastRsyaBannerClicks": [{}],
        "LastSearchBannerClicks": [{}],
        "MarketLoyaltyCoins": [{}],
        "MarketLoyaltyDisabledPromoThreasholds": [{}],
        "OrderShows": [],
        "FrequencyEvents": [],
        "DjProfiles": [{}],
    }
    # сюда нужно добавлять новое поле с данными, которые не будет чиститься клинапом и
    # смогут отдаваться всем клиентам, запрашивающим его
