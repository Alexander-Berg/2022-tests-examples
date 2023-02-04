#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart//libs/geometry/include/hex_wkb.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/area.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/road.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/dwellplace.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/filter_by_regions.h>

#include <mapreduce/yt/util/temp_table.h>

#include <iostream>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(filter_by_regions_tests)
{

Y_UNIT_TEST(filter_areas_by_regions_test)
{
    const geolib3::MultiPolygon2 regions({
        geolib3::Polygon2({{0., 0.}, {5., 0.}, {5., 5.}, {0., 5.}}),
        geolib3::Polygon2({{10., 0.}, {15., 0.}, {15., 5.}, {10., 5.}})
    });
    const std::vector<Area> areas{
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({{1., 1.}, {2., 1.}, {2., 2.}, {1., 2.}})
            })
        ),
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({{2., 2.}, {3., 2.}, {3., 3.}, {2., 3.}})
            })
        ),
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({{6., 0.}, {7., 0.}, {7., 1.}, {6., 1.}})
            })
        ),
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({{11., 1.}, {12., 1.}, {12., 2.}, {11., 2.}})
            })
        )
    };
    const std::vector<Area> expectedAreas{areas[0], areas[1], areas[3]};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Area& area : areas) {
        writer->AddRow(area.toYTNode());
    }
    writer->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    filterByRegions<Area>(
        txn,
        {inputYTTable.Name()},
        regions,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Area> testAreas;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testAreas.emplace_back(Area::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testAreas.begin(), testAreas.end(),
            expectedAreas.begin()
        )
    );
}

Y_UNIT_TEST(filter_buildings_by_regions_test)
{
    const geolib3::MultiPolygon2 regions({
        geolib3::Polygon2({{0., 0.}, {5., 0.}, {5., 5.}, {0., 5.}}),
        geolib3::Polygon2({{10., 0.}, {15., 0.}, {15., 5.}, {10., 5.}})
    });
    const std::vector<Building> blds{
        Building::fromMercatorGeom(
            geolib3::Polygon2({{1., 1.}, {2., 1.}, {2., 2.}, {1., 2.}})),
        Building::fromMercatorGeom(
            geolib3::Polygon2({{2., 2.}, {3., 2.}, {3., 3.}, {2., 3.}})),
        Building::fromMercatorGeom(
            geolib3::Polygon2({{6., 0.}, {7., 0.}, {7., 1.}, {6., 1.}})),
        Building::fromMercatorGeom(
            geolib3::Polygon2({{11., 1.}, {12., 1.}, {12., 2.}, {11., 2.}}))
    };
    const std::vector<Building> expectedBlds{blds[0], blds[1], blds[3]};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Building& bld : blds) {
        writer->AddRow(bld.toYTNode());
    }
    writer->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    filterByRegions<Building>(
        txn,
        {inputYTTable.Name()},
        regions,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Building> testBlds;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testBlds.emplace_back(Building::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testBlds.begin(), testBlds.end(),
            expectedBlds.begin()
        )
    );
}

Y_UNIT_TEST(filter_roads_by_regions_test)
{
    const geolib3::MultiPolygon2 regions({
        geolib3::Polygon2({{0., 0.}, {5., 0.}, {5., 5.}, {0., 5.}}),
        geolib3::Polygon2({{10., 0.}, {15., 0.}, {15., 5.}, {10., 5.}})
    });
    std::vector<Road> roads{
        Road::fromMercatorGeom(
            geolib3::Polyline2({{1., 1.}, {2., 1.}, {2., 2.}, {1., 2.}})),
        Road::fromMercatorGeom(
            geolib3::Polyline2({{2., 2.}, {3., 2.}, {3., 3.}, {2., 3.}})),
        Road::fromMercatorGeom(
            geolib3::Polyline2({{6., 0.}, {7., 0.}, {7., 1.}, {6., 1.}})),
        Road::fromMercatorGeom(
            geolib3::Polyline2({{11., 1.}, {12., 1.}, {12., 2.}, {11., 2.}}))
    };
    roads[0].setFc(1);
    roads[0].setFow(2);
    roads[1].setFc(3);
    roads[1].setFow(4);
    roads[2].setFc(5);
    roads[2].setFow(6);
    roads[3].setFc(7);
    roads[3].setFow(8);
    const std::vector<Road> expectedRoads{roads[0], roads[1], roads[3]};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Road& road : roads) {
        writer->AddRow(road.toYTNode());
    }
    writer->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    filterByRegions<Road>(
        txn,
        {inputYTTable.Name()},
        regions,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Road> testRoads;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testRoads.emplace_back(Road::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testRoads.begin(), testRoads.end(),
            expectedRoads.begin()
        )
    );
}

