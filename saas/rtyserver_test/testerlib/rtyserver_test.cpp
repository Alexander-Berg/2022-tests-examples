#include "globals.h"
#include "rtyserver_test.h"
#include "test_indexer_client.h"
#include "indexed_doc_generator.h"
#include "standart_generator.h"

#include <saas/library/socket_adapter/socket_adapter_factory.h>
#include <saas/rtyserver/common/sharding.h>
#include <saas/util/queue.h>
#include <saas/util/system/pmap.h>
#include <saas/util/logging/exception_process.h>

#include <search/idl/meta.pb.h>

#include <library/cpp/charset/ci_string.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/packedtypes/packedfloat.h>

#include <util/folder/filelist.h>
#include <library/cpp/cgiparam/cgiparam.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/thread/pool.h>
#include <util/system/fs.h>
#include <util/random/random.h>
#include <util/charset/wide.h>
#include <util/folder/dirut.h>

using namespace NMetaProtocol;
using namespace NRTYServer;

const int SLEEP_WAIT_SEARCH = 5;

const char* TTestMarksPool::Merger = "MERGER";
const char* TTestMarksPool::NoCoverage = "NOCOVERAGE";
const char* TTestMarksPool::Slow = "SLOW";
const char* TTestMarksPool::StaticData = "STATICDATA";
const char* TTestMarksPool::Repair = "REPAIR";
const char* TTestMarksPool::OneBackendOnly = "ONEBACKEND";
const char* TTestMarksPool::NeedRabbit = "RABBITMQ";

TRTYServerTestCase::TRTYServerTestCase()
    : IsResDirSet(false)
    , SendIndexReply(true)
    , Controller(nullptr)
    , Callback(nullptr)
    , ConfigDiff(new TConfigFields)
    , SPConfigDiff(new TConfigFields)
    , IPConfigDiff(new TConfigFields)
    , CProxyConfigDiff(new TConfigFields)
    , GenerateIndexedDoc(false)
    , GetSaveResponses(false)
{
    NoSearch = false;
}

void TRTYServerTestCase::WaitIndexersClose() {
    ui64 closingIndexers = Max<ui64>();
    while (closingIndexers) {
        closingIndexers = 0;
        TJsonPtr serverInfo(Controller->GetServerInfo());
        CHECK_WITH_LOG(serverInfo->IsArray());
        const NJson::TJsonValue::TArray& arr = serverInfo->GetArray();
        for (ui32 i = 0; i < arr.size(); ++i) {
            const NJson::TJsonValue& info = arr[i];
            closingIndexers += info["closing_indexers"].GetUInteger();
        }
        if (closingIndexers) {
            Sleep(TDuration::Seconds(1));
        }
    }
}

bool TRTYServerTestCase::Finish() {
    RTY_MEM_LOG("TRTYServerTestCase::Finish");
    TVector<TString> files = NUtil::GetDescriptorsInUsage(getpid());
    bool hasRemovedOpenFiles = false;
    for (ui32 i = 0; i < files.size(); ++i) {
        DEBUG_LOG << "Used fd: " << files[i] << Endl;
        if (files[i].find_last_of("_merge/mergetmp-") != TString::npos) {
            continue;
        }
        hasRemovedOpenFiles |= ((files[i].find("(deleted)") != TString::npos) && (files[i].find("suggest_tmp_sort_") == TString::npos));
    }

    bool hasDeletedMappedFiles = false;
    const auto& maps = NUtil::GetMappedRegions(getpid());
    DEBUG_LOG << "Mapped files:" << Endl;
    ui32 auxInt = 0;
    for (const auto& mappedregion : maps) {
        DEBUG_LOG << mappedregion.Ptr << " " << mappedregion.File << " size: " << mappedregion.Size << (mappedregion.IsDeleted ? " deleted " : "") << Endl;
        hasDeletedMappedFiles |= mappedregion.IsDeleted && !mappedregion.File.EndsWith(".so") && !TryFromString<ui32>(mappedregion.File, auxInt);
    }
    if (hasDeletedMappedFiles)
        ythrow yexception() << "Deleted mapped files found";

    for (auto&& i : Cluster->GetBackends().GetNodes()) {
        CHECK_WITH_LOG(!!i.second);
        CHECK_WITH_LOG(!!i.second->Agent);
        TString command = "?command=get_info_server";
        TString report;
        TInstant start = Now();
        NJson::TJsonValue json;
        while (Now() - start < TDuration::Seconds(150)) {
            i.second->Agent->ExecuteCommand(command, report, 3000, 2, "");
            TStringStream ss(report);
            CHECK_TEST_TRUE(NJson::ReadJsonTree(&ss, &json));
            CHECK_TEST_TRUE(json.GetMap().at("result").Has("active_contexts"));
            CHECK_TEST_TRUE(json.GetMap().at("result").Has("active_repliers"));
            if (!json.GetMap().at("result").GetMap().at("active_contexts").GetUInteger() && !json.GetMap().at("result").GetMap().at("active_repliers").GetUInteger())
                break;
            sleep(1);
        }
        CHECK_TEST_EQ(json.GetMap().at("result").GetMap().at("active_contexts").GetUInteger(), 0);
        CHECK_TEST_EQ(json.GetMap().at("result").GetMap().at("active_repliers").GetUInteger(), 0);
    }

    return !hasDeletedMappedFiles && !hasRemovedOpenFiles;
}

bool TRTYServerTestCase::Prepare() {
    return true;
}

bool TRTYServerTestCase::InitConfig() {
    return true;
}

bool TRTYServerTestCase::ApplyConfig() {
    (*ConfigDiff)["Searcher.Enabled"] = !NoSearch;
    if (!ConfigDiff->empty())
        Controller->ApplyConfigDiff(ConfigDiff);
    if (!SPConfigDiff->empty() && Controller->GetConfig().HasSearchproxy)
        Controller->ApplyProxyConfigDiff(SPConfigDiff, "search", "SearchProxy");
    if (!IPConfigDiff->empty() && Controller->GetConfig().HasIndexerproxy)
        Controller->ApplyProxyConfigDiff(IPConfigDiff, "indexer", "Proxy");
    if (!CProxyConfigDiff->empty() && Controller->GetConfig().HasCommonProxy)
        Controller->ApplyProxyConfigDiff(CProxyConfigDiff, "common", "Proxy");
    return true;
}

