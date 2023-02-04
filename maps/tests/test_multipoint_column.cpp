#include <maps/factory/libs/db/multipoint_column.h>

#include <maps/factory/libs/db/common.h>

#include <maps/factory/libs/geometry/multipoint.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <utility>

namespace maps::factory::db::tests {
using namespace testing;
using namespace geometry;

Y_UNIT_TEST_SUITE(multipoint_column_should) {

class Entity {
public:
    Entity(int64_t id, MultiPoint3d geom)
        : id_(id)
        , geom_(std::move(geom)) {}

    [[nodiscard]] int64_t id() const { return id_; }

    [[nodiscard]] const MultiPoint3d& geom() const { return geom_; }

    void setGeom(const MultiPoint3d& geom) { geom_ = geom; }

private:
    friend class sql_chemistry::GatewayAccess<Entity>;

    template <typename T>
    static auto introspect(T& t) { return std::tie(t.id_, t.geom_); }

    Entity() = default;

    int64_t id_{};
    MultiPoint3d geom_{};
};

MultiPoint3d points1() { return {{10, 30, 5}, {30, 10, 4}, {40, 40, 6}}; }

MultiPoint3d points2() { return {{10, 130, 2}, {30, 110, 1}, {40, 140, 3}}; }

struct Table : sql_chemistry::Table<Entity> {
    static constexpr std::string_view name_{"sat.test_patch"sv};
    static constexpr sql_chemistry::Int32PrimaryKey id{"id"sv, name_};
    static constexpr MultiPointColumn geom{"geom"sv, name_};

    static constexpr auto columns_() { return std::tie(id, geom); }
};

class TableGateway : public sql_chemistry::Gateway<Table> {
public:
    using sql_chemistry::Gateway<Table>::Gateway;

    template <typename... C>
    std::vector<Entity> loadIntersection(const tile::Tile& tile, const C& ... clauses)
    {
        struct IntersectedTable : Table {
            const IntersectedMultiPointColumn geom;
            auto columns_() const { return std::tie(id, geom); }
        };
        const IntersectedTable table{.geom={tile, Table::geom.name(), Table::name_}};
        return Gateway<IntersectedTable>(this->txn(), table).load(clauses...);
    }
};

void setup(pqxx::work& txn)
{
    txn.exec("CREATE TABLE sat.test_patch(id integer, geom geometry(MULTIPOINTZ, 3395));");
    txn.exec("INSERT INTO sat.test_patch(id, geom) VALUES"
             "(1, ST_GeomFromText('MULTIPOINTZ (10 30 5,30 10 4,40 40 6)')), "
             "(2, ST_GeomFromText('MULTIPOINTZ (10 130 2,30 110 1,40 140 3)'))"
             ";");
}

Y_UNIT_TEST(load)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    setup(txn);
    const auto entities = TableGateway(txn).load(sql_chemistry::orderBy(Table::id));
    ASSERT_EQ(entities.size(), 2u);
    EXPECT_EQ(entities[0].geom(), points1());
    EXPECT_EQ(entities[1].geom(), points2());
}

Y_UNIT_TEST(load_intersected)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    setup(txn);
    const tile::Tile tile(0, 0, 0);
    const auto entities = TableGateway(txn).loadIntersection(tile, sql_chemistry::orderBy(Table::id));
    ASSERT_EQ(entities.size(), 2u);
    EXPECT_EQ(entities[0].geom(), points1());
    EXPECT_EQ(entities[1].geom(), points2());
}

Y_UNIT_TEST(load_intersected_empty)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    setup(txn);
    const tile::Tile tile(1048576, 1048576, 22);
    const auto entities = TableGateway(txn).loadIntersection(tile, sql_chemistry::orderBy(Table::id));
    ASSERT_EQ(entities.size(), 2u);
    EXPECT_THAT(entities[0].geom(), IsEmpty());
    EXPECT_THAT(entities[1].geom(), IsEmpty());
}

Y_UNIT_TEST(update)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    setup(txn);
    const Id id = 2;
    const MultiPoint3d points{{1, 2, 3}, {4, 5, 6}};
    {
        Entity e = TableGateway(txn).loadById(id);
        e.setGeom(points);
        TableGateway(txn).update(std::move(e));
    }
    const Entity e = TableGateway(txn).loadById(id);
    EXPECT_EQ(e.geom(), points);
}

Y_UNIT_TEST(insert)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);
    setup(txn);
    const Id id = 3;
    const MultiPoint3d points{{1, 2, 3}, {4, 5, 6}};
    {
        Entity entity(id, points);
        TableGateway(txn).insert(std::move(entity));
    }
    const Entity e = TableGateway(txn).loadById(id);
    EXPECT_EQ(e.geom(), points);
}

} // suite
} // namespace maps::factory::db::tests
