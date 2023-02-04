#include <boost/test/unit_test.hpp>
#include <maps/mobile/libs/runtime/common/tests/android_typeinfo_tests/testlib/testlib.h>

BOOST_AUTO_TEST_SUITE(AndroidTypeinfoTestSuite)

BOOST_AUTO_TEST_CASE(catchCrossLibrariesExceptionTest) {
    try {
        throwTestException();
    } catch(const TestException&) {
        BOOST_TEST(true);
    }
    catch(...) {
        BOOST_TEST(false);
    }
}

BOOST_AUTO_TEST_CASE(compareTypeinfoTest) {
    BOOST_CHECK(typeid(TestException) == testExceptionTypeInfo());
}

BOOST_AUTO_TEST_SUITE_END()
