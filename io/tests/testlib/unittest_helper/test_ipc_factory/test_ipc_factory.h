#pragma once

#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/mock/mock_i_connector.h>
#include <yandex_io/libs/ipc/i_ipc_factory.h>

#include <unordered_map>

class TestIpcFactory: public quasar::ipc::IIpcFactory {
public:
    explicit TestIpcFactory(std::shared_ptr<quasar::ipc::IIpcFactory> realFactory);

    /**
     * After call, createIpcConnector(serviceName) will return only mockIConnector for serviceName
     * @param serviceName
     * @return mockIConnector for serviceName
     */
    std::shared_ptr<quasar::ipc::mock::MockIConnector> allocateGMockIpcConnector(const std::string& serviceName);

    /// IIpcFactory impl
    std::shared_ptr<quasar::ipc::IServer> createIpcServer(const std::string& serviceName) override;

    /**
     * @param serviceName
     * @return - mockIpcConnector if it was allocated before
     *         - realFactory_->createIpcConnector(serviceName) otherwise
     */
    std::shared_ptr<quasar::ipc::IConnector> createIpcConnector(const std::string& serviceName) override;

private:
    std::shared_ptr<quasar::ipc::IIpcFactory> realFactory_;
    std::unordered_map<std::string, std::shared_ptr<quasar::ipc::mock::MockIConnector>> mockConnectors_;
};
