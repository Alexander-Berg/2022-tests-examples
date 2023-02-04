#include <saas/rtyserver_test/testerlib/search_checker.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/library/daemon_base/actions_engine/controller_script.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/rtyserver/common/common_messages.h>

#include <saas/util/bomb.h>

#include <util/folder/filelist.h>
#include <util/system/sysstat.h>

void MakeIndexReadOnly(const TString& index) {
    TFileEntitiesList fl(TFileEntitiesList::EM_FILES_DIRS);
    fl.Fill(index, TStringBuf(), TStringBuf(), 100);
    while (const char * filename = fl.Next()) {
        if (IsDir(index + "/" + TString(filename))) {
            Chmod((index + "/" + TString(filename)).c_str(), 0555);
        } else {
            Chmod((index + "/" + TString(filename)).c_str(), 0444);
        }
    }
}

START_TEST_DEFINE(TestReadOnly)
    bool Run() override {

        TFsPath path("/read_only/index/");
        PrepareData(path);
        TString index = GetIndexDir();
        MakeIndexReadOnly(index);
        Controller->RestartServer();

        CHECK_TEST_EQ(QueryCount(), 100);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        (*ConfigDiff)["DDKManager"] = DDK_COMPONENT_NAME;
        (*ConfigDiff)["IsReadOnly"] = true;
        (*ConfigDiff)["Indexer.Common.Enabled"] = false;
        (*ConfigDiff)["Merger.Enabled"] = false;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        return true;
    }
};

START_TEST_DEFINE(TestReadOnlyFlat)
    bool Run() override {

        TFsPath path("/read_only/flat/index/");
        PrepareData(path);
        TString index = GetIndexDir();
        MakeIndexReadOnly(index);
        Controller->RestartServer();

        CHECK_TEST_EQ(QueryCount(), 100);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_FLAT";
        (*ConfigDiff)["DDKManager"] = DDK_COMPONENT_NAME;
        (*ConfigDiff)["IsReadOnly"] = true;
        (*ConfigDiff)["Indexer.Common.Enabled"] = false;
        (*ConfigDiff)["Merger.Enabled"] = false;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        return true;
    }
};

START_TEST_DEFINE(TestReadOnlyNoDDK)
    bool Run() override {

        TFsPath path("/read_only/no_ddk/");
        Controller->ProcessCommand("stop");
        PrepareData(path);
        TString index = GetIndexDir();
        MakeIndexReadOnly(index);
        Controller->RestartServer();

        CHECK_TEST_EQ(QueryCount(), 100);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        (*ConfigDiff)["DDKManager"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["IsReadOnly"] = true;
        (*ConfigDiff)["Indexer.Common.Enabled"] = false;
        (*ConfigDiff)["Merger.Enabled"] = false;
        (*ConfigDiff)["ComponentsConfig.DDK.Enabled"] = false;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        return true;
    }
};

START_TEST_DEFINE(TestReadOnlyMAKEUP)
    bool Run() override {

        TFsPath path("/read_only/index/");
        Controller->ProcessCommand("stop");
        PrepareData(path);
        TString index = GetIndexDir();
        MakeIndexReadOnly(index);
        Controller->RestartServer();

        CHECK_TEST_EQ(QueryCount(), 100);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        (*ConfigDiff)["DDKManager"] = DDK_COMPONENT_NAME;
        (*ConfigDiff)["IsReadOnly"] = true;
        (*ConfigDiff)["Indexer.Common.Enabled"] = false;
        (*ConfigDiff)["Merger.Enabled"] = false;
        (*ConfigDiff)["Components"] = "MAKEUP";
        return true;
    }
};

START_TEST_DEFINE(TestReadOnlyFastArc)
    bool Run() override {

        TFsPath path("/read_only/index/");
        Controller->ProcessCommand("stop");
        PrepareData(path);
        TString index = GetIndexDir();
        MakeIndexReadOnly(index);
        Controller->RestartServer();

        CHECK_TEST_EQ(QueryCount(), 100);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        (*ConfigDiff)["DDKManager"] = DDK_COMPONENT_NAME;
        (*ConfigDiff)["IsReadOnly"] = true;
        (*ConfigDiff)["Indexer.Common.Enabled"] = false;
        (*ConfigDiff)["Merger.Enabled"] = false;
        (*ConfigDiff)["Components"] = "FASTARC";
        return true;
    }
};

START_TEST_DEFINE(TestReadOnlySuggest)
    bool Run() override {

        TFsPath path("/read_only/index/");
        Controller->ProcessCommand("stop");
        PrepareData(path);
        TString index = GetIndexDir();
        MakeIndexReadOnly(index);
        Controller->RestartServer();

        CHECK_TEST_EQ(QueryCount(), 100);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        (*ConfigDiff)["DDKManager"] = DDK_COMPONENT_NAME;
        (*ConfigDiff)["IsReadOnly"] = true;
        (*ConfigDiff)["Indexer.Common.Enabled"] = false;
        (*ConfigDiff)["Merger.Enabled"] = false;
        (*ConfigDiff)["Components"] = "Suggest";
        return true;
    }
};
//INDEX,MAKEUP,DDK,FASTARC,FULLARC,Suggest,Keys
