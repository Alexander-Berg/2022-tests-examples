#include <yandex_io/interfaces/glagol/connector/glagol_cluster_provider.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/mock/simple_connector.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE(GlagolClusterProvider) {

    Y_UNIT_TEST(testDeviceListInitial)
    {
        auto glagolConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        GlagolClusterProvider glagolCluster(glagolConnector);

        auto deviceList = glagolCluster.deviceList().value();
        auto publicIp = glagolCluster.publicIp().value();

        UNIT_ASSERT(deviceList);
        UNIT_ASSERT_VALUES_EQUAL(deviceList->size(), 0);
        UNIT_ASSERT_VALUES_EQUAL(publicIp, "");
    }

    Y_UNIT_TEST(testPublicIpChanged) {
        auto glagolConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        GlagolClusterProvider glagolCluster(glagolConnector);

        proto::QuasarMessage message;
        message.mutable_glagold_state()->set_public_ip("1.1.1.1");
        glagolConnector->pushMessage(message);

        auto publicIp = glagolCluster.publicIp().value();

        UNIT_ASSERT_VALUES_EQUAL(publicIp, "1.1.1.1");
    }

    Y_UNIT_TEST(testPublicIpSignal) {
        auto glagolConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        GlagolClusterProvider glagolCluster(glagolConnector);

        std::atomic<int> stage{0};
        glagolCluster.publicIp().connect(
            [&](const auto& newPublicIp) {
                if (stage == 0) {
                    UNIT_ASSERT_VALUES_EQUAL(newPublicIp, "");
                    stage = 1;
                } else if (stage == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(newPublicIp, "1.1.1.1");
                    stage = 2;
                } else {
                    UNIT_ASSERT(false);
                }
            }, Lifetime::immortal);

        proto::QuasarMessage message;
        message.mutable_glagold_state()->set_public_ip("1.1.1.1");
        glagolConnector->pushMessage(message);
        waitUntil([&] { return stage == 2; });
    }

    Y_UNIT_TEST(testDeviceListChanged)
    {
        auto glagolConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        GlagolClusterProvider glagolCluster(glagolConnector);

        proto::QuasarMessage message;
        message.mutable_glagold_state()->add_cluster_device_list("CCC");
        message.mutable_glagold_state()->add_cluster_device_list("AAA");
        message.mutable_glagold_state()->add_cluster_device_list("BBB");
        glagolConnector->pushMessage(message);

        auto deviceList = glagolCluster.deviceList().value();

        UNIT_ASSERT(deviceList);
        UNIT_ASSERT_VALUES_EQUAL(deviceList->size(), 3);
        UNIT_ASSERT_VALUES_EQUAL(deviceList->at(0), "CCC");
        UNIT_ASSERT_VALUES_EQUAL(deviceList->at(1), "AAA");
        UNIT_ASSERT_VALUES_EQUAL(deviceList->at(2), "BBB");
    }

    Y_UNIT_TEST(testDeviceListSignal)
    {
        auto glagolConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        GlagolClusterProvider glagolCluster(glagolConnector);

        std::atomic<int> stage{0};
        glagolCluster.deviceList().connect(
            [&](const auto& deviceList) {
                UNIT_ASSERT(deviceList);
                if (stage == 0) {
                    UNIT_ASSERT_VALUES_EQUAL(deviceList->size(), 0);
                    stage = 1;
                } else if (stage == 1) {
                    UNIT_ASSERT_VALUES_EQUAL(deviceList->size(), 3);
                    UNIT_ASSERT_VALUES_EQUAL(deviceList->at(0), "CCC");
                    UNIT_ASSERT_VALUES_EQUAL(deviceList->at(1), "AAA");
                    UNIT_ASSERT_VALUES_EQUAL(deviceList->at(2), "BBB");
                    stage = 2;
                }
            }, Lifetime::immortal);

        proto::QuasarMessage message;
        message.mutable_glagold_state()->add_cluster_device_list("CCC");
        message.mutable_glagold_state()->add_cluster_device_list("AAA");
        message.mutable_glagold_state()->add_cluster_device_list("BBB");
        glagolConnector->pushMessage(message);

        waitUntil([&] { return stage == 2; });
    }

    Y_UNIT_TEST(testSendAll)
    {
        std::atomic<bool> msgReceived{false};
        auto glagolConnector = std::make_shared<ipc::mock::SimpleConnector>(nullptr);
        glagolConnector->setSendMessageMethod(
            [&](const auto& msg) {
                UNIT_ASSERT(msg->has_glagol_cluster_message());
                UNIT_ASSERT(msg->glagol_cluster_message().has_target_device_all());
                UNIT_ASSERT(msg->glagol_cluster_message().target_device_ids().empty());
                UNIT_ASSERT_VALUES_EQUAL(msg->glagol_cluster_message().service_name(), "test");
                UNIT_ASSERT_VALUES_UNEQUAL(msg->glagol_cluster_message().quasar_message_base64(), "");

                auto binaryQuasarMessage = base64Decode(msg->glagol_cluster_message().quasar_message_base64());
                proto::QuasarMessage quasarMessage;
                Y_PROTOBUF_SUPPRESS_NODISCARD quasarMessage.ParseFromString(TString(binaryQuasarMessage));
                UNIT_ASSERT(quasarMessage.has_wifi_list_request());

                msgReceived = true;
                return true;
            });

        GlagolClusterProvider glagolCluster(glagolConnector);

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });
        glagolCluster.send(GlagolClusterProvider::Target::ALL, "test", message);

        waitUntil([&] { return msgReceived == true; });
    }

} // Y_UNIT_TEST_SUITE(GlagolClusterProvider)
