#include <maps/factory/libs/db/delivery.h>
#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/delivery_vendor.h>
#include <maps/factory/libs/db/delivery_vendor_gateway.h>
#include <maps/factory/libs/db/source.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/db/vendor.h>
#include <maps/factory/libs/db/vendor_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::db::tests {
using namespace table::alias;

Y_UNIT_TEST_SUITE(test_delivery_gateway) {

Y_UNIT_TEST(test_creating_sat_source)
{
    Source testSource("test data", SourceType::Local, "/dev/null");

    testSource.setUser("test_user").setPassword("qwerty");

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        SourceGateway(txn).insert(testSource);
        txn.commit();

        EXPECT_EQ(testSource.id(), 1);
    }

    {
        pqxx::work txn(conn);
        auto loadedSource = SourceGateway(txn).loadById(testSource.id());

        EXPECT_EQ(loadedSource.name(), testSource.name());
        EXPECT_EQ(loadedSource.user(), testSource.user());
        EXPECT_EQ(loadedSource.password(), testSource.password());
    }

    Delivery testDelivery(testSource.id(), "2018-12-25", "From Santa Claus", "/via/chimney/to/fireplace");
    EXPECT_FALSE(testDelivery.enabled());
    testDelivery.enable();
    EXPECT_TRUE(testDelivery.enabled());
    {
        pqxx::work txn(conn);
        DeliveryGateway(txn).insert(testDelivery);
        txn.commit();

        EXPECT_EQ(testDelivery.id(), 1);
    }

    {
        pqxx::work txn(conn);
        auto enabledDeliveries = DeliveryGateway(txn).load(_Delivery::enabled);

        ASSERT_EQ(enabledDeliveries.size(), 1u);
        EXPECT_EQ(enabledDeliveries[0].name(), "From Santa Claus");
    }
}

Y_UNIT_TEST(test_delivery_vendors)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    const std::string VENDOR_NAME = "vendor";
    Vendor vendor(VENDOR_NAME);

    VendorGateway vendorGtw(txn);
    vendorGtw.insert(vendor);
    EXPECT_EQ(vendor.id(), 1);
    EXPECT_EQ(vendor.name(), VENDOR_NAME);
    EXPECT_EQ(vendorGtw.loadById(1).name(), VENDOR_NAME);

    Source testSource("test data", SourceType::Local, "/dev/null");
    SourceGateway(txn).insert(testSource);
    Delivery delivery(testSource.id(), "2018-12-25", "From Santa Claus", "/via/chimney/to/fireplace");
    DeliveryGateway(txn).insert(delivery);

    DeliveryVendor deliveryVendor(delivery.id(), vendor.id());
    DeliveryVendorGateway dvGtw(txn);
    dvGtw.insert(deliveryVendor);

    EXPECT_EQ(dvGtw.load().size(), 1u);
}

} // suite

} // namespace maps::factory::db::tests
