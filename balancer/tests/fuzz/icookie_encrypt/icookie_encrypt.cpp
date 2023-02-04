#include <balancer/kernel/http/parser/tests/fuzzutil/crash.h>

#include <yweb/webdaemons/icookiedaemon/icookie_lib/icookie.h>
#include <yweb/webdaemons/icookiedaemon/icookie_lib/process.h>

using namespace NIcookie;
using namespace NSrvKernel::NFuzzUtil;

static int TestIcookieEncrypt(const uint8_t* data, size_t size) noexcept {
    try {
        const TString hmac = "DB178888CB7D02E0410A1B9C2153DC3D";
        const TString aes = "AE5633672AC8F94FC15B06B83E173B23";

        TKeys secrets{ TKey{ hmac , aes } };
        TKeysSet storage(std::move(secrets), 0);;
        TIcookieEncrypter processor{ storage };

        TStringBuf yandexuid{ (const char*)data, size };

        TUid uid;

        if (!GenerateUidFromYandexuid(yandexuid, TUid::ESource::Yandexuid, uid)) {
            uid = TUid::GenerateNew();
        }

        auto result = processor.Encrypt(uid);
        if (result.IsOk()) {
            auto uidResult = result.GetUidEncrypted();
            if (!uidResult) {
                Crash(); // encrypted ok, but no result - logical error
            }
        } else {
            auto uidResult = result.GetUidEncrypted();
            if (uidResult) {
                Crash(); // encryption failed, but got result - logical error
            }
        }
    } catch (yexception& e) {
    }
    return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return TestIcookieEncrypt(data, size);
}
