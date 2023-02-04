#include "oxy.h"

#include <saas/api/yt_pull_client/saas_yt_writer.h>

#include <saas/library/histogram/histogram.h>

#include <saas/rtyserver_test/common/mr_opts.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/kiwi_export/export.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/rtyserver/common/common_messages.h>

#include <saas/util/json/json.h>

#include <search/fresh/factors/factors_gen/factors_gen.h>

#include <kernel/web_factors_info/factors_gen.h>

#include <google/protobuf/text_format.h>

#include <util/system/event.h>

START_TEST_DEFINE(TestExportRefresh)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    ExportFromDump(GetResourcesDirectory() + "/kiwi_test/refresh.calctext",
                   Controller->GetConfig().Export.Port);

    TQuerySearchContext context;
    context.ResultCountRequirement = 428;
    context.AttemptionsCount = 42;
    context.PrintResult = true;

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off", results, context);
    CHECK_TEST_EQ(results.size(), 428);

    ReopenIndexers();

    QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off", results, context);
    CHECK_TEST_EQ(results.size(), 428);

    Controller->ProcessCommand("stop");

    QuerySearch("url:\"*\"&numdoc=500&rearr=AntiDup_off", results, context);
    CHECK_TEST_EQ(results.size(), 428);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestRefreshSwitchPruning, TestOxygenDocs)
bool InitConfig() override {
    return true;
}
bool Run() override {

    if (GetIsPrefixed())
        return true;

    GenerateDocs("kiwi_test/refresh.docs", NRTYServer::TMessage::MODIFY_DOCUMENT, 20, false, true);
    const TVector<NRTYServer::TMessage> messages1(Messages.begin(), Messages.begin() + 10);
    const TVector<NRTYServer::TMessage> messages2(Messages.begin() + 11, Messages.end());
    IndexMessages(messages1, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 5;

    context.ResultCountRequirement = 10;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 10);

    const TFsPath oldOxygenOptionsFile(Controller->GetConfigValue("Indexer.Common.OxygenOptionsFile", "server", TBackendProxy::TBackendSet(0)));

    // Switch to intermediate, reindex documents
    Controller->ProcessCommand("stop");
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = oldOxygenOptionsFile.Parent() / "RefreshOxygenOptions-Intermediate.cfg";
    ApplyConfig();
    Controller->ProcessCommand("restart");

    IndexMessages(messages1, REALTIME, 1);
    ReopenIndexers();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    // Switch to final
    Controller->ProcessCommand("stop");
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = oldOxygenOptionsFile.Parent() / "RefreshOxygenOptions-LegacyPruning.cfg";
    ApplyConfig();
    Controller->ProcessCommand("restart");

    IndexMessages(messages2, REALTIME, 1);
    ReopenIndexers();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    context.ResultCountRequirement = 19;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 19);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestRefreshHistogram, TestOxygenDocs)
bool InitConfig() override {
    return true;
}
bool Run() override {
    if (GetIsPrefixed())
        return true;

    GenerateDocs("kiwi_test/refresh.docs", NRTYServer::TMessage::MODIFY_DOCUMENT, 20, false, true);
    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 5;
    context.ResultCountRequirement = 20;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 20);

    auto getHistrogramResult = Controller->ProcessCommand("get_histogram", 0);
    NRTYServer::THistograms histograms;
    histograms.Deserialize((*getHistrogramResult)[0]["histogram"]);
    CHECK_TEST_EQ(histograms.GetAttribute("AddTimeFull").Get(1427975400), 2);
    CHECK_TEST_EQ(histograms.GetAttribute("AddTimeFull").Get(1427975500), 1);

    auto infoHistogram = Query("/?info=histogram");
    NRTYServer::THistograms histograms2;
    histograms2.Deserialize(NUtil::JsonFromString(infoHistogram));
    CHECK_TEST_EQ(histograms2.GetAttribute("AddTimeFull").Get(1427975400), 2);
    CHECK_TEST_EQ(histograms2.GetAttribute("AddTimeFull").Get(1427975500), 1);

    return true;
}
};

START_TEST_DEFINE(TestRefreshMergeStatic)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=500", results);
    CHECK_TEST_NEQ(results.size(), 0);

    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(RefreshUpdateBase)
ui32 MessageId = 1;

