#include <maps/infra/ecstatic/coordinator/lib/mongo/conditions.h>

#include <maps/libs/mongo/include/bson.h>

#include <library/cpp/testing/unittest/registar.h>

namespace bson {
using namespace maps::mongo::bson;
} // namespace bson

Y_UNIT_TEST_SUITE(test_mongo)
{
Y_UNIT_TEST(test_quote_const_char)
{
    UNIT_ASSERT_VALUES_EQUAL(mongo::impl::quote("postdl"), "postdl");
}

Y_UNIT_TEST(test_populate_conditions_pair)
{
    auto condition = mongo::conditions(bson::object("type", "postdl"));
    const auto& expected = R"({ "type" : "postdl" })";
    UNIT_ASSERT_VALUES_EQUAL(bsoncxx::to_json(condition.view()), expected);
}

Y_UNIT_TEST(test_mongo_conditions)
{
    Host host("h1");
    QualifiedDataset q("dataset1", "1.0");
    auto condition = mongo::conditions(host, q, bson::object("type", "postdl"));
    const auto& expected = R"({ "host" : "h1", "dataset" : "dataset1", "version" : )"
                           R"("1.0", "tag" : "", "type" : "postdl" })";
    UNIT_ASSERT_VALUES_EQUAL(bsoncxx::to_json(condition.view()), expected);
}
}
