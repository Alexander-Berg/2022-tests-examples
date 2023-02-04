#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/const.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/objects/include/dwellplace.h>
#include <maps/wikimap/mapspro/services/autocart/pipeline/libs/detection/include/extract_unique_dwellplaces.h>

#include <maps/libs/tile/include/const.h>

#include <util/system/env.h>

#include <mapreduce/yt/util/ypath_join.h>
#include <mapreduce/yt/util/temp_table.h>

using namespace testing;

namespace maps::wiki::autocart::pipeline {

namespace tests {

namespace {

void writeDwellplaces(
    NYT::IClientPtr txn,
    const TString& ytPath,
    const std::vector<Dwellplace>& places)
{
    NYT::TTableWriterPtr<NYT::TNode> writer
        = txn->CreateTableWriter<NYT::TNode>(ytPath);
    for (const Dwellplace& place : places) {
        writer->AddRow(place.toYTNode());
    }
    writer->Finish();
}

} // namespace

Y_UNIT_TEST_SUITE(update_dwellplaces_tests)
{

Y_UNIT_TEST(basic_test)
{
    NYT::IClientPtr client = NYT::NTesting::CreateTestClient();

    const TString dwellplacesYTFolderPath = "//home/dwellplaces";
    client->Create(
        dwellplacesYTFolderPath,
        NYT::NT_MAP,
        NYT::TCreateOptions().Recursive(true)
    );

    const TString table1Name = "2019-10-11";
    const std::vector<Dwellplace> places1{
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(geolib3::MERCATOR_MIN + 1.,
                            geolib3::MERCATOR_MIN + 1.)
        ),
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(geolib3::MERCATOR_MIN + 4.,
                            geolib3::MERCATOR_MIN + 5.)
        )
    };
    writeDwellplaces(
        client,
        NYT::JoinYPaths(dwellplacesYTFolderPath, table1Name),
        places1
    );

    const TString table2Name = "2019-11-11";
    const std::vector<Dwellplace> places2{
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(geolib3::MERCATOR_MIN + 4.,
                            geolib3::MERCATOR_MIN + 4.)
        ),
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(geolib3::MERCATOR_MIN + 2.,
                            geolib3::MERCATOR_MIN + 1.)
        )
    };
    writeDwellplaces(
        client,
        NYT::JoinYPaths(dwellplacesYTFolderPath, table2Name),
        places2
    );

    const double distanceInMercator = 1.5;

    const std::vector<Dwellplace> expectedPlaces{
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(geolib3::MERCATOR_MIN + distanceInMercator,
                            geolib3::MERCATOR_MIN + distanceInMercator)
        ),
        Dwellplace::fromMercatorGeom(
            geolib3::Point2(geolib3::MERCATOR_MIN + 3 * distanceInMercator,
                            geolib3::MERCATOR_MIN + 3 * distanceInMercator)
        )
    };

    NYT::ITransactionPtr txn = client->StartTransaction();

    NYT::TTempTable uniquePlacesTable(txn);

    extractUniqueDwellplaces(
        txn,
        dwellplacesYTFolderPath,
        uniquePlacesTable.Name(),
        distanceInMercator
    );

    std::vector<Dwellplace> testPlaces;
    NYT::TTableReaderPtr<NYT::TNode> reader
        = txn->CreateTableReader<NYT::TNode>(uniquePlacesTable.Name());
    for (; reader->IsValid(); reader->Next()) {
        const NYT::TNode& row = reader->GetRow();
        testPlaces.push_back(Dwellplace::fromYTNode(row));
    }

    EXPECT_TRUE(
        std::is_permutation(
            testPlaces.begin(), testPlaces.end(),
            expectedPlaces.begin()
        )
    );

    txn->Commit();
}

} // Y_UNIT_TEST_SUITE(update_dwellplaces_tests)

} // namespace test

} // namespace maps::wiki::autocart::pipeline
