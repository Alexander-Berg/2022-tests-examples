#include "../hist_recorder_impl.h"

#include "mocks/mock_hist_manager.h"

#include <yandex/maps/navikit/time_utils.h>
#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/optional.hpp>
#include <boost/test/unit_test.hpp>

#include <unordered_map>

namespace yandex::maps::navi::profiling {

using namespace std::chrono_literals;

namespace {

const auto TIME_EPS = 5ms;
const auto WAIT_LIMIT = 1s;

bool timeIntervalsClose(
    const runtime::TimeInterval& first, const runtime::TimeInterval& second,
    const runtime::TimeInterval& eps = TIME_EPS)
{
    if (first < second) {
        return second - first < eps;
    } else {
        return first - second < eps;
    }
}

void checkRequest(
    const MockHistRequest& request, const HistParams& params,
    boost::optional<runtime::TimeInterval> testDuration = boost::none, std::string eventSuffix = {})
{
    BOOST_CHECK(request.name == params.name + eventSuffix);
    if (testDuration) {
        BOOST_CHECK(timeIntervalsClose(request.duration, *testDuration));
    }
}

void checkRequest(
    runtime::async::MultiFuture<MockHistRequest>& requests, const HistParams& params,
    boost::optional<runtime::TimeInterval> testDuration = boost::none,
    const std::string& eventSuffix = {})
{
    BOOST_REQUIRE(requests.waitFor(WAIT_LIMIT) == runtime::async::FutureStatus::Ready);
    checkRequest(requests.get(), params, testDuration, eventSuffix);
}

struct HistRecorderBaseFixture {
    HistRecorderBaseFixture(std::vector<HistParams> params)
        : params(params)
        , recorder(params)
        , manager(std::make_shared<MockHistManager>())
        , requests(manager->requests())
    {
        recorder.init(manager, /* testBuckets= */ {}, /* testMode= */ false);
        recorder.onManagerInitialized();
    }

    std::vector<HistParams> params;
    HistRecorderImpl recorder;
    std::shared_ptr<MockHistManager> manager;
    runtime::async::MultiFuture<MockHistRequest> requests;
};

std::vector<HistParams> buildSimpleParams(size_t count)
{
    std::vector<HistParams> params;
    for (size_t i = 0; i < count; ++i) {
        std::string name = "test_" + std::to_string(i);
        params.push_back({/* event */ static_cast<HistEvent>(i),
                          /* name */ name,
                          /* minDuration */ 1ms,
                          /* maxDuration */ 1000ms,
                          /* numberOfBuckets */ 100,
                          /* threshold */ boost::none});
    }
    return params;
}

struct HistRecorderSimpleFixture : public HistRecorderBaseFixture {
    HistRecorderSimpleFixture() : HistRecorderBaseFixture(buildSimpleParams(2)) {}
};

struct HistRecorderThresholdedFixture : public HistRecorderBaseFixture {
    HistRecorderThresholdedFixture()
        : HistRecorderBaseFixture({{/* event */ static_cast<HistEvent>(0),
                                    /* name */ "test",
                                    /* minDuration */ 1ms,
                                    /* maxDuration */ 1000ms,
                                    /* numberOfBuckets */ 100,
                                    /* threshold */ 200ms}})
    {
    }
};

}  // namespace

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Test suite
//

BOOST_AUTO_TEST_SUITE(HistRecorderTest)

BOOST_AUTO_TEST_CASE(testManagerInit)
{
    HistRecorderImpl recorder({});
    auto manager = std::make_shared<MockHistManager>();

    const std::string testBuckets = "test";
    recorder.init(manager, testBuckets, /* testMode= */ false);
    const std::map<std::string, std::string> variationsOk = {{"bucket_test", "enabled"}};

    BOOST_CHECK(manager->initialized());
    BOOST_CHECK(manager->variations() == variationsOk);
}

BOOST_FIXTURE_TEST_CASE(testSendSimpleEvent, HistRecorderSimpleFixture)
{
    auto testDuration = 100ms;
    recorder.begin(params[0].event);
    auto testBegin = navikit::relativeNow();
    runtime::async::sleepFor(testDuration);
    auto testEnd = navikit::relativeNow();
    recorder.end(params[0].event);
    checkRequest(requests, params[0], testEnd - testBegin);
}

BOOST_FIXTURE_TEST_CASE(testSendThresholdedEvent, HistRecorderThresholdedFixture)
{
    auto threshold = *params[0].threshold;
    // first event - should be "cold"
    recorder.begin(params[0].event);
    recorder.end(params[0].event);
    checkRequest(requests, params[0], 0ms, ".Cold");

    // wait half of threshold time
    runtime::async::sleepFor(threshold / 2);

    // second event - should be also "cold"
    recorder.begin(params[0].event);
    recorder.end(params[0].event);
    checkRequest(requests, params[0], 0ms, ".Cold");

    // wait another half of threshold time plus eps
    runtime::async::sleepFor(threshold / 2 + TIME_EPS);

    // last event - should be "warm"
    recorder.begin(params[0].event);
    recorder.end(params[0].event);
    checkRequest(requests, params[0], 0ms, ".Warm");
}

BOOST_FIXTURE_TEST_CASE(testSendParallelEvent, HistRecorderSimpleFixture)
{
    const size_t parallelEvents = params.size();
    std::unordered_map<std::string, size_t> nameToId;

    for (size_t i = 0; i < parallelEvents; ++i) {
        nameToId[params[i].name] = i;
    }

    size_t samplesPerEvent = 1000;
    std::vector<std::thread> threads;
    for (const auto& param : params) {
        auto event = param.event;
        threads.emplace_back([=] {
            for (size_t i = 0; i < samplesPerEvent; ++i) {
                recorder.begin(event);
                recorder.end(event);
            }
        });
    }
    for (auto& thread : threads) {
        thread.join();
    }

    std::vector<size_t> samples(parallelEvents, 0);
    for (size_t i = 0; i < samplesPerEvent * parallelEvents; ++i) {
        BOOST_REQUIRE(requests.waitFor(WAIT_LIMIT) == runtime::async::FutureStatus::Ready);
        auto request = requests.get();
        BOOST_REQUIRE(nameToId.count(request.name));
        auto id = nameToId[request.name];
        ++samples[id];
        checkRequest(request, params[id]);
    }
    for (size_t i = 0; i < parallelEvents; ++i) {
        BOOST_CHECK(samples[i] == samplesPerEvent);
    }
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex
