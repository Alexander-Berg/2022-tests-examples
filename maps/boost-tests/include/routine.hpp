/*******************************************************************************
 * ATTENTION!
 * This file is shared between 2 projects: renderer boost-postgres-tests and
 * tilerenderer boost-tests. 
 ******************************************************************************/

#include <yandex/maps/renderer5/postgres/IPostgresTransactionProvider.h>
#include <pqxx/pqxx>
#include <boost/filesystem.hpp>
#include <fstream>
#include <iostream>

#ifndef EXECUTE_SCRIPT_NAMESPACE_START
#define EXECUTE_SCRIPT_NAMESPACE_START
#endif

#ifndef EXECUTE_SCRIPT_NAMESPACE_END
#define EXECUTE_SCRIPT_NAMESPACE_END
#endif

EXECUTE_SCRIPT_NAMESPACE_START
void executeScript(maps::renderer5::postgres::PQXXConnectionPtr conn, const std::string& scriptName)
{
    static const std::string scriptEncoding = "UTF8";
    conn->set_client_encoding(scriptEncoding);

    if (!boost::filesystem::exists(scriptName))
        throw std::runtime_error("sql script was not found");

    std::ifstream in(scriptName.c_str());
    std::string fileContents = std::string(
        std::istreambuf_iterator<char>(in), 
        std::istreambuf_iterator<char>());

    maps::renderer5::postgres::PQXXTransactionPtr trans = 
        maps::renderer5::postgres::PQXXTransactionPtr(new pqxx::work(*conn));

    std::cout << "Executing SQL script '" << scriptName << "' ..." << std::endl;

    trans->exec(fileContents);
    trans->commit();

    std::cout << "Executing SQL script '" << scriptName << "' done" << std::endl;

    static const std::string clientEncoding = "UTF8";
    conn->set_client_encoding(clientEncoding);
}
EXECUTE_SCRIPT_NAMESPACE_END

#ifndef PREPARE_DB_NAMESPACE_START
#define PREPARE_DB_NAMESPACE_START
#endif

#ifndef PREPARE_DB_NAMESPACE_END
#define PREPARE_DB_NAMESPACE_END
#endif

PREPARE_DB_NAMESPACE_START
void prepareDb(const std::string& opt, const std::string& scriptName)
{
    try
    {
        maps::renderer5::postgres::PQXXConnectionPtr conn = 
            maps::renderer5::postgres::PQXXConnectionPtr(new pqxx::connection(opt));
        pqxx::disable_noticer scoped_disabler(*conn);
        executeScript(conn, scriptName);
    }
    catch (const std::exception& ex)
    {
        std::cout << "Failed to init postgres db: " << ex.what() << std::endl;
        throw;
    }
    catch (...)
    {
        std::cout << "Failed to init postgres db" << std::endl;
        throw;
    }
}
PREPARE_DB_NAMESPACE_END