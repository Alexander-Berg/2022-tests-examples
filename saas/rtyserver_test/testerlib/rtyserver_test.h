#pragma once

#include "backend_proxy.h"
#include "messages_generator.h"
#include "test_indexer_client.h"

#include <saas/protos/rtyserver.pb.h>
#include <saas/rtyserver_test/common/test_abstract.h>
#include <saas/rtyserver_test/mini_cluster/mini_cluster.h>

#include <library/cpp/charset/ci_string.h>
#include <library/cpp/uri/http_url.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <library/cpp/string_utils/quote/quote.h>

#include <util/system/tempfile.h>


#define START_STOP_LOG(act) {INFO_LOG << #act << " started" << Endl; try {act; INFO_LOG << #act << " finished" << Endl;} catch (...) {INFO_LOG << #act << " failed" << Endl; throw;}};

#define CHECK_TEST_NEQ(var, value) { auto __result = var; if ((__result) == (value)) {ERROR_LOG << (#var) << " == " << (value) << Endl; return false;} else {DEBUG_LOG << #var << " != " << #value << "... OK" << Endl;}; };
#define CHECK_TEST_EQ(var, value) { auto __result = var; if ((__result) != (value)) {ERROR_LOG << (__result) << " != " << (value) << Endl; return false;} else {DEBUG_LOG << #var << " == " << #value << "... OK" << Endl;}; };
#define CHECK_TEST_SUBSTR(str, substr) { auto __str = str; auto __sub = substr; if (__str.find(__sub) == TString::npos) {ERROR_LOG << (__sub) << " is not substr of " << (__str) << Endl; return false;}; };
#define CHECK_TEST_LESS(var, value) { auto __result = var; if ((__result) >= (value)) {ERROR_LOG << (__result) << " >= " << (value) << Endl; return false;} else {DEBUG_LOG << #var << " < " << #value << "... OK" << Endl;}; };
#define CHECK_TEST_LESSEQ(var, value) { auto __result = var; if ((__result) > (value)) {ERROR_LOG << (__result) << " > " << (value) << Endl; return false;} else {DEBUG_LOG << #var << " <= " << #value << "... OK" << Endl;}; };
#define CHECK_TEST_GREATER(var, value) { auto __result = var; if ((__result) <= (value)) {ERROR_LOG << (__result) << " <= " << (value) << Endl; return false;} else {DEBUG_LOG << #var << " > " << #value << "... OK" << Endl;}; };
#define CHECK_TEST_GREATEREQ(var, value) { auto __result = var; if ((__result) < (value)) {ERROR_LOG << (__result) << " < " << (value) << Endl; return false;} else {DEBUG_LOG << #var << " >= " << #value << "... OK" << Endl;}; };
#define CHECK_DSI_URL(dsi, url) {CHECK_TEST_EQ(dsi.GetUrl(), url);}
#define CHECK_DSI_TITLE_RLV(dsi, title, relev) {CHECK_TEST_EQ(dsi.GetTitle(), title); CHECK_TEST_EQ(dsi.GetRelevance(), relev);}
#define CHECK_DSI_URL_RLV(dsi, url, relev) {CHECK_TEST_EQ(dsi.GetUrl(), url); CHECK_TEST_EQ(dsi.GetRelevance(), relev);}
#define CHECK_TEST_FAILED(cond, message) { if (cond) {ERROR_LOG << (#cond) << " failed: " << (message) << Endl; return false;} else {DEBUG_LOG << #cond << " == false... OK" << Endl;}; };
#define CHECK_TEST_TRUE(cond) { if (!(cond)) {ERROR_LOG << "Condition is not true: " << #cond << Endl; return false;} else {DEBUG_LOG << #cond << " == true... OK" << Endl;}; };
#define TEST_FAILED(message) {ERROR_LOG << (message) << Endl; return false;};
#define PRINT_INFO_AND_TEST(test) {PrintInfoServer(); test;}
#define NOT_EXCEPT(action) {try {action;} catch (...) {ERROR_LOG << "Exception on " << #action << "(" << CurrentExceptionMessage() << ")" << Endl; return false;}};

