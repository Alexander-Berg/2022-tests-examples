#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/string/vector.h>
#include <util/string/split.h>
#include <saas/rtyserver/components/erf/erf_manager.h>
#include <saas/rtyserver/components/erf/erf_disk.h>
#include <saas/api/factors_erf.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/rtyserver/components/erf/erf_component.h>
#include <saas/rtyserver/components/erf/erf_parsed_entity.h>
#include <search/idl/meta.pb.h>
#include <util/digest/city.h>
#include <util/generic/ymath.h>
#include <util/system/tempfile.h>

START_TEST_DEFINE(TestIncorrectFactors)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1a", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
        }
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        TSet<std::pair<ui64, TString> > deleted;
        MUST_BE_BROKEN(IndexMessages(messages, indexer, 1));
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }

    bool Run() override {
        if (GlobalOptions().GetUsingDistributor())
            return true;
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchFactors)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
        }
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        TSet<std::pair<ui64, TString> > deleted;
        IndexMessages(messages, indexer, 100);
        ReopenIndexers();
        IndexMessages(messages, indexer, 1);
        DeleteSomeMessages(messages, deleted, indexer);
        ReopenIndexers();
        IndexMessages(messages, indexer, 1);
        DeleteSomeMessages(messages, deleted, indexer);
        ReopenIndexers();
        IndexMessages(messages, indexer, 1);
        DeleteSomeMessages(messages, deleted, indexer);
        ReopenIndexers();
        IndexMessages(messages, indexer, 1);
        DeleteSomeMessages(messages, deleted, indexer);
        ReopenIndexers();
        IndexMessages(messages, indexer, 1);
        DeleteSomeMessages(messages, deleted, indexer);
        ReopenIndexers();
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        sleep(10);
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }

    bool Run() override {
        //        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchWithUrlHash)
void CheckRequest(const TString& request, ui32 countDocs) {
    TString searchResult;
    for (ui32 i = 5; i; i--) {
        ui32 code = ProcessQuery("/?ms=proto&text=" + request, &searchResult);
        INFO_LOG << "Check request: " << request << Endl;

        if (code != 200) {
            ythrow yexception() << "Error check " << request << "/" << countDocs;
        }
        NMetaProtocol::TReport report;
        Y_PROTOBUF_SUPPRESS_NODISCARD report.ParseFromString(searchResult);
        ui32 countDocsResult = 0;
        if (report.GroupingSize() != 1) {
            DEBUG_LOG << "grouping fail" << Endl;
            if (i > 1) {
                sleep(5);
                continue;
            }
            ythrow yexception() << "Incorrect grouping size for " << request;
        }

        for (ui32 i = 0; i < report.GetGrouping(0).GroupSize(); ++i) {
            countDocsResult += report.GetGrouping(0).GetGroup(i).DocumentSize();
        }

        if (countDocsResult != countDocs) {
            DEBUG_LOG << "Docs count fail" << Endl;
            if (i > 1) {
                sleep(5);
                continue;
            }
            ythrow yexception() << "Incorrect docs count: " << countDocs << "/" << report.GetGrouping(0).GetNumDocs(0);
        }
        break;
    }
}

public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
        (*ConfigDiff)["Searcher.SkipSameDocids"] = "true";

        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "true";
        (*SPConfigDiff)["Service.MetaSearch.InsensitiveClientNumFetch"] = "true";
        (*SPConfigDiff)["Service.MetaSearch.SwitchToNextSourceFetchStage"] = "true";
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NRTYServer::TAttribute& attr = *messages[i].MutableDocument()->AddGroupAttributes();
            attr.SetName("mid112");
            attr.SetValue(ToString(i + 1));
            attr.set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());

        TVector<TDocSearchInfo> results;
        THashMultiMap<TString, TString> searchProperties;
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());

        IndexMessages(messages, REALTIME, 1);
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 3);
        CheckRequest("body" + kps, 3);

        ReopenIndexers();
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 3);
        CheckRequest("body" + kps, 3);

        IndexMessages(messages, REALTIME, 1);
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 6);
        CheckRequest("body" + kps, 3);

        ReopenIndexers();
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 6);
        CheckRequest("body" + kps, 3);

        CheckRequest("body&rearr=dontskipsamedocids&g=1.mid112.10.10" + kps, 6);
        CheckRequest("body&g=1.mid112.10.10" + kps, 3);

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        CheckRequest("body&rearr=dontskipsamedocids" + kps, 3);
        CheckRequest("body" + kps, 3);

        CheckRequest("body&rearr=dontskipsamedocids&g=1.mid112.10.10" + kps, 3);
        CheckRequest("body&g=1.mid112.10.10" + kps, 3);

        return true;
    }
};

