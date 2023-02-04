#include <yweb/webdaemons/icookiedaemon/icookie_lib/icookie.h>

using namespace NIcookie;

static int TestIcookieSecrets(const uint8_t* data, size_t size) noexcept {
    try {
        TStringBuf hmac((const char*)data, size);
        TStringBuf aes((const char*)data, size);

        TKeys secrets{ TKey{ hmac , aes } };
        TKeysSet storage(std::move(secrets), 0);
    } catch (yexception& e) {
    }
    return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return TestIcookieSecrets(data, size);
}
