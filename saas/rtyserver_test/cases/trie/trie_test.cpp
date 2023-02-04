#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

#include <saas/rtyserver/config/const.h>
#include <saas/rtyserver/components/trie/config.h>

#include <search/idl/meta.pb.h>

#include <kernel/saas_trie/idl/saas_trie.pb.h>
#include <kernel/saas_trie/idl/trie_key.h>

#include <tuple>

namespace {
    const TString ATTRIBUTE_NAME = "v";

    auto CreateMessages(const TVector<std::tuple<TString, TString, int>>& data) {
        TVector<NRTYServer::TMessage> messages(data.size());
        TString key;
        TString value;
        int kps;
        for (size_t i = 0, imax = messages.size(); i < imax; ++i) {
            std::tie(key, value, kps) = data[i];
            messages[i] = CreateSimpleKVMessage(key, value, ATTRIBUTE_NAME, kps);
        }
        return messages;
    }

    auto SplitAwayMessages(TVector<NRTYServer::TMessage>& source) {
        size_t half = source.size() / 2;
        TVector<NRTYServer::TMessage> result(source.begin() + half, source.end());
        source.resize(half);
        return result;
    }

    auto FindMessage(const TVector<NRTYServer::TMessage>& messages, const TString& url) {
        TVector<NRTYServer::TMessage> result;
        for (auto& message : messages) {
            if (message.GetDocument().GetUrl() == url) {
                result.push_back(message);
                break;
            }
        }
        return result;
    }

    TString MakeSimpleQuery(int kps, int max_docs = 100, TStringBuf key = "key") {
        TString query = TString::Join("/?text=", key);
        if (kps >= 0) {
            query += "&sgkps=" + ToString(kps);
        }
        query += "&component=" + NRTYServer::TrieComponentName +
                 "&comp_search=comp:" + NRTYServer::TrieComponentName + ";max_docs:" + ToString(max_docs) +
                 "&ms=proto&skip-wizard=1&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=yes";

        return query;
    }

    TVector<NRTYServer::TMessage> GetMessagesForSimpleKey(bool withKps) {
        TVector<NRTYServer::TMessage> messages;
        if (withKps) {
            messages = CreateMessages({
                {"key2", "value2", 1},
                {"key3", "value11", 3},
                {"key1", "value1", 1},
                {"key4", "value22", 3}
            });
        } else {
            messages = CreateMessages({
                {"key1", "value1", -1},
                {"other_key1", "value11", -1},
                {"other_key2", "value22", -1},
                {"key2", "value2", -1}
            });
        }
        return messages;
    }

    TVector<std::pair<TString, TString>> GetResponseForSimpleKey() {
        return {
            {"key1", "value1"},
            {"key2", "value2"}
        };
    }

    NSaasTrie::TComplexKey MakeComplexKey(int kps) {
        NSaasTrie::TComplexKey complexKey;
        if (kps >= 0) {
            complexKey.SetKeyPrefix(kps);
        }
        complexKey.SetMainKey("line");
        complexKey.AddKeyRealms("color");
        complexKey.AddKeyRealms("style");
        auto color = complexKey.AddAllRealms();
        color->SetName("color");
        color->AddKey("/red");
        color->AddKey("/green");
        auto style = complexKey.AddAllRealms();
        style->SetName("style");
        style->AddKey("/solid");
        style->AddKey("/dashed");
        return complexKey;
    }

    std::pair<NSaasTrie::TComplexKey, NSaasTrie::TComplexKey> MakeComplexKey2(int kps) {
        NSaasTrie::TComplexKey complexKey;
        NSaasTrie::TComplexKey complexKey2;
        if (kps >= 0) {
            complexKey.SetKeyPrefix(kps);
            complexKey2.SetKeyPrefix(kps);
        }
        complexKey.SetMainKey("line");
        complexKey.AddKeyRealms("color");
        complexKey.AddKeyRealms("style");
        auto color = complexKey.AddAllRealms();
        color->SetName("color");
        color->AddKey("/green");
        auto style = complexKey.AddAllRealms();
        style->SetName("style");
        style->AddKey("/solid");
        style->AddKey("/dashed");

        complexKey2.SetMainKey("line");
        complexKey2.AddKeyRealms("color");
        complexKey2.AddKeyRealms("style");
        auto color2 = complexKey2.AddAllRealms();
        color2->SetName("color");
        color2->AddKey("/red");
        auto style2 = complexKey2.AddAllRealms();
        style2->SetName("style");
        style2->AddKey("/solid");
        style2->AddKey("/dashed");
        return {std::move(complexKey), std::move(complexKey2)};
    }

