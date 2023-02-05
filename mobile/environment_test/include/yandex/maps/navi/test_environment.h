#pragma once

#include <yandex/maps/navi/environment_config.h>
#include <yandex/maps/navi/test_environment_config.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/handle.h>

#define UI(expr) runtime::async::ui()->spawn([&] { expr; }).wait()

namespace yandex::maps::navikit {

class TestEnvironment {
public:
    virtual ~TestEnvironment() {}

    virtual const EnvironmentConfig* config() const = 0;
    virtual runtime::Handle setConfig(std::unique_ptr<TestEnvironmentConfig> config) = 0;
};

TestEnvironment* getTestEnvironment();

}  // namespace yandex
