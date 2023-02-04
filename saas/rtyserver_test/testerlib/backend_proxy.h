#pragma once

#include <saas/rtyserver_test/testerlib/config_fields.h>
#include <saas/rtyserver_test/common/test_abstract.h>
#include <saas/rtyserver_test/common/backend_config.h>

#include <saas/rtyserver/common/doc_search_info.h>
#include <saas/rtyserver/synchronizer/library/sync.h>

#include <saas/library/sharding/sharding.h>
#include <saas/library/daemon_base/actions_engine/controller_client.h>

#include <library/cpp/charset/ci_string.h>

#include <util/string/vector.h>
#include <util/generic/hash_set.h>
#include <util/generic/typetraits.h>
#include <util/generic/maybe.h>

struct TRestartServerStatistics {
    ui64 StopTimeMilliseconds;
    ui64 StartTimeMilliseconds;
};

class ISearchSourceSelector {
public:
    ISearchSourceSelector(i32 source = -1)
        : Source(source)
    {}

    virtual ~ISearchSourceSelector() {}
    bool ParseAndCheck(const TString& docidStr, const TString& query, ui32 requiredMetaSearchLevel);
    bool HasSpecialSource() const;

    ui32 GetParsedDocId() const;
    ui32 GetParsedSearcherId() const;
    i32 GetSource() const;

    using TPtr = TAtomicSharedPtr<ISearchSourceSelector>;
private:
    virtual bool DoCheck(const TVector<TString>& splitted, const TString& query, ui32 requiredMetaSearchLevel) = 0;

    TMaybeFail<ui64> DocId;
    TMaybeFail<ui16> SearcherId;
    const i32 Source;
};

class TDefaultSourceSelector : public ISearchSourceSelector {
public:
    TDefaultSourceSelector(i32 source)
        : ISearchSourceSelector(source)
    {}
private:
    virtual bool DoCheck(const TVector<TString>& splitted, const TString& query, ui32 requiredMetaSearchLevel);
};

struct TQuerySearchContext {
    using TDocProperties = TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >;

    TDocProperties* DocProperties = nullptr;
    THashMultiMap<TString, TString>* SearchProperties = nullptr;
    THashMap<TString, TString> HttpHeaders;
    THashMap<TString, ui32>* DocsPerCategory = nullptr;
    bool PrintResult = false;
    TString Host = TString();
    ui16 Port = 0;
    ISearchSourceSelector::TPtr SourceSelector = new TDefaultSourceSelector(-1);
    ui32 AttemptionsCount = 1;
    i32 ResultCountRequirement = -1;
    bool CompressedReport = false;
    bool AppendService = true;
    bool HumanReadable = false;
    bool CheckReportInAnyCase = true;
    TString ReportFormat = "";
    TVector<TString> Errors;
    TVector<TString> EventLog;
};

class TIndifferentSourceSelector : public ISearchSourceSelector {
public:
    TIndifferentSourceSelector(i32 source)
        : ISearchSourceSelector(source)
    {}
private:
    virtual bool DoCheck(const TVector<TString>& splitted, const TString& query, ui32 requiredMetaSearchLevel);
};

class TLevelSourceSelector: public ISearchSourceSelector {
private:
    ui32 Level;
public:
    TLevelSourceSelector(ui32 level)
        : Level(level)
    {

    }
private:

    virtual bool DoCheck(const TVector<TString>& splitted, const TString& query, ui32 requiredMetaSearchLevel);
};

class TDocIdsCollector: public ISearchSourceSelector {
public:
    struct TDocIdInfo {
        TString DocId;
        TMap<TString, TDocIdInfo> Children;

        TDocIdInfo() {

        }

        TDocIdInfo(const TString& docId) {
            DocId = docId;
        }

        void AddInfo(const TVector<TString>& splitted, ui32 index = 0) {
            if (splitted.size() == index)
                return;
            Children[splitted[index]] = TDocIdInfo(splitted[index]);
            Children[splitted[index]].AddInfo(splitted, index + 1);
        }
    };
private:
    TDocIdInfo Collection;
public:
    TDocIdsCollector()
        : Collection("root")
    {

    }

    const TDocIdInfo& GetCollection() const {
        return Collection;
    }
private:

    virtual bool DoCheck(const TVector<TString>& splitted, const TString& /*query*/, ui32 /*requiredMetaSearchLevel*/) {
        Collection.AddInfo(splitted);
        return true;
    }
};

class TDirectSourceSelector: public TLevelSourceSelector {
public:
    TDirectSourceSelector()
        : TLevelSourceSelector(1)
    {

    }
};

class TBackendProxy : public IBackendController {
public:
    enum TCachePolicy {NO_CACHE, LIFE_TIME, LIFE_TIME_MODIFIED};
    class TBackendSet : public TSet<ui32> {
    public:
        TBackendSet(int number) {
            if (number >= 0)
                insert(number);
        }

