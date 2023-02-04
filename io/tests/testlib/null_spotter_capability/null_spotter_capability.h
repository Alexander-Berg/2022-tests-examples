#pragma once

#include <yandex_io/capabilities/spotter/interfaces/i_spotter_capability.h>

namespace YandexIO {

    class NullSpotterCapability: public ISpotterCapability {
    public:
        void setModelPaths(const std::map<std::string, std::string>& spotterTypeToModelPath) override;
        void setSpotterWord(const std::string& spotterWord) override;
        void addListener(std::weak_ptr<IListener> listener) override;
        void removeListener(std::weak_ptr<IListener> listener) override;
    };

} // namespace YandexIO
