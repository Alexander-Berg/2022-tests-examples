#pragma once

#include "unit_test_fixture.h"

class TelemetryTestFixture: public virtual QuasarUnitTestFixture {
public:
    using Base = QuasarUnitTestFixture;

    using EventListener = std::function<void(const std::string&, const std::string&, YandexIO::ITelemetry::Flags)>;
    using KeyValueListener = std::function<void(const std::string&, const std::unordered_map<std::string, std::string>&, YandexIO::ITelemetry::Flags)>;
    using AppEnvironmentListener = std::function<void(const std::string&, const std::string&)>;

    void SetUp(NUnitTest::TTestContext& context) override;
    void TearDown(NUnitTest::TTestContext& context) override;

    void setEventListener(EventListener listener);
    void setKeyValueListener(KeyValueListener listener);
    void setAppEnvironmentListener(AppEnvironmentListener listener);

private:
    class Telemetry;

    EventListener eventListener_;
    KeyValueListener keyValueListener_;
    AppEnvironmentListener appEnvironmentListener_;
};
