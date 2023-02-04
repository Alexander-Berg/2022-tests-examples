#include <maps/wikimap/feedback/api/src/synctool/lib/get_request_template.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::sync::tests {

Y_UNIT_TEST_SUITE(test_get_request_template)
{

Y_UNIT_TEST(get_request_template)
{
    UNIT_ASSERT(!getRequestTemplate(RequestTemplateId::Other).has_value());
    UNIT_ASSERT(!getRequestTemplate(RequestTemplateId::Unknown).has_value());
    UNIT_ASSERT_VALUES_EQUAL(
        *getRequestTemplate(RequestTemplateId::NeedMoreInfo),
        "Пожалуйста, попросите пользователя подробнее "
            "описать, что именно не так на карте."
    );
}

} // Y_UNIT_TEST_SUITE(test_get_request_template)

} // namespace maps::wiki::feedback::api::tests
