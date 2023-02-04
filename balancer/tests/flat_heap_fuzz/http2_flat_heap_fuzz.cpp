#include <balancer/kernel/http2/server/utils/tests/common/http2_test_common.h>
#include <util/system/types.h>
#include <util/generic/xrange.h>
#include <util/generic/hash.h>

#include <string>

extern "C" int LLVMFuzzerTestOneInput(const ui8* const wireData, const size_t wireSize) {
    using namespace NSrvKernel::NHTTP2;
    using namespace NUt;
    using namespace NUt::NFlatHeapUt;

    TDeque<TTestItem> nodes;
    THashMap<ui64, TTestItem*> nodesReg;
    TIntrusiveFlatHeap<TTestItem> heap;

    for (auto i : xrange(wireSize)) {
        auto data = wireData[i];
        TTestItem** item = nodesReg.FindPtr(data);

        if (!item) {
            item = &(nodesReg[data] = &nodes.emplace_back(data, data));
        }

        if ((*item)->IsLinked()) {
            if ((*item)->Get().Id > 127) {
                auto size = heap.Size();
                heap.Update(**item);
                HTTP2_ASSERT_EQ(size, heap.Size());
                HTTP2_ASSERT((*item)->IsLinked());
            } else {
                auto size = heap.Size();
                heap.Delete(**item);
                HTTP2_ASSERT_EQ(size - 1, heap.Size());
                HTTP2_ASSERT(!(*item)->IsLinked());
            }
        } else {
            auto size = heap.Size();
            heap.Insert(**item);
            HTTP2_ASSERT_EQ(size + 1, heap.Size());
            HTTP2_ASSERT((*item)->IsLinked());
        }

        AssertFlatHeapStructure(heap);
    }

    return 0;
}
