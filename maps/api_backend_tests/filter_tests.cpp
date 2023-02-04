#include "fixture.h"
#include <maps/factory/services/sputnica_back/lib/filter.h>
#include <maps/factory/services/sputnica_back/lib/exception.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

#include <string_view>
#include <vector>

namespace maps::factory::sputnica::tests {

Y_UNIT_TEST_SUITE_F(test_filter, Fixture) {

Y_UNIT_TEST(test_parsing_conjunctions)
{
    pqxx::connection connection(postgres().connectionString());
    pqxx::work txn(connection);

    {
        auto conj = Conjunction::fromString("mosaic-source:1");
        EXPECT_EQ(
            conj.makeMosaicSourcesFilter().serializeToSql(txn),
            " sat.mosaic_sources.id IN ( 1 ) "
        );
    }
    {
        auto conj = Conjunction::fromString("status:ready,parsed");
        EXPECT_EQ(
            conj.makeMosaicSourcesFilter().serializeToSql(txn),
            " sat.mosaic_sources.status IN ( 'ready' , 'parsed' ) "
        );
    }
    {
        auto conj = Conjunction::fromString("order:100,500*aoi:31,337");
        EXPECT_EQ(
            conj.makeMosaicSourcesFilter().serializeToSql(txn),
            " ( sat.mosaic_sources.order_id IN ( 100 , 500 ) ) AND"
            " ( sat.mosaic_sources.aoi_id IN ( 31 , 337 ) ) "
        );
    }
}

Y_UNIT_TEST(test_parsing_filters)
{
    pqxx::connection connection(postgres().connectionString());
    pqxx::work txn(connection);

    {
        auto filter = Filter::fromString("mosaic-source:1");
        EXPECT_EQ(
            filter.makeMosaicSourcesFilter().serializeToSql(txn),
            " sat.mosaic_sources.id IN ( 1 ) "
        );
    }
    {
        auto filter = Filter::fromString("order:100,500|aoi:31,337");
        EXPECT_EQ(
            filter.makeMosaicSourcesFilter().serializeToSql(txn),
            " ( sat.mosaic_sources.order_id IN ( 100 , 500 ) ) OR"
            " ( sat.mosaic_sources.aoi_id IN ( 31 , 337 ) ) "
        );
    }
}

Y_UNIT_TEST(test_parsing_invalid_filters)
{
    const std::vector<std::string_view> invalidFilters{
        "name-but-no-values",
        "name-but-no-values:",
        ":no-name",
        "unknown-name:1,2,3",
        "mosaic-source:1*",
        "*",
        "|",
        "***",
        "|||",
        "status:unknown-status",
        "mosaic-source:1,,2,3"
    };

    for (auto invalidFilter: invalidFilters) {
        EXPECT_THROW(Filter::fromString(invalidFilter), BadFilter);
    }
}

} //Y_UNIT_TEST_SUITE
} //namespace maps::factory::sputnica::tests