START_TEST_DEFINE(TestSearchWithUrlHashSkipSameDocsOnSP)
void CheckRequest(const TString& request, ui32 countDocs) {
    TString searchResult;
    for (ui32 i = 5; i; i--) {
        ui32 code = ProcessQuery("/?ms=proto&text=" + request, &searchResult);
        INFO_LOG << "Check request: " << request << Endl;

        if (code != 200) {
            ythrow yexception() << "Error check " << request << "/" << countDocs;
        }
        NMetaProtocol::TReport report;
        Y_PROTOBUF_SUPPRESS_NODISCARD report.ParseFromString(searchResult);
        ui32 countDocsResult = 0;
        if (report.GroupingSize() != 1) {
            DEBUG_LOG << "grouping fail" << Endl;
            if (i > 1) {
                sleep(5);
                continue;
            }
            ythrow yexception() << "Incorrect grouping size for " << request;
        }

        for (ui32 i = 0; i < report.GetGrouping(0).GroupSize(); ++i) {
            countDocsResult += report.GetGrouping(0).GetGroup(i).DocumentSize();
        }

        if (countDocsResult != countDocs) {
            DEBUG_LOG << "Docs count fail" << Endl;
            if (i > 1) {
                sleep(5);
                continue;
            }
            ythrow yexception() << "Incorrect docs count: " << countDocs << "/" << report.GetGrouping(0).GetNumDocs(0);
        }
        break;
    }
}

public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";

        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "true";
        (*SPConfigDiff)["Service.MetaSearch.InsensitiveClientNumFetch"] = "true";
        (*SPConfigDiff)["Service.MetaSearch.SwitchToNextSourceFetchStage"] = "true";
        (*SPConfigDiff)["Service.MetaSearch.MergeOptions"] = "SkipSameDocids=true";
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NRTYServer::TAttribute& attr = *messages[i].MutableDocument()->AddGroupAttributes();
            attr.SetName("mid112");
            attr.SetValue(ToString(i + 1));
            attr.set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());

        TVector<TDocSearchInfo> results;
        THashMultiMap<TString, TString> searchProperties;
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());

        IndexMessages(messages, REALTIME, 1);
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 3);
        CheckRequest("body" + kps, 3);

        ReopenIndexers();
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 3);
        CheckRequest("body" + kps, 3);

        IndexMessages(messages, REALTIME, 1);
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 6);
        CheckRequest("body" + kps, 3);

        ReopenIndexers();
        CheckRequest("body&rearr=dontskipsamedocids" + kps, 6);
        CheckRequest("body" + kps, 3);

        CheckRequest("body&rearr=dontskipsamedocids&g=1.mid112.10.10" + kps, 6);
        CheckRequest("body&g=1.mid112.10.10" + kps, 3);

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        CheckRequest("body&rearr=dontskipsamedocids" + kps, 3);
        CheckRequest("body" + kps, 3);

        CheckRequest("body&rearr=dontskipsamedocids&g=1.mid112.10.10" + kps, 3);
        CheckRequest("body&g=1.mid112.10.10" + kps, 3);

        return true;
    }
};

START_TEST_DEFINE(TestMemorySearchUrlHash)
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 600, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
        (*ConfigDiff)["Searcher.SkipSameDocids"] = "true";
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 2;
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1000, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
        }
        TVector<NRTYServer::TMessage> reverseMessages(messages.rbegin(), messages.rend());
        IndexMessages(messages, REALTIME, 1);
        DeleteSpecial();
        IndexMessages(reverseMessages, REALTIME, 1);
        CheckSearchResults(messages);
        return true;
    }
};

START_TEST_DEFINE(TestFilterFactors)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 3, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", ToString(i), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", ToString(i), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", ToString(i), *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        }
        TSet<std::pair<ui64, TString> > deleted;
        IndexMessages(messages, indexer, 100);
        sleep(2);
        TVector<TDocSearchInfo> results;
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
        THashMultiMap<TString, TString> searchProperties;
        QuerySearch(indexerType + "&relev=filter=80010000000V3&relev=filter_border=1" + kps, results, nullptr, &searchProperties);
        if (results.size() != 2)
            ythrow yexception() << "failed case 0 " << results.size();

        QuerySearch(indexerType + "&relev=filter=20010000000V3&relev=filter_border=1" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 1 " << results.size();
        }

        QuerySearch(indexerType + "&relev=filter=20070020000000U70000OV0&relev=filter_border=2" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 1A " << results.size();
        }

        QuerySearch(indexerType + "&relev=formula=20010000000V3&relev=filter_border=1" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 1B " << results.size();
        }

        QuerySearch(indexerType + "&relev=formula=20070020000000U70000OV0&relev=filter_border=2" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 1C " << results.size();
        }

        QuerySearch(indexerType + " <- " + indexerType + "&relev=filter=80010000000V3&relev=filter_border=1&relev=border_keep_refine=1" + kps, results, nullptr, &searchProperties);
        if (results.size() != 3)
            ythrow yexception() << "failed case KeepRefine=1 " << results.size();

        QuerySearch(indexerType + " <- " + indexerType + "&relev=filter=80010000000V3&relev=filter_border=1" + kps, results, nullptr, &searchProperties);
        if (results.size() != 2)
            ythrow yexception() << "failed case KeepRefine=0 " << results.size();

        QuerySearch(indexerType + " <- " + indexerType + "&relev=filter=80010000000V3&relev=filter_border=1&relev=u=" + messages[0].GetDocument().GetUrl() + kps, results, nullptr, &searchProperties);
        if (results.size() != 3)
            ythrow yexception() << "failed case relev=u " << results.size();

        ReopenIndexers();

        QuerySearch(indexerType + "&relev=filter=80010000000V3&relev=filter_border=1" + kps, results, nullptr, &searchProperties);
        if (results.size() != 2)
            ythrow yexception() << "failed case 1.5 " << results.size();

        QuerySearch(indexerType + "&relev=filter=20010000000V3&relev=filter_border=1" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 2 " << results.size();
        }

        QuerySearch(indexerType + "&relev=filter=20070020000000U70000OV0&relev=filter_border=2" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 2A " << results.size();
        }

        QuerySearch(indexerType + "&relev=formula=20010000000V3&relev=filter_border=1" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 2B " << results.size();
        }

        QuerySearch(indexerType + "&relev=formula=20070020000000U70000OV0&relev=filter_border=2" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "failed case 2C " << results.size();
        }
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }

    bool Run() override {
        Test(REALTIME);
        return true;
    }
};

