#include "test_tcp_endpoints.h"

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::ipc::detail::datacratic;

MockTCPEndpoint::MockTCPEndpoint(const std::string& /* name */)
{
    onRequest = [=](const std::string& /* message */, const std::shared_ptr<TCPConnectionHandler>& handler) {
        handler->send(answer);
    };
}

void MockTCPEndpoint::setRequestHandler(RequestHandler requestHandler)
{
    onRequest = std::move(requestHandler);
}

MockTCPEndpoint::~MockTCPEndpoint()
{
    shutdown();
}

void TestTCPConnector::handleMessageReceived(const std::string& response)
{
    if (onMessage) {
        onMessage(response);
    }
}
