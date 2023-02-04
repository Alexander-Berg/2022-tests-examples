#pragma once

#include "../test_tools/test_suite.h"

#include <yandex/maps/wiki/topo/exception.h>

#include <boost/test/test_tools.hpp>
#include <boost/test/unit_test.hpp>
#include <boost/test/parameterized_test.hpp>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

#define CHECK_TOPO_ERROR( expression, expected_error ) \
    try { \
        expression; \
        BOOST_FAIL("topo::GeomProcessing exception is expected with error " \
            << errorCodeToString(expected_error)); \
    } catch (const topo::GeomProcessingError& ex) { \
        BOOST_REQUIRE_MESSAGE( \
            ex.errorCode() == expected_error, \
            "Received error mismatch, expected " << errorCodeToString(expected_error) \
            << ", got " << errorCodeToString(ex.errorCode())); \
    } \


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
            ts->add(
                MAKE_TEST_CASE(
                    std::bind(TestRunnerT::run, test),
                    suite.name() + ":" + test.name()));
        }
        parentSuite_.add(ts);
    }

    virtual void visit(const std::string& suiteName, const TestDataT& test)
    {
        boost::unit_test::test_suite* ts = BOOST_TEST_SUITE(suiteName);
        ts->add(
                MAKE_TEST_CASE(
                    std::bind(TestRunnerT::run, test),
                    suiteName + ":" + test.name()));
        parentSuite_.add(ts);
    }

private:
    boost::unit_test::test_suite& parentSuite_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
