#include "oxy.h"

#include <saas/rtyserver_test/testerlib/standart_generator.h>
#include <saas/rtyserver/common/common_rty.h>
#include <saas/rtyserver/config/const.h>

#include <google/protobuf/text_format.h>
#include <ysite/yandex/erf_format/erf_format.h>
#include <robot/library/oxygen/indexer/tuple_value/tuple_value.h>

#include <util/folder/path.h>
#include <util/system/env.h>

namespace {
    bool IsDeletion(const NRealTime::TIndexedDoc& doc, bool readFromKiwiTuples) {
        if (readFromKiwiTuples) {
            if (doc.HasKiwiObject()) {
                const NKiwi::TKiwiObject& kiwiObj = (NKiwi::TKiwiObject)doc.GetKiwiObject();
                const NKiwi::NTuples::TTuple *tuple = kiwiObj.FindByLabel("HttpCode");
                if (tuple) {
                    ui32 httpCode = 0;
                    tuple->GetValue(httpCode);
                    return (httpCode == 404);
                }
                return false;
            } else {
                return false;
            }
        } else {
            return (doc.GetLogelType() == NRealTime::TIndexedDoc::UpdUrlLogel);
        }
    }
}

void TestOxygenDocs::GenerateDocs(const TString& path, NRTYServer::TMessage::TMessageType type, ui32 count, bool backOrder, bool checkDel, ui32 startPos, ui64 timestamp) {
    TStandartDocumentGenerator* dg = new TStandartDocumentGenerator(GetIsPrefixed());
    dg->SetTextConstant("");
    TStandartMessagesGenerator smg(dg, true);
    smg.SetMessageType(type);
    TVector<NRTYServer::TMessage> messages;
    for (ui32 i = startPos; true; ++i) {
        TString filePath = GetResourcesDirectory() + "/" + path + "/inddoc." + ToString(i);
        if (!TFsPath(filePath).Exists())
            break;
        DEBUG_LOG << "file " << filePath << " read for oxy-test" << Endl;
        TFileInput fi(filePath);
        TVector<NRTYServer::TMessage> msgs;
        GenerateInput(msgs, 1, smg);
        NRTYServer::TMessage& msg = msgs.back();
        msg.MutableDocument()->ClearBody();
        if (!::google::protobuf::TextFormat::ParseFromString(fi.ReadAll(), msg.MutableDocument()->MutableIndexedDoc()))
            ythrow yexception() << "cannot deserialize doc from file:" << i;
        if (!msg.GetDocument().GetIndexedDoc().GetUrl())
            ythrow yexception() << "there is no url for doc " << i;
        msg.MutableDocument()->SetUrl(msg.GetDocument().GetIndexedDoc().GetUrl());
        NOxygen::TObjectContext oc(msg.GetDocument().GetIndexedDoc().GetKiwiObject());
        NOxygen::TTupleNameSet names = oc.GetTupleLabels();
        THashSet<TString> tuplesForErase;
        bool modified = false;
        for (auto&& name : names) {
            if ((!SelectedTuples.empty() && !SelectedTuples.contains(name)) || ErasedTuples.contains(name)) {
                // if (name == "FreshErfInfo" && type == NRTYServer::TMessage::MODIFY_DOCUMENT)  - TFreshErfInfo removed (see commit)
                tuplesForErase.insert(name);
            } else {
                if (timestamp) {
                    auto attr = oc[name];
                    oc.Erase(name);
                    oc.AddAttr(name, attr->As<TString>(), NKwTupleMeta::AT_STRING, timestamp);
                    auto realTimestamp = oc[name]->GetTag();
                    if (realTimestamp != timestamp) {
                        ythrow yexception() << "incorrect tuples timestamp for: " << i << "/" << realTimestamp;
                    }
                    modified = true;
                }
            }
        }
        if (modified || !tuplesForErase.empty()) {
            if (!tuplesForErase.empty()) {
                oc.Erase(tuplesForErase);
            }
            TString kiwiObject;
            for (const auto& attr : oc) {
                NOxygen::TTupleAttrValuePtr tupleAttr = NOxygen::ConvertToTupleValue(attr);
                tupleAttr->AppendAsTuple(kiwiObject);
            }
            msg.MutableDocument()->MutableIndexedDoc()->SetKiwiObject(kiwiObject);
        }
        NOxygen::TObjectContext ocTest(msg.GetDocument().GetIndexedDoc().GetKiwiObject());
        for (auto&& i : ocTest.GetTupleLabels()) {
            auto realTimestamp = ocTest[i]->GetTag();
            if (timestamp && realTimestamp != timestamp)
                ythrow yexception() << "incorrect tuples timestamp for " << i << ":" << realTimestamp << " != " << timestamp;
            if (tuplesForErase.contains(i))
                ythrow yexception() << "incorrect tuples list after cleaner: " << i;
        }
        if (!checkDel || !IsDeletion(msg.GetDocument().GetIndexedDoc(), true)) {
            messages.push_back(msg);
            if (messages.size() >= count)
                break;
        }
    }
    if (backOrder)
        Messages.insert(Messages.end(), messages.rbegin(), messages.rend());
    else
        Messages.insert(Messages.end(), messages.begin(), messages.end());
}

void TestOxygenDocs::GetResponses(){
    ui16 port = Controller->GetConfig().Searcher.Port;
    SetEnv("SEARCH_PORT", ToString(port));
    TString pref = GetIsPrefixed() ? "_k_on" : "_k_off";
    SetEnv("PREF", pref);

    DEBUG_LOG << "Responses getting..." << Endl;
    if (!Callback->RunNode("get_responses"))
        ythrow yexception() << "fail to run node" << Endl;
    Callback->WaitNode("get_responses");
    DEBUG_LOG << "Responses getting... Ok" << Endl;
}

bool TestOxygenDocs::Finish(){
    if (GetSaveResponses)
        GetResponses();
    return TRTYServerTestCase::Finish();
}

bool TestOxygenDocs::InitConfig() {
    (*ConfigDiff)["IndexGenerator"] = "OXY";
    (*ConfigDiff)["PruneAttrSort"] = "oxy";
    (*ConfigDiff)["Components"] = "DDK";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptions.cfg";
    (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "0";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.ExternalSearch"] = "";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "FOR_MERGE,COMPLEMENT";
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersFilteredForIndex"] = NRTYServer::NFullArchive::FullLayer;
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersFilteredForMerge"] = "FOR_MERGE";
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersMergeComplement"] = "COMPLEMENT";
    SetMergerParams(true, 1, -1, TRTYServerTestCase::mcpNONE);
    MergeTuples.clear();
    MergeTuples.insert("keyinv");
    MergeTuples.insert("arc");
    return true;
}
