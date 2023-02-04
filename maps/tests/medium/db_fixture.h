
#pragma once

#include <yandex/maps/wiki/unittest/arcadia.h>

#include <pqxx/pqxx>

namespace maps::wiki::socialsrv::serialize::tests {

struct DbFixture : public unittest::ArcadiaDbFixture {
    pqxx::connection conn;
    DbFixture()
        : conn(connectionString())
    {}
};

} // namespace maps::wiki::socialsrv::serialize::tests
