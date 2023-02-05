#include <maps/wikimap/mapspro/libs/acl/impl/factory.h>
#include <maps/wikimap/mapspro/libs/acl/include/schedule.h>
#include <maps/libs/chrono/include/time_point.h>

#include <boost/test/unit_test.hpp>

namespace maps::wiki {

using namespace acl;
using namespace std::chrono_literals;

BOOST_AUTO_TEST_SUITE(acl_test_schedules)

BOOST_AUTO_TEST_CASE(acl_schedule_active)
{
    const auto now = std::chrono::system_clock::now();
    const auto startDateActive = chrono::formatIntegralDateTime(now, "%d.%m.%Y");
    const auto endDateActive = chrono::formatIntegralDateTime(
        now + std::chrono::days(14), "%d.%m.%Y");
    const auto startDateInactive = chrono::formatIntegralDateTime(
        now - std::chrono::days(5), "%d.%m.%Y");
    const auto endDateInactive = chrono::formatIntegralDateTime(
        now - std::chrono::days(1), "%d.%m.%Y");
    const auto weekdays = std::stoi(chrono::formatIntegralDateTime(now, "%u"));

    auto startDateTp = chrono::parseIntegralDateTime(
        startDateActive + " 00:00", "%d.%m.%Y %H:%M");

    const auto s1 = Factory::schedule(1,
        startDateActive, endDateActive,
        std::nullopt, std::nullopt,
        std::nullopt, "[[4,2],[6,2]]");

    BOOST_CHECK(s1.isActive(startDateTp));
    BOOST_CHECK(s1.isActive(startDateTp + std::chrono::days(3)));
    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(4)));
    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(5)));
    BOOST_CHECK(s1.isActive(startDateTp + std::chrono::days(6)));
    BOOST_CHECK(s1.isActive(startDateTp + std::chrono::days(11)));
    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(12)));
    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(13)));
    BOOST_CHECK(s1.isActive(startDateTp + std::chrono::days(14)));

    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(-5)));
    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(-1)));
    BOOST_CHECK(!s1.isActive(startDateTp + std::chrono::days(15)));

    const auto s2 = Factory::schedule(2,
        startDateActive, std::nullopt,
        "08:00", "17:00",
        std::nullopt, "[[5,2]]");

    BOOST_CHECK(!s2.isActive(startDateTp));
    BOOST_CHECK(!s2.isActive(startDateTp + 7h + 59min));
    BOOST_CHECK(s2.isActive(startDateTp + 8h));
    BOOST_CHECK(s2.isActive(startDateTp + 16h + 59min));
    BOOST_CHECK(!s2.isActive(startDateTp + 17h + 1min));
    BOOST_CHECK(s2.isActive(startDateTp + std::chrono::days(4) + 16h + 59min));
    BOOST_CHECK(!s2.isActive(startDateTp + std::chrono::days(4) + 17h + 1min));
    BOOST_CHECK(!s2.isActive(startDateTp + std::chrono::days(5) + 12h));
    BOOST_CHECK(!s2.isActive(startDateTp + std::chrono::days(6) + 12h));
    BOOST_CHECK(s2.isActive(startDateTp + std::chrono::days(7) + 8h));

    const auto s3 = Factory::schedule(3,
        startDateActive, endDateActive,
        std::nullopt, std::nullopt,
        1 << (weekdays - 1), std::nullopt);

    BOOST_CHECK(s3.isActive(startDateTp));
    BOOST_CHECK(!s3.isActive(startDateTp + std::chrono::days(1)));
    BOOST_CHECK(!s3.isActive(startDateTp + std::chrono::days(6)));
    BOOST_CHECK(s3.isActive(startDateTp + std::chrono::days(7)));

    const auto s4 = Factory::schedule(4,
        startDateInactive, endDateInactive,
        std::nullopt, std::nullopt,
        std::nullopt, "[[1,1]]");
    const auto s5 = Factory::schedule(6,
        startDateInactive, endDateInactive,
        std::nullopt, std::nullopt,
        1 << (weekdays - 1), std::nullopt);

    BOOST_CHECK(!s4.isActive(startDateTp));
    BOOST_CHECK(!s5.isActive(startDateTp));
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace maps::wiki
