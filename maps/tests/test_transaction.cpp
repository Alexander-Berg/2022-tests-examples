#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/dao/transaction.h>
#include <maps/automotive/store_internal/tests/postgres.h>

#include <pqxx/pqxx>

namespace maps::automotive::store_internal {

TEST_F(AppContextPostgresFixture, NoModificationInReadOnlyTxn)
{
    auto txn = dao::makeReadOnlyTransaction();
    EXPECT_THROW(txn->exec("delete from package"), pqxx::sql_error);
}

}
