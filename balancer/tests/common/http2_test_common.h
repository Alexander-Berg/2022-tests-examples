#pragma once

#include <balancer/kernel/http2/server/utils/http2_flat_heap.h>

#include <util/generic/algorithm.h>
#include <util/generic/deque.h>
#include <util/generic/yexception.h>
#include <util/generic/bt_exception.h>

#include <functional>

namespace NSrvKernel::NHTTP2 {
    namespace NUt {
#define HTTP2_ASSERT_C(cond, mess) \
        Y_ENSURE_EX(cond, TWithBackTrace<yexception>() << Y_STRINGIZE(cond) << " assertion failed " << mess)

#define HTTP2_ASSERT(cond) \
        HTTP2_ASSERT_C(cond, "")

#define HTTP2_ASSERT_EQ_C(a, b, mess) HTTP2_ASSERT_C((a) == (b), \
        " (" << (a) << " == " << (b) << ") " << mess)

#define HTTP2_ASSERT_EQ(a, b) \
        HTTP2_ASSERT_EQ_C(a, b, "")

        namespace NFlatHeapUt {
            struct TTestItem : public TIntrusiveFlatHeapItem<TTestItem> {
                explicit TTestItem(ui64 id, ui64 val)
                    : Id(id)
                    , Value(val)
                {}
                friend bool operator<(const TTestItem& x, const TTestItem& y) {
                    return x.Id < y.Id;
                }
                friend bool operator==(const TTestItem& x, const TTestItem& y) {
                    return x.Id == y.Id;
                }
                ui64 Id = 0;
                ui64 Value = 0;
            };

            template <class T>
            void AssertFlatHeapStructure(TIntrusiveFlatHeap<T>& heap) {
                if (heap.Empty()) {
                    HTTP2_ASSERT_EQ(heap.Size(), 0);
                    HTTP2_ASSERT_EQ(static_cast<void*>(heap.GetMin()), nullptr);
                    return;
                }
                HTTP2_ASSERT(heap.Size() > 0);
                HTTP2_ASSERT(heap.GetMin() != nullptr);

                auto size = heap.Size();
                TVector<typename TIntrusiveFlatHeap<T>::TItem*> items;
                while (!heap.Empty()) {
                    items.push_back(heap.ExtractMin());
                }
                HTTP2_ASSERT_EQ(size, items.size());
                HTTP2_ASSERT(IsSorted(items.begin(), items.end(), [](const auto* a, const auto* b) {
                    return a->Get() < b->Get();
                }));
                for (auto* item : items) {
                    heap.Insert(*item);
                }
            }
        }
    }
}