#define CHECK_VALUE(index, value, name) \
    {\
        if (Abs(data[index] - value) > 0.01) {\
            ERROR_LOG << "Incorrect factor " << name << " value: " << data[index] << Endl;\
            return false;\
        }\
        if (edm.GetHeader().GetFactorName(index) != name) {\
            ERROR_LOG << "Incorrect factor name " << name << " for index " << index << Endl;\
            return false;\
        }\
    }


START_TEST_DEFINE(TestSearchFactorsModifyConfig, TTestMarksPool::OneBackendOnly)
protected:
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/change_factors1.cfg";

        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "1.1", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "2.2", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "3.3", *messages[i].MutableDocument()->MutableFactors());
        }
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        TSet<TString> deleted;
        IndexMessages(messages, DISK, 100);

        Controller->ProcessCommand("restart");
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        {
            TSet<TString> indexes = Controller->GetFinalIndexes();

            if (indexes.size() != 1) {
                ERROR_LOG << "Incorrect indexes count after merge (A)" << Endl;
                return false;
            }

            TString Directory = *indexes.begin();

            TAtomicSharedPtr<NRTYFactors::TConfig> conf = new NRTYFactors::TConfig(FactorsFileName.data());
            TRTYStaticFactorsConfig statFactorsConf(conf.Get());
            TRTYErfDiskManager::TCreationContext cc(TPathName{Directory}, "indexerf.rty", &statFactorsConf);
            cc.ReadOnly = true;
            TRTYErfDiskManager edm(cc, ERF_COMPONENT_NAME);
            edm.Open();
            TBasicFactorStorage data(3);
            edm.ReadRaw(data, 0);
            CHECK_VALUE(2, 1.1, "stat1");
            CHECK_VALUE(0, 2.2, "stat2");
            CHECK_VALUE(1, 3.3, "stat3");
        }

        TString newFactorsFileName = GetResourcesDirectory() + "/factors/change_factors2.cfg";
        (*ConfigDiff)["Searcher.FactorsInfo"] = newFactorsFileName;
        ApplyConfig();
        Controller->ProcessCommand("restart");
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        {
            TSet<TString> indexes = Controller->GetFinalIndexes();

            if (indexes.size() != 1) {
                ERROR_LOG << "Incorrect indexes count after merge (B)" << Endl;
                return false;
            }

            TString Directory = *indexes.begin();

            TAtomicSharedPtr<NRTYFactors::TConfig> conf = new NRTYFactors::TConfig(newFactorsFileName.data());
            TRTYStaticFactorsConfig sfc(conf.Get());
            TRTYErfDiskManager::TCreationContext cc(TPathName{Directory}, "indexerf.rty", &sfc);
            cc.ReadOnly = true;
            TRTYErfDiskManager edm(cc, ERF_COMPONENT_NAME);
            edm.Open();
            TBasicFactorStorage data(4);
            edm.ReadRaw(data, 0);
            CHECK_VALUE(0, 2.2, "stat2");
            CHECK_VALUE(1, 0, "stat4");
            CHECK_VALUE(2, 0, "stat5");
            CHECK_VALUE(3, 1.1, "stat1");
        }
        return true;
    }
};

