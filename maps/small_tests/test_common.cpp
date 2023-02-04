#include <maps/wikimap/feedback/api/src/libs/common/feedback_task.h>
#include <maps/wikimap/feedback/api/src/libs/common/tests/helpers/printers.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_common)
{

Y_UNIT_TEST(parse_integration)
{
    Integration expected{{
        {
            Service::Nmaps,
            ServiceDesc{
                ServiceObjectId("1654073191"),
                "https://n.maps.yandex.ru/#!/objects/1654073191",
                ServiceDesc::NO_RESOLUTION
            }
        }
    }};
    const auto json = maps::json::Value::fromFile(SRC_("data/integration1.json"));
    UNIT_ASSERT_VALUES_EQUAL(Integration(json), expected);
}

Y_UNIT_TEST(parse_multiservice_integration)
{
    Integration expected{{
        {
            Service::Nmaps,
            ServiceDesc{
                std::nullopt,
                std::nullopt,
                ServiceDesc::NO_RESOLUTION
            }
        },
        {
            Service::Support,
            ServiceDesc{
                ServiceObjectId("107490833"),
                "https://api.samsara.yandex-team.ru/api/v2/tickets/107490833",
                "some resolution"
            },
        }
    }};
    const auto json = maps::json::Value::fromFile(SRC_("data/integration2.json"));
    UNIT_ASSERT_VALUES_EQUAL(Integration(json), expected);
}

Y_UNIT_TEST(parse_integration_empty_id)
{
    Integration expected{{
        {
            Service::Nmaps,
            ServiceDesc{
                std::nullopt,
                "https://n.maps.yandex.ru/#!/objects/null",
                "no-data"
            }
        },
        {
            Service::Support,
            ServiceDesc{
                ServiceObjectId("107415684"),
                "https://api.samsara.yandex-team.ru/api/v2/tickets/107415684",
                ServiceDesc::NO_RESOLUTION
            }
        }
    }};
    const auto json = maps::json::Value::fromFile(SRC_("data/integration3.json"));
    UNIT_ASSERT_VALUES_EQUAL(Integration(json), expected);
}

} // test_common suite

} // namespace maps::wiki::feedback::api::tests
