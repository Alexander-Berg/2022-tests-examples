#pragma once
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/config_checker.h>

SERVICE_TEST_RTYSERVER_DEFINE(TMergerTest)
    bool TestMerger(NRTYServer::TMessage::TMessageType messageType);
public:
    bool InitConfig() override;
protected:
    TMaybeFail<ui64> GetMaxDeadlineRemovedStats();
};

