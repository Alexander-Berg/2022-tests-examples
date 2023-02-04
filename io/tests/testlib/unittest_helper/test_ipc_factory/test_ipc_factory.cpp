#include "test_ipc_factory.h"

TestIpcFactory::TestIpcFactory(std::shared_ptr<quasar::ipc::IIpcFactory> realFactory)
    : realFactory_(std::move(realFactory))
{
}

std::shared_ptr<quasar::ipc::IConnector> TestIpcFactory::createIpcConnector(const std::string& serviceName)
{
    auto iter = mockConnectors_.find(serviceName);
    if (iter != mockConnectors_.end()) {
        return iter->second;
    }

    return realFactory_->createIpcConnector(serviceName);
}

std::shared_ptr<quasar::ipc::IServer> TestIpcFactory::createIpcServer(const std::string& serviceName)
{
    return realFactory_->createIpcServer(serviceName);
}

std::shared_ptr<quasar::ipc::mock::MockIConnector> TestIpcFactory::allocateGMockIpcConnector(const std::string& serviceName)
{
    auto mockConnector = std::make_shared<quasar::ipc::mock::MockIConnector>();
    mockConnectors_[serviceName] = mockConnector;
    return mockConnector;
}
