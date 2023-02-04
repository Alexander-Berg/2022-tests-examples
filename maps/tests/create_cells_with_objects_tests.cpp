#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/const.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polygon.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/bbox.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/area.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/road.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/dwellplace.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/cover_by_cells.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/extract_unique_rows.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/create_cells_with_objects.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/state.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(merge_objects_by_cells_tests)
{

Y_UNIT_TEST(create_cells_by_dwellplaces_test)
{
    const double cellSizeInMercator = 20.;
    const double padSizeInMercator = 10.;
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
                               {minCoord + 1. + cellSizeInMercator, minCoord + 2.}}))
    };
    std::vector<Road> roads{
        Road::fromMercatorGeom(
            geolib3::Polyline2({{minCoord + 1., minCoord + 1.},
                                {minCoord + 2., minCoord + 1.},
                                {minCoord + 2., minCoord + 2.},
                                {minCoord + 1., minCoord + 2.}})),
        Road::fromMercatorGeom(
            geolib3::Polyline2({{minCoord + 1. + cellSizeInMercator, minCoord + 1.},
                                {minCoord + 2. + cellSizeInMercator, minCoord + 1.},
                                {minCoord + 2. + cellSizeInMercator, minCoord + 2.},
                                {minCoord + 1. + cellSizeInMercator, minCoord + 2.}}))
    };
    roads[0].setFc(3);
    roads[0].setFow(6);
    roads[1].setFc(5);
    roads[1].setFow(2);
    const std::vector<Area> areas{
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({
                    {minCoord + 1., minCoord + 1.},
                    {minCoord + 2., minCoord + 1.},
                    {minCoord + 2., minCoord + 2.},
                    {minCoord + 1., minCoord + 2.}
                })
            })
        ),
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({
                    {minCoord + 1. + cellSizeInMercator, minCoord + 1.},
                    {minCoord + 2. + cellSizeInMercator, minCoord + 1.},
                    {minCoord + 2. + cellSizeInMercator, minCoord + 2.},
                    {minCoord + 1. + cellSizeInMercator, minCoord + 2.}
                })
            })
        )
    };
    const std::vector<Dwellplace> places{
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(minCoord + 1., minCoord + 1.)),
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(minCoord + 2., minCoord + 1.))
    };
    const BBox bbox = BBox::fromMercatorGeom(
        geolib3::BoundingBox(
            geolib3::Point2(minCoord, minCoord),
            geolib3::Point2(minCoord + cellSizeInMercator,
                            minCoord + cellSizeInMercator)
        )
    );

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString STATE_PATH = "//home/state";
    State::initialize(client, STATE_PATH);

    NYT::TTempTable bldsYTTable(client);
    NYT::TTempTable roadsYTTable(client);
    NYT::TTempTable areasYTTable(client);
    NYT::TTempTable placesYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto bldWriter = client->CreateTableWriter<NYT::TNode>(bldsYTTable.Name());
    for (const Building& bld : blds) {
        bldWriter->AddRow(bld.toYTNode());
    }
    bldWriter->Finish();

    auto roadWriter = client->CreateTableWriter<NYT::TNode>(roadsYTTable.Name());
    for (const Road& road : roads) {
        roadWriter->AddRow(road.toYTNode());
    }
    roadWriter->Finish();

    auto areaWriter = client->CreateTableWriter<NYT::TNode>(areasYTTable.Name());
    for (const Area& area : areas) {
        areaWriter->AddRow(area.toYTNode());
    }
    areaWriter->Finish();

    auto placeWriter = client->CreateTableWriter<NYT::TNode>(placesYTTable.Name());
    for (const Dwellplace& place : places) {
        placeWriter->AddRow(place.toYTNode());
    }
    placeWriter->Finish();

    NYT::ITransactionPtr txn = client->StartTransaction();

    createCellsWithObjectsByDwellplaces(
        txn,
        bldsYTTable.Name(),
        roadsYTTable.Name(),
        areasYTTable.Name(),
        placesYTTable.Name(),
        cellSizeInMercator,
        padSizeInMercator,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<NYT::TNode> testNodes;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testNodes.push_back(row);
    }

    EXPECT_EQ(testNodes.size(), 1u);

    const NYT::TNode& testNode = testNodes[0];

    std::vector<Building> testBlds = bldsFromYTNode(testNode);
    std::vector<Road> testRoads = roadsFromYTNode(testNode);
    std::vector<Area> testAreas = areasFromYTNode(testNode);
    EXPECT_TRUE(std::is_permutation(blds.begin(), blds.end(), testBlds.begin()));
    EXPECT_TRUE(std::is_permutation(roads.begin(), roads.end(), testRoads.begin()));
    EXPECT_TRUE(std::is_permutation(areas.begin(), areas.end(), testAreas.begin()));
    EXPECT_TRUE(bbox == BBox::fromYTNode(testNode));

    State::remove(client);
}

