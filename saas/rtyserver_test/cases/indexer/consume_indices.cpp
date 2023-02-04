#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/synchronizer/library/sync.h>
#include <saas/rtyserver/index_storage/index_consume_transaction.h>
#include <saas/rtyserver/components/fullarchive/layer.h>
#include <saas/rtyserver/index_storage/index_storage.h>
#include <saas/rtyserver/config/config.h>
#include <saas/rtyserver/common/search_area_modifier.h>

#include <library/cpp/yconf/patcher/config_patcher.h>
#include <util/folder/dirut.h>
#include <util/folder/tempdir.h>
#include <util/folder/filelist.h>
#include <util/system/fs.h>
#include <util/system/shellcommand.h>


namespace {
    THolder<TTempDir> CopyIndex(const TString& index, const std::function<bool(const TString& filename)> filterExpression = {}) {
        THolder<TTempDir> result = MakeHolder<TTempDir>();

        TFileList fileList;
        fileList.Fill(index, "index", "", 1000);

        while (true) {
            TString fileName(fileList.Next());
            if (fileName.empty()) {
                break;
            }

            TFsPath path(index);
            path /= fileName;
            if (!!filterExpression && !filterExpression(path.GetName())) {
                continue;
            }

            TFsPath dst(result->Path());
            dst /= fileName;
            path.CopyTo(dst, true);
        }
        return result;
    }
} // namespace


START_TEST_DEFINE(TestInvalidIndexConsumption)
bool InitConfig() override {
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "full";
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Common.SaveDeletedDocuments"] = "true";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Indexer.Common.TimestampControlEnabled"] = false;
    (*ConfigDiff)["Indexer.Disk.MaxDocuments"] = "100";
    (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
    (*ConfigDiff)["Indexer.Disk.TimeToLiveSec"] = "1200";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["ResourceFetchConfig.ApplyEmptyIndex"] = "false";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.TimeoutSeconds"] = "66";
    return true;
}

// ignores base layer that should be restored
THolder<TTempDir> CreatePartiallyCorruptedIndex() {
    static constexpr TStringBuf FullArcBasePrefix = "indexfullarc.";

    return CopyIndex(GetIndexDir(), [](const TString& filename)->bool{
        return !filename.StartsWith(FullArcBasePrefix);
    });
}

THolder<TTempDir> CreateEmptyIndex() {
    return MakeHolder<TTempDir>();
}

void CreateRemoveBinDelta(const TVector<NRTYServer::TMessage>& messages, TFsPath /* initialIndex*/) {
    TSet<std::pair<ui64, TString> > deleted;
    Cout << 3.5 << Endl;
    DeleteSomeMessages(messages, deleted, DISK, 1);
    Cout << 3.7 << Endl;
    ReopenIndexers();
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;

    Cout << 1 << Endl;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    Cout << 2 << Endl;

    ReopenIndexers();
    TQuerySearchContext ctx;
    TVector<TDocSearchInfo> results;
    QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
    CHECK_TEST_EQ(results.size(), 1);

    Cout << 3 << Endl;
    Controller->StopBackends();
    THolder<TTempDir> fullLayerIndex = CopyIndex(GetIndexDir());
    THolder<TTempDir> corruptedIndex = CreatePartiallyCorruptedIndex();
    Controller->RestartServer(false, nullptr);
    CreateRemoveBinDelta(messages, GetIndexDir());
    Controller->StopBackends();
    THolder<TTempDir> deltaIndex = CopyIndex(GetIndexDir());
    NFs::RemoveRecursive(GetIndexDir());
    TShellCommand copyCmd("cp", {"-rf", fullLayerIndex->Path(), GetIndexDir()});
    copyCmd.Run();
    Cout << 4 << Endl;
    Controller->RestartServer(false, nullptr);

    // initial check: document is obtained from initial index
    QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
    CHECK_TEST_EQ(results.size(), 1);

    // empty index
    {
        THolder<TTempDir> emptyIndex = CreateEmptyIndex();

        TString syncResult;
        CHECK_TEST_TRUE(Controller->Synchronize(emptyIndex->Path(), NRTYServer::EConsumeMode::Replace, syncResult));
        QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
        CHECK_TEST_EQ(results.size(), 1); // shouldn't consume empty index
    }

    //  Corrupred index
    {
        TString syncResult;
        CHECK_TEST_TRUE(Controller->Synchronize(corruptedIndex->Path(), NRTYServer::EConsumeMode::Replace, syncResult));
        QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
        CHECK_TEST_EQ(results.size(), 1);
    }

    // Should apply empty index because of HardReplace
    {
        THolder<TTempDir> emptyIndex = CreateEmptyIndex();
        TString syncResult;
        CHECK_TEST_TRUE(Controller->Synchronize(emptyIndex->Path(), NRTYServer::EConsumeMode::HardReplace, syncResult));

        QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
        CHECK_TEST_EQ(results.size(), 0);
    }

    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "base,full";
    ApplyConfig();
    Controller->RestartServer(false, nullptr);
    // After we previoulsly got zero documents, we should successfully restore partially corrupted FullArc without base layer
    {
        TString syncResult;
        CHECK_TEST_TRUE(Controller->Synchronize(fullLayerIndex->Path(), NRTYServer::EConsumeMode::Replace, syncResult));
        QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
        CHECK_TEST_EQ(results.size(), 1);
    }


     // delta with only removes index & apply
    {
        TString syncResult;
        CHECK_TEST_TRUE(Controller->Synchronize(deltaIndex->Path(), NRTYServer::EConsumeMode::Apply, syncResult));
        QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
        CHECK_TEST_EQ(results.size(), 0);
    }
    return true;
}
};

