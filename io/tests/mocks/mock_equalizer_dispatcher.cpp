#include "mock_equalizer_dispatcher.h"

MockFixedBandsEqualizerDispatcher::MockFixedBandsEqualizerDispatcher() {
    ON_CALL(*this, getFixedBandsConfiguration()).WillByDefault(testing::Return(YandexIO::EqualizerDispatcher::BandsConfiguration{5}));
}

MockAdjustableBandsEqualizerDispatcher::MockAdjustableBandsEqualizerDispatcher() {
    ON_CALL(*this, getFixedBandsConfiguration()).WillByDefault(testing::Return(YandexIO::EqualizerDispatcher::BandsConfiguration{}));
}

MockEqualizerDispatcherFactory::MockEqualizerDispatcherFactory() {
    ON_CALL(*this, createDispatcher).WillByDefault([this](const std::string& equalizerDispatcherType) -> EqualizerDispatcherPtr {
        resetDispatchers();

        if (equalizerDispatcherType == FIXED_BANDS_EQUALIZER_TYPE) {
            auto fixedBandsDispatcher = std::make_unique<MockFixedBandsEqualizerDispatcher>();
            fixedBandsDispatcher_ = fixedBandsDispatcher.get();
            return fixedBandsDispatcher;
        } else if (equalizerDispatcherType == ADJUSTABLE_BANDS_EQUALIZER_TYPE) {
            auto adjustableBandsDispatcher = std::make_unique<MockAdjustableBandsEqualizerDispatcher>();
            adjustableBandsDispatcher_ = adjustableBandsDispatcher.get();
            return adjustableBandsDispatcher;
        }

        [] { FAIL() << "Invalid equalizer dispatcher type"; }();
        return nullptr;
    });
}

void MockEqualizerDispatcherFactory::resetDispatchers() {
    fixedBandsDispatcher_ = nullptr;
    adjustableBandsDispatcher_ = nullptr;
}
