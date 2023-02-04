#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/charset/wide.h>
#include <util/string/vector.h>
#include <util/string/split.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestSnippetsHelper)
protected:
    TString Kps;
protected:
    TVector<NRTYServer::TMessage> IndexTexts(const TVector<TString>& texts, TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const bool isPrefixed = GetIsPrefixed();
        GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
        for (unsigned i = 0; i < texts.size(); ++i) {
            messages[i].MutableDocument()->SetBody(WideToUTF8(CharToWide(texts[i], csYandex)));
            if (isPrefixed)
                messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        Kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
        return messages;
    }
    void CheckQuery(const TString& query, int count, TString snipTest, TString headlineTest = "") {
        TVector<TString> snips;
        StringSplitter(snipTest.data()).Split('|').SkipEmpty().Collect(&snips);
        TVector<TString> heads;
        StringSplitter(headlineTest.data()).Split('|').SkipEmpty().Collect(&heads);
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        QuerySearch(query + Kps, results, &docProperties);
        if (results.ysize() != count) {
            ythrow yexception() << "Query '" << query + Kps << "'results count incorrect: " << results.ysize() << " != " << count;
        }
        if (count == 0)
            return;
        if (!!headlineTest) {
            THashMultiMap<TString, TString>::const_iterator iter = docProperties[0]->find("__headline");
            if (iter != docProperties[0]->end()) {
                TString snpResult = iter->second;
                for (ui32 i = 0; i < heads.size(); i++) {
                    TString snp = heads[i];
                    if (snp[0] == '+') {
                        size_t find = snpResult.find(snp.substr(1));
                        if (find != TString::npos) {
                            snpResult.replace(find, snp.size() - 1, "");
                        } else
                            ythrow yexception() << "headline not found: " << snp;
                    } else if (snp[0] == '-') {
                        size_t find = snpResult.find(snp.substr(1));
                        if (find != TString::npos) {
                            ythrow yexception() << "incorrect headline found: " << snp;
                        }
                    } else
                        VERIFY_WITH_LOG(false, "incorrect test script");
                }
            } else
                ythrow yexception() << "no headline info";
        }
        THashMultiMap<TString, TString>::const_iterator iter = docProperties[0]->find("__passage");
        if (!snipTest && !!iter->second)
            ythrow yexception() << "no empty passage";
        else if (iter != docProperties[0]->end()) {
            TString snpResult = iter->second;
            for (ui32 i = 0; i < snips.size(); i++) {
                TString snp = snips[i];
                if (snp[0] == '+') {
                    size_t find = snpResult.find(snp.substr(1));
                    if (find != TString::npos) {
                        snpResult.replace(find, snp.size() - 1, "");
                    } else
                        ythrow yexception() << "passage not found: " << snp;
                } else if (snp[0] == '-') {
                    size_t find = snpResult.find(snp.substr(1));
                    if (find != TString::npos) {
                        ythrow yexception() << "incorrect passage found: " << snp;
                    }
                } else
                    VERIFY_WITH_LOG(false, "incorrect test script");
            }
        } else
            ythrow yexception() << "no passage info";

    }
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestSnippetsBase, TestSnippetsHelper)
protected:
    void CheckFinalSnippets() {
        CheckQuery("gagaga", 1, "+gagaga");
        CheckQuery("gygygy", 1, "+gygygy");
        CheckQuery("kostya", 1, "+kostya|+was|+here");
        CheckQuery("vasya gagaga", 1, "+gagaga|-vasya");
        CheckQuery("kostya gagaga", 1, "+kostya|+was|+here|+gagaga");
    }

    void IndexDocuments() {
        TVector<TString> texts;
        texts.push_back(
            "<xml><no_snip><zonebbb>gagaga</zonebbb></no_snip><aaa>gygygy</aaa><no_snip1><aaa>kostya was here</aaa><zonebbb>vasya was here</zonebbb></no_snip1><hhh>http://r-a-p-i-d-s-h-a-r-e-.-c-o-m-/-f-i-l-e-s-/-1-7-6-2-0-9-0-2-/-b-a-r-a-k-a-r-s-u-y-l-e-b-i-r-g-e-e-r-z-a-m-a-m-k-i-3-0-b-l-m-.-m-p-3- . -h-t-m-l</hhh><denied_zone>vasya</denied_zone></xml>");

        TVector<NRTYServer::TMessage> messages;
        const bool isPrefixed = GetIsPrefixed();
        GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
        for (unsigned i = 0; i < texts.size(); ++i) {
            messages[i].MutableDocument()->SetBody(texts[i]);
            messages[i].MutableDocument()->SetMimeType("text/xml");
            if (isPrefixed) {
                messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
                Kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
            }
        }
        IndexMessages(messages, REALTIME, 1);

        CheckQuery("gagaga", 1, "", "+gygygy");
        CheckQuery("gygygy", 1, "+gygygy");
        CheckQuery("kostya", 1, "", "+gygygy");
        CheckQuery("vasya gagaga", 1, "");
        CheckQuery("vasya gygygy", 1, "+gygygy|-vasya");

        ReopenIndexers();
        texts[0] =
            "<xml><aaa>gygygy</aaa><zonebbb>gagaga</zonebbb><aaa>kostya was here</aaa><no_snip1><zonebbb>vasya was here</zonebbb></no_snip1><denied_zone>kostya</denied_zone></xml>";

        messages[0].MutableDocument()->SetBody(WideToUTF8(CharToWide(texts[0], csYandex)));
        messages[0].SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
        messages[0].SetMessageId(IMessageGenerator::CreateMessageId());
        IndexMessages(messages, REALTIME, 1);
    }
};

