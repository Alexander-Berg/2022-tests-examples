#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/libs/edge_persistent_index/include/persistent_index.h>
#include <yandex/maps/jams/static_graph2/persistent_index.h>

const auto FB_FILENAME = "/edges_persistent_index.fb";
const auto MMS_FILENAME = "/edges_persistent_index.mms.1";
const auto PATH_V3 = BinaryPath("maps/data/test/graph3");
const auto PATH_V4 = BinaryPath("maps/data/test/graph4");

using namespace maps;

Y_UNIT_TEST_SUITE(PersistentIndexCompatibilityForTestgraph) {

Y_UNIT_TEST(TestData)
{
    for (const auto& path: {PATH_V3, PATH_V4}) {
        road_graph::PersistentIndex persistentIndex(path + FB_FILENAME);
        jams::static_graph2::PersistentIndex mmsPersistentIndex(path + MMS_FILENAME);

        for (const auto[longId, shortId]: mmsPersistentIndex.items()) {
            UNIT_ASSERT_VALUES_EQUAL(
                persistentIndex
                    .findLongId(road_graph::EdgeId(shortId))
                    .value()
                    .value(),
                mmsPersistentIndex
                    .findLongId(shortId)
                    .value()
                    .value());

            UNIT_ASSERT_VALUES_EQUAL(
                persistentIndex
                    .findShortId(road_graph::LongEdgeId(longId.value()))
                    .value()
                    .value(),
                mmsPersistentIndex
                    .findShortId(longId)
                    .value());
        }
    }
}

}