START_TEST_DEFINE(TestSearchFactorsExists)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 3, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
        }
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        TSet<std::pair<ui64, TString> > deleted;
        MUST_BE_BROKEN(IndexMessages(messages, indexer, 100));

        CheckSearchResults(messages, deleted, 0);

        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
        }

        try {
            IndexMessages(messages, indexer, 100);
        } catch (...) {
            ythrow yexception() << "error on indexing!!";
        }

        CheckSearchResults(messages, deleted, 1);

        for (int i = 0; i < messages.ysize(); i++) {
            messages[i].MutableDocument()->clear_factors();
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
        }

        MUST_BE_BROKEN(IndexMessages(messages, indexer, 100));

        CheckSearchResults(messages, deleted, 1);
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }

    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchFactorsFormula)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType + " body");
        messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());

        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", ToString((10 + i) * 1), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", ToString((10 + i) * 2), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", ToString((10 + i) * 3), *messages[i].MutableDocument()->MutableFactors());
        }

        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        QuerySearch(indexerType + "&how=usrf&userhow=formula&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 3)
            ythrow yexception() << "polnaya hren'";
        if (results[0].GetUrl() != messages[2].GetDocument().GetUrl())
            ythrow yexception() << "first document wrong";
        if (results[1].GetUrl() != messages[1].GetDocument().GetUrl())
            ythrow yexception() << "second document wrong";
        if (results[2].GetUrl() != messages[0].GetDocument().GetUrl())
            ythrow yexception() << "third document wrong";
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }

    bool Run() override {
        //        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchFactorsSortBy)
protected:
    TVector<NRTYServer::TMessage> messages;
    void Test(TIndexerType indexer) {
        if (indexer == DISK)
            ReopenIndexers();
        else {
            const TString indexerType(indexer == DISK ? "disk" : "memory");
            GenerateInput(messages, 4, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType + " body");
            messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            messages[2].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            messages[3].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());

            messages[1].MutableDocument()->SetBody("a1a bbb ccc");
            messages[2].MutableDocument()->SetBody("a1a bbb");
            messages[3].MutableDocument()->SetBody("b1b.fff xxx");
            messages[0].MutableDocument()->SetBody("a1a");
            IndexMessages(messages, indexer, 1);
            Sleep(TDuration::Seconds(2));
        }
        TVector<TDocSearchInfo> results;
        QuerySearch("a1a+bbb+softness:49&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 2)
            ythrow yexception() << "Incorrect check softness 50";

        QuerySearch("b1b.fff+jjj+kkk+lll+softness:100&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect check softness 100 Ex A";

        QuerySearch("\"b1b.fff\"+jjj+kkk+lll+softness:100&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect check softness 100 Ex B";

        QuerySearch("\"b1b.fff\"+jjj+softness:100&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect check softness 100 Ex C";

        QuerySearch("\"b1b.fff\"+softness:100&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect check softness 100 Ex D";

        QuerySearch("a1a+bbb+ccc+softness:100&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 3)
            ythrow yexception() << "Incorrect check softness 100";

        QuerySearch("a1a+bbb+ccc+softness:67&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 3)
            ythrow yexception() << "Incorrect check softness 67";
        if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
            ythrow yexception() << "first document wrong (softness 100)";
        if (results[1].GetUrl() != messages[2].GetDocument().GetUrl())
            ythrow yexception() << "second document wrong (softness 100)";
        if (results[2].GetUrl() != messages[0].GetDocument().GetUrl())
            ythrow yexception() << "third document wrong (softness 100)";
        QuerySearch("a1a+bbb+ccc+softness:34&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 2)
            ythrow yexception() << "Incorrect check softness 67";
        if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
            ythrow yexception() << "first document wrong (softness 67)";
        if (results[1].GetUrl() != messages[2].GetDocument().GetUrl())
            ythrow yexception() << "second document wrong (softness 67)";
        QuerySearch("a1a+bbb+ccc+softness:33&pron=sortbyTRDocQuorum&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect check softness 34";
        if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
            ythrow yexception() << "first document wrong (softness 34)";

    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/fast_rank_factors.cfg";
        return true;
    }

    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestFastRankBase)
protected:
    TVector<NRTYServer::TMessage> Messages;

    virtual void Index(TIndexerType indexer) {
        GenerateInput(Messages, 11, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        Messages[0].MutableDocument()->SetBody("two aaa three two");
        Messages[1].MutableDocument()->SetBody("one aaa two");
        for (size_t i = 2; i < Messages.size(); ++i)
            Messages[i].MutableDocument()->SetBody("one aaa");
        if (GetIsPrefixed()) {
            for (size_t i = 1; i < Messages.size(); ++i)
                Messages[i].MutableDocument()->SetKeyPrefix(Messages[0].GetDocument().GetKeyPrefix());
        }
        IndexMessages(Messages, indexer, 1);
    }

    bool Expect(size_t pos, TString factor, bool expectExists, bool expectNonZero,
            const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>& resultProps) {
        DEBUG_LOG << resultProps[pos]->find("_JsonFactors")->second << Endl;
        THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[pos];
        THashMap<TString, double>::const_iterator iFactor = factors.find(factor);
        if (!expectExists) {
            CHECK_TEST_FAILED(iFactor != factors.end(), "Factor should not be present: " + factor);
        } else {
            CHECK_TEST_FAILED(iFactor == factors.end(), "Factor should be present: " + factor);
            if (expectNonZero) {
                CHECK_TEST_FAILED(iFactor->second < 0.1, "Too small factor: " + factor);
            } else {
                CHECK_TEST_FAILED(iFactor->second > 0.001, "Too big factor: " + factor);
            }
        }

        return true;
    }

public:
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/fast_rank_factors.cfg";
        SetIndexerParams(REALTIME, 100, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestFastRank, TestFastRankBase)
bool Run() override {
    Index(REALTIME);

    TString kps;
    if (GetIsPrefixed()) {
        kps = "&kps=" + ToString(Messages[0].GetDocument().GetKeyPrefix());
    }

    TVector<TDocSearchInfo> results;
    QuerySearch("one+two+aaa+softness:100&pron=fastcount2&numdoc=1" + kps, results);
    if (results[0].GetUrl() != Messages[0].GetDocument().GetUrl()) {
        ythrow yexception() << "fastrank works wrong:" << results[0].GetUrl() << " != " << Messages[0].GetDocument().GetUrl();
    }

    // fastrank + filter_border
    QuerySearch("one+two+aaa+softness:100&pron=fastcount2&numdoc=1&relev=filter_border=100" + kps, results);
    CHECK_TEST_EQ(results.size(), 0);
    QuerySearch("one+two+aaa+softness:100&pron=fastcount2&numdoc=5&relev=filter_border=0.00001" + kps, results);
    CHECK_TEST_EQ(results.size(), Min<size_t>(5, Messages.size()));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestFactorMasks, TestFastRankBase)
bool Run() override {
    // This test does not use FastRank. It checks the basic behavior, and the "factors_set" feature
    Index(REALTIME);

    TString kps;
    if (GetIsPrefixed()) {
        kps = "&kps=" + ToString(Messages[0].GetDocument().GetKeyPrefix());
    }
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;

    TString testQuery = "one+aaa+two+softness:100&dbgrlv=da&fsgta=_JsonFactors&numdoc=1";
    const size_t doc = 0;
    TString relev;
 
    // Case 1.1. no extra options
    relev = "&relev=formula=full_rank";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 1.2 no extra options
    relev = "&relev=formula=ff_tr_docquorum";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", false, false, resultProps));

    // Case 2. Add factor by gta
    relev = "&relev=formula=ff_tr_docquorum&gta=Swbm25";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 3. Add factor by factors_set
    relev = "&relev=formula=ff_tr_docquorum&relev=factors=fs_2c";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps)); // this one is added
    relev = "&relev=formula=full_rank&relev=factors=fs_0";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps)); // this one is added
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    return true;
}
};

START_TEST_DEFINE_PARENT(TestFastRankFactorMasks, TestFastRankBase)
public:
bool Run() override {
    Index(REALTIME);

    TString kps;
    if (GetIsPrefixed()) {
        kps = "&kps=" + ToString(Messages[0].GetDocument().GetKeyPrefix());
    }
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;

    TString testQuery = "one+aaa+two+softness:100&relev=fast_formula=ff_tr_docquorum&pron=fastcount2&dbgrlv=da&fsgta=_JsonFactors&numdoc=1";
    TString relev;
    const size_t doc = 0;

    // Case 0. pron=nofastrank (calculate the main formula)
    relev = "&relev=formula=full_rank&pron=nofastrank";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 1. NotJustFastRank
    relev = "&relev=formula=full_rank";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl()); // because the top is by TRDocQuorum
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 2. JustFastRank, Group.BM25 is disabled
    relev = "&relev=formula=full_rank&pron=onlyfastrank";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", false, false, resultProps));

    // Case 3.1 JustFastRank + relev=all_factors
    relev = "&relev=formula=full_rank&pron=onlyfastrank&relev=all_factors";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    // factors not used by the fast formula should be either absent or zeroed. The current impl. makes them absent.
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", false, false, resultProps));

     // Case 3.2. JustFastRank + relev=all_factors=ext
    relev = "&relev=formula=full_rank&pron=onlyfastrank&relev=all_factors=ext";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    // factors not used by the fast formula should be present and non-zero.
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 3.3 JustFastRank + fsgta
    relev = "&relev=formula=full_rank&pron=onlyfastrank&fsgta=TextBM25&fsgta=Swbm25";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 3.3.1 Same, but using a gta instead of fsgta
    relev = "&relev=formula=full_rank&pron=onlyfastrank&gta=TextBM25&gta=Swbm25";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", false, false, resultProps));

    // Case 3.3.2 Same, but using a factor_set instead of fsgta
    relev = "&relev=formula=full_rank&pron=onlyfastrank&fsgta=TextBM25&relev=factors=fs_2a"; // fs_2a contains Swbm25
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", true, true, resultProps));

    // Case 3.4 JustFastRank with special simulate_not_just_fast_rank to get fast rank factors as if FullRank was enabled + fsgta
    relev = "&relev=formula=full_rank&pron=onlyfastrank&fsgta=TextBM25&fsgta=Swbm25&relev=simulate_not_just_fast_rank";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", false, false, resultProps));
 
    // Case 3.4.1 Same, but using a factor_set instead of fsgta
    relev = "&relev=formula=full_rank&pron=onlyfastrank&fsgta=TextBM25&relev=factors=fs_2a&relev=simulate_not_just_fast_rank";
    QuerySearch(testQuery + relev + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[doc].GetUrl(), Messages[1].GetDocument().GetUrl());
    CHECK_TEST_TRUE(Expect(doc, "TRDocQuorum", true, true, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "TextBM25", false, false, resultProps));
    CHECK_TEST_TRUE(Expect(doc, "Swbm25", false, false, resultProps));
    return true;
}
};

START_TEST_DEFINE(TestFastRankNoSortingByRelev)
    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "static_factors":{
                "search_area": 1
            },
            "user_factors":{
                "hit_area": 2
            },
            "rty_dynamic_factors": {
                "TestDpFactor": 0
            },
            "formulas":{
                "default":{
                    "polynom":"100100KPC6JR3"
                },
                "fast_rank": {
                    "polynom": "100100KPC6JR3"
                }
            }
        })";
        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        return true;
    }

    bool CheckFactorExistance(const TString& query, const TVector<TString>& factorNames) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps, nullptr, true);

        const TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
        CHECK_TEST_TRUE(!factors.empty());

        for (ui32 nDoc = 0; nDoc < factors.size(); ++nDoc) {
            const auto& docFactors = factors[nDoc];
            for (const auto& factorName : factorNames) {
                const auto i = docFactors.find(factorName);
                CHECK_TEST_TRUE(i != docFactors.end());
                CHECK_TEST_TRUE(i->second != 0.0f);
            }
        }
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        {
            GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
            auto* document = messages[0].MutableDocument();
            document->SetUrl("33e778cd-da03-4dd7-a262-966ac733895c");
            NSaas::AddSimpleFactor("search_area", "5", *document->MutableFactors());

            IndexMessages(messages, REALTIME, 1);
        }

        const TString kps = GetAllKps(messages);

        // qtree changes SortingByRelev that excludes the second pass
        const TString qtree = "&qtree=cHic45rHyMXDwSDAIMGpwKDBZMAgxFxalCPFoMSgpapkbJxqbm6RnKKbkmhgrGuSkmKum2hkZqRraWaWmGxubGxhaZpsxGDFw8EM1M8A1M9gwODA4MEQwBDBkJCc8WLOwb_8ExgZZjGCzFzESJyBmxgZ9jIyAMEJRoYLjClaM5i4PKBOZIE6kQHsPgaw1QxIVrN5sIGszmAEW8qwiJEBzTCpg4xcc2jkYep414DBgtFJDOQ-kJM0GA0YwYEJ9FMVQwMjA9B4kAoEC79aAHTuXGY%2C";
        CHECK_TEST_TRUE(CheckFactorExistance(
            "body&dbgrlv=da&fsgta=_JsonFactors&relev=fast_formula=default&relev=all_factors;calc=hit_area:gt(#search_area,1e-6)&pron=fastcount15&" + qtree + kps,
            {"search_area", "hit_area"}
        ));
        return true;
    }
};

