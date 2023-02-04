#include "../include/MapFeaturesSelectionTest.h"
#include "tests/boost-tests/include/tools/map_tools.h"
#include "../include/TestConfirmationProvider.h"

#include "core/feature.h"

#include <maps/renderer/libs/image/include/image_storage.h>
#include <yandex/maps/renderer5/core/OperationProgress.h>

#include <agg_trans_affine.h>

using namespace boost::unit_test;
using namespace maps::renderer5;
using namespace maps::renderer5::test;
using namespace maps::renderer;
using namespace maps::renderer::base;

namespace {
    const char* mapXmlFileName = "tests/boost-tests/maps/MapFeaturesSelection.xml";

    struct FeatureInfo
    {
        FeatureInfo(
            unsigned int layerId,
            core::FeatureIdType featureId,
            core::FeatureIdType sourceFeatureId):
        layerId(layerId),
            featureId(featureId),
            sourceFeatureId(sourceFeatureId)
        {}

        bool operator== (const FeatureInfo& other) const
        {
            return layerId == other.layerId;
        }

        unsigned int layerId;
        core::FeatureIdType featureId;
        core::FeatureIdType sourceFeatureId;
    };

    struct TestFeaturesCollection
    {
    public:
        void clear()
        {
            features.clear();
        }

        void reset(core::FeaturesCollection fc)
        {
            features.clear();
            for (const auto& featuresOnLayer : fc) {
                featuresOnLayer.iterator->reset();
                while (featuresOnLayer.iterator->hasNext())
                {
                    auto& feature = featuresOnLayer.iterator->next();
                    features.push_back(FeatureInfo(
                        featuresOnLayer.layerId,
                        feature.id(),
                        feature.sourceId()));
                }
            }
        }

        TestFeaturesCollection & operator ()(
            unsigned int layerId,
            core::FeatureIdType featureId,
            core::FeatureIdType sourceFeatureId)
        {
            features.push_back(FeatureInfo(layerId, featureId, sourceFeatureId));
            return *this;
        }

        bool operator==(const TestFeaturesCollection& other) const
        {
            return features == other.features;
        }

        std::list<FeatureInfo> features;
    };
}

void map::findFeaturesTest()
{
    io::dir::create(io::tempDirPath());
    {
        TestConfirmationProviderPtr confirmationProvider(
            new test::map::TestConfirmationProvider());

        confirmationProvider->idColumnNameConf->idColumnName() = L"uid";

        core::IMapGuiPtr mapGui = map::createTestMapGui();

        mapGui->setExternalConfirmationsProvider(confirmationProvider);

        mapGui->loadFromXml(mapXmlFileName, true);

        mapGui->open(core::OperationProgressPtr(new core::ProgressStub()));



        agg::trans_affine mtx(
            0.83729053120836894,
            0.0,
            0.0,
            0.83729053120836894,
            9740.7677633103140,
            3045.3861083706888);

        core::FeaturesCollection fc;
        BoxD bbox;
        double px = 0;
        double py = 0;

        TestFeaturesCollection testFC;
        TestFeaturesCollection expectedFC;

        // find visibility features by bounding box
        //
        bbox = BoxD({Vec2D(-11497.5,-3168.42),Vec2D(-11263.4,-3388.17)});
        fc = mapGui->findFeatures(bbox, mtx, false);
        testFC.reset(fc);

        expectedFC = TestFeaturesCollection()
            (21, 78 , 78   )
            (21, 114, 114  )
            (21, 497, 497  )
            (21, 856, 856  )
            (22, 78 , 78   )
            (22, 114, 114  )
            (23, 497, 497  )
            (23, 856, 856  );

        BOOST_CHECK(testFC == expectedFC);

        // find all features by bounding box
        //
        fc = mapGui->findFeatures(bbox, mtx, true);
        testFC.reset(fc);

        expectedFC = TestFeaturesCollection()
            (21, 78 , 78 )
            (21, 114, 114)
            (21, 497, 497)
            (21, 856, 856)
            (23, 497, 497)
            (23, 856, 856);

        BOOST_CHECK(testFC == expectedFC);

        // find features by point
        //
        px = -11501.7; py = -3290.84;

        fc = mapGui->findFeatures(px, py, mtx, false);
        testFC.reset(fc);
        expectedFC = TestFeaturesCollection();
        BOOST_CHECK(testFC == expectedFC);

        px = -11548.9; py = -3352.64;

        fc = mapGui->findFeatures(px, py, mtx, false);
        testFC.reset(fc);
        expectedFC = TestFeaturesCollection()
            (21, 497, 497);
        BOOST_CHECK(testFC == expectedFC);


        px = -11548.3; py = -3353.84;

        fc = mapGui->findFeatures(px, py, mtx, false);
        testFC.reset(fc);
        expectedFC = TestFeaturesCollection()
            (21, 497, 497)
            (23, 497, 497);
        BOOST_CHECK(testFC == expectedFC);


        px = -11274.2; py = -3325.47;

        fc = mapGui->findFeatures(px, py, mtx, true);
        testFC.reset(fc);
        expectedFC = TestFeaturesCollection()
            (21, 78, 78);
        BOOST_CHECK(testFC == expectedFC);

        fc = mapGui->findFeatures(px, py, mtx, false);

        testFC.reset(fc);
        expectedFC = TestFeaturesCollection()
            (21, 78, 78)
            (22, 78, 78);
        BOOST_CHECK(testFC == expectedFC);


        px = -11418.7; py = -3209.62;

        fc = mapGui->findFeatures(px, py, mtx, true);
        testFC.reset(fc);
        expectedFC = TestFeaturesCollection()
            (21, 497, 497)
            (23, 497, 497);
        BOOST_CHECK(testFC == expectedFC);

        fc = mapGui->findFeatures(px, py, mtx, false);
        testFC.reset(fc);
        expectedFC = TestFeaturesCollection()
            (21, 497, 497)
            (23, 497, 497);
        BOOST_CHECK(testFC == expectedFC);
    }
    deleteFilesFromDir(io::tempDirPath());
}

boost::unit_test::test_suite * map::initMapFeaturesSelectionTestSuite()
{
    test_suite * suite = BOOST_TEST_SUITE("Map features selection test suite");

    suite->add(
        BOOST_TEST_CASE(&map::findFeaturesTest));

    return suite;
}
