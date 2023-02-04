#include "merger_test.h"
#include <util/folder/filelist.h>


START_TEST_DEFINE_PARENT(TestDeadlineForDocs, TMergerTest)

bool Test(i32 delta) {
    TVector<NRTYServer::TMessage> messagesD, messagesR;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messagesD, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);
    GenerateInput(messagesR, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "realtime", false);

    time_t secs = Seconds();

    for (unsigned i = 0; i < messagesD.size(); ++i) {
        if (isPrefixed)
            messagesD[i].MutableDocument()->SetKeyPrefix(messagesD.front().GetDocument().GetKeyPrefix());
        messagesD[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 + delta);
    }

    for (unsigned i = 0; i < messagesR.size(); ++i) {
        if (isPrefixed)
            messagesR[i].MutableDocument()->SetKeyPrefix(messagesR.front().GetDocument().GetKeyPrefix());
        messagesR[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 + delta);
    }

    TString KpsD = "&kps=" + ToString(messagesD.front().GetDocument().GetKeyPrefix());
    TString KpsR = "&kps=" + ToString(messagesR.front().GetDocument().GetKeyPrefix());
    if (delta > 0) {
        IndexMessages(messagesD, DISK, 1);
    } else {
        MUST_BE_BROKEN(IndexMessages(messagesD, DISK, 1));
    }
    ReopenIndexers();
    if (delta > 0) {
        IndexMessages(messagesR, REALTIME, 1);
    } else {
        MUST_BE_BROKEN(IndexMessages(messagesR, REALTIME, 1));
    }

    TVector<TDocSearchInfo> results;
    QuerySearch("disk" + KpsD, results);
    CHECK_TEST_EQ(delta > 0, (bool)results.size());

    QuerySearch("realtime" + KpsR, results);
    CHECK_TEST_EQ(delta > 0, (bool)results.size());

    Sleep(TDuration::Seconds(10));

    QuerySearch("disk" + KpsD, results);
    CHECK_TEST_EQ(delta > 0, (bool)results.size());

    QuerySearch("realtime" + KpsR, results);
    CHECK_TEST_EQ(delta > 0, (bool)results.size());

    if (delta > 0) {
        Sleep(TDuration::Minutes(delta));
    }

    QuerySearch("realtime" + KpsR, results);
    CHECK_TEST_EQ(0u, (ui32)results.size());

    QuerySearch("disk" + KpsD, results);
    CHECK_TEST_EQ(0u, (ui32)results.size());

    return true;
}

bool Run() override {
    CHECK_TEST_TRUE(Test(-1));
    CHECK_TEST_TRUE(Test(2));
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["DeadDocsClearIntervalSeconds"] = 2;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeadlineForDocsOffset, TMergerTest)

bool Test() {
    TVector<NRTYServer::TMessage> messagesD, messagesR;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messagesD, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);
    GenerateInput(messagesR, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "realtime", false);

    time_t secs = Seconds();

    for (unsigned i = 0; i < messagesD.size(); ++i) {
        if (isPrefixed)
            messagesD[i].MutableDocument()->SetKeyPrefix(messagesD.front().GetDocument().GetKeyPrefix());
        messagesD[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 - 2);
    }

    for (unsigned i = 0; i < messagesR.size(); ++i) {
        if (isPrefixed)
            messagesR[i].MutableDocument()->SetKeyPrefix(messagesR.front().GetDocument().GetKeyPrefix());
        messagesR[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 - 2);
    }

    TString KpsD = "&kps=" + ToString(messagesD.front().GetDocument().GetKeyPrefix());
    TString KpsR = "&kps=" + ToString(messagesR.front().GetDocument().GetKeyPrefix());

    IndexMessages(messagesD, DISK, 1);
    ReopenIndexers();
    IndexMessages(messagesR, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("disk" + KpsD, results);
    if (results.ysize() != 3)
        ythrow yexception() << "failed case AD: " << results.ysize();

    QuerySearch("realtime" + KpsR, results);
    if (results.ysize() != 3)
        ythrow yexception() << "failed case AR: " << results.ysize();

    Sleep(TDuration::Minutes(2));

    QuerySearch("disk" + KpsD, results);
    if (results.ysize() != 3)
        ythrow yexception() << "failed case BD: " << results.ysize();

    QuerySearch("realtime" + KpsR, results);
    if (results.ysize() != 3)
        ythrow yexception() << "failed case BR: " << results.ysize();

    return true;
}

bool Run() override {
    return Test();
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["ComponentsConfig.DDK.LifetimeMinutesOffset"] = 1000;
        (*ConfigDiff)["DeadDocsClearIntervalSeconds"] = 2;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeadlineForDocsCountByDeadline, TMergerTest)

