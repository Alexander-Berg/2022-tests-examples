#include <yandex_io/libs/metrica/clickdaemon/clickdaemon_consumer.h>
#include <yandex_io/libs/metrica/clickdaemon/clickdaemon_sender.h>
#include <yandex_io/libs/metrica/base/events_database_creator.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::proto;
using namespace quasar::TestUtils;

namespace {
    using Event = std::map<std::string, std::string>;
    using EventBatch = std::vector<Event>;
    using Events = std::vector<EventBatch>;

    class MockClickdaemon {
    public:
        TestHttpServer httpEndpoint;

        EventBatch expectedEvents;
        std::vector<std::promise<void>> promisesPerEvent;
        std::atomic_size_t gotBatches;
        std::atomic_size_t gotEvents;

        void prepareForEvents(const Events& events) {
            expectedEvents.clear();
            for (const auto& batch : events) {
                for (const auto& e : batch) {
                    expectedEvents.push_back(e);
                }
            }
            promisesPerEvent.clear();
            promisesPerEvent.resize(expectedEvents.size());
            YIO_LOG_DEBUG("Will wait for " << promisesPerEvent.size() << " events");

            gotBatches = 0;
            gotEvents = 0;

            httpEndpoint.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                               const std::string& payload, TestHttpServer::HttpConnection& handler)
            {
                YIO_LOG_DEBUG("Got payload: " << payload);
                auto receivedEvents = parseJson(payload);
                handler.doReplay(200, "application/json", "{}");

                ++gotBatches;
                for (unsigned int i = 0; i < receivedEvents.size(); i++) {
                    auto eventIdx = gotEvents++;
                    UNIT_ASSERT(eventIdx < expectedEvents.size());
                    for (auto& eventField : expectedEvents[eventIdx]) {
                        UNIT_ASSERT_VALUES_EQUAL_C(receivedEvents[i][eventField.first].asString(), eventField.second, eventField.first);
                    }
                    promisesPerEvent[eventIdx].set_value();
                }
            };
        }

        void waitForEvents() {
            for (auto& promise : promisesPerEvent) {
                promise.get_future().wait();
            }
        }
    };

    class ClickdaemonConsumerWithChecks: public ClickdaemonConsumer {
    public:
        ClickdaemonConsumerWithChecks(const Json::Value& config, std::shared_ptr<YandexIO::IDevice> device)
            : ClickdaemonConsumer(config, std::move(device))
        {
        }

        size_t getQueueSize() const {
            return immediateQueue_->size();
        }

        std::unique_ptr<EventsDatabase::Event> getNextDbEvent() {
            std::vector<uint64_t> idsToSkip;
            std::unique_ptr<EventsDatabase::Event> dbEvent;

            while (dbEvent = dbQueue_->getEarliestEvent(idsToSkip)) {
                auto& event = dbEvent->databaseMetricaEvent;

                if (event.has_new_environment()) {
                    idsToSkip.push_back(dbEvent->id);
                } else {
                    break;
                }
            }

            return dbEvent;
        }
    };

    std::string generateRandomString() {
        std::random_device rd;
        std::uniform_int_distribution<int> charDist(0, 25);
        std::string rval;
        for (int i = 0; i < 7; ++i) {
            rval += static_cast<char>('a' + charDist(rd));
        };
        return rval;
    }

    struct Fixture: public QuasarUnitTestFixture {
        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<YandexIO::IDevice> device;
        Json::Value consumerConfig;

        MockClickdaemon mockClickdaemon;

        std::string dbName_;
        std::string metadataPath_;
        std::string sessionIdPersistentPart_;
        std::string sessionIdTemporaryPart_;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            device = getDeviceForTests();
            const std::string basePath = tryGetRamDrivePath() + "/" + generateRandomString() + "_";
            dbName_ = basePath + "test_data_base.db";
            metadataPath_ = basePath + "clickdaemon_metadata.json";
            sessionIdPersistentPart_ = basePath + "clickdaemon_session_id_pers.txt";
            sessionIdTemporaryPart_ = basePath + "clickdaemon_session_id_temp.txt";

            consumerConfig["enabled"] = true;
            consumerConfig["endpointUri"] = "http://localhost:" + std::to_string(mockClickdaemon.httpEndpoint.start(getPort()));
            consumerConfig["eventsBatchSize"] = 5;
            consumerConfig["runtime"]["maxSizeKb"] = 1024 /*KB*/;
            consumerConfig["runtime"]["filename"] = dbName_;
            consumerConfig["immediateQueueSize"] = 5;
            consumerConfig["sendMetricaPeriodMs"] = 10;
            consumerConfig["sendDbSizePeriodMs"] = 10000;
            consumerConfig["eventWhitelist"] = Json::Value(Json::objectValue);
            consumerConfig["eventBlacklist"] = Json::Value(Json::objectValue);
            consumerConfig["metricaMetadataPath"] = metadataPath_;
            consumerConfig["metricaSessionIdPersistentPart"] = sessionIdPersistentPart_;
            consumerConfig["metricaSessionIdTemporaryPart"] = sessionIdTemporaryPart_;
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(dbName_.c_str());
            std::remove(metadataPath_.c_str());
            std::remove(sessionIdPersistentPart_.c_str());
            std::remove(sessionIdTemporaryPart_.c_str());

            Base::TearDown(context);
        }

        static void sendEvents(const EventBatch& events, ClickdaemonConsumer& consumer) {
            for (auto& event : events) {
                consumer.processEvent(event.at("MetricName"), event.at("MetricValue"));
            }
        }
    };

}; // namespace

