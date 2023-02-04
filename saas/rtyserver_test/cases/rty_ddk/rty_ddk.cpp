#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/doc_info.h>

#include <saas/util/json/json.h>

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDDKPatchDocsBase, TRTYServerTestCase)

    bool CheckMessage(const NRTYServer::TMessage& message, ui32 deadline, ui32 version, ui32 timestamp, ui32 streamId) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;


        const TString indexGenerator = Controller->GetConfigValue("IndexGenerator", "Server");
        if (indexGenerator == FULL_ARCHIVE_COMPONENT_NAME) {
            QuerySearch(message.GetDocument().GetUrl() + "&sgkps=" + ToString(message.GetDocument().GetKeyPrefix()), results, ctx);
        } else {
            if (indexGenerator == INDEX_COMPONENT_NAME) {
                const TString kps = GetIsPrefixed() ? "&kps=" + ToString(message.GetDocument().GetKeyPrefix()) : TString();
                QuerySearch("url:\"" + message.GetDocument().GetUrl() + "\"" + kps, results, ctx);
            } else {
                ERROR_LOG << "Unknown indexGenerator: " << indexGenerator << Endl;
            }
        }

        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        const TDocSearchInfo& dsi = results[0];
        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
        DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
        TDocInfo di(*jsonDocInfoPtr);
        CHECK_TEST_EQ(di.GetDDKDocInfo()["Deadline"], deadline);
        CHECK_TEST_EQ(di.GetDDKDocInfo()["Version"], version);
        CHECK_TEST_EQ(di.GetDDKDocInfo()["Timestamp"], timestamp);
        CHECK_TEST_EQ(di.GetDDKDocInfo()["StreamId"], streamId);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDDKPatchDocsSlowUpdates, TestDDKPatchDocsBase)

    bool Run() override {

        const time_t secs = Seconds();
        const ui64 deadlineMinutesUTC = secs / 60 + 10000;
        ui32 version = 1;
        ui32 timestamp = 2;
        ui32 streamId = 0;

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        NRTYServer::TMessage& message = messages.front();

        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        message.MutableDocument()->SetStreamId(streamId);

        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        message.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        message.MutableDocument()->ClearDeadlineMinutesUTC();
        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        message.MutableDocument()->SetDeadlineMinutesUTC(0);

        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, 0, version, timestamp, streamId), true);

        return true;
    }

    bool InitConfig() override {
        if (!TRTYServerTestCase::InitConfig())
            return false;
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDDKPatchDocsFastUpdates, TestDDKPatchDocsBase)

    bool Run() override {

        const time_t secs = Seconds();
        const ui64 deadlineMinutesUTC = secs / 60 + 10000;
        ui32 version = 1;
        ui32 timestamp = 2;
        ui32 streamId = 0;

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        NRTYServer::TMessage& message = messages.front();

        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        message.MutableDocument()->SetStreamId(streamId);

        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        message.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        message.MutableDocument()->SetUpdateType(NRTYServer::TMessage::FAST);
        message.MutableDocument()->ClearBody();
        message.MutableDocument()->ClearSearchAttributes();
        message.MutableDocument()->ClearGroupAttributes();
        message.MutableDocument()->ClearDocumentProperties();

        message.MutableDocument()->ClearDeadlineMinutesUTC();
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        message.MutableDocument()->SetDeadlineMinutesUTC(0);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, 0, version, timestamp, streamId), true);

        return true;
    }

    bool InitConfig() override {
        if (!TRTYServerTestCase::InitConfig())
            return false;
        // Disabling FullArc
        SetEnabledRepair(false);
        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDDKPatchDocsSlowUpdatesWithZeroTs, TestDDKPatchDocsBase)

    bool Run() override {

        const ui32 DefaultLifetimeMinutes = FromString<ui32>(Controller->GetConfigValue("ComponentsConfig.DDK.DefaultLifetimeMinutes", "Server"));

        ui32 version = 1;
        ui32 timestamp = Seconds() - 30 * 60;
        ui64 deadlineMinutesUTC = timestamp / 60 + DefaultLifetimeMinutes;
        ui32 streamId = 0;

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        NRTYServer::TMessage& message = messages.front();

        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        message.MutableDocument()->SetStreamId(streamId);

        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        message.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        version = 2;
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(0);
        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        version = 3;
        deadlineMinutesUTC = Seconds() + 10000;
        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(0);
        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        version = 4;
        message.MutableDocument()->ClearDeadlineMinutesUTC();
        timestamp = Seconds() - 20 * 60;
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        version = 5;
        deadlineMinutesUTC = Seconds() + 20000;
        timestamp = Seconds() - 10 * 60;
        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        IndexMessages(messages, REALTIME, 1);
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        return true;
    }

    bool InitConfig() override {
        if (!TRTYServerTestCase::InitConfig())
            return false;
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
        (*ConfigDiff)["ComponentsConfig.DDK.DefaultLifetimeMinutes"] = 60;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDDKPatchDocsFastUpdatesWithZeroTs, TestDDKPatchDocsBase)

    bool Run() override {

        const ui32 DefaultLifetimeMinutes = FromString<ui32>(Controller->GetConfigValue("ComponentsConfig.DDK.DefaultLifetimeMinutes", "Server"));

        ui32 version = 1;
        ui32 timestamp = Seconds() - 30 * 60;
        ui64 deadlineMinutesUTC = timestamp / 60 + DefaultLifetimeMinutes;
        ui32 streamId = 0;

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        NRTYServer::TMessage& message = messages.front();

        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        message.MutableDocument()->SetStreamId(streamId);

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        message.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        message.MutableDocument()->SetUpdateType(NRTYServer::TMessage::FAST);
        message.MutableDocument()->ClearBody();
        message.MutableDocument()->ClearSearchAttributes();
        message.MutableDocument()->ClearGroupAttributes();
        message.MutableDocument()->ClearDocumentProperties();

        version = 2;
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(0);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        version = 3;
        deadlineMinutesUTC = Seconds() + 10000;
        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(0);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        version = 4;
        message.MutableDocument()->ClearDeadlineMinutesUTC();
        timestamp = Seconds() - 20 * 60;
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        version = 5;
        deadlineMinutesUTC = Seconds() + 20000;
        timestamp = Seconds() - 10 * 60;
        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        return true;
    }

    bool InitConfig() override {
        if (!TRTYServerTestCase::InitConfig())
            return false;
        // Disabling FullArc
        SetEnabledRepair(false);
        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
        (*ConfigDiff)["ComponentsConfig.DDK.DefaultLifetimeMinutes"] = 60;
        return true;
    }
};