bool Run() override {
    TVector<NRTYServer::TMessage> messagesD, messagesR;
    const bool isPrefixed = GetIsPrefixed();
    const int shardsNumber = GetShardsNumber();
    GenerateInput(messagesD, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);
    GenerateInput(messagesR, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "realtime", false);

    time_t secs = Seconds();

    // Deadlines: D_DL<1>, D_DL<3>, D_DL<5>
    for (unsigned i = 0; i < messagesD.size(); ++i) {
        if (isPrefixed)
            messagesD[i].MutableDocument()->SetKeyPrefix(messagesD.front().GetDocument().GetKeyPrefix());
        messagesD[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 + 1000 + 2 * i + 1);
    }

    // Deadlines: R_DL<4>, R_DL<6>, R_DL<8>
    for (unsigned i = 0; i < messagesR.size(); ++i) {
        if (isPrefixed)
            messagesR[i].MutableDocument()->SetKeyPrefix(messagesD.front().GetDocument().GetKeyPrefix() + shardsNumber);
        messagesR[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 + 1000 + 2 * (i + 2));
    }

    TString KpsD = "&kps=" + ToString(messagesD.front().GetDocument().GetKeyPrefix());
    TString KpsR = "&kps=" + ToString(messagesR.front().GetDocument().GetKeyPrefix());
    TString Kps = "&kps=" + ToString(messagesR.front().GetDocument().GetKeyPrefix()) + "," + ToString(messagesD.front().GetDocument().GetKeyPrefix());

    IndexMessages(messagesD, DISK, 1);
    ReopenIndexers();
    IndexMessages(messagesR, REALTIME, 1);

    TQuerySearchContext searchContext;
    searchContext.AttemptionsCount = 10;
    searchContext.ResultCountRequirement = 3;
    TVector<TDocSearchInfo> results;
    QuerySearch("disk" + KpsD, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    QuerySearch("realtime" + KpsR, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    ReopenIndexers();

    QuerySearch("disk" + KpsD, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    QuerySearch("realtime" + KpsR, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=true");
    CheckMergerResult();
    // Merged index by deadline:
    //   R_DL<4>, D_DL<5>, R_DL<6>, R_DL<8>
    Sleep(TDuration::Seconds(10));

    searchContext.ResultCountRequirement = 1;
    QuerySearch("disk" + KpsD, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 1);

    searchContext.ResultCountRequirement = 3;
    QuerySearch("realtime" + KpsR, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    searchContext.ResultCountRequirement = 4;
    QuerySearch("url:\"*\"" + Kps, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 4);

    return true;
}

public:
    bool InitConfig() override {
        SetPruneAttrSort("");
        (*ConfigDiff)["merger.MaxDeadlineDocs"] = 4;
        (*ConfigDiff)["indexer.disk.MaxDocuments"] = 3;
        (*ConfigDiff)["DeadDocsClearIntervalSeconds"] = 2;
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeadlineForDocsCountOnStart, TMergerTest)

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, 1000, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);

    if (isPrefixed) {
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(1);
        }
    }

    TString Kps = "&kps=" + (TString)(isPrefixed ? "1" : "0");
    ui64 oldTassValue = GetMaxDeadlineRemovedStats().GetOrElse(0);

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TQuerySearchContext searchContext;
    searchContext.AttemptionsCount = 10;
    searchContext.ResultCountRequirement = 1000;
    QuerySearch("url:\"*\"" + Kps, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 1000);

    (*ConfigDiff)["merger.MaxDeadlineDocs"] = 100;
    (*ConfigDiff)["indexer.disk.MaxDocuments"] = 99;
    ApplyConfig();

    searchContext.AttemptionsCount = 10;
    searchContext.ResultCountRequirement = 100;
    QuerySearch("url:\"*\"" + Kps, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 100);
    CHECK_TEST_EQ(*GetMaxDeadlineRemovedStats() - oldTassValue, 1000 - 100);

    return true;
}

public:
    bool InitConfig() override {
        SetPruneAttrSort("");
        (*ConfigDiff)["merger.MaxDeadlineDocs"] = 10000;
        (*ConfigDiff)["indexer.disk.MaxDocuments"] = 10000;
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeadlineForDocsCountOnly, TMergerTest)

bool Run() override {
    TVector<NRTYServer::TMessage> messagesD1, messagesD2, messagesD3;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messagesD1, 100, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);
    GenerateInput(messagesD2, 100, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);
    GenerateInput(messagesD3, 100, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);

    if (isPrefixed) {
        for (auto&& m : messagesD1) {
            m.MutableDocument()->SetKeyPrefix(1);
        }
        for (auto&& m : messagesD2) {
            m.MutableDocument()->SetKeyPrefix(1);
        }
        for (auto&& m : messagesD3) {
            m.MutableDocument()->SetKeyPrefix(1);
        }
    }

    TString Kps = "&kps=" + (TString)(isPrefixed ? "1" : "0");
    ui64 oldTassValue = GetMaxDeadlineRemovedStats().GetOrElse(0);

    IndexMessages(messagesD1, DISK, 1);
    ReopenIndexers();
    IndexMessages(messagesD2, DISK, 1);
    ReopenIndexers();
    IndexMessages(messagesD3, DISK, 1);
    ReopenIndexers();

    TSet<std::pair<ui64, TString> > deleted;

    DeleteSomeMessages(messagesD1, deleted, DISK, 2);
    DeleteSomeMessages(messagesD2, deleted, DISK, 2);
    DeleteSomeMessages(messagesD3, deleted, DISK, 2);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=true");
    CheckMergerResult();

    TVector<TDocSearchInfo> results;
    TQuerySearchContext searchContext;
    searchContext.AttemptionsCount = 10;
    searchContext.ResultCountRequirement = 101;
    QuerySearch("url:\"*\"" + Kps, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 101);
    CHECK_TEST_EQ(*GetMaxDeadlineRemovedStats() - oldTassValue, 300 - 300 / 2 - 101);

    return true;
}

public:
    bool InitConfig() override {
        SetPruneAttrSort("");
        (*ConfigDiff)["merger.MaxDeadlineDocs"] = 101;
        (*ConfigDiff)["indexer.disk.MaxDocuments"] = 101;
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeadlineForDocsCountByPruningRank, TMergerTest)

bool Test() {

    TVector<NRTYServer::TMessage> messagesD, messagesR;
    const bool isPrefixed = GetIsPrefixed();
    const int shardsNumber = GetShardsNumber();
    GenerateInput(messagesD, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);
    GenerateInput(messagesR, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "realtime", false);

    time_t secs = Seconds();

    // Deadlines:  D_DL<1>, D_DL<3>, D_DL<5>
    // PruneAttrs: D_PR[0], D_PR<1>, D_PR<2>
    for (unsigned i = 0; i < messagesD.size(); ++i) {
        if (isPrefixed)
            messagesD[i].MutableDocument()->SetKeyPrefix(messagesD.front().GetDocument().GetKeyPrefix());
        messagesD[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 + 1000 + 2 * i + 1);
        ::NRTYServer::TAttribute* attr = messagesD[i].MutableDocument()->AddGroupAttributes();
        attr->SetName("unique_attr");
        attr->SetValue(ToString(i));
        attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }

    // Deadlines: R_DL<4>, R_DL<6>, R_DL<8>
    // PruneAttrs: R_PR[0], R_PR<1>, R_PR<2>
    for (unsigned i = 0; i < messagesR.size(); ++i) {
        if (isPrefixed)
            messagesR[i].MutableDocument()->SetKeyPrefix(messagesD.front().GetDocument().GetKeyPrefix() + shardsNumber);
        messagesR[i].MutableDocument()->SetDeadlineMinutesUTC(secs / 60 + 1000 + 2 * (i + 2));
        ::NRTYServer::TAttribute* attr = messagesR[i].MutableDocument()->AddGroupAttributes();
        attr->SetName("unique_attr");
        attr->SetValue(ToString(i));
        attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }

    TString KpsD = "&kps=" + ToString(messagesD.front().GetDocument().GetKeyPrefix());
    TString KpsR = "&kps=" + ToString(messagesR.front().GetDocument().GetKeyPrefix());
    TString Kps = "&kps=" + ToString(messagesR.front().GetDocument().GetKeyPrefix()) + "," + ToString(messagesD.front().GetDocument().GetKeyPrefix());

    IndexMessages(messagesD, DISK, 1);
    ReopenIndexers();
    IndexMessages(messagesR, REALTIME, 1);

    TQuerySearchContext searchContext;
    searchContext.AttemptionsCount = 10;
    searchContext.ResultCountRequirement = 3;
    TVector<TDocSearchInfo> results;
    QuerySearch("disk" + KpsD, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    QuerySearch("realtime" + KpsR, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    ReopenIndexers();

    QuerySearch("disk" + KpsD, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    QuerySearch("realtime" + KpsR, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 3);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=true");
    CheckMergerResult();
    // Merged index by deadline:
    //   R_DL<4>, D_DL<5>, R_DL<6>, R_DL<8>
    Sleep(TDuration::Seconds(10));

    searchContext.ResultCountRequirement = 2;
    QuerySearch("disk" + KpsD, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 2);

    searchContext.ResultCountRequirement = 2;
    QuerySearch("realtime" + KpsR, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 2);

    searchContext.ResultCountRequirement = 4;
    QuerySearch("url:\"*\"" + Kps, results, searchContext);
    CHECK_TEST_EQ(results.ysize(), 4);

    return true;
}

bool Run() override {
    return Test();
}
public:
    bool InitConfig() override {
        SetPruneAttrSort("unique_attr");
        (*ConfigDiff)["merger.MaxDeadlineDocs"] = 4;
        (*ConfigDiff)["indexer.disk.MaxDocuments"] = 3;
        (*ConfigDiff)["DeadDocsClearIntervalSeconds"] = 2;
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDefaultDeadline, TMergerTest)
void Test() {
    TVector<NRTYServer::TMessage> messages;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "disk", false);

    time_t modifTs = (TInstant::Now() - TDuration::Minutes(2)).Seconds();
    for (unsigned i = 0; i < messages.size(); ++i) {
        if (isPrefixed) {
            messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
        }
        messages[i].MutableDocument()->SetModificationTimestamp(modifTs);
    }

    const TString kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());

    IndexMessages(messages, DISK, 1);
    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("disk" + kps, results);
    if (results.ysize() != messages.ysize())
        ythrow yexception() << "Documents do not indexed: " << results.ysize();

    Controller->ProcessCommand("stop");

    (*ConfigDiff)["ComponentsConfig.DDK.DefaultLifetimeMinutes"] = 3;
    ApplyConfig();
    Sleep(TDuration::Minutes(2));
    Controller->RestartServer();

    results.clear();
    QuerySearch("disk" + kps, results);
    if (results.ysize() != 0)
        ythrow yexception() << "Outdate documents do not deleted on start: " << results.ysize();

    modifTs = (TInstant::Now() - TDuration::Minutes(2)).Seconds();
    for (unsigned i = 0; i < messages.size(); ++i)
        messages[i].MutableDocument()->SetModificationTimestamp(modifTs);
    IndexMessages(messages, REALTIME, 1);

    results.clear();
    QuerySearch("disk" + kps, results);
    if (results.ysize() != messages.ysize())
        ythrow yexception() << "documents do not indexed: " << results.ysize();

    Sleep(TDuration::Minutes(1));

    TVector<NRTYServer::TMessage> aliveMessages;
    GenerateInput(aliveMessages, 2, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "alive", false);
    if (isPrefixed) {
        for (unsigned i = 0; i < aliveMessages.size(); ++i)
            aliveMessages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
    }
    IndexMessages(aliveMessages, REALTIME, 1);

    Sleep(TDuration::Minutes(1));

    QuerySearch("disk" + kps, results);
    if (results.ysize() != 0)
        ythrow yexception() << "Outdate documents do not deleted by merger: " << results.ysize();

    results.clear();
    QuerySearch("alive" + kps, results);
    if (results.ysize() != aliveMessages.ysize())
        ythrow yexception() << "Alive documents was deleted as outdated: " << results.ysize();
}

public:
    bool Run() override {
        Test();
        return true;
    }

    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE(TestIndexDeadDocuments)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        const bool isPrefixed = GetIsPrefixed();
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);

        time_t secs = Seconds();
        for (unsigned i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            messages[i].MutableDocument()->SetModificationTimestamp(secs - 120);
        }

        const TString kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());

        MUST_BE_BROKEN(IndexMessages(messages, REALTIME, 1));

        secs = Seconds();
        for (unsigned i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            messages[i].MutableDocument()->SetModificationTimestamp(secs + 60);
        }
        MUSTNT_BE_BROKEN(IndexMessages(messages, REALTIME, 1));

        TVector<TDocSearchInfo> results;
        QuerySearch("body" + kps, results);
        CHECK_TEST_NEQ(results.ysize(), 0);

        Sleep(TDuration::Minutes(3));

        TDirsList dirList;
        const char *dir = nullptr;
        dirList.Fill(GetIndexDir(), (TString)"index_");
        while ((dir = dirList.Next()) != nullptr) {
            CHECK_TEST_NEQ(Controller->IsFinalIndex(dir), true);
        }

        QuerySearch("body" + kps, results);
        CHECK_TEST_EQ(results.ysize(), 0);
        Controller->ProcessCommand("restart");
        QuerySearch("body" + kps, results);
        CHECK_TEST_EQ(results.ysize(), 0);
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["ComponentsConfig.DDK.DefaultLifetimeMinutes"] = 1;
        SetMergerParams(true, 1, -1, mcpNONE);
        return true;
    }
};
