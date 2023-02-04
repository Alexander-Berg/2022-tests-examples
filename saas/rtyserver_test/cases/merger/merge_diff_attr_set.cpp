#include "merger_test.h"

START_TEST_DEFINE_PARENT(TestMergeWithDifferentAttrsSet, TMergerTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        ui32 maxSegments = GetMergerMaxSegments();
        VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
        VERIFY_WITH_LOG(!IsMergerTimeCheck(), "timed check must be off for test");
        const int messagesPerIndex = 3;
        const int step = messagesPerIndex * maxSegments;
        TAttrMap mids;
        TAttrMap::value_type attrValues;
        for (int i = 0; i < messagesPerIndex; ++i) {
            attrValues["attr1"] = i;
            attrValues["attr2"] = i + step;
            mids.push_back(attrValues);
        }

        attrValues.clear();
        for (int i = 0; i < messagesPerIndex; ++i) {
            if (i) {
                attrValues["attr1"] = i + step * 2;
            }
            attrValues["attr2"] = i + step * 3;
            mids.push_back(attrValues);
        }

        attrValues.clear();
        for (int i = 0; i < messagesPerIndex; ++i) {
            attrValues["attr1"] = i + step * 4;
            mids.push_back(attrValues);
        }

        for (unsigned i = 0; i < messagesPerIndex * (maxSegments - 2); ++i) {
            mids.push_back(TAttrMap::value_type());
        }

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesPerIndex * (maxSegments + 1), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), mids);
        for (unsigned i = 0; i < maxSegments + 1; ++i) {
            IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), REALTIME, 1);
            ReopenIndexers();
        }
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        CheckMergerResult();
        CheckSearchResults(messages);
        TVector<TDocSearchInfo> results;
        const bool isPrefixed = GetIsPrefixed();
        for (size_t i = 0; i < messages.size(); ++i) {
            const TString& url = messages[i].GetDocument().GetUrl();
            const TString& keyprefix = isPrefixed ? ("&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix())) : "";
            TString query = "body" + keyprefix;
            if (!mids[i].empty()) {
                query += "&fa=";
                for (TAttrMap::value_type::const_iterator iter = mids[i].begin(), end = mids[i].end(); iter != end; ++iter) {
                    if (iter != mids[i].begin()) {
                        query += ';';
                    }
                    query += iter->first + ":" + iter->second.Value;
                }
            }
            QuerySearch(query, results);
            const size_t requiredResults = isPrefixed ? 1 :
                (i < messagesPerIndex * 3 ? 1 : mids.size());
            if (results.size() != requiredResults) {
                ythrow yexception() << "results count is " << results.size() << " for url: " << url;
            } else if (requiredResults == 1 && results.front().GetUrl() != url) {
                ythrow yexception() << "Expected url: " << url << ", got: " << results.front().GetUrl();
            }
        }
        return true;
    }
public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        SetIndexerParams(REALTIME, 100);
        SetMergerParams(true, 2, -1, mcpNONE);
        return true;
    }
};
