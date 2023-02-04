#include <yandex_io/libs/device_cryptography/plain_file/plain_file_device_cryptography.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace YandexIO;

namespace {
    Cryptography::KeyPair loadKeyPair() {
        return Cryptography::KeyPair::fromFiles(
            ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/public.pem",
            ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/private.pem");
    }
} // namespace

Y_UNIT_TEST_SUITE(PlainFileDeviceCryptography) {
    Y_UNIT_TEST(EncryptDecrypt) {
        auto keyPair = loadKeyPair();

        Cryptography crypto;
        crypto.setKeyPair(keyPair);

        auto deviceCrypto = PlainFileDeviceCryptography(keyPair);
        UNIT_ASSERT_VALUES_EQUAL(deviceCrypto.getEncryptPublicKey(), keyPair.publicKey);

        std::string encrypted = crypto.encrypt("Hello");
        std::string decrypted = deviceCrypto.decrypt(encrypted);
        UNIT_ASSERT_VALUES_EQUAL(decrypted, "Hello");
    }

    Y_UNIT_TEST(Sign) {
        auto keyPair = loadKeyPair();

        Cryptography crypto;
        crypto.setKeyPair(keyPair);

        auto deviceCrypto = PlainFileDeviceCryptography(keyPair);

        std::string signature = deviceCrypto.sign("Hello");
        UNIT_ASSERT(crypto.checkSignature("Hello", signature));
    }
}
