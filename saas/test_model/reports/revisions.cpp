#include <saas/tools/test_model/viewer_client.h>
#include <saas/tools/test_model/utils.h>
#include <library/cpp/regex/pcre/regexp.h>

class TReportRevisions : public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportRevisions> Registrator;
};

using namespace NJson;

TViewerClientReportFactory::TRegistrator<TReportRevisions> TReportRevisions::Registrator("/revisions");

void TReportRevisions::Process(void* /*ThreadSpecificResource*/) {
    TCiString db = RD->CgiParam.Get("database");
    TReaderPtr<TModel::TChildren> dbs = Model->GetChildren();
    TModel::TChildren::const_iterator iDb = dbs->find(db);
    if (iDb == dbs->end()) {
        Errors << "Unknown database: " << db;
        Success = false;
        return;
    }
    size_t count = 10;
    if (RD->CgiParam.Find("count") != RD->CgiParam.end())
        count = FromString<size_t>(RD->CgiParam.Get("count"));

    ui64 revision = Max<ui64>();
    if (RD->CgiParam.Find("revision") != RD->CgiParam.end())
        revision = FromString<ui64>(RD->CgiParam.Get("revision"));

    TString regStr("TEST_");
    if (RD->CgiParam.Find("filter") != RD->CgiParam.end())
        regStr = RD->CgiParam.Get("filter");
    TRegExMatch filter(regStr.data());

    TReaderPtr<TDataBase::TChildren> tests = iDb->second->GetChildren();
    TMap <ui64, TRevisionInfo*> results;
    for (TDataBase::TChildren::const_iterator iTest = tests->begin(); iTest != tests->end(); ++iTest) {
        if (!filter.Match(iTest->first.data()))
            continue;
        TReaderPtr<TTest::TChildren> executions = iTest->second->GetChildren();
        TTest::TChildren::const_iterator iEx = executions->upper_bound(revision);
        for (size_t i = 0; i < count; ++i) {
            if (iEx == executions->end())
                break;
            results[iEx->first] = &iEx->second->GetData().Revision;
            ++iEx;
        }
        iEx = executions->lower_bound(revision);
        for (size_t i = 0; i < count; ++i) {
            if (iEx != executions->end())
                results[iEx->first] = &iEx->second->GetData().Revision;
            if (iEx == executions->begin())
                break;
            --iEx;
        }
    }
    NJson::TJsonValue& jvRevisions = Result.InsertValue("revisions", NJson::JSON_MAP);
        for(TMap <ui64, TRevisionInfo*>::const_iterator i = results.begin(); i != results.end(); ++i)
            WriteRevisionInfo(*i->second, jvRevisions.InsertValue(ToString(i->first), NJson::JSON_MAP));
}