Y_UNIT_TEST(create_cells_by_regions_test)
{
    const double cellSizeInMercator = 20.;
    const double padSizeInMercator = 10.;
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
                               {minCoord + 1. + cellSizeInMercator, minCoord + 2.}}))
    };
    std::vector<Road> roads{
        Road::fromMercatorGeom(
            geolib3::Polyline2({{minCoord + 1., minCoord + 1.},
                                {minCoord + 2., minCoord + 1.},
                                {minCoord + 2., minCoord + 2.},
                                {minCoord + 1., minCoord + 2.}})),
        Road::fromMercatorGeom(
            geolib3::Polyline2({{minCoord + 1. + cellSizeInMercator, minCoord + 1.},
                                {minCoord + 2. + cellSizeInMercator, minCoord + 1.},
                                {minCoord + 2. + cellSizeInMercator, minCoord + 2.},
                                {minCoord + 1. + cellSizeInMercator, minCoord + 2.}}))
    };
    roads[0].setFc(3);
    roads[0].setFow(6);
    roads[1].setFc(5);
    roads[1].setFow(2);
    const std::vector<Area> areas{
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({
                    {minCoord + 1., minCoord + 1.},
                    {minCoord + 2., minCoord + 1.},
                    {minCoord + 2., minCoord + 2.},
                    {minCoord + 1., minCoord + 2.}
                })
            })
        ),
        Area::fromMercatorGeom(
            geolib3::MultiPolygon2({
                geolib3::Polygon2({
                    {minCoord + 1. + cellSizeInMercator, minCoord + 1.},
                    {minCoord + 2. + cellSizeInMercator, minCoord + 1.},
                    {minCoord + 2. + cellSizeInMercator, minCoord + 2.},
                    {minCoord + 1. + cellSizeInMercator, minCoord + 2.}
                })
            })
        )
    };

    const BBox bbox = BBox::fromMercatorGeom(
        geolib3::BoundingBox(
            geolib3::Point2(minCoord, minCoord),
            geolib3::Point2(minCoord + cellSizeInMercator,
                            minCoord + cellSizeInMercator)
        )
    );

    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString STATE_PATH = "//home/state";
    State::initialize(client, STATE_PATH);

    NYT::TTempTable bldsYTTable(client);
    NYT::TTempTable roadsYTTable(client);
    NYT::TTempTable areasYTTable(client);
    NYT::TTempTable outputYTTable(client);

    auto bldWriter = client->CreateTableWriter<NYT::TNode>(bldsYTTable.Name());
    for (const Building& bld : blds) {
        bldWriter->AddRow(bld.toYTNode());
    }
    bldWriter->Finish();

    auto roadWriter = client->CreateTableWriter<NYT::TNode>(roadsYTTable.Name());
    for (const Road& road : roads) {
        roadWriter->AddRow(road.toYTNode());
    }
    roadWriter->Finish();

    auto areaWriter = client->CreateTableWriter<NYT::TNode>(areasYTTable.Name());
    for (const Area& area : areas) {
        areaWriter->AddRow(area.toYTNode());
    }
    areaWriter->Finish();

    const geolib3::MultiPolygon2 mercRegions(
        {
         geolib3::Polygon2({{minCoord + 0.5, minCoord + 0.5},
                            {minCoord + 5.,  minCoord + 0.5},
                            {minCoord + 5.,  minCoord + 5.},
                            {minCoord + 0.5, minCoord + 5.}})
        }
    );

    NYT::ITransactionPtr txn = client->StartTransaction();

    createCellsWithObjectsByRegions(
        txn,
        bldsYTTable.Name(),
        roadsYTTable.Name(),
        areasYTTable.Name(),
        mercRegions,
        cellSizeInMercator,
        padSizeInMercator,
        outputYTTable.Name()
    );

    txn->Commit();

    std::vector<NYT::TNode> testNodes;
    auto reader = client->CreateTableReader<NYT::TNode>(outputYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const auto& row = reader->GetRow();
        testNodes.push_back(row);
    }

    EXPECT_EQ(testNodes.size(), 1u);

    const NYT::TNode& testNode = testNodes[0];

    std::vector<Building> testBlds = bldsFromYTNode(testNode);
    std::vector<Road> testRoads = roadsFromYTNode(testNode);
    std::vector<Area> testAreas = areasFromYTNode(testNode);

    EXPECT_TRUE(std::is_permutation(blds.begin(), blds.end(), testBlds.begin()));
    EXPECT_TRUE(std::is_permutation(roads.begin(), roads.end(), testRoads.begin()));
    EXPECT_TRUE(std::is_permutation(areas.begin(), areas.end(), testAreas.begin()));
    EXPECT_TRUE(bbox == BBox::fromYTNode(testNode));

    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(merge_objects_by_cells_tests)

Y_UNIT_TEST_SUITE(extract_unqiue_cells_tests)
{

Y_UNIT_TEST(base_test)
{
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString STATE_PATH = "//home/state";
    State::initialize(client, STATE_PATH);

    const std::vector<Cell> gt{Cell(1, 0), Cell(1, 2)};

    NYT::TTempTable table1(client);
    NYT::TTempTable table2(client);

    NYT::TTableWriterPtr<NYT::TNode> writer1
        = client->CreateTableWriter<NYT::TNode>(table1.Name());
    writer1->AddRow(gt[0].toYTNode());
    writer1->AddRow(gt[1].toYTNode());
    writer1->Finish();

    NYT::TTableWriterPtr<NYT::TNode> writer2
        = client->CreateTableWriter<NYT::TNode>(table2.Name());
    writer2->AddRow(gt[1].toYTNode());
    writer2->AddRow(gt[0].toYTNode());
    writer2->Finish();

    NYT::TTempTable resultYTTable(client);

    NYT::ITransactionPtr txn = client->StartTransaction();

    extractUniqueRows(
        txn,
        {table1.Name(), table2.Name()},
        resultYTTable.Name(),
        {Cell::X, Cell::Y}
    );

    txn->Commit();

    std::vector<Cell> result;
    NYT::TTableReaderPtr<NYT::TNode> reader
        = client->CreateTableReader<NYT::TNode>(resultYTTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const NYT::TNode& row = reader->GetRow();
        result.push_back(Cell::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(gt.begin(), gt.end(), result.begin())
    );

    State::remove(client);
}

} // Y_UNIT_TEST_SUITE(extract_unique_cells_tests)


} // namespace test

} // namespace maps::wiki::autocart::pipeline
