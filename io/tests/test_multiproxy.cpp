#include <yandex_io/sdk/private/endpoint_storage/endpoint.h>
#include <yandex_io/sdk/private/endpoint_storage/endpoint_storage_host.h>
#include <yandex_io/sdk/private/endpoint_storage/endpoint_storage_proxy.h>
#include <yandex_io/sdk/private/remoting/remoting_message_router.h>
#include <yandex_io/sdk/private/remoting/remoting_connector_wrapper.h>

#include <yandex_io/sdk/interfaces/mocks/mock_i_capability.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_directive_handler.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_endpoint.h>
#include <yandex_io/sdk/interfaces/mocks/mock_i_endpoint_storage.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/debug.h>
#include <yandex_io/libs/base/named_callback_queue.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

#include <future>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

namespace {

    MATCHER_P2(VerifyEndpointIdWithDH, endpointId, directiveHandlerName, "description") {
        const std::shared_ptr<IEndpoint>& endpoint = arg;
        if (endpoint->getId() != endpointId) {
            *result_listener << "Invalid EndpointID. Got: " << endpoint->getId() << ", Expected: " << endpointId;
            return false;
        }
        if (!endpoint->getDirectiveHandler()) {
            *result_listener << "Directive handler is null";
            return false;
        } else if (directiveHandlerName != endpoint->getDirectiveHandler()->getHandlerName()) {
            *result_listener << "Invalid Directive handler name. Got: " << endpoint->getDirectiveHandler()->getHandlerName() << ", Expected: " << directiveHandlerName;
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyEndpointId, endpointId, "description") {
        const std::shared_ptr<IEndpoint>& endpoint = arg;
        if (endpoint->getId() != endpointId) {
            *result_listener << "Invalid EndpointID. Got: " << endpoint->getId() << ", Expected: " << endpointId;
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyCapabilityId, capabilityId, "description") {
        const std::shared_ptr<ICapability>& capability = arg;
        if (capability->getId() != capabilityId) {
            *result_listener << "Invalid CapabilityId. Got: " << capability->getId() << ", Expected: " << capabilityId;
            return false;
        }
        return true;
    }

    MATCHER_P(VerifyDirectiveHame, directiveName, "description") {
        const std::shared_ptr<Directive>& directive = arg;
        if (directive->getData().name != directiveName) {
            *result_listener << "Invalid DirectiveName. Got: " << directive->getData().name << ", Expected: " << directiveName;
            return false;
        }
        return true;
    }

    class TestFixture: public QuasarUnitTestFixture {
    public:
        void SetUp(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::SetUp(context);

            mockServer_ = createIpcServerForTests("aliced");

            const auto hostRouter = std::make_shared<RemotingMessageRouter>();
            mockServer_->setMessageHandler([hostRouter, this](const auto& msg, auto& connection) {
                auto message = *msg;
                if (message.has_remoting()) {
                    YIO_LOG_DEBUG("Protobuf=" << quasar::shortUtf8DebugString(message) << " thread: " << std::this_thread::get_id());
                    worker_->add([hostRouter, message, connection{connection.share()}]() {
                        hostRouter->routeRemotingMessage(message.remoting(), connection);
                    });
                }
            });

            // currently remoting doesn't allow to have same objectId. Create separate routers
            remotingConnectorWrapper1_ = std::make_unique<YandexIO::RemotingConnectorWrapper>(*ipcFactoryForTests(), "aliced", worker_, "router1");
            remotingConnectorWrapper2_ = std::make_unique<YandexIO::RemotingConnectorWrapper>(*ipcFactoryForTests(), "aliced", worker_, "router2");

            storageHost_ = std::make_shared<EndpointStorageHost>(hostRouter, "deviceid", NAlice::TEndpoint::SpeakerEndpointType);
            storageHost_->init();

            storageProxy1_ = std::make_shared<EndpointStorageProxy>("proxy1", "deviceid", worker_, remotingConnectorWrapper1_->getRemotingRegistry());
            storageProxy2_ = std::make_shared<EndpointStorageProxy>("proxy2", "deviceid", worker_, remotingConnectorWrapper2_->getRemotingRegistry());
            storageProxy1_->start();
            storageProxy2_->start();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            QuasarUnitTestFixture::TearDown(context);
            storageHost_.reset();
            storageProxy2_->stop();
            storageProxy1_->stop();
            worker_->destroy();
        }

        void callHostMethod(std::function<void()> func) {
            std::promise<void> promise;
            worker_->add([func, &promise]() {
                func();
                promise.set_value();
            });
            promise.get_future().get();
        }

        void startStorages() const {
            // start host remoting
            mockServer_->listenService();

            // start proxies remoting
            remotingConnectorWrapper1_->start();
            remotingConnectorWrapper2_->start();
        }

        std::shared_ptr<quasar::ipc::IServer> mockServer_;
        std::mutex hostMutex_;
        std::shared_ptr<EndpointStorageHost> storageHost_; // host is signle threaded for now
        std::shared_ptr<EndpointStorageProxy> storageProxy1_;
        std::shared_ptr<EndpointStorageProxy> storageProxy2_;
        std::unique_ptr<YandexIO::RemotingConnectorWrapper> remotingConnectorWrapper1_;
        std::unique_ptr<YandexIO::RemotingConnectorWrapper> remotingConnectorWrapper2_;
        const std::shared_ptr<quasar::NamedCallbackQueue> worker_ = std::make_shared<quasar::NamedCallbackQueue>("worker");
    };

    std::shared_ptr<IEndpoint> findEndpoint(const std::list<std::shared_ptr<IEndpoint>>& endpoints, const std::string& id) {
        for (const auto& endpoint : endpoints) {
            if (endpoint->getId() == id) {
                return endpoint;
            }
        }
        return nullptr;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(EndpointStorageMultiproxy, TestFixture) {
    Y_UNIT_TEST(OneProxyToHost) {
        // FIXME: EndpointStorageHost isn't thread safe. Rework this test

        const auto hostListener = std::make_shared<MockIEndpointStorageListener>();
        storageHost_->addListener(hostListener);

        const auto endpoint1 = storageProxy1_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto endpoint2 = storageProxy1_->createEndpoint("endpoint2", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        storageProxy1_->addEndpoint(endpoint1);
        storageProxy1_->addEndpoint(endpoint2);

        // Test SYNC on connect
        std::promise<void> expectation;
        {
            testing::InSequence s;

            EXPECT_CALL(*hostListener, onEndpointAdded(VerifyEndpointId(endpoint1->getId())));
            EXPECT_CALL(*hostListener, onEndpointAdded(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));
        }
        // init wire connections. Sync starts after that
        startStorages();
        expectation.get_future().get();
        // localEndpoint + endpoint1 + endpoint2
        UNIT_ASSERT_VALUES_EQUAL(storageHost_->getEndpoints().size(), 3);

        // test adding endpoint to proxy after sync
        expectation = std::promise<void>();
        const auto endpoint3 = storageProxy1_->createEndpoint("endpoint3", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        EXPECT_CALL(*hostListener, onEndpointAdded(VerifyEndpointId(endpoint3->getId()))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        storageProxy1_->addEndpoint(endpoint3);
        expectation.get_future().get();

        // localEndpoint + endpoint1 + endpoint2 + endpoint3
        UNIT_ASSERT_VALUES_EQUAL(storageHost_->getEndpoints().size(), 4);

        // test removing endpoint to proxy after sync
        expectation = std::promise<void>();
        EXPECT_CALL(*hostListener, onEndpointRemoved(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        storageProxy1_->removeEndpoint(endpoint2);
        expectation.get_future().get();
        // localEndpoint + endpoint1 + endpoint3
        UNIT_ASSERT_VALUES_EQUAL(storageHost_->getEndpoints().size(), 3);

        // test adding capability
        expectation = std::promise<void>();

        std::shared_ptr<IEndpoint> endpointProxy1;
        for (const auto& endpoint : storageHost_->getEndpoints()) {
            if (endpoint->getId() == endpoint1->getId()) {
                endpointProxy1 = endpoint;
                break;
            }
        }
        UNIT_ASSERT(endpointProxy1);

        const auto endpointListener1 = std::make_shared<MockIEndpointListener>();
        endpointProxy1->addListener(endpointListener1); // add endpoint listener into proxy in endpoint storage host

        const auto directiveHandler1 = std::make_shared<MockIDirectiveHandler>();
        const std::string directiveHandlerName1 = "directiveHandlerName1";
        const std::set<std::string> directiveNames1 = {"directive1", "directive2"};
        EXPECT_CALL(*directiveHandler1, getHandlerName()).WillRepeatedly(ReturnRef(directiveHandlerName1));
        EXPECT_CALL(*directiveHandler1, getSupportedDirectiveNames()).WillRepeatedly(ReturnRef(directiveNames1));

        const auto capability1 = std::make_shared<MockICapability>("capability1", directiveHandler1);
        EXPECT_CALL(*endpointListener1, onCapabilityAdded(VerifyEndpointId(endpoint1->getId()), VerifyCapabilityId(capability1->getId()))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        endpoint1->addCapability(capability1); // add capability to real endpoint in endpoint storage proxy
        expectation.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(endpointProxy1->getCapabilities().size(), 1);

        // test remote directive handler
        expectation = std::promise<void>();
        const auto capabilityProxy1 = endpointProxy1->getCapabilities().front();

        {
            testing::InSequence s;

            EXPECT_CALL(*directiveHandler1, handleDirective(VerifyDirectiveHame("directive1")));
            EXPECT_CALL(*directiveHandler1, handleDirective(VerifyDirectiveHame("directive2"))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));
        }

        UNIT_ASSERT(capabilityProxy1->getDirectiveHandler());
        const auto directive1 = std::make_shared<Directive>(Directive::Data("directive1", "local_action"));
        const auto directive2 = std::make_shared<Directive>(Directive::Data("directive2", "local_action"));

        capabilityProxy1->getDirectiveHandler()->handleDirective(directive1);
        capabilityProxy1->getDirectiveHandler()->handleDirective(directive2);

        expectation.get_future().get();
    }

    Y_UNIT_TEST(SyncHostAndProxyEndpoints) {
        const auto proxyListener1 = std::make_shared<MockIEndpointStorageListener>();
        const auto proxyListener2 = std::make_shared<MockIEndpointStorageListener>();
        const auto hostListener = std::make_shared<MockIEndpointStorageListener>();
        callHostMethod([this, hostListener]() {
            storageHost_->addListener(hostListener);
        });
        storageProxy1_->addListener(proxyListener1);
        storageProxy2_->addListener(proxyListener2);

        const auto endpoint1 = storageHost_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto endpoint2 = storageHost_->createEndpoint("endpoint2", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto endpoint3 = storageProxy1_->createEndpoint("endpoint3", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);

        callHostMethod([this, endpoint1, endpoint2]() {
            storageHost_->addEndpoint(endpoint1);
            storageHost_->addEndpoint(endpoint2);
        });
        storageProxy1_->addEndpoint(endpoint3);
        Y_UNUSED(storageProxy1_->getEndpoints()); // ensure sync addEndpoint (before sync)

        std::promise<void> proxy1Ep1;
        std::promise<void> proxy1Ep2;
        // Test sync: host endpoints in proxy
        EXPECT_CALL(*proxyListener1, onEndpointAdded(VerifyEndpointId(endpoint1->getId()))).WillOnce(testing::Invoke([&]() {
            proxy1Ep1.set_value();
        }));
        EXPECT_CALL(*proxyListener1, onEndpointAdded(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            proxy1Ep2.set_value();
        }));

        // Test sync: proxy endpoints in host
        std::promise<void> hostEp3;
        EXPECT_CALL(*hostListener, onEndpointAdded(VerifyEndpointId(endpoint3->getId()))).WillOnce(testing::Invoke([&]() {
            hostEp3.set_value();
        }));

        // Test second proxy: get all endpoints
        std::promise<void> proxy2ep1;
        std::promise<void> proxy2ep2;
        std::promise<void> proxy2ep3;
        EXPECT_CALL(*proxyListener2, onEndpointAdded(VerifyEndpointId(endpoint1->getId()))).WillOnce(testing::Invoke([&]() {
            proxy2ep1.set_value();
        }));
        EXPECT_CALL(*proxyListener2, onEndpointAdded(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            proxy2ep2.set_value();
        }));
        EXPECT_CALL(*proxyListener2, onEndpointAdded(VerifyEndpointId(endpoint3->getId()))).WillOnce(testing::Invoke([&]() {
            proxy2ep3.set_value();
        }));

        // Start test by starting connections
        startStorages();

        proxy1Ep1.get_future().get();
        proxy1Ep2.get_future().get();
        hostEp3.get_future().get();

        proxy2ep1.get_future().get();
        proxy2ep2.get_future().get();
        proxy2ep3.get_future().get();

        callHostMethod([this]() {
            UNIT_ASSERT_VALUES_EQUAL(storageHost_->getEndpoints().size(), 4);
        });
        UNIT_ASSERT_VALUES_EQUAL(storageProxy1_->getEndpoints().size(), 4);
        UNIT_ASSERT_VALUES_EQUAL(storageProxy2_->getEndpoints().size(), 4);
    }

    Y_UNIT_TEST(AddEndpointToHost) {
        const auto proxyListener = std::make_shared<MockIEndpointStorageListener>();
        storageProxy1_->addListener(proxyListener);

        const auto endpoint1 = storageHost_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto endpoint2 = storageHost_->createEndpoint("endpoint2", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        startStorages();

        // Test Adding Endpoint from storage host to proxy
        std::promise<void> expectation;
        EXPECT_CALL(*proxyListener, onEndpointAdded(VerifyEndpointId(endpoint1->getId()))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));

        callHostMethod([this, endpoint1]() {
            storageHost_->addEndpoint(endpoint1);
        });

        expectation.get_future().get();
        // second endpoint (guaranteed after sync)
        expectation = std::promise<void>();
        EXPECT_CALL(*proxyListener, onEndpointAdded(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            expectation.set_value();
        }));
        callHostMethod([this, endpoint2]() {
            storageHost_->addEndpoint(endpoint2);
        });
        expectation.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(storageProxy1_->getEndpoints().size(), 3);
        YIO_LOG_DEBUG("done...");
    }

    Y_UNIT_TEST(Proxy1ToHostToProxy2) {
        const auto proxyListener1 = std::make_shared<MockIEndpointStorageListener>();
        const auto proxyListener2 = std::make_shared<MockIEndpointStorageListener>();
        storageProxy1_->addListener(proxyListener1);
        storageProxy2_->addListener(proxyListener2);

        const auto endpoint1 = storageProxy1_->createEndpoint("endpoint1", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        const auto endpoint2 = storageProxy2_->createEndpoint("endpoint2", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
        startStorages();

        // Test Adding Endpoint from proxy is forwarded to another proxy
        std::promise<void> localAdd;
        std::promise<void> proxyAdd;
        EXPECT_CALL(*proxyListener1, onEndpointAdded(VerifyEndpointId(endpoint1->getId()))).WillOnce(testing::Invoke([&]() {
            localAdd.set_value();
        }));
        EXPECT_CALL(*proxyListener2, onEndpointAdded(VerifyEndpointId(endpoint1->getId()))).WillOnce(testing::Invoke([&]() {
            proxyAdd.set_value();
        }));

        storageProxy1_->addEndpoint(endpoint1);
        localAdd.get_future().get();
        proxyAdd.get_future().get();

        // other way around
        localAdd = std::promise<void>();
        proxyAdd = std::promise<void>();
        EXPECT_CALL(*proxyListener2, onEndpointAdded(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            localAdd.set_value();
        }));
        EXPECT_CALL(*proxyListener1, onEndpointAdded(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            proxyAdd.set_value();
        }));

        storageProxy2_->addEndpoint(endpoint2);
        localAdd.get_future().get();
        proxyAdd.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(storageProxy1_->getEndpoints().size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(storageProxy2_->getEndpoints().size(), 3);
        callHostMethod([this]() {
            UNIT_ASSERT_VALUES_EQUAL(storageHost_->getEndpoints().size(), 3);
        });

        std::promise<void> removeEp2;
        // test remove
        EXPECT_CALL(*proxyListener1, onEndpointRemoved(VerifyEndpointId(endpoint2->getId()))).WillOnce(testing::Invoke([&]() {
            removeEp2.set_value();
        }));

        storageProxy2_->removeEndpoint(endpoint2);
        removeEp2.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(storageProxy1_->getEndpoints().size(), 2);
        UNIT_ASSERT_VALUES_EQUAL(storageProxy2_->getEndpoints().size(), 2);
        callHostMethod([this]() {
            UNIT_ASSERT_VALUES_EQUAL(storageHost_->getEndpoints().size(), 2);
        });
    }
    Y_UNIT_TEST(HandleDirectiveProxiedThroughEndpointStorageHost) {
        // EndToEnd test checks that it's possible to Add Endpoint into one EndpointStorageProxy
        // And then add Capability into this endpoint via another EndpointStorageProxy.
        // After that if we call handleDirective via CapabilitProxy added to first EndpointStorageProxy
        // -> handleDirective is properly forwarded to actual capability (added to second EndpointStorageProxy)
        // EndpointStorageHost (and EndpointProxy inside it) should properly forward DirectiveHandler methods

        // suffix 1 -- something is added to storageProxy1
        // suffix 2 -- something is added to storageProxy2

        const auto endpointDirectiveHandler1 = std::make_shared<MockIDirectiveHandler>();
        const std::string endpointId1 = "endpoint1";
        const std::set<std::string> directiveNames1 = {};
        ON_CALL(*endpointDirectiveHandler1, getHandlerName()).WillByDefault(ReturnRef(endpointId1));
        ON_CALL(*endpointDirectiveHandler1, getSupportedDirectiveNames()).WillByDefault(ReturnRef(directiveNames1));
        ON_CALL(*endpointDirectiveHandler1, getEndpointId()).WillByDefault(ReturnRef(endpointId1));
        const auto endpoint1 = storageProxy1_->createEndpoint(endpointId1, NAlice::TEndpoint::SpeakerEndpointType, {}, endpointDirectiveHandler1);
        const auto proxyListener2 = std::make_shared<MockIEndpointStorageListener>();

        // directive handler
        const auto directiveHandler2 = std::make_shared<MockIDirectiveHandler>();
        const std::string directiveHandlerName2 = "directiveHandlerName2";
        const std::set<std::string> directiveNames2 = {"directive1", "directive2"};
        ON_CALL(*directiveHandler2, getHandlerName()).WillByDefault(ReturnRef(directiveHandlerName2));
        ON_CALL(*directiveHandler2, getSupportedDirectiveNames()).WillByDefault(ReturnRef(directiveNames2));

        storageProxy1_->addEndpoint(endpoint1);
        storageProxy2_->addListener(proxyListener2);

        std::promise<void> expectation;
        {
            // wait until storageProxy2 creates EndpointProxy
            EXPECT_CALL(*proxyListener2, onEndpointAdded(VerifyEndpointIdWithDH(endpoint1->getId(), endpoint1->getId()))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            startStorages();
            expectation.get_future().get();
        }
        expectation = std::promise<void>();

        // check endpoint directive handler
        {
            testing::InSequence s;
            EXPECT_CALL(*endpointDirectiveHandler1, handleDirective(VerifyDirectiveHame("directive1"))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            Directive::Data data("directive1", "local_action");
            data.endpointId = endpointId1;

            findEndpoint(storageProxy2_->getEndpoints(), endpointId1)->getDirectiveHandler()->handleDirective(std::make_shared<Directive>(std::move(data)));
        }
        expectation.get_future().get();
        expectation = std::promise<void>();

        const auto capability2 = std::make_shared<MockICapability>("capability2", directiveHandler2);

        const auto hostEndpointListener = std::make_shared<MockIEndpointListener>();
        callHostMethod([this, hostEndpointListener, endpointId1]() {
            findEndpoint(storageHost_->getEndpoints(), endpointId1)->addListener(hostEndpointListener);
        });
        const auto endpointListener1 = std::make_shared<MockIEndpointListener>();
        endpoint1->addListener(endpointListener1);

        {
            // wait until EndpointProxy inside storageProxy1 and storageHost creates CapabilityProxy
            EXPECT_CALL(*endpointListener1, onCapabilityAdded(VerifyEndpointId(endpoint1->getId()), VerifyCapabilityId(capability2->getId()))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));
            std::promise<void> expectation2;
            EXPECT_CALL(*hostEndpointListener, onCapabilityAdded(VerifyEndpointId(endpoint1->getId()), VerifyCapabilityId(capability2->getId()))).WillOnce(testing::Invoke([&]() {
                expectation2.set_value();
            }));

            findEndpoint(storageProxy2_->getEndpoints(), endpointId1)->addCapability(capability2);
            expectation.get_future().get();
            expectation2.get_future().get();
        }
        expectation = std::promise<void>();

        // Actual test: Make sure that handleDirective call on CapabilityProxy will forward
        // handleDirective to Capability2 (which is inside proxyStorage2);

        {
            // test DirectiveHandlerProxy inside EndpointStoragePorxy1
            testing::InSequence s;

            EXPECT_CALL(*directiveHandler2, handleDirective(VerifyDirectiveHame("directive1")));
            EXPECT_CALL(*directiveHandler2, handleDirective(VerifyDirectiveHame("directive2"))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            const auto capabilityProxy = findEndpoint(storageProxy1_->getEndpoints(), endpointId1)->getCapabilities().front();
            UNIT_ASSERT(capabilityProxy->getDirectiveHandler());
            const auto directive1 = std::make_shared<Directive>(Directive::Data("directive1", "local_action"));
            const auto directive2 = std::make_shared<Directive>(Directive::Data("directive2", "local_action"));

            capabilityProxy->getDirectiveHandler()->handleDirective(directive1);
            capabilityProxy->getDirectiveHandler()->handleDirective(directive2);

            expectation.get_future().get();
        }

        expectation = std::promise<void>();
        {
            // test DirectiveHandlerProxy inside EndpointStorageHost
            testing::InSequence s;

            EXPECT_CALL(*directiveHandler2, handleDirective(VerifyDirectiveHame("directive3")));
            EXPECT_CALL(*directiveHandler2, handleDirective(VerifyDirectiveHame("directive4"))).WillOnce(testing::Invoke([&]() {
                expectation.set_value();
            }));

            const auto capabilityProxy = findEndpoint(storageHost_->getEndpoints(), endpointId1)->getCapabilities().front();
            UNIT_ASSERT(capabilityProxy->getDirectiveHandler());
            const auto directive1 = std::make_shared<Directive>(Directive::Data("directive3", "local_action"));
            const auto directive2 = std::make_shared<Directive>(Directive::Data("directive4", "local_action"));

            capabilityProxy->getDirectiveHandler()->handleDirective(directive1);
            capabilityProxy->getDirectiveHandler()->handleDirective(directive2);

            expectation.get_future().get();
        }
    }

    Y_UNIT_TEST(TestLocalEndpoint) {
        const auto capability1 = std::make_shared<MockICapability>("capability1", nullptr);
        const auto capability2 = std::make_shared<MockICapability>("capability2", nullptr);
        const auto capability3 = std::make_shared<MockICapability>("capability3", nullptr);

        storageProxy1_->getLocalEndpoint()->addCapability(capability1);
        storageProxy2_->getLocalEndpoint()->addCapability(capability2);
        callHostMethod([this, capability3]() {
            storageHost_->getLocalEndpoint()->addCapability(capability3);
        });

        auto waitCapabilities = [this](const std::set<std::string>& caps) {
            TestUtils::waitUntil([&]() {
                bool res = false;
                callHostMethod([&]() {
                    res = storageHost_->getLocalEndpoint()->getCapabilities().size() == caps.size();
                });
                return res;
            });
            TestUtils::waitUntil([&]() {
                return storageProxy1_->getLocalEndpoint()->getCapabilities().size() == caps.size();
            });
            TestUtils::waitUntil([&]() {
                return storageProxy2_->getLocalEndpoint()->getCapabilities().size() == caps.size();
            });
            auto buildIdsSet = [](const auto& caps) {
                std::set<std::string> ids;
                for (const auto& cap : caps) {
                    ids.insert(cap->getId());
                }
                return ids;
            };

            UNIT_ASSERT_EQUAL(caps, buildIdsSet(storageProxy1_->getLocalEndpoint()->getCapabilities()));
            UNIT_ASSERT_EQUAL(caps, buildIdsSet(storageProxy2_->getLocalEndpoint()->getCapabilities()));
            callHostMethod([&]() {
                UNIT_ASSERT_EQUAL(caps, buildIdsSet(storageHost_->getLocalEndpoint()->getCapabilities()));
            });
        };

        // test sync works properly
        startStorages();
        waitCapabilities({"capability1", "capability2", "capability3"});

        // test add works properly
        const auto capability4 = std::make_shared<MockICapability>("capability4", nullptr);
        storageProxy1_->getLocalEndpoint()->addCapability(capability4);
        waitCapabilities({"capability1", "capability2", "capability3", "capability4"});

        const auto capability5 = std::make_shared<MockICapability>("capability5", nullptr);
        callHostMethod([this, capability5]() {
            storageHost_->getLocalEndpoint()->addCapability(capability5);
        });
        waitCapabilities({"capability1", "capability2", "capability3", "capability4", "capability5"});

        // test remove works properly
        storageProxy2_->getLocalEndpoint()->removeCapability(storageProxy2_->getLocalEndpoint()->findCapabilityById("capability1"));
        waitCapabilities({"capability2", "capability3", "capability4", "capability5"});

        callHostMethod([this]() {
            storageHost_->getLocalEndpoint()->removeCapability(storageHost_->getLocalEndpoint()->findCapabilityById("capability2"));
        });
        waitCapabilities({"capability3", "capability4", "capability5"});
    }
}
