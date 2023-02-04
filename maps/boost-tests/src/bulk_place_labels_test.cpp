#include "../include/tools.h"
#include "../include/contexts.hpp"

#include <yandex/maps/tilerenderer4/IOnlineRenderer.h>
#include <yandex/maps/renderer5/postgres/IPostgresTransactionProvider.h>

#include <boost/test/unit_test.hpp>

using namespace maps;
using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::tilerenderer4;
using test::tools::countRowsFromTable;

BOOST_AUTO_TEST_SUITE( OnlineRenderer )
BOOST_AUTO_TEST_SUITE( updateLabels  )
BOOST_AUTO_TEST_SUITE( bulkPlaceLabels )

namespace {
class OperationProgressStub: public tilerenderer4::IOperationProgress
{
public:
    virtual bool cancelled() { return false; }
    virtual void progress(double percentage) {}
};

auto execQuery(PQXXTransactionPtr trans, const std::string& query) -> decltype(trans->exec(query))
{
    static unsigned int id = 0;
    std::ostringstream queryHashSS;
    queryHashSS << "__tilerenderer_test_query__" << id++;
    const std::string queryHash = queryHashSS.str();

    try {
        trans->conn().prepare_binary(queryHash, query);
        return trans->prepared(queryHash).exec();
    } catch (const std::exception& e) {
        std::ostringstream ss;
        ss << "Error occurred during sql query '" << query << "': " << e.what();
        BOOST_TEST_MESSAGE(ss.str());
        throw;
    } catch (...) {
        std::ostringstream ss;
        ss << "Error occurred during sql query '" << query << "'";
        BOOST_TEST_MESSAGE(ss.str());
        throw;
    }
}

void deleteAllFromTable(PQXXTransactionPtr trans, const std::string& tableName)
{
    std::ostringstream query;
    query
        << "DELETE "
        << "FROM " << tableName;

    execQuery(trans, query.str());
}

class BulkPlaceLabelsContext: public OnlineRendererContext<TransactionProviderContext<>>
{
public:
    enum ResultType {
        USE_LABELS_TABLE_1,
        USE_LABELS_TABLE_2
    };

    void run(
        const unsigned int maxThreadsToUse,
        const PostgresTransactionProviderExPtr& transactionProvider,
        const ResultType result)
    {
        const std::string TABLE_WITH_LABELS_1 = "renderer_autotest.testptfc";
        const std::string TABLE_WITH_LABELS_2 = "renderer_autotest2.testptfc";
        const std::string XML_MAP_NAME = "tests/boost-tests/maps/SmallTextMap.xml";

        BOOST_REQUIRE_NO_THROW(deleteAllFromTable(trans, TABLE_WITH_LABELS_1));
        BOOST_REQUIRE_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 0);

        BOOST_REQUIRE_NO_THROW(deleteAllFromTable(trans2, TABLE_WITH_LABELS_2));
        BOOST_REQUIRE_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);

        BOOST_REQUIRE_NO_THROW(renderer->open(XML_MAP_NAME));

        OperationProgressStub op;
        std::unique_ptr<BulkLabelTask> task = BulkLabelTask::create();
        task->zooms.insert(18);
        task->maxThreadsToUse = maxThreadsToUse;
        BOOST_REQUIRE_NO_THROW(renderer->placeLabels(op, *task, transactionProvider));

        if (result == USE_LABELS_TABLE_1) {
            BOOST_CHECK_NE(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 0);
            BOOST_CHECK_EQUAL(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);
        } else { // result == USE_LABELS_TABLE_2
            BOOST_CHECK_EQUAL(countRowsFromTable(*trans, TABLE_WITH_LABELS_1), 0);
            BOOST_CHECK_NE(countRowsFromTable(*trans2, TABLE_WITH_LABELS_2), 0);
        }
    }
};
}

BOOST_FIXTURE_TEST_CASE(threads0, BulkPlaceLabelsContext)
{
    run(0, providerEx, USE_LABELS_TABLE_1);
}

BOOST_FIXTURE_TEST_CASE(threads1, BulkPlaceLabelsContext)
{
    run(1, providerEx, USE_LABELS_TABLE_1);
}

BOOST_FIXTURE_TEST_CASE(threads2, BulkPlaceLabelsContext)
{
    run(2, providerEx, USE_LABELS_TABLE_1);
}

BOOST_FIXTURE_TEST_CASE(threads10, BulkPlaceLabelsContext)
{
    run(10, providerEx, USE_LABELS_TABLE_1);
}

BOOST_FIXTURE_TEST_CASE(threads0_2trn, BulkPlaceLabelsContext)
{
    run(0, provider2Trns, USE_LABELS_TABLE_2);
}

BOOST_FIXTURE_TEST_CASE(threads1_2trn, BulkPlaceLabelsContext)
{
    run(1, provider2Trns, USE_LABELS_TABLE_2);
}

BOOST_FIXTURE_TEST_CASE(threads2_2trn, BulkPlaceLabelsContext)
{
    run(2, provider2Trns, USE_LABELS_TABLE_2);
}

BOOST_FIXTURE_TEST_CASE(threads10_2trn, BulkPlaceLabelsContext)
{
    run(10, provider2Trns, USE_LABELS_TABLE_2);
}

BOOST_AUTO_TEST_SUITE_END() // bulkPlaceLabels
BOOST_AUTO_TEST_SUITE_END() // updateLabels
BOOST_AUTO_TEST_SUITE_END() // OnlineRenderer
