#include <yandex_io/sdk/interfaces/i_capability.h>
#include <yandex_io/protos/quasar_proto.pb.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace YandexIO;

Y_UNIT_TEST_SUITE(SdkInterfacesCapability) {
    Y_UNIT_TEST(getId) {
        class CapabilityStub: public ICapability {
        public:
            NAlice::TCapabilityHolder getState() const override {
                NAlice::TCapabilityHolder holder;
                holder.mutable_onoffcapability();
                return holder;
            }
            IDirectiveHandlerPtr getDirectiveHandler() override {
                return nullptr;
            }
            void addListener(std::weak_ptr<IListener> /*wlistener*/) override {
            }
            void removeListener(std::weak_ptr<IListener> /*wlistener*/) override {
            }
        } stub;
        UNIT_ASSERT_VALUES_EQUAL(stub.getId(), "OnOffCapability");
    }
}
