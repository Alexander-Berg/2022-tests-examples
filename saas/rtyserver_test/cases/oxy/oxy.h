#pragma once

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <robot/library/oxygen/processing_model/processing_model/processing_model.h>
#include <robot/library/oxygen/processing_model/input_stream/input_stream.h>
#include <robot/library/oxygen/processing_model/index/index.h>
#include <robot/library/oxygen/processing_model/setvar/setvar.h>
#include <robot/library/oxygen/processing_model/interval/interval.h>
#include <saas/util/system/dir_digest.h>
#include <util/folder/filelist.h>
#include <saas/rtyserver/common/debug_messages.h>
#include <saas/rtyserver/indexer_core/index_metadata_processor.h>


struct TOxyProcessorSleepAction: public IMessageProcessor {
    TOxyProcessorSleepAction(const ui32 seconds) {
        Seconds = seconds;
        RegisterGlobalMessageProcessor(this);
    }
    ~TOxyProcessorSleepAction() {
        UnregisterGlobalMessageProcessor(this);
    }
    bool Process(IMessage* message) override {
        TMessageOnCloseIndex* msg = dynamic_cast<TMessageOnCloseIndex*>(message);
        if (msg) {
            switch(msg->GetStage()) {
            case TMessageOnCloseIndex::AfterBuildersClose:
            case TMessageOnCloseIndex::AfterRenameDir:
                sleep(Seconds);
            default:
                break;
            }
            return true;
        }
        return false;
    }
    TString Name() const override {
        return "TOxyProcessorSleepAction";
    }
private:
    ui32 Seconds;
};

SERVICE_TEST_RTYSERVER_DEFINE(TestOxygenDocs)
void GenerateDocs(const TString& path, NRTYServer::TMessage::TMessageType type, ui32 count = 0, bool backOrder = false, bool checkDel = true, ui32 startPos = 1, ui64 timestamp = 0);
    bool InitConfig() override;
    virtual void GetResponses();
    bool Finish() override;
protected:
    TSet<TString> MergeTuples; // tuples that merge directly (e.g. keyinv, arc)
    TSet<TString> AddRequiredMergeTuples;   // tuples that do not participate in index creation
    TSet<TString> AddRequiredTuples;   // tuples that do not participate in index creation