class TSlowUpdateCrash: public IMessageProcessor {
public:
    TSlowUpdateCrash() {
        RegisterGlobalMessageProcessor(this);
    }
    ~TSlowUpdateCrash() {
        UnregisterGlobalMessageProcessor(this);
    }
    TString Name() const override {
        return "SlowUpdateCrash";
    }
    bool Process(IMessage* message) override {
        if (auto m = message->As<TMessageSlowUpdateInvoked>()) {
            FAIL_LOG("Slow update invoked for %s", m->DocSearchInfo.GetUrl().data());
        }
        return false;
    }
};
void IndexFromTextProtobufFile(const TString& path, TIndexerType indexerType = REALTIME) {
    TIFStream inputStream(path);
    const TString& data = inputStream.ReadAll();

    NRTYServer::TMessage message;
    if (!google::protobuf::TextFormat::ParseFromString(data, &message)) {
        ythrow yexception() << "cannot parse protobuf file " << path;
    }
    message.SetMessageId(MessageId++);

    DEBUG_LOG << "Indexing message from " << path << Endl;
    IndexMessages({ message }, indexerType, 1);
    DEBUG_LOG << "Indexing message from " << path << " OK" << Endl;
}
};

START_TEST_DEFINE_PARENT(TestRefreshUpdate, RefreshUpdateBase)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.ResultCountRequirement = 1;
    context.AttemptionsCount = 20;
    context.PrintResult = true;

    IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/refresh_updatetest_add_message.0");
    ReopenIndexers();
    QuerySearch("url:\"*\"&dbgrlv=da&fsgta=_JsonFactors&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 1);

    TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(results[0].GetFullDocId());
    DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
    TDocInfo diBefore(*jsonDocInfoPtr);

    IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/refresh_updatetest_update_message.0");
    QuerySearch("url:\"*\"&dbgrlv=da&fsgta=_JsonFactors&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 1);

    jsonDocInfoPtr = Controller->GetDocInfo(results[0].GetFullDocId());
    DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
    TDocInfo diAfter(*jsonDocInfoPtr);

    CHECK_TEST_EQ(diBefore.GetDDKDocInfo()["Version"], diAfter.GetDDKDocInfo()["Version"]);
    CHECK_TEST_EQ(diBefore.GetDDKDocInfo()["Timestamp"], diAfter.GetDDKDocInfo()["Timestamp"]);

    IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/refresh_updatetest_delete_message.0");
    context.ResultCountRequirement = 0;
    QuerySearch("url:\"*\"&dbgrlv=da&fsgta=_JsonFactors&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 0);

    IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/refresh_updatetest_update_message.0");
    context.ResultCountRequirement = 0;
    QuerySearch("url:\"*\"&dbgrlv=da&fsgta=_JsonFactors&nocache=da", results, context);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestRefreshUpdateFastDetection, RefreshUpdateBase)
bool Run() override {
    IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/refresh_updatetest_add_message.0");
    MUST_BE_BROKEN(IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/refresh_updatetest_slowupdate_FreshErfInfo_message.0"));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestRefreshMemorySearchFactorDiff, TestOxygenDocs)
struct TDoc {
    THashMap<TString, double> Factors;
    i64 Position = -1;
};

using TDocs = TMap<TString, TDoc>;

TDocs ProcessDocs(const TVector<TDocSearchInfo>& results, const TQuerySearchContext::TDocProperties& properties) {
    TDocs docs;
    auto factors = TRTYFactorsParser::GetJsonFactorsValues(properties);
    for (size_t i = 0; i < results.size(); ++i) {
        auto& d = docs[results[i].GetUrl()];
        d.Position = i;
        d.Factors = factors[i];
    }

    return docs;
}

bool CheckQueries(const TVector<TString>& texts) {
    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    TQuerySearchContext::TDocProperties properties;
    context.DocProperties = &properties;
    context.PrintResult = true;

    TVector<TDocs> memoryResults;
    for (auto&& text : texts) {
        QuerySearch(text + "&g=1.d:fresh.500.500&ag=d:fresh&relev=fml=fresh&nocache=da&rearr=AntiDup_off&rearr=Fusion_off&fsgta=_JsonFactors&dbgrlv=da", results, context);
        memoryResults.push_back(ProcessDocs(results, properties));
    }

    ReopenIndexers();

    TVector<TDocs> diskResults;
    for (auto&& text : texts) {
        QuerySearch(text + "&g=1.d:fresh.500.500&ag=d:fresh&relev=fml=fresh&nocache=da&rearr=AntiDup_off&rearr=Fusion_off&fsgta=_JsonFactors&dbgrlv=da", results, context);
        diskResults.push_back(ProcessDocs(results, properties));
    }

    bool failed = false;
    THashMap<TString, double> factorDiff;
    for (size_t i = 0; i < diskResults.size(); ++i) {
        auto& diskDocs = diskResults[i];
        auto& memoryDocs = memoryResults[i];
        for (auto&& d : diskDocs) {
            const TString& url = d.first;
            const i64 pos = d.second.Position;
            const auto& factors = d.second.Factors;

            if (!memoryDocs.contains(url)) {
                ERROR_LOG << url << " not found in memory search" << Endl;
                failed = true;
                continue;
            }
            if (memoryDocs[url].Position != pos) {
                WARNING_LOG << url << " position differs: memory " << memoryDocs[url].Position << ", disk " << pos << Endl;
            }
            for (auto&& f : factors) {
                const TString& name = f.first;
                const double value = f.second;
                if (!memoryDocs[url].Factors.contains(name)) {
                    ERROR_LOG << url << " hasn't got factor " << name << Endl;
                    failed = true;
                }
                if (Abs(memoryDocs[url].Factors[name] - value) > 0.01f) {
                    ERROR_LOG << url << " factor " << name << " has different values mem=" << memoryDocs[url].Factors[name] << ", disk=" << value << Endl;
                    const double diff = Abs(memoryDocs[url].Factors[name] - value) / Max(memoryDocs[url].Factors[name], value);
                    factorDiff[name] = Max(factorDiff[name], diff);
                    failed = true;
                }
            }
        }
    }

    ERROR_LOG << "Factor diff: " << factorDiff.size() << " factors" << Endl;
    for (auto&& f : factorDiff) {
        ERROR_LOG << f.first << "=" << f.second << Endl;
    }

    return failed;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;

    GenerateDocs("kiwi_test/refresh.docs", NRTYServer::TMessage::MODIFY_DOCUMENT, 428, false, true);
    IndexMessages(Messages, REALTIME, 1);

    {
        TQuerySearchContext context;
        TQuerySearchContext::TDocProperties properties;
        context.DocProperties = &properties;
        context.ResultCountRequirement = 428;
        context.AttemptionsCount = 42;
        context.PrintResult = false;

        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off", results, context);
        CHECK_TEST_EQ(results.size(), 428);
    }

    const TVector<TString> queries = {
        "music",
        "news today",
        "������� �������",
        "new yandex",
        "�������� � ���������",
        "��� ��� ��������",
        "������ �����",
        "Affordable printer",
        "����� � ����������"
    };

    return !CheckQueries(queries);
}
bool InitConfig() override {
    return true;
}
};

START_TEST_DEFINE_PARENT(TestRefreshPantherSearcher, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    GenerateDocs("kiwi_test/refresh.docs", NRTYServer::TMessage::MODIFY_DOCUMENT, 400, false, true);
    IndexMessages(Messages, REALTIME, 1);

    {
        TQuerySearchContext context;
        TQuerySearchContext::TDocProperties properties;
        context.DocProperties = &properties;
        context.ResultCountRequirement = 400;
        context.AttemptionsCount = 42;
        context.PrintResult = false;

        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off", results, context);
        CHECK_TEST_EQ(results.size(), 400);
    }

    const TVector<TString> queries = {
        "music",
        "news today",
        "������� �������",
        "new yandex",
        "�������� � ���������",
        "��� ��� ��������",
        "������ �����",
        "Affordable printer",
        "����� � ����������"
    };

    for (auto&& q : queries) {
        TVector<TDocSearchInfo> results;
        QuerySearch(q + "&pron=termsearch&nocache=da", results);
        Y_ENSURE(!results.empty(), "No results in MemorySearch for query " << q.Quote());
    }

    ReopenIndexers();

    for (auto&& q : queries) {
        TVector<TDocSearchInfo> results;
        QuerySearch(q + "&pron=termsearch&nocache=da", results);
        Y_ENSURE(!results.empty(), "No results in BaseSearch for query " << q.Quote());
    }
    return true;
}
bool InitConfig() override {
    return true;
}
};

START_TEST_DEFINE_PARENT(TestPersQueueSimple, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const ui32 docsCount = 20;
    GenerateDocs("kiwi_test/refresh.docs", NRTYServer::TMessage::MODIFY_DOCUMENT, docsCount, false, true);

    IndexMessages(Messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 180;
    context.ResultCountRequirement = docsCount;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da", results, context);

    CHECK_TEST_EQ(results.size(), docsCount);

    return true;
}
bool InitConfig() override {
    TestOxygenDocs::InitConfig();
    return true;
}
};