START_TEST_DEFINE(TestSearchFactorsPassages)
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "0.123", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "0.123", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "0.123", *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        }
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
        IndexMessages(messages, REALTIME, 1);
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        QuerySearch("body&gta=DocLen" + kps, results, &docProperties);
        if (results.size() != 1)
            ythrow yexception() << "incorrect doc count: " << results.size();
        return true;
    }
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }
};

START_TEST_DEFINE(TestSetMissingUserFactors)
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "12.3", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "12.3", *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        }
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
        IndexMessages(messages, REALTIME, 1);

        for (const bool strict: {true, false}) {
            const TString relevStrict = strict ? "&relev=strict_factors_cgi" : Default<TString>();

            TVector<TDocSearchInfo> results;
            TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
            THashMultiMap<TString, TString> searchProperties;
            QuerySearch("body" + relevStrict + "&relev=calc=user1:1,user2:43" + kps, results, &docProperties, &searchProperties);
            if (!strict) {
                CHECK_TEST_NEQ(results.size(), 0);
                CHECK_TEST_EQ(searchProperties.count("SearchErrors.debug"), 0);
            } else {
                CHECK_TEST_FAILED(results.size() != 0, "Query must be fail, but results size is equal to " + ToString(results.size()));

                THashMultiMap<TString, TString>::const_iterator i = searchProperties.find("SearchErrors.debug");
                if (i == searchProperties.end()) {
                    ythrow yexception() << "We haven't errors info in reply";
                }
                if (i->second.find("Unknown user factor") == TString::npos) {
                    ythrow yexception() << "Incorrect error message";
                }
            }
        }
        return true;
    }
    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/no_user_factors.cfg";
        return true;
    }
};