protected:

    bool CheckLayersAndCount(ui32 checkCount = Max<ui32>()) {
        try {
            TJsonPtr serverInfo(Controller->GetServerInfo());
            NJson::TJsonValue& info = (*serverInfo)[0];

            auto map = info["indexes"].GetMap();
            ui32 compRes = 0;
            bool foundCount = false;
            for (auto& i : map) {
                compRes += i.second["count"].GetUInteger();
                if (i.second["count"].GetUInteger() && TFsPath(GetIndexDir() + "/" + i.first + "/indexfrq").Exists() && TFile(GetIndexDir() + "/" + i.first + "/indexfrq", RdOnly).GetLength()) {
                    TIndexMetadataProcessor processor(GetIndexDir() + "/" + i.first);

                    DEBUG_LOG << GetIndexDir() + "/" + i.first << " checking... " << Endl;

                    TSet<TString> full;
                    TSet<TString> forMerge;
                    TSet<TString> complement;
                    for (ui32 i = 0; i < processor->TuplesListSize(); ++i) {
                        auto&& list = processor->GetTuplesList(i);
                        DEBUG_LOG << list.GetLayer() << " checking..." << Endl;
                        if (list.GetLayer() == "FOR_MERGE") {
                            for (ui32 j = 0; j < list.TuplesSize(); ++j) {
                                forMerge.insert(list.GetTuples(j));
                            }
                        }
                        if (list.GetLayer() == "full") {
                            for (ui32 j = 0; j < list.TuplesSize(); ++j) {
                                full.insert(list.GetTuples(j));
                            }
                        }
                        if (list.GetLayer() == "COMPLEMENT") {
                            for (ui32 j = 0; j < list.TuplesSize(); ++j) {
                                complement.insert(list.GetTuples(j));
                            }
                        }
                    }

                    CHECK_TEST_TRUE(full.size());
                    CHECK_TEST_TRUE(complement.size());
                    CHECK_TEST_TRUE(forMerge.size());

                    for (auto&& i : MergeTuples) {
                        DEBUG_LOG << i << " checking ..." << Endl;
                        CHECK_TEST_TRUE(full.contains(i));
                        CHECK_TEST_TRUE(complement.contains(i));
                        CHECK_TEST_TRUE(!forMerge.contains(i));
                        DEBUG_LOG << i << " checking ... OK" << Endl;
                    }

                    for (auto&& i : AddRequiredMergeTuples) {
                        DEBUG_LOG << i << " checking ..." << Endl;
                        CHECK_TEST_TRUE(full.contains(i));
                        CHECK_TEST_TRUE(!complement.contains(i));
                        CHECK_TEST_TRUE(forMerge.contains(i));
                        DEBUG_LOG << i << " checking ... OK" << Endl;
                    }

                    for (auto&& i : AddRequiredTuples) {
                        DEBUG_LOG << i << " checking ..." << Endl;
                        CHECK_TEST_TRUE(full.contains(i));
                        CHECK_TEST_TRUE(complement.contains(i));
                        CHECK_TEST_TRUE(!forMerge.contains(i));
                        DEBUG_LOG << i << " checking ... OK" << Endl;
                    }
                    if (checkCount == i.second["count"].GetUInteger())
                        foundCount = true;
                }

            }
            if (checkCount != Max<ui32>()) {
                CHECK_TEST_TRUE(foundCount);
                CHECK_TEST_EQ(compRes, checkCount);
            }
        } catch (...) {
            ERROR_LOG << CurrentExceptionMessage() << Endl;
            return false;
        }
        return true;
    }

    void CleanReorderSensitive() {
        TFsPath(TmpDir + "/oxy_index/incremental.dat").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexaa").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexerf2").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/url.dat").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexarc").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexsent").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/index.refarc").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/index.refdir").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/d.c2n").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/h.c2n").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexdir").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexinvhash").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexherf").DeleteIfExists();
        TFsPath(TmpDir + "/oxy_index/indexregherf").DeleteIfExists();
    }

    TString GetOxyDataParentDir() {
        if (GetEnv("OXY_DATA_P_PATH") != TString()){
            return GetEnv("OXY_DATA_P_PATH");
        } else {
            return GetResourcesDirectory();
        }
    }

    void BuildOXYIndex(ui32 from = 0, ui32 to = Max<ui32>(), const TString& dumpName = "/oxy/test_compare/docs/docs_oxygen000.search.yandex.net.dump.snappy:dump", const TString& confName = "/oxy/configs/OxygenOptionsGA2_kwd.cfg") {
        TProcessingModel model;
        TInputStreamModificator input;
        INFO_LOG << "MODIFY: " << GetResourcesDirectory() << dumpName << Endl;
        input.Modify(model, GetResourcesDirectory() + dumpName);

        if (to != Max<ui32>() || from != 0) {
            TInputStreamIntervalModificator inputInterval;
            inputInterval.Modify(model, ToString(from) + "," + ToString(to));
        }

        TSetVariableModificator varDir;
        TmpDir = TFsPath(GetIndexDir()).Parent().GetPath();
        auto outputDir = TFsPath(TmpDir) / "oxy_index";
        outputDir.MkDir();
        INFO_LOG << "MODIFY: " << TmpDir << "/oxy_index" << Endl;
        varDir.Modify(model, "ResDir=" + GetResourcesDirectory());
        varDir.Modify(model, "OxyPDir=" + GetOxyDataParentDir());
        varDir.Modify(model, "TmpDir=" + TmpDir);
        TIndexingModificator im;
        INFO_LOG << "MODIFY: " << GetResourcesDirectory() << confName << Endl;
        im.Modify(model, GetResourcesDirectory() + confName);
        model.Run();
        TFsPath(TmpDir + "/oxy_index/incremental.dat").DeleteIfExists();
    }

    bool CompareIndexes(const TString& pathIdeal = "") {
        PrintInfoServer();

        if (GetSaveResponses){
            INFO_LOG << "Compare indexes skipped because it's getting-search-responses test" << Endl;
            return true;
        }

        TRegExMatch antimatch("stamp\\.TAG");

        CHECK_WITH_LOG(!!TmpDir);
        TString cmpDir = !pathIdeal ? TmpDir + "/oxy_index" : pathIdeal;
        INFO_LOG << "Compare indexes with " << cmpDir << Endl;
        TDirHashInfo hashOxy = GetDirHashes(cmpDir, nullptr, &antimatch);
        TDirsList dl;
        dl.Fill(GetIndexDir());
        TString dir;
        while (!!(dir = dl.Next())) {
            if (dir.StartsWith("index_") && TFsPath(GetIndexDir() + "/" + dir + "/indexkey").Exists()) {
                DEBUG_LOG << "Try to compare directory " << dir << Endl;
                TDirHashInfo hashMy = GetDirHashes(GetIndexDir() + "/" + dir, nullptr, &antimatch);
                TMap<TString, TString> report;
                bool result = CompareHashes(hashOxy, hashMy, report, true);
                for (auto& i : report) {
                    INFO_LOG << i.first << " : " << i.second << Endl;
                }
                return result;
            }
        }
        ythrow yexception() << "cant find correct index";
        return false;
    }
    TString TmpDir;
    TVector<NRTYServer::TMessage> Messages;
    TSet<TString> SelectedTuples;
    TSet<TString> ErasedTuples;
};