void TRTYServerTestCase::SetController(IBackendController* controller) {
    Controller = dynamic_cast<TBackendProxy*>(controller);
    CHECK_WITH_LOG(Controller);
    GlobalOptions().SetBackendProxy(Controller);
}

void TRTYServerTestCase::SetCallback(ICallback* callback) {
    Callback = callback;
}

void TRTYServerTestCase::SetGenerateIndexedDoc(bool value) {
    GenerateIndexedDoc = value;
}

void TRTYServerTestCase::SetGetSaveResponses(bool value) {
    GetSaveResponses = value;
}

void TRTYServerTestCase::SetRunNDolbs(ui16 dolbs){
    RunNDolbs = dolbs;
}

bool TRTYServerTestCase::ConfigureRuns(TTestVariants& variants, bool cluster) {
    auto env = TTestConfigsCreator(Name()).CreateDefault();

    if (cluster) {
        TString conf = GetClusterConfig();
        if (conf) {
            env.ClusterConfig = "cluster/" + conf;
            variants.push_back(env);
        }
    } else {
        variants.push_back(env);
    }

    return true;
}

TString TRTYServerTestCase::GetClusterConfig() {
    return "cluster_nodm_1be.cfg";
}

bool TRTYServerTestCase::CheckExistsByText(const TString& text, bool isStrict, TVector<NRTYServer::TMessage>& messages, const TString& firstText) {
    TSet<int> prefixes;
    typedef TMap<TString, TString> TUrls;
    TUrls urls;
    for (unsigned i = 0; i < messages.size(); i++) {
        const TMessage& message(messages[i]);
        prefixes.insert(message.GetDocument().GetKeyPrefix());
        urls.insert(TUrls::value_type(message.GetDocument().GetUrl(), message.GetDocument().GetBody()));
    }

    TUrls urlsOriginal = urls;
    int attemption = 1;
    INFO_LOG << "Find results:" << Endl;
    do {
        if (attemption > 1)
            sleep(5);
        urls = urlsOriginal;
        for (TSet<int>::const_iterator i = prefixes.begin(), e = prefixes.end(); i != e; ++i) {
            TVector<TDocSearchInfo> results;
            TString query = text;
            if (isStrict)
                query = "\""+query+"\"";
            if (*i)
                query += "&kps=" + ToString(*i);
            QuerySearch(query, results);
            for (TVector<TDocSearchInfo>::const_iterator url = results.begin(), url_e = results.end(); url != url_e; ++url) {
                TUrls::iterator urlIt = urls.find((*url).GetUrl());
                if (urlIt == urls.end()) {
                    ERROR_LOG << "Some docs in search result are duplicates" << Endl;
                    return false;
                }
                if (!firstText.empty() && url == results.begin() && urlIt->second != firstText) {
                    ERROR_LOG << "Wrong first message" << Endl;
                    return false;
                }
                INFO_LOG << urlIt->first << " " << urlIt->second << Endl;
                urls.erase(urlIt);
            }
        }
        attemption++;
    } while (urls.size() && (attemption < 5));
    if (urls.size()) {
        ERROR_LOG << "Some messages were not found" << Endl;
        return false;
    }
    return true;

}

TString TRTYServerTestCase::Query(const TString& query) {
    TString result;
    if(200 != Controller->ProcessQuery(query, &result))
        ythrow yexception() << "Incorrect query on search server is switched off";
    return result;
}

int TRTYServerTestCase::QueryCount() {
    TJsonPtr info = Controller->GetServerInfo();
    if (!info)
        ythrow yexception() << "cannot get server info";
    const NJson::TJsonValue::TArray& infos = info->GetArray();
    int count = -1;
    for (NJson::TJsonValue::TArray::const_iterator i = infos.begin(); i != infos.end(); ++i) {
        const int curCount = i->operator[]("docs_in_final_indexes").GetInteger() + i->operator[]("docs_in_memory_indexes").GetInteger();
        if (count >= 0 && count != curCount)
            ythrow yexception() << "counts of documents is different on backends: " << curCount << " != " << count << Endl;
        else
            count = curCount;
    }
    return count;
}

bool TRTYServerTestCase::CheckIndexSize(ui64 docsCount, TIndexerType indexerType, ui32 attempts) {
    for (ui32 att = 1; att <= attempts; ++att) {
        if (CheckIndexSize(docsCount, indexerType))
            return true;
        else {
            Sleep(TDuration::Seconds(1));
        }
    }
    return false;
};

bool TRTYServerTestCase::CheckIndexSize(ui64 docsCount, TIndexerType indexerType) {
    PrintInfoServer();
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue* info = &(*serverInfo)[0];
    ui64 docsFinal = (*info)["docs_in_final_indexes"].GetUInteger();
    ui64 docsMemory = (*info)["docs_in_memory_indexes"].GetUInteger();
    if (indexerType == DISK) {
        if (docsCount != docsFinal)
            DEBUG_LOG << "final_index: " << docsFinal << " != " << docsCount << ", memory_index: " << docsMemory << Endl;
        return docsCount == docsFinal;
    }
    if (indexerType == REALTIME) {
        if (docsCount != docsMemory)
            DEBUG_LOG << "memory_index: " << docsMemory << " != " << docsCount << ", final_index: " << docsFinal << Endl;
        return docsCount == docsMemory;
    }
    if (indexerType == ALL) {
        if (docsCount != docsMemory + docsFinal)
            DEBUG_LOG << "sum_docs: " << docsMemory << "(memory)+" << docsFinal << "(disk) != " << docsCount << Endl;
        return docsCount == docsMemory + docsFinal;
    }
    FAIL_LOG("Incorrect indexer type");
    return false;
}

