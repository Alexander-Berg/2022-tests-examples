#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/gpstiles_realtime/libs/index/include/chained_iterator.h>

#include <vector>
#include <list>

namespace maps {
namespace gpstiles_realtime {
namespace index {
namespace test {

Y_UNIT_TEST_SUITE(chained_iterator_tests)
{

using Vec = std::vector<int>;
using IntIt = Vec::const_iterator;
using List = std::list<Vec>;
using ExtIt = List::const_iterator;

using It = ChainedIterator<IntIt, ExtIt>;

Y_UNIT_TEST(empty_test)
{
    List e;
    UNIT_ASSERT(It(e.begin(), e.end()) == It());
}

Y_UNIT_TEST(empty_range_test)
{
    List v;
    v.push_back({});

    It i(v.begin(), v.end());
    UNIT_ASSERT(i == It());
}

Y_UNIT_TEST(one_range_test)
{
    Vec v = {1, 2, 3, 4};
    List l = {v};
    It i(l.begin(), l.end());
    for (int c = 1; c <= 4; ++c) {
        UNIT_ASSERT(*i++ == c);
    }
    UNIT_ASSERT(i == It());
}

Y_UNIT_TEST(multiple_ranges_test)
{
    List l = {
        {},
        {1, 2, 3},
        {4},
        {},
        {},
        {5, 6, 7, 8},
        {},
        {9, 10},
        {}
    };
    It i(l.begin(), l.end());
    for (int c = 1; c <= 10; ++c) {
        UNIT_ASSERT(*i++ == c);
    }
    UNIT_ASSERT(i == It());
}

} // test suite end

} // namespace test
} // namespace index
} // namespace gpstiles_realtime
} // namespace maps
