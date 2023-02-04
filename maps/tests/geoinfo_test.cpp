#include <maps/analyzer/libs/geoinfo/include/geoinfo.h>
#include <maps/libs/geolib/include/point.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <filesystem>
#include <string>
#include <vector>


inline std::filesystem::path binaryPath(const char* p) {
    return std::filesystem::path(static_cast<std::string>(BinaryPath(p)));
}


const auto GEODATA_PATH = binaryPath("maps/data/test/geobase/geodata5.bin");
const auto GEOID_PATH = binaryPath("maps/data/test/geoid/geoid.mms.1");


const maps::geolib3::Point2 yandex{37.588091, 55.733818};
const maps::geolib3::Point2 kadikoy{29.078709, 40.961216}; // Istanbul
const maps::geolib3::Point2 tbilisi{44.799307, 41.697048};
const auto MOSCOW_ID = 213;
const auto MOSCOW_REGION_ID = 1;
const auto CENTRAL_DISTRICT_ID = 3;
const auto RUSSIA_ID = 225;
const auto ISTANBUL_ID = 11508;
const auto TURKEY_ID = 983;
const auto TBILISI_ID = 10277;
const auto EURASIA_ID = 10001;
const auto EARTH_ID = 10000;
const std::vector<maps::geoinfo::RegionId> MOSCOW_IDS{MOSCOW_ID, MOSCOW_REGION_ID, RUSSIA_ID, EARTH_ID};
const std::vector<maps::geoinfo::RegionId> MOSCOW_FULL_IDS{MOSCOW_ID, MOSCOW_REGION_ID, CENTRAL_DISTRICT_ID, RUSSIA_ID, EURASIA_ID, EARTH_ID};


using maps::geoinfo::RegionType;


struct GeoInfoFixture: public ::NUnitTest::TBaseFixture {
    GeoInfoFixture(): geoInfo(GEOID_PATH, GEODATA_PATH) {}

    maps::geoinfo::GeoInfo geoInfo;
};


Y_UNIT_TEST_SUITE_F(GeoInfoTest, GeoInfoFixture) {
    Y_UNIT_TEST(RegionAtTest) {
        const auto moscow = geoInfo.regionIdAt(yandex);
        EXPECT_TRUE(moscow);
        EXPECT_EQ(*moscow, MOSCOW_ID);

        const auto russia = geoInfo.regionIdAt(yandex, RegionType::COUNTRY);
        EXPECT_TRUE(russia);
        EXPECT_EQ(*russia, RUSSIA_ID);

        const auto regions = geoInfo.regionIdsAt(yandex);
        EXPECT_EQ(regions, MOSCOW_IDS);
    }

    Y_UNIT_TEST(RegionTest) {
        const auto russia = geoInfo.regionAt(yandex, RegionType::COUNTRY, true);
        EXPECT_TRUE(russia);
        EXPECT_EQ(russia->GetEnName(), "Russia");
    }

    Y_UNIT_TEST(ParentsTest) {
        const auto regions = geoInfo.parentsIds(MOSCOW_ID);
        EXPECT_EQ(regions, MOSCOW_FULL_IDS);
    }

    Y_UNIT_TEST(ChildrenTest) {
        const auto children = geoInfo.childrenIds(MOSCOW_REGION_ID);
        EXPECT_TRUE(children.size() > 0);
        for (const auto r: children) {
            const auto p = geoInfo.regionById(r).GetParentId();
            EXPECT_EQ(p, MOSCOW_REGION_ID);
        }
    }

    Y_UNIT_TEST(TurkeyTest) {
        const auto istanbul = geoInfo.regionIdAt(kadikoy, RegionType::CITY);
        EXPECT_TRUE(istanbul);
        EXPECT_EQ(*istanbul, ISTANBUL_ID);

        const auto turkey = geoInfo.regionIdAt(kadikoy, RegionType::COUNTRY);
        EXPECT_TRUE(turkey);
        EXPECT_EQ(*turkey, TURKEY_ID);
    }

    Y_UNIT_TEST(GeorgiaTest) {
        const auto capital = geoInfo.regionIdAt(tbilisi, RegionType::CITY);
        EXPECT_TRUE(capital);
        EXPECT_EQ(*capital, TBILISI_ID);
    }

    Y_UNIT_TEST(Ctor) {
        const auto capital = geoInfo.regionAt(tbilisi, RegionType::CITY);
        const maps::geoinfo::Region r = *capital;
        const auto cp = r;
        EXPECT_TRUE(cp.GetId() == TBILISI_ID);
    }
}
