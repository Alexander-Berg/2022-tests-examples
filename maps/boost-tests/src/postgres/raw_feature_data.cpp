#include "tests/boost-tests/include/tools/map_tools.h"
#include "../../include/contexts.hpp"
#include "../../include/TestConfirmationProvider.h"

#include "core/feature.h"

#include <yandex/maps/renderer5/core/IRawFeatureDataAccessor.h>
#include <yandex/maps/renderer5/core/FeatureRawData.h>

#include <boost/test/unit_test.hpp>

using namespace maps::renderer5::test::map;

namespace maps {
namespace renderer5 {
namespace postgres {

namespace {
struct TestPostgresRecord
{
    TestPostgresRecord(
        unsigned int id,
        unsigned int curRevId,
        const std::wstring& screenLabel,
        const std::wstring& renderLabel):
            id(id), curRevId(curRevId),
            screenLabel(screenLabel),
            renderLabel(renderLabel)
    {}

    unsigned int id;
    unsigned int curRevId;
    std::wstring screenLabel;
    std::wstring renderLabel;
};
typedef std::vector<TestPostgresRecord> TestRecordCollection;

const size_t idColumnIndex = 0;
const size_t curRevIdColumnIndex = 6;
const size_t screenLabelColumnIndex = 2;
const size_t renderLabelColumnIndex = 3;

void validateData(
    renderer::feature::FeatureIterUPtr fi,
    const TestRecordCollection& records)
{
    fi->reset();
    for (const TestPostgresRecord& r : records)
    {
        BOOST_REQUIRE(fi->hasNext());

        renderer::feature::Feature& feature = fi->next();
        BOOST_CHECK_NO_THROW(feature.rawData());

        core::FeatureRawData& rawData = feature.rawData();
        BOOST_CHECK(rawData.record().size() == 20);

        {
            core::Variant v = rawData.record()[idColumnIndex];
            BOOST_CHECK(v.type() == core::Variant::Long);
            BOOST_CHECK(v.get<int32_t>() == r.id);
        }

        {
            core::Variant v = rawData.record()[curRevIdColumnIndex];
            BOOST_CHECK(v.type() == core::Variant::Long);
            BOOST_CHECK(v.get<int32_t>() == r.curRevId);
        }

        {
            core::Variant v = rawData.record()[screenLabelColumnIndex];
            BOOST_CHECK(v.type() == core::Variant::WString);
            std::wstring ws = v.get<std::wstring>();
            BOOST_CHECK(ws == r.screenLabel);
        }

        {
            core::Variant v = rawData.record()[renderLabelColumnIndex];
            BOOST_CHECK(v.type() == core::Variant::WString);
            std::wstring ws = v.get<std::wstring>();
            BOOST_CHECK(ws == r.renderLabel);
        }
    }
    BOOST_REQUIRE(!fi->hasNext());
}

}

BOOST_AUTO_TEST_SUITE( postgres )

BOOST_FIXTURE_TEST_CASE( rawFeatureData, TransactionProviderContext<> )
{
    TestConfirmationProviderPtr confProv(new TestConfirmationProvider());

    core::IMapGuiPtr mapGui = createTestMapGui();
    mapGui->setExternalConfirmationsProvider(confProv);
    mapGui->setPostgresTransactionProvider(provider);

    mapGui->open(createProgressStub());

    confProv->addPostgresLayerConf->featureType = core::FeatureType::Polyline;
    confProv->addPostgresLayerConf->tableName = L"wiki_streets";
    confProv->addPostgresLayerConf->setIdColumnName(L"id");

    auto geometryLayer = mapGui->addLayerFromPostgres(createProgressStub(), 0);

    // can not create test layer from postgres layer
    // TBD: load from xml file, when DirectDatabaseTextLayer is exist
    //
    //auto textLayer = mapGui->annotateGeometryLayer(0, 0, geometryLayer->id());

    auto rawFDAccessor = geometryLayer->cast<core::IRawFeatureDataAccessor>();
    BOOST_CHECK(rawFDAccessor);
    BOOST_CHECK(rawFDAccessor->rowCount() == 7);

    core::ColumnDefinitions columnDefinitions =
        rawFDAccessor->columnDefinitions();

    BOOST_CHECK(columnDefinitions.size() == 20);

    TestRecordCollection baseRecords;
    baseRecords.push_back(TestPostgresRecord(
        3771057, 2846651,
        L"улица Марии Поливановой", L"улица Марии Поливановой"));
    baseRecords.push_back(TestPostgresRecord(
        3800139, 2846734,
        L"Б&amp; Очаковская", L"Б&amp; Очаковская"));

    {
        auto records = baseRecords;
        records.push_back(TestPostgresRecord(
            3800142, 2846723,
            L"Б&amp; Очаковская", L"Б&amp; Очаковская"));

        BOOST_CHECK_NO_THROW(validateData(rawFDAccessor->getFeaturesData(1, 3), records));
    }

    {
        rawFDAccessor->setSortingOrder(0, core::IRawFeatureDataAccessor::AscendOrder);

        auto records = baseRecords;
        records.push_back(TestPostgresRecord(
            3800140, 2796790,
            L"Б&amp; Очаковская", L""));

        BOOST_CHECK_NO_THROW(validateData(rawFDAccessor->getFeaturesData(2, 3), records));
    }
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace postgres
} // namespace renderer5
} // namespace maps
