#include <yandex_io/libs/ipc/datacratic/length_value_tokenizer.h>

#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;
using namespace quasar::ipc::detail::datacratic;

Y_UNIT_TEST_SUITE_F(TestLengthValueTokenizer, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(testLengthValueTokenizer) {
        LengthValueTokenizer tokenizer;

        std::atomic_int called = 0;
        tokenizer.onToken = [&](const std::string& message) {
            UNIT_ASSERT_VALUES_EQUAL(message, "token1");
            ++called;
        };

        tokenizer.pushData(LengthValueTokenizer::getLengthValue("token1"));
        UNIT_ASSERT_VALUES_EQUAL(called.load(), 1);

        tokenizer.onToken = [&](const std::string& message) {
            if (1 == called) {
                UNIT_ASSERT_VALUES_EQUAL(message, "token2");
            } else if (2 == called) {
                UNIT_ASSERT_VALUES_EQUAL(message, "token3_long");
            } else {
                UNIT_FAIL("called == " + std::to_string(called.load()));
            }
            ++called;
        };

        tokenizer.pushData(LengthValueTokenizer::getLengthValue("token2") +
                           LengthValueTokenizer::getLengthValue("token3_long"));
        UNIT_ASSERT_VALUES_EQUAL(called.load(), 3);

        std::string veryLongToken(65537, 't');
        tokenizer.onToken = [&](const std::string& message) {
            UNIT_ASSERT_VALUES_EQUAL(message, veryLongToken);
            ++called;
        };
        tokenizer.pushData(LengthValueTokenizer::getLengthValue(veryLongToken));
        UNIT_ASSERT_VALUES_EQUAL(called.load(), 4);

        tokenizer.onToken = [&](const std::string& message) {
            UNIT_ASSERT(message.empty());
            ++called;
        };
        tokenizer.pushData(LengthValueTokenizer::getLengthValue(""));
        UNIT_ASSERT_VALUES_EQUAL(called.load(), 5);

        std::string token4 = LengthValueTokenizer::getLengthValue("token4");
        std::string token5 = LengthValueTokenizer::getLengthValue("token5");
        tokenizer.onToken = [&](const std::string& message) {
            if (5 == called) {
                UNIT_ASSERT_VALUES_EQUAL(message, "token4");
            } else if (6 == called) {
                UNIT_ASSERT_VALUES_EQUAL(message, "token5");
            } else {
                UNIT_FAIL("called == " + std::to_string(called.load()));
            }
            ++called;
        };

        tokenizer.pushData(std::string(1, token4[0]));
        tokenizer.pushData(std::string(1, token4[1]));
        tokenizer.pushData(token4.substr(2) + token5.substr(0, 3));
        tokenizer.pushData(token5.substr(3, 3));
        tokenizer.pushData(token5.substr(6));

        UNIT_ASSERT_VALUES_EQUAL(called.load(), 7);

        std::string token6(129, 'x');
        tokenizer.onToken = [&](const std::string& message) {
            UNIT_ASSERT_VALUES_EQUAL(message, token6);
            ++called;
        };
        tokenizer.pushData(LengthValueTokenizer::getLengthValue(token6));
        UNIT_ASSERT_VALUES_EQUAL(called.load(), 8);
    }

    Y_UNIT_TEST(testGetLengthValue) {
        const std::string value = "abracadabra";
        std::string lengthValue = LengthValueTokenizer::getLengthValue(value);
        const uint32_t length = *(uint32_t*)lengthValue.data();
        UNIT_ASSERT_VALUES_EQUAL(length, value.length());
        UNIT_ASSERT_VALUES_EQUAL(lengthValue.substr(4), value);
    }
}