    TString MakeComplexQuery(int kps) {
        auto complexKey = MakeComplexKey(kps);
        TString query = "/?text=" + NSaasTrie::SerializeToCgi(complexKey, false) + "&"
                       "component=" + NRTYServer::TrieComponentName + "&"
                       "comp_search=comp:" + NRTYServer::TrieComponentName + ";max_docs:100;key_type:complex_key&"
                       "ms=proto&skip-wizard=1&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=yes";

        return query;
    }

    TString MakeComplexQueryPacked(const NSaasTrie::TComplexKey& complexKey) {
        TString query = "/?text=" + NSaasTrie::SerializeToCgi(complexKey, true) + "&"
                       "component=" + NRTYServer::TrieComponentName + "&"
                       "comp_search=comp:" + NRTYServer::TrieComponentName + ";max_docs:100;key_type:complex_key_packed"
                       "&ms=proto&skip-wizard=1&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=yes";

        return query;
    }

    TString MakeComplexQueryPacked2(const std::pair<NSaasTrie::TComplexKey, NSaasTrie::TComplexKey>& key) {
        TString query = "/?text=" + NSaasTrie::SerializeToCgi(key.first, true) + "&text=" + NSaasTrie::SerializeToCgi(key.second, true) + "&"
                       "component=" + NRTYServer::TrieComponentName + "&"
                       "comp_search=comp:" + NRTYServer::TrieComponentName + ";max_docs:100;key_type:complex_key_packed"
                       "&ms=proto&skip-wizard=1&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=yes";

        return query;
    }

    TString MakeComplexQueryPacked(int kps) {
        return MakeComplexQueryPacked(MakeComplexKey(kps));
    }

    TVector<NRTYServer::TMessage> GetMessagesForComplexKey(bool withKps) {
        TVector<NRTYServer::TMessage> messages;
        if (withKps) {
            messages = CreateMessages({
                {"line/red/solid", "1", 1},
                {"line/red/dashed", "2", 2},
                {"line/red/dotted", "3", 1},
                {"line/green/solid", "4", 2},
                {"line/green/dashed", "5", 1},
                {"line/green/dotted", "6", 1},
                {"line/blue/solid", "7", 1},
                {"line/blue/dashed", "8", 1},
                {"line/blue/dotted", "9", 1}
            });
        } else {
            messages = CreateMessages({
                {"line/red/solid", "1", -1},
                {"line/red/dotted", "3", -1},
                {"line/green/dashed", "5", -1},
                {"line/green/dotted", "6", -1},
                {"line/blue/solid", "7", -1},
                {"line/blue/dashed", "8", -1},
                {"line/blue/dotted", "9", -1}
            });
        }
        return messages;
    }