START_TEST_DEFINE_PARENT(TestDDKExportedGta, TestDDKPatchDocsBase)

    bool Run() override {
        ui32 version = 567;
        ui32 timestamp = 2;
        ui32 streamId = 0;
        const time_t secs = Seconds();
        const ui64 deadlineMinutesUTC = secs / 60 + 10000;

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        NRTYServer::TMessage& message = messages.front();

        message.MutableDocument()->SetVersion(version);
        message.MutableDocument()->SetModificationTimestamp(timestamp);
        message.MutableDocument()->SetStreamId(streamId);
        message.MutableDocument()->SetDeadlineMinutesUTC(deadlineMinutesUTC);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        // pre-check that the DDK info is correct
        CHECK_TEST_EQ(CheckMessage(message, deadlineMinutesUTC, version, timestamp, streamId), true);

        TStringBuf gta("_DDK_Version");
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> docProps;
        QuerySearch("url:\"" + message.GetDocument().GetUrl() + "\"&fsgta=" + gta + GetAllKps(messages), results, &docProps, 0, true);

        CHECK_TEST_EQ(1u, results.size());
        CHECK_TEST_EQ(1u, docProps.size());
        const auto& prop = docProps[0];
        auto i = prop->find(gta);
        CHECK_TEST_TRUE(i != prop->end());
        CHECK_TEST_EQ("567", i->second);

        return true;
    }

    bool InitConfig() override {
        if (!TRTYServerTestCase::InitConfig())
            return false;

        SetEnabledRepair(false);
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/ddk_test.cfg";
        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        return true;
    }
};

