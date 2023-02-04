#pragma once

#include <util/system/types.h>

namespace NRTYServer {
    namespace NJupiTest {
        extern const ui32 PORTIONS_COUNT;
        extern const ui32 PORTION_SIZE_STEP;
        extern const ui32 MIN_DOC_COUNT;
        extern const ui32 MIN_REMOVE_COUNT;
        extern const double MAX_DEADLINE_DOCS_FACTOR;  // Merger.MaxDeadlineDocs = MAX_DEADLINE_DOCS_FACTOR * TotalDocumentCount;

        inline size_t GetPortionSize(size_t nMessages, size_t nPortions, size_t step) {
            size_t stepSize = step * nPortions;
            size_t nSteps = (nMessages + stepSize - 1) / stepSize;
            return nSteps * step;
        }

        inline size_t GetPortionSize(size_t nMessages) {
            return GetPortionSize(nMessages, PORTIONS_COUNT, PORTION_SIZE_STEP);
        }
    }
}