namespace {
    void CheckFactorValue(const THashMap<TString, double>& factors, const TString& name, double expectedValue) {
        THashMap<TString, double>::const_iterator i = factors.find(name);
        if (i == factors.end())
            ythrow yexception() << "factor " << name << " not found";
        if (Abs(i->second - expectedValue) > 1e-5)
            ythrow yexception() << "factor " << name << " value is incorrect: " << i->second << " != " << expectedValue;
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(TestRefineFactors)
public:
    virtual TString GetQuery(const TString& factorToRefine) const {
        return "somebody+<-+refinefactor:" + factorToRefine + "=0.1234+today";
    }
    virtual size_t RightFactorCount() const {
        return 1;
    }

    bool DoRun(const TString& factorToRefine, const TString& formula) {
        TString body = "somebody today";
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", "0", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", "0", *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", "0", *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            messages[i].MutableDocument()->SetBody(body);
        }
        IndexMessages(messages, REALTIME, 1);
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;

        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
        TString query = GetQuery(factorToRefine);
        QuerySearch(query + "&dbgrlv=da&relev=formula=" + formula + "&fsgta=_JsonFactors" + kps, results, &docProperties);
        TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(docProperties);
        if (!docProperties.size())
            throw yexception() << "no documents found";
        for (ui32 i = 0; i < factors.size(); ++i) {
            const THashMap<TString, double>& docFactors = factors[i];
            if (docFactors.size() != RightFactorCount())
                throw yexception() << "invalid factors count in report: " << docFactors.size();
            CheckFactorValue(docFactors, factorToRefine, 0.1234);
        }
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FiltrationModel"] = "WEIGHT";
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRefineDynamicFactors, TestRefineFactors)
public:
    size_t RightFactorCount() const override {
        return 3;
    }
    bool Run() override {
        return DoRun("TR", "dyn");
    }
};

