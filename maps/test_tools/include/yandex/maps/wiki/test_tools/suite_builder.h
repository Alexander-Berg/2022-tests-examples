#pragma once

#include <yandex/maps/wiki/test_tools/suite.h>

#include <boost/test/test_tools.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/test/parameterized_test.hpp>

namespace maps {
namespace wiki {
namespace test_tools {

#ifdef YANDEX_MAPS_BUILD
#define MAKE_TEST_CASE( test_function, name ) \
    boost::unit_test::make_test_case(boost::unit_test::callback0<>(test_function), \
        name)
#else
#define MAKE_TEST_CASE( test_function, name ) \
    boost::unit_test::make_test_case(boost::function<void ()>(test_function), \
        name, \
        __FILE__, __LINE__ )
#endif

template <class TestDataT, class TestRunnerT>
class BoostTestSuiteBuilder : public Visitor<TestDataT>
{
public:
    explicit BoostTestSuiteBuilder(boost::unit_test::test_suite& parentSuite)
        : parentSuite_(parentSuite)
    {}

    virtual void visit(const TestSuite<TestDataT>& suite)
    {
        boost::unit_test::test_suite* ts = BOOST_TEST_SUITE(suite.name());

        for (const auto& test : suite) {
            auto tm = std::bind(TestRunnerT::run, test);
            ts->add(MAKE_TEST_CASE(tm, suite.name() + ":" + test.name()));
        }

        parentSuite_.add(ts);
    }

    virtual void visit(const std::string& suiteName, const TestDataT& test)
    {
        boost::unit_test::test_suite* ts = BOOST_TEST_SUITE(suiteName);

        auto tm = std::bind(TestRunnerT::run, test);
        ts->add(MAKE_TEST_CASE(tm, suiteName + ":" + test.name()));

        parentSuite_.add(ts);
    }

private:
    boost::unit_test::test_suite& parentSuite_;
};

} // namespace test_tools
} // namespace wiki
} // namespace maps
