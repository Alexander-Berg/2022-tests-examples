#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/roquefort/lib/utils.h>
#include <maps/libs/common/include/exception.h>

#include <sstream>

using namespace maps::roquefort;

Y_UNIT_TEST_SUITE(UtilsTestSuite)
{
    Y_UNIT_TEST(TskvFieldNameTest)
    {
        UNIT_ASSERT_VALUES_EQUAL("request", tskvFieldName("request_url"));
        UNIT_ASSERT_VALUES_EQUAL("request", tskvFieldName("request"));
    }

    Y_UNIT_TEST(GetCachedValuesTest)
    {
        {
            Fields fields({{"a", "-, 0.015"}});
            UNIT_ASSERT_VALUES_EQUAL(fields.getCachedValues("a"), (std::vector<double>{0.015}));
        }
        {
            Fields fields({{"b", "-, 0.015, 0.01"}});
            UNIT_ASSERT_VALUES_EQUAL(fields.getCachedValues("b"), (std::vector<double>{0.015, 0.01}));
        }
        {
            UNIT_ASSERT_EXCEPTION_CONTAINS(Fields{}.getCachedValues("c"), maps::RuntimeError, "Key c not found");
        }
        {
            Fields fields({{"c", "-, -"}});
            UNIT_ASSERT_VALUES_EQUAL(fields.getCachedValues("c"), (std::vector<double>{}));
        }
        {
            Fields fields({{"d", "-"}});
            UNIT_ASSERT_VALUES_EQUAL(fields.getCachedValues("d"), (std::vector<double>{}));
        }
    }
}
