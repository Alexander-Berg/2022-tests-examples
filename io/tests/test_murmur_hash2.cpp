#include <yandex_io/callkit/util/murmur_hash2.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <cstring>

namespace {

    std::string decodeHex(const char* input) {
        static const char* const digits = "0123456789abcdef";
        size_t len = strlen(input);
        std::string output;
        output.reserve(len / 2);
        for (size_t i = 0; i < len; i += 2) {
            const char* p = std::lower_bound(digits, digits + 16, input[i]);
            const char* q = std::lower_bound(digits, digits + 16, input[i + 1]);
            output.push_back(((p - digits) << 4) | (q - digits));
        }
        return output;
    }

} // namespace

Y_UNIT_TEST_SUITE_F(murmurHash, QuasarUnitTestFixture) {
    const auto& murmurHash2 = ::messenger::murmurHash2;

    Y_UNIT_TEST(basic_test) {
        UNIT_ASSERT_VALUES_EQUAL(0u, murmurHash2(""));
        UNIT_ASSERT_VALUES_EQUAL(636687721u, murmurHash2("A"));
        UNIT_ASSERT_VALUES_EQUAL(636687721u, murmurHash2("A"));
        UNIT_ASSERT_VALUES_EQUAL(2493417937u, murmurHash2("Test"));
        UNIT_ASSERT_VALUES_EQUAL(1178050029u, murmurHash2(decodeHex("380a")));
        UNIT_ASSERT_VALUES_EQUAL(1178050029u, murmurHash2("8\n"));
        UNIT_ASSERT_VALUES_EQUAL(1918338759u,
                                 murmurHash2("An online implementation of MurmurHash"));
        UNIT_ASSERT_VALUES_EQUAL(
            54935095u,
            murmurHash2(decodeHex(
                "124931656561383233302d396164372d346265642d396235642d613965"
                "6437656531326162315f35646131353737372d643165312d343935622d"
                "626262322d38616333666632613431363220642881c8f5a6baa5e402")));
    }
}
