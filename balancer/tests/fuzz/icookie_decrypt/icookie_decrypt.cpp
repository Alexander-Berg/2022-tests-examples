#include <balancer/kernel/http/parser/tests/fuzzutil/crash.h>
#include <yweb/webdaemons/icookiedaemon/icookie_lib/icookie.h>

using namespace NIcookie;
using namespace NSrvKernel::NFuzzUtil;

static int TestIcookieDecrypt(const uint8_t* data, size_t size) noexcept {
    const TString hmac = "DB178888CB7D02E0410A1B9C2153DC3D";
    const TString aes = "AE5633672AC8F94FC15B06B83E173B23";

    TKeys secrets{ TKey{ hmac , aes } };
    TKeysSet storage(std::move(secrets), 0);;
    TIcookieEncrypter processor{ storage };
    TStringBuf cookie{ (const char*)data, size };

    try {
        auto result = processor.Decrypt(cookie);
        auto uid = result.GetUid();
        if (result.IsOk() && !uid) {
            if (!uid) {
                Crash(); // ok, but no result - logical error
            }
        } else {
            if (uid) {
                Crash(); // got uid where it should have appeared not
            }
        }
    } catch (yexception& e) {
        return 0;
    }
    return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return TestIcookieDecrypt(data, size);
}
