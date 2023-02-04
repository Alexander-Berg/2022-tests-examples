#include <maps/libs/local_postgres/include/instance.h>

#include <library/cpp/testing/common/env.h>

inline std::string appSourcePath()
{
    return ArcadiaSourceRoot() + "/maps/goods/lib/goods_db/schema/";
}

inline std::string appSourcePath(const std::string& relPath)
{
    return appSourcePath() + relPath;
}

void createExtensions(maps::local_postgres::Database& db);
std::string getNormalizedDatabaseSchema(maps::local_postgres::Database& db);