ui16 TRTYServerTestCase::QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, TQuerySearchContext& context) {
    ui16 result = 0;
    TCgiParameters params;
    params.Scan("&text=" + query);
    if (params.Get("numdoc") == "")
        params.InsertUnescaped("numdoc", context.ResultCountRequirement == -1 ? "100" : ToString(context.ResultCountRequirement + 1));

    TString text = params.Get("text");
    params.ReplaceUnescaped("text", text);

    TString qLocal = params.Print();
    if (context.AttemptionsCount == 1) {
        return Controller->QuerySearch("&" + qLocal, results, GetIsPrefixed(), context);
    }

    for (ui32 i = 1; i <= context.AttemptionsCount; ++i) {
        try {
            INFO_LOG << "Attemption for searching " << query << ": " << i << Endl;
            result = Controller->QuerySearch("&" + qLocal, results, GetIsPrefixed(), context);
            if (context.ResultCountRequirement != -1 && results.ysize() == context.ResultCountRequirement)
                return result;
            if (context.ResultCountRequirement == -1 && results.ysize())
                return result;
        } catch (...) {
            ERROR_LOG << query << " processing exception: " << CurrentExceptionMessage() << Endl;
            if (i == context.AttemptionsCount) {
                PrintInfoServer();
                throw;
            }
        }
        Sleep(TDuration::Seconds(1));
    }
    PrintInfoServer();
    return result;
}

ui16 TRTYServerTestCase::QuerySearch(const TString& query, TVector<TDocSearchInfo>& results, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >* docProperties, THashMultiMap<TString, TString>* searchProperties, bool printResult, const TString& host, ui16 port, ISearchSourceSelector::TPtr sourceSelector, const THashMap<TString, TString>* headers) {
    TQuerySearchContext context;
    context.SearchProperties = searchProperties;
    context.DocProperties = docProperties;
    context.PrintResult = printResult;
    context.Host = host;
    context.Port = port;
    context.SourceSelector = sourceSelector;
    if (!!headers)
        context.HttpHeaders = *headers;
    return QuerySearch(query, results, context);
}

ui16 TRTYServerTestCase::QuerySearchNonEncode(const TString& query, TVector<TDocSearchInfo>& results, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >* docProperties, THashMultiMap<TString, TString>* searchProperties) {
    TString qLocal = "text=" + query + "&numdoc=100";
    return Controller->QuerySearch("&" + qLocal, results, GetIsPrefixed(), docProperties, searchProperties);
}

ui32 TRTYServerTestCase::ProcessQuery(const TString& query, TString* result) {
    return Controller->ProcessQuery(query, result);
}

TAtomic word = 0;

TString GetSimpleRandomWord(size_t length, ui64 wordCount) {
    ui64 thisWord;
    thisWord = AtomicIncrement(word);
    if (wordCount != 0)
        thisWord = thisWord % wordCount;
    TString result;
    for (size_t i = 0; i < length; ++i) {
        char c = 'a' + (thisWord % 10);
        result.append(c);
        thisWord /= 10;
    }
    return result;
}

TString GetHonestRandomWord(size_t length) {
    TString result;
    for (size_t i = 0; i < length; i++) {
        result.push_back(static_cast<char>(RandomNumber<ui8>(25)) + 'a');
    }

    return result;
}

TString GetRandomWord(size_t length, ui64 wordCount, ERandMode mode) {
    if (mode == ERandMode::R_SIMPLE) {
        return GetSimpleRandomWord(length, wordCount);
    }
    return GetHonestRandomWord(length);
}

void TRTYServerTestCase::GenerateInput(TVector<NRTYServer::TMessage>& messages, size_t num, IMessageGenerator& mGen) const {
    for (size_t i = 0; i < num; i++) {
        messages.push_back(TMessage());
        mGen.BuildMessage(i, messages.back());
    }
}

void TRTYServerTestCase::ResetGenerationShift() {
    IMessageGenerator::ResetMessageId();
}

void TRTYServerTestCase::GenerateInput(TVector<TMessage>& messages, size_t num, TMessage::TMessageType messageType, bool isPrefixed, const TAttrMap& attrMap, const TString& bodyText, const bool useAttrMap, const TAttrMap& searchAttr) const {
    if (useAttrMap && attrMap.size() && num > attrMap.size()) {
        ythrow yexception() << "Wrong use of attrMap param";
    }

    const ui64 attrShift = 10000000000LL;

    bool randomBody = bodyText == "random";
    for (size_t i = 0; i < num; i++) {
        i64 messId = IMessageGenerator::CreateMessageId();
        messages.push_back(TMessage());
        messages.back().SetMessageType(messageType);
        if (SendIndexReply)
            messages.back().SetMessageId(messId);
        TMessage::TDocument& doc = *messages.back().MutableDocument();
        {
            doc.SetUrl(WideToUTF8(UTF8ToWide("http://ПаНаМар.cOm/" + ToString(messId + 123456789) + "_" + ToString(messId % 3))));
            if (isPrefixed)
                doc.SetKeyPrefix(TStandartDocumentGenerator::BASE_KEY_PREFIX + messId);
        }
        doc.SetMimeType("text/html");
        doc.SetCharset("UTF8");
        doc.SetLanguage("rus");
        doc.SetBody((randomBody ? GetRandomWord() : bodyText).data());
        if (randomBody)
            DEBUG_LOG << "GenInfo: Url = " << doc.GetUrl() << ", Body = " << doc.GetBody() << ", Kps = " << doc.GetKeyPrefix() << Endl;

        {
            ::NRTYServer::TMessage::TDocument::TProperty& attr = *doc.AddAdditionalKeys();
            attr.set_name("key1");
            attr.set_value("value1");
        }

        {
            ::NRTYServer::TMessage::TDocument::TProperty& attr = *doc.AddAdditionalKeys();
            attr.set_name("key1");
            attr.set_value("value3");
        }

        {
            ::NRTYServer::TMessage::TDocument::TProperty& attr = *doc.AddAdditionalKeys();
            attr.set_name("key2");
            attr.set_value("value2");
        }

        {
            ::NRTYServer::TMessage::TDocument::TProperty& attr = *doc.AddDocumentProperties();
            attr.set_name("geoPolyline");
            attr.set_value("37.4 45;");
        }

        TAttribute& attr = *doc.AddSearchAttributes();
        attr.set_name("test");
        attr.set_value("test@test");
        attr.set_type(TAttribute::LITERAL_ATTRIBUTE);
        if (i < searchAttr.size()) {
            for (TAttrMap::value_type::const_iterator it = searchAttr[i].begin(), e = searchAttr[i].end(); it != e; ++it) {
                TAttribute& att = *doc.AddSearchAttributes();
                att.set_name(it->first);
                att.set_type(it->second.Type);
                att.set_value(it->second.Value);
            }
        }
        NRTYServer::TMessage::TSentenceData& sent = *doc.MutableAnnData()->AddSentences();
        sent.SetText("Annotations test");
        sent.SetTextLanguage(2);
        NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
        NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
        ui8 data = Float2Frac<ui8>(0.0196078);
        NSaas::AddSimpleStream("DT_CORRECTED_CTR", TString((char*)&data, 1), *stream);
        data = Float2Frac<ui8>(0.05);
        NSaas::AddSimpleStream("DT_ONE_CLICK", TString((char*)&data, 1), *stream);
        if (!useAttrMap)
            continue;
        if (attrMap.empty()) {
            TAttribute& attr = *doc.AddGroupAttributes();
            attr.set_name("mid");
            attr.set_value(ToString(attrShift + i).data());
            attr.set_type(TAttribute::INTEGER_ATTRIBUTE);

            TAttribute& attrLiteral = *doc.AddGroupAttributes();
            attrLiteral.set_name("union");
            attrLiteral.set_value("string" + ToString(attrShift + i));
            attrLiteral.set_type(TAttribute::LITERAL_ATTRIBUTE);
        } else {
            for (TAttrMap::value_type::const_iterator iter = attrMap[i].begin(),
                end = attrMap[i].end(); iter != end; ++iter) {
                TAttribute& attr = *doc.AddGroupAttributes();
                attr.set_name(iter->first);
                attr.set_value(iter->second.Value);
                attr.set_type(iter->second.Type);
            }
        }
    }
}

