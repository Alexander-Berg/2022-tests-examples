#include "../test_tools.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/jams_manager.h>


class JamsManagerMock: public JamsManager
{
public:
    JamsManagerMock(
        const std::string& taxiUuidsFile,
        const maps::road_graph::Graph& graph
    ):
        JamsManager(taxiUuidsFile, graph) { }

    bool isApplicable(maps::road_graph::EdgeId edgeId) const override {
        return edgeId.value() % 2;
    }
};

struct JamsManagerFixture : NUnitTest::TBaseFixture
{
    JamsManagerMock jamsManager{TAXI_UUIDS_PATH, getGraph()};
};

Y_UNIT_TEST_SUITE_F(JamsManagerTest, JamsManagerFixture)
{
    Y_UNIT_TEST(JamsTypeByName)
    {
        UNIT_ASSERT(jamsManager.jamsTypeByName(jams_name::DEFAULT) == JamsType::DEFAULT);
        UNIT_ASSERT(jamsManager.jamsTypeByName(jams_name::MASS_TRANSIT) == JamsType::MASS_TRANSIT);
        UNIT_ASSERT(!jamsManager.jamsTypeByName("taxi"));
    }

    Y_UNIT_TEST(UuidsFilter)
    {
        using maps::road_graph::EdgeId;
        {
            const auto uuidsFilter = jamsManager.uuidFilter(JamsType::MASS_TRANSIT, EdgeId(1));
            UNIT_ASSERT(uuidsFilter("taxi-uuid1"));
            UNIT_ASSERT(uuidsFilter("taxi-uuid2"));
            UNIT_ASSERT(!uuidsFilter("uuid"));
        }
        {
            const auto uuidsFilter = jamsManager.uuidFilter(JamsType::MASS_TRANSIT, EdgeId(0));
            UNIT_ASSERT(uuidsFilter("taxi-uuid1"));
            UNIT_ASSERT(uuidsFilter("taxi-uuid2"));
            UNIT_ASSERT(uuidsFilter("uuid"));
        }
        {
            const auto uuidsFilter = jamsManager.uuidFilter(JamsType::DEFAULT, EdgeId(1));
            UNIT_ASSERT(!uuidsFilter("taxi-uuid1"));
            UNIT_ASSERT(!uuidsFilter("taxi-uuid1"));
            UNIT_ASSERT(uuidsFilter("uuid"));
        }
        {
            const auto uuidsFilter = jamsManager.uuidFilter(JamsType::DEFAULT, EdgeId(0));
            UNIT_ASSERT(uuidsFilter("taxi-uuid1"));
            UNIT_ASSERT(uuidsFilter("taxi-uuid2"));
            UNIT_ASSERT(uuidsFilter("uuid"));
        }
    }
}
