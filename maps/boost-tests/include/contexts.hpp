#pragma once

#include "tests/boost-tests/include/tools/map_tools.h"
#include "postgres/connection_options.h"

#include <yandex/maps/renderer5/postgres/SinglePostgresTransactionProvider.h>
#include <yandex/maps/renderer5/postgres/default_transaction_provider_ex.h>
#include <pqxx/pqxx>

#include <boost/test/unit_test.hpp>

class EmptyContext {};

template<class ParentContext = EmptyContext>
class TransactionContext: public ParentContext
{
public:
    TransactionContext()
    {
        BOOST_REQUIRE_NO_THROW(conn.reset(new pqxx::connection(maps::renderer5::test::postgres::options)));
        trans.reset(new pqxx::work(*this->conn));
    }

public:
    maps::renderer5::postgres::PQXXConnectionPtr conn;
    maps::renderer5::postgres::PQXXTransactionPtr trans;
};

template<class ParentContext = EmptyContext>
class TransactionProviderContext: public TransactionContext<ParentContext>
{
public:
    TransactionProviderContext()
        : provider(new maps::renderer5::postgres::SinglePostgresTransactionProvider(*TransactionContext<ParentContext>::trans))
    {
        providerEx.reset(new maps::renderer5::postgres::DefaultTransactionProviderEx(provider));
    }

public:
    maps::renderer5::postgres::PostgresTransactionProviderPtr provider;
    maps::renderer5::postgres::PostgresTransactionProviderExPtr providerEx;
};

template<class ParentContext = EmptyContext>
class CleanContext: public ParentContext
{
public:
    CleanContext(): tempDir(maps::renderer::io::tempDirPath())
    {
        maps::renderer5::test::map::deleteFilesFromDir(tempDir);
    }
    ~CleanContext()
    {
        maps::renderer5::test::map::deleteFilesFromDir(tempDir);
    }
    const std::string tempDir;
};
