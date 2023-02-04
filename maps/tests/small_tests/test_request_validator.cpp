#include <maps/wikimap/ugc/backoffice/src/lib/request_validator.h>
#include <maps/infra/yacare/include/error.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::ugc::backoffice::tests {

Y_UNIT_TEST_SUITE(test_request_validator)
{

Y_UNIT_TEST(request_validator)
{
    RequestValidator validator{maps::json::Value::fromString(R"({
        "prefixes": {
            "2020771": ["fb"]
        },
        "metadata": {
            "2020771": [3, 4, 5]
        }
    })")};

    UNIT_ASSERT_NO_EXCEPTION(validator.checkId(2020771, "fb:id1"));
    UNIT_ASSERT_EXCEPTION(
        validator.checkId(2020772, "fb:id1"),
        yacare::errors::Forbidden
    );
    UNIT_ASSERT_EXCEPTION(
        validator.checkId(2020771, "ff:id1"),
        yacare::errors::Forbidden
    );
    UNIT_ASSERT_EXCEPTION(
        validator.checkId(2020771, "fffid1"),
        yacare::errors::BadRequest
    );

    UNIT_ASSERT_NO_EXCEPTION(validator.checkMetadataId(2020771, MetadataId{3}));
    UNIT_ASSERT_EXCEPTION(
        validator.checkMetadataId(2020772, MetadataId{3}),
        yacare::errors::Forbidden
    );
    UNIT_ASSERT_EXCEPTION(
        validator.checkMetadataId(2020771, MetadataId{2}),
        yacare::errors::Forbidden
    );
}

} // test_request_validator suite

} // namespace maps::wiki::ugc::backoffice::tests
