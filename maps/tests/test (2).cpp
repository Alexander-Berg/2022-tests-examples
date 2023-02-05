#include <maps/wikimap/feedback/pushes/addresses/prepare_dwellplaces/lib/aggregate_reducer.h>
#include <maps/wikimap/feedback/pushes/addresses/prepare_dwellplaces/lib/prepare_dwellplaces.h>
#include <maps/wikimap/feedback/pushes/addresses/prepare_dwellplaces/tests/constants.h>
#include <maps/wikimap/feedback/pushes/addresses/prepare_dwellplaces/tests/helpers.h>

#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>


namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests {

Y_UNIT_TEST_SUITE(prepare_dwellplaces) {

Y_UNIT_TEST(load_ad_name_candidates)
{
    auto clientPtr = NYT::NTesting::CreateTestClient();

    createTable(clientPtr, AD_NM_TABLE);
    writeToTable(
        clientPtr,
        AD_NM_TABLE,
        {
            NYT::TNode::CreateMap()
                ("lang", "ru")
                ("ad_id", 1u)
                ("is_auto", false)
                ("name", "СНТ 1"),
            NYT::TNode::CreateMap()
                ("lang", "ru")
                ("ad_id", 2u)
                ("is_auto", true)
                ("name", "СНТ 2"),
            NYT::TNode::CreateMap()
                ("lang", "en")
                ("ad_id", 3u)
                ("is_auto", false)
                ("name", "СНТ 1"),
            NYT::TNode::CreateMap()
                ("lang", "ru")
                ("ad_id", 4u)
                ("is_auto", false)
                ("name", "город N"),
            NYT::TNode::CreateMap()
                ("lang", "ru")
                ("ad_id", 5u)
                ("is_auto", false)
                ("name", "СНТ 1")
        }
    );

    createTable(clientPtr, AD_TABLE);
    writeToTable(
        clientPtr,
        AD_TABLE,
        {
            NYT::TNode::CreateMap()
                ("ad_id", 1u)
                ("p_ad_id", 118u),
            NYT::TNode::CreateMap()
                ("ad_id", 2u)
                ("p_ad_id", 118u),
            NYT::TNode::CreateMap()
                ("ad_id", 3u)
                ("p_ad_id", 118u),
            NYT::TNode::CreateMap()
                ("ad_id", 4u)
                ("p_ad_id", 118u),
            NYT::TNode::CreateMap()
                ("ad_id", 5u)
                ("p_ad_id", 1444278541u), // Крым
        }
    );

    std::unordered_map<uint64_t, std::string> expected{
        {1u, "СНТ 1"},
    };
    UNIT_ASSERT_VALUES_EQUAL(
        loadAdCandidates(clientPtr, YMAPSDF_PATH),
        expected
    );
}

Y_UNIT_TEST(load_whitelist_ad_ids)
{
    auto clientPtr = NYT::NTesting::CreateTestClient();
    createTable(clientPtr, AD_TABLE);
    writeToTable(
        clientPtr,
        AD_TABLE,
        {
            NYT::TNode::CreateMap()
                ("ad_id", 1u), // ok, no parent, whitelist
            NYT::TNode::CreateMap()
                ("ad_id", 2u)
                ("p_ad_id", 118u), // single parent, got in whitelist
            NYT::TNode::CreateMap()
                ("ad_id", 3u)
                ("p_ad_id", 1444278541u), // bad parent, blacklist
            NYT::TNode::CreateMap()
                ("ad_id", 1444278541u), // bad object, blacklist
            NYT::TNode::CreateMap()
                ("ad_id", 4u)
                ("p_ad_id", 3u), // bad grandpa: 4 -> 3-> 977
            NYT::TNode::CreateMap()
                ("ad_id", 5u)
                ("p_ad_id", 53000056u), // Адыгея
            NYT::TNode::CreateMap()
                ("ad_id", 53000056u) // bad object
                ("p_ad_id", 8u), // parent is ok
            NYT::TNode::CreateMap()
                ("ad_id", 8u), // ok
            NYT::TNode::CreateMap()
                ("ad_id", 53000037u), // Краснодарский край
            NYT::TNode::CreateMap()
                ("ad_id", 10672u), // bad object
        }
    );
    std::unordered_set<uint64_t> expected{
        1u, 2u, 8u, 118u
    };
    UNIT_ASSERT_VALUES_EQUAL(
        loadWhitelistAdIds(clientPtr, YMAPSDF_PATH),
        expected
    );
}

Y_UNIT_TEST(load_ad_geom)
{
    auto clientPtr = NYT::NTesting::CreateTestClient();

    createTable(clientPtr, AD_GEOM_TABLE);

    writeToTable(
        clientPtr,
        AD_GEOM_TABLE,
        {
            NYT::TNode::CreateMap()("ad_id", 1u)("shape", SHAPE1.c_str()),
            NYT::TNode::CreateMap()("ad_id", 2u)("shape", SHAPE2.c_str()),
            NYT::TNode::CreateMap()("ad_id", 3u)("shape", SHAPE3.c_str()),
            NYT::TNode::CreateMap()("ad_id", 4u)("shape", SHAPE1.c_str())
        }
    );

    AdIdToNameMap adIdToName{
        {1u, "СНТ 1"},
        {2u, "СНТ 2"},
        {3u, "СНТ 3"},
        {5u, "СНТ 5"}
    };
    AdIdToAdMap expected{
        {1u, Ad(1u, "СНТ 1", geolib3::MultiPolygon2({POLYGON}))},
        {2u, Ad(2u, "СНТ 2", MULTIPOLYGON)}
    };
    UNIT_ASSERT_VALUES_EQUAL(
        loadAdGeom(clientPtr, YMAPSDF_PATH, adIdToName),
        expected
    );
}

Y_UNIT_TEST(copy_dwell_places_in_ad)
{
    auto clientPtr = NYT::NTesting::CreateTestClient();


    createTable(clientPtr, DWELLPLACES1);
    createTable(clientPtr, DWELLPLACES2);
    createTable(clientPtr, OUTPUT_PATH);

    writeToTable(
        clientPtr,
        DWELLPLACES1,
        {
            NYT::TNode::CreateMap()
                ("Longitude", 15.) // inside SHAPE1 and SHAPE2
                ("Latitude", 20.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 120)
                ("Latitude", 20.)
                ("CDeviceID", "device1")
                ("mmetric_devids", "[\"device1\",\"another_device1\"]"),
            NYT::TNode::CreateMap()
                ("Longitude", 30.) // inside SHAPE2
                ("Latitude", 10.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 2020)
                ("CDeviceID", "device2")
                ("mmetric_devids", "[\"device2\"]"),
            NYT::TNode::CreateMap()
                ("Longitude", 16.) // inside SHAPE1 and SHAPE2
                ("Latitude", 21.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 30020)
                ("CDeviceID", "device3")
                ("mmetric_devids", "[\"device3\"]"),
            NYT::TNode::CreateMap()
                ("Longitude", 50.) // not in known ad
                ("Latitude", 50.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 20)
                ("CDeviceID", "device4")
                ("mmetric_devids", "[\"device5\"]"),
            NYT::TNode::CreateMap()
                ("Longitude", 41.) // inside SHAPE1
                ("Latitude", 41.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 30020)
                ("CDeviceID", "device5")
                ("mmetric_devids", "[\"device5\"]"),
        }
    );
    writeToTable(
        clientPtr,
        DWELLPLACES2,
        {
            NYT::TNode::CreateMap()
                ("Longitude", 14.) // inside SHAPE1 and SHAPE2
                ("Latitude", 21.)
                ("StartTimestamp", 50)
                ("FinishTimestamp", MIN_SPENT_TIME)
                ("CDeviceID", "device1")
                ("mmetric_devids", "[\"device1\"]"),
            NYT::TNode::CreateMap()
                ("Longitude", 14.) // SHAPE1 and SHAPE2
                ("Latitude", 21.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 20)
                ("CDeviceID", "device2")
                ("mmetric_devids", "[\"device2\"]"),
            NYT::TNode::CreateMap()
                ("Longitude", 29.) // SHAPE2
                ("Latitude", 11.)
                ("StartTimestamp", 20)
                ("FinishTimestamp", 5020) // too small period
                ("CDeviceID", "device6")
                ("mmetric_devids", "[\"device6\"]"),
        }
    );

    AdIdToAdMap adIdToAdMap{
        {1u, Ad(1u, "СНТ 1", geolib3::MultiPolygon2({POLYGON}))},
        {2u, Ad(2u, "СНТ 2", MULTIPOLYGON)}
    };

    copyDwellPlacesInAd(clientPtr, adIdToAdMap, {DWELLPLACES1, DWELLPLACES2}, OUTPUT_PATH);

    auto reader = clientPtr->CreateTableReader<NYT::TNode>(OUTPUT_PATH.c_str());

    UNIT_ASSERT_EQUAL(reader->IsValid(), true);
    auto row = reader->GetRow();

    UNIT_ASSERT_EQUAL(row["ad_id"].AsUint64(), 2u);
    UNIT_ASSERT_EQUAL(row["ad_name"].AsString(), "СНТ 2");
    UNIT_ASSERT_EQUAL(row["CDeviceID"].AsString(), "device1");
    UNIT_ASSERT_EQUAL(row["ds_lon"].As<double>(), 14.5);
    UNIT_ASSERT_EQUAL(row["ds_lat"].As<double>(), 20.5);
    UNIT_ASSERT_EQUAL(row["ghash6"].AsString(), "s7dk9g");
    UNIT_ASSERT_EQUAL(row["mmetric_devids"].AsString(), "[\"device1\",\"another_device1\"]");

    reader->Next();
    UNIT_ASSERT_EQUAL(reader->IsValid(), true);
    row = reader->GetRow();

    UNIT_ASSERT_EQUAL(row["ad_id"].AsUint64(), 2u);
    UNIT_ASSERT_EQUAL(row["ad_name"].AsString(), "СНТ 2");
    UNIT_ASSERT_EQUAL(row["CDeviceID"].AsString(), "device3");
    UNIT_ASSERT_EQUAL(row["ds_lon"].As<double>(), 16.);
    UNIT_ASSERT_EQUAL(row["ds_lat"].As<double>(), 21.);
    UNIT_ASSERT_EQUAL(row["ghash6"].AsString(), "s7erkn");
    UNIT_ASSERT_EQUAL(row["mmetric_devids"].AsString(), "[\"device3\"]");

    reader->Next();
    UNIT_ASSERT_EQUAL(reader->IsValid(), true);
    row = reader->GetRow();

    UNIT_ASSERT_EQUAL(row["ad_id"].AsUint64(), 1u);
    UNIT_ASSERT_EQUAL(row["ad_name"].AsString(), "СНТ 1");
    UNIT_ASSERT_EQUAL(row["CDeviceID"].AsString(), "device5");
    UNIT_ASSERT_EQUAL(row["ds_lon"].As<double>(), 41.);
    UNIT_ASSERT_EQUAL(row["ds_lat"].As<double>(), 41.);
    UNIT_ASSERT_EQUAL(row["ghash6"].AsString(), "szm1hz");
    UNIT_ASSERT_EQUAL(row["mmetric_devids"].AsString(), "[\"device5\"]");

    reader->Next();
    UNIT_ASSERT_EQUAL(reader->IsValid(), false);
}

}

} // namespace maps::wikimap::feedback::pushes::addresses::prepare_dwellplaces::tests
