#include <yandex_io/libs/keymaster_proxy_client/message_packer.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <string>

#include <arpa/inet.h>

using namespace quasar;
using namespace quasar::keymaster_proxy_client;

namespace {
    const uint32_t MAGIC = 0x42fa851f;

    class FakeSignerTransport: public SignerTransportBase {
    public:
        FakeSignerTransport(std::vector<char> data)
            : data_(data)
                  {};

        void send(const std::vector<char>& /*data*/) override {
        }

        std::vector<char> receive(int size) override {
            std::vector<char> result = std::vector<char>(
                data_.begin() + offset_,
                data_.begin() + offset_ + size);
            offset_ += size;
            return result;
        }

    private:
        uint32_t offset_ = 0;
        std::vector<char> data_;
    };

    Y_UNIT_TEST_SUITE(MessagePackerTest) {
        Y_UNIT_TEST(testPack) {
            MessagePacker packer;
            const std::string payload = "foo bar";

            const std::vector<char> bytes = packer.pack(payload);

            int actualMagic;
            std::memcpy(&actualMagic, bytes.begin(), sizeof(uint32_t));

            int length;
            std::memcpy(&length, bytes.begin() + sizeof(uint32_t), sizeof(uint32_t));

            std::string actualPayload(bytes.begin() + 2 * sizeof(uint32_t), bytes.end());

            UNIT_ASSERT_VALUES_EQUAL(HostToInet<uint32_t>(MAGIC), actualMagic);
            UNIT_ASSERT_VALUES_EQUAL(HostToInet<uint32_t>(payload.length()), length);
            UNIT_ASSERT_VALUES_EQUAL(actualPayload, payload);
        }

        Y_UNIT_TEST(testPack_with_zeroByte) {
            MessagePacker packer;
            std::string payload = "foo";
            payload += '\0';
            payload += "bar";

            const std::vector<char> bytes = packer.pack(payload);

            int actualMagic;
            std::memcpy(&actualMagic, bytes.begin(), sizeof(uint32_t));

            int length;
            std::memcpy(&length, bytes.begin() + sizeof(uint32_t), sizeof(uint32_t));

            std::string actualPayload(bytes.begin() + 2 * sizeof(uint32_t), bytes.end());

            UNIT_ASSERT_VALUES_EQUAL(HostToInet<uint32_t>(MAGIC), actualMagic);
            UNIT_ASSERT_VALUES_EQUAL(HostToInet<uint32_t>(payload.length()), length);
            UNIT_ASSERT_VALUES_EQUAL(actualPayload, payload);
        }

        Y_UNIT_TEST(testUnpack) {
            MessagePacker packer;
            FakeSignerTransport transport(packer.pack("this is a fake message"));
            const std::string payload = packer.unpack(transport);

            UNIT_ASSERT_VALUES_EQUAL("this is a fake message", payload);
        }
    }
} // namespace