START_TEST_DEFINE_PARENT(TestSnippets, TestSnippetsBase)
bool Run() override {
    IndexDocuments();
    CheckFinalSnippets();

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
    return true;
}
};


START_TEST_DEFINE_PARENT(TestBadSnippets, TestSnippetsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back(
        "<xml><no_snip><zonebbb>gagaga</zonebbb></no_snip><no_snip1><aaa>kostya was here</aaa><zonebbb>vasya was here</zonebbb></no_snip1><hhh>http://r-a-p-i-d-s-h-a-r-e-.-c-o-m-/-f-i-l-e-s-/-1-7-6-2-0-9-0-2-/-b-a-r-a-k-a-r-s-u-y-l-e-b-i-r-g-e-e-r-z-a-m-a-m-k-i-3-0-b-l-m-.-m-p-3-.</hhh><denied_zone>vasya</denied_zone></xml>");

    TVector<NRTYServer::TMessage> messages;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    for (unsigned i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetBody(texts[i]);
        messages[i].MutableDocument()->SetMimeType("text/xml");
        if (isPrefixed) {
            messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            Kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
        }
    }
    IndexMessages(messages, REALTIME, 1);
    CheckQuery("gagaga", 0, "", "");
    CheckQuery("gagaga&rearr=RTYCleanSpecial_off", 1, "", "");
    ReopenIndexers();
    CheckQuery("gagaga", 0, "", "");
    CheckQuery("gagaga&rearr=RTYCleanSpecial_off", 1, "", "");
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.ReArrangeOptions"] = "RTYCleanSpecial";

    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "RTYCleanSpecial";
    (*SPConfigDiff)["Service.MetaSearch.MorphologyLanguages"] = "ukr,kaz,tur,en,rus";
    (*SPConfigDiff)["Service.MetaSearch.PreferedMorphologyLanguages"] = "ukr,kaz,tur,en,rus";
    return true;
}
};


