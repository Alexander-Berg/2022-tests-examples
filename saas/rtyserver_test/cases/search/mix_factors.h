#pragma once

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestMixFactorsBase)
    TVector<NRTYServer::TMessage> Messages;
    typedef THashMap<TString, float> TFactors;
    typedef THashMap<TString, TFactors> TVectorFactorsByQuery;
    typedef THashMap<TString, TVectorFactorsByQuery> TFactorsByUrl;
    TFactorsByUrl FactorsByUrl;

    bool CheckResults(const TString& query, const TString& qsQuery, const TString& cgi, i32 mustBeCount = -1);
    void CheckResults(i32 mustBeCount = -1);
    void GenerateMessages(size_t count);
    bool InitConfig() override;
};
