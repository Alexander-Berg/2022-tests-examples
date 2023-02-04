#pragma once
#include <maps/libs/local_postgres/include/instance.h>
#include <maps/renderer/cartograph/lib/config/config.h>
#include <maps/renderer/cartograph/lib/infra/pg_pool_wrapper.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::renderer::cartograph::test_util {

struct PostgresFixture : NUnitTest::TBaseFixture {
    PostgresFixture();
    local_postgres::Database db;
    std::shared_ptr<PgPoolWrapper> pgPool;
};

} // namespace maps::renderer::cartograph::test_util
