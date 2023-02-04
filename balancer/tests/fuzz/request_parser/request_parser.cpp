#include <balancer/kernel/http/parser/http.h>

#include <balancer/kernel/http/parser/tests/fuzzutil/test_request.h>

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
    return NSrvKernel::NFuzzUtil::TestRequest(data, size);
}
