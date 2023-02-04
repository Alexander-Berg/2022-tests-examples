#include "../include/tools.h"
#include "../include/contexts.hpp"
#include <yandex/maps/tilerenderer4/IOnlineRenderer.h>
#include <boost/test/unit_test.hpp>
#include <agg_trans_affine.h>

#include <iostream>

using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::tilerenderer4;
using namespace maps::tilerenderer4::test;
using test::tools::countRowsFromTable;

namespace {

const std::string SMALL_TEST_TEXT_MAP_NAME = "tests/boost-tests/maps/SmallTextMap.xml";
const std::string TABLE_WITH_LABELS_1 = "renderer_autotest.testptfc";
const std::string TABLE_WITH_LABELS_2 = "renderer_autotest2.testptfc";

class MockTraverse: public ILayersTraverse
{
public:
    bool onEnterGroupLayer(const LayerInfo& layer) { return true; }
    bool onVisitLayer(const LayerInfo& layer) { return true; }
    void onLeaveGroupLayer(const LayerInfo& layer) { }
    bool isDone() { return false; }
};

}

BOOST_AUTO_TEST_SUITE( OnlineRenderer )

BOOST_FIXTURE_TEST_CASE( prov_ex, OnlineRendererContext<TransactionProviderContext<CleanContext<>>> )    // checks extended transaction provider
{
    // providerEx uses single transaction
    BOOST_REQUIRE(&providerEx->labelsTransaction()->get() == trans.get());
    BOOST_REQUIRE(&providerEx->objectsTransaction()->get() == trans.get());

    renderer->open(SMALL_TEST_TEXT_MAP_NAME);

    int x = 39585;
    int y = 20569;
    double mtx[6] = {0, 0, 0, 0, 0, 0};
    agg::trans_affine().store_to(mtx);
    unsigned int z = 16;
    RenderedTile tile_auto1;
    {
        BOOST_CHECK_NO_THROW(tile_auto1 = renderer->render(x, y, z, OutputFormatRgbaPng, providerEx, mtx, nullptr));
    }

    // change search path, but dont inform provider
    // should use prepared statement with old schema => will retrieve same tile
    providerEx->objectsTransaction()->get().exec("SET search_path TO renderer_autotest2,public;");
    providerEx->setQueryNamePrefix("renderer_autotest2");

    RenderedTile tile_auto2;
    {
        BOOST_CHECK_NO_THROW(tile_auto2 = renderer->render(x, y, z, OutputFormatRgbaPng, providerEx, mtx, nullptr));
    }

    // go back to first schema
    providerEx->objectsTransaction()->get().exec("SET search_path TO renderer_autotest,public;");
    providerEx->setQueryNamePrefix("renderer_autotest");

    RenderedTile tile_auto3;
    {
        BOOST_CHECK_NO_THROW(tile_auto3 = renderer->render(x, y, z, OutputFormatRgbaPng, providerEx, mtx, nullptr));
    }

    BOOST_CHECK(
        (tile_auto1.outputDataSize > 0) &&
        (tile_auto2.outputDataSize > 0) &&
        (tile_auto3.outputDataSize > 0));

    BOOST_CHECK_EQUAL(tile_auto1.outputDataSize, tile_auto3.outputDataSize);
    BOOST_CHECK(tile_auto1.outputDataSize != tile_auto2.outputDataSize);

    BOOST_CHECK_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 0);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);

    // go back to first schema
    providerEx->objectsTransaction()->get().exec("SET search_path TO renderer_autotest,public;");
    providerEx->setQueryNamePrefix("renderer_autotest");
    // check updateLabels with extended provider functionality
    uint64_t featureId  = 993771056;
    auto labelReport = renderer->placeLabels(featureId, providerEx);
    BOOST_CHECK_NE(labelReport.size(), 0);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 6);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);

    // switch to 2nd scheme
    providerEx->objectsTransaction()->get().exec("SET search_path TO renderer_autotest2,public;");
    providerEx->setQueryNamePrefix("renderer_autotest2");

    BOOST_CHECK_NO_THROW(labelReport = renderer->placeLabels(featureId, providerEx));  // if schema switched correctly, should give empty result
    BOOST_CHECK_EQUAL(labelReport.size(), 0);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 6);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);

    // switch back
    providerEx->objectsTransaction()->get().exec("SET search_path TO renderer_autotest,public;");
    providerEx->setQueryNamePrefix("renderer_autotest");
    // check non-empty traverse
    {
        std::unique_ptr<MockTraverse> traverse(new MockTraverse());
        RenderedTile tile;
        BOOST_CHECK_NO_THROW(tile = renderer->render(x, y, z, OutputFormatRgbaPng, providerEx, mtx, traverse.get()));
    }
}

BOOST_FIXTURE_TEST_CASE(prov_ex2, OnlineRendererContext<TransactionProviderContext<CleanContext<>>>)
{
    // provider2Trns uses two different transactions
    BOOST_REQUIRE(&provider2Trns->labelsTransaction()->get() != trans.get());
    BOOST_REQUIRE(&provider2Trns->objectsTransaction()->get() == trans.get());

    renderer->open(SMALL_TEST_TEXT_MAP_NAME);

    BOOST_CHECK_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 0);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);

    uint64_t featureId = 993771056;
    auto labelReport = renderer->placeLabels(featureId, provider2Trns);
    BOOST_CHECK_NE(labelReport.size(), 0);

    BOOST_CHECK_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 0);
    BOOST_CHECK_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 6);
}

BOOST_AUTO_TEST_SUITE_END()
