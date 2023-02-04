#include <maps/infra/yacare/include/yacare.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/generic/ptr.h>
#include <util/thread/pool.h>

#include <algorithm>
#include <sstream>
#include <string>
#include <list>
#include <numeric>

Y_UNIT_TEST_SUITE(test_dynamic_metrics) {

Y_UNIT_TEST(count_from_thread_pool) {
    class Job : public IObjectInQueue {
    public:
        explicit Job(std::function<void()> action)
            : action_(std::move(action)) {}

        void Process(void*) override {
            action_();
        }

    private:
        std::function<void()> action_;

    };

    TThreadPool pool;

    pool.Start(4);

    std::list<double> values{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    auto sum = std::accumulate(values.begin(), values.end(), 0.);
    auto max = *std::max_element(values.begin(), values.end());

    for (const auto& value : values) {
        pool.SafeAddAndOwn(MakeHolder<Job>([&value](){
            YCR_LOG_DYNAMIC_METRIC("sum", YCR_SUM, value);
        }));
        pool.SafeAddAndOwn(MakeHolder<Job>([&value](){
            YCR_LOG_DYNAMIC_METRIC("max", YCR_MAX, value);
        }));
    }

    pool.Stop();

    std::ostringstream expectedFirstSum;
    expectedFirstSum << "[[\"sum_ammv\"," << sum << "],[\"max_axxv\"," << max << "]]";
    std::ostringstream expectedFirstMax;
    expectedFirstMax << "[[\"max_axxv\"," << max << "],[\"sum_ammv\"," << sum << "]]";
    std::ostringstream oss;
    oss << yacare::impl::dynamicServiceMetrics().extractAndReset();

    UNIT_ASSERT(
        oss.str() == expectedFirstSum.str() || oss.str() == expectedFirstMax.str()
    );
}

}
