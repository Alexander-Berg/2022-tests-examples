#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

#include <saas/rtyserver/config/const.h>

#include <search/idl/meta.pb.h>

namespace {
    constexpr TDuration DefaultWaitTimeout = TDuration::Minutes(1);

    const TString ATTRIBUTE_NAME = "v";
    const TString KEY_PREFIX = "key_prefix_";
    const TString VALUE_PREFIX = "value_prefix_";

    auto GenerateDocs(size_t from, size_t to) {
        TVector<std::pair<TString, TString>> docs(to - from);
        for (size_t i = 0, imax = docs.size(); i < imax; ++i) {
            docs[i].first = TString::Join(KEY_PREFIX, ToString(from + i));
            docs[i].second = TString::Join(VALUE_PREFIX, ToString(from + i));
        }
        return docs;
    }

    auto CreateMessages(const TVector<std::pair<TString, TString>>& docs, size_t from, size_t to) {
        TVector<NRTYServer::TMessage> messages(to - from);
        for (size_t i = 0; i < messages.size(); ++i) {
            messages[i] = CreateSimpleKVMessage(docs[i + from].first, docs[i + from].second, ATTRIBUTE_NAME, -1);
            messages[i].MutableDocument()->SetRealtime(false);
        }
        return messages;
    }

    TString MakeQuery(TStringBuf key) {
        return TString::Join("/?text=", key, "&ms=proto&skip-wizard=1&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=yes&gta=_AllDocInfos");
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(TestFullArcCompressedExtBase)
protected:
    bool InitConfig() override {
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = "";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = false;
        (*ConfigDiff)["Indexer.Disk.Enabled"] = true;
        (*ConfigDiff)["Indexer.Disk.MaxDocuments"] = 1000000;
        (*ConfigDiff)["Indexer.Disk.TimeToLiveSec"] = 0;
        (*ConfigDiff)["Merger.Enabled"] = true;
        (*ConfigDiff)["Merger.MergerCheckPolicy"] = "TIME";
        (*ConfigDiff)["Merger.MaxSegments"] = 1;
        (*ConfigDiff)["Merger.TimingCheckIntervalMilliseconds"] = 86400;
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = false;
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.Compression"] = "COMPRESSED_EXT";
        TweakConfig();
        return true;
    }

    virtual void TweakConfig() {
    }

    // FIXME code below is "copy&paste" from cases/trie/trie_test.cpp - should be fixed
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
        size_t nGroups = grouping.GroupSize();

        for (size_t i = 0; i < nGroups; ++i) {
            auto& group = grouping.GetGroup(i);
            CHECK_TEST_EQ(group.DocumentSize(), 1);

            auto& document = group.GetDocument(0);
            auto& archive = document.GetArchiveInfo();
            CHECK_TEST_EQ(archive.GtaRelatedAttributeSize(), 1+4);
            auto& attr = archive.GetGtaRelatedAttribute(0);
            CHECK_TEST_EQ(attr.GetKey(), ATTRIBUTE_NAME);
            actualDocs.emplace_back(archive.GetTitle(), attr.GetValue());
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

    bool AddFinalIndex(const TVector<NRTYServer::TMessage>& messages, i64 newDocCount) {
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        return WaitForDocuments(newDocCount);
    }

    template<typename TStopCondition>
    bool WaitForSomething(TDuration timeout, TStopCondition&& shouldStop) const {
        if (shouldStop()) {
            return true;
        }
        TInstant t1 = TInstant::Now();
        TInstant t2 = t1;
        while (t2 - t1 < timeout) {
            Sleep(TDuration::Seconds(1));
            if (shouldStop()) {
                return true;
            }
            t2 = TInstant::Now();
        }
        return false;
    }

    bool WaitForDocuments(i64 targetDocCount) const {
        return WaitForSomething(DefaultWaitTimeout, [&]() {
            return CheckDocCount(targetDocCount);
        });
    }

    bool CheckDocCount(i64 targetDocCount) const {
        auto docCounts = GetDocCount();
        for (auto& docCount : docCounts) {
            if (docCount != targetDocCount) {
                return false;
            }
        }
        return true;
    }

    TVector<i64> GetDocCount() const {
        auto infoJson = this->GetInfoRequest();
        auto& rootArr = infoJson.GetArraySafe();
        TVector<i64> result(rootArr.size());
        for (size_t i = 0; i < result.size(); ++i) {
            result[i] = infoJson[i].GetMapSafe()["searchable_docs"].GetIntegerSafe();
        }
        return result;
    }

    bool WaitForFinalIndexes(size_t targetCount) const {
        return WaitForSomething(DefaultWaitTimeout, [&]() {
            return CheckFinalIndexes(targetCount);
        });
    }

    bool CheckFinalIndexes(size_t targetCount) const {
        TJsonPtr indexInfos = Controller->ProcessCommand("get_final_indexes&full_path=false");
        auto& infosArr = indexInfos->GetArraySafe();
        for (auto& info : infosArr) {
            auto indexCount = info.GetMapSafe()["dirs"].GetArraySafe().size();
            if (indexCount != targetCount) {
                return false;
            }
        }
        return true;
    }

    bool FindAllDocs(const TVector<std::pair<TString, TString>>& testDocs) {
        for (auto& doc : testDocs) {
            if (!TestSearch(MakeQuery(doc.first), {doc})) {
                return false;
            }
        }
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestFullArcCompressedExtSingleIndex, TestFullArcCompressedExtBase)
    bool Run() override {
        auto testDocs = GenerateDocs(0, 100);
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, 0, testDocs.size()), testDocs.size()));
        return FindAllDocs(testDocs);
    }
};

