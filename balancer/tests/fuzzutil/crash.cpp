#include "crash.h"

namespace NSrvKernel {
    namespace NFuzzUtil {
        void Crash() {
            char* p = nullptr;
            *p = 'c'; // explicitly crashing
        }
    }
}
