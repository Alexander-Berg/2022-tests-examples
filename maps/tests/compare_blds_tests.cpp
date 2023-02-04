#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/building.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/compare_blds.h>

#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

Y_UNIT_TEST_SUITE(compare_blds_tests)
{

Y_UNIT_TEST(basic_test)
{
    NYT::IClientPtr ytClient = NYT::NTesting::CreateTestClient();

    NYT::TTempTable gtTable(ytClient);
    NYT::TTempTable testTable(ytClient);
    const double iou = 0.98;
    // bld 1 without changes
    Building gtBld1 = Building::fromGeodeticGeom(
        geolib3::Polygon2({{0., 0.}, {1., 0.}, {1., 1.}}));
    gtBld1.setId(1);
    gtBld1.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);
    gtBld1.setHeight(3);
    // bld 2 is deleted
    Building gtBld2 = Building::fromGeodeticGeom(
        geolib3::Polygon2({{0., 1.}, {1., 1.}, {1., 2.}}));
    gtBld2.setId(2);
    gtBld2.setFTTypeId(FTTypeId::URBAN_INDUSTRIAL);
    gtBld2.setHeight(6);
    // bld 3 change geom
    Building gtBld3 = Building::fromGeodeticGeom(
        geolib3::Polygon2({{0., 2.}, {1., 2.}, {1., 3.}}));
    gtBld3.setId(3);
    gtBld3.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);
    gtBld3.setHeight(9);
    Building testBld3 = Building::fromGeodeticGeom(
        geolib3::Polygon2({{0., 3.}, {1., 3.}, {1., 4.}}));
    testBld3.setId(3);
    testBld3.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);
    testBld3.setHeight(9);
    // bld 4 change ft_type_id and height
    Building gtBld4 = Building::fromGeodeticGeom(
        geolib3::Polygon2({{0., 4.}, {1., 4.}, {1., 5.}}));
    gtBld4.setId(4);
    gtBld4.setFTTypeId(FTTypeId::URBAN_INDUSTRIAL);
    gtBld4.setHeight(6);
    Building testBld4 = Building::fromGeodeticGeom(
        geolib3::Polygon2({{0., 4.}, {1., 4.}, {1., 5.}}));
    testBld4.setId(4);
    testBld4.setFTTypeId(FTTypeId::URBAN_RESIDENTIAL);
    testBld4.setHeight(9);

    NYT::TTableWriterPtr<NYT::TNode> gtWriter
        = ytClient->CreateTableWriter<NYT::TNode>(gtTable.Name());
    gtWriter->AddRow(gtBld1.toYTNode());
    gtWriter->AddRow(gtBld2.toYTNode());
    gtWriter->AddRow(gtBld3.toYTNode());
    gtWriter->AddRow(gtBld4.toYTNode());
    gtWriter->Finish();

    NYT::TTableWriterPtr<NYT::TNode> testWriter
        = ytClient->CreateTableWriter<NYT::TNode>(testTable.Name());
    testWriter->AddRow(gtBld1.toYTNode());
    testWriter->AddRow(testBld3.toYTNode());
    testWriter->AddRow(testBld4.toYTNode());
    testWriter->Finish();

    ytClient->Sort(
        NYT::TSortOperationSpec()
            .AddInput(gtTable.Name())
            .Output(gtTable.Name())
            .SortBy({Building::BLD_ID})
    );

    ytClient->Sort(
        NYT::TSortOperationSpec()
            .AddInput(testTable.Name())
            .Output(testTable.Name())
            .SortBy({Building::BLD_ID})
    );

    NYT::ITransactionPtr txn = ytClient->StartTransaction();

    std::vector<Change> changes = compareBuildings(
        txn,
        gtTable.Name(), testTable.Name(),
        iou
    );

    txn->Commit();

    EXPECT_EQ(changes.size(), 3u);

    auto it1 = std::find_if(
        changes.begin(), changes.end(),
        [&](const Change& change) {
            return change.bldId == static_cast<uint64_t>(gtBld1.getId());
        }
    );
    EXPECT_TRUE(it1 == changes.end());

    auto it2 = std::find_if(
        changes.begin(), changes.end(),
        [&](const Change& change) {
            return change.bldId == static_cast<uint64_t>(gtBld2.getId());
        }
    );
    EXPECT_TRUE(it2 != changes.end());
    EXPECT_EQ(it2->isDeleted, true);

    auto it3 = std::find_if(
        changes.begin(), changes.end(),
        [&](const Change& change) {
            return change.bldId == static_cast<uint64_t>(gtBld3.getId());
        }
    );
    EXPECT_TRUE(it3 != changes.end());
    EXPECT_EQ(it3->isDeleted, false);
    EXPECT_EQ(it3->isGeomChanged, true);
    EXPECT_TRUE(!it3->ftTypeIdChange);
    EXPECT_TRUE(!it3->heightChange);

    auto it4 = std::find_if(
        changes.begin(), changes.end(),
        [&](const Change& change) {
            return change.bldId == static_cast<uint64_t>(gtBld4.getId());
        }
    );
    EXPECT_TRUE(it4 != changes.end());
    EXPECT_EQ(it4->isDeleted, false);
    EXPECT_EQ(it4->isGeomChanged, false);
    EXPECT_TRUE(it4->ftTypeIdChange);
    EXPECT_EQ(it4->ftTypeIdChange->first, gtBld4.getFTTypeId());
    EXPECT_EQ(it4->ftTypeIdChange->second, testBld4.getFTTypeId());
    EXPECT_TRUE(it4->heightChange);
    EXPECT_EQ(it4->heightChange->first, gtBld4.getHeight());
    EXPECT_EQ(it4->heightChange->second, testBld4.getHeight());
}

} // Y_UNIT_TEST_SUITE(compare_blds_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
