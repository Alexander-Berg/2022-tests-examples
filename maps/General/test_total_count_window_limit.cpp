#include <library/cpp/testing/unittest/registar.h>

#include <maps/goods/lib/goods_db/price_filter_conversion.h>

using namespace maps::goods;

namespace {

struct TotalCountTestParameters {
    const size_t limit;
    const size_t maxPages;

    const size_t expectedWindowLimit;
};

} // anonymous namespace


Y_UNIT_TEST_SUITE(test_total_count_window_limit) {

Y_UNIT_TEST(test_calculated_correctly)
{
    std::vector<TotalCountTestParameters> params = {
        // test normal values
        {
            .limit = 10,
            .maxPages = 5,

            .expectedWindowLimit = 10 * 5 - 1,
        },
        // test zero max pages
        {
            .limit = 10,
            .maxPages = 0,

            .expectedWindowLimit = 0,
        },
        // test zero limit
        {
            .limit = 0,
            .maxPages = 5,

            .expectedWindowLimit = 0,
        },
        // test overflow prevented
        {
            .limit = 1ul << 40,
            .maxPages = 1ul << 40,

            .expectedWindowLimit = std::numeric_limits<int64_t>::max(), 
        },
    };

    for (const auto& p : params) {
        const size_t windowLimit = calcTotalCountWindowLimit(p.limit, p.maxPages);
        UNIT_ASSERT_VALUES_EQUAL(windowLimit, p.expectedWindowLimit);
    }
}

}
