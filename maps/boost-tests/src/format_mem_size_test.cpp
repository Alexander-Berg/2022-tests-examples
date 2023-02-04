#include <yandex/maps/renderer5/labeler/labeler_tools.h>
#include <boost/test/unit_test.hpp>

namespace maps {
namespace renderer5 {
namespace labeler {

BOOST_AUTO_TEST_CASE(format_mem_size)
{
    BOOST_CHECK_EQUAL(formatMemSize(0), "0 B");
    BOOST_CHECK_EQUAL(formatMemSize(200), "200 B");
    BOOST_CHECK_EQUAL(formatMemSize(1023), "1023 B");

    BOOST_CHECK_EQUAL(formatMemSize(1024), "1 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1200), "1.2 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 + 512), "1.5 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(2048), "2 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(2500), "2.4 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 9), "9 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 9 + 1), "9 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 9 + 200), "9.2 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 9 + 512), "9.5 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 9 + 972), "9.9 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 9 + 973), "10 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 10), "10 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 10 + 511), "10 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 10 + 512), "11 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 11), "11 KiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 - 1), "1024 KiB");

    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024), "1 MiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 + 1), "1 MiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 + 1024 * 200), "1.2 MiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 * 8), "8 MiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 * 9 + 1024 * 200), "9.2 MiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 * 11 + 1024 * 200), "11 MiB");
    BOOST_CHECK_EQUAL(formatMemSize(1024 * 1024 * 100 + 1024 * 200), "100 MiB");

    size_t Mi = 1024 * 1024;
    size_t Gi = 1024 * 1024 * 1024;
    BOOST_CHECK_EQUAL(formatMemSize(Gi * 8), "8 GiB");
    BOOST_CHECK_EQUAL(formatMemSize(Gi * 9 + Mi * 200), "9.2 GiB");
    BOOST_CHECK_EQUAL(formatMemSize(Gi * 11 + Mi * 200), "11 GiB");
    BOOST_CHECK_EQUAL(formatMemSize(Gi * 100 + Mi * 200), "100 GiB");
}

} // namespace labeler
} // namespace renderer5
} // namespace maps
