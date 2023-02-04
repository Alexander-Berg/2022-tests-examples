#include <maps/goods/lib/goods_db/schema/tests/helper.h>
#include <library/cpp/testing/gtest/gtest.h>

TEST(sql_functions, rotate_items_history)
{
    maps::local_postgres::Database db;
    createExtensions(db);
    db.runPgMigrate(appSourcePath("migrations"));

    auto migrationsSchema = getNormalizedDatabaseSchema(db);

    db.executeSql("CALL rotate_items_history()");
    db.executeSql("DROP TABLE items_history_snapshot");

    auto afterRotateSchema = getNormalizedDatabaseSchema(db);
    ASSERT_EQ(migrationsSchema, afterRotateSchema);
}

