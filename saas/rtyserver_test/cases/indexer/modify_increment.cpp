#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestMODIFY_INCREMENT)
    void Test(TIndexerType indexer, bool reopen, bool check) {
        const int countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
        TString kps;
        if (GetIsPrefixed()) {
            sdg->SetPrefixConstant(1);
            kps = "&kps=1";
        }

        TStandartAttributesFiller* saf = new TStandartAttributesFiller();
        saf->AddCommonAttribute("mid", "add:2", TStandartAttributesFiller::atGroup);
        sdg->RegisterFiller("mid", saf);
        sdg->SetTextConstant("");
        TStandartMessagesGenerator smg(sdg, true);
        smg.SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
        GenerateInput(messages, countMessages, smg);
        for (int i = 0; i < 3; ++i) {
            IndexMessages(messages, indexer, 1);
            if (reopen)
                ReopenIndexers();
            if (check) {
                TVector<TDocSearchInfo> results;
                QuerySearch("url:\"*\"&fa=mid:" + ToString((i + 1) * 2) + kps, results);
                if (results.size() != countMessages)
                    ythrow yexception() << "attr does not increment for " << i << " iteration";
            }
        }
    }

    bool InutConfig() {
        (*ConfigDiff)["Components"] = "INDEX";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMODIFY_INCREMENT_DISK, TestMODIFY_INCREMENT)
    bool Run() override {
        Test(DISK, true, true);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMODIFY_INCREMENT_MEMORY, TestMODIFY_INCREMENT)
    bool Run() override {
        Test(REALTIME, false, true);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMODIFY_INCREMENT_TEMP, TestMODIFY_INCREMENT)
    bool Run() override {
        Test(DISK, false, false);
        ReopenIndexers();
        TString kps;
        if (GetIsPrefixed())
            kps = "&kps=1";
        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"*\"&fa=mid:6" + kps, results);
        if (results.size() != 10)
            ythrow yexception() << "attr does not increment for 2 iteration";
        return true;
    }
};
