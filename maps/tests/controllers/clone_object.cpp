#include <maps/wikimap/mapspro/services/editor/src/actions/clone_object.h>
#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/objects_cache.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(clone_object)
{

std::vector<TOid>
objectsOfCategory(const std::string& categoryId, ObjectsCache& cache)
{
    auto revs = cache.revisionsFacade().snapshot().
            revisionIdsByFilter(
                revision::filters::Attr(
                    plainCategoryIdToCanonical(categoryId)).defined());
    std::vector<TOid> oids;
    oids.reserve(revs.size());
    for (const auto& revId : revs) {
        oids.push_back(revId.objectId());
    }
    return oids;
}

size_t
countObjectsOfCategory(const std::string& categoryId)
{
    ObjectsCache cache(
        BranchContextFacade::acquireRead(revision::TRUNK_BRANCH_ID, ""),
        boost::none);
    return objectsOfCategory(categoryId, cache).size();
}

WIKI_FIXTURE_TEST_CASE(test_object_clone, EditorTestFixture)
{
    performObjectsImport("tests/data/tram_route_and_thread.json",
        db.connectionString());
    {
        auto threadsBefore = countObjectsOfCategory(CATEGORY_TRANSPORT_TRAM_THREAD);
        auto threadStopsBefore = countObjectsOfCategory(CATEGORY_TRANSPORT_THREAD_STOP);
        WIKI_TEST_REQUIRE(threadsBefore && threadStopsBefore);
        SaveObjectParser parser;
        parser.parse(
            common::FormatType::JSON,
            loadFile("tests/data/clone_tram_thread_request.json")
        );

        const auto noObservers = makeObservers<>();
        CloneObject controller {
            noObservers,
            CloneObject::Request {
                {TESTS_USER, {}},
                revision::TRUNK_BRANCH_ID,
                1892811809,
                parser.objects(),
                /*feedbackTaskId*/ boost::none
            }
        };
        UNIT_ASSERT_NO_EXCEPTION(controller());
        auto threadsAfter = countObjectsOfCategory(CATEGORY_TRANSPORT_TRAM_THREAD);
        auto threadStopsAfter = countObjectsOfCategory(CATEGORY_TRANSPORT_THREAD_STOP);
        UNIT_ASSERT_EQUAL(threadsAfter, threadsBefore * 2);
        UNIT_ASSERT_EQUAL(threadStopsAfter, threadStopsBefore * 2);
        ObjectsCache cache(
            BranchContextFacade::acquireRead(revision::TRUNK_BRANCH_ID, ""),
            boost::none);
        const auto routes = objectsOfCategory(CATEGORY_TRANSPORT_TRAM_ROUTE, cache);
        UNIT_ASSERT_EQUAL(routes.size(), 2);
        const auto stops1 = cache.getExisting(routes[0])->slaveRelations().range(ROLE_ASSIGNED);
        const auto stops2 = cache.getExisting(routes[1])->slaveRelations().range(ROLE_ASSIGNED);
        UNIT_ASSERT_EQUAL(stops1.size(), stops2.size());
    }

}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
