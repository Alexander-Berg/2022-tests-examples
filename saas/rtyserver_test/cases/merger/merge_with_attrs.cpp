#include "merger_test.h"
#include <util/system/filemap.h>

START_TEST_DEFINE_PARENT(TestMergeWithAttrs, TMergerTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        ui32 maxSegments = GetMergerMaxSegments();
        VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
        VERIFY_WITH_LOG(!IsMergerTimeCheck(), "timed check must be off for test");
        ui32 messagesPerIndex = 2 * GetShardsNumber();
        TAttrMap mids;
        TAttrMap::value_type attrValues;
        for (ui64 i = messagesPerIndex * (maxSegments + 2); i--;) {
            attrValues["mid"] = i;
            mids.push_back(attrValues);
        }
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesPerIndex * (maxSegments + 2), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), mids);
        TSet<std::pair<ui64, TString> > deleted;
        for (unsigned i = 0; i < maxSegments + 2; ++i) {
            IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), REALTIME, 1);
            ReopenIndexers();
            switch (i) {
                case 1: {
                        TSet<std::pair<ui64, TString> > currentlyDeleted;
                        DeleteSomeMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex - 1), currentlyDeleted, REALTIME, 1);
                        deleted.insert(currentlyDeleted.begin(), currentlyDeleted.end());
                    }
                    break;
                case 2: {
                        TSet<std::pair<ui64, TString> > currentlyDeleted;
                        DeleteSomeMessages(TVector<NRTYServer::TMessage>(messages.begin() + (i + 1) * messagesPerIndex - 1, messages.begin() + (i + 1) * messagesPerIndex), currentlyDeleted, REALTIME, 1);
                        deleted.insert(currentlyDeleted.begin(), currentlyDeleted.end());
                    }
                    break;
                case 3: {
                        TSet<std::pair<ui64, TString> > currentlyDeleted;
                        DeleteSomeMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), currentlyDeleted, REALTIME, 1);
                        deleted.insert(currentlyDeleted.begin(), currentlyDeleted.end());
                    }
                    break;
                default:
                    break;
            }
        }
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        if (deleted.size() != messagesPerIndex * 2) {
            ythrow yexception() << "deleted messages count is " << deleted.size();
        }
        CheckMergerResult();
        CheckSearchResults(messages, deleted);
        TVector<TDocSearchInfo> results;
        for (TVector<NRTYServer::TMessage>::const_iterator iter = messages.begin(), end = messages.end(); iter != end; ++iter) {
            const TString& url = iter->GetDocument().GetUrl();
            const ui64 kps = iter->GetDocument().GetKeyPrefix();
            const bool removed = deleted.find(std::make_pair(kps, url)) != deleted.end();
            const TString& keyprefix = GetIsPrefixed() ? ("&kps=" + ToString(kps)) : "";
            const TString& attr = mids[iter - messages.begin()]["mid"].Value;
            QuerySearch("body" + keyprefix + "&fa=mid:" + attr, results);
            if (results.size() != !removed) {
                ythrow yexception() << "result size is " << results.size() << " for attr: " << attr;
            }
            if (results.size() == 1 && results.front().GetUrl() != url) {
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