Y_UNIT_TEST(filter_dwellplaces_by_regions_test)
{
    const geolib3::MultiPolygon2 regions({
        geolib3::Polygon2({{0., 0.}, {5., 0.}, {5., 5.}, {0., 5.}}),
        geolib3::Polygon2({{10., 0.}, {15., 0.}, {15., 5.}, {10., 5.}})
    });
    const std::vector<Dwellplace> places{
        Dwellplace::fromMercatorGeom(geolib3::Point2(1., 1.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(2., 2.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(6., 0.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(11., 1.))
    };
    const std::vector<Dwellplace> expectedPlaces{places[0], places[1], places[3]};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Dwellplace& place : places) {
        writer->AddRow(place.toYTNode());
    }
    writer->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    filterByRegions<Dwellplace>(
        txn,
        {inputYTTable.Name()},
        regions,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Dwellplace> testPlaces;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testPlaces.emplace_back(Dwellplace::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testPlaces.begin(), testPlaces.end(),
            expectedPlaces.begin()
        )
    );
}

} // Y_UNIT_TEST_SUITE(filter_by_regions_tests)

Y_UNIT_TEST_SUITE(extract_by_regions_tests)
{

Y_UNIT_TEST(extract_dwellplaces_by_regions_test)
{
    const geolib3::MultiPolygon2 regions({
        geolib3::Polygon2({{2., 2.}, {4., 2.}, {4., 4.}, {2., 4.}})
    });
    const std::vector<Dwellplace> places{
        Dwellplace::fromMercatorGeom(geolib3::Point2(0., 0.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(2.5, 2.5)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(3., 3.)),
        Dwellplace::fromMercatorGeom(geolib3::Point2(5., 5.))
    };
    const std::vector<Dwellplace> expectedInPlaces{places[1], places[2]};
    const std::vector<Dwellplace> expectedOutPlaces{places[0], places[3]};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Dwellplace& place : places) {
        writer->AddRow(place.toYTNode());
    }
    writer->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    extractByRegions<Dwellplace>(
        txn,
        {inputYTTable.Name()},
        regions,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Dwellplace> testInPlaces;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testInPlaces.emplace_back(Dwellplace::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testInPlaces.begin(), testInPlaces.end(),
            expectedInPlaces.begin()
        )
    );

    std::vector<Dwellplace> testOutPlaces;
    reader = client->CreateTableReader<NYT::TNode>(inputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testOutPlaces.emplace_back(Dwellplace::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testOutPlaces.begin(), testOutPlaces.end(),
            expectedOutPlaces.begin()
        )
    );
}

} // Y_UNIT_TEST_SUITE(filter_by_regions_tests)


Y_UNIT_TEST_SUITE(fail_geodetic_to_mercator)
{
Y_UNIT_TEST(base_test)
{
    // 3307276025, cis2
    const std::string hexWKB = "0103000020E6100000020000000F000000CFFA06B91AF7"
        "4940EDB69E30C78C47404A42D6DE1DF7494057487F83C48C4740D3EFA48012F74940F"
        "2CD739ABE8C47408CF337A110F74940E50B5A48C08C474065FCFB8C0BF7494054E3A5"
        "9BC48C4740A98AB36607F7494091F74C24C88C4740E6CCBD5A0AF749404BAE20D5C98"
        "C4740E2C7009F0BF7494093907DCDC88C474084D60F260FF7494009806BD3CA8C4740"
        "488818CD0FF7494061ED0E7ACD8C4740D852C6130FF74940644F4C28CE8C47409D2AD"
        "F3312F749407FFB3A70CE8C4740A551957714F74940EF8DAFAACF8C47406EC811FB1C"
        "F74940E444A26DC88C4740CFFA06B91AF74940EDB69E30C78C47400D000000DF04B6B"
        "50BF74940A7A7C2B1C48C4740B341B4C310F74940D35D495AC08C4740E50AEF7211F7"
        "49407A5567B5C08C47408CBFED0912F7494044149337C08C4740ACC5A70018F74940A"
        "8716F7EC38C4740A6D425E318F74940F0517FBDC28C474044300E2E1DF7494089247A"
        "19C58C47407790627612F74940C60EA437CE8C4740DFE17B2112F7494046F71B09CE8"
        "C4740D39FFD4811F74940B70C384BC98C4740172B6A300DF749408EAD6708C78C4740"
        "E2E995B20CF74940CB13083BC58C4740DF04B6B50BF74940A7A7C2B1C48C4740";

    geolib3::Polygon2 geoGeom = hexWKBToPolygon(hexWKB);

    EXPECT_THROW(geolib3::convertGeodeticToMercator(geoGeom), std::exception);
}

} // Y_UNIT_TEST_SUITE(fail_geodetic_to_mercator)


} // namespace test

} // namespace maps::wiki::autocart::pipeline
