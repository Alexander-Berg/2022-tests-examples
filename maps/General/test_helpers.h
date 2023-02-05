#pragma once

#include <util/stream/file.h>


namespace maps::ydb {

/**
 * Returns endpoint and database for the local YDB
 * For more details:
 *  https://a.yandex-team.ru/arc/trunk/arcadia/kikimr/public/tools/ydb_recipe/README.md
 *
 * Example:
 *   const auto& [endpoint, database] = getLocalYdb();
 */
inline std::pair<TString, TString> getLocalYdb()
{
    TFileInput endpointFile("ydb_endpoint.txt");
    TFileInput databaseFile("ydb_database.txt");
    TString endpoint = endpointFile.ReadAll();
    TString database = databaseFile.ReadAll();
    return {endpoint, database};
}

} // namespace maps::ydb
