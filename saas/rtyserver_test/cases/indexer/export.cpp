#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/kiwi_export/export.h>

START_TEST_DEFINE(TestExportIndex)
bool Run() override {
    if (!GetIsPrefixed())
        return true;

    ExportFromDump(GetResourcesDirectory() + "/kiwi_test/pds.calctext",
                   Controller->GetConfig().Export.Port, "localhost", 10);
    Sleep(TDuration::Minutes(2));

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&kps=2", results);
    if (!results.size()) {
        ythrow yexception() << "no docs after export";
    }

    ReopenIndexers();

    QuerySearch("url:\"*\"&kps=2", results);
    if (!results.size()) {
        ythrow yexception() << "no docs after reopen";
    }

    return true;
}
};

START_TEST_DEFINE(TestExportIncorrect)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    ExportFromDump(GetResourcesDirectory() + "/kiwi_test/incorrect_service.calctext",
                   Controller->GetConfig().Export.Port, "localhost", 10);
    Sleep(TDuration::Seconds(15));

    TQuerySearchContext context;
    context.ResultCountRequirement = 0;
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=500", results, context);

    CHECK_TEST_EQ(results.size(), 0);

    ReopenIndexers();

    QuerySearch("url:\"*\"&numdoc=500", results, context);

    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
};
