#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/roquefort/lib/operation.h>
#include <maps/libs/common/include/exception.h>

using namespace maps::roquefort;

constexpr auto TEST_LINE{"tskv\trequest=/ping\tstatus=200"};

Y_UNIT_TEST_SUITE(FieldsTestSuite)
{
    Y_UNIT_TEST(FieldsConstructionTest)
    {
        Fields fields;
        UNIT_ASSERT_VALUES_EQUAL(fields.count("request"), 0);
    }

    Y_UNIT_TEST(FieldsParseTskvTest)
    {
        auto fields = Fields::parseTskv(TEST_LINE);
        UNIT_ASSERT_VALUES_EQUAL(fields.count("request"), 1);
    }

    Y_UNIT_TEST(FieldsLookupTest)
    {
        auto fields = Fields::parseTskv(TEST_LINE);
        UNIT_ASSERT_VALUES_EQUAL(fields.at("request"), "/ping");
    }

    Y_UNIT_TEST(FieldsLookupRaisesTest)
    {
        auto fields = Fields::parseTskv(TEST_LINE);
        UNIT_ASSERT_EXCEPTION_CONTAINS(fields.at("unexpected"), std::out_of_range, "Key unexpected not found");
    }

    Y_UNIT_TEST(FieldsLookupAndCacheIntegerValueTest)
    {
        auto fields = Fields::parseTskv(TEST_LINE);
        UNIT_ASSERT_VALUES_EQUAL(fields.getCachedValue("status"), 200);
    }

    Y_UNIT_TEST(FieldsLookupIntegerValueRaisesTest)
    {
        auto fields = Fields::parseTskv(TEST_LINE);
        UNIT_ASSERT_EXCEPTION_CONTAINS(fields.getCachedValues("bytes_send"), maps::RuntimeError, "Key bytes_send not found");
    }
}
