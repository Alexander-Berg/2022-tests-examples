#include <yandex/maps/wiki/common/rd/speed_cat.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::common::tests {

namespace {

constexpr bool PAVED = true;
constexpr bool NOT_PAVED = false;

void check(int fc, FOW fow, bool paved, int speedCat)
{
    UNIT_ASSERT_VALUES_EQUAL(
        predictSpeedCategory(fc, fow, paved),
        speedCat
    );
}

} // namespace

Y_UNIT_TEST_SUITE(speed_cat) {

Y_UNIT_TEST(test_predict_speed_category)
{
    check(6, FOW::Turnabout, PAVED, 81);
    check(1, FOW::None, PAVED, 31);
    check(3, FOW::None, NOT_PAVED, 63);
    check(7, FOW::None, NOT_PAVED, 74);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::common::tests
