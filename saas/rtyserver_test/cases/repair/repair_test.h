#pragma once

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(TRepairTest)
    TVector<TString> TempIndices;
protected:
    void Check(int countDocs);
    void PrepareIndex(ui32 countDocs);
public:
    bool InitConfig() override;
};
