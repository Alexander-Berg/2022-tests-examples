#pragma once

#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

namespace maps::factory::idm::tests {

class Fixture : public testing::Test,
                public unittest::BaseFixture {
public:
    Fixture()
        : pgpool(
        postgres().connectionString(),
        pgpool3::PoolConstants(1, 6, 1, 6)
    ) {}

    void replaceData()
    {
        truncateTables();
        auto txn = txnHandle();
        txn->exec(maps::common::readFileToString(SRC_("data.sql")));
        txn->commit();
    }

    pgpool3::TransactionHandle txnHandle()
    {
        return pgpool.masterWriteableTransaction();
    }

private:
    pgpool3::Pool pgpool;

};

} // namespace maps::factory::idm::tests
