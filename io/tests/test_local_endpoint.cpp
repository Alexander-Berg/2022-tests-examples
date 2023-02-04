#include <yandex_io/sdk/private/endpoint_storage/local_endpoint.h>

#include <yandex_io/sdk/interfaces/mocks/mock_i_capability.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_endpoint.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <memory>

using namespace YandexIO;
using namespace quasar;
using namespace testing;

namespace {

    MATCHER_P(VerifySamePtr, targetPtr, "description") {
        const auto& ptr = arg;
        if (ptr != targetPtr) {
            return false;
        }
        return true;
    }

    MATCHER_P(VerifySameWeakPtr, targetPtr, "description") {
        const auto& weakPtr = arg;
        if (weakPtr.lock() != targetPtr.lock()) {
            return false;
        }
        return true;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(TestLocalEndpoint, QuasarUnitTestFixture) {
    Y_UNIT_TEST(AddGetWithoutRealEndpoint) {
        // Check that localEndpoint works as real endpoint
        // even without RealEndpoint
        const std::shared_ptr<IEndpoint> iendpoint = std::make_shared<LocalEndpoint>("id");
        const auto mockCapability = std::make_shared<MockICapability>("cap_id", nullptr);
        const auto mockListener = std::make_shared<MockIEndpointListener>();
        iendpoint->addListener(mockListener);
        EXPECT_CALL(*mockListener, onCapabilityAdded(VerifySamePtr(iendpoint), VerifySamePtr(mockCapability)));
        iendpoint->addCapability(mockCapability);

        EXPECT_CALL(*mockListener, onCapabilityRemoved(VerifySamePtr(iendpoint), VerifySamePtr(mockCapability)));
        iendpoint->removeCapability(mockCapability);

        // check do not crash
        Y_UNUSED(iendpoint->getState());
        Y_UNUSED(iendpoint->getStatus());
        Y_UNUSED(iendpoint->getDirectiveHandler());
    }

    Y_UNIT_TEST(RedirectCallsToRealEndpoint) {
        const std::string id = "id";
        const auto localEndpoint = std::make_shared<LocalEndpoint>(id);
        const std::shared_ptr<IEndpoint> iendpoint = localEndpoint;
        const auto realEndpoint = std::make_shared<MockIEndpoint>();

        EXPECT_CALL(*realEndpoint, getId()).WillOnce(ReturnRef(id));
        localEndpoint->setLocalEndpoint(realEndpoint);

        NAlice::TEndpoint state;
        NAlice::TEndpoint::TStatus status;
        const std::list<std::shared_ptr<ICapability>> capabilities;

        EXPECT_CALL(*realEndpoint, getState()).WillOnce(Return(state));
        EXPECT_CALL(*realEndpoint, getStatus()).WillOnce(Return(status));
        EXPECT_CALL(*realEndpoint, getCapabilities()).WillOnce(Return(capabilities));
        EXPECT_CALL(*realEndpoint, getDirectiveHandler()).WillOnce(Return(nullptr));

        // trigger
        Y_UNUSED(iendpoint->getState());
        Y_UNUSED(iendpoint->getStatus());
        Y_UNUSED(iendpoint->getCapabilities());
        Y_UNUSED(iendpoint->getDirectiveHandler());
    }

    Y_UNIT_TEST(DispatchRealEndpointEvents) {
        const std::string id = "id";
        const auto localEndpoint = std::make_shared<LocalEndpoint>(id);
        const std::shared_ptr<IEndpoint> iendpoint = localEndpoint;
        const std::shared_ptr<IEndpoint::IListener> ilistener = localEndpoint;
        const auto realEndpoint = std::make_shared<MockIEndpoint>();
        EXPECT_CALL(*realEndpoint, getId()).WillOnce(ReturnRef(id));

        // local endpoint should install self as "listener" to dispatch real endpoint events
        EXPECT_CALL(*realEndpoint, addListener(VerifySameWeakPtr(std::weak_ptr<IEndpoint::IListener>(localEndpoint))));
        localEndpoint->setLocalEndpoint(realEndpoint);

        const auto mockCapability = std::make_shared<MockICapability>("cap_id", nullptr);
        const auto mockListener = std::make_shared<MockIEndpointListener>();
        iendpoint->addListener(mockListener);

        // "RealEndpoint" produce added/removed events.
        // LocalEndpoint should dispatch this events passing self into firest arg
        EXPECT_CALL(*mockListener, onCapabilityAdded(VerifySamePtr(iendpoint), VerifySamePtr(mockCapability)));
        ilistener->onCapabilityAdded(realEndpoint, mockCapability);

        EXPECT_CALL(*mockListener, onCapabilityRemoved(VerifySamePtr(iendpoint), VerifySamePtr(mockCapability)));
        ilistener->onCapabilityRemoved(realEndpoint, mockCapability);

        EXPECT_CALL(*mockListener, onEndpointStateChanged(VerifySamePtr(iendpoint)));
        ilistener->onEndpointStateChanged(realEndpoint);
    }

    Y_UNIT_TEST(AddRemoveCapabilitiesOnSetRemoveRealEndpoint) {
        const std::string id = "id";
        const auto localEndpoint = std::make_shared<LocalEndpoint>(id);
        const std::shared_ptr<IEndpoint> iendpoint = localEndpoint;
        const std::shared_ptr<IEndpoint::IListener> ilistener = localEndpoint;
        const auto realEndpoint = std::make_shared<MockIEndpoint>();
        EXPECT_CALL(*realEndpoint, getId()).WillOnce(ReturnRef(id));
        const auto mockCapability1 = std::make_shared<MockICapability>("local_capability", nullptr);
        const auto mockListener = std::make_shared<MockIEndpointListener>();
        iendpoint->addCapability(mockCapability1); // local capability
        iendpoint->addListener(mockListener);

        const auto mockCapability2 = std::make_shared<MockICapability>("remote_capability", nullptr);
        const std::list<std::shared_ptr<ICapability>> capabilities{mockCapability2};

        // Local Endpoint notifies own listeners about new capabilities
        EXPECT_CALL(*realEndpoint, getCapabilities()).WillOnce(Return(capabilities));
        EXPECT_CALL(*mockListener, onCapabilityAdded(VerifySamePtr(iendpoint), VerifySamePtr(mockCapability2)));

        // Local Endpoint install own capabilities into real endpoint
        EXPECT_CALL(*realEndpoint, addCapability(VerifySamePtr(mockCapability1)));
        // trigger
        localEndpoint->setLocalEndpoint(realEndpoint);

        // after add RealEndpoint will have 2 caps. Remote and Local
        const std::list<std::shared_ptr<ICapability>> capabilities2{mockCapability2, mockCapability1};
        // Local Endpoint notifies own listeners about removing remote capabilities
        EXPECT_CALL(*realEndpoint, getCapabilities()).WillOnce(Return(capabilities2));
        EXPECT_CALL(*mockListener, onCapabilityRemoved(VerifySamePtr(iendpoint), VerifySamePtr(mockCapability2)));
        localEndpoint->removeLocalEndpoint();
    }
}
