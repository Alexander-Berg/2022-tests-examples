#include "../altay_commit_notifier.h"
#include "server.h"
#include "test_fixture.h"

#include <yandex/maps/wiki/revision/exception.h>
#include <yandex/maps/mrc/common/algorithm/retry.h>

#define BOOST_AUTO_TEST_MAIN
#define BOOST_TEST_DYN_LINK
#include <boost/test/test_tools.hpp>
#include <boost/test/unit_test.hpp>

#include <functional>
#include <thread>

namespace maps {
namespace wiki {
namespace tasks {
namespace altay_notifier {

const char* TEST_ALTAY_ID_1 = "12345";
const char* TEST_ALTAY_ID_2 = "23456";
const revision::DBID TEST_MAPS_ID_1 = 45654;
const revision::DBID TEST_MAPS_ID_2 = 56765;

BOOST_GLOBAL_FIXTURE(TestLoggingSetup)

BOOST_FIXTURE_TEST_SUITE(altay_commit_notifier_tests,
                         AltayCommitNotifierFixture)

BOOST_AUTO_TEST_CASE(test_create_obj_with_poi)
{
    RequestsCollection requests;
    TestServer server(makeSuccessHandler(requests));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());

    (void)createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    // fast-forward and send the initial snapshot
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);

    (void)createCommit({Data::createObj(TEST_MAPS_ID_2, TEST_ALTAY_ID_2)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 1);

    BOOST_REQUIRE_EQUAL(requests.size(), 2);
    BOOST_CHECK(checkHTTPRequest(requests[0], "POST", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));
    BOOST_CHECK(checkHTTPRequest(requests[1], "POST", TEST_MAPS_ID_2,
                                 TEST_ALTAY_ID_2));
}

BOOST_AUTO_TEST_CASE(test_add_poi_to_obj)
{
    RequestsCollection requests;
    TestServer server(makeSuccessHandler(requests));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());

    // Let the notifier fast-forward on empty DB
    const revision::DBID commitId
        = createCommit({Data::createObj(TEST_MAPS_ID_1, "")});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);

    // Update the test object with new POI data
    (void)createCommit(
        {Data::updatePOI(commitId, TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 1);

    BOOST_REQUIRE_EQUAL(requests.size(), 1);
    BOOST_CHECK(checkHTTPRequest(requests[0], "POST", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));
}

BOOST_AUTO_TEST_CASE(test_update_poi)
{
    RequestsCollection requests;
    TestServer server(makeSuccessHandler(requests));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());

    // The notifier sends a stable snapshot at the very first run
    const revision::DBID commitId
        = createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);
    BOOST_CHECK_EQUAL(requests.size(), 1);
    BOOST_CHECK(checkHTTPRequest(requests[0], "POST", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));

    // Update the test object with new POI data
    (void)createCommit(
        {Data::updatePOI(commitId, TEST_MAPS_ID_1, TEST_ALTAY_ID_2)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 1);
    BOOST_REQUIRE_EQUAL(requests.size(), 3);
    BOOST_CHECK(checkHTTPRequest(requests[1], "DELETE", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));
    BOOST_CHECK(checkHTTPRequest(requests[2], "POST", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_2));
}

BOOST_AUTO_TEST_CASE(test_delete_poi)
{
    RequestsCollection requests;
    TestServer server(makeSuccessHandler(requests));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());

    // The notifier sends a stable snapshot at the very first run
    const revision::DBID commitId
        = createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);
    BOOST_CHECK_EQUAL(requests.size(), 1);
    BOOST_CHECK(checkHTTPRequest(requests[0], "POST", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));

    // Delete POI data
    (void)createCommit({Data::deletePOI(commitId, TEST_MAPS_ID_1)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 1);

    BOOST_REQUIRE_EQUAL(requests.size(), 2);
    BOOST_CHECK(checkHTTPRequest(requests[1], "DELETE", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));
}

BOOST_AUTO_TEST_CASE(test_delete_obj)
{
    RequestsCollection requests;
    TestServer server(makeSuccessHandler(requests));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());

    // The notifier sends a stable snapshot at the very first run
    const revision::DBID commitId
        = createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);
    BOOST_CHECK_EQUAL(requests.size(), 1);
    BOOST_CHECK(checkHTTPRequest(requests[0], "POST", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));

    // Delete POI data
    (void)createCommit({Data::deleteObj(commitId, TEST_MAPS_ID_1)});
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 1);
    BOOST_REQUIRE_EQUAL(requests.size(), 2);
    BOOST_CHECK(checkHTTPRequest(requests[1], "DELETE", TEST_MAPS_ID_1,
                                 TEST_ALTAY_ID_1));
}

BOOST_AUTO_TEST_CASE(test_initial_snapshot)
{
    RequestsCollection requests;
    TestServer server(makeSuccessHandler(requests));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());

    const std::size_t bunchSize = theNotifier.batchSize() * 2 + 1;
    std::vector<Data> dataBunch;
    dataBunch.reserve(bunchSize);
    for (std::size_t id = 0; id < bunchSize; ++id) {
        dataBunch.push_back(
            {id + 1, {{POI_BUSINESS_ID_KEY, std::to_string(id + 1)}}});
    }
    (void)createCommit(dataBunch);

    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);
    BOOST_REQUIRE_EQUAL(requests.size(), bunchSize);
    for (std::size_t id = 0; id < bunchSize; ++id) {
        BOOST_CHECK(checkHTTPRequest(requests[id], "POST", id + 1,
                                     std::to_string(id + 1)));
    }
}

BOOST_AUTO_TEST_CASE(test_http_client_error)
{
    RequestsCollection requests;
    TestServer server(makeNotFoundHandler(requests, TEST_ALTAY_ID_1));
    AltayCommitNotifier theNotifier(configXml());
    theNotifier.overrideAltayPort(server.port());
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);

    // create two commits, the first got an error 404
    // the second is OK
    // check the second is sent in spite of the first client error
    (void)createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    (void)createCommit({Data::createObj(TEST_MAPS_ID_2, TEST_ALTAY_ID_2)});
    // fast-forward and send the initial snapshot
    BOOST_CHECK_EQUAL(theNotifier.processNewCommits(), 0);

    BOOST_REQUIRE_EQUAL(requests.size(), 1);
    BOOST_CHECK(checkHTTPRequest(requests[0], "POST", TEST_MAPS_ID_2,
                                 TEST_ALTAY_ID_2));
}

BOOST_AUTO_TEST_CASE(test_server_error)
{
    AltayCommitNotifier theNotifier(configXml());
    TestServer server(makeServerErrorHandler());
    theNotifier.overrideAltayPort(server.port());
    (void)createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    BOOST_REQUIRE_THROW(theNotifier.processNewCommits(),
                        maps::mrc::common::MaxRetryNumberReached);
}

BOOST_AUTO_TEST_CASE(test_http_timeout)
{
    AltayCommitNotifier theNotifier(configXml());
    TestServer server(makeTimeoutHandler(theNotifier.totalTimeout()
                                         + std::chrono::seconds(1)));
    theNotifier.overrideAltayPort(server.port());

    (void)createCommit({Data::createObj(TEST_MAPS_ID_1, TEST_ALTAY_ID_1)});
    BOOST_REQUIRE_THROW(theNotifier.processNewCommits(),
                        maps::mrc::common::MaxRetryNumberReached);
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace altay_notifier
} // namespace tasks
} // namespace wiki
} // namespace maps
