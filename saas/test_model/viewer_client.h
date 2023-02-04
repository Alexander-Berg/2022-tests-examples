#pragma once

#include "model.h"
#include <library/cpp/object_factory/object_factory.h>
#include <library/cpp/http/server/http_ex.h>
#include <library/cpp/json/json_value.h>

class TViewerClientReport;

class TViewerClient : public THttpClientRequestEx {
    friend class TViewerClientReport;
public:
    TViewerClient(TModel& model);
    virtual bool Reply(void* ThreadSpecificResource);
private:
    TModel& Model;
};

class TViewerClientReport : public IObjectInQueue {
public:
    TViewerClientReport()
        : Success(true)
        , Model(nullptr)
        , RD(nullptr)
    {}

    void SetParams(TModel& model, TServerRequestData& rd) {
        Model = &model;
        RD = &rd;
    }
    bool IsSuccess() {return Success;}
    const NJson::TJsonValue& GetResult() {return Result;}
    const TString& GetErrors(){return Errors.Str();}

protected:
    TStringStream Errors;
    NJson::TJsonValue Result;
    bool Success;
    TModel* Model;
    TServerRequestData* RD;
};

typedef NObjectFactory::TObjectFactory<TViewerClientReport, TCiString> TViewerClientReportFactory;
