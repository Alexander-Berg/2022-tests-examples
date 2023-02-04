#include <yandex_io/libs/cryptography/digest.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE(Digest) {
    Y_UNIT_TEST(MD5) {
        UNIT_ASSERT_VALUES_EQUAL(quasar::calcMD5Digest("YandexIO"), "46a9a58660333100b3b68adb78d001f3");
    }

    Y_UNIT_TEST(SHA1) {
        UNIT_ASSERT_VALUES_EQUAL(quasar::calcSHA1Digest("YandexIO"), "1b014d35f8567c7db6f7e918e3877c98b93205ee");
    }

    Y_UNIT_TEST(testMd5Hasher0) {
        UNIT_ASSERT_VALUES_EQUAL(quasar::Md5Hasher{}.hashString(), "00000000000000000000000000000000");
    }

    Y_UNIT_TEST(testMd5HasherUpdateCharStart) {
        quasar::Md5Hasher hasher;
        hasher.update("YandexIO");
        hasher.finalize();
        UNIT_ASSERT_VALUES_EQUAL(hasher.hashString(), "46a9a58660333100b3b68adb78d001f3");
    }

    Y_UNIT_TEST(testMd5HasherUpdateString) {
        quasar::Md5Hasher hasher;
        hasher.update(std::string("YandexIO"));
        hasher.finalize();
        UNIT_ASSERT_VALUES_EQUAL(hasher.hashString(), "46a9a58660333100b3b68adb78d001f3");
    }

    Y_UNIT_TEST(testMd5HasherUpdateStringView) {
        quasar::Md5Hasher hasher;
        hasher.update(std::string_view("YandexIO"));
        hasher.finalize();
        UNIT_ASSERT_VALUES_EQUAL(hasher.hashString(), "46a9a58660333100b3b68adb78d001f3");
    }

    Y_UNIT_TEST(testMd5HasherUint8VsUin32) {
        quasar::Md5Hasher hasher;
        hasher.update(uint32_t{100});
        UNIT_ASSERT_VALUES_UNEQUAL(
            quasar::Md5Hasher{}.update(uint8_t{100}).finalize().hash(),
            quasar::Md5Hasher{}.update(uint32_t{100}).finalize().hash());
    }

    Y_UNIT_TEST(testMd5HasherCancat) {
        quasar::Md5Hasher hasher;
        hasher.update("Yand");
        hasher.update("exIO");
        UNIT_ASSERT_VALUES_EQUAL(hasher.finalize().hashString(), "46a9a58660333100b3b68adb78d001f3");
    }
    Y_UNIT_TEST(testMd5HasherHashStringWithPartBytes) {
        quasar::Md5Hasher hasher;
        hasher.update("YandexIO");
        UNIT_ASSERT_VALUES_EQUAL(hasher.finalize().hashString(quasar::Md5Hasher::Encoder::HEX, 4), "46a9a586");
    }

    Y_UNIT_TEST(testMd5HasherHashStringWithBase64) {
        quasar::Md5Hasher hasher;
        hasher.update("YandexIO");
        UNIT_ASSERT_VALUES_EQUAL(hasher.finalize().hashString(quasar::Md5Hasher::Encoder::BASE64), "RqmlhmAzMQCztorbeNAB8w==");
    }
    Y_UNIT_TEST(testMd5HasherHashStringWithBase64PartBytes) {
        quasar::Md5Hasher hasher;
        hasher.update("YandexIO");
        UNIT_ASSERT_VALUES_EQUAL(hasher.finalize().hashString(quasar::Md5Hasher::Encoder::BASE64, 6), "RqmlhmAz");
    }

    Y_UNIT_TEST(testMd5HasherHashStringWithBase58) {
        quasar::Md5Hasher hasher;
        hasher.update("YandexIO");
        UNIT_ASSERT_VALUES_EQUAL(hasher.finalize().hashString(quasar::Md5Hasher::Encoder::BASE58), "9j6MUozXJtDAdzP2ZNfaSe");
    }
    Y_UNIT_TEST(testMd5HasherHashStringWithBase58PartBytes) {
        quasar::Md5Hasher hasher;
        hasher.update("YandexIO");
        UNIT_ASSERT_VALUES_EQUAL(hasher.finalize().hashString(quasar::Md5Hasher::Encoder::BASE58, 6), "cBuGeoRL");
    }

    Y_UNIT_TEST(testXXH64Hasher) {
        UNIT_ASSERT_VALUES_EQUAL(XXH64Hasher{}.update("123").hash(), XXH64Hasher{}.update(std::string("123")).hash());
        UNIT_ASSERT_VALUES_EQUAL(XXH64Hasher{}.update("123").hash(), XXH64Hasher{}.update(std::string_view("123")).hash());

        UNIT_ASSERT_VALUES_EQUAL(XXH64Hasher{}.update("YandexIO").hash(), XXH64Hasher{}.update("Yandex").update("IO").hash());

        UNIT_ASSERT_VALUES_UNEQUAL(XXH64Hasher{0}.update("123").hash(), XXH64Hasher{1}.update("123").hash());
        UNIT_ASSERT_VALUES_UNEQUAL(XXH64Hasher{}.update(static_cast<uint8_t>(123)).hash(), XXH64Hasher{}.update(static_cast<uint16_t>(123)).hash());
    }
}
