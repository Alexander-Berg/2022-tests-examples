#pragma once

#include "backend_proxy.h"
#include <saas/rtyserver_test/log_checker/log_checker.h>
#include <saas/rtyserver_test/testerlib/config_fields.h>
#include <saas/rtyserver_test/common/test_abstract.h>
#include <saas/rtyserver_test/common/backend_config.h>
#include <library/cpp/getopt/opt.h>

class TTestSet {
public:
    class ICallback : public ITestCase::ICallback {
    public:
        virtual ~ICallback() {}
        virtual void StopCluster() = 0;
        virtual void RestartCluster(TBackendProxyConfig& proxyConfig, const TString& testDir, const TConfigFieldsPtr configDiff) = 0;
        virtual const TMiniCluster& GetCluster() const = 0;
    };
    TTestSet(const NLastGetopt::TOptsParseResult& parseOpt, ICallback* callback);
    TTestSet(const TString& root, const TRtyServerTestEnv& env, ICallback* callback);
    int Run(NUnitTest::TTestContext& context, const TConfigFieldsPtr& externalPatch);
    TBackendProxyConfig& GetProxyConfig();
private:
    void RunTest(const TString& test, NUnitTest::TTestContext& context, const TConfigFieldsPtr& externalPatches);
    TConfigFieldsPtr PrepareBackend(int backendNumber, const TString& rootDir, const TString& diffPrefix);
    void PrepareTest(ITestCase::Ptr test, bool prefixed);
private:
    TString FactorsFileName;
    TSet<TString> ExecutableTests;
    TString RootDir;
    TString ResourcesDir;
    TString StartTest;
    TString RecognizerLibPath;
    TString CacheDir;
    TString ReArrangeOptions;
    TString CacheLifeTime;
    int CountErrors;
    bool SendIndexReply = true;
    bool NoSearch = false;
    bool NotUseParsers = false;
    ui16 CountRunTest = 1;
    THolder<TBackendProxy> Proxy;
    TBackendProxyConfig ProxyConfig;
    bool GenerateIndexedDoc = false;
    bool GetSaveResponses = false;
    ui16 RunNDolbs = 0;
    ICallback* Callback;
    TVector<TLogChecker::TPtr> LogCheckers;
};