START_TEST_DEFINE_PARENT(TestRefineStaticFactors, TestRefineFactors)
public:
    size_t RightFactorCount() const override {
        return 3;
    }
    bool Run() override {
        return DoRun("stat2", "default");
    }
};

START_TEST_DEFINE_PARENT(TestRefineRtyDynamicFactors, TestRefineFactors)
public:
    bool Run() override {
        return DoRun("RefineUserFactor", "B0010000000V3");
    }
};

START_TEST_DEFINE_PARENT(TestAllFactors, TestRefineFactors)
public:
    TString GetQuery(const TString& factorToRefine) const override {
        return "somebody+<-+refinefactor:" + factorToRefine + "=0.1234+today&relev=all_factors";
    }
    size_t RightFactorCount() const override {
        return 12;
    }
    bool Run() override {
        return DoRun("RefineUserFactor", "B0010000000V3");
    }
};


START_TEST_DEFINE(TestDpFactors)
    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "static_factors":{
                "stat1": 1
            },
            "rty_dynamic_factors": {
                "TestDpFactor": 0
            },
            "formulas":{
                "default":{
                    "polynom":"1005002008JPC6N7D6JP4V0"
                },
                "no_dp":{
                    "polynom":"200100KPC6JS3"
                }
            }
        })";

        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }
