#pragma once

#include <util/system/sanitizers.h>

namespace NSan {
    // Determines if ubsan present
    inline constexpr static bool UBSanIsOn() noexcept {
#if defined(_ubsan_enabled_)
        return true;
#else
        return false;
#endif
    }

    inline constexpr static bool SanIsOn() noexcept {
#if defined(_san_enabled_)
        return true;
#else
        return false;
#endif
    }
}
