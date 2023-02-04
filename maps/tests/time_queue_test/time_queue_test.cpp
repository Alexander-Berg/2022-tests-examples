#define BOOST_TEST_MAIN

#include <maps/analyzer/libs/common/include/time_queue.h>
#include <maps/analyzer/libs/common/include/time_queue_stat.h>

#include <boost/test/unit_test.hpp>
#include <boost/optional/optional_io.hpp>

#include <vector>
#include <random>
#include <algorithm>

using namespace maps::analyzer;
using namespace boost::posix_time;

// shortcut for `from_iso_string`
inline ptime iso(const std::string& s)
{
    return from_iso_string(s);
}

BOOST_AUTO_TEST_CASE(time_queue_exception_test)
{
    TimeQueue<int> q(seconds(60));

    q.push(10, iso("20171001T000000"));
    q.push(20, iso("20171001T000010"));
    q.push(30, iso("20171001T000020"));
    BOOST_CHECK_THROW(
        q.push(40, iso("20171001T000000")),
        maps::Exception
    );
}

BOOST_AUTO_TEST_CASE(time_queue_test)
{
    TimeQueue<int> q(seconds(60));

    BOOST_CHECK(q.empty());

    q.push(10, iso("20171001T000000"));
    q.push(20, iso("20171001T000010"));
    q.push(30, iso("20171001T000020"));
    q.push(40, iso("20171001T000030"));
    q.push(50, iso("20171001T000040"));
    q.push(60, iso("20171001T000050"));
    q.push(70, iso("20171001T000100"));

    BOOST_CHECK(!q.empty());
    BOOST_CHECK_EQUAL(q.front().first, 10);

    q.push(80, iso("20171001T000110"));
    q.push(90, iso("20171001T000120"));

    BOOST_CHECK(!q.empty());
    BOOST_CHECK_EQUAL(q.front().first, 30);

    q.push(100, iso("20171001T000500"));

    BOOST_CHECK(!q.empty());
    BOOST_CHECK_EQUAL(q.front().first, 100);
}

BOOST_AUTO_TEST_CASE(time_queue_stat_test)
{
    TimeQueueStat<int, stats::Many<stats::Min, stats::Max, stats::Sum>::Stat> q(seconds(60));

    BOOST_CHECK(q.empty());
    BOOST_CHECK_EQUAL(std::get<0>(q.getStat()), boost::none);
    BOOST_CHECK_EQUAL(std::get<1>(q.getStat()), boost::none);
    BOOST_CHECK_EQUAL(std::get<2>(q.getStat()), 0);

    q.push(-10, iso("20171001T000000"));
    q.push(20, iso("20171001T000010"));
    q.push(30, iso("20171001T000020"));
    q.push(40, iso("20171001T000030"));
    q.push(0, iso("20171001T000040"));
    q.push(60, iso("20171001T000050"));
    q.push(10, iso("20171001T000100"));

    BOOST_CHECK(!q.empty());
    BOOST_CHECK_EQUAL(q.front().first, -10);
    BOOST_CHECK_EQUAL(std::get<0>(q.getStat()), boost::make_optional(-10));
    BOOST_CHECK_EQUAL(std::get<1>(q.getStat()), boost::make_optional(60));
    BOOST_CHECK_EQUAL(std::get<2>(q.getStat()), 150);

    q.push(80, iso("20171001T000110"));
    q.push(90, iso("20171001T000120"));

    BOOST_CHECK(!q.empty());
    BOOST_CHECK_EQUAL(q.front().first, 30);
    BOOST_CHECK_EQUAL(std::get<0>(q.getStat()), boost::make_optional(0));
    BOOST_CHECK_EQUAL(std::get<1>(q.getStat()), boost::make_optional(90));
    BOOST_CHECK_EQUAL(std::get<2>(q.getStat()), 310);

    q.push(-100, iso("20171001T000500"));

    BOOST_CHECK(!q.empty());
    BOOST_CHECK_EQUAL(q.front().first, -100);
    BOOST_CHECK_EQUAL(std::get<0>(q.getStat()), boost::make_optional(-100));
    BOOST_CHECK_EQUAL(std::get<1>(q.getStat()), boost::make_optional(-100));
    BOOST_CHECK_EQUAL(std::get<2>(q.getStat()), -100);
}

BOOST_AUTO_TEST_CASE(time_queue_stat_random_test)
{
    std::default_random_engine generator;
    std::uniform_int_distribution<int> distribution(-100, 100);
    std::uniform_int_distribution<int> step(1, 10);

    std::vector<int> values;
    for (std::size_t i = 0; i < 1000; ++i) {
        values.push_back(distribution(generator));
    }

    TimeQueueStat<int, stats::Many<stats::Min, stats::Max, stats::Sum>::Stat> q(seconds(6));
    ptime tm = iso("20171005T000000");
    time_duration step_duration = seconds(1);

    std::size_t index = 0;
    std::size_t next_index = 0;

    while (next_index < values.size()) {
        next_index = std::min(values.size(), index + step(generator));
        auto lower_index = next_index > 7 ? next_index - 7 : 0;

        for (std::size_t i = index; i < next_index; ++i) {
            q.push(values[i], tm);
            tm += step_duration;
        }

        auto minmax_it = std::minmax_element(values.begin() + lower_index, values.begin() + next_index);

        BOOST_CHECK(!q.empty());
        BOOST_CHECK_EQUAL(q.front().first, values[lower_index]);
        BOOST_CHECK_EQUAL(
            std::get<0>(q.getStat()),
            boost::make_optional(*(minmax_it.first))
        );
        BOOST_CHECK_EQUAL(
            std::get<1>(q.getStat()),
            boost::make_optional(*(minmax_it.second))
        );
        BOOST_CHECK_EQUAL(
            std::get<2>(q.getStat()),
            std::accumulate(values.begin() + lower_index, values.begin() + next_index, 0)
        );

        index = next_index;
    }
}
