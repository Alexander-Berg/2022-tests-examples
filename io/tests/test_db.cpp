#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/libs/logging/setup/setup.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/callback_queue.h>

#include <yandex_io/libs/metrica/db/lmdb_events.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/tests/testlib/unittest_helper/logging_test_fixture.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/common/env.h>
#include <util/system/env.h>
#include <util/folder/tempdir.h>

#include <future>
#include <random>

using namespace YandexIO;

namespace {
    struct TempDirWrapper {
        TTempDir tempDir;

        TempDirWrapper(const std::string& path)
            : tempDir(TTempDir::NewTempDir(TString(path)))
        {
        }
    };

    struct TestSink: public EventsDB::Sink {
        using SinkFn = std::function<void(EventsDB::ISourceControl&, EventsDB::Event, EventsDB::Environment)>;
        SinkFn realSink_;

        TestSink(SinkFn sink)
            : realSink_(std::move(sink))
        {
        }

        void handleDbEvent(ITelemetryEventsDB::ISourceControl& handler, EventsDB::Event event, EventsDB::Environment env) override {
            realSink_(handler, std::move(event), std::move(env));
        }
    };

    std::shared_ptr<EventsDB::Sink> makeTestSink(TestSink::SinkFn sink) {
        return std::make_shared<TestSink>(std::move(sink));
    }

    struct LmdbEventsFixture: public QuasarLoggingTestFixture {
        using Base = QuasarLoggingTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            dbDir = std::make_unique<TempDirWrapper>(quasar::TestUtils::tryGetRamDrivePath());
            dbQueue = std::make_shared<quasar::CallbackQueue>(100);
            db = YandexIO::makeLmdbEventsDb(dbDir->tempDir.Name(), 1024000, dbQueue);
            passAllFilter.isWhiteList = false;
            sinkQueue = std::make_shared<quasar::CallbackQueue>(10);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            db.reset();
            Base::TearDown(context);
        }

        static void defaultFiller(ITelemetryEventsDB::Event& event, int idx) {
            event.set_value("payload_" + std::to_string(idx));
        }

        void pushOneEvent(const std::string& name, const std::string& payload = "dummy") const {
            ITelemetryEventsDB::Event event;
            event.set_name(TString(name));
            event.set_value(TString(payload));
            db->pushEvent(event);
        }

        void generateTestEvents(int amount, std::function<void(ITelemetryEventsDB::Event&, int)> filler = defaultFiller) {
            static const char* eventNames[] = {
                "systemMetrics",
                "wifiStats",
                "ysk_error_network",
                "glagold_heartbeat",
                "minidump",
            };
            std::mt19937 gen{rnd()};
            std::discrete_distribution<> d({200, 80, 30, 10, 5});

            for (int i = 0; i < amount; ++i) {
                ITelemetryEventsDB::Event event;
                const int idx = (int)round(d(gen));
                event.set_name(eventNames[idx]);
                filler(event, i);
                db->pushEvent(event);
            };
        }

        static void checkEnv(const ITelemetryEventsDB::Environment& env, std::map<std::string, std::string> expect) {
            auto vals = env.environment_values();
            UNIT_ASSERT_EQUAL(vals.size(), expect.size());
            for (auto [key, value] : expect) {
                auto iter = vals.find(TString(key));
                UNIT_ASSERT(iter != vals.end());
                UNIT_ASSERT(iter->second == value);
            }
        }

        static void checkEnvByBlackList(const ITelemetryEventsDB::Environment& env, const std::set<std::string>& blackList) {
            auto& vals = env.environment_values();
            for (auto [name, value] : vals) {
                UNIT_ASSERT_C(!blackList.contains(name), "Blacklisted environment arrived " + name);
            };
        }

        std::unique_ptr<TempDirWrapper> dbDir;
        std::shared_ptr<quasar::ICallbackQueue> dbQueue;
        std::shared_ptr<ITelemetryEventsDB> db;
        std::random_device rnd;
        YandexIO::EventsDB::EventsFilter passAllFilter;
        std::shared_ptr<quasar::ICallbackQueue> sinkQueue;
    };
} // namespace