Y_UNIT_TEST_SUITE_F(ClickdaemonConsumerTests, Fixture) {
    Y_UNIT_TEST(testMetricaEvent) {
        EventBatch events{
            {{"MetricName", "testEvent"}, {"MetricValue", ""}},
            {{"MetricName", "testEventWithValue"}, {"MetricValue", "imValue"}},
        };
        mockClickdaemon.prepareForEvents({events});

        ClickdaemonConsumer consumer(consumerConfig, device);
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testConnectionTypeIsSet) {
        ClickdaemonConsumer consumer(consumerConfig, device);

        consumer.setConnectionType(proto::CONNECTION_TYPE_WIFI);
        {
            EventBatch events{
                {{"MetricName", "testEvent_1"}, {"MetricValue", "testValue_1"}}};
            EventBatch expectedEvents{
                {{"MetricName", "testEvent_1"}, {"MetricValue", "testValue_1"}, {"ConnectionType", std::to_string(ClickdaemonSender::ConnectionType::CONNECTION_WIFI)}}};

            mockClickdaemon.prepareForEvents({expectedEvents});
            sendEvents(events, consumer);
            mockClickdaemon.waitForEvents();
        }

        consumer.setConnectionType(proto::CONNECTION_TYPE_ETHERNET);
        {
            EventBatch events{
                {{"MetricName", "testEvent_2"}, {"MetricValue", "testValue_2"}}};
            EventBatch expectedEvents{
                {{"MetricName", "testEvent_2"}, {"MetricValue", "testValue_2"}, {"ConnectionType", std::to_string(ClickdaemonSender::ConnectionType::CONNECTION_ETHERNET)}}};

            mockClickdaemon.prepareForEvents({expectedEvents});
            sendEvents(events, consumer);
            mockClickdaemon.waitForEvents();
        }
    }

    Y_UNIT_TEST(testMetricaError) {
        EventBatch events{
            {{"MetricName", "testError"}, {"MetricValue", ""}},
            {{"MetricName", "testSeriousError"}, {"MetricValue", "bad things happen"}},
        };
        mockClickdaemon.prepareForEvents({events});

        ClickdaemonConsumer consumer(consumerConfig, device);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testImmediateEvent) {
        std::string testEvent = "suchFastEvent";
        std::string testEventValue = "veryHeavyValue";
        EventBatch events{
            {{"MetricName", testEvent}, {"MetricValue", testEventValue}},
        };
        mockClickdaemon.prepareForEvents({events});

        ClickdaemonConsumer consumer(consumerConfig, device);
        consumer.processEvent(testEvent, testEventValue, true);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testEventWhitelist) {
        EventBatch events{
            {{"MetricName", "goodEvent1"}, {"MetricValue", ""}},
            {{"MetricName", "goodEvent2"}, {"MetricValue", ""}},
            {{"MetricName", "badEvent"}, {"MetricValue", ""}},
        };
        mockClickdaemon.prepareForEvents({{events[0], events[1]}});

        ClickdaemonConsumer consumer(consumerConfig, device);
        auto config = parseJson(R"({"eventWhitelist": "goodEvent1,goodEvent2"})");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testEventWhitelistDict) {
        EventBatch events{
            {{"MetricName", "goodEvent1"}, {"MetricValue", ""}},
            {{"MetricName", "goodEvent2"}, {"MetricValue", ""}},
            {{"MetricName", "badEvent"}, {"MetricValue", ""}},
        };
        mockClickdaemon.prepareForEvents({{events[0], events[1]}});

        ClickdaemonConsumer consumer(consumerConfig, device);
        auto config = parseJson(R"({"eventWhitelist": {"goodEvent1": true, "goodEvent2": true, "badEvent": false}})");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testEventBlacklist) {
        EventBatch events{
            {{"MetricName", "goodEvent1"}, {"MetricValue", ""}},
            {{"MetricName", "goodEvent2"}, {"MetricValue", ""}},
            {{"MetricName", "badEvent"}, {"MetricValue", ""}},
        };
        mockClickdaemon.prepareForEvents({{events[0], events[1]}});

        ClickdaemonConsumer consumer(consumerConfig, device);
        auto config = parseJson(R"({"eventBlacklist": "badEvent"})");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testEventBlacklistDict) {
        EventBatch events{
            {{"MetricName", "goodEvent1"}, {"MetricValue", ""}},
            {{"MetricName", "goodEvent2"}, {"MetricValue", ""}},
            {{"MetricName", "badEvent"}, {"MetricValue", ""}},
        };
        mockClickdaemon.prepareForEvents({{events[0], events[1]}});

        ClickdaemonConsumer consumer(consumerConfig, device);
        auto config = parseJson(R"({"eventBlacklist": {"badEvent": true, "goodEvent1": false}})");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testEventBatchSize) {
        constexpr size_t eventCount = 19;
        constexpr size_t batchSize = 7;
        constexpr size_t batchCount = eventCount / batchSize + (eventCount % batchSize ? 1 : 0);
        Events events{{}};
        for (size_t i = 0; i < eventCount; ++i) {
            events.back().push_back({{"MetricName", "testEvent" + std::to_string(i)}, {"MetricValue", ""}});
        }
        mockClickdaemon.prepareForEvents(events);

        auto immediateQueue = std::make_shared<MetricaBlockingQueue>(getUInt64(consumerConfig, "immediateQueueSize"));
        auto dbQueue = quasar::createEventsDatabase(consumerConfig, nullptr);
        ClickdaemonSender sender(getString(consumerConfig, "endpointUri"), batchSize, immediateQueue, dbQueue, ClickdaemonMetadata{makeUUID()}, device);
        for (auto& batch : events) {
            for (auto& event : batch) {
                DatabaseMetricaEvent dbevent;
                auto newEvent = dbevent.mutable_new_event();
                newEvent->set_type(DatabaseMetricaEvent::NewEvent::CLIENT);
                newEvent->set_timestamp(std::time(nullptr));
                newEvent->set_name(TString(event["MetricName"]));
                newEvent->set_value(TString(event["MetricValue"]));
                newEvent->set_session_id(1);
                newEvent->set_serial_number(2);
                dbQueue->addEvent(dbevent);
            }
        }
        while (sender.flushEvents()) {
        }
        mockClickdaemon.waitForEvents();
        UNIT_ASSERT_VALUES_EQUAL(mockClickdaemon.gotBatches.load(), batchCount);
    }

    Y_UNIT_TEST(testEventsWhitelistAndBlacklist) {
        EventBatch events{
            {{"MetricName", "goodEvent1"}, {"MetricValue", ""}},
            {{"MetricName", "goodEvent2"}, {"MetricValue", ""}},
            {{"MetricName", "badEvent"}, {"MetricValue", ""}},
        };
        mockClickdaemon.prepareForEvents({{events[1]}});

        ClickdaemonConsumer consumer(consumerConfig, device);
        auto config = parseJson(R"({
        "eventWhitelist": {"goodEvent1": true ,"goodEvent2": true}
    })");
        consumer.processConfigUpdate(config, config);
        config = parseJson(R"({
        "eventBlacklist": {"goodEvent1": true}
    })");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testMetricaDisabled) {
        consumerConfig["enabled"] = false;
        consumerConfig["endpointUri"] = "not_reachable";

        ClickdaemonConsumerWithChecks consumer(consumerConfig, device);
        const auto dbEventBefore = consumer.getNextDbEvent();
        UNIT_ASSERT(!dbEventBefore);

        consumer.processEvent("testEvent", "shouldBeSkipped");
        consumer.processEvent("testImmediateEvent", "shouldBeSkipped", true);

        UNIT_ASSERT_VALUES_EQUAL(consumer.getQueueSize(), 0);
        const auto dbEventAfter = consumer.getNextDbEvent();
        UNIT_ASSERT(!dbEventAfter);
    }

    Y_UNIT_TEST(testMetricaEnvironment) {
        consumerConfig["enabled"] = false;
        ClickdaemonConsumer consumer(consumerConfig, device);

        consumer.putEnvironmentVariable("env1", "val1");
        consumer.putEnvironmentVariable("env2", "val2");
        consumer.putEnvironmentVariable("env1", "val3");
        consumer.deleteEnvironmentVariable("env2");

        const auto env = R"({"env1":"val3","session_id_ordered":"1"})";
        EventBatch events{
            {{"MetricName", "testEvent"}, {"MetricValue", ""}, {"Environment", env}},
        };
        mockClickdaemon.prepareForEvents({events});

        auto config = parseJson(R"({"enabled": true})");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testMetricaQuasmodrom) {
        Event event1{
            {"MetricName", "testEvent1"},
            {"MetricValue", ""},
            {"QuasmodromGroup", "unknown"},
            {"QuasmodromSubgroup", ""},
        };
        Event event2{
            {"MetricName", "testEvent2"},
            {"MetricValue", ""},
            {"QuasmodromGroup", "test"},
            {"QuasmodromSubgroup", ""},
        };
        Event event3{
            {"MetricName", "testEvent3"},
            {"MetricValue", ""},
            {"QuasmodromGroup", "test"},
            {"QuasmodromSubgroup", "subtest"},
        };

        mockClickdaemon.prepareForEvents({{event1}, {event2}, {event3}});
        ClickdaemonConsumer consumer(consumerConfig, device);

        consumer.processEvent(event1.at("MetricName"), event1.at("MetricValue"));
        consumer.putEnvironmentVariable("quasmodrom_group", "test");
        consumer.processEvent(event2.at("MetricName"), event2.at("MetricValue"));
        consumer.putEnvironmentVariable("quasmodrom_subgroup", "subtest");
        consumer.processEvent(event3.at("MetricName"), event3.at("MetricValue"));

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testMetricaFields) {
        auto deviceId = getDeviceForTests() -> deviceId();
        auto platform = getDeviceForTests() -> configuration() -> getDeviceType();
        auto softwareVersion = getDeviceForTests() -> softwareVersion();

        EventBatch events{
            {
                {"MetricName", "event"},
                {"MetricValue", ""},
                {"DeviceId", deviceId},
                {"Platform", platform},
                {"SoftwareVersion", softwareVersion},
            },
        };
        mockClickdaemon.prepareForEvents({events});

        ClickdaemonConsumer consumer(consumerConfig, device);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testMetricaUUID) {
        // assert metadata file doesn't exist
        UNIT_ASSERT_EXCEPTION(readJsonFromFile(metadataPath_), std::runtime_error);

        {
            // read unexisting metadata and create it
            ClickdaemonConsumer consumer(consumerConfig, device);
        }

        auto json = readJsonFromFile(metadataPath_);
        auto metadata = ClickdaemonMetadata::fromJson(json);
        UNIT_ASSERT(!metadata.UUID.empty());

        EventBatch events{
            {{"MetricName", "testEvent"}, {"MetricValue", ""}, {"UUID", metadata.UUID}},
        };
        mockClickdaemon.prepareForEvents({events});

        ClickdaemonConsumer consumer(consumerConfig, device);
        sendEvents(events, consumer);

        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testMetricaSessionId) {
        MetricaSessionProvider sessionProvider(sessionIdPersistentPart_, sessionIdTemporaryPart_);
        auto session = sessionProvider.getAndIncrementSession();

        std::stringstream ss;
        ss << session.id + 1;
        auto nextSession = ss.str();

        EventBatch events{
            {{"MetricName", "testEvent1"}, {"MetricValue", ""}, {"SessionId", nextSession}, {"EventNumber", "1"}},
            {{"MetricName", "testEvent2"}, {"MetricValue", ""}, {"SessionId", nextSession}, {"EventNumber", "2"}},
            {{"MetricName", "testEvent3"}, {"MetricValue", ""}, {"SessionId", nextSession}, {"EventNumber", "3"}},
        };
        mockClickdaemon.prepareForEvents({events});

        ClickdaemonConsumer consumer(consumerConfig, device);
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();
    }

    Y_UNIT_TEST(testMetricaEnvironmentBlacklist) {
        consumerConfig["enabled"] = false;
        ClickdaemonConsumer consumer(consumerConfig, device);

        consumer.putEnvironmentVariable("env1", "1");
        consumer.putEnvironmentVariable("env2", "2");

        EventBatch events{
            {{"MetricName", "testEvent"}, {"MetricValue", ""}, {"Environment", R"({"env1":"1","env2":"2","session_id_ordered":"1"})"}},
        };
        mockClickdaemon.prepareForEvents({events});

        auto config = parseJson(R"({"enabled": true})");
        consumer.processConfigUpdate(config, config);
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();

        config = parseJson(R"({"envKeysBlacklist": {"env2": true, "env3": true}})");
        consumer.processConfigUpdate(config, config);
        EventBatch eventsWithBlacklist{
            {{"MetricName", "testEvent"}, {"MetricValue", ""}, {"Environment", R"({"env1":"1","session_id_ordered":"1"})"}},
        };
        mockClickdaemon.prepareForEvents({eventsWithBlacklist});
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();

        consumer.putEnvironmentVariable("env3", "3");
        mockClickdaemon.prepareForEvents({eventsWithBlacklist});
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();

        config = parseJson(R"({"envKeysBlacklist": {}})");
        consumer.processConfigUpdate(config, config);
        EventBatch eventsWithoutBlacklist{
            {{"MetricName", "testEvent"}, {"MetricValue", ""}, {"Environment", R"({"env1":"1","env2":"2","env3":"3","session_id_ordered":"1"})"}},
        };
        mockClickdaemon.prepareForEvents({eventsWithoutBlacklist});
        sendEvents(events, consumer);
        mockClickdaemon.waitForEvents();
    }
}
