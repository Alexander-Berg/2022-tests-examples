#pragma once

#include <library/cpp/logger/global/global.h>
#include <library/cpp/object_factory/object_factory.h>
#include <util/generic/hash.h>
#include <util/generic/ptr.h>
#include <util/generic/string.h>

namespace NUnitTest {
    struct TTestContext;
}

class TMiniCluster;

enum TRtyTestNodeType { TNODE_RTYSERVER, TNODE_SEARCHPROXY, TNODE_INDEXERPROXY, TNODE_SCRIPT, TNODE_DISTRIBUTOR };

class IBackendController {
public:
    virtual ~IBackendController() {}
};

struct TRtyServerTestEnv {
    TString TestName;
    TString ResourceDir;
    TString LogPath = "console";
    TString ConfigPatches;
    TString KeyPreffixed;
    TString ClusterConfig;
    ui32 ShardsNumber = 5;
    ui32 LogLevel = TLOG_DEBUG;

    TString CacheDir;
    TString CacheLifeTime;
    bool NoSearch = false;

public:
    TRtyServerTestEnv(const TString& testName)
        : TestName(testName)
    {}
};

class ITestCase {
public:
    typedef TSimpleSharedPtr<ITestCase> Ptr;
    typedef TVector<TRtyServerTestEnv> TTestVariants;

    class ICallback {
    public:
        virtual ~ICallback() {}
        virtual bool RunNode(TString nodeName, TRtyTestNodeType product = TNODE_SCRIPT) = 0;
        virtual bool WaitNode(TString nodeName, TRtyTestNodeType product = TNODE_SCRIPT) = 0;
        virtual bool RestartNode(TString nodeName, TRtyTestNodeType product = TNODE_SCRIPT) = 0;
        virtual bool StopNode(TString nodeName, TRtyTestNodeType product = TNODE_SCRIPT) = 0;
        virtual bool SendSignalNode(TString nodeName, TRtyTestNodeType product, ui32 signum) = 0;
        virtual TSet<TString> GetNodesNames(TRtyTestNodeType product = TNODE_SCRIPT) const = 0;
    };
public:
    virtual ~ITestCase() {}
    virtual TString Name() = 0;
    virtual bool ConfigureRuns(TTestVariants& variants, bool cluster) = 0;
    virtual bool Prepare() = 0;
    virtual bool Run() { return false; }
    virtual bool Run(NUnitTest::TTestContext& /*context*/) { return Run(); }
    virtual bool Finish() = 0;
    virtual void SetNoSearch(bool value) = 0;
    virtual bool InitConfig() = 0;
    virtual bool ApplyConfig() = 0;
    virtual void CheckConfig() = 0;
    virtual void InitCluster() {}
    virtual bool NeedStartBackend() = 0;
    virtual void SetController(IBackendController* controller) = 0;
    virtual void SetCallback(ICallback* callback) = 0;
    virtual void SetResourcesDirectory(const TString& resDir) = 0;
    virtual void SetRootDirectory(const TString& resDir) = 0;
    virtual void SetSendIndexReply(bool value) = 0;
    virtual void SetNavSourceFileName(const TString& value) = 0;
    virtual void SetFactorsFileName(const TString& value) = 0;
    virtual void SetGenerateIndexedDoc(bool value) = 0;
    virtual void SetGetSaveResponses(bool value) = 0;
    virtual void SetRunNDolbs(ui16 value) = 0;
    virtual void SetPrefixed(bool value) = 0;
    virtual void ResetGenerationShift() = 0;
    virtual void SetInfoCluster(const TMiniCluster* cluster) = 0;
    typedef TSet<TString> TTags;
    static TTags& SetTags(TTags& tags) {tags.clear(); return tags;}
};

class TRTYServerTestsFactory : public NObjectFactory::TObjectFactory<ITestCase, TString> {
public:
    template<class Product>
    void Register(const TString& name) {
        NObjectFactory::TObjectFactory<ITestCase, TString>::Register<Product>(name);
        Product::SetTags(Tags[name]);
    }

    const ITestCase::TTags* GetTags(const TString& name) {
        TTagHash::const_iterator i = Tags.find(name);
        return i == Tags.end() ? nullptr : &i->second;
    }

    template<class Product>
    class TRegistrator {
    public:
        TRegistrator(const TKey& key) {
            Singleton<TRTYServerTestsFactory>()->Register<Product>(key);
        }
    };

private:
    typedef THashMap<TString, ITestCase::TTags> TTagHash;
    TTagHash Tags;
};