START_TEST_DEFINE_PARENT(TestSearchArchiveMultipartNormalization, TestSnippetsBase)
bool Run() override {
    IndexDocuments();
    CheckFinalSnippets();
    (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
    Controller->ApplyConfigDiff(ConfigDiff);
    Controller->RestartServer();
    CheckFinalSnippets();
    (*ConfigDiff)["Searcher.ArchiveType"] = "AT_FLAT";
    Controller->ApplyConfigDiff(ConfigDiff);
    Controller->RestartServer();
    CheckFinalSnippets();
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestMultipartRepair)
virtual bool BreakPart(const TFsPath& path) = 0;

bool Run() override {
    using namespace NRTYArchive;

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 500, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TSet<TString> indexes = Controller->GetFinalIndexes();
    TMap<TString, NRTYArchive::TPartMetaInfo> partMetaInfos;


    Controller->ProcessCommand("stop");
    for (const auto& path : indexes) {
        TVector<ui32> partIndexes;
        TFsPath arcPrefix = path + "/indexarc";
        arcPrefix.Fix();
        NRTYArchive::TMultipartArchive::FillPartsIndexes(arcPrefix, partIndexes);
        for (auto& index : partIndexes) {
            TFsPath file = GetPartPath(arcPrefix, index);
            DEBUG_LOG << "FILE CHANGES: " << file.GetPath() << Endl;
            partMetaInfos[file.GetPath()] = TPartMetaSaver(GetPartMetaPath(arcPrefix, index)).GetProto();
            CHECK_TEST_TRUE(TPartMetaSaver(GetPartMetaPath(arcPrefix, index))->GetStage() == TPartMetaInfo::CLOSED);
            DEBUG_LOG << "Breaking " << GetPartPath(arcPrefix, index) << Endl;
            if (!BreakPart(GetPartPath(arcPrefix, index))) {
                return false;
            }
        }
    }
    Controller->RestartServer();

    ReopenIndexers();
    indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");

    ui64 totalParts = 0;
    for (const auto& path : indexes) {
        TVector<ui32> partIndexes;
        TFsPath arcPrefix = path + "/indexarc";
        arcPrefix.Fix();
        NRTYArchive::TMultipartArchive::FillPartsIndexes(arcPrefix, partIndexes);
        for (auto& index : partIndexes) {
            totalParts += 1;

            TFsPath file = GetPartPath(arcPrefix, index);
            DEBUG_LOG << "FILE: " << file.GetPath() << Endl;

            const auto& oldMetaIt = partMetaInfos.find(file.GetPath());
            CHECK_TEST_TRUE(oldMetaIt != partMetaInfos.end());
            const auto& oldMeta = oldMetaIt->second;
            const auto metaSaver = NRTYArchive::TPartMetaSaver(NRTYArchive::GetPartMetaPath(arcPrefix, index));
            const auto& newMeta = metaSaver.GetProto();
            CHECK_TEST_TRUE(newMeta.GetStage() == oldMeta.GetStage());
            CHECK_TEST_TRUE(newMeta.GetDocsCount() == oldMeta.GetDocsCount());
            CHECK_TEST_TRUE(newMeta.GetRemovedDocsCount() == oldMeta.GetRemovedDocsCount());
        }
    }
    CHECK_TEST_TRUE(totalParts == partMetaInfos.size());
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetMergerParams(false);
    (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMultipartRepairStage, TestMultipartRepair)
    bool BreakPart(const TFsPath& path) override {
        const auto& metaPath = NRTYArchive::GetPartMetaPath(path);
        CHECK_TEST_TRUE(metaPath.Exists());
        NRTYArchive::TPartMetaSaver(metaPath)->SetStage(NRTYArchive::TPartMetaInfo::OPENED);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMultipartRepairHdr, TestMultipartRepair)
    bool BreakPart(const TFsPath& path) override {
        const auto& hdrPath = NRTYArchive::GetPartHeaderPath(path);
        CHECK_TEST_TRUE(hdrPath.Exists());
        hdrPath.DeleteIfExists();
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMultipartRepairMeta, TestMultipartRepair)
    bool BreakPart(const TFsPath& path) override {
        const auto& metaPath = NRTYArchive::GetPartMetaPath(path);
        CHECK_TEST_TRUE(metaPath.Exists());
        metaPath.DeleteIfExists();
        return true;
    }
};

