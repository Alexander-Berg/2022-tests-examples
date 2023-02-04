#include <maps/factory/libs/db/secret_object_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

#include <string>

namespace maps::factory::db::tests {

Y_UNIT_TEST_SUITE(test_secret_object) {

Y_UNIT_TEST(basic_test)
{

    const std::string TEST_MOSAIC_NAME = "just_test_mosaic";
    const geolib3::MultiPolygon2 TEST_GEOMETRY{{
        geolib3::Polygon2{geolib3::PointsVector{
            {0.0, 0.0},
            {2.0, 0.0},
            {2.0, 2.0},
            {0.0, 2.0},
            {0.0, 0.0}
        }}
    }};

    SecretObject testObject(
        "area0",
        TEST_GEOMETRY,
        14 /* zmin */
    );

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    {
        pqxx::work txn(conn);
        SecretObjectGateway(txn).insert(testObject);
        txn.commit();
        EXPECT_EQ(testObject.id(), 1);
    }

    {
        pqxx::work txn(conn);
        auto loadedObject = SecretObjectGateway(txn).loadById(testObject.id());

        EXPECT_EQ(testObject.name(), loadedObject.name());
        EXPECT_EQ(testObject.zMin(), loadedObject.zMin());
        EXPECT_TRUE(geolib3::test_tools::approximateEqual(
            testObject.mercatorGeom(), loadedObject.mercatorGeom(), 1e-9));
        EXPECT_FALSE(testObject.updatedAt().has_value());
        EXPECT_FALSE(loadedObject.updatedAt().has_value());
    }

    testObject.setUpdatedAt(chrono::TimePoint::clock::now());
    {
        pqxx::work txn(conn);
        SecretObjectGateway(txn).update(testObject);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto loadedObject = SecretObjectGateway(txn).loadById(testObject.id());

        EXPECT_EQ(testObject.name(), loadedObject.name());
        EXPECT_EQ(testObject.zMin(), loadedObject.zMin());
        EXPECT_TRUE(geolib3::test_tools::approximateEqual(
            testObject.mercatorGeom(), loadedObject.mercatorGeom(), 1e-9));
        ASSERT_TRUE(testObject.updatedAt().has_value());
        ASSERT_TRUE(loadedObject.updatedAt().has_value());
        EXPECT_EQ(testObject.updatedAt().value(),
            loadedObject.updatedAt().value());
    }
}

} // suite

} // namespace maps::factory::db::tests

