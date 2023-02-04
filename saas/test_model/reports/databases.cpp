#include <saas/tools/test_model/viewer_client.h>


class TReportDatabases :  public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportDatabases> Registrator;
};

using namespace NJson;

TViewerClientReportFactory::TRegistrator<TReportDatabases> TReportDatabases::Registrator("/databases");

void TReportDatabases::Process(void* /*ThreadSpecificResource*/) {

    TReaderPtr<TModel::TChildren> dbs = Model->GetChildren();
    Result.SetType(JSON_MAP);
    TJsonValue& dbList = Result.InsertValue("databases", JSON_ARRAY);
    for(TModel::TChildren::const_iterator iDb = dbs->begin(); iDb != dbs->end(); ++iDb){
        TJsonValue& dbDesc = dbList.AppendValue(JSON_MAP);
        dbDesc.InsertValue("name", iDb->first);
        dbDesc.InsertValue("is_started", ToString(iDb->second->GetData().Status));
    }
}