TString Cut(const TString& s) {
    if (s.size() < 100) return s;
    return TString(s.data(), 50) + "..." + TString(s.data() + s.size() - 50, 49);
}


TVector<NRTYServer::TReply> TRTYServerTestCase::IndexMessages(const TVector<NRTYServer::TMessage>& messages, TRTYServerTestCase::TIndexerType indexerType, TIndexerClient::TContext context) {
    return IndexMessages(messages, indexerType, context.CopiesNumber, context.WaitResponseMilliseconds,
        context.DoWaitIndexing,
        context.DoWaitReply,
        context.InterByteTimeout,
        context.InterMessageTimeout,
        context.CountThreads,
        context.Service,
        context.PackSendMark,
        context.IgnoreErrors,
        context.CgiRequest,
        context.Callback,
        context.ProtocolOverride
        );
}

TVector<NRTYServer::TReply> TRTYServerTestCase::IndexMessages(const TVector<TMessage>& messages,
                   TIndexerType indexer,
                   int copiesNumber,
                   ui64 waitResponseMilliseconds,
                   bool doWaitIndexing,
                   bool doWaitReply,
                   const TDuration& interByteTimeout,
                   const TDuration& interMessageTimeout,
                   size_t countThreads,
                   const TString& service,
                   int packSendMark,
                   bool ignoreErrors,
                   TString cgiRequest,
                   TIndexerClient::ICallback* callback,
                   const TString& protocolOverride
                   ) {
    const TBackendProxyConfig::TIndexer* indexConfig;
    indexConfig = &Controller->GetConfig().Indexer;
    TVector<TMessage> copy = messages;
    for (ui32 i = 0; i < copy.size(); ++i) {
        copy[i].MutableDocument()->SetRealtime(indexer != DISK);
        if (!copy[i].GetDocument().HasModificationTimestamp())
            copy[i].MutableDocument()->SetModificationTimestamp(Seconds());
    }
    if (GenerateIndexedDoc) {
        TIndexedDocGenerator generator(*Controller);
        for (TVector<TMessage>::iterator i = copy.begin(); i != copy.end(); ++i)
            generator.ProcessDoc(*i);
    }

    bool packSend = indexConfig->PackSend;
    if (packSendMark > 0)
        packSend = true;
    else if (packSendMark < 0)
        packSend = false;

    TString protocol = protocolOverride.empty() ? indexConfig->Protocol : protocolOverride;
    TIndexerClient client(indexConfig->Host
                        , indexConfig->Port
                        , TDuration::MilliSeconds(waitResponseMilliseconds)
                        , interByteTimeout
                        , protocol
                        , service
                        , packSend
                        , ignoreErrors
                        , cgiRequest, callback);
    for (int i = 0; i < copiesNumber; ++i)
        client.Run(copy, countThreads, interMessageTimeout, doWaitReply);
    if (doWaitIndexing) {
        for (ui32 i = 0; i < 5; ++i) {
            Controller->WaitActiveServer();
            Controller->WaitEmptyIndexingQueues();
            WaitIndexersClose();
            Sleep(TDuration::MilliSeconds(10));
        }
    }
    return client.GetReplies();
}

class TSearchResultCheckJob : public IObjectInQueue {
public:
    TSearchResultCheckJob(const TMessage& message, const TRTYServerTestCase::TSearchMessagesContext context, bool& success, TBackendProxy& controller, bool prefixedIndex)
        : Message(message)
        , Context(context)
        , AnyCount(Context.CountResults.empty())
        , Success(success)
        , Controller(controller)
        , PrefixedIndex(prefixedIndex)
    {}

