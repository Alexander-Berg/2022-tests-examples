#include <saas/protos/rtyserver.pb.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/cases/jupi/test_params/jupi_test_params.h>

#include <robot/library/utils/gdb_break.h>
#include <saas/rtyserver_jupi/library/extbuilder/arc_verify.h>

#include <library/cpp/json/writer/json_value.cpp>
#include <library/cpp/logger/global/global.h>

#include <util/stream/file.h>

#include <google/protobuf/messagext.h>

using namespace NRTYServer;
using TProtoReader = ::google::protobuf::io::TProtoReader;

SERVICE_TEST_RTYSERVER_DEFINE(TestRtyJupiIndexDumpBase)
protected:
    void LoadMessagesFromDump(TVector<TMessage>& messages, IInputStream& input) {
        auto bogusModTime = Seconds();
        TProtoReader rd(&input);
        auto limit = GetMaxDocs();
        for (;;) {
            TMessage msg;
            if (!rd.Load(msg))
                break;

            msg.SetMessageId(IMessageGenerator::CreateMessageId());
            if (msg.HasDocument()) {
                auto& document = *msg.MutableDocument();
                if (!document.HasVersion()) {
                    document.SetVersion(0);
                }
                if (!document.HasModificationTimestamp()) {
                    document.SetModificationTimestamp(bogusModTime);
                }
            }
            messages.emplace_back(std::move(msg));

            if (messages.size() >= limit) {
                break;
            }
        }
    }

    virtual ui32 GetMaxDocs() const {
        return Max<ui32>();
    }

    static void DebugPrintUrls(const TVector<TMessage>& messages, ui32 maxDeadlineDocs) {
        //
        // This debug output predicts the "listing" of the final segment. It takes versioning into account
        // This is useful for debugging issues like REFRESH-337
        //
        TMap<TString, ui64> versions;
        TVector<bool> removed;
        removed.resize(messages.size(), false);
        for (ui32 p = 0; p < messages.size(); ++p) {
            const TMessage& message = messages[p];
            if (!message.HasDocument()) {
                removed[p] = true;
                continue;
            }
            auto& document = message.GetDocument();
            auto pos = versions.insert(std::make_pair(document.GetUrl(), document.GetVersion()));
            bool updated = pos.second || pos.first->second < document.GetVersion() || (pos.first->second == 0 && document.GetVersion() == 0);
            if (!updated) {
                removed[p] = true;
            } else {
                if (!pos.second) {
                    pos.first->second = document.GetVersion();
                }
            }
        }

        ui32 nextDocId = 0;
        ui32 removeCount = 0;
        for (ui32 p = 0; p < messages.size(); ++p) {
            const TMessage& message = messages[p];
            if (!message.HasDocument()) {
                continue;
            }
            auto& document = message.GetDocument();
            auto pos = versions.find(document.GetUrl());
            if (pos != versions.end() && pos->second != document.GetVersion()) {
                removed[p] = true;
            }

            i32 displayDocId;
            if (removed[p]) {
                displayDocId = -1;
                ++removeCount;
            } else {
                displayDocId = static_cast<i32>(nextDocId++);
            }

            INFO_LOG << "DebugPrintUrls: docid_predict=" << displayDocId << ";url=" << document.GetUrl() << Endl;
        }
        INFO_LOG << "Remove count: " << removeCount << Endl;
        INFO_LOG << "MaxDeadlineDocs: " << maxDeadlineDocs << Endl;
        INFO_LOG << "Total messages: " << messages.size() << Endl;
        Y_ENSURE(removeCount >= NJupiTest::MIN_REMOVE_COUNT,
            "Input data do not contain enough removes: expected " << NJupiTest::MIN_REMOVE_COUNT << ", got " << removeCount);
        Y_ENSURE(maxDeadlineDocs + removeCount < messages.size(),
            "Input data do not contain enough uniq documents for MaxDeadlineDocs");
    }

    virtual bool MaxDeadlineDocsEnabled() const {
        return true;
    }

protected:
    TFsPath DumpFileName; // for logging
    TVector<TMessage> Messages;
    ui32 MaxDeadlineDocs = 0;

