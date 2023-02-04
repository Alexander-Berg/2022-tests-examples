#include <yandex_io/sdk/private/endpoint_storage/endpoint.h>
#include <yandex_io/sdk/private/endpoint_storage/endpoint_storage_host.h>

#include <yandex_io/libs/ipc/mock/server.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace testing;
using namespace YandexIO;

namespace {

    class EndpointStorageHostFixture: public NUnitTest::TBaseFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            NUnitTest::TBaseFixture::SetUp(context);
            init();
        }

    private:
        void init() {
            server_ = std::make_shared<NiceMock<quasar::ipc::mock::Server>>();
            storage_ = std::make_shared<EndpointStorageHost>(std::weak_ptr<IRemotingRegistry>(), "deviceid", NAlice::TEndpoint::SpeakerEndpointType);
        }

    public:
        std::shared_ptr<quasar::ipc::mock::Server> server_;
        std::shared_ptr<EndpointStorageHost> storage_;
    };

    std::list<std::shared_ptr<IEndpoint>> createEndpoints(const std::list<std::shared_ptr<IEndpoint>>& endpoints) {
        return endpoints;
    }

} // anonymous namespace

Y_UNIT_TEST_SUITE(EndpointStorageHostTest) {
    Y_UNIT_TEST_F(testAddRemoveEndpoint, EndpointStorageHostFixture) {
        std::shared_ptr<IEndpoint> endpoint1 = storage_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        std::shared_ptr<IEndpoint> endpoint2 = storage_->createEndpoint("endpoint2", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);

        // always has local endpoint
        ASSERT_TRUE(storage_->getEndpoints().size() == 1);
        const auto localEndpoint = storage_->getLocalEndpoint();
        storage_->addEndpoint(nullptr);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint})));

        storage_->removeEndpoint(nullptr);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint})));

        storage_->removeEndpoint(endpoint1);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint})));

        storage_->addEndpoint(endpoint1);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint, endpoint1})));

        storage_->addEndpoint(endpoint2);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint,
                                                 endpoint1,
                                                 endpoint2})));

        storage_->removeEndpoint(endpoint1);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint, endpoint2})));

        storage_->removeEndpoint(endpoint2);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint})));
    }

    Y_UNIT_TEST_F(testEndpointIdUnique, EndpointStorageHostFixture) {
        const auto endpoint1 = storage_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto endpoint2 = storage_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto localEndpoint = storage_->getLocalEndpoint();

        storage_->addEndpoint(endpoint1);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint, endpoint1})));

        storage_->addEndpoint(endpoint2);
        EXPECT_THAT(storage_->getEndpoints(),
                    ContainerEq(createEndpoints({localEndpoint, endpoint1})));
    }
}
