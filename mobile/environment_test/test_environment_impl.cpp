#include "environment_config_impl.h"

#include <yandex/maps/navikit/check_context.h>
#include <yandex/maps/navi/test_environment.h>

#include <yandex/maps/runtime/assert.h>
#include <yandex/maps/runtime/singleton.h>

namespace yandex::maps::navikit {

namespace {

class TestEnvironmentImpl : public TestEnvironment {
public:
    TestEnvironmentImpl()
        : config_(nullptr)
        , defaultConfig_(std::make_unique<EnvironmentConfigImpl>())
    {}

    virtual const EnvironmentConfig* config() const override
    {
        assertUi();

        if (!config_) {
            return defaultConfig_.get();
        }

        return config_.get();
    }

    virtual runtime::Handle setConfig(std::unique_ptr<TestEnvironmentConfig> config) override
    {
        assertUi();
        ASSERT(!config_);

        config_ = std::move(config);

        return runtime::Handle([&] {
            config_.reset();
        });
    }

private:
    std::unique_ptr<TestEnvironmentConfig> config_;
    const std::unique_ptr<EnvironmentConfigImpl> defaultConfig_;
};

} // anonymous namespace

TestEnvironment* getTestEnvironment()
{
    return &runtime::singleton<TestEnvironmentImpl>();
}

}  // namespace yandex