START_TEST_DEFINE_PARENT(TestFullArcCompressedExtTwoIndexes, TestFullArcCompressedExtBase)
    bool Run() override {
        const size_t limit = 100;
        auto testDocs = GenerateDocs(0, limit);
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, 0, limit / 2), limit / 2));
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, limit / 2, limit), testDocs.size()));
        return FindAllDocs(testDocs);
    }
};

START_TEST_DEFINE_PARENT(TestFullArcCompressedExtMerge, TestFullArcCompressedExtBase)
    bool Run() override {
        const size_t limit = 100;
        auto testDocs = GenerateDocs(0, limit);
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, 0, limit / 2), limit / 2));
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, limit / 2, limit), testDocs.size()));

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        CHECK_TEST_TRUE(WaitForFinalIndexes(1));

        return FindAllDocs(testDocs);
    }
};

START_TEST_DEFINE_PARENT(TestFullArcCompressedExtZstdSmallData, TestFullArcCompressedExtBase)
    bool Run() override {
        auto testDocs = GenerateDocs(0, 10);
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, 0, testDocs.size()), testDocs.size()));
        return FindAllDocs(testDocs);
    }

    void TweakConfig() override {
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.CodecName"] = "zstd08d-1";
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.BlockSize"] = 1024;
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.LearnSize"] = 262144;
    }
};

START_TEST_DEFINE_PARENT(TestFullArcCompressedExtZstdMediumSizeData, TestFullArcCompressedExtBase)
    const size_t LearnSampleSize = 20000;
    bool Run() override {
        auto testDocs = GenerateDocs(0, LearnSampleSize / 10);
        CHECK_TEST_TRUE(AddFinalIndex(CreateMessages(testDocs, 0, testDocs.size()), testDocs.size()));
        return FindAllDocs(testDocs);
    }

    void TweakConfig() override {
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.CodecName"] = "zstd08d-1";
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.BlockSize"] = 1024;
        (*ConfigDiff)["ComponentsConfig.FULLARC.Layers.full.CompressionExtParams.LearnSize"] = LearnSampleSize;
    }
};

