#include "unit_test_fixture.h"

#include <yandex_io/tests/testlib/test_hal.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/ipc/datacratic/datacratic_ipc_factory.h>
#include <yandex_io/libs/ipc/mixed/mixed_ipc_factory.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/logging/setup/setup.h>
#include <yandex_io/libs/telemetry/telemetry.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>

#include <library/cpp/testing/common/env.h>

#include <util/system/env.h>
#include <util/system/yassert.h>

#include <memory>

using namespace quasar;

namespace {
    std::unique_ptr<YandexIO::Device> makeTestDevice() {
        return std::make_unique<YandexIO::Device>(
            QuasarUnitTestFixture::makeTestDeviceId(),
            QuasarUnitTestFixture::makeTestConfiguration(),
            std::make_shared<NullMetrica>(),
            QuasarUnitTestFixture::makeTestHAL());
    }

    class PortAllocatingIpcFactory: public ipc::IIpcFactory {
        std::shared_ptr<YandexIO::Configuration> configuration_;
        std::shared_ptr<TPortManager> portManager_;
        std::shared_ptr<ipc::IIpcFactory> realFactory_;
        std::mutex configMutex_;

    public:
        PortAllocatingIpcFactory(std::shared_ptr<YandexIO::Configuration> configuration, std::shared_ptr<TPortManager> portManager, std::shared_ptr<ipc::IIpcFactory> realFactory)
            : configuration_(std::move(configuration))
            , portManager_(std::move(portManager))
            , realFactory_(std::move(realFactory))
        {
        }

        std::shared_ptr<ipc::IServer> createIpcServer(const std::string& serviceName) override {
            ensurePortAllocated(serviceName);
            return realFactory_->createIpcServer(serviceName);
        }

        std::shared_ptr<ipc::IConnector> createIpcConnector(const std::string& serviceName) override {
            ensurePortAllocated(serviceName);
            return realFactory_->createIpcConnector(serviceName);
        }

    private:
        void ensurePortAllocated(const std::string& serviceName) {
            auto lock = std::scoped_lock{configMutex_};

            YandexIO::Configuration::TestGuard testGuard;
            auto& config = configuration_->getMutableConfig(testGuard);
            YIO_LOG_DEBUG("Config for " << serviceName << " = " << jsonToString(config[serviceName]));
            if (auto& portConfig = config[serviceName]["port"]; !portConfig.isInt()) {
                portConfig = portManager_->GetPort();
                YIO_LOG_INFO("IPC port for " << serviceName << " = " << portConfig.asInt());
            } else {
                YIO_LOG_INFO("IPC port for " << serviceName << " = " << portConfig.asInt() << " (pre-allocated)");
            }
        }
    };

} /* anonymous namespace */

void QuasarUnitTestFixtureWithoutIpc::SetUp(NUnitTest::TTestContext& context) {
    Base::SetUp(context);

    setDeviceForTests(makeTestDevice());
}

void QuasarUnitTestFixtureWithoutIpc::setDeviceForTests(std::shared_ptr<YandexIO::IDevice> device) {
    device_ = std::move(device);
}

void QuasarUnitTestFixtureWithoutIpc::enableLoggingToTelemetry(std::shared_ptr<YandexIO::ITelemetry> telemetry) {
    quasar::Logging::addLoggingToTelemetryIfNeeded(Json::Value{}, std::move(telemetry));
}

std::shared_ptr<YandexIO::IDevice> QuasarUnitTestFixtureWithoutIpc::getDeviceForTests() const {
    return device_;
}

int QuasarUnitTestFixtureWithoutIpc::getPort() {
    const int port = portManager_->GetPort();
    YIO_LOG_INFO("Allocate port via portManager: " << port);
    return port;
}

std::string QuasarUnitTestFixtureWithoutIpc::makeTestDeviceId() {
    return "DEVICE_ID";
}

