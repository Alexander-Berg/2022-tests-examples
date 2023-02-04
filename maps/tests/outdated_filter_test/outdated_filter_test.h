#pragma once
#include <maps/analyzer/libs/signal_filters/include/signal_consumer.h>

namespace maps {
namespace analyzer {
namespace signal_filters {
    SignalType generateSignalTypeByIndex(size_t index) {
        index %= 24;
        if (index < 6)
            return UNCLASSIFIED_SIGNAL;
        else if (index < 12)
            return MOVING_SIGNAL;
        else if (index < 18)
            return STANDING_SIGNAL;
        else
            return UNCLASSIFIED_SIGNAL;
    }
}
}
}
