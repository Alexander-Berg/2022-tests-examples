#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/const.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/road.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/dwellplace.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/cover_by_cells.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(cover_by_cells_tests)
{

Y_UNIT_TEST(cover_buildings_by_cells_test)
{
    const double cellSizeInMercator = 1000.;
    const double minCoord = geolib3::MERCATOR_MIN;

    const std::vector<Building> blds{
        Building::fromMercatorGeom(
            geolib3::Polygon2({{minCoord + 1., minCoord + 1.},
                               {minCoord + 2., minCoord + 1.},
                               {minCoord + 2., minCoord + 2.},
                               {minCoord + 1., minCoord + 2.}})),
        Building::fromMercatorGeom(
            geolib3::Polygon2({{minCoord + 1. + cellSizeInMercator, minCoord + 1.},
                               {minCoord + 2. + cellSizeInMercator, minCoord + 1.},
                               {minCoord + 2. + cellSizeInMercator, minCoord + 2.},
                               {minCoord + 1. + cellSizeInMercator, minCoord + 2.}})),
        Building::fromMercatorGeom(
            geolib3::Polygon2({{minCoord + 1., minCoord + 1. + cellSizeInMercator},
                               {minCoord + 2., minCoord + 1. + cellSizeInMercator},
                               {minCoord + 2., minCoord + 2. + cellSizeInMercator},
                               {minCoord + 1., minCoord + 2. + cellSizeInMercator}}))
    };
    const std::vector<Cell> expectedCells{{0, 0}, {1, 0}, {0, 1}};

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Building& bld : blds) {
        writer->AddRow(bld.toYTNode());
    }
    writer->Finish();

    DirectlyCellsCover cover(cellSizeInMercator);

    NYT::ITransactionPtr txn = client->StartTransaction();

    coverObjectsByCells<Building>(
        txn,
        {inputYTTable.Name()},
        cover,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<Building> testBlds;
    std::vector<Cell> testCells;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testBlds.emplace_back(Building::fromYTNode(row));
        testCells.emplace_back(Cell::fromYTNode(row));
    }

    EXPECT_EQ(expectedCells.size(), testCells.size());
    for (size_t i = 0; i < testBlds.size(); i++) {
        bool isFound = false;
        for (size_t j = 0; j < blds.size(); j++) {
             if (geolib3::test_tools::approximateEqual(
                     testBlds[i].toMercatorGeom(),
                     blds[j].toMercatorGeom(),
                     geolib3::EPS)) {
                  EXPECT_EQ(expectedCells[j].x, testCells[i].x);
                  EXPECT_EQ(expectedCells[j].y, testCells[i].y);
                  isFound = true;
                  break;
             }
        }
        EXPECT_TRUE(isFound);
    }
}

Y_UNIT_TEST(cover_dwellplaces_by_adjacent_cells_test)
{
    const double cellSizeMercator = 5.;
    const double adjacentDistanceMercator = 2.;
    const double minCoord = geolib3::MERCATOR_MIN;
    const int64_t gridSize
        = (geolib3::MERCATOR_MAX - geolib3::MERCATOR_MIN) / cellSizeMercator;
    const std::vector<Dwellplace> places{
        Dwellplace::fromMercatorGeom(
            geolib3::Point2{minCoord + 1., minCoord + 1.}),
        Dwellplace::fromMercatorGeom(
            geolib3::Point2{minCoord + 1. + cellSizeMercator, minCoord + 1.}),
        Dwellplace::fromMercatorGeom(
            geolib3::Point2{minCoord + 1., minCoord + 1. + cellSizeMercator}),
    };

    const std::vector<std::pair<Dwellplace, std::set<Cell>>> expectedCells{
        {places[0], {{gridSize - 1, 0},            {0, 0},
                     {gridSize - 1, gridSize - 1}, {0, gridSize - 1}}},
        {places[1], {{0, 0},            {1, 0},
                     {0, gridSize - 1}, {1, gridSize - 1}}},
        {places[2], {{gridSize - 1, 1}, {0, 1},
                     {gridSize - 1, 0}, {0, 0}}}
    };

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    NYT::TTempTable inputYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto writer = client->CreateTableWriter<NYT::TNode>(inputYTTable.Name());
    for (const Dwellplace& place : places) {
        writer->AddRow(place.toYTNode());
    }
    writer->Finish();

    AdjacentCellsCover cover(cellSizeMercator, adjacentDistanceMercator);

    NYT::ITransactionPtr txn = client->StartTransaction();

    coverObjectsByCells<Dwellplace>(
        txn,
        {inputYTTable.Name()},
        cover,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<std::pair<Dwellplace, std::set<Cell>>> testCells;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        Dwellplace place = Dwellplace::fromYTNode(row);
        Cell cell = Cell::fromYTNode(row);

        bool isInserted = false;
        for (size_t i = 0; i < testCells.size(); i++) {
            if (geolib3::test_tools::approximateEqual(
                    testCells[i].first.toMercatorGeom(),
                    place.toMercatorGeom(),
                    geolib3::EPS))
            {
                testCells[i].second.insert(cell);
                isInserted = true;
            }
        }
        if (!isInserted) {
            testCells.emplace_back(place, std::set<Cell>{cell});
        }
    }

    EXPECT_EQ(expectedCells.size(), testCells.size());
    for (size_t i = 0; i < expectedCells.size(); i++) {
        bool isFound = false;
        for (size_t j = 0; j < testCells.size(); j++) {
            if (geolib3::test_tools::approximateEqual(
                    expectedCells[i].first.toMercatorGeom(),
                    testCells[j].first.toMercatorGeom(),
                    geolib3::EPS))
            {
                EXPECT_EQ(expectedCells[i].second.size(), testCells[j].second.size());
                for (const Cell& expectedCell : expectedCells[i].second) {
                    EXPECT_TRUE(testCells[j].second.find(expectedCell)
                                    != testCells[j].second.end());
                }
                isFound = true;
                break;
            }
        }
        EXPECT_TRUE(isFound);
    }
}

Y_UNIT_TEST(cell_to_bbox_test)
{
    const Cell cell(1, 2);
    const double cellSizeInMercator = 100.;

    const geolib3::BoundingBox expectedMercBBox(
        geolib3::Point2(geolib3::MERCATOR_MIN + cell.x * cellSizeInMercator,
                        geolib3::MERCATOR_MIN + cell.y * cellSizeInMercator),
        geolib3::Point2(geolib3::MERCATOR_MIN + (cell.x + 1) * cellSizeInMercator,
                        geolib3::MERCATOR_MIN + (cell.y + 1) * cellSizeInMercator)
    );

    EXPECT_TRUE(geolib3::test_tools::approximateEqual(
                    cell.toBBox(cellSizeInMercator).toMercatorGeom(),
                    expectedMercBBox, geolib3::EPS));
}

Y_UNIT_TEST(cell_to_node_conversion_test)
{
    const Cell cell(1, 2);
    EXPECT_TRUE(cell == Cell::fromYTNode(cell.toYTNode()));
}

} // Y_UNIT_TEST_SUITE(cover_by_cells_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
