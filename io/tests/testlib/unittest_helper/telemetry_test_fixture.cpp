#include "telemetry_test_fixture.h"

#include <yandex_io/libs/telemetry/null/null_metrica.h>

using namespace quasar;

namespace {

    std::unique_ptr<YandexIO::Device> makeDevice(std::shared_ptr<YandexIO::ITelemetry> telemetry) {
        return std::make_unique<YandexIO::Device>(
            QuasarUnitTestFixture::makeTestDeviceId(),
            QuasarUnitTestFixture::makeTestConfiguration(),
            std::move(telemetry),
            QuasarUnitTestFixture::makeTestHAL());
    }

} // namespace

class TelemetryTestFixture::Telemetry: public NullMetrica {
public:
    Telemetry(TelemetryTestFixture& fixture)
        : fixture_(fixture)
    {
    }

    void reportEvent(const std::string& event, ITelemetry::Flags flags = 0) override {
        if (fixture_.eventListener_) {
            fixture_.eventListener_(event, std::string(), flags);
        }
    }

    void reportEvent(const std::string& event, const std::string& eventJson, ITelemetry::Flags flags = 0) override {
        if (fixture_.eventListener_) {
            fixture_.eventListener_(event, eventJson, flags);
        }
    }

    void reportKeyValues(const std::string& eventName, const std::unordered_map<std::string, std::string>& keyValues, ITelemetry::Flags flags = 0) override {
        if (fixture_.keyValueListener_) {
            fixture_.keyValueListener_(eventName, keyValues, flags);
        }
    }

    void putAppEnvironmentValue(const std::string& key, const std::string& value) override {
        if (fixture_.appEnvironmentListener_) {
            fixture_.appEnvironmentListener_(key, value);
        }
    }

private:
    TelemetryTestFixture& fixture_;
};

void TelemetryTestFixture::SetUp(NUnitTest::TTestContext& context) {
    Base::SetUp(context);
    setDeviceForTests(makeDevice(std::make_shared<Telemetry>(*this)));
}

void TelemetryTestFixture::TearDown(NUnitTest::TTestContext& context) {
    Base::TearDown(context);
}

void TelemetryTestFixture::setEventListener(EventListener listener) {
    eventListener_ = std::move(listener);
}

void TelemetryTestFixture::setKeyValueListener(KeyValueListener listener) {
    keyValueListener_ = std::move(listener);
}

void TelemetryTestFixture::setAppEnvironmentListener(AppEnvironmentListener listener) {
    appEnvironmentListener_ = std::move(listener);
}
