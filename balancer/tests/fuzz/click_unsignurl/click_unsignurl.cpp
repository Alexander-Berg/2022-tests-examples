#include <kernel/signurl/signurl.h>
#include <balancer/modules/click/tests/fuzz/common.h>
#include <util/string/cast.h>

static int TestUnsignUrl(const uint8_t *data, size_t size) {
    try {
        TStringBuf url((const char*) data, size);
        NSignUrl::TSignKeys signKeys = NSignUrl::TSignKeys::CreateFromLines(SIGN_KEYS_STRING);

        NSignUrl::UnsignUrl(ToString(url), &signKeys);
    } catch (yexception&) {
    }

    return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return TestUnsignUrl(data, size);
}
