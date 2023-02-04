#pragma once

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestLoadCommon)

virtual void SendProfSignals(){
    TString profileSignal = GetEnv("CPUPROFILESIGNAL");
    if (profileSignal == TString())
        return;
    int signal = FromString<int>(profileSignal);
    TRY
        Callback->SendSignalNode("*", TNODE_SEARCHPROXY, signal);
    CATCH("signal to searchproxy not sent\n");

    TRY
        Callback->SendSignalNode("*", TNODE_INDEXERPROXY, signal);
    CATCH("signal to indexerproxy not sent");

    TRY
        Callback->SendSignalNode("*", TNODE_RTYSERVER, signal);
    CATCH("signal to rtyserver not sent");
}

bool ConfigureRuns(TTestVariants& /*variants*/, bool) override {
    return true;
}
};