    void Process(void* /*ThreadSpecificResource*/) override {
        THolder<TSearchResultCheckJob> suicide(this);
        TVector<TDocSearchInfo> results;
        TString url(Message.GetDocument().GetUrl());
        TString text(Message.GetDocument().GetBody());
        ui64 kps(Message.GetDocument().GetKeyPrefix());
        TString query = Context.ByText ? "\"" + text + "\"" : "url:\"" + url + "\"";
        CGIEscape(query);
        query = "&text=" + query + "&numdoc=100&service=" + Context.ServiceName;
        if (Message.GetDocument().GetKeyPrefix()) {
            TStringOutput ss(query);
            ss << "&kps=" << kps;
        }
        TRY
            if (Context.Deleted.find(std::make_pair(kps, url)) == Context.Deleted.end()) {
                for (int iter = 0; iter < 20; iter++) {
                    if (!Success)
                        return;
                    try {
                        Controller.QuerySearch(query, results, PrefixedIndex, nullptr, nullptr);
                    } catch (...) {

                    }
                    if (Context.CountResults.find(results.size()) != Context.CountResults.end())
                        return;
                    if (AnyCount && results.size())
                        return;
                    if (iter != 20-1)
                        sleep(SLEEP_WAIT_SEARCH);
                }
                DEBUG_LOG << "Checking presence of document " << url << Endl;
                if (AnyCount && !results.size())
                    ythrow yexception() << "Error. Documents not found: " << query << Endl;
                if (!AnyCount && Context.CountResults.find(results.size()) == Context.CountResults.end()) {
                    TString CountResultsVariants;
                    for (TSet<size_t>::const_iterator variant = Context.CountResults.begin(); variant != Context.CountResults.end(); ++variant)
                        CountResultsVariants += ToString(*variant) + ", ";
                    TString LogInfo = "";
                    for (int i = 0; i < results.ysize(); i++) {
                        LogInfo += results[i].GetUrl() + "---";
                    }
                    ERROR_LOG << "query=" << query << ";result=" << LogInfo << Endl;
                    ythrow yexception() << "Error. Documents count is incorrect: " << results.size() << "(result) must be in [" << CountResultsVariants << "], query = " << query << Endl;
                }
            } else {
                for (int iter = 0; iter < 20; iter++) {
                    if (!Success)
                        return;
                    try {
                        Controller.QuerySearch(query, results, PrefixedIndex, nullptr, nullptr);
                    } catch (...) {

                    }
                    if (!results.size())
                        return;
                    if (iter != 20-1)
                        sleep(SLEEP_WAIT_SEARCH);
                }
                DEBUG_LOG << "Checking absence of document " << url << Endl;
                for (unsigned i = 0; i < results.size(); i++)
                    DEBUG_LOG << "Error. Unexpected search result: " << results[i].GetUrl() << Endl;
                if (results.size() > 0)
                    ythrow yexception() << "Error. Found unexpected results for query " << query << Endl;
            }
            return;
        CATCH("CheckSearchResults fail");
        Success = false;
    }
private:
    const TMessage& Message;
    TRTYServerTestCase::TSearchMessagesContext Context;
    bool AnyCount;
    bool& Success;
    TBackendProxy& Controller;
    bool PrefixedIndex;
};

bool TRTYServerTestCase::CheckGroupsTry(TString query, int countGroups, int countDocs, int countGroupings, int countTry) {
    TString searchResult;
    for (int i = 0; i < countTry; ++i) {
        if (200 != ProcessQuery(query, &searchResult))
            return false;
        try {
            CheckGroups(searchResult, countGroups, countDocs, countGroupings);
            return true;
        }
        catch (...) {
            continue;
        }
    }
    return false;
}

bool TRTYServerTestCase::CheckGroups(TString searchResult, int countGroups, int countDocs, int countGroupings) {
    NMetaProtocol::TReport report;
    Y_PROTOBUF_SUPPRESS_NODISCARD report.ParseFromString(searchResult);

    DEBUG_LOG << report.DebugString() << Endl;

    if (!countGroupings && !report.GetGrouping().size())
        return true;
    if (report.GetGrouping().size() == countGroupings) {
        const NMetaProtocol::TGrouping& grouping(report.GetGrouping(0));
        if (grouping.GetGroup().size() != countGroups)
            ythrow yexception() << "Incorrect groups count: " << countGroups << " != " << grouping.GetGroup().size();
        for (int i = 0; i < grouping.GetGroup().size(); i++) {
            if (grouping.GetGroup(i).GetDocument().size() != countDocs) {
                ythrow yexception() << "Incorrect documents count in group: " << countDocs << " != " << grouping.GetGroup(i).GetDocument().size();
            }
        }
    } else {
        ythrow yexception() << "report.GetGrouping().size() != " << countGroupings << " !!";
    }
    return true;
}

void TRTYServerTestCase::CheckSearchResults(const TVector<NRTYServer::TMessage>& messages, const TRTYServerTestCase::TSearchMessagesContext& context) {
    bool success = true;
    TRTYMtpQueue queue;
    bool prefixedIndex = GetIsPrefixed();
    queue.Start(context.CountThreads);
    for (TVector<TMessage>::const_iterator i = messages.begin(), e = messages.end(); success && (i != e); ++i)
        queue.SafeAdd(new TSearchResultCheckJob(*i, context, success, *Controller, prefixedIndex));
    queue.Stop();
    if (!success && !NoSearch) {
        PrintInfoServer();
        ythrow yexception() << "Incorrect search results";
    }
}

void TRTYServerTestCase::CheckSearchResults(const TVector<TMessage>& messages, const TSet<std::pair<ui64, TString> > deleted, size_t countResults, size_t countResultsVariant, bool byText, size_t countThreads) {
    TSearchMessagesContext context;
    if ((int)countResults >= 0) {
        context.CountResults.insert(countResults);
        if ((int)countResultsVariant >= 0)
            context.CountResults.insert(countResultsVariant);
    }
    context.Deleted = deleted;
    context.ByText = byText;
    context.CountThreads = countThreads;
    CheckSearchResults(messages, context);
}

void TRTYServerTestCase::PrintInfoServer() const {
    INFO_LOG << WriteJson(GetInfoRequest()) << Endl;
}

void TRTYServerTestCase::CheckCount(int correctCount) {
    for (ui32 i = 0; i < 5; ++i) {
        if (correctCount != QueryCount()) {
            if (i == 4)
                ythrow yexception() << "Error. Incorrect documents count " << correctCount << " != " << QueryCount() << Endl;
        } else {
            return;
        }
        sleep(1);
    }
}

void TRTYServerTestCase::SetResourcesDirectory(const TString& resDir) {
    SafeResolveDir(resDir.data(), ResourcesDir);
    IsResDirSet = true;
}

void TRTYServerTestCase::SetRootDirectory(const TString& rootDir) {
    RootDir = rootDir;
}

TString TRTYServerTestCase::GetResourcesDirectory() const {
    VERIFY_WITH_LOG(IsResDirSet, "SetResourcesDirectory wasn't used");
    return ResourcesDir;
}

