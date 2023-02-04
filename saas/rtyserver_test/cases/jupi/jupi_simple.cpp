#include "jupi.h"
#include <saas/protos/rtyserver.pb.h>
#include <saas/rtyserver_test/util/oxy/kiwi_util.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/model/external_builder.h>
#include <saas/rtyserver/indexer_core/index_dir.h>
#include <robot/jupiter/library/rtdoc/protos/builder_task.pb.h>
#include <robot/jupiter/library/rtdoc/file/model/files.h>
#include <robot/jupiter/library/rtdoc/file/docidmap_io.h>

#include <util/system/fs.h>
#include <util/system/event.h>

using namespace NRTYServer;

class IMockExtBuilderConn {
public:
    virtual ~IMockExtBuilderConn() = default;

    virtual void OnClientRun(const NRtDoc::TBuilderTask& task) = 0;
};

class TMockExtBuilderClient : public IExtBuilderClient {
private:
    IMockExtBuilderConn& Owner;
public:
    TMockExtBuilderClient(IMockExtBuilderConn& owner)
        : Owner(owner)
    {
    }

    void Run(NRtDoc::TBuilderTaskResult& result, NRtDoc::TBuilderTask& task) override {
        Owner.OnClientRun(task);
        result.SetErrorCode(0);
    }
};

class TMockExtBuilderConn : public IExtBuilderConnection, public IMockExtBuilderConn {
public:
    NRtDoc::TBuilderTask Task;
    int CallsCount = 0;

protected:
    void OnClientRun(const NRtDoc::TBuilderTask& task) override {
        Task.CopyFrom(task);
        CallsCount++;

        // now some hacks to collect data

        // move temporary merger mappings into destdir
        TFsPath trgDir(task.GetOutput().GetTrgDir());
        for (ui32 i = 0; i < task.InputsSize(); i++) {
            const NRtDoc::TBuilderTask::TBuilderInput& input = task.GetInputs(i);
            Y_ENSURE_EX(input.HasSrcMapping(), yexception() << "input.HasSrcMapping()");
            NFs::Rename(input.GetSrcMapping(), trgDir / (ToString(i) + ".merger.mapping.tmp"));
        }

        // create a flag (a lil' hack to disable OXY manager start on an empty folder)
        TFile flag(trgDir / "jupi_prep_index", WrOnly | OpenAlways);
    }

public:
    void Register() {
        TExtBuilderEngine::RegisterConnection(this);
    }
    void UnRegister() {
        TExtBuilderEngine::RegisterConnection(nullptr);
    }

    IExtBuilderClient::TPtr CreateClient() override {
        return MakeIntrusive<TMockExtBuilderClient>(*this);
    }
};

START_TEST_DEFINE_PARENT(TestRtyJupiComponent, TestJupi)
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
        SetNoSearch(true);
        return true;
    }
    bool Run() override {
        TVector<TMessage> messages1;
        messages1.push_back(GenerateAddMessage("http://aaa.com/a", 1));
        messages1.push_back(GenerateAddMessage("http://aaa.com/b", 1));
        IndexMessages(messages1, DISK, 1);
        ReopenIndexers();

        TVector<TMessage> messages2;
        messages2.push_back(GenerateAddMessage("http://aaa.com/c", 2));
        messages2.push_back(GenerateAddMessage("http://aaa.com/b", 2));
        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();

        auto mockExtBuilder = MakeIntrusive<TMockExtBuilderConn>();
        mockExtBuilder->Register();

        auto indexDirs = Controller->GetFinalIndexes(/*stopServer=*/false);
        CHECK_TEST_TRUE(indexDirs.size() == 2);
        for (auto indexDir : indexDirs) {
            CHECK_TEST_TRUE(NRTYServer::HasIndexDirPrefix(indexDir, DIRPREFIX_PREP));
            NRtDoc::TPrepTableBase prep(TFsPath(indexDir) / "prep.lumps", TString());
            CHECK_TEST_TRUE(TFsPath(prep.GetFilePath()).Exists());
            CHECK_TEST_TRUE(TFsPath(prep.GetFileIndexPath()).Exists());
            CHECK_TEST_TRUE(TFsPath(prep.GetFileMapPath()).Exists());
        }

        CHECK_TEST_TRUE(mockExtBuilder->CallsCount == 0); // extbuilder should not be called when PREPARED index is closing

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        mockExtBuilder->UnRegister(); // doing this early, just for fun
        ReopenIndexers();

        CHECK_TEST_TRUE(mockExtBuilder->CallsCount == 1);
        CHECK_TEST_TRUE(mockExtBuilder->Task.InputsSize() == 2);
        //TODO(yrum): check more of the Task fields
        mockExtBuilder.Drop();

        auto mergedDirs = Controller->GetFinalIndexes(/*stopServer=*/true);
        CHECK_TEST_TRUE(mergedDirs.size() == 1);
        TFsPath resultDir(*mergedDirs.begin());

        NRtDoc::TDocIdMap map1;
        NRtDoc::TDocIdMap map2;
        NRtDoc::TDocIdMapIo::Load(&map1, resultDir / "0" ".merger.mapping.tmp");
        NRtDoc::TDocIdMapIo::Load(&map2, resultDir / "1" ".merger.mapping.tmp");

        CHECK_TEST_TRUE(map1.GetData()->size() == 2 && map2.GetData()->size() == 2);
        CHECK_TEST_TRUE((*map1.GetData() == TVector<ui32>{0, NRtDoc::TDocIdMap::DeletedDocument()})
                || (*map1.GetData() == TVector<ui32>{NRtDoc::TDocIdMap::DeletedDocument(), 0}));
        CHECK_TEST_TRUE((*map2.GetData() == TVector<ui32>{1, 2}));

        //In a configuration with JUPI, some index components should not be turned on automatically (REFRESH-308)
        const TVector<TString> filesThatShouldNotExist = {
            "index.keys.bloom",
            "index.keys.inv",
            "index.keys.key"
        };

        for (const TString& unnecessaryFile : filesThatShouldNotExist) {
            CHECK_TEST_FAILED((resultDir / unnecessaryFile).Exists(), unnecessaryFile + " should not exist")
        }

        return true;
    }
};
