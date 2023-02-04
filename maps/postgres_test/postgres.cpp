#include "postgres.h"

#include <library/cpp/resource/resource.h>
#include <util/string/split.h>

#include <maps/b2bgeo/libs/postgres/helpers.h>

#include <vector>

using namespace maps::b2bgeo;

Postgres::Postgres(const std::string& dbSchema)
{
    const auto dbSchemaQuery = NResource::Find(dbSchema);
    executeSql(dbSchemaQuery);
}

maps::pgpool3::Pool createPool(const Postgres& postgres)
{
    const std::vector<std::string> hosts =
        StringSplitter(postgres.host()).Split(',').SkipEmpty();
    return maps::pgpool3::Pool(
        pg::getConfiguration(hosts, postgres.port()),
        postgres.connectionString(),
        pg::getConstants());
}
