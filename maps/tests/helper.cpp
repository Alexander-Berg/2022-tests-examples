#include "helper.h"

#include <maps/goods/lib/goods_db/schema/tests/pg_dump_output_normalizer.h>
#include <maps/libs/log8/include/log8.h>
#include <boost/process.hpp>

void createExtensions(maps::local_postgres::Database& db)
{
    db.createExtension("pg_trgm");
    db.createExtension("btree_gist");
}

std::string getNormalizedDatabaseSchema(maps::local_postgres::Database& db)
{
    INFO() << "pg_dump path: " << boost::process::search_path("pg_dump");

    boost::process::ipstream out;
    boost::process::child pgDump(
        boost::process::search_path("pg_dump"),
        boost::process::env["PGPASSWORD"]=db.password(),
        "--schema-only",
        "-d", db.dbname(),
        "-h", db.host(),
        "-p", std::to_string(db.port()),
        "-U", db.user(),
        boost::process::std_out > out,
        boost::process::std_err > boost::process::null);

    maps::goods::PgDumpOutputNormalizer normalizer(db);
    auto result = normalizer.normalize(out);

    pgDump.wait();

    INFO() << "Database " << db.dbname() << " schema: \n" << result;
    return result;
}