std::shared_ptr<YandexIO::Configuration> QuasarUnitTestFixtureWithoutIpc::makeTestConfiguration() {
    YandexIO::ConfigPatterns extra;
    extra["SOURCE_ROOT"] = ArcadiaSourceRoot();
    return YandexIO::makeConfiguration(ArcadiaSourceRoot() + "/yandex_io/misc/configs/tests.cfg", extra);
}

std::unique_ptr<YandexIO::HAL> QuasarUnitTestFixtureWithoutIpc::makeTestHAL() {
    return std::make_unique<TestHAL>();
}

std::string QuasarUnitTestFixtureWithoutIpc::getRamDrivePath() {
    return GetRamDrivePath();
}

std::string QuasarUnitTestFixtureWithoutIpc::tryGetRamDrivePath() {
    return quasar::TestUtils::tryGetRamDrivePath();
}

void QuasarUnitTestFixture::SetUp(NUnitTest::TTestContext& context) {
    Base::SetUp(context);
}

void QuasarUnitTestFixture::TearDown(NUnitTest::TTestContext& context) {
    // Stop mock servers
    for (auto& item : mockIpcServers_) {
        item.second->shutdown();
        item.second = nullptr;
    }

    Base::TearDown(context);
}

std::shared_ptr<TestIpcFactory> QuasarUnitTestFixture::ipcFactoryForTests() {
    ensureIpcFactoryInitialized();
    return ipcFactory_;
}

std::shared_ptr<quasar::ipc::IConnector> QuasarUnitTestFixture::createIpcConnectorForTests(std::string serviceName) {
    ensureIpcFactoryInitialized();
    return ipcFactory_->createIpcConnector(serviceName);
}

std::shared_ptr<quasar::ipc::IServer> QuasarUnitTestFixture::createIpcServerForTests(std::string serviceName) {
    ensureIpcFactoryInitialized();
    return ipcFactory_->createIpcServer(serviceName);
}

void QuasarUnitTestFixture::startMockIpcServers(const std::vector<std::string>& serviceNames) {
    std::vector<ipc::IServer*> newServers;
    newServers.reserve(serviceNames.size());

    for (const auto& serviceName : serviceNames) {
        auto& server = mockIpcServers_[serviceName];
        if (server == nullptr) {
            YIO_LOG_INFO("Starting mock IPC server for " << serviceName);
            server = createIpcServerForTests(serviceName);
            server->listenService();
            newServers.push_back(server.get());
        }
    }

    for (auto* server : newServers) {
        server->waitListening();
    }
}

void QuasarUnitTestFixture::ensureIpcFactoryInitialized() {
    // use lazy initialization to allow tests to setup custom device
    if (ipcFactory_ == nullptr) {
        const auto configuration = device_->sharedConfiguration();
        const auto ipcParam = GetTestParam("ipc", "mixed");
        YIO_LOG_INFO("Use " << ipcParam << " ipc factory for tests");
        if (ipcParam == "mixed") {
            using quasar::ipc::MixedIpcFactory;
            auto context = std::make_shared<MixedIpcFactory::Context>(
                MixedIpcFactory::Context{
                    .configuration = configuration,
                    .transport = MixedIpcFactory::Transport::LOCAL,
                });
            ipcFactory_ = std::make_shared<TestIpcFactory>(std::make_shared<quasar::ipc::MixedIpcFactory>(std::move(context)));
        } else if (ipcParam == "datacratic") {
            auto realIpcFactory = std::make_shared<ipc::DatacraticIpcFactory>(configuration);
            ipcFactory_ = std::make_shared<TestIpcFactory>(std::make_shared<PortAllocatingIpcFactory>(configuration, portManager_, realIpcFactory));
        } else {
            // abort
            Y_FAIL("Invalid \"ipc\" param");
        }
    }
}

void QuasarUnitTestFixture::setDeviceForTests(std::shared_ptr<YandexIO::IDevice> device) {
    // do not allow to setup device when ipcFactory was already created via lazy initialization
    Y_VERIFY(ipcFactory_ == nullptr);
    QuasarUnitTestFixtureWithoutIpc::setDeviceForTests(std::move(device));
}
