#include "merger_test.h"

#include <saas/rtyserver/components/zones_makeup/read_write_makeup_manager.h>

START_TEST_DEFINE_PARENT(TestMergeZonesMakeup, TMergerTest, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<aaa>body</aaa>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<xml><aaa>body</aaa><bbb>big body</bbb></xml>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<bbb>small body</bbb>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<ggg>little body</ggg>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<xml><aa>test</aa><ggg>sss</ggg><sss>vvv</sss><ggg>little body</ggg></xml>", false);
    for (int i = 0; i < messages.ysize(); i++) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
    }
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    CheckSearchResults(messages);
    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
        (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 1, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestUnusedZonesCleanup , TMergerTest, TTestMarksPool::OneBackendOnly)

TVector<NRTYServer::TMessage> PrepareMessages() {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<aaa>body</aaa>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<xml><aaa>body</aaa><bbb>big body</bbb></xml>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<bbb>small body</bbb>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<ggg>little body</ggg>", false);
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<xml><aa>test</aa><ggg>sss</ggg><sss>vvv</sss><ggg>little body</ggg></xml>", false);
    for (int i = 0; i < messages.ysize(); i++) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
    }
    return messages;
}

bool Run() override {
    const TVector<NRTYServer::TMessage> messages1 = PrepareMessages();
    IndexMessages(messages1, REALTIME, 1);

    TVector<NRTYServer::TMessage> messageToRemove;
    GenerateInput(messageToRemove, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<zzz>something to remove</zzz>", false);
    messageToRemove[0].MutableDocument()->SetMimeType("text/xml");
    IndexMessages(messageToRemove, REALTIME, 1);
    ReopenIndexers();
    {
        auto rm = BuildDeleteMessage(messageToRemove[0]);
        TVector<NRTYServer::TMessage> removeMessages;
        removeMessages.push_back(rm);
        IndexMessages(removeMessages, REALTIME, 1);
    }
    ReopenIndexers();

    const TVector<NRTYServer::TMessage> messages2 = PrepareMessages();
    IndexMessages(messages2, REALTIME, 1);

    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    CheckSearchResults(messages1);
    CheckSearchResults(messages2);

    TRTYServerConfig config(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false));
    config.IsPrefixedIndex = GetIsPrefixed();

    const TSet<TString> finalIndexes = Controller->GetFinalIndexes();
    TSet<TString> zoneNames;
    for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
        TReadWriteRTYMakeupManager makeupManager(TPathName{*i}, config, false, true, false);
        makeupManager.Open();
        NZonesMakeup::IZonesDescription* headerPtr = makeupManager.GetZonesDescription();
        for (ui32 zoneIdx = 0; zoneIdx < headerPtr->GetZoneCount(); ++zoneIdx) {
            const TString& zoneName = headerPtr->GetZoneName(zoneIdx);
            zoneNames.insert(zoneName);
            NOTICE_LOG << "Found zone " << zoneName << Endl;
        }
        makeupManager.Close();
    }
    CHECK_TEST_TRUE(!zoneNames.contains("zzz"))
    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
        (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};
