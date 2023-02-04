#include <maps/analyzer/services/jams_analyzer/modules/dispatcher/lib/targets_pinger/http_logger.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <string>
#include <thread>

using std::chrono_literals::operator""s;
using std::chrono_literals::operator""ms;

TEST(TargetStateLoggerTest, simpleTest) {
    const std::string nodes = "foo.com bar.com";
    const std::string offlineNodesStr = "bar.com";
    time_t startTime = time(0);
    TargetStateLogger t(1, 5, 10);
    std::atomic<bool> stop = false;

    auto reachableSetter = std::thread([&] {
        while (!stop) {
            t.setTargetNetworkState(TargetNetworkState::Reachable, nodes, offlineNodesStr);
            EXPECT_EQ(t.reachableByNetwork(), true);
            t.updateServerState(TargetServerState::Offline, nodes, offlineNodesStr);
            EXPECT_EQ(
                t.getServerState(),
                time(0) - startTime > 4 ? TargetServerState::Offline : TargetServerState::Online
            );
            std::this_thread::sleep_for(1s);
        }
    });

    std::this_thread::sleep_for(500ms);

    auto unreachableSetter = std::thread([&] {
        while (!stop) {
            t.setTargetNetworkState(TargetNetworkState::Unreachable, nodes, offlineNodesStr);
            EXPECT_EQ(t.reachableByNetwork(), false);
            t.updateServerState(TargetServerState::Offline, nodes, offlineNodesStr);
            EXPECT_EQ(
                t.getServerState(),
                time(0) - startTime > 4 ? TargetServerState::Offline : TargetServerState::Online
            );
            std::this_thread::sleep_for(1s);
        }
    });

    std::this_thread::sleep_for(5s);
    stop = true;
    reachableSetter.join();
    unreachableSetter.join();
}


TEST(TargetStateLoggerTest2, chaosTest) {
    // This test checks TargetStateLogger for data races

    constexpr size_t THREADS_COUNT = 1000;
    const std::string nodes = "foo.com bar.com";
    const std::string offlineNodesStr = "bar.com";
    TargetStateLogger t(1, 2, 4);
    std::atomic<bool> stop = false;

    std::vector<std::thread> threads;
    threads.reserve(THREADS_COUNT);
    for (size_t i = 0; i < THREADS_COUNT; ++i) {
        threads.emplace_back([&, i] {
            const auto networkState = i % 2 == 0 ? TargetNetworkState::Reachable : TargetNetworkState::Unreachable;
            const auto serverState = i % 3 == 0 ? TargetServerState::Offline
                : (i % 3 == 1 ? TargetServerState::PartiallyOffline : TargetServerState::Online);

            while (!stop) {
                t.setTargetNetworkState(networkState, nodes, offlineNodesStr);
                t.updateServerState(serverState, nodes, offlineNodesStr);
                std::this_thread::sleep_for(5ms);
                t.getServerState();
                t.reachableByNetwork();
                std::this_thread::sleep_for(5ms);
            }
        });
    }

    std::this_thread::sleep_for(5s);
    stop = true;
    for (size_t i = 0; i < THREADS_COUNT; ++i) {
        threads[i].join();
    }
}
