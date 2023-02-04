#include "oxy.h"

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/kiwi_export/export.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/util/json/json.h>
#include <saas/rtyserver/common/common_messages.h>
#include <saas/api/object_context/serialize.h>

#include <extsearch/video/kernel/erf/erf_format.h>
#include <extsearch/video/kernel/protobuf/writer.h>

#include <search/fresh/factors/factors_gen/factors_gen.h>
#include <kernel/web_factors_info/factors_gen.h>

#include <yweb/robot/erf/mosaic/meta/metadata.pb.h>

#include <google/protobuf/text_format.h>
#include <util/datetime/base.h>
#include <util/system/event.h>

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestExportVideoBase, TestOxygenDocs)
bool RunDocs(const TString& docs, size_t count = 100) {
    if (GetIsPrefixed())
        return true;

    GenerateDocs(docs, NRTYServer::TMessage::MODIFY_DOCUMENT, count, false, true, 0);
    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();

    TQuerySearchContext context;
    context.ResultCountRequirement = count;
    context.AttemptionsCount = 10;
    context.PrintResult = true;

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off", results, context);
    CHECK_TEST_EQ(results.size(), count);

    Controller->ProcessCommand("restart");

    QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off", results, context);
    CHECK_TEST_EQ(results.size(), count);

    return true;
}

bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;

    SetMergerParams(true, 1, -1, mcpNEWINDEX);

    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/video/VideoOxygenOptions.cfg";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Indexer.Disk.MaxDocuments"] = "30";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["Searcher.ExternalSearch"] = "videosearch";
    (*ConfigDiff)["Searcher.DefaultBaseSearchConfig"] = GetResourcesDirectory() + "/video/base.cfg";

    return true;
}

};

START_TEST_DEFINE_PARENT(TestExportVideo, TestExportVideoBase)

const TCateg COUNTRY_CODE = 225;
const TString LOCALE = "ru";

bool CheckQuerySearch(TString query, bool isVideotop, const TVector<TString>& resultUrls) {
    TQuerySearchContext context;
    context.ResultCountRequirement = resultUrls.size();
    context.AttemptionsCount = 10;
    context.PrintResult = false;

    TString reqId = ToString(TInstant::Now().MicroSeconds()) + "-1525125557442223806588817-2-039";
    query += "&reqid=" + reqId;
    if (isVideotop) {
        query += "&relev=relev_locale=" + LOCALE;
        query += "&relev=vtop";
        query += "&relev=vtop_fastviews=0";
        query += "&relev=vtop_wrong_face=0";
        query += "&relev=vtop_modtime=1";
        query += "&relev=vtop_age=0";
        query += "&relev=vtopsharesboost=0";
    }

    TVector<TDocSearchInfo> results;
    QuerySearch(query, results, context);

    CHECK_TEST_LESSEQ(resultUrls.size(), results.size());
    for (size_t i = 0; i < resultUrls.size(); ++i) {
        CHECK_TEST_EQ(results[i].GetUrl(), resultUrls[i]);
    }
    return true;
}

bool IndexContextForUrl(const TString& url, const NOxygen::TObjectContext& context) {
    NRTYServer::TMessage message;
    message.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);

    NRTYServer::TMessage::TDocument* doc = message.MutableDocument();
    doc->SetUrl(url);
    doc->MutableIndexedDoc()->SetUrl(url);
    doc->SetUpdateType(NRTYServer::TMessage::FAST);
    doc->MutableIndexedDoc()->SetKiwiObject(NOxygen::Serialize(context));
    const TVector<NRTYServer::TReply>& replies = IndexMessages({message}, REALTIME, 1, TDuration::Max().MilliSeconds(), true, true);
    for (const auto& reply : replies) {
        CHECK_TEST_EQ(reply.GetStatus(), 0);
    }

    return true;
}

bool SetDocDateForUrl(const TString& url, const TInstant& docDate) {
    NOxygen::TObjectContext context(NOxygen::TObjectContext::Empty);

    NErfMosaic::TRecordPatch patch;
    patch.SetRecordSignature(NErf::TStaticReflection<NVideo::TDocErf>::SIGNATURE);

    auto* fieldPatch = patch.AddFieldPatch();
    fieldPatch->SetOffset(NErf::TStaticReflection<NVideo::TDocErf>::DocDate().Offset);
    fieldPatch->SetWidth(NErf::TStaticReflection<NVideo::TDocErf>::DocDate().Width);
    fieldPatch->SetValue(docDate.Seconds());

    context.AddAttr("VideoErf2Features", NVideo::TProtoWriter::ToStringBinary(patch));

    return IndexContextForUrl(url, context);
}

bool Run() override {
    size_t firstCount = 102;
    if (!RunDocs("video/docs", firstCount))
        return false;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestExportVideoAnn, TestExportVideoBase)
bool Run() override {
    return RunDocs("video/anndocs");
}
bool InitConfig() override {
    if (!TestExportVideoBase::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/video/VideoFreshOxygenOptions.cfg";
    return true;
}
};


START_TEST_DEFINE_PARENT(TestExportVideoBrokenUserdata, TestExportVideoBase)
bool Run() override {
    return RunDocs("video/brokenuserdata", 1);
}
bool InitConfig() override {
    if (!TestExportVideoBase::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/video/VideoFreshOxygenOptions.cfg";
    return true;
}
};


START_TEST_DEFINE_PARENT(TestExportVideoPanther, TestExportVideoBase)
bool Run() override {
    return RunDocs("video/pantherdocs", 10);
}
bool InitConfig() override {
    if (!TestExportVideoBase::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/video/VideoPantherOxygenOptions.cfg";
    return true;
}
};
