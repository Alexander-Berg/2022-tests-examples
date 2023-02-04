#include "common.h"

#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/common/env.h>
#include <boost/test/unit_test.hpp>

#include <string>
#include <memory>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

DbFixture::DbFixture()
{
    instance_.createExtension("hstore");
    instance_.createExtension("postgis");

    createSchema();

    try {
        conn_.reset(new pqxx::connection(instance_.connectionString()));
    } catch (const pqxx::broken_connection&) {
        // pass
    }
}

void DbFixture::createSchema()
{
    instance_.executeSql(
        "DROP SCHEMA IF EXISTS " + sql::schema::REVISION + " CASCADE;");
    instance_.executeSqlInTransaction(
        common::readFileToString(SRC_("../sql/postgres_upgrade.sql")));
}

std::string DbFixture::connectionString()
{
    return instance_.connectionString();
}

pqxx::connection& DbFixture::getConnection()
{
    BOOST_REQUIRE_MESSAGE(conn_, "Couldn't obtain DB connection");
    return *conn_;
}

void DbFixture::setTestData(const std::string& sqlPath)
{
    pqxx::work work(getConnection());
    RevisionsGateway gtw(work);
    gtw.truncateAll();
    work.exec(common::readFileToString(SRC_(sqlPath)));
    work.exec(
        "INSERT INTO " + sql::table::ATTRIBUTES_RELATIONS +
        " SELECT * FROM " + sql::table::ATTRIBUTES +
        " WHERE " + sql::col::ID + " IN "
            "(SELECT " + sql::col::ATTRIBUTES_ID +
            " FROM " + sql::table::OBJECT_REVISION_RELATION + ")");
    work.commit();
}

void DbFixture::setTestData()
{
    pqxx::work work(getConnection());
    RevisionsGateway gtw(work);
    gtw.truncateAll();
    work.commit();
}

} //namespace tests
} //namepsace revision
} //namespace wiki
} //namespace maps

