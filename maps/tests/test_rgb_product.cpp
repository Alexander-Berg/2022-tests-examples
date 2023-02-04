#include <maps/factory/libs/db/rgb_product_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

namespace maps::factory::db::tests {
using namespace table::alias;

Y_UNIT_TEST_SUITE(test_rgb_products_gateway) {

Y_UNIT_TEST(create_product)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    {
        pqxx::work txn(conn);

        MosaicSource ms{"test_ms"};
        MosaicSourceGateway{txn}.insert(ms);

        std::vector products{
            RgbProduct{"first"},
            RgbProduct{"second", ms.id(), RgbProductType::Custom},
        };
        RgbProductGateway{txn}.insert(products);
        txn.commit();
    }

    {
        pqxx::work txn(conn);

        MosaicSource ms{"other_ms"};
        MosaicSourceGateway{txn}.insert(ms);

        auto product = RgbProductGateway{txn}.loadOne(_RgbProduct::path == "first");
        EXPECT_FALSE(product.hasMosaicSourceId());
        product.setMosaicSourceId(ms.id());
        RgbProductGateway{txn}.update(product);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto products = RgbProductGateway{txn}.load(sql_chemistry::orderBy(_RgbProduct::path));

        ASSERT_EQ(products.size(), 2u);

        EXPECT_TRUE(products[0].hasMosaicSourceId());
        EXPECT_EQ(products[0].mosaicSourceId(), 2);
        EXPECT_EQ(products[0].path(), "first");
        EXPECT_EQ(products[0].type(), RgbProductType::Scanex);

        EXPECT_TRUE(products[1].hasMosaicSourceId());
        EXPECT_EQ(products[1].mosaicSourceId(), 1);
        EXPECT_EQ(products[1].path(), "second");
        EXPECT_EQ(products[1].type(), RgbProductType::Custom);
    }
}

} // suite

} // namespace maps::factory::db::tests