NRTYServer::TReply TRTYServerTestCase::DeleteSpecial() {
    TMessage message;
    message.SetMessageType(TMessage::DELETE_DOCUMENT);
    if (SendIndexReply)
        message.SetMessageId(IMessageGenerator::CreateMessageId());
    TMessage::TDocument* doc = message.MutableDocument();
    doc->SetUrl("*");
    doc->SetBody("query_del:$remove_all$");
    DEBUG_LOG << "Deleting query: " << "$remove_all$" << Endl;
    TVector<NRTYServer::TReply> results = IndexMessages(TVector<TMessage>(1, message), REALTIME, 1);
    if (SendIndexReply) {
        if (results[0].GetStatus() == NRTYServer::TReply::DATA_ACCEPTED && !GlobalOptions().GetUsingDistributor())
            return DeleteSpecial();
        else
            return results[0];
    }
    else
        return NRTYServer::TReply();
}

NRTYServer::TReply TRTYServerTestCase::DeleteSpecial(ui64 kps) {
    TMessage message;
    message.SetMessageType(TMessage::DELETE_DOCUMENT);
    if (SendIndexReply)
        message.SetMessageId(IMessageGenerator::CreateMessageId());
    TMessage::TDocument* doc = message.MutableDocument();
    doc->SetUrl("*");
    doc->SetBody("query_del:$remove_kps$");
    doc->SetKeyPrefix(kps);
    DEBUG_LOG << "Deleting query: " << "$remove_kps$" << Endl;
    TVector<NRTYServer::TReply> results = IndexMessages(TVector<TMessage>(1, message), REALTIME, 1);
    if (SendIndexReply) {
        if (results[0].GetStatus() == NRTYServer::TReply::DATA_ACCEPTED && !GlobalOptions().GetUsingDistributor())
            return DeleteSpecial(kps);
        else
            return results[0];
    }
    else
        return NRTYServer::TReply();
}

NRTYServer::TReply TRTYServerTestCase::DeleteQueryResult(const TString& query, TIndexerType indexer) {
    for (ui32 attCounter = 0; ; ++attCounter) {
        TMessage message;
        message.SetMessageType(TMessage::DELETE_DOCUMENT);
        if (SendIndexReply)
            message.SetMessageId(IMessageGenerator::CreateMessageId());
        TMessage::TDocument* doc = message.MutableDocument();
        doc->SetUrl("*");
        doc->SetBody("query_del:" + query);
        doc->SetModificationTimestamp(Seconds());
        DEBUG_LOG << "Deleting query(" << attCounter << "): " << query << Endl;
        TVector<NRTYServer::TReply> results = IndexMessages(TVector<TMessage>(1, message), indexer, 1);
        if (SendIndexReply) {
            if (results[0].GetStatus() == NRTYServer::TReply::DATA_ACCEPTED && !GlobalOptions().GetUsingDistributor())
                continue;
            else
                return results[0];
        } else
            return NRTYServer::TReply();
    }
}

TMessage TRTYServerTestCase::BuildDeleteMessage(const TMessage& messOriginal) {
    VERIFY_WITH_LOG(messOriginal.HasDocument(), "Incorrect BuildDeleteMessage usage for non-content message");
    VERIFY_WITH_LOG(
        messOriginal.GetMessageType() == TMessage::ADD_DOCUMENT || messOriginal.GetMessageType() == TMessage::MODIFY_DOCUMENT,
        "Incorrect BuildDeleteMessage usage for non-content-type message");
    TMessage messageDel;
    messageDel.MutableDocument()->SetUrl(messOriginal.GetDocument().GetUrl());
    if (messOriginal.GetDocument().HasKeyPrefix())
        messageDel.MutableDocument()->SetKeyPrefix(messOriginal.GetDocument().GetKeyPrefix());
    if (SendIndexReply)
        messageDel.SetMessageId(IMessageGenerator::CreateMessageId());
    messageDel.SetMessageType(TMessage::DELETE_DOCUMENT);
    return messageDel;
}

void TRTYServerTestCase::DeleteSomeMessages(const TVector<TMessage>& messages, TSet<std::pair<ui64, TString> >& deleted, TIndexerType indexer, size_t step) {
    TVector<TMessage> delMessages;
    for (unsigned i = 0; i < messages.size(); i += step) {
        delMessages.push_back(BuildDeleteMessage(messages[i]));
        TString url(delMessages.back().GetDocument().GetUrl());
        deleted.insert(std::make_pair(delMessages.back().GetDocument().GetKeyPrefix(), delMessages.back().GetDocument().GetUrl()));
        DEBUG_LOG << "Deleting document: " << url << Endl;
    }
    IndexMessages(delMessages, indexer, 1);
}

void TRTYServerTestCase::ReopenIndexers(const TString serviceName) {
    TVector<TMessage> messages(1);
    messages.back().SetMessageType(NRTYServer::TMessage::REOPEN_INDEXERS);
    messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
    messages.back().MutableDocument()->SetUrl("reopen");
    messages.back().MutableDocument()->SetModificationTimestamp(Seconds());
    while (true) {
        TVector<NRTYServer::TReply> results = IndexMessages(messages, DISK, 1, 0, true, true, TDuration(), TDuration(), 1, serviceName);
        if (SendIndexReply) {
            if (GlobalOptions().GetUsingDistributor())
                break;
            if (results.empty() || (results[0].GetStatus() == NRTYServer::TReply::DATA_ACCEPTED) || (results[0].GetStatus() == NRTYServer::TReply::NOTNOW))
                Sleep(TDuration::Seconds(1));
            else
                break;
        } else {
            break;
        }
    }
}

TVector<NRTYServer::TReply> TRTYServerTestCase::IndexMessageAsIs(const TVector<TMessage>& messages, bool strictOrder){
    const TBackendProxyConfig::TIndexer& indexer = Controller->GetConfig().Indexer;
    TSocket socket(TNetworkAddress(indexer.Host, indexer.Port));
    TIndexerClient client(indexer.Host
        , indexer.Port
        , TDuration()
        , TDuration()
        , indexer.Protocol
        , "tests"
        , /*packSend=*/!strictOrder ? indexer.PackSend : false
        , /*ignoreErrors=*/false
        , "");
    client.Run(messages, 1, TDuration(), /*waitForReply=*/true);
    return client.GetReplies();
}

