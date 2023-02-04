#include <maps/factory/libs/db/dg_delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::db::tests {
using namespace table::alias;

Y_UNIT_TEST_SUITE(dg_gateways_should) {

const std::string TEST_ORDER_NO = "059490188170_01";
const std::string TEST_PART_ID = "P001";
const std::string TEST_PART_ID2 = "P002";
const delivery::DgProductId TEST_ID(TEST_ORDER_NO, TEST_PART_ID);
const delivery::DgProductId TEST_ID2(TEST_ORDER_NO, TEST_PART_ID2);

Y_UNIT_TEST(create_delivery)
{
    unittest::Fixture fixture;
    {
        pqxx::connection conn(fixture.postgres().connectionString());
        pqxx::work txn(conn);

        std::vector deliveries{
            DgDelivery{"059490188140_01", "059490188140_01_Vladivostok"},
            DgDelivery{"059490188170_01", "059490188170_01_Voronezh"},
        };
        DgDeliveryGateway{txn}.insert(deliveries);
        txn.commit();
    }

    {
        pqxx::connection conn(fixture.postgres().connectionString());
        pqxx::work txn(conn);
        auto delivery = DgDeliveryGateway{txn}.loadById("059490188140_01");
        EXPECT_EQ(delivery.orderNumber(), "059490188140_01");
        EXPECT_EQ(delivery.path(), "059490188140_01_Vladivostok");
    }
}

Y_UNIT_TEST(test_accepting_product)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    std::vector sources{
        MosaicSource{"059490188140_01_P001_Vladivostok"},
        MosaicSource{"059490188140_01_P002_Vladivostok"},
        MosaicSource{"059490188170_01_P001_Voronezh"},
    };

    {
        std::vector deliveries{
            DgDelivery{"059490188140_01", "059490188140_01_Vladivostok"},
            DgDelivery{"059490188170_01", "059490188170_01_Voronezh"},
        };
        pqxx::work txn(conn);
        MosaicSourceGateway{txn}.insert(sources);
        DgDeliveryGateway{txn}.insert(deliveries);
        txn.commit();
    }
    {
        pqxx::work txn(conn);
        DgProductGateway gtw{txn};
        gtw.acceptProduct({"059490188140_01", "P001"}, sources[0].id());
        gtw.acceptProduct({"059490188140_01", "P002"}, sources[1].id());
        gtw.acceptProduct({"059490188170_01", "P001"}, sources[2].id());
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto products = DgProductGateway{txn}
            .load(_DgProduct::orderNumber == "059490188140_01", orderBy(_DgProduct::partId));
        ASSERT_EQ(products.size(), 2u);
        EXPECT_EQ(products[0].orderNumber(), "059490188140_01");
        EXPECT_EQ(products[0].partId(), "P001");
        EXPECT_EQ(products[0].mosaicSourceId(), sources[0].id());
        EXPECT_EQ(products[1].orderNumber(), "059490188140_01");
        EXPECT_EQ(products[1].partId(), "P002");
        EXPECT_EQ(products[1].mosaicSourceId(), sources[1].id());
    }
}

} // suite

} // namespace maps::factory::db::tests

