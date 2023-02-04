#include <balancer/kernel/http2/server/utils/http2_queue.h>

#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/xrange.h>
#include <util/generic/deque.h>


Y_UNIT_TEST_SUITE(THTTP2QueueTest) {
    using namespace NSrvKernel::NHTTP2;

    struct TItem {
        const ui64 Id;
        ui64 Prio = 0;

        TItem(ui64 id)
            : Id(id)
            , Prio(id)
        {}
    };

    using TQueueItem = TQueueItem<TItem>;

    using TH2Queue = IQueue<TItem>;

    using TQueueItems = TDeque<TQueueItem>;
    using TChecker = std::function<void(const TH2Queue&, const TQueueItem&, const TQueueItems&)>;
    using TUpdater = std::function<void(TItem&, const TQueueItems&)>;

    struct TPrioMore {
        bool operator() (const TItem& a, const TItem& b) const {
            return a.Prio > b.Prio;
        }
    };

    static void NoUpdate(TItem&, const TQueueItems&) {}

    static void CheckNoUpdate(const TH2Queue& queue, const TQueueItem&, const TQueueItems&) {
        UNIT_ASSERT(queue.GetNext());
        UNIT_ASSERT_EQUAL(queue.GetNext()->Get().Id, 0);
    }

    static void UpdateRoundRobin(TItem& item, const TQueueItems& items) {
        item.Prio += items.size();
    }

    static void CheckUpdateRoundRobin(const TH2Queue& queue, const TQueueItem& item, const TQueueItems& items) {
        UNIT_ASSERT(queue.GetNext());
        UNIT_ASSERT_EQUAL(queue.GetNext()->Get().Id, (item.Get().Id + 1) % items.size());
    }

    void DoTestQueue(TH2Queue& queue, TChecker insertChecker, TChecker updateChecker, TUpdater updater) {
        for (auto itemsCount : xrange<ui64>(1, 13)) {
            {
                TDeque<TItem> theItems;
                TDeque<TQueueItem> items;
                for (auto id : xrange<ui64>(0, itemsCount)) {
                    items.emplace_back(
                        theItems.emplace_back(id)
                    );
                }

                for (auto& item : items) {
                    UNIT_ASSERT(!item.IsLinked());
                    queue.Insert(item);
                    insertChecker(queue, item, items);
                    UNIT_ASSERT(item.IsLinked());
                }

                UNIT_ASSERT(!queue.Empty());
                UNIT_ASSERT_EQUAL(queue.GetNext(), &items.front());

                for (auto& item : items) {
                    updater(item.Get(), items);
                    queue.Update(item);
                    updateChecker(queue, item, items);
                    UNIT_ASSERT(item.IsLinked());
                }

                for (auto& item : items) {
                    UNIT_ASSERT_EQUAL(queue.GetNext(), &item);
                    queue.Remove(item);
                    UNIT_ASSERT(!item.IsLinked());
                }

                UNIT_ASSERT(queue.Empty());
                UNIT_ASSERT(!queue.GetNext());

                for (auto& item : items) {
                    queue.Insert(item);
                    UNIT_ASSERT(item.IsLinked());
                }

                UNIT_ASSERT(!queue.Empty());
                UNIT_ASSERT_EQUAL(queue.GetNext(), &items.front());
            }
            UNIT_ASSERT(queue.Empty());
            UNIT_ASSERT(!queue.GetNext());
        }
    }


    Y_UNIT_TEST(TestStaticFIFO) {
        TStaticFIFOQueue<TItem> queue;
        DoTestQueue(queue, CheckNoUpdate, CheckNoUpdate, NoUpdate);
    }


    Y_UNIT_TEST(TestRoundRobinFIFO) {
        TRoundRobinQueue<TItem> queue;
        DoTestQueue(queue, CheckNoUpdate, CheckUpdateRoundRobin, NoUpdate);
    }


    Y_UNIT_TEST(TestStaticHeap) {
        THeapQueueBase<TItem, TPrioMore> queue;
        DoTestQueue(queue, CheckNoUpdate, CheckNoUpdate, NoUpdate);
    }


    Y_UNIT_TEST(TestRoundRobinHeap) {
        THeapQueueBase<TItem, TPrioMore> queue;
        DoTestQueue(queue, CheckNoUpdate, CheckUpdateRoundRobin, UpdateRoundRobin);
    }
}
