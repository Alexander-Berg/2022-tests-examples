#include "deploy_manager.h"
#include <util/folder/filelist.h>

//special 'test' for external usage, run cluster and do nothing
START_TEST_DEFINE_PARENT(SuspendIdleDM, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    TString path = GetResourcesDirectory();
    TFileList fl;
    fl.Fill(path);
    const char* fileName;
    const TString pathInDM = "configs/tests";
    while (fileName = fl.Next()) {
        INFO_LOG << "Upload file " << path + fileName << " -> " << pathInDM + "/" + fileName << Endl;
        Controller->UploadFileToDeployManager(path + fileName, pathInDM + "/" + fileName);
    }
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    DeployBe("tests");
    DeployIp();
    DeploySp();
}

bool Run() override {
    Cout << "search_port=" << Controller->GetConfig().Searcher.Port
         << ";indexer_port=" << Controller->GetConfig().Indexer.Port
         << ";dm_port=" << Controller->GetConfig().DeployManager.Port;
    Cout.Flush();
    while (true) {
        Sleep(TDuration::Seconds(5));
    }
    return true;
}
};