        TBackendSet()
        {}
    };
public:
    TBackendProxy(const TBackendProxyConfig& config);
    void RestartServer(bool rigidStop = false, TRestartServerStatistics* statistics = nullptr);
    void StopBackends();
    void StopBackend(ui32 backend);
    void RestartBackend(ui32 backend);
    void AbortBackends(TDuration waitStartTime, const TBackendSet& backends = TBackendSet());
    void WaitEmptyIndexingQueues() const;
    void WaitActiveServer(const TDuration& duration = TDuration::Zero(), const TBackendSet& backends = TBackendSet()) const;
    void WaitServerDesiredStatus(const THashSet<TCiString>& statuses, const TDuration& duration = TDuration::Zero(),
        const TBackendSet& backends = TBackendSet()) const;
    void WaitDistributorExhausted() const;
    bool IsServerActive(const TBackendSet& backends = TBackendSet()) const;
    bool ServerHasStatus(const THashSet<TCiString>& statuses, const TBackendSet& backends = TBackendSet()) const;
    bool IsDistributorExhausted() const;
    ui64 GetMetric(const TString& name, const TBackendSet& backends = TBackendSet()) const;
    NJson::TJsonValue GetDistributorReplies() const;
    void CheckFormat(const TString& searchResult, TQuerySearchContext& context) const;
    ui32 ProcessQuery(const TString& query, TString* result, TString host = TString(), ui16 port = 0, bool appendService = true, const THashMap<TString, TString>* headers = nullptr) const;
    ui16 QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, bool preffixedIndex, TQuerySearchContext& context) const;
    ui16 QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, bool preffixedIndex, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >* docProperties, THashMultiMap<TString, TString>* searchProperties, bool printResult = false, const TString& host = TString(), ui16 port = 0, ISearchSourceSelector::TPtr sourceSelector = new TDefaultSourceSelector(-1), const THashMap<TString, TString>* headers = nullptr) const;
    void ProcessReport(const TString& searchResult, const TString& query, TVector<TDocSearchInfo>& results, bool preffixedIndex, TQuerySearchContext& context) const;
    void QueryPrint(const TString& query) const;
    bool IsFinalIndex(const TString& dir) const;
    const TSet<TString> GetFinalIndexes(bool stopServer = true) const;
    void WaitIsRepairing() const;

    TString SendCommandToDeployManager(const TString& command, const ui16 shiftPort = 0, const TString& data = "") const;
    void UploadDataToDeployManager(const TString& data, const TString& path) const;
    void UploadFileToDeployManager(const TString& filename, const TString& path) const;
    TString Deploy(const TString& command) const;
    void WaitDeploy(const TString& taskId) const;
    TConfigFieldsPtr PatchConfigDiff(TConfigFieldsPtr diff, const TString& configType = TString(), const TString& service = "tests") const;
    void PatchConfigDiff(const TString& filename, const TString& configType = TString(), const TString& service = "tests") const;
    void ExecuteActionOnDeployManager(NDaemonController::TAction& action) const;

    TJsonPtr ProcessCommand(const TString& command, const TBackendSet& backends = TBackendSet(), const TString& data = "", ui32 attemps = 10) const;
    TJsonPtr ProcessCommandOneHost(const TString& command, const TString& chost, const ui16 cport, const TString& data = "", ui32 attemps = 10) const;
    TJsonPtr GetServerInfo() const;
    TJsonPtr GetDocInfo(const TString& fullDocId) const;
    TJsonPtr GetDocInfo(ui32 searcherId, ui32 docId) const;
    TString GetServerBrief() const;
    TCachePolicy GetCachePolicy();

    bool Detach(NSearchMapParser::TShardIndex shardMin, NSearchMapParser::TShardIndex shardMax, const NSaas::TShardsDispatcher::TContext& sharding, TString& reply);
    bool Detach(TVector<NSearchMapParser::TShardIndex> shardMin, TVector<NSearchMapParser::TShardIndex> shardMax, const NSaas::TShardsDispatcher::TContext& sharding, TVector<TString>& result);
    bool ShardsAction(NSearchMapParser::TShardIndex shardMin, NSearchMapParser::TShardIndex shardMax, const NSaas::TShardsDispatcher::TContext& sharding, TString& result, const TString& action);
    bool Synchronize(const TString& idRes, TString& reply);
    bool Synchronize(const TString& idRes, NRTYServer::EConsumeMode consumeMode, TString& result);


    TConfigFieldsPtr ApplyConfigDiff(TConfigFieldsPtr diff, const TBackendSet& backends = TBackendSet(), const TCiString& prefix = "server") const;
    TConfigFieldsPtr ApplyProxyConfigDiff(TConfigFieldsPtr diff, const TCiString& proxyKind, const TCiString& prefix = "") const;
    NJson::TJsonValue GetInfoServerProxy(const TCiString& proxyKind) const;
    TConfigFieldsPtr GetConfigValues(TConfigFieldsPtr diff, const TCiString& prefix = "server", const TBackendSet& backends = TBackendSet(), const TRtyTestNodeType binary=TNODE_RTYSERVER) const;
    TString GetConfigValue(const TCiString& key, const TCiString& prefix = "server", const TBackendSet& backends = TBackendSet(), const TRtyTestNodeType binary = TNODE_RTYSERVER) const;
    template<class T>
    T GetConfigValue(const TCiString& key, const TCiString& prefix = "server", const TBackendSet& backends = TBackendSet()) const {
        return FromString<T>(GetConfigValue(key, prefix, backends));
    }
    const TBackendProxyConfig& GetConfig();
    void SetActiveBackends(const TBackendSet& backends) {
        ActiveBackends = backends;
    }
    void SetRequiredMetaSearchLevel(ui32 level) {
        RequiredMetaSearchLevel = level;
    }
    ui32 GetRequiredMetaSearchLevel() const {
        return RequiredMetaSearchLevel;
    }
    const TBackendSet& GetActiveBackends() const {
        return ActiveBackends;
    }
private:
    bool SynchronizeInternal(const TString& idRes, const NRTYServer::EConsumeMode* consumeMode, TString& result);

private:
    const TBackendProxyConfig& Config;
    TBackendSet ActiveBackends;
    ui32 RequiredMetaSearchLevel;
};
