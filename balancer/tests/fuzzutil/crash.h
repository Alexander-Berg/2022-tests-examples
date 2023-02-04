#pragma once

#include <util/system/compiler.h>

namespace NSrvKernel {
    namespace NFuzzUtil {
        // Crashes fuzzer via writing to nullptr.
        //
        // The backtrace of crash looks much pretty in fuzzer error message with
        // this approach.
        void Crash();
    }
}
