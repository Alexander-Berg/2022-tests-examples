#include <kernel/signurl/keyholder.h>
#include <util/string/cast.h>

static int TestSignKeys(const uint8_t *data, size_t size) {
    if (Y_UNLIKELY(size == 0)) {
        return 0;
    }
    try {
        TStringBuf signLine((const char *) data, size);
        auto singKeys = NSignUrl::TSignKeys::CreateFromLines(ToString(signLine));
    } catch (yexception& e) {
    }

    return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return TestSignKeys(data, size);
}
