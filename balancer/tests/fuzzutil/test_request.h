#pragma once
#include <balancer/kernel/http/parser/http.h>

namespace NSrvKernel {
namespace NFuzzUtil {

    int TestRequest(const uint8_t *data, size_t size, THttpParseOptions options = {}) noexcept;

}
}
