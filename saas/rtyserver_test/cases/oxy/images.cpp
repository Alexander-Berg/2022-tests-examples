#include "oxy.h"

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/kiwi_export/export.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/util/json/json.h>
#include <saas/rtyserver/common/common_messages.h>

#include <search/fresh/factors/factors_gen/factors_gen.h>
#include <kernel/web_factors_info/factors_gen.h>

#include <google/protobuf/text_format.h>
#include <util/system/event.h>

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestExportImagesBase, TestOxygenDocs)
bool RunTest(const TString& docDir, ui32 docCount) {
    if (GetIsPrefixed())
        return true;

    GenerateDocs(docDir, NRTYServer::TMessage::MODIFY_DOCUMENT, docCount, false, true, 0);
    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();

    TQuerySearchContext context;
    context.ResultCountRequirement = docCount;
    context.AttemptionsCount = 10;
    context.PrintResult = true;

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off&relev=attr_limit=9999999&relev=imgqf%3D6%7CCKm4KxDxquUBGMaVVCD7mQIwtMsBOO7bAUDUDkj1D1AqWBJgl8kDaPjUA3AteBSAASeIAReQAUSYAWygAcPsA6gBv-sDsAEkwAHQ6gM%2C", results, context);
    CHECK_TEST_EQ(results.size(), docCount);

    Controller->ProcessCommand("restart");

    QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off&relev=attr_limit=9999999&relev=imgqf%3D6%7CCKm4KxDxquUBGMaVVCD7mQIwtMsBOO7bAUDUDkj1D1AqWBJgl8kDaPjUA3AteBSAASeIAReQAUSYAWygAcPsA6gBv-sDsAEkwAHQ6gM%2C", results, context);
    CHECK_TEST_EQ(results.size(), docCount);

    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/images/ImagesOxygenOptions.cfg";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "false";
    (*ConfigDiff)["Searcher.ExternalSearch"] = "imagesearch";
    (*ConfigDiff)["Searcher.ArchivePolicy"] = "MAPMEM";
    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/images/base.cfg";

    return true;
}
};

START_TEST_DEFINE_PARENT(TestExportImages, TestExportImagesBase)
bool Run() override {
    return RunTest("images/docs", 100);
}
};

START_TEST_DEFINE_PARENT(TestExportImagesOneDoc, TestExportImagesBase)
bool Run() override {
    return RunTest("images/docs_one", 1);
}
};

START_TEST_DEFINE_PARENT(TestExportImagesMissed, TestExportImagesBase)
bool Run() override {
    return RunTest("images/docs_missed", 1);
}
};