public:
    bool InitConfig() override {
        LoadMessages();

        if (MaxDeadlineDocsEnabled()) {
            INFO_LOG << "Setup MaxDeadlineDocs" << Endl;
            MaxDeadlineDocs = static_cast<ui32>(NJupiTest::MAX_DEADLINE_DOCS_FACTOR * Messages.size());
            (*ConfigDiff)["Merger.MaxDeadlineDocs"] = MaxDeadlineDocs;
        }

        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
        (*ConfigDiff)["Indexer.Common.HttpOptions.Threads"] = "1";
        (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
        SetIndexerParams(DISK, 101, 1);

        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["AdditionalModules"] = "EXTBUILDER";
        (*ConfigDiff)["ModulesConfig.EXTBUILDER.ForceKeepPreparates"] = false;
        (*ConfigDiff)["ModulesConfig.EXTBUILDER.OnFail"] = "CRASH";
        SetNoSearch(true);
        return true;
    }

    virtual bool DoIndexation(TVector<TMessage>& messages) {
        Y_ENSURE(messages.size() > NJupiTest::MIN_DOC_COUNT, (TString)"Not enough messages in the DumpFile:" + DumpFileName);

        // Index messages in NJupiTest::PORTIONS_COUNT portions, enforce merge after each portion
        // (we need completely deterministic behavior here - and do not need to test "indexing while merging"
        // - that is why we start the merger tasks explicitly)
        const size_t portionSize = Max<size_t>(100, NJupiTest::GetPortionSize(messages.size()));
        for(size_t begin = 0; begin < messages.size(); begin += portionSize) {
            size_t end = Min(messages.size(), begin + portionSize);
            INFO_LOG << "Indexing documents from #" << begin << " - #" << end << " range" << Endl;
            TVector<TMessage> portion;
            portion.insert(portion.end(), messages.begin() + begin, messages.begin() + end);

            IndexMessageAsIs(portion, /*strictOrder=*/true);
            ReopenIndexers();
            Controller->ProcessCommand("create_merger_tasks");
            Controller->ProcessCommand("do_all_merger_tasks");
        }
        return true;
    }

    void LoadMessages() {
        DumpFileName = TFsPath(GetResourcesDirectory()) / "messages.protobin";
        INFO_LOG << "Reading messages from protobin:" << DumpFileName << Endl;
        Y_ENSURE(DumpFileName.Exists(), DumpFileName << " not found");

        TFileInput dumpFile(DumpFileName);
        Messages.clear();
        LoadMessagesFromDump(Messages, dumpFile);
    }

    bool Run() override {
        Y_DEBUG_BREAK("GDB:RtyJupiTest");

        CHECK_TEST_TRUE(Controller->GetFinalIndexes(false).size() == 0);
        DebugPrintUrls(Messages, MaxDeadlineDocs);

        const bool indexationSuccess = DoIndexation(Messages);
        CHECK_TEST_TRUE(indexationSuccess);

        auto mergedDirs = Controller->GetFinalIndexes(/*stopServer=*/true);
        CHECK_TEST_TRUE(mergedDirs.size() == 1);
        TFsPath resultDir(*mergedDirs.begin());

        NJson::TJsonValue result(NJson::JSON_MAP);
        result["shard"] = (TString)resultDir.Fix();

        TFsPath jsonReportPath = TFsPath(GetRunDir()) / "test_result.json";
        INFO_LOG << "Saving test report to " << jsonReportPath << Endl;
        TFixedBufferFileOutput jsonResult(jsonReportPath);
        jsonResult << result;
        jsonResult.Finish();

        return true;
    }

};

START_TEST_DEFINE_PARENT(TestRtyJupiIndexDump, TestRtyJupiIndexDumpBase)
};

START_TEST_DEFINE_PARENT(TestRtyJupiIndexDump_NoMaxDeadlineDocs, TestRtyJupiIndexDumpBase)
    bool MaxDeadlineDocsEnabled() const override {
        return false;
    }
};

// Little one-doc indexation test - only for manual runs
START_TEST_DEFINE_PARENT(TestRtyJupiIndexDumpOneDoc, TestRtyJupiIndexDumpBase)
    ui32 GetMaxDocs() const override {
        return 1;
    }

    bool DoIndexation(TVector<TMessage>& messages) override {
        IndexMessageAsIs(messages);
        ReopenIndexers();
        return true;
    }
};
