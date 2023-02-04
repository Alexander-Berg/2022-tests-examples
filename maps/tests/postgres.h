#pragma once

#include <maps/automotive/store_internal/lib/dao/transaction.h>
#include <maps/automotive/store_internal/tests/mocks.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <maps/libs/pgpool/include/pgpool3.h>

#include <pqxx/pqxx>

namespace maps::automotive::store_internal {

class AppContextPostgresMock: public AppContextMock
{
public:
    AppContextPostgresMock();

    pgpool3::Pool& pgPool() override
    {
        return pgPool_;
    }

private:
    local_postgres::Database postgres_;
    pgpool3::Pool pgPool_;
};

class AppContextPostgresFixture: public AppContextFixture
{
public:
    AppContextPostgresFixture();
};

} // namespace maps::automotive::store_internal
