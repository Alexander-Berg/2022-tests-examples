#pragma once

#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/arcadia.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/assessment/include/console.h>

namespace maps::wiki::assessment::tests {

struct DbFixture: public unittest::ArcadiaDbFixture
{
    pqxx::connection conn;

    DbFixture()
        : conn(connectionString())
    {}
};

std::vector<Console> createConsoles(pqxx::transaction_base& txn, const std::vector<TUid>& uids);
TId createUnit(pqxx::transaction_base& txn, const Unit& unit);

} // namespace maps::wiki::assessment::tests
