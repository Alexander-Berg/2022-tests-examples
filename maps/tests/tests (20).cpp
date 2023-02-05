#include <yandex/maps/wiki/filters/parsed_expression.h>
#include <yandex/maps/wiki/filters/stored_expression.h>
#include <yandex/maps/wiki/filters/stored_filter.h>
#include <yandex/maps/wiki/filters/exception.h>

#include <yandex/maps/wiki/common/pgpool3_helpers.h>
#include <yandex/maps/wiki/unittest/localdb.h>

#include <maps/libs/log8/include/log8.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace filters {
namespace tests {

namespace {

const TUId TEST_UID = 111;
const TUId OTHER_UID = 222;

struct SetLogLevelFixture
{
    SetLogLevelFixture() { maps::log8::setLevel(maps::log8::Level::ERROR); }
};

class DbFixture : public SetLogLevelFixture, public unittest::MapsproDbFixture
{
};

} // namespace

BOOST_FIXTURE_TEST_CASE( test_parse, DbFixture )
{
    BOOST_CHECK_THROW(ParsedExpression::parse("a = "), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("a = 1 1"), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse(""), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("a in ('1', 2)"), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("id = '8'"), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("a < '8'"), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("id = ''"), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("id <> '3'"), ParseError);
    BOOST_CHECK_THROW(ParsedExpression::parse("a in ('1', '')"), ParseError);

    std::map<std::string, std::string> exprToSql =
    {
          { "s::a % 'b10'", "(service_attrs ? 'a' AND service_attrs->'a' LIKE '%b10%')" }
        , { "a % 'b10'", "(domain_attrs ? 'a' AND domain_attrs->'a' LIKE '%b10%')" }
        , { "a =1", "(domain_attrs ? 'a' AND domain_attrs->'a' = '1')" }
        , { "rd_el:oneway= 'B'",
            "(domain_attrs ? 'rd_el:oneway' AND domain_attrs->'rd_el:oneway' = 'B')" }
        , { "rd_el:oneway in ('B', 'F')",
            "(domain_attrs ? 'rd_el:oneway' AND domain_attrs->'rd_el:oneway' IN ('B','F'))" }
        , { "b = 1 or b = 2 and a = 3",
            "((domain_attrs ? 'b' AND domain_attrs->'b' = '1') OR "
                "((domain_attrs ? 'b' AND domain_attrs->'b' = '2') "
                    "AND (domain_attrs ? 'a' AND domain_attrs->'a' = '3')))" }
        , { "(b = 1 or b = 2) and a = 3",
            "(((domain_attrs ? 'b' AND domain_attrs->'b' = '1') "
                    "OR (domain_attrs ? 'b' AND domain_attrs->'b' = '2')) "
                "AND (domain_attrs ? 'a' AND domain_attrs->'a' = '3'))" }
        , { "(a = 3 or b = 1) and (id in (5, 6))",
            "(((domain_attrs ? 'a' AND domain_attrs->'a' = '3') "
                    "OR (domain_attrs ? 'b' AND domain_attrs->'b' = '1')) "
                "AND (id IN (5,6)))" }
        , { "a = 1 or not (b = 1 and not b = 2)",
            "((domain_attrs ? 'a' AND domain_attrs->'a' = '1') "
                "OR (NOT ((domain_attrs ? 'b' AND domain_attrs->'b' = '1') "
                    "AND (NOT (domain_attrs ? 'b' AND domain_attrs->'b' = '2')))))" }
        , { "a > 1 and a>=  2 and a <=3 and a < 4",
            "((domain_attrs ? 'a' AND (domain_attrs->'a')::bigint > 1) "
            "AND (domain_attrs ? 'a' AND (domain_attrs->'a')::bigint >= 2) "
            "AND (domain_attrs ? 'a' AND (domain_attrs->'a')::bigint <= 3) "
            "AND (domain_attrs ? 'a' AND (domain_attrs->'a')::bigint < 4))"}
        , { "a <> 'L45,S:auto;S,R135:auto'",
            "(NOT (domain_attrs ? 'a' AND domain_attrs->'a' = 'L45,S:auto;S,R135:auto'))" }
        , { "a = ''",
            "(NOT domain_attrs ? 'a')" }
        , { "a <> ''",
            "(NOT (NOT domain_attrs ? 'a'))" }
        , { "outsource_region:task_type='rd_el_attrs'",
            "(domain_attrs ? 'outsource_region:task_type' "
            "AND domain_attrs->'outsource_region:task_type' = 'rd_el_attrs')" }
        , { "id=123",
            "(id = 123)" }
    };

    auto txn = pool().masterReadOnlyTransaction();

    for (const auto& pair : exprToSql) {
        const auto& expr = pair.first;
        const auto& sql = pair.second;
        try {
            BOOST_CHECK_EQUAL(
                    ParsedExpression::parse(expr).viewFilterClause(*txn), sql);
        } catch (const ParseError& ex) {
            BOOST_ERROR(
                    "parse error while parsing expr: `" << expr << "', trace: " << ex);
        }
    }
}

BOOST_FIXTURE_TEST_CASE( test_stored_expression, DbFixture )
{
    TExpressionId id;
    {
        auto txn = pool().masterWriteableTransaction();
        auto expr = StoredExpression::create(*txn, TEST_UID, "a = 1");
        id = expr.id();
        BOOST_CHECK(!expr.createdAt().empty());
        txn->commit();
    }

    auto txn = pool().masterReadOnlyTransaction();
    auto expr = StoredExpression::load(*txn, id);
    BOOST_CHECK_EQUAL(expr.createdBy(), TEST_UID);
    BOOST_CHECK_EQUAL(expr.text(), "a = 1");

    {
        auto txn = pool().masterWriteableTransaction();
        BOOST_CHECK_THROW(StoredExpression::create(*txn, TEST_UID, "a = "),
                          ParseError);
    }
}

BOOST_FIXTURE_TEST_CASE( test_stored_filter, DbFixture )
{
    TFilterId id;
    {
        auto txn = pool().masterWriteableTransaction();
        auto filter = StoredFilter::create(
                *txn, TEST_UID, "my filter", true, "a = 1");
        id = filter.id();
        BOOST_CHECK(!filter.createdAt().empty());
        txn->commit();
    }

    auto txn = pool().masterReadOnlyTransaction();
    auto filter = StoredFilter::load(*txn, id);
    BOOST_CHECK_EQUAL(filter.createdBy(), TEST_UID);
    BOOST_CHECK_EQUAL(filter.name(), "my filter");
    BOOST_CHECK_EQUAL(filter.expression().text(), "a = 1");

    {
        auto txn = pool().masterWriteableTransaction();
        filter.setName(*txn, "new name");
        filter.setExpression(*txn, TEST_UID, "b = 1");
        txn->commit();
    }

    auto loadedFilter = StoredFilter::load(*txn, id);
    BOOST_CHECK_EQUAL(filter.name(), "new name");
    BOOST_CHECK_EQUAL(loadedFilter.name(), "new name");
    BOOST_CHECK_EQUAL(filter.expression().text(), "b = 1");
    BOOST_CHECK_EQUAL(loadedFilter.expression().text(), "b = 1");

    auto loadedExpr = StoredExpression::load(*txn, filter.expression().id());
    BOOST_CHECK_EQUAL(loadedExpr.text(), "b = 1");
    BOOST_CHECK_EQUAL(loadedExpr.createdBy(), TEST_UID);
}

BOOST_FIXTURE_TEST_CASE( test_stored_filter_load_paged, DbFixture )
{
    {
        auto txn = pool().masterWriteableTransaction();
        StoredFilter::create(*txn, TEST_UID, "filter1", true, "a = 1");
        StoredFilter::create(*txn, TEST_UID, "filter2", false, "a = 2");
        StoredFilter::create(*txn, OTHER_UID, "filter3", true, "a = 3");
        txn->commit();
    }

    auto txn = pool().masterReadOnlyTransaction();

    auto result1 = StoredFilter::loadPaged(*txn, TEST_UID, 1, 2, boost::none);
    BOOST_CHECK_EQUAL(result1.pager().totalCount(), 3);
    BOOST_CHECK_EQUAL(result1.value().size(), 2);

    auto result2 = StoredFilter::loadPaged(*txn, OTHER_UID, 1, 10, boost::none);
    BOOST_CHECK_EQUAL(result2.pager().totalCount(), 2);

    auto result3 = StoredFilter::loadPaged(*txn, OTHER_UID, 1, 10, TEST_UID);
    BOOST_CHECK_EQUAL(result3.pager().totalCount(), 1);
}

} // namespace tests
} // namespace filters
} // namespace wiki
} // namespace maps
