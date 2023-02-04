#include <balancer/kernel/http2/server/hpack/tests/common/hpack_test_common.h>
#include <balancer/kernel/http2/server/hpack/hpack.h>
#include <balancer/kernel/http2/server/hpack/hpack_integers.h>

extern "C" int LLVMFuzzerTestOneInput(const ui8* const wireData, const size_t wireSize) {
    using namespace NSrvKernel::NHTTP2;
    using namespace NUt;

    TFuzzerData data;

    if (!ParseFuzzerInput(data, {(const char*)wireData, wireSize})) {
        return 0;
    }

    TestHPackOnFuzzerInput(data);

    return 0;
}