    TVector<std::pair<TString, TString>> GetResponseForComplexKey() {
        return {
            {"line/green/dashed", "5"},
            {"line/red/solid", "1"}
        };
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(TrieComponentTestBase)
    bool InitConfig() override {
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = NRTYServer::TrieComponentName;
        (*ConfigDiff)["Merger.Enabled"] = true;
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = false;
        TweakConfig();
        return true;
    }

    virtual void TweakConfig() {
    }

protected:
    bool DoSearch(const TString& query, TVector<std::pair<TString, TString>>& actualDocs) {
        TString rawResult;
        ui32 backendResponseCode;
        int cnt = 5;
        actualDocs.clear();
        do {
            backendResponseCode = Controller->ProcessQuery(query, &rawResult, {}, 0, true, nullptr);
            if (backendResponseCode == 404) {
                return true;
            }
        } while (backendResponseCode != 200 || --cnt > 0);
        CHECK_TEST_EQ(backendResponseCode, 200);

        NMetaProtocol::TReport report;
        if (!report.ParseFromString(rawResult)) {
            TEST_FAILED("Can not parse backend response");
        }
        {
            TString textReport;
            ::google::protobuf::TextFormat::PrintToString(report, &textReport);
            DEBUG_LOG << "--- Search response for query " << query << '\n';
            DEBUG_LOG << textReport << '\n';
            DEBUG_LOG << "---" << Endl;
        }
        if (report.GroupingSize() == 0) {
            return true;
        }
        auto& grouping = report.GetGrouping(0);
        for (auto& group : grouping.GetGroup()) {
            CHECK_TEST_EQ(group.DocumentSize(), 1);
            auto& document = group.GetDocument(0);
            auto& archive = document.GetArchiveInfo();
            for (auto& attr : archive.GetGtaRelatedAttribute()) {
                if (attr.GetKey() == ATTRIBUTE_NAME) {
                    actualDocs.emplace_back(document.GetUrl(), attr.GetValue());
                }
            }
        }
        Sort(actualDocs, [](auto& a, auto& b) {
            return a.first < b.first;
        });
        return true;
    }
    bool TestSearch(const TString& query, const TVector<std::pair<TString, TString>>& expectedDocs) {
        TVector<std::pair<TString, TString>> actualDocs;
        if (!DoSearch(query, actualDocs)) {
            return false;
        }
        CHECK_TEST_TRUE(actualDocs == expectedDocs);
        return true;
    }
    bool TestSearchResponseSize(const TString& query, size_t expectedSize) {
        TVector<std::pair<TString, TString>> actualDocs;
        if (!DoSearch(query, actualDocs)) {
            return false;
        }
        CHECK_TEST_EQ(actualDocs.size(), expectedSize);
        return true;
    }
    bool RemoveLastDocument(const TVector<NRTYServer::TMessage>& allMessages,
                            TRTYServerTestCase::TIndexerType indexerType,
                            const TString& searchUrl,
                            TVector<std::pair<TString, TString>>& expectedResponse) {
        CHECK_TEST_TRUE(expectedResponse.size() > 1);
        TVector<NRTYServer::TMessage> toRemove = FindMessage(allMessages, expectedResponse.back().first);
        CHECK_TEST_TRUE(!toRemove.empty());
        toRemove.front().SetMessageType(NRTYServer::TMessage::DELETE_DOCUMENT);
        toRemove.front().SetMessageId(IMessageGenerator::CreateMessageId());
        IndexMessages(toRemove, indexerType, 1);
        if (indexerType != REALTIME) {
            ReopenIndexers();
        }

        expectedResponse.resize(expectedResponse.size() - 1);
        return TestSearch(searchUrl, expectedResponse);
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMemorySimple, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForSimpleKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskSimple, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForSimpleKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskSimple2, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1, 100, "key1&text=key2");
        return TestSearch(query, GetResponseForSimpleKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMemoryComplex, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForComplexKey(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);
        auto query = MakeComplexQuery(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForComplexKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskComplex, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForComplexKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeComplexQuery(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForComplexKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMemoryComplexKeyPacked, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForComplexKey(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);
        auto query = MakeComplexQueryPacked(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForComplexKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskComplexKeyPacked, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForComplexKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeComplexQueryPacked(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForComplexKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskComplexKeyPacked2, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForComplexKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeComplexQueryPacked2(MakeComplexKey2(GetIsPrefixed() ? 1 : -1));
        return TestSearch(query, GetResponseForComplexKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskComplexUnsorted, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForComplexKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeComplexQueryPacked(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForComplexKey());
    }

    void TweakConfig() override {
        (*ConfigDiff)["ComponentsConfig.TRIE.SortComplexKey"] = false;
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskComplexLastDimensionUnique, TrieComponentTestBase)
    bool Run() override {
        if (GetIsPrefixed()) {
            return true;
        }
        TVector<NRTYServer::TMessage> messages;
        messages = CreateMessages({
            {"line\tred\tsolid", "1", -1},
            {"line\tred\tdotted", "3", -1},
            {"line\tgreen\tdashed", "5", -1},
            {"line\tgreen\tdotted", "6", -1},
            {"line\tblue\tsolid", "7", -1},
            {"line\tblue\tdashed", "8", -1},
            {"line\tblue\tdotted", "9", -1}
        });
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        NSaasTrie::TComplexKey complexKey;
        complexKey.SetMainKey("line");
        complexKey.AddKeyRealms("color");
        complexKey.AddKeyRealms("style");
        auto color = complexKey.AddAllRealms();
        color->SetName("color");
        color->AddKey("\tred");
        color->AddKey("\tgreen");
        color->AddKey("\tblue");
        auto style = complexKey.AddAllRealms();
        style->SetName("style");
        style->AddKey("\tsolid");
        style->AddKey("\tdotted");

        TVector<std::pair<TString, TString>> expectedResponse = {
            {"line\tblue\tsolid", "7"},
            {"line\tgreen\tdotted", "6"},
            {"line\tred\tsolid", "1"}
        };

        auto query = MakeComplexQueryPacked(complexKey);
        return TestSearch(query, expectedResponse);
    }

    void TweakConfig() override {
        (*ConfigDiff)["ComponentsConfig.TRIE.SortComplexKey"] = false;
        (*ConfigDiff)["ComponentsConfig.TRIE.UniqueDimension"] = "style";
        (*ConfigDiff)["ComponentsConfig.TRIE.PropertyPrefix"] = ATTRIBUTE_NAME;
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskComplexLastDimensionUnique2, TrieComponentTestBase)
    bool Run() override {
        if (GetIsPrefixed()) {
            return true;
        }
        TVector<NRTYServer::TMessage> messages;
        messages = CreateMessages({
            {"line\tred\tsolid", "1", -1},
            {"line\tred\tdotted", "3", -1},
            {"line\tgreen\tdashed", "5", -1},
            {"line\tgreen\tdotted", "6", -1},
            {"line\tblue\tsolid", "7", -1},
            {"line\tblue\tdashed", "8", -1},
            {"line\tblue\tdotted", "9", -1}
        });
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        NSaasTrie::TComplexKey complexKey;
        complexKey.SetMainKey("line");
        complexKey.AddKeyRealms("color");
        complexKey.AddKeyRealms("style");
        auto color = complexKey.AddAllRealms();
        color->SetName("color");
        color->AddKey("\tred");
        color->AddKey("\tgreen");
        color->AddKey("\tblue");
        auto style = complexKey.AddAllRealms();
        style->SetName("style");
        style->AddKey("\tsolid");
        style->AddKey("\tdotted");
        complexKey.SetLastRealmUnique(true);

        TVector<std::pair<TString, TString>> expectedResponse = {
            {"line\tblue\tsolid", "7"},
            {"line\tgreen\tdotted", "6"},
            {"line\tred\tsolid", "1"}
        };

        auto query = MakeComplexQueryPacked(complexKey);
        return TestSearch(query, expectedResponse);
    }

    void TweakConfig() override {
        (*ConfigDiff)["ComponentsConfig.TRIE.SortComplexKey"] = false;
        (*ConfigDiff)["ComponentsConfig.TRIE.PropertyPrefix"] = ATTRIBUTE_NAME;
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchDiskParallelSearch, TrieComponentTestBase)
    bool Run() override {
        if (GetIsPrefixed()) {
            return true;
        }
        auto messages = CreateMessages({
            {"line/red/solid", "1", -1},
            {"line/red/dotted", "3", -1},
            {"line/green/dashed", "5", -1},
            {"line/green/dotted", "6", -1},
            {"line/blue/solid", "7", -1},
            {"line/blue/dashed", "8", -1},
            {"line/blue/dotted", "9", -1}
        });
        for (auto& message : messages) {
            TVector<NRTYServer::TMessage> singleMessage{1, message};
            IndexMessages(singleMessage, DISK, 1);
            ReopenIndexers();
        }

        TVector<std::pair<TString, TString>> expectedResponse = {
            {"line/blue/dashed", "8"},
            {"line/blue/dotted", "9"},
            {"line/blue/solid", "7"},
            {"line/green/dashed", "5"},
            {"line/green/dotted", "6"},
            {"line/red/dotted", "3"},
            {"line/red/solid", "1"}
        };

        auto query = MakeSimpleQuery(-1, 100, "line/");
        return TestSearch(query, expectedResponse);
    }

    void TweakConfig() override {
        (*ConfigDiff)["ComponentsConfig.TRIE.SearchThreads"] = 16;
        (*ConfigDiff)["Merger.Enabled"] = false;
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMetaTwoDisks, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        auto messages2 = SplitAwayMessages(messages);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForSimpleKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMetaDiskMemory, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        auto messages2 = SplitAwayMessages(messages);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        IndexMessages(messages2, REALTIME, 1);
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return TestSearch(query, GetResponseForSimpleKey());
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMaxDocs, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1, /*max_docs*/1);
        return TestSearchResponseSize(query, 1);
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMetaMaxDocs, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        auto messages2 = SplitAwayMessages(messages);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1, /*max_docs*/1);
        return TestSearchResponseSize(query, 1);
    }
};

START_TEST_DEFINE_PARENT(TestTrieSearchMetaMaxDocs2, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        auto messages2 = SplitAwayMessages(messages);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        IndexMessages(messages2, DISK, 1);
        ReopenIndexers();
        auto query = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1, /*max_docs*/2);
        return TestSearchResponseSize(query, 2);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveDocsMemory, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);

        auto response = GetResponseForSimpleKey();
        auto searchQuery = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return RemoveLastDocument(messages, REALTIME, searchQuery, response);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveDocsDisk, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        auto response = GetResponseForSimpleKey();
        auto searchQuery = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return RemoveLastDocument(messages, REALTIME, searchQuery, response);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveDocsDiskDisk, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        auto response = GetResponseForSimpleKey();
        auto searchQuery = MakeSimpleQuery(GetIsPrefixed() ? 1 : -1);
        return RemoveLastDocument(messages, DISK, searchQuery, response);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveKpsMemory, TrieComponentTestBase)
    bool Run() override {
        if (!GetIsPrefixed()) {
            return true;
        }

        auto messages = GetMessagesForSimpleKey(true);
        IndexMessages(messages, REALTIME, 1);

        if (!TestSearchResponseSize(MakeSimpleQuery(3), 2)) {
            return false;
        }
        if (!TestSearchResponseSize(MakeSimpleQuery(1), 2)) {
            return false;
        }
        DeleteSpecial(3);
        if (!TestSearchResponseSize(MakeSimpleQuery(3), 0)) {
            return false;
        }
        return TestSearchResponseSize(MakeSimpleQuery(1), 2);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveKpsDisk, TrieComponentTestBase)
    bool Run() override {
        if (!GetIsPrefixed()) {
            return true;
        }

        auto messages = GetMessagesForSimpleKey(true);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        if (!TestSearchResponseSize(MakeSimpleQuery(3), 2)) {
            return false;
        }
        if (!TestSearchResponseSize(MakeSimpleQuery(1), 2)) {
            return false;
        }
        DeleteSpecial(3);
        if (!TestSearchResponseSize(MakeSimpleQuery(3), 0)) {
            return false;
        }
        return TestSearchResponseSize(MakeSimpleQuery(1), 2);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveAllMemory, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);

        if (!TestSearchResponseSize(MakeSimpleQuery(GetIsPrefixed() ? 1 : -1), 2)) {
            return false;
        }
        DeleteSpecial();
        return TestSearchResponseSize(MakeSimpleQuery(GetIsPrefixed() ? 1 : -1), 0);
    }
};

START_TEST_DEFINE_PARENT(TestTrieRemoveAllDisk, TrieComponentTestBase)
    bool Run() override {
        auto messages = GetMessagesForSimpleKey(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        if (!TestSearchResponseSize(MakeSimpleQuery(GetIsPrefixed() ? 1 : -1), 2)) {
            return false;
        }
        DeleteSpecial();
        return TestSearchResponseSize(MakeSimpleQuery(GetIsPrefixed() ? 1 : -1), 0);
    }
};