class TTestMarksPool {
public:
    static const char* Merger;
    static const char* NoCoverage;
    static const char* Slow;
    static const char* StaticData;
    static const char* Repair;
    static const char* OneBackendOnly;
    static const char* NeedRabbit;
};

#define MUST_BE_BROKEN(command) \
    {\
        bool isFailed = false;\
        try {\
            command;\
        } catch(...) {\
            isFailed = true;\
        }\
        if (!isFailed)\
            ythrow yexception() << #command << " not broken";\
    }

#define MUSTNT_BE_BROKEN(command) \
    {\
        bool isFailed = false;\
        try {\
            command;\
        } catch(...) {\
            isFailed = true;\
        }\
        if (isFailed)\
            ythrow yexception() << #command << " not broken";\
    }

struct TAttr {
    TAttr(TString val, NRTYServer::TAttribute::TAttributeType type = NRTYServer::TAttribute::LITERAL_ATTRIBUTE)
        : Value(val)
        , Type(type)
    {}
    TAttr(i64 val, NRTYServer::TAttribute::TAttributeType type = NRTYServer::TAttribute::INTEGER_ATTRIBUTE)
        : Value(ToString(val))
        , Type(type)
    {}
    TAttr()
        : Type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE)
    {}

    TString Value;
    NRTYServer::TAttribute::TAttributeType Type;
};

class TTestConfigsCreator {
public:
    TTestConfigsCreator(const TString& name)
        : TestName(name)
    {}

    TRtyServerTestEnv CreateDefault() {
        TRtyServerTestEnv env(TestName);
        env.ShardsNumber = 5;
        env.KeyPreffixed = "";
        env.LogPath = "console";
        env.ResourceDir = GetArcadiaTestsData() + "/rtyserver/test_data/";
        return env;
    }

    TRtyServerTestEnv CreateNoSearch() {
        TRtyServerTestEnv env(CreateDefault());
        env.NoSearch = true;
        return env;
    }

    TRtyServerTestEnv CreateWithCache(const TString& cacheDir = "memory", const TString& lifeTime = "") {
        TRtyServerTestEnv env(CreateDefault());
        env.CacheDir = cacheDir;
        env.CacheLifeTime = lifeTime;
        return env;
    }
private:
    TString TestName;
};

typedef TVector<TMap<TString, TAttr> > TAttrMap;

class TRTYServerTestCase: public ITestCase {
    bool IsPrefixed;
    bool IsResDirSet;
    TString ResourcesDir;
    TString RootDir;
protected:
    bool NoSearch;
    TString NavSourceFileName;
    TString FactorsFileName;
    bool SendIndexReply;
    TBackendProxy* Controller;
    ICallback* Callback;
    TConfigFieldsPtr ConfigDiff;
    TConfigFieldsPtr SPConfigDiff;
    TConfigFieldsPtr IPConfigDiff;
    TConfigFieldsPtr CProxyConfigDiff;
    bool GenerateIndexedDoc;
    bool GetSaveResponses;
    ui16 RunNDolbs;
    const TMiniCluster* Cluster = nullptr;
    enum TIndexerType {DISK, REALTIME, ALL};
    enum TMergerCheckPolicyType {mcpTIME, mcpNEWINDEX, mcpCONTINUOUS, mcpNONE};
    enum TAutoBool {abAUTO = -1, abFALSE = false, abTRUE = true};
public:
    bool HasSearchproxy() const {
        return Cluster->GetNodesNames(TNODE_SEARCHPROXY).size();
    }

