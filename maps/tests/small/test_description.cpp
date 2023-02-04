#include <maps/indoor/long-tasks/src/startrek-sync/lib/description.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::mirc::startrek::tests {

Y_UNIT_TEST_SUITE(test_description)
{

Y_UNIT_TEST(generation)
{
    const geolib3::Point2 ll{55.5, 37.5};
    const std::string address = "Москва";
    const std::string nmapsId = "123";
    const std::string reference =
        "Адрес: Москва\n"
        "Ссылка на НЯК: https://n.maps.yandex.ru/?ll=55.5%2C37.5&z=17\n"
        "Ссылка на БЯК: https://yandex.ru/maps/?ll=55.5%2C37.5&z=17\n"
        "Идентификатор в НК: ((https://n.maps.yandex.ru/#!/objects/123 123))";

    UNIT_ASSERT_VALUES_EQUAL(generateDescription(address, nmapsId, ll), reference);
}

} // test_description suite

} // namespace maps::mirc::startrek::tests
