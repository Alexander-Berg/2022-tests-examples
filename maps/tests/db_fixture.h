#pragma once

#include <maps/libs/pgpool/include/pgpool3.h>

#include <maps/libs/local_postgres/include/instance.h>

namespace maps::wiki::jams_arm2::test {

class DbFixture
{
public:
    DbFixture();

    pgpool3::Pool& pool() { return *pool_; }
    std::shared_ptr<pgpool3::Pool> poolPtr() { return pool_; }

private:
    local_postgres::Database database_;
    std::shared_ptr<pgpool3::Pool> pool_;
};

} //namespace maps::wiki::jams_arm2::test