    ui32 GetSearchLayersCount() const {
        return HasSearchproxy() ? 2 : 1;
    }

protected:
    void SetInfoCluster(const TMiniCluster* cluster) override {
        Cluster = cluster;
    }
    void ResetGenerationShift() override;
    void CheckMergerResult();
    void GenerateInput(TVector<NRTYServer::TMessage>& messages, size_t num, IMessageGenerator& mGen) const;
    void GenerateInput(TVector<NRTYServer::TMessage>& messages, size_t num, NRTYServer::TMessage::TMessageType messageType, bool isPrefixed
        , const TAttrMap& attrMap = TAttrMap(), const TString& bodyText = "body", const bool useAttrMap = true, const TAttrMap& searchAttr = TAttrMap()) const;

    TVector<NRTYServer::TReply> IndexMessageAsIs(const TVector<NRTYServer::TMessage>& messages, bool strictOrder = false);
    void PrepareData(const TString& source, const TString& destination = TString());

    bool CheckGroups(TString searchResult, int countGroups, int countDocs, int countGroupings = 1);
    bool CheckGroupsTry(TString query, int countGroups, int countDocs, int countGroupings = 1, int countTry = 3);
    void CheckCount(int correctCount);
    void PrintInfoServer() const;
    NRTYServer::TMessage BuildDeleteMessage(const NRTYServer::TMessage& messOriginal);
    void DeleteSomeMessages(const TVector<NRTYServer::TMessage>& messages, TSet<std::pair<ui64, TString> >& deleted, TIndexerType indexer, size_t step = 10);
    NRTYServer::TReply DeleteQueryResult(const TString& query, TIndexerType indexer);
    NRTYServer::TReply DeleteSpecial();
    NRTYServer::TReply DeleteSpecial(ui64 kps);
    void ReadDump(const char* fname, TVector<NRTYServer::TMessage>& messages);
    void ReopenIndexers(const TString serviceName = "tests");
    void SwitchDefaultKps(ui64 newKps);
    ui16 QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >* docProperties = nullptr, THashMultiMap<TString, TString>* searchProperties = nullptr, bool printResult = false, const TString& host = TString(), ui16 port = 0, ISearchSourceSelector::TPtr sourceSelector = new TDefaultSourceSelector(-1), const THashMap<TString, TString>* headers = nullptr);
    ui16 QuerySearchNonEncode(const TString& query, TVector<TDocSearchInfo>& results, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >* docProperties = nullptr, THashMultiMap<TString, TString>* searchProperties = nullptr);
    ui32 ProcessQuery(const TString& query, TString* result);
    bool CheckExistsByText(const TString& text, bool isStrict, TVector<NRTYServer::TMessage>& messages, const TString& firstText = TString());
    int QueryCount();

    enum class EConfigType {
        Factors,
    };

    template <EConfigType>
    TString WriteConfigFile(const NJson::TJsonValue& config) {
        static THolder<TTempFileHandle> tempFile;
        tempFile.Reset(new TTempFileHandle());
        TFixedBufferFileOutput buf(*tempFile.Get());
        NJson::WriteJson(&buf, &config);
        return tempFile->Name();
    }

    void AddProperty(NRTYServer::TMessage& message, const TString& propName, const TString& value);

    template <class T>
    void AddProperty(NRTYServer::TMessage& message, const TString& propName, const T& value) {
        AddProperty(message, propName, ToString(value));
    }

    void AddSearchProperty(NRTYServer::TMessage& message, const TString& propName, const TString& value);
    void AddGroupAttr(NRTYServer::TMessage& message, const TString& propName, const ui64 value);

    TString Query(const TString& query);
    int CheckMessage(const TString& text, i64 keyPreffix, bool useKeyPreffix = true);
    ui32 GetMaxDocuments();
    void SetIndexerParams(TIndexerType indexer, ui32 maxDocuments, i32 threads = -1, i32 maxConnections = -1);
    ui32 GetShardsNumber() const;
    ui32 GetMergerMaxSegments() const;
    bool IsMergerTimeCheck();
    void SetMergerParams(bool enabled, i32 maxSegments = -1, i32 threads = -1, TMergerCheckPolicyType policy = mcpTIME, i32 checkIntervalMillisec = -1, i32 maxDocs = -1);
    void SetSearcherParams(TAutoBool useCache = abAUTO, const TString& cacheLifeTime = "auto", const TString& rearrange = "auto", i32 threads = -1, TAutoBool reask = abAUTO);
    void SetEnabledRepair(bool value = true);
    void SetEnabledDiskSearch(bool clearRtDir = true, const TString& path = "");
    void SetMorphologyParams(const TString& languages = "auto", TAutoBool noMorphology = abAUTO);
    void SetPruneAttrSort(const TString& attr);
    bool CheckIndexSize(ui64 docsCount, TIndexerType indexerType);
    bool CheckIndexSize(ui64 docsCount, TIndexerType indexerType, ui32 attempts);
    TString GetIndexDir();
    TString GetRunDir();
    TString GetAllKps(const TVector<NRTYServer::TMessage>& messages, const TString& paramPrefix = "&kps=");

    void SetPrefixed(bool value) override {
        IsPrefixed = value;
    }
    bool NeedStartBackend() override {
        return true;
    }
    static void SetTagsInternal(TTags* tags, ...);
    inline static bool HasTags() {
        return false;
    }
    template<class T>
    inline static bool HasTags(T, ...) {
        return true;
    }

