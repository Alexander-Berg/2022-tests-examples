#include <yandex_io/libs/errno/errno_exception.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(libErrno) {
    Y_UNIT_TEST(strErrorResult) {
        UNIT_ASSERT(!quasar::strError(2).empty());
    }
}
