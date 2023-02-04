#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/infra/yacare/thread_pool.h>

struct ReferenceTesting {
    int number = 0;

    explicit ReferenceTesting(int n) : number(n) {}
    ReferenceTesting(const ReferenceTesting& other) = default;
    ReferenceTesting(ReferenceTesting&& other) noexcept : number(other.number) {
        other.number = 0;
    }
};

namespace yacare::tests {
using namespace testing;

Y_UNIT_TEST_SUITE(test_run_parallel) {
    Y_UNIT_TEST(test_run_parallel_references) {
        const int threadCount = 10;
        ReferenceTesting test{1};
        std::atomic<int> got{0};
        // Check that it's ok to pass rvalue args
        yacare::impl::runParallel(threadCount, [&](const ReferenceTesting& t) {
            got += t.number;
        }, std::move(test));
        EXPECT_EQ(threadCount, got);
    }

} //Y_UNIT_TEST_SUITE

} //namespace yacare::tests