public:
    TRTYServerTestCase();
    bool Finish() override;
    bool Prepare() override;
    bool InitConfig() override;
    bool ApplyConfig() override;
    void SetController(IBackendController* controller) override;
    void SetCallback(ICallback* callback) override;
    void SetNavSourceFileName(const TString& value) override;
    void SetFactorsFileName(const TString& value) override;
    void SetGenerateIndexedDoc(bool value) override;
    void SetGetSaveResponses(bool value) override;
    void SetRunNDolbs(ui16 dolbs) override;
    bool ConfigureRuns(TTestVariants& variants, bool cluster) override;
    virtual TString GetClusterConfig();
    NJson::TJsonValue GetInfoRequest() const;
    bool GetIsPrefixed() const;

    void WaitIndexersClose();

    TVector<NRTYServer::TReply> IndexMessages(const TVector<NRTYServer::TMessage>& messages, TRTYServerTestCase::TIndexerType indexerType, TIndexerClient::TContext context);

    TVector<NRTYServer::TReply> IndexMessages(const TVector<NRTYServer::TMessage>& messages,
        TIndexerType indexConfig,
        int copiesNumber,
        ui64 waitResponseMilliseconds = 0,
        bool doWaitIndexing = true,
        bool doWaitReply = true,
        const TDuration& interByteTimeout = TDuration(),
        const TDuration& interMessageTimeout = TDuration(),
        size_t countThreads = 1,
        const TString& service = "tests",
        int packSendMark = 0,
        bool ignoreErrors = false,
        TString cgiRequest = "",
        TIndexerClient::ICallback* callback = nullptr,
        const TString& protocolOverride = ""
        );

    void SetNoSearch(bool value) override {
        NoSearch = value;
    }

    ui16 QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, TQuerySearchContext& context);
    void SetSendIndexReply(bool value) override;
    TString GetResourcesDirectory() const;
    void SetResourcesDirectory(const TString& resDir) override;
    void SetRootDirectory(const TString& rootDir) override;
    static TTags& SetTags(TTags& tags);
    virtual TTags GetTags();
    void CheckConfig() override;
