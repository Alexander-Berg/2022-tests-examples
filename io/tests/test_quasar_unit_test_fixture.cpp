#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

class SomeFixture: public QuasarUnitTestFixture {
public:
    SomeFixture() = default;
};

/* Make sure that fixture works */
Y_UNIT_TEST_SUITE_F(QuasarInitTestFixtureCheck, SomeFixture) {
    Y_UNIT_TEST(test1) {
        UNIT_ASSERT_VALUES_EQUAL(1, 1);
    }

    Y_UNIT_TEST(test2) {
        UNIT_ASSERT_VALUES_EQUAL(2, 2);
    }

    Y_UNIT_TEST(test3) {
        UNIT_ASSERT_VALUES_EQUAL(3, 3);
    }
}