Y_UNIT_TEST_SUITE(TestLmdbEventsDb) {
    Y_UNIT_TEST_F(oneSinkNormalFlow, LmdbEventsFixture) {
        const std::string finalEvent = quasar::makeUUID();
        pushOneEvent("test", "payload");
        std::promise<void> promise;
        auto sink = makeTestSink([&promise, &finalEvent](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
            YIO_LOG_DEBUG("Event name = " << event.name() << ", value = " << event.value());
            if (event.value().back() == '0') {
                handler.releaseBeforeLast();
            }
            if (event.name() == finalEvent) {
                handler.releaseIncludingLast();
                promise.set_value();
            } else {
                handler.readyForNext();
            }
        });
        auto sinkHandler = db->registerSink(0, sink, sinkQueue, passAllFilter);
        sinkHandler->readyForNext();

        generateTestEvents(10000);

        pushOneEvent(finalEvent);
        promise.get_future().get();
    }

    Y_UNIT_TEST_F(twoSinksSecondDelayed, LmdbEventsFixture) {
        auto sinkQueue2 = std::make_shared<quasar::CallbackQueue>(10);

        std::shared_ptr<ITelemetryEventsDB::ISourceControl> sinkHandler2;

        std::promise<void> promise1;
        unsigned sink1Count = 0;
        {
            auto sink1 = makeTestSink([&sinkHandler2, &promise1, &sink1Count](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                YIO_LOG_DEBUG("sink1 received event: name = " << event.name() << ", value = " << event.value());
                ++sink1Count;
                if (event.name().back() == '0') {
                    handler.releaseBeforeLast();
                }
                if (event.value() == "payload_100" && sinkHandler2) {
                    sinkHandler2->readyForNext();
                }
                if (event.value() == "payload_999") {
                    handler.releaseIncludingLast();
                    promise1.set_value();
                } else {
                    handler.readyForNext();
                }
            });
            auto sinkHandler1 = db->registerSink(0, sink1, sinkQueue, passAllFilter);
            sinkHandler1->readyForNext();
        }

        std::promise<void> promise2;
        unsigned sink2Count = 0;
        {
            auto sink2 = makeTestSink([&promise2, &sink2Count](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                YIO_LOG_DEBUG("sink2 received event: name = " << event.name() << ", value = " << event.value());
                ++sink2Count;
                if ((random() % 30) == 1) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(100));
                }
                if (event.value() == "payload_999") {
                    handler.releaseIncludingLast();
                    promise2.set_value();
                } else {
                    if (event.value().back() == '0') {
                        handler.releaseIncludingLast();
                    }
                    handler.readyForNext();
                }
            });
            sinkHandler2 = db->registerSink(1, sink2, sinkQueue2, passAllFilter);
        }
        generateTestEvents(1000);
        promise1.get_future().get();
        YIO_LOG_INFO("Sink1 finished");
        promise2.get_future().get();
        YIO_LOG_INFO("Sink2 finished");
        UNIT_ASSERT_EQUAL(sink1Count, 1000);
        UNIT_ASSERT_EQUAL(sink2Count, 1000);
    }

    Y_UNIT_TEST_F(priorityExclusion, LmdbEventsFixture) {
        using Priority = YandexIO::EventsDB::Priority;
        db->setConfig(YandexIO::EventsDB::Config{
            .priority = [](const std::string_view& eventName) -> Priority {
                if (eventName == "prio0") {
                    return Priority::LOWEST;
                }
                if (eventName == "prio1") {
                    return Priority::LOW;
                }
                if (eventName == "prio3") {
                    return Priority::HIGH;
                }
                if (eventName == "prio4") {
                    return Priority::HIGHEST;
                }
                return Priority::DEFAULT;
            }});

        std::promise<void> promise;
        unsigned cnt1 = 0;
        unsigned cnt2 = 0;
        auto sink = makeTestSink([&promise, &cnt1, &cnt2](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
            YIO_LOG_DEBUG("sink2 received event: name = " << event.name());
            UNIT_ASSERT_C(event.name() != "prio0", "Lowest priority events shouldnt exist");
            if (event.name() == "prio5") {
                handler.releaseIncludingLast();
                promise.set_value();
            } else {
                if (event.name() == "prio1") {
                    ++cnt1;
                } else if (event.name() == "prio2") {
                    ++cnt2;
                }
                handler.readyForNext();
            }
        });

        auto sinkHandler = db->registerSink(0, sink, sinkQueue, passAllFilter);
        generateTestEvents(100, [](auto& event, int idx) { // fill whole db
            event.set_value(TString(10240, '#'));
            if (idx % 2) {
                event.set_name("prio1");
            } else {
                event.set_name("prio0");
            }
        });
        // now we have 49 of prio0 and 50 of prio1
        generateTestEvents(100, [](auto& event, int idx) { // fill whole db
            event.set_value(TString(10240, '#'));
            if (idx % 2) {
                event.set_name("prio1");
            } else {
                event.set_name("prio2");
            }
        });
        // check that we have prio2 >= prio1 and no prio0
        pushOneEvent("prio5");
        sinkHandler->readyForNext();
        promise.get_future().get();
        UNIT_ASSERT(cnt2 >= cnt1);
    }

    Y_UNIT_TEST_F(emptyMessages, LmdbEventsFixture) {
        auto sink = makeTestSink([](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event /*event*/, ITelemetryEventsDB::Environment /*env*/) {
            handler.readyForNext();
        });
        auto sinkHandler = db->registerSink(0, sink, sinkQueue, passAllFilter);
        generateTestEvents(27670, [](auto& event, int /*idx*/) {
            event.set_name("");
            //            event = ITelemetryEventsDB::Event();
        });
    }

    Y_UNIT_TEST_F(longPriorityDeleting, LmdbEventsFixture) {
        using Priority = YandexIO::EventsDB::Priority;
        db->setConfig(YandexIO::EventsDB::Config{
            .priority = [](const std::string_view& eventName) -> Priority {
                if (eventName == "wifiStats") {
                    return Priority::HIGH;
                }
                if (eventName == "ysk_error_network") {
                    return Priority::HIGHEST;
                }
                if (eventName == "glagold_heartbeat") {
                    return Priority::LOW;
                }
                if (eventName == "minidump") {
                    return Priority::LOWEST;
                }
                return Priority::DEFAULT;
            }});
        auto sink = makeTestSink([](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event /*event*/, ITelemetryEventsDB::Environment /*env*/) {
            handler.readyForNext();
        });
        auto sinkHandler = db->registerSink(0, sink, sinkQueue, passAllFilter);
        generateTestEvents(2000, [](auto& event, int /*idx*/) {
            if (event.name() == "minidump") {
                event.set_value(TString(10000, '#'));
            } else if (event.name() == "wifiStats") {
                event.set_value(TString(2000, '*'));
            }
        });
    }

    Y_UNIT_TEST_F(filterOneSink, LmdbEventsFixture) {
        YandexIO::EventsDB::EventsFilter filter;
        const std::string finalEvent = quasar::makeUUID();
        const std::string midEvent = quasar::makeUUID();
        filter.eventNames = {"ysk_error_network", "wifiStats", midEvent, finalEvent};
        filter.isWhiteList = true;
        std::promise<void> midPromise;
        std::promise<void> promise;
        auto sink = makeTestSink([&midPromise, &promise, &midEvent, &finalEvent, &filter](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
            YIO_LOG_DEBUG("GOT " << event.name());
            handler.releaseIncludingLast();
            handler.readyForNext();
            if (event.name() == midEvent) {
                YIO_LOG_DEBUG("midPromise reached");
                midPromise.set_value();
            } else if (event.name() == finalEvent) {
                YIO_LOG_DEBUG("Promise fulfilled");
                promise.set_value();
            }
            UNIT_ASSERT(filter.eventNames.contains(event.name()));
        });
        auto sinkHandler = db->registerSink(0, sink, sinkQueue, filter);
        sinkHandler->readyForNext();
        generateTestEvents(50);
        pushOneEvent(midEvent);
        midPromise.get_future().get();

        filter.eventNames.erase("ysk_error_network");
        filter.eventNames.insert("systemMetrics");
        db->updateFilter(0, filter);
        YIO_LOG_DEBUG("filter updated");

        generateTestEvents(50);
        pushOneEvent(finalEvent);
        promise.get_future().get();
    }

    Y_UNIT_TEST_F(filterOneSinkInverted, LmdbEventsFixture) {
        YandexIO::EventsDB::EventsFilter filter;
        filter.eventNames = {"systemMetrics", "glagold_heartbeat", "minidump"};
        const std::string finalEvent = quasar::makeUUID();
        filter.isWhiteList = false;
        std::promise<void> promise;
        auto sink = makeTestSink([&promise, &finalEvent, &filter](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
            handler.releaseIncludingLast();
            handler.readyForNext();
            if (event.name() == finalEvent) {
                promise.set_value();
            } else {
                UNIT_ASSERT(!filter.eventNames.contains(event.name()));
            }
        });
        auto sinkHandler = db->registerSink(0, sink, sinkQueue, filter);
        sinkHandler->readyForNext();
        generateTestEvents(100);
        pushOneEvent(finalEvent);
        promise.get_future().get();
    }

    Y_UNIT_TEST_F(twoDifferentFiltersForTwoSinks, LmdbEventsFixture) {
        const std::string finalEvent = quasar::makeUUID();
        std::promise<void> promise1;
        unsigned count = 0;
        {
            YandexIO::EventsDB::EventsFilter filter;
            filter.eventNames = {"ysk_error_network", "wifiStats", finalEvent};
            filter.isWhiteList = true;
            filter.envBlackList = {"test1", "test2"};
            auto sink = makeTestSink([&promise1, &count, &finalEvent, filter](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment env) {
                handler.releaseIncludingLast();
                handler.readyForNext();
                if (!count++) {
                    std::this_thread::sleep_for(std::chrono::milliseconds(400)); // delay this sink
                }
                if (event.name() == finalEvent) {
                    promise1.set_value();
                }
                UNIT_ASSERT(filter.eventNames.contains(event.name()));
                checkEnvByBlackList(env, filter.envBlackList);
            });
            auto sinkHandler = db->registerSink(0, sink, sinkQueue, filter);
            sinkHandler->readyForNext();
        }
        std::promise<void> promise2;
        auto sinkQueue2 = std::make_shared<quasar::CallbackQueue>(10);
        {
            YandexIO::EventsDB::EventsFilter filter;
            filter.eventNames = {"glagold_heartbeat", "wifiStats", finalEvent};
            filter.isWhiteList = true;
            filter.envBlackList = {"test1", "test3"};
            auto sink = makeTestSink([&promise2, &finalEvent, filter](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment env) {
                handler.releaseIncludingLast();
                handler.readyForNext();
                if (event.name() == finalEvent) {
                    promise2.set_value();
                }
                UNIT_ASSERT(filter.eventNames.contains(event.name()));
                checkEnvByBlackList(env, filter.envBlackList);
            });
            auto sinkHandler = db->registerSink(1, sink, sinkQueue2, filter);
            sinkHandler->readyForNext();
        }
        db->updateEnvironmentVar("test", "testValue");
        db->updateEnvironmentVar("test1", "test1Value");
        db->updateEnvironmentVar("test2", "test2Value");
        db->updateEnvironmentVar("test3", "test3Value");
        generateTestEvents(10);
        db->removeEnvironmentVar("test");
        db->removeEnvironmentVar("test1");
        generateTestEvents(10);
        pushOneEvent(finalEvent);
        promise1.get_future().get();
        promise2.get_future().get();
    }

    Y_UNIT_TEST_F(environment, LmdbEventsFixture) {
        std::promise<void> promise;
        auto sink = makeTestSink([&promise](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment env) {
            YIO_LOG_DEBUG("Event name = " << event.name() << ", value = " << event.value());
            if (event.name() == "final") {
                handler.releaseIncludingLast();
                promise.set_value();
            } else {
                if (event.name() == "event1") {
                    checkEnv(env, {{"env1", "val1"}, {"env2", "val2"}});
                } else if (event.name() == "event2") {
                    checkEnv(env, {{"env1", "val1"}});
                } else if (event.name() == "event3") {
                    checkEnv(env, {{"env1", "val1"}, {"env3", "val23"}});
                } else if (event.name() == "event4") {
                    checkEnv(env, {{"env3", "val23"}});
                }
                handler.readyForNext();
            }
        });

        db->updateEnvironmentVar("env1", "val1");
        db->updateEnvironmentVar("env2", "val2");
        pushOneEvent("event1");
        db->removeEnvironmentVar("env2");
        pushOneEvent("event2");

        auto sinkHandler = db->registerSink(0, sink, sinkQueue, passAllFilter);
        sinkHandler->readyForNext();

        db->updateEnvironmentVar("env3", "val23");
        pushOneEvent("event3");
        db->removeEnvironmentVar("env1");
        pushOneEvent("event4");
        pushOneEvent("final");

        promise.get_future().get();
    }

    /* on overflow db new event passed into sink which is already processed all previous
     */
    Y_UNIT_TEST_F(bypass, LmdbEventsFixture) {
        {
            auto sink1 = makeTestSink([](ITelemetryEventsDB::ISourceControl& /*handler*/, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                YIO_LOG_DEBUG("sink1 received event: name = " << event.name() << ", value = " << event.value());
            });
            db->registerSink(0, sink1, sinkQueue, passAllFilter);
            // this sink just holds events in db
        }

        auto sinkQueue2 = std::make_shared<quasar::CallbackQueue>(10);
        std::promise<void> promise2;
        std::promise<void> promise3;
        {
            auto sink2 = makeTestSink([&promise2, &promise3](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                YIO_LOG_DEBUG("sink2 received event: name = " << event.name() << ", value.size() = " << event.value().size());
                handler.releaseIncludingLast();
                handler.readyForNext();
                if (event.name() == "event2") {
                    promise2.set_value();
                }
                if (event.name() == "event3") {
                    promise3.set_value();
                }
            });
            auto sinkHandler2 = db->registerSink(1, sink2, sinkQueue2, passAllFilter);
            sinkHandler2->readyForNext();
        }
        pushOneEvent("event1", std::string(300000, '*'));
        pushOneEvent("event1", std::string(300000, '*'));
        pushOneEvent("event2", std::string(300000, '*'));
        promise2.get_future().get();
        pushOneEvent("event3", std::string(300000, '*')); // this one will not be stored cos db already overflowed
        promise3.get_future().get();
    }

    /* sinks are restored at their positions after crashes */
    Y_UNIT_TEST_F(restorePosition, LmdbEventsFixture) {
        auto registerFirstSink = [this]() {
            auto sink1 = makeTestSink([](ITelemetryEventsDB::ISourceControl& /*handler*/, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                YIO_LOG_DEBUG("sink1 received event: name = " << event.name() << ", value = " << event.value());
            });
            db->registerSink(0, sink1, sinkQueue, passAllFilter);
            // this sink just holds events in db
        };
        { // first run
            registerFirstSink();
            auto sinkQueue2 = std::make_shared<quasar::CallbackQueue>(10);
            std::promise<void> promise;
            {
                auto sink2 = makeTestSink([&promise](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                    YIO_LOG_DEBUG("sink2 received event: name = " << event.name() << ", value.size() = " << event.value().size());
                    handler.readyForNext();
                    if (event.name() == "nextStage") {
                        handler.releaseIncludingLast();
                        promise.set_value();
                    }
                });
                auto sinkHandler2 = db->registerSink(1, sink2, sinkQueue2, passAllFilter);
                sinkHandler2->readyForNext();
            }
            generateTestEvents(10);
            pushOneEvent("nextStage");
            promise.get_future().get();
        }
        db.reset();
        { // dummy check
            std::promise<void> finishPromise;
            dbQueue->add([&finishPromise]() {
                finishPromise.set_value();
            });
            finishPromise.get_future().get();
        }
        db = YandexIO::makeLmdbEventsDb(dbDir->tempDir.Name(), 1024000, dbQueue);
        registerFirstSink();
        auto sinkQueue2 = std::make_shared<quasar::CallbackQueue>(10);
        std::promise<void> promise;
        {
            auto sink2 = makeTestSink([&promise](ITelemetryEventsDB::ISourceControl& handler, ITelemetryEventsDB::Event event, ITelemetryEventsDB::Environment /*env*/) {
                YIO_LOG_DEBUG("sink2 received event: name = " << event.name() << ", value.size() = " << event.value().size());
                if (event.name() == "final") {
                    handler.releaseIncludingLast();
                    promise.set_value();
                } else {
                    UNIT_ASSERT_EQUAL(event.name(), "secondStage");
                }
                handler.readyForNext();
            });
            auto sinkHandler2 = db->registerSink(1, sink2, sinkQueue2, passAllFilter);
            sinkHandler2->readyForNext();
        }
        pushOneEvent("secondStage");
        pushOneEvent("secondStage");
        pushOneEvent("secondStage");
        pushOneEvent("final");
        promise.get_future().get();
    }
}
