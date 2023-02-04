#include <library/cpp/testing/unittest/registar.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>

#include <util/folder/tempdir.h>
#include <util/system/tempfile.h>

#include <yandex_io/metrica/teleme3d/teleme3d_endpoint.h>

#include <future>

using namespace quasar;
using namespace YandexIO;

namespace {
    struct TempDirWrapper {
        TTempDir tempDir;

        TempDirWrapper(const std::string& path)
            : tempDir(TTempDir::NewTempDir(TString(path)))
        {
        }
    };

    auto buildMetricaMessage(auto builder) {
        return quasar::ipc::buildMessage([&builder](auto& msg) {
            auto metricaMessage = msg.mutable_metrica_message();
            builder(*metricaMessage);
        });
    }

    class TestSinksConfigurer: public Teleme3dEndpoint::SinksConfigurer {
    public:
        struct Data {
            std::mutex mutex;
            std::condition_variable cond;

            int32_t offsetSec{0};
            double lat{0.0};
            double lon{0.0};
            Json::Value config;
            std::shared_ptr<quasar::ICallbackQueue> queue;
            std::vector<std::tuple<EventsDB::Event, EventsDB::Environment>> events;

            template <typename Fn_>
            void wait(Fn_ fn) {
                std::unique_lock lock(mutex);
                cond.wait(lock, [this, &fn]() -> bool {
                    return fn(*this);
                });
            }

            void clearEvents() {
                std::scoped_lock lock(mutex);
                events.clear();
            }
        };
        Data& data;

        class TestSink: public EventsDB::Sink {
            Data& data;

        public:
            TestSink(Data& d)
                : data(d)
            {
            }

            void handleDbEvent(EventsDB::ISourceControl& ctrl, EventsDB::Event event, EventsDB::Environment env) override {
                YIO_LOG_INFO("Event came to sink: " << (event.has_name() ? event.name() : ""));
                {
                    std::scoped_lock lock(data.mutex);
                    data.events.emplace_back(std::move(event), std::move(env));
                }
                data.cond.notify_all();
                ctrl.releaseIncludingLast();
                ctrl.readyForNext();
            }
        };

        TestSinksConfigurer(Data& d)
            : data(d)
        {
        }

        void registerSinks(const std::shared_ptr<YandexIO::IDevice>& /*device*/, const std::shared_ptr<YandexIO::ITelemetryEventsDB>& eventsDb) override {
            auto ctrl = eventsDb->registerSink(0, std::make_shared<TestSink>(data), data.queue, YandexIO::EventsDB::EventsFilter{.isWhiteList = false});
            ctrl->readyForNext();
        }

        void updateConfig(const Json::Value& config) override {
            data.config = config;
        }

        void setTimezoneOffsetSec(int32_t offsetSec) override {
            data.offsetSec = offsetSec;
        }

        void setLocation(double lat, double lon) override {
            data.lat = lat;
            data.lon = lon;
        }
    };

    struct Teleme3dTestFixture: public QuasarUnitTestFixture {
        using Base = QuasarUnitTestFixture;

        static Json::Value vecToJson(const std::vector<std::string>& src) {
            return vectorToJson(src);
        }

        static Json::Value makeTestPriorities() {
            Json::Value result = Json::objectValue;
            result["lowest"] = vecToJson({"lowest1", "lowest2"});
            result["low"] = vecToJson({"low1"});
            ;
            result["high"] = vecToJson({"hi1", "hi2", "hi3"});
            ;
            result["highest"] = vecToJson({"top1"});
            ;
            return result;
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            dbDir = std::make_unique<TempDirWrapper>(quasar::TestUtils::tryGetRamDrivePath());
            Json::Value cfg = Json::objectValue;
            cfg["dbPath"] = dbDir->tempDir.Name();
            cfg["dbSizeLimitMB"] = 1.5;
            cfg["priorities"] = makeTestPriorities();
            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            auto& metricadCfg = config["metricad"];
            metricadCfg["teleme3d"] = cfg;
            metricadCfg["metricaSessionIdPersistentPart"] = metricaSessionIdPersistentPart.Name();
            metricadCfg["metricaSessionIdTemporaryPart"] = metricaSessionIdTemporaryPart.Name();

            teleme3dData.queue = std::make_shared<quasar::NamedCallbackQueue>("testSink", 10);

            teleme3d = std::make_unique<Teleme3dEndpoint>(getDeviceForTests(), ipcFactoryForTests(), std::make_unique<TestSinksConfigurer>(teleme3dData));
            connector = ipcFactoryForTests()->createIpcConnector("metricad");
            connector->connectToService();
            connector->waitUntilConnected();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        template <typename Fn>
        void waitEventEnv(Fn cb) {
            teleme3dData.wait([&cb](const auto& data) {
                if (!data.events.empty()) {
                    return cb(std::get<0>(data.events.back()), std::get<1>(data.events.back()));
                }
                return false;
            });
        }

        template <typename Fn>
        void waitEvent(Fn cb) {
            waitEventEnv([&cb](const EventsDB::Event& event, const EventsDB::Environment& /*env*/) {
                return cb(event);
            });
        }

        void buildAndSendMetricaMessage(auto builder) {
            auto message = buildMetricaMessage(builder);
            connector->sendMessage(message);
        }

        YandexIO::Configuration::TestGuard testGuard;
        TTempFile metricaSessionIdPersistentPart{MakeTempName()};
        TTempFile metricaSessionIdTemporaryPart{MakeTempName()};

        std::unique_ptr<TempDirWrapper> dbDir;
        TestSinksConfigurer::Data teleme3dData;
        std::unique_ptr<Teleme3dEndpoint> teleme3d;
        std::shared_ptr<ipc::IConnector> connector;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(TestTeleme3dEndpoint, Teleme3dTestFixture) {
    Y_UNIT_TEST(eventMessages) {
        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_event("test");
            metricaMessage.set_report_event_json_value("{}");
        });

        waitEvent([](const EventsDB::Event& event) {
            return event.name() == "test" && event.value() == "{}" && event.type() == EventsDB::Event::CLIENT;
        });

        teleme3dData.clearEvents();

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_error("testError");
            metricaMessage.set_report_error_value("[]");
        });