void TRTYServerTestCase::SwitchDefaultKps(ui64 newKps) {
    TVector<TMessage> messages(1);
    messages.back().SetMessageType(NRTYServer::TMessage::SWITCH_PREFIX);
    messages.back().MutableDocument()->SetKeyPrefix(newKps);
    messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
    messages.back().MutableDocument()->SetUrl("-");
    messages.back().MutableDocument()->SetModificationTimestamp(Seconds());
    IndexMessageAsIs(messages);
}

NJson::TJsonValue TRTYServerTestCase::GetInfoRequest() const {
    return (*Controller->GetServerInfo());
}

namespace {
    bool CopyDir(const TString& source, const TString& destination) {
        if (!NFs::Exists(destination))
            return false;
        TFileEntitiesList fl(TFileEntitiesList::EM_FILES_DIRS);
        fl.Fill(source, TStringBuf(), TStringBuf(), 100);
        while (const char * filename = fl.Next()) {
            if (IsDir(source + "/" + TString(filename))) {
                MakeDirIfNotExist((destination + "/" + TString(filename)).c_str());
            } else {
                NFs::Copy(source + "/" + filename, destination + "/" + filename);
            }
        }
        return true;
    }
}

void TRTYServerTestCase::PrepareData(const TString& source, const TString& destination) {
    DEBUG_LOG << "Preparing for test. Copying files... " << GetResourcesDirectory() << "/" << source << Endl;
    if (!CopyDir(GetResourcesDirectory() + "/" + source, destination.empty() ? GetIndexDir() : destination))
        ythrow yexception() << "incorrect resource directory - " << GetResourcesDirectory() << "/" << source << "not found";
    DEBUG_LOG << "Preparing for test. Copying files... " << GetResourcesDirectory() << "/" << source << " ... OK" << Endl;
}

int TRTYServerTestCase::CheckMessage(const TString& text, i64 keyPreffix, bool useKeyPreffix) {
    TVector<TDocSearchInfo> results;
    TString query = text + (useKeyPreffix ? "&kps=" + ToString(keyPreffix) : "");
    INFO_LOG << "TestWILDCARD test text: \"" << query << "\"" << Endl;
    QuerySearch(query, results);
    if (!results.size())
    {
        ERROR_LOG << "TestWILDCARD fail on text: \"" << text << "\"" << Endl;
        return 1;
    }
    return 0;
}

void TRTYServerTestCase::SetSendIndexReply(bool value) {
    SendIndexReply = value;
}

void TRTYServerTestCase::SetFactorsFileName(const TString& value) {
    FactorsFileName = value;
}

void TRTYServerTestCase::SetNavSourceFileName(const TString& value) {
    NavSourceFileName = value;
}

ui32 TRTYServerTestCase::GetMaxDocuments() {
    return Controller->GetConfigValue<ui32>("indexer.disk.MaxDocuments");
}

void TRTYServerTestCase::SetIndexerParams(TIndexerType indexer, ui32 maxDocuments, i32 threads, i32 maxConnections) {
    TVector<TCiString> indexers;
    switch(indexer) {
        case DISK:
            indexers.push_back("indexer.disk");
            break;
        case REALTIME:
            indexers.push_back("indexer.memory");
            break;
        case ALL:
            indexers.push_back("indexer.disk");
            indexers.push_back("indexer.memory");
            break;
        default:
            ythrow yexception() << "Invalid usage TRTYServerTestCase::SetIndexerParams";
    }
    for (TVector<TCiString>::const_iterator i = indexers.begin(); i != indexers.end(); ++i) {
        if (threads >= 0)
            (*ConfigDiff)[*i + ".Threads"] = threads;
        if (maxConnections >= 0)
            (*ConfigDiff)[*i + ".HttpOptions.MaxConnections"] = maxConnections;
        (*ConfigDiff)[*i + ".MaxDocuments"] = maxDocuments;
    }
}

bool TRTYServerTestCase::GetIsPrefixed() const {
    return IsPrefixed;
}

ui32 TRTYServerTestCase::GetShardsNumber() const {
    const TString val = Controller->GetConfigValue("ShardsNumber");
    const size_t pos = val.find('(');
    return FromString<ui32>(val.substr(0, pos));
}

ui32 TRTYServerTestCase::GetMergerMaxSegments() const {
    return Controller->GetConfigValue<ui32>("Merger.MaxSegments");
}

bool TRTYServerTestCase::IsMergerTimeCheck() {
    return Controller->GetConfigValue("Merger.MergerCheckPolicy") == "TIME";
}

void TRTYServerTestCase::SetMergerParams(bool enabled, i32 maxSegments, i32 threads, TMergerCheckPolicyType policy, i32 checkIntervalMillisec, i32 maxDocs) {
    (*ConfigDiff)["Merger.Enabled"] = enabled;
    if (maxSegments >= 0)
        (*ConfigDiff)["Merger.MaxSegments"] = maxSegments;
    if (threads >= 0)
        (*ConfigDiff)["Merger.Threads"] = threads;
    switch(policy) {
    case mcpTIME:
        (*ConfigDiff)["Merger.MergerCheckPolicy"] = "TIME";
        break;
    case mcpNEWINDEX:
        (*ConfigDiff)["Merger.MergerCheckPolicy"] = "NEWINDEX";
        break;
    case mcpCONTINUOUS:
        (*ConfigDiff)["Merger.MergerCheckPolicy"] = "CONTINUOUS";
        break;
    case mcpNONE:
        (*ConfigDiff)["Merger.MergerCheckPolicy"] = "NONE";
        break;
    }
    if (checkIntervalMillisec >= 0)
        (*ConfigDiff)["Merger.TimingCheckIntervalMilliseconds"] = checkIntervalMillisec;
    if (maxDocs >= 0)
        (*ConfigDiff)["Merger.MaxDocumentsToMerge"] = maxDocs;
}