public:
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        return true;
    }

    bool CheckDpValue(const TMaybe<float>& expectedDp, ui64 expectedRelev, const TString& query, const TString& kps) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        QuerySearch(query + "&dbgrlv=da&fsgta=_JsonFactors" + kps, results, &docProperties);
        TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(docProperties);
        if (!docProperties.size())
            throw yexception() << "no documents found";
        for (ui32 nDoc = 0; nDoc < factors.size(); ++nDoc) {
            const THashMap<TString, double>& docFactors = factors[nDoc];
            const size_t expectedFactorsNum = expectedDp.Defined() ? 2 : 1;
            if (docFactors.size() != expectedFactorsNum) {
                throw yexception() << "invalid factors count in report: " << docFactors.size();
            }
            CheckFactorValue(docFactors, "stat1", 1.0);
            if (expectedDp.Defined()) {
                CheckFactorValue(docFactors, "TestDpFactor", *expectedDp.Get());
            }
        }
        CHECK_TEST_EQ(results.size(), 1);
        const ui64 rlvFormula = results[0].GetRelevance();
        CHECK_TEST_EQ(rlvFormula, expectedRelev);

        return true;
    }

    bool Run() override {
        const TString body = "aaa bbb";
        const TString query = "aaa";
        const TString formula = "default";
        const TString formulaNoDp = "no_dp";
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", ToString(1.0f), *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            messages[i].MutableDocument()->SetBody(body);
        }
        IndexMessages(messages, REALTIME, 1);

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
        {
            // Check 1. DP factors are not used
            const TMaybe<float> expectedDp = Nothing();
            const ui64 expectedRelev = 102000000u; // polynom is 0.2*stat1
            CHECK_TEST_TRUE(CheckDpValue(expectedDp, expectedRelev, query + "&relev=formula=" + formulaNoDp, kps));
        }

        {
            // Check 2. DP factors are used
            const float expectedDp = 0.6f;
            const ui64 expectedRelev = 102600000u; // polynom is 0.1*TestDpFactor+0.2*stat1
            CHECK_TEST_TRUE(CheckDpValue(expectedDp, expectedRelev, query + "&relev=formula=" + formula, kps));
        }

        {
            // Check 3. Disable mock_rty_feature
            NJson::TJsonValue config = CreateFactorsConfigTemplate();
            config["enabled_rty_features"] = NJson::TJsonValue{NJson::JSON_ARRAY};
            (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(config);
            ApplyConfig();
            Controller->RestartServer(false, nullptr);

            const TMaybe<float> expectedDp = 0.0f;
            const ui64 expectedRelev = 102000000u; // polynom is 0.1*TestDpFactor+0.2*stat1
            CHECK_TEST_TRUE(CheckDpValue(expectedDp, expectedRelev, query + "&relev=formula=" + formula, kps));
        }
        return true;
    }
};


START_TEST_DEFINE(TestErfGta)
public:
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/mix_factors_width.cfg";
        return true;
    }

    bool Run() override {
        TString body = "somebody today";
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("f32", ToString(i * 0.01f), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("f16", ToString(0.14f), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("f8", ToString(i * 0.1f), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("i8", ToString(i), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("i16", ToString(i * 128), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("i32", ToString(100004), *messages[i].MutableDocument()->MutableFactors());

            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            messages[i].MutableDocument()->SetBody(body);
        }
        IndexMessages(messages, REALTIME, 1);

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
        TString query = body;
        QuerySearch(query + kps + "&gta=_Erf_f16&fsgta=_Erf_i32", results, &docProperties, nullptr, true);

        auto p = *(docProperties[0]);

        if (!p.contains("_Erf_i32"))
            ythrow yexception() << "No i32 value";
        if (FromString<ui32>(p.find("_Erf_i32")->second) != 100004)
            ythrow yexception() << "Incorrect _Erf_i32 value: " << FromString<ui32>(p.find("_Erf_i32")->second);

        if (!p.contains("_Erf_f16"))
            ythrow yexception() << "No _Erf_f16 value";
        if (Abs(FromString<float>(p.find("_Erf_f16")->second) - 0.14f) > 0.1)
            ythrow yexception() << "Incorrect _Erf_f16 value: " << FromString<float>(p.find("_Erf_f16")->second);

        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(CatboostCategBase)
protected:
    //static ui32 EncodeCat(const TStringBuf catName) {
    //    ui32 hash = CityHash64(catName) & 0x00ffffff;
    //    Y_ASSERT(static_cast<ui32>((float)hash) == hash);
    //    return hash;
    //}

    bool Test(TIndexerType indexer) {
        TString body = "where is my cat";
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 4, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        TVector<std::pair<TString, TString>> factorVals {
            {"0.1", "1"},
            {"0.9", "1"},
            {"0.1", "2"},
            {"0.9", "2"}
        };
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("f_val", factorVals[i].first, *messages[i].MutableDocument()->MutableFactors());
            //NSaas::AddSimpleIntFactor("f_categ", factorVals[i].second, *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("f_categ", factorVals[i].second, *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            messages[i].MutableDocument()->SetBody(body);
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        TString kps = GetAllKps(messages);
        TString query = body;
        QuerySearch(query + kps + "&gta=f_categ&gta=f_val&fsgta=_Erf_f_categ", results, &docProperties, nullptr, true);


        CHECK_TEST_EQ(4u, results.size());
        if (results[0].GetUrl() != messages[3].GetDocument().GetUrl())
            ythrow yexception() << "first document wrong";
        if (results[1].GetUrl() != messages[0].GetDocument().GetUrl())
            ythrow yexception() << "second document wrong";
        if (results[2].GetUrl() != messages[1].GetDocument().GetUrl())
            ythrow yexception() << "third document wrong";
        if (results[3].GetUrl() != messages[2].GetDocument().GetUrl())
            ythrow yexception() << "third document wrong";
        return true;
    }
public:

    bool InitConfig() override {
        SetIndexerParams(ALL, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/catboost/cat_factors.cfg";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestCatboostCategDisk, CatboostCategBase)
    bool Run() override {
        CHECK_TEST_TRUE(Test(DISK));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestCatboostCategMem, CatboostCategBase)
    bool Run() override {
        CHECK_TEST_TRUE(Test(REALTIME));
        return true;
    }
};
