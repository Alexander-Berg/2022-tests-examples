#pragma once

#include <yandex/maps/wiki/revision/common.h>
#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/libs/log8/include/log8.h>
#include <yandex/maps/wiki/unittest/arcadia.h>
#include <yandex/maps/wiki/unittest/localdb.h>

#include <string>
#include <list>

namespace maps::wiki::misc::tests {

const revision::UserID TEST_UID = 111;

// returns max commit id
std::list<revision::DBID> importDataToRevision(pgpool3::Pool& pgPool, const std::string& filename);

void syncViewWithRevision(unittest::ArcadiaDbFixture& db);

std::string arcadiaDataPath(const std::string& filename);

struct DBFixture : unittest::ArcadiaDbFixture
{
    DBFixture()
    {
        log8::setLevel(log8::Level::INFO);
    }
};

} // namespace maps::wiki::misc::tests
