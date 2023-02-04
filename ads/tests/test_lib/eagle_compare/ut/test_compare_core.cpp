#include <library/cpp/testing/unittest/registar.h>

#include <ads/bsyeti/tests/test_lib/eagle_compare/lib/compare.h>

#include <yabs/proto/user_profile.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>


Y_UNIT_TEST_SUITE(CompareCore) {

    NBSYeti::TPublicProfileProto PP(const TString jsonProfile) {
        return NProtobufJson::Json2Proto<NBSYeti::TPublicProfileProto>(jsonProfile);
    }

    Y_UNIT_TEST(ComparePass) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), true);
    }

    Y_UNIT_TEST(CompareFail) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 123
            }
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "user_identifiers": {
                "CryptaId": 1337
            }
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
    }

    Y_UNIT_TEST(QueriesEqualTime) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "queries": [
                {
                    "unix_update_time": 123
                }
            ],
            "query_candidates": [
                {
                    "unix_update_time": 123
                }
            ]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "queries": [
                {
                    "unix_update_time": 1337
                }
            ],
            "query_candidates": [
                {
                    "unix_update_time": 1337
                }
            ]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), true);
    }

    Y_UNIT_TEST(QueriesNotEqual) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "queries": [
                {
                    "query_id": 123
                }
            ]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "queries": [
                {
                    "query_id": 1337
                }
            ]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
    }

    Y_UNIT_TEST(CheckKeywordSorted) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "banners": [
                {
                    "select_type": 1,
                    "banner_id": 1
                },
                {
                    "select_type": 2,
                    "banner_id": 2
                },
                {
                    "select_type": 3,
                    "banner_id": 3
                }
            ]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "banners": [
                {
                    "select_type": 2,
                    "banner_id": 2
                },
                {
                    "select_type": 3,
                    "banner_id": 3
                },
                {
                    "select_type": 1,
                    "banner_id": 1
                }
            ]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), true);
    }

    Y_UNIT_TEST(CheckKeywordSortedFailed) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "banners": [
                {
                    "select_type": 1,
                    "banner_id": 123
                },
                {
                    "select_type": 3,
                    "banner_id": 444
                },
                {
                    "select_type": 5,
                    "banner_id": 777
                }
            ]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "banners": [
                {
                    "select_type": 1,
                    "banner_id": 123
                },
                {
                    "select_type": 4,
                    "banner_id": 444
                },
                {
                    "select_type": 5,
                    "banner_id": 777
                }
            ]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
    }

    Y_UNIT_TEST(CheckCounterSorted) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "counters": [
                {
                    "counter_id": 1,
                    "key": [1, 2, 3],
                    "value": [1.0, 2.0, 3.0]
                },
                {
                    "counter_id": 2,
                    "key": [11, 12, 13],
                    "value": [211.0, 112.0, 13.0]
                },
                {
                    "counter_id": 3,
                    "key": [21, 22, 23],
                    "value": [421.0, 222.0, 323.0]
                }
            ]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "counters": [
                {
                    "counter_id": 2,
                    "key": [11, 13, 12],
                    "value": [211.0, 13.0, 112.0]
                },
                {
                    "counter_id": 3,
                    "key": [22, 23, 21],
                    "value": [222.0, 323.0, 421.0]
                },
                {
                    "counter_id": 1,
                    "key": [3, 2, 1],
                    "value": [3.0, 2.0, 1.0]
                }
            ]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), true);
    }

    Y_UNIT_TEST(CheckCounterSortedFailed) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "counters": [
                {
                    "counter_id": 1,
                    "key": [1, 2, 3],
                    "value": [1.0, 2.0, 3.0]
                },
                {
                    "counter_id": 3,
                    "key": [11, 12, 13],
                    "value": [211.0, 112.0, 13.0]
                },
                {
                    "counter_id": 5,
                    "key": [21, 22, 23],
                    "value": [421.0, 222.0, 323.0]
                }
            ]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "counters": [
                {
                    "counter_id": 1,
                    "key": [1, 2, 3],
                    "value": [1.0, 2.0, 3.0]
                },
                {
                    "counter_id": 4,
                    "key": [11, 12, 13],
                    "value": [211.0, 112.0, 13.0]
                },
                {
                    "counter_id": 5,
                    "key": [21, 22, 23],
                    "value": [421.0, 222.0, 323.0]
                }
            ]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
        UNIT_ASSERT_EQUAL(tr.Keywords.find("328_4_counters")->second.AddedKeywords.size(), 1); // NBSData::NKeywords::KW_BT_COUNTER
        UNIT_ASSERT_EQUAL(tr.Keywords.find("328_3_counters")->second.DeletedKeywords.size(), 1); // NBSData::NKeywords::KW_BT_COUNTER
    }

    Y_UNIT_TEST(CompareIsFullPass) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "is_full": true
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "is_full": true
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), true);
    }

    Y_UNIT_TEST(CompareIsFullFail) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "is_full": true
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "is_full": false
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
    }

    Y_UNIT_TEST(CompareAdded) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "lm_features": []
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "lm_features": [{
                "counter_id": 123
            }]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
        UNIT_ASSERT_EQUAL(tr.Keywords.find("328_123_lm_features")->second.AddedKeywords.size(), 1); // NBSData::NKeywords::KW_BT_COUNTER
    }

    Y_UNIT_TEST(CompareDeleted) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "applications": [{
                "md5int_hash": 123
            }]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
        UNIT_ASSERT_EQUAL(tr.Keywords.find("395")->second.DeletedKeywords.size(), 1); // NBSData::NKeywords::KW_EXCEPT_APPS_ON_CPI
        UNIT_ASSERT_EQUAL(tr.Keywords.find("541") == tr.Keywords.end(), true); // NBSData::NKeywords::KW_INSTALLED_MOBILE_APPS
    }

    Y_UNIT_TEST(CompareModified) {
        NBSYeti::TProtoCompareResult tr;
        auto p1 = PP(R"JSONMSG({
            "items": [{
                "keyword_id": 1
            }]
        })JSONMSG");
        auto p2 = PP(R"JSONMSG({
            "items": [{
                "keyword_id": 2
            }]
        })JSONMSG");
        UNIT_ASSERT_EQUAL(NBSYeti::CompareProtos(p1, p2, tr), false);
        UNIT_ASSERT_EQUAL(tr.Keywords.find("1")->second.DeletedKeywords.size(), 1);
        UNIT_ASSERT_EQUAL(tr.Keywords.find("2")->second.AddedKeywords.size(), 1);
    }

    // TODO: Test Accuracy

    // TODO: Test tsar_vectors
}