        waitEvent([](const EventsDB::Event& event) {
            return event.name() == "testError" && event.value() == "[]" && event.type() == EventsDB::Event::ERROR;
        });

        teleme3dData.clearEvents();

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            auto kv = metricaMessage.mutable_report_key_value();
            kv->set_event_name("testKV");
            kv->mutable_key_values()->insert({TString("a"), TString("b")});
        });

        waitEvent([](const EventsDB::Event& event) {
            return event.name() == "testKV" && event.value() == "{\"a\":\"b\"}\n" && event.type() == EventsDB::Event::CLIENT;
        });
    }

    Y_UNIT_TEST(environmentUpdates) {
        buildAndSendMetricaMessage([](auto& metricaMessage) {
            auto env = metricaMessage.mutable_app_environment_value();
            env->set_key("key");
            env->set_value("val");
        });
        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_event("test");
        });

        waitEventEnv([](const EventsDB::Event& event, const EventsDB::Environment& env) {
            const auto& vals = env.environment_values();
            if (auto iter = vals.find("key"); iter != vals.end()) {
                return event.name() == "test" && iter->second == "val";
            }
            return false;
        });
        teleme3dData.clearEvents();

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_delete_environment_key("key");
        });
        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_event("test2");
            metricaMessage.set_report_event_json_value("{}");
        });

        waitEventEnv([](const EventsDB::Event& event, const EventsDB::Environment& env) {
            const auto& vals = env.environment_values();
            return event.name() == "test2" && vals.find("key") == vals.end();
        });
    }

    Y_UNIT_TEST(timeZone) {
        buildAndSendMetricaMessage([](auto& metricaMessage) {
            auto tz = metricaMessage.mutable_timezone();
            tz->set_timezone_name("Asia/Tomsk");
            tz->set_timezone_offset_sec(7 * 60 * 60);
        });

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_event("tztest");
        });

        waitEventEnv([](const EventsDB::Event& event, const EventsDB::Environment& env) {
            const auto& vals = env.environment_values();
            if (auto iter = vals.find("timezone"); iter != vals.end()) {
                return event.name() == "tztest" && iter->second == "Asia/Tomsk";
            }
            return false;
        });

        UNIT_ASSERT_EQUAL(teleme3dData.offsetSec, 7 * 60 * 60);
    }

    Y_UNIT_TEST(netLocation) {
        const double lat = 49.3328976;
        const double lon = 17.5927929;

        buildAndSendMetricaMessage([&lat, &lon](auto& metricaMessage) {
            auto location = metricaMessage.mutable_location();
            location->set_latitude(lat);
            location->set_longitude(lon);
        });

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.mutable_network_status()->set_type(proto::ConnectionType::CONNECTION_TYPE_WIFI);
        });

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_event("nettest");
        });

        waitEvent([](const EventsDB::Event& event) {
            return event.name() == "nettest" && event.connection_type() == proto::ConnectionType::CONNECTION_TYPE_WIFI;
        });

        UNIT_ASSERT_EQUAL(teleme3dData.lon, lon);
        UNIT_ASSERT_EQUAL(teleme3dData.lat, lat);

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.mutable_network_status()->set_type(proto::ConnectionType::CONNECTION_TYPE_ETHERNET);
        });

        buildAndSendMetricaMessage([](auto& metricaMessage) {
            metricaMessage.set_report_event("nettest2");
        });

        waitEvent([](const EventsDB::Event& event) {
            return event.name() == "nettest2" && event.connection_type() == proto::ConnectionType::CONNECTION_TYPE_ETHERNET;
        });
    }
}
