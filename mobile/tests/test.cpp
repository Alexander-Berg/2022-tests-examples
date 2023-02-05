#include <boost/test/auto_unit_test.hpp>

#include <yandex/maps/runtime/logging/logging.h>
#include <yandex/maps/navikit/report/report.h>

using namespace yandex::maps::navikit::report;

enum Bool {
    YES = 1,
    NO = 2
};

BOOST_AUTO_TEST_SUITE(NaviKitReporter)

BOOST_AUTO_TEST_CASE(Base)
{
    Bool b = NO;
    report("event occured", {
        {"value_int",   1},
        {"value_float", 1.f},
        {"value_bool",  true},
        {"Bool", choose(b, {
            {YES, "YES"},
            {NO, "NO"}
        })}
    });
}

BOOST_AUTO_TEST_CASE(NoParams)
{
    report("event occured");
}

BOOST_AUTO_TEST_SUITE_END()
