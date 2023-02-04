#include <yandex_io/libs/keymaster_proxy_client/payload_packer.h>
#include <yandex_io/libs/keymaster_proxy_client/protos/keymaster_proxy_client.pb.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

using namespace quasar;
using namespace quasar::keymaster_proxy_client;
using namespace quasar::keymaster_proxy_client::proto;

namespace {

    Y_UNIT_TEST_SUITE(PayloadPackerTest) {
        Y_UNIT_TEST(testPackSign) {
            Request request = packSignRequest("hello");

            Y_ASSERT(request.has_sign_request());
            Y_ASSERT(request.sign_request().has_payload());
            Y_ASSERT(request.sign_request().payload() == "hello");
            Y_ASSERT(!request.has_decrypt_request());
        }

        Y_UNIT_TEST(testPackSign_with_zeroByte) {
            std::string text = "hello";
            text += '\0';
            text += "world";
            Request request = packSignRequest(text);

            Y_ASSERT(request.has_sign_request());
            Y_ASSERT(request.sign_request().has_payload());
            Y_ASSERT(request.sign_request().payload() == text);
            Y_ASSERT(!request.has_decrypt_request());
        }

        Y_UNIT_TEST(testPackDecrypt) {
            Request request = packDecryptRequest("ciphertext");

            Y_ASSERT(request.has_decrypt_request());
            Y_ASSERT(request.decrypt_request().has_payload());
            Y_ASSERT(request.decrypt_request().payload() == "ciphertext");
            Y_ASSERT(!request.has_sign_request());
        }

        Y_UNIT_TEST(testPackDecrypt_with_zeroByte) {
            std::string ciphertext = "cipher";
            ciphertext += '\0';
            ciphertext += "text";
            Request request = packDecryptRequest(ciphertext);

            Y_ASSERT(request.has_decrypt_request());
            Y_ASSERT(request.decrypt_request().has_payload());
            Y_ASSERT(request.decrypt_request().payload() == ciphertext);
            Y_ASSERT(!request.has_sign_request());
        }

        Y_UNIT_TEST(testUnpackSignResponse) {
            Response response;
            response.mutable_sign_response()->set_payload("signed text");

            const std::string signedtext = unpackResponse(response);

            UNIT_ASSERT_VALUES_EQUAL("signed text", signedtext);
        }

        Y_UNIT_TEST(testUnpackDecryptResponse) {
            Response response;
            response.mutable_decrypt_response()->set_payload("decrypted text");

            const std::string text = unpackResponse(response);

            UNIT_ASSERT_VALUES_EQUAL("decrypted text", text);
        }
    }
} // namespace
