#include <yandex_io/libs/base/base58.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <random>
#include <set>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestBase58, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testEncodingDecoding)
    {
        std::mt19937 generator(42);
        std::uniform_int_distribution d1(1, 100);
        std::uniform_int_distribution d2(0, 255);

        auto makeRandomData = [&] {
            std::vector<uint8_t> data(d1(generator));
            for (uint8_t& v : data)
            {
                v = static_cast<uint8_t>(d2(generator));
            }
            return data;
        };

        std::vector<uint8_t> originalData;
        std::vector<uint8_t> dacodedData;
        std::string encodedString;

        constexpr size_t iterationCount = 1000;
        std::set<std::vector<uint8_t>> uniqData;
        std::set<std::string> uniqEnc;
        for (size_t i = 0; i < iterationCount; i++) {
            originalData = makeRandomData();
            encodedString = base58Encode(originalData.data(), originalData.size());
            dacodedData = base58Decode(encodedString);
            UNIT_ASSERT(originalData == dacodedData);

            uniqData.insert(std::move(originalData));
            uniqEnc.insert(std::move(encodedString));
        }
        UNIT_ASSERT_VALUES_EQUAL(uniqEnc.size(), uniqData.size());
    }

    Y_UNIT_TEST(testEncoding)
    {
        UNIT_ASSERT_VALUES_EQUAL(base58Encode("abcdef"), "qVgfxYy3");

        UNIT_ASSERT_VALUES_EQUAL(base58Encode("Everyone is destined to die, but not every death has the same meaning"),
                                 "h7SjhgUam9x7JDeznv3D4QgopDr2QC2vagvQDpVuGa37EJD7F5y7hJ2CtZ8uMeAHc5CBeZeojcr2kB4pAJKtCmc6XaQKti");
    }
}