public:
    struct TSearchMessagesContext {
        TSet<std::pair<ui64, TString> > Deleted;
        TSet<size_t> CountResults;
        bool ByText = false;
        size_t CountThreads = 1;
        TString ServiceName = "tests";

        static TSearchMessagesContext BuildDefault(const TString& serviceName, ui32 count = 1) {
            TSearchMessagesContext result = BuildDefault(count);
            result.ServiceName = serviceName;
            return result;
        }

        static TSearchMessagesContext BuildDefault(ui32 count = 1) {
            TSearchMessagesContext result;
            result.CountResults.insert(count);
            return result;
        }
    };

    void CheckSearchResults(const TVector<NRTYServer::TMessage>& messages, const TSet<std::pair<ui64, TString> > deleted = TSet<std::pair<ui64, TString> >(), size_t countResults = 1, size_t countResultsVariant = -1, bool byText = false, size_t countThreads = 1);

    void CheckSearchResults(const TVector<NRTYServer::TMessage>& messages, const TSearchMessagesContext& context);
    bool CheckSearchResultsSafe(const TVector<NRTYServer::TMessage>& messages, const TSearchMessagesContext& context) {
        try {
            CheckSearchResults(messages, context);
            return true;
        } catch (...) {
            ERROR_LOG << CurrentExceptionMessage() << Endl;
            return false;
        }
    }

    static size_t GetSearchableDocsCount(TBackendProxy* controller) {
        TJsonPtr info = controller->GetServerInfo();
        NJson::TJsonValue::TArray jsonArr;
        if (!info->GetArray(&jsonArr)) {
            ythrow yexception() << "some errors in server_info format" << info->GetStringRobust() << Endl;
        }
        NJson::TJsonValue countSearchable = jsonArr[0]["searchable_docs"];
        if (!countSearchable.IsInteger()) {
            ythrow yexception() << "there is no countSearchable: " << info->GetStringRobust() << Endl;
        }
        return countSearchable.GetInteger();
    }

    static TString GetMemoryIndexRps(TBackendProxy* controller) {
        TJsonPtr info = controller->GetServerInfo();
        NJson::TJsonValue::TArray jsonArr;
        if (!info->GetArray(&jsonArr)) {
            ythrow yexception() << "some errors in server_info format" << info->GetStringRobust() << Endl;
        }
        NJson::TJsonValue rps = jsonArr[0]["memory_index_rps"];
        return rps.GetStringRobust();
    }

};

class IConfigChecker {
public:
    virtual ~IConfigChecker() {};
    virtual void Check(TRTYServerTestCase& testCase) = 0;
};
typedef NObjectFactory::TObjectFactory<IConfigChecker, TCiString> TConfigCheckerFactory;


#define SERVICE_TEST_RTYSERVER_DEFINE_PARENT(classname, parent_class_name, ...) \
class classname: public parent_class_name { \
public: \
    static TTags& SetTags(ITestCase::TTags& tags) { \
        parent_class_name::SetTags(tags); \
        if (HasTags(__VA_ARGS__)) \
            SetTagsInternal(&tags, ## __VA_ARGS__, NULL); \
        return tags;\
    } \
    TTags GetTags() override { \
        TTags tags; \
        return SetTags(tags); \
    } \
protected:

#define SERVICE_TEST_RTYSERVER_DEFINE(classname, ...) \
    SERVICE_TEST_RTYSERVER_DEFINE_PARENT(classname, TRTYServerTestCase, ## __VA_ARGS__)

#define START_TEST_DEFINE_PARENT(name, parent_class_name, ...) \
class T ## name ## CaseClass; \
    static TRTYServerTestsFactory::TRegistrator<T ## name ## CaseClass> Registrator ## name(#name); \
    SERVICE_TEST_RTYSERVER_DEFINE_PARENT(T ## name ## CaseClass, parent_class_name, ## __VA_ARGS__)\
public: \
    TString Name() override { return #name; }

#define START_TEST_DEFINE(name, ...) \
    START_TEST_DEFINE_PARENT(name, TRTYServerTestCase, ## __VA_ARGS__)


enum ERandMode {
    R_SIMPLE,
    R_RANDOM
};

TString GetRandomWord(size_t length = 10, ui64 wordCount = 0, ERandMode mode = R_SIMPLE);
