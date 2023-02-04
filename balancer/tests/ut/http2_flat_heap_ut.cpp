#include <balancer/kernel/http2/server/utils/tests/common/http2_test_common.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/deque.h>

#include <algorithm>

Y_UNIT_TEST_SUITE(HTTP2FlatHeapTest) {
    using namespace NSrvKernel::NHTTP2;

    class TTestNode : public TIntrusiveFlatHeapItem<TTestNode> {
    public:
        int Value;
        TTestNode(int v) : Value(v) {};
        friend bool operator<(const TTestNode& x, const TTestNode& y) noexcept {
            return x.Value < y.Value;
        }
        friend bool operator==(const TTestNode& x, const TTestNode& y) noexcept {
            return x.Value == y.Value;
        }
    };

    Y_UNIT_TEST(SmokingTest) {
        THolder<TTestNode> item = MakeHolder<TTestNode>(5);
        UNIT_ASSERT(!item->IsLinked());

        TIntrusiveFlatHeap<TTestNode> fh;
        UNIT_ASSERT(fh.Empty());

        fh.Insert(*item);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(1, fh.Size());
        UNIT_ASSERT_EQUAL(fh.GetMin()->Get(), TTestNode{ 5 });
        UNIT_ASSERT_EQUAL(fh.GetMin(), item.Get());
        UNIT_ASSERT(item->IsLinked());
        UNIT_ASSERT_EQUAL(item->GetHeap(), &fh);

        UNIT_ASSERT_EQUAL(fh.ExtractMin(), item.Get());
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT(!item->IsLinked());

        fh.Insert(*item);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(1, fh.Size());
        UNIT_ASSERT_EQUAL(fh.GetMin()->Get(), TTestNode{ 5 });
        UNIT_ASSERT_EQUAL(fh.GetMin(), item.Get());
        UNIT_ASSERT(item->IsLinked());
        UNIT_ASSERT_EQUAL(item->GetHeap(), &fh);

        fh.Delete(*item);
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT(!item->IsLinked());

        fh.Insert(*item);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(1, fh.Size());
        UNIT_ASSERT_EQUAL(fh.GetMin()->Get(), TTestNode{ 5 });
        UNIT_ASSERT_EQUAL(fh.GetMin(), item.Get());
        UNIT_ASSERT(item->IsLinked());
        UNIT_ASSERT_EQUAL(item->GetHeap(), &fh);

        item->Unlink();
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT(!item->IsLinked());
    }

    Y_UNIT_TEST(FuzzingTest) {
        NUt::NFlatHeapUt::TTestItem a(10, 10), b(190, 190);
        UNIT_ASSERT(!a.IsLinked());
        UNIT_ASSERT(!b.IsLinked());

        TIntrusiveFlatHeap<NUt::NFlatHeapUt::TTestItem> fh;
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT_EQUAL(0, fh.Size());

        fh.Insert(a);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(1, fh.Size());
        UNIT_ASSERT(a.IsLinked());
        UNIT_ASSERT_EQUAL(&fh, a.GetHeap());

        fh.Insert(b);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(2, fh.Size());
        UNIT_ASSERT(a.IsLinked());
        UNIT_ASSERT(b.IsLinked());
        UNIT_ASSERT_EQUAL(&fh, a.GetHeap());
        UNIT_ASSERT_EQUAL(&fh, b.GetHeap());

        fh.Delete(b);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(1, fh.Size());
        UNIT_ASSERT(a.IsLinked());
        UNIT_ASSERT(!b.IsLinked());
        UNIT_ASSERT_EQUAL(&fh, a.GetHeap());
        UNIT_ASSERT_EQUAL(nullptr, b.GetHeap());
    }

    Y_UNIT_TEST(FuzzingTest2) {
        NUt::NFlatHeapUt::TTestItem a(72, 72), b(59, 59), c(45, 45);
        UNIT_ASSERT(!a.IsLinked());
        UNIT_ASSERT(!b.IsLinked());
        UNIT_ASSERT(!c.IsLinked());

        TIntrusiveFlatHeap<NUt::NFlatHeapUt::TTestItem> fh;
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT_EQUAL(0, fh.Size());

        fh.Insert(a);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(1, fh.Size());
        UNIT_ASSERT(a.IsLinked());
        UNIT_ASSERT_EQUAL(&fh, a.GetHeap());

        TVector<decltype(fh)::TItem*> items;
        while (!fh.Empty()) {
            items.push_back(fh.ExtractMin());
        }
        UNIT_ASSERT_EQUAL(1, items.size());
        UNIT_ASSERT(!a.IsLinked());
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT(std::is_sorted(items.begin(), items.end(), [](const auto* a, const auto* b) {
            return a->Get() < b->Get();
        }));

        for (auto* item : items) {
            fh.Insert(*item);
        }
        items.clear();

        fh.Insert(b);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(2, fh.Size());
        UNIT_ASSERT(a.IsLinked());
        UNIT_ASSERT(b.IsLinked());
        UNIT_ASSERT_EQUAL(&fh, a.GetHeap());
        UNIT_ASSERT_EQUAL(&fh, b.GetHeap());

        while (!fh.Empty()) {
            items.push_back(fh.ExtractMin());
        }
        UNIT_ASSERT_EQUAL(2, items.size());
        UNIT_ASSERT(!a.IsLinked());
        UNIT_ASSERT(!b.IsLinked());
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT(std::is_sorted(items.begin(), items.end(), [](const auto* a, const auto* b) {
            return a->Get() < b->Get();
        }));

        for (auto* item : items) {
            fh.Insert(*item);
        }
        items.clear();

        fh.Insert(c);
        UNIT_ASSERT(!fh.Empty());
        UNIT_ASSERT_EQUAL(3, fh.Size());
        UNIT_ASSERT(a.IsLinked());
        UNIT_ASSERT(b.IsLinked());
        UNIT_ASSERT(c.IsLinked());
        UNIT_ASSERT_EQUAL(&fh, a.GetHeap());
        UNIT_ASSERT_EQUAL(&fh, b.GetHeap());
        UNIT_ASSERT_EQUAL(&fh, c.GetHeap());

        while (!fh.Empty()) {
            items.push_back(fh.ExtractMin());
        }
        UNIT_ASSERT_EQUAL(3, items.size());
        UNIT_ASSERT(!a.IsLinked());
        UNIT_ASSERT(!b.IsLinked());
        UNIT_ASSERT(!c.IsLinked());
        UNIT_ASSERT(fh.Empty());
        UNIT_ASSERT(std::is_sorted(items.begin(), items.end(), [](const auto* a, const auto* b) {
            return a->Get() < b->Get();
        }));
    }
}
