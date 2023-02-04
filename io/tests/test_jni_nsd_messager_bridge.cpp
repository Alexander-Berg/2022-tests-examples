#include <yandex_io/android_sdk/cpp/launcher/global_context.h>
#include <yandex_io/android_sdk/cpp/launcher/jni_nsd_messager_bridge.h>

#include <yandex_io/libs/mdns/nsd_messager.h>
#include <yandex_io/libs/mdns/nsd_receiver.h>
#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/unittest/registar.h>

#include <vector>

using namespace quasar;

namespace {
    class NsdReceiverStub: public INsdReceiver {
    public:
        void onNsdDiscovered(const proto::NsdInfo& nsdInfo) override {
            discoveredHosts_.push_back(nsdInfo);
        }

        void onNsdListDiscovered(const proto::NsdInfoList& nsdInfoLists) override {
            discoveredHostLists_.push_back(nsdInfoLists);
        }

        void onNsdLost(const proto::NsdInfo& nsdInfo) override {
            lostHosts_.push_back(nsdInfo);
        }

        const std::vector<proto::NsdInfo>& discoveredHosts() const {
            return discoveredHosts_;
        }

        const std::vector<proto::NsdInfoList>& discoveredHostLists() const {
            return discoveredHostLists_;
        }

        const std::vector<proto::NsdInfo>& lostHosts() const {
            return lostHosts_;
        }

    private:
        std::vector<proto::NsdInfo> discoveredHosts_;
        std::vector<proto::NsdInfoList> discoveredHostLists_;
        std::vector<proto::NsdInfo> lostHosts_;
    };

    class NsdMessagerStub: public INsdMessager {
    public:
        NsdMessagerStub()
            : enableNsdSent_{false}
        {
        }

        void enableNsd(bool /*guestMode*/, uint32_t /*port*/, OptBool /*stereopair*/, OptBool /*tandem*/) override {
            enableNsdSent_ = true;
        }

        void disableNsd() override {
        }

        bool enableNsdSent() const {
            return enableNsdSent_;
        }

    private:
        bool enableNsdSent_;
    };

} // namespace

Y_UNIT_TEST_SUITE(TestJniNsdMessagerFactory)
{
    Y_UNIT_TEST(TestGlagolEventsSentAfterRegistration) {
        auto factory = std::make_shared<JniNsdMessagerFactory>();
        auto receiver = std::make_unique<NsdReceiverStub>();
        auto receiverPtr = receiver.get();
        auto messagerBridge = factory->createMessager(std::move(receiver));
        auto messager = std::make_unique<NsdMessagerStub>();
        auto messagerPtr = messager.get();
        factory->setJniNsdMessager(std::move(messager));

        messagerBridge->enableNsd(false, 1000, {}, {});

        proto::NsdInfo discoveredHost;
        discoveredHost.set_name("discovered_host");
        proto::NsdInfo lostHost;
        lostHost.set_name("lost_host");
        proto::NsdInfoList hostList;
        hostList.add_items()->set_name("host_in_list");
        factory->nsdReceiver().onNsdDiscovered(discoveredHost);
        factory->nsdReceiver().onNsdLost(lostHost);
        factory->nsdReceiver().onNsdListDiscovered(hostList);

        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHosts().size(), 1);
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHosts()[0].name(), discoveredHost.name());
        UNIT_ASSERT_EQUAL(receiverPtr->lostHosts().size(), 1);
        UNIT_ASSERT_EQUAL(receiverPtr->lostHosts()[0].name(), lostHost.name());
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists().size(), 1);
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists()[0].items_size(), hostList.items_size());
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists()[0].items(0).name(), hostList.items(0).name());

        UNIT_ASSERT(messagerPtr->enableNsdSent());
    }

    Y_UNIT_TEST(TestGlagolEventsSentBeforeRegistration) {
        auto factory = std::make_shared<JniNsdMessagerFactory>();
        auto messagerBridge = factory->createMessager(std::make_unique<NsdReceiverStub>());
        messagerBridge->enableNsd(false, 1000, {}, {});
        auto messager = std::make_unique<NsdMessagerStub>();
        auto messagerPtr = messager.get();
        factory->setJniNsdMessager(std::move(messager));
        UNIT_ASSERT(messagerPtr->enableNsdSent());
    }

    Y_UNIT_TEST(TestJniEventsLostBeforeRegistration) {
        auto factory = std::make_shared<JniNsdMessagerFactory>();
        auto messager = std::make_unique<NsdMessagerStub>();
        factory->setJniNsdMessager(std::move(messager));

        proto::NsdInfo discoveredHost;
        discoveredHost.set_name("discovered_host");
        proto::NsdInfo lostHost;
        lostHost.set_name("lost_host");
        proto::NsdInfoList hostList;
        hostList.add_items()->set_name("host_in_list");
        factory->nsdReceiver().onNsdDiscovered(discoveredHost);
        factory->nsdReceiver().onNsdLost(lostHost);
        factory->nsdReceiver().onNsdListDiscovered(hostList);

        auto receiver = std::make_unique<NsdReceiverStub>();
        auto receiverPtr = receiver.get();
        auto messgerBridge = factory->createMessager(std::move(receiver));

        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHosts().size(), 0);
        UNIT_ASSERT_EQUAL(receiverPtr->lostHosts().size(), 0);
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists().size(), 0);
    }

    Y_UNIT_TEST(TestJniEventsSentAfterGlagolResets) {
        auto factory = std::make_shared<JniNsdMessagerFactory>();

        // it's important not to save shared_ptr in order to expire it
        // simulates glagol shutdown
        {
            auto messager = factory->createMessager(std::make_unique<NsdReceiverStub>());
            messager->enableNsd(false, 1000, {}, {});
        }

        auto messager = std::make_unique<NsdMessagerStub>();
        factory->setJniNsdMessager(std::move(messager));

        proto::NsdInfo discoveredHost;
        discoveredHost.set_name("discovered_host");
        proto::NsdInfo lostHost;
        lostHost.set_name("lost_host");
        proto::NsdInfoList hostList;
        hostList.add_items()->set_name("host_in_list");
        factory->nsdReceiver().onNsdDiscovered(discoveredHost);
        factory->nsdReceiver().onNsdLost(lostHost);
        factory->nsdReceiver().onNsdListDiscovered(hostList);

        // now recreate it and receive all messages
        // simulates glagol restart
        auto receiver = std::make_unique<NsdReceiverStub>();
        auto receiverPtr = receiver.get();
        auto messagerBridge = factory->createMessager(std::move(receiver));

        // Those items are lost
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHosts().size(), 0);
        UNIT_ASSERT_EQUAL(receiverPtr->lostHosts().size(), 0);
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists().size(), 0);

        factory->nsdReceiver().onNsdDiscovered(discoveredHost);
        factory->nsdReceiver().onNsdLost(lostHost);
        factory->nsdReceiver().onNsdListDiscovered(hostList);

        // After resend works fine
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHosts().size(), 1);
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHosts()[0].name(), discoveredHost.name());
        UNIT_ASSERT_EQUAL(receiverPtr->lostHosts().size(), 1);
        UNIT_ASSERT_EQUAL(receiverPtr->lostHosts()[0].name(), lostHost.name());
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists().size(), 1);
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists()[0].items_size(), hostList.items_size());
        UNIT_ASSERT_EQUAL(receiverPtr->discoveredHostLists()[0].items(0).name(), hostList.items(0).name());
    }
}
