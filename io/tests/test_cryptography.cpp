#include <yandex_io/libs/cryptography/cryptography.h>
#include <yandex_io/libs/cryptography/digest.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;

Y_UNIT_TEST_SUITE(Cryptography) {
    Y_UNIT_TEST(EncryptDecrypt) {
        Cryptography crypto;

        crypto.loadPublicKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/public.pem");
        std::string encrypted = crypto.encrypt("Hello");

        UNIT_ASSERT_VALUES_UNEQUAL(encrypted, "Hello");

        crypto.loadPrivateKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/private.pem");
        std::string decrypted = crypto.decrypt(encrypted);

        UNIT_ASSERT_VALUES_EQUAL(decrypted, "Hello");
    }

    Y_UNIT_TEST(Sign) {
        Cryptography crypto;

        crypto.loadPrivateKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/private.pem");

        std::string signature = crypto.sign("Hello");

        crypto.loadPublicKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/public.pem");
        UNIT_ASSERT(!crypto.checkSignature("Hello", "abracadabra"));
        UNIT_ASSERT(crypto.checkSignature("Hello", signature));
    }

    Y_UNIT_TEST(CompareToOpenssl) {
        Cryptography crypto;

        crypto.loadPrivateKey(ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/private.pem");

        std::string signature = crypto.sign("foo\n");

        /*
         * Just to make sure we got RSA signature right this time..
         *
         * $ echo 'foo' | openssl dgst -sha256 -sign ../../misc/cryptography/private.pem | md5sum -
         * 4c4923bfea49a365097eb2f555053e15  -
         * $
         *
         */
        UNIT_ASSERT_VALUES_EQUAL(quasar::calcMD5Digest(signature), "4c4923bfea49a365097eb2f555053e15");
    }

    Y_UNIT_TEST(KeyPair) {
        const auto pairA = Cryptography::KeyPair::fromFiles(
            ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/public.pem",
            ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/private.pem");

        const auto pairB = Cryptography::KeyPair::fromPrivateKeyFile(
            ArcadiaSourceRoot() + "/yandex_io/misc/cryptography/private.pem");

        UNIT_ASSERT_VALUES_EQUAL(pairA.publicKey, pairB.publicKey);
        UNIT_ASSERT_VALUES_EQUAL(pairA.privateKey, pairB.privateKey);
    }
}