void TRTYServerTestCase::SetSearcherParams(TAutoBool useCache, const TString& cacheLifeTime, const TString& rearrange, i32 threads, TAutoBool reask) {
    if (useCache != abAUTO) {
        if (useCache == abTRUE) {
            (*ConfigDiff)["Searcher.QueryCache.UseCache"] = true;
            (*ConfigDiff)["Searcher.QueryCache.MemoryLimit"] = 200000000;
            }
        else
            (*ConfigDiff)["Searcher.QueryCache"] = "__remove__";
    }
    if (cacheLifeTime != "auto" && useCache != abFALSE)
        (*ConfigDiff)["Searcher.QueryCache.CacheLifeTime"] = cacheLifeTime;
    if (rearrange != "auto")
        (*ConfigDiff)["Searcher.ReArrangeOptions"] = rearrange;
    if (threads >= 0)
        (*ConfigDiff)["Searcher.HttpOptions.Threads"] = threads;
    if (reask != abAUTO)
        (*ConfigDiff)["Searcher.ReAskBaseSearches"] = reask  == abTRUE;
}

void TRTYServerTestCase::SetEnabledRepair(bool value) {
    if (!GlobalOptions().GetUsingDistributor())
        (*ConfigDiff)["Repair.Enabled"] = value;
}

void TRTYServerTestCase::SetEnabledDiskSearch(bool clearRtDir, const TString& path) {
    TFsPath sod(path ? path : TString("rty_search"));
    if (!sod.IsAbsolute()) {
        sod = TFsPath(GetIndexDir()).Parent() / sod;
    }
    sod.Fix();

    if (clearRtDir) {
        // check for safe removal (we do not want to mistakenly remove something useful)
        TVector<TFsPath> children;
        if (sod.Exists())
            sod.List(children);
        for (const TFsPath &child : children) {
            auto &&name = child.GetName();
            if (name.StartsWith("index_") || name.StartsWith("temp_") || name.StartsWith("merge"))
                continue;
            ythrow yexception() << "cannot safely clear SearchObjectDirectory: unexpected item " + child.GetPath();
        }

        sod.ForceDelete();
        sod.MkDirs();
    }

    (*ConfigDiff)["Indexer.Disk.SearchEnabled"] = "true";
    (*ConfigDiff)["Indexer.Disk.SearchObjectsDirectory"] = sod;
}

void TRTYServerTestCase::SetMorphologyParams(const TString& languages, TAutoBool noMorphology) {
    TConfigFieldsPtr proxyDiff = new TConfigFields();

    if (languages != "auto"){
        (*ConfigDiff)["MorphologyLanguages"] = languages;
        (*SPConfigDiff)["Service.MetaSearch.MorphologyLanguages"] = languages;
    }
    if (noMorphology != abAUTO){
        (*ConfigDiff)["NoMorphology"] = noMorphology == abTRUE;
        if (noMorphology == abTRUE)
            (*SPConfigDiff)["Service.MetaSearch.MorphologyLanguages"] = "non";
    }
}

void TRTYServerTestCase::SetPruneAttrSort(const TString& attr) {
    (*ConfigDiff)["PruneAttrSort"] = attr;
}

TString TRTYServerTestCase::GetRunDir() {
    return RootDir;
}

TString TRTYServerTestCase::GetIndexDir() {
    return Controller->GetConfigValue("IndexDir");
}

void TRTYServerTestCase::AddProperty(NRTYServer::TMessage& message, const TString& propName, const TString& value) {
    auto* newProp = message.MutableDocument()->AddDocumentProperties();
    newProp->SetName(propName);
    newProp->SetValue(value);
}

void TRTYServerTestCase::AddSearchProperty(NRTYServer::TMessage& message, const TString& propName, const TString& value) {
    auto* newProp = message.MutableDocument()->AddSearchAttributes();
    newProp->SetName(propName);
    newProp->SetType(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
    newProp->SetValue(value);
}

void TRTYServerTestCase::AddGroupAttr(NRTYServer::TMessage& message, const TString& propName, const ui64 value) {
    auto* newProp = message.MutableDocument()->AddGroupAttributes();
    newProp->SetName(propName);
    newProp->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    newProp->SetValue(ToString(value));
}

TString TRTYServerTestCase::GetAllKps(const TVector<NRTYServer::TMessage>& messages, const TString& paramPrefix) {
    if (!GetIsPrefixed())
        return TString();
    TSet<i64> prefixes;
    for (size_t i = 0; i < messages.size(); ++i)
        prefixes.insert(messages[i].GetDocument().GetKeyPrefix());
    if (prefixes.empty())
        return TString();

    TSet<i64>::const_iterator i = prefixes.begin();
    TStringStream kps;
    kps << paramPrefix << *i;
    for (++i;i != prefixes.end();++i)
        kps << "," << *i;
    return kps.Str();
}

void TRTYServerTestCase::SetTagsInternal(TTags* tags, ...) {
    va_list args;
    va_start(args, tags);
    try {
    while (const char* tag = va_arg(args, const char*))
        tags->insert(tag);
    } catch (...) {
        FATAL_LOG << "Cannot initialize TestCase tags" << Endl;
    }
    va_end(args);
}

void TRTYServerTestCase::CheckMergerResult() {
    Controller->ProcessCommand("do_all_merger_tasks");
    for (auto&& i : Controller->GetActiveBackends()) {
        TBackendProxy::TBackendSet bs;
        bs.insert(i);
        TString indexDir = Controller->GetConfigValue("IndexDir", "server", bs);
        TVector<TFsPath> children;
        TFsPath path(indexDir);
        path.List(children);
        TMap<int, unsigned> countOnShard;
        for (const TFsPath& dir : children) {
            if (!dir.IsDirectory())
                continue;
            if (!dir.GetName().StartsWith("index_"))
                continue;
            if (Controller->IsFinalIndex(dir)) {
                size_t shard = NRTYServer::GetShard(TFsPath(dir).Basename());
                countOnShard[shard]++;
                if (countOnShard[shard] > GetMergerMaxSegments())
                    ythrow yexception() << "incorrect number of indexes after merging: " << countOnShard[shard] << " != " << GetMergerMaxSegments();
            }
        }
    }
}

ITestCase::TTags& TRTYServerTestCase::SetTags(TTags& tags) {
    return ITestCase::SetTags(tags);
}

ITestCase::TTags TRTYServerTestCase::GetTags() {
    TTags tags;
    return SetTags(tags);
}

void TRTYServerTestCase::CheckConfig() {
    TTags tags(GetTags());
    for(TTags::const_iterator i = tags.begin(); i != tags.end(); ++i) {
        THolder<IConfigChecker> checker(TConfigCheckerFactory::Construct(*i));
        if (!!checker)
            checker->Check(*this);
    }
}