START_TEST_DEFINE(TestBrokenIndexConsumptionTransaction)
bool InitConfig() override {
    (*ConfigDiff)["Components"] =  INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "full";
    (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Indexer.Common.TimestampControlEnabled"] = false;
    (*ConfigDiff)["Indexer.Disk.MaxDocuments"] = "100";
    (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
    (*ConfigDiff)["Indexer.Disk.TimeToLiveSec"] = "1200";
    (*ConfigDiff)["ResourceFetchConfig.ApplyEmptyIndex"] = "false";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.TimeoutSeconds"] = "66";
    return true;
}

bool Run() override {
    const TString InitialIndexDocumentUrl = "initial";

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl(InitialIndexDocumentUrl);
    messages[1].MutableDocument()->SetUrl("updated");
    const TVector<NRTYServer::TMessage> initialIndexMessages = { messages[0] };
    const TVector<NRTYServer::TMessage> updatedIndexMessages = { messages[1] };

    const TString kps{ GetAllKps(messages) };
    const TString query = "body&dbgrlv=da" + kps;
    TQuerySearchContext ctx;
    ctx.PrintResult = true;

    // Prepare index for update
    THolder<TTempDir> updatedIndex;
    {
        IndexMessages(updatedIndexMessages, DISK, 1);
        ReopenIndexers();

        Controller->StopBackends();
        updatedIndex = CopyIndex(GetIndexDir());

        NFs::RemoveRecursive(GetIndexDir());
        NFs::MakeDirectory(GetIndexDir());
        Controller->RestartServer(false, nullptr);
    }

    auto checkSingleDocumentWithUrl = [&](const TString& url) -> bool {
        TVector<TDocSearchInfo> results;
        QuerySearch(query, results, ctx);
        CHECK_TEST_EQ(results.size(), 1);
        CHECK_TEST_EQ(results[0].GetUrl(), url);
        return true;
    };

    // create initial index
    IndexMessages(initialIndexMessages, DISK, 1);
    ReopenIndexers();
    CHECK_TEST_TRUE(checkSingleDocumentWithUrl(InitialIndexDocumentUrl));

    // Emulate broken transaction as if like rtyserver was restarted in the middle of transaction
    // Place index, manually create transaction file and restart the server

    const TFsPath root(GetIndexDir());

    TVector<TString> brokenTransactionIndicies;
    {
        TDirsList updateIndexList;
        updateIndexList.Fill(updatedIndex->Path(), "index_");

        while (true) {
            const TString indexDirName(updateIndexList.Next());
            if (indexDirName.empty()) {
                break;
            }
            TFsPath newIndex {updatedIndex->Path()};
            newIndex /= indexDirName;

            TFsPath destination {root};
            destination /= TString("index_0000000000_00") + IntToString<10>(10 + brokenTransactionIndicies.size());
            newIndex.CopyTo(destination, true);

            brokenTransactionIndicies.push_back(destination.GetName());
        }
    }
    TIndexConsumeTransaction::CreateTransactionFile(GetIndexDir(), brokenTransactionIndicies);
    Controller->RestartServer(false, nullptr);

    CHECK_TEST_TRUE(checkSingleDocumentWithUrl(InitialIndexDocumentUrl));

    // check that meta file was deleted
    TVector<TString> transactionIndicies, metaFiles;
    TIndexConsumeTransaction::GetIncompletedTransactionIndicies(GetIndexDir(), transactionIndicies, metaFiles);
    CHECK_TEST_TRUE(transactionIndicies.empty());
    CHECK_TEST_TRUE(metaFiles.empty());
    return true;
}
};
