#pragma once

#include <yandex_io/modules/equalizer_controller/dispatcher/equalizer_dispatcher.h>
#include <yandex_io/modules/equalizer_controller/dispatcher/factory/equalizer_dispatcher_factory.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

class MockEqualizerDispatcher: public YandexIO::EqualizerDispatcher {
public:
    MOCK_METHOD(void, setUserConfig, (YandexIO::EqualizerConfig), (override));
    MOCK_METHOD(void, setSmartConfig, (YandexIO::EqualizerConfig), (override));
    MOCK_METHOD(BandsConfiguration, getFixedBandsConfiguration, (), (const, override));
};

class MockFixedBandsEqualizerDispatcher: public MockEqualizerDispatcher {
public:
    MockFixedBandsEqualizerDispatcher();
};

class MockAdjustableBandsEqualizerDispatcher: public MockEqualizerDispatcher {
public:
    MockAdjustableBandsEqualizerDispatcher();
};

class MockEqualizerDispatcherFactory: public YandexIO::IEqualizerDispatcherFactory {
public:
    static constexpr auto FIXED_BANDS_EQUALIZER_TYPE = "fixedBands";
    static constexpr auto ADJUSTABLE_BANDS_EQUALIZER_TYPE = "adjustableBands";

    explicit MockEqualizerDispatcherFactory();

    MOCK_METHOD(EqualizerDispatcherPtr, createDispatcher, (const std::string&), (override));

    MockEqualizerDispatcher* fixedBandsDispatcher_ = nullptr;
    MockEqualizerDispatcher* adjustableBandsDispatcher_ = nullptr;

private:
    void resetDispatchers();
};
