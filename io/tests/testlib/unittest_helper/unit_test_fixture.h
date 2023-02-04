#pragma once

#include "test_ipc_factory/test_ipc_factory.h"

#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/hal/hal.h>
#include <yandex_io/libs/ipc/i_ipc_factory.h>
#include <yandex_io/libs/telemetry/telemetry.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include "logging_test_fixture.h"

/**
 * @brief Fixture that can be used Y_UNIT_TEST_SUITE_F. This fixture prepare YandexIO::Device
 *        and init/deinit logger
 */
struct QuasarUnitTestFixtureWithoutIpc: public QuasarLoggingTestFixture {
    using Base = QuasarLoggingTestFixture;

    void SetUp(NUnitTest::TTestContext& context) override;

    /* helper to create test device id */
    static YandexIO::DeviceID makeTestDeviceId();
    static std::shared_ptr<YandexIO::Configuration> makeTestConfiguration();
    static std::unique_ptr<YandexIO::HAL> makeTestHAL();

    std::shared_ptr<YandexIO::IDevice> getDeviceForTests() const;
    virtual void setDeviceForTests(std::shared_ptr<YandexIO::IDevice> device);

    static void enableLoggingToTelemetry(std::shared_ptr<YandexIO::ITelemetry> telemetry);

    /**
     * @brief Allocate 1 port
     */
    int getPort();

    /**
     * @brief Returns ram drive path
     *
     * Works only in distbuild and sandbox environments, returns empty string if tests are run locally
     * Needs `ram_disk` to be defined in ya.make in `REQUIREMENTS`
     */
    static std::string getRamDrivePath();

    /**
     * @brief Returns ram drive path if available, else (e.g. locally) Returns current work path
     */
    static std::string tryGetRamDrivePath();

protected:
    std::shared_ptr<YandexIO::IDevice> device_;
    std::shared_ptr<TPortManager> portManager_ = std::make_shared<TPortManager>();
};

struct QuasarUnitTestFixture: public QuasarUnitTestFixtureWithoutIpc {
    using Base = QuasarUnitTestFixtureWithoutIpc;

    void SetUp(NUnitTest::TTestContext& context) override;
    void TearDown(NUnitTest::TTestContext& context) override;

    void setDeviceForTests(std::shared_ptr<YandexIO::IDevice> device) override;

    std::shared_ptr<TestIpcFactory> ipcFactoryForTests();

    std::shared_ptr<quasar::ipc::IConnector> createIpcConnectorForTests(std::string serviceName);

    std::shared_ptr<quasar::ipc::IServer> createIpcServerForTests(std::string serviceName);

    void startMockIpcServers(const std::vector<std::string>& serviceNames);

private:
    void ensureIpcFactoryInitialized();

    std::shared_ptr<TestIpcFactory> ipcFactory_;
    std::unordered_map<std::string, std::shared_ptr<quasar::ipc::IServer>> mockIpcServers_;
};
