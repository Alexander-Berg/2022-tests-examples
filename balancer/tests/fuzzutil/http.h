#pragma once

#include <balancer/kernel/http/parser/http.h>

#include <util/system/compiler.h>

namespace NSrvKernel {
    namespace NFuzzUtil {
        // Checks request line and headers.
        void CheckRequest(const TRequest& request);
        // Checks response line and headers.
        void CheckResponse(const TResponse& response);
    }
}
