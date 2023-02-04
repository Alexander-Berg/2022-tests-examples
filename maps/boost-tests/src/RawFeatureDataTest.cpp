#include "../include/RawFeatureDataTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"

#include "core/feature.h"

#include <yandex/maps/renderer5/core/IRawFeatureDataAccessor.h>
#include <yandex/maps/renderer5/core/FeatureRawData.h>

#include <boost/test/unit_test.hpp>

using namespace boost::unit_test;
using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::test;

namespace {
const char* sourceFileName = "tests/manual-tests/data/roads.MIF";
}

void map::rawFeatureDataFromFileTest()
{
    const size_t idColumnIndex = 1;
    const size_t objCodeColumnIndex = 2;

    core::IMapGuiPtr mapGui = createTestMapGui();

    mapGui->open(createProgressStub());

    auto ls = mapGui->addLayerFromSource(
        createProgressStub(),
        io::path::absolute(sourceFileName),
        0);

    auto geometryLayer = *ls.begin();
    auto textLayer = mapGui->annotateGeometryLayer(0, 0, geometryLayer->id());

    auto rawFDAccessor = geometryLayer->cast<core::IRawFeatureDataAccessor>();
    BOOST_CHECK(rawFDAccessor);

    BOOST_CHECK(!textLayer->has(core::LayerCapabilities::LayerRawFeatureDataAccessor));


    BOOST_CHECK(rawFDAccessor->rowCount() == 20);

    core::ColumnDefinitions columnDefinitions =
        rawFDAccessor->columnDefinitions();

    BOOST_CHECK(columnDefinitions.size() == 10);

    {
        auto fi = rawFDAccessor->getFeaturesData(3, 5);

        fi->reset();

        std::list<unsigned int> ids;
        std::list<unsigned int> objCods;
        ids.push_back(833); objCods.push_back(831);
        ids.push_back(834); objCods.push_back(832);
        ids.push_back(836); objCods.push_back(834);
        ids.push_back(842); objCods.push_back(840);
        ids.push_back(844); objCods.push_back(842);

        std::list<unsigned int>::const_iterator idIt = ids.begin();
        std::list<unsigned int>::const_iterator objCodsIt = objCods.begin();
        for (; idIt != ids.end(); ++idIt, ++objCodsIt)
        {
            BOOST_CHECK(fi->hasNext());

            renderer::feature::Feature& feature = fi->next();
            BOOST_CHECK_NO_THROW(feature.rawData());

            core::FeatureRawData& rawData = feature.rawData();
            BOOST_CHECK(rawData.record().size() == 10);

            {
                core::Variant v = rawData.record()[idColumnIndex];
                BOOST_REQUIRE(v.type() == core::Variant::LongLong);

                BOOST_CHECK(v.get<int64_t>() == *idIt);
            }

            {
                core::Variant v = rawData.record()[objCodeColumnIndex];
                BOOST_REQUIRE(v.type() == core::Variant::LongLong);

                BOOST_CHECK(v.get<int64_t>() == *objCodsIt);
            }
        }
        BOOST_CHECK(!fi->hasNext());
    }

    {
        // column name = "UID"
        //
        rawFDAccessor->setSortingOrder(6, core::IRawFeatureDataAccessor::DescendOrder);

        auto fi = rawFDAccessor->getFeaturesData(4, 5);

        fi->reset();

        std::list<unsigned int> ids;
        std::list<unsigned int> objCods;
        ids.push_back(1559); objCods.push_back(1441);
        ids.push_back(1558); objCods.push_back(1440);
        ids.push_back(1557); objCods.push_back(1439);
        ids.push_back(1556); objCods.push_back(9999051);
        ids.push_back(1555); objCods.push_back(1438);

        std::list<unsigned int>::const_iterator idIt = ids.begin();
        std::list<unsigned int>::const_iterator objCodsIt = objCods.begin();
        for (; idIt != ids.end(); ++idIt, ++objCodsIt)
        {
            BOOST_CHECK(fi->hasNext());

            renderer::feature::Feature& feature = fi->next();
            BOOST_CHECK_NO_THROW(feature.rawData());

            core::FeatureRawData& rawData = feature.rawData();
            BOOST_CHECK(rawData.record().size() == 10);

            {
                core::Variant v = rawData.record()[idColumnIndex];
                BOOST_REQUIRE(v.type() == core::Variant::LongLong);

                BOOST_CHECK(v.getNumeric<int32_t>() == *idIt);
            }

            {
                core::Variant v = rawData.record()[objCodeColumnIndex];
                BOOST_REQUIRE(v.type() == core::Variant::LongLong);

                BOOST_CHECK(v.getNumeric<int32_t>() == *objCodsIt);
            }
        }
        BOOST_CHECK(!fi->hasNext());
    }

    mapGui.reset();

    map::deleteFilesFromTmpDir();
}
