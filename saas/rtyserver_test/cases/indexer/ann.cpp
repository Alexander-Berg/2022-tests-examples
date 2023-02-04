#include <saas/rtyserver_test/cases/indexer/ann.h>

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/rtyserver_test/util/factors_parsers.h>

#include <saas/rtyserver/components/ann/const.h>
#include <saas/rtyserver/components/ann/storage/iterator.h>
#include <saas/rtyserver/components/ann/storage/accessor.h>
#include <saas/api/factors_erf.h>
#include <saas/util/json/json.h>

#include <kernel/indexann_data/data.h>
#include <kernel/multipart_archive/owner.h>

using TArchiveOwner = NRTYArchive::TMultipartOwner<NRTYArchive::TMultipartArchive>;
using NSaas::TAnnFormats;
using NSaas::TDocFactorsView;

class TSingleDocAccessor : public NRTYAnn::IDocBlobAccessor {
    ui32 DocId;
    TBlob DocData;
public:
    TSingleDocAccessor(ui32 docId, TBlob docData)
        : DocId(docId)
        , DocData(docData)
    {
    }

    TBlob Get(ui32 docId) const override {
        Y_VERIFY(docId == DocId);
        return DocData;
    }
};


SERVICE_TEST_RTYSERVER_DEFINE(TestAnnFactorsHelper)
protected:
    THashMap<TString, double> Factors__;
    TDocFactorsView Factors;

protected:
    void GenerateMessages(TVector<NRTYServer::TMessage>& messages, bool setLang = true) {
        GenerateInput(messages, 4, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        {
            NRTYServer::TMessage::TDocument& mes = *messages[0].MutableDocument();
            mes.MutableAnnData()->Clear();
            mes.SetBody("Alice felt too sleepy to play, and there was nobody to play with. It was a hot afternoon, so she was sitting in the garden under a tree. Alice looked at the book. There were no pictures in the book, and Alice didn't like books without pictures. 'I think I'll go and pick some flowers', she said to herself. She began to get up, but she forgot about the flowers at once because she saw a rabbit.");

            NRTYServer::TMessage::TSentenceData& sent = *mes.MutableAnnData()->AddSentences();
            sent.SetText("Rabbits naturally spawn in plains, extreme hills, forest, taiga, swamp, jungle, birch forest, roofed forest, cold taiga, mega taiga, and savanna biomes.");
            if (setLang)
                sent.SetTextLanguage(2);
            NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
            NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
            NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.0196078), *stream);
            stream = reg.AddStreams();
            NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.05), *stream);
            stream = reg.AddStreams();
            NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.0002), *stream);
        }

        {
            NRTYServer::TMessage::TDocument& mes = *messages[1].MutableDocument();
            mes.MutableAnnData()->Clear();
            mes.SetBody("Алиса сидела со старшей сестрой на берегу и маялась: делать ей было совершенно нечего, а сидеть без дела, сами знаете, дело нелегкое; раз-другой она, правда, сунула нос в книгу, которую сестра читала, но там не оказалось ни картинок, ни стишков. 'Кому нужны книжки без картинок.- или хоть стишков, не понимаю!' - думала Алиса. С горя она начала подумывать (правда, сейчас это тоже было дело не из легких - от жары ее совсем разморило), что, конечно, неплохо бы сплести венок из маргариток, но плохо то, что тогда нужно подниматься и идти собирать эти маргаритки, как вдруг... Как вдруг совсем рядом появился белый кролик с розовыми глазками!");

            NRTYServer::TMessage::TSentenceData& sent = *mes.MutableAnnData()->AddSentences();
            sent.SetText("Кролики - это не только ценный мех, но и 3 - 4 килограмма диетического, легкоусвояемого мяса");
            if (setLang)
                sent.SetTextLanguage(1);
            {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.0196078), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.05), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.0002), *stream);
            }

            NRTYServer::TMessage::TSentenceData& sent2 = *mes.MutableAnnData()->AddSentences();
            sent2.SetText("Алиса любит маргаритки");
            if (setLang)
                sent2.SetTextLanguage(1);
            {
                NRTYServer::TMessage::TRegionData& reg = *sent2.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.0596088), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.05), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.0002), *stream);
            }
        }

        {
            NRTYServer::TMessage::TDocument& mes = *messages[2].MutableDocument();
            mes.MutableAnnData()->Clear();
            mes.SetBody("Страшно было в пещере Гингемы. Там под потолком висело чучело огромного крокодила. На высоких шестах сидели большие филины, с потолка свешивались связки сушёных мышей, привязанных к верёвочкам за хвостики, как луковки. Длинная толстая змея обвилась вокруг столба и равномерно качала пёстрой и плоской головой. И много ещё всяких странных и жутких вещей было в обширной пещере Гингемы.");

            NRTYServer::TMessage::TSentenceData& sent = *mes.MutableAnnData()->AddSentences();
            sent.SetText("в далёкой стране, за высокими горами, колдовала в угрюмой глубокой пещере злая волшебница Гингема");
            if (setLang)
                sent.SetTextLanguage(1);
            {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.0196078), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.0122), *stream);
            }
        }

        {
            NRTYServer::TMessage::TDocument& mes = *messages[3].MutableDocument();
            mes.MutableAnnData()->Clear();
            mes.SetBody("В этом документе отсутствует аннотация. Так и должно быть.");
        }
    }

    void ReadFactors(TDocFactorsView& factors, const TString& query, const TString& kps, bool deleted = false) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        TString textSearch = query;
        Quote(textSearch);
        QuerySearch(textSearch + "&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors&" + kps, results, &resultProps);
        if (deleted) {
            if (results.size() != 0)
                ythrow yexception() << "Deleted documents found";
            factors.Clear();
        } else {
            if (results.size() != 1)
                ythrow yexception() << "No documents found";
            factors.AssignFromSearchResult(*resultProps[0]);
        }
        factors.DebugPrint();
    }

    void SetFactorsConfig(const TString& factorsConfig) {
        TString newFactorsFileName = GetResourcesDirectory() + "/factors/" + factorsConfig;
        (*ConfigDiff)["Searcher.FactorsInfo"] = newFactorsFileName;
        ApplyConfig();
    }

    void CheckSomeFactors(const TString& kps, bool checkDeleted = false) {
        ReadFactors(Factors, "rabbits", kps);
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.94007);

        // Factors from first annotaton sentence
        ReadFactors(Factors, "кролик", kps);
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.135593);

        // Factors from second annotaton sentence
        ReadFactors(Factors, "маргаритки", kps);
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.0972752);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0588235);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.983284);

        // Word not present in annotation
        ReadFactors(Factors, "сестра", kps);
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0);

        // Word from doc without annotation
        ReadFactors(Factors, "отсутствует", kps);
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0);

        ReadFactors(Factors, "пещере", kps, checkDeleted);

        if (checkDeleted)
            return;

        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.94007);
    }

    bool CheckIndexAnnFiles(bool withSent = false) {
        TSet<TString> indexes = Controller->GetFinalIndexes();
        for (auto& dir: indexes) {
            Controller->ProcessCommand("stop");
            INFO_LOG << "Index dir " << dir << Endl;

            CHECK_TEST_TRUE(TFsPath(dir + "/indexann.fat").Exists());
            CHECK_TEST_TRUE(TFsPath(dir + "/indexann.key").Exists());
            CHECK_TEST_TRUE(TFsPath(dir + "/indexann.inv").Exists());
            if (withSent) {
                CHECK_TEST_TRUE(TFsPath(dir + "/indexann.sent").Exists());
            }
        }
        Controller->RestartServer();
        return true;
    }

    bool CheckStreams(const TVector<NRTYServer::TMessage>& messages, ui32 indexedStreamsCount) {
        for (auto& mes : messages) {
            ui32 streamsCount = 0;
            ui32 sentencesCount = mes.GetDocument().GetAnnData().SentencesSize();
            for (ui32 i = 0; i < sentencesCount; ++i) {
                const auto& sent = mes.GetDocument().GetAnnData().GetSentences(i);
                for (ui32 j = 0; j < sent.StreamsByRegionSize(); ++j) {
                    const auto& reg = sent.GetStreamsByRegion(j);
                    streamsCount+= reg.StreamsSize();
                }
            }
            TVector<TDocSearchInfo> results;
            TQuerySearchContext ctx;
            ctx.AttemptionsCount = 5;
            QuerySearch(mes.GetDocument().GetUrl() + "&kps=" + ToString(mes.GetDocument().GetKeyPrefix()), results, ctx);
            if (results.size() != 1) {
                PrintInfoServer();
                TEST_FAILED("Test failed: " + ToString(results.size()));
            }

            const TDocSearchInfo& dsi = results[0];
            TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
            DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
            TDocInfo di(*jsonDocInfoPtr);
            auto& annInfo = di.GetAnnotationsInfo();
            INFO_LOG << "Count streams in indexing message: " << streamsCount << Endl;
            if (sentencesCount == 0) {
                CHECK_TEST_EQ(annInfo.size(), 0);
            } else {
                CHECK_TEST_EQ(annInfo.size() % sentencesCount, 0);
                CHECK_TEST_EQ(annInfo.size() / sentencesCount, indexedStreamsCount);
            }
        }
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Components"] = NRTYServer::AnnComponentName;
        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DataType"] = "COMPTRIE";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DefaultLanguage"] = "rus";
        return true;
    }

};

START_TEST_DEFINE_PARENT(TestAnnConvertDataFormat, TestAnnFactorsHelper)
bool Run() override {
    SetFactorsConfig("ann_streams.cfg");

    TVector<NRTYServer::TMessage> messages;
    GenerateMessages(messages, false);

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    CheckSomeFactors(GetAllKps(messages));
    {
        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(0, 0);
        DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
    }

    (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DataType"] = "PLAIN_ARRAY";
    Controller->ApplyConfigDiff(ConfigDiff);

    {
        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(0, 0);
        DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
    }
    CheckSomeFactors(GetAllKps(messages));

    return true;
}
};

START_TEST_DEFINE_PARENT(TestAnnDocInfos, TestAnnFactorsHelper)
    bool Run() override {
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".SkipUnknownStreams"] = "true";
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CHECK_TEST_TRUE(CheckStreams(messages, 3));

        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        QuerySearch(messages[0].GetDocument().GetUrl() + "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        const TDocSearchInfo& dsi = results[0];
        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
        DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
        TDocInfo di(*jsonDocInfoPtr);
        auto& annInfo = di.GetAnnotationsInfo();
        CHECK_TEST_EQ(annInfo[0].Stream, "DT_LONG_CLICK");
        CHECK_TEST_EQ(annInfo[0].Value, TAnnFormats::GetStreamIntegerData(0.0002f));
        CHECK_TEST_EQ(annInfo[1].Stream, "DT_CORRECTED_CTR");
        CHECK_TEST_EQ(annInfo[1].Value, TAnnFormats::GetStreamIntegerData(0.0196078f));
        CHECK_TEST_EQ(annInfo[2].Stream, "DT_ONE_CLICK");
        CHECK_TEST_EQ(annInfo[2].Value, TAnnFormats::GetStreamIntegerData(0.05f));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnSkipUnknownStreams, TestAnnFactorsHelper)
public:
    bool Run() override {
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".SkipUnknownStreams"] = "true";
        SetFactorsConfig("ann_streams_unknown.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages);

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CHECK_TEST_TRUE(CheckStreams(messages, 2));

        ReadFactors(Factors, "пещере", "&kps=" + ToString(messages[2].GetDocument().GetKeyPrefix()));
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnDifferentRegions, TestAnnFactorsHelper)
    bool Run() override {
        SetFactorsConfig("ann_streams.cfg");
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        {
            NRTYServer::TMessage::TDocument& mes = *messages[0].MutableDocument();
            mes.MutableAnnData()->Clear();
            mes.SetBody("Alice felt too sleepy to play, and there was nobody to play with. It was a hot afternoon, so she was sitting in the garden under a tree. Alice looked at the book. There were no pictures in the book, and Alice didn't like books without pictures. 'I think I'll go and pick some flowers', she said to herself. She began to get up, but she forgot about the flowers at once because she saw a rabbit.");

            NRTYServer::TMessage::TSentenceData& sent = *mes.MutableAnnData()->AddSentences();
            sent.SetText("Rabbits naturally spawn in plains, extreme hills, forest, taiga, swamp, jungle, birch forest, roofed forest, cold taiga, mega taiga, and savanna biomes.");
            sent.SetTextLanguage(2);
            NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
            NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
            reg.SetRegion(102);
            NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.0196078), *stream);
            stream = reg.AddStreams();
            NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.05), *stream);
            stream = reg.AddStreams();
            NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.0002), *stream);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        ReadFactors(Factors, "rabbits", "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()));
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0);

        ReadFactors(Factors, "rabbits", "&relev=relevgeo=102&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()));
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.94007);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnDefaultLang, TestAnnFactorsHelper)
public:
    bool Run() override {
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        {
            NRTYServer::TMessage::TDocument& mes = *messages[0].MutableDocument();
            mes.MutableAnnData()->Clear();
            mes.SetBody("Страшно было в пещере Гингемы. Там под потолком висело чучело огромного крокодила. На высоких шестах сидели большие филины, с потолка свешивались связки сушёных мышей, привязанных к верёвочкам за хвостики, как луковки. Длинная толстая змея обвилась вокруг столба и равномерно качала пёстрой и плоской головой. И много ещё всяких странных и жутких вещей было в обширной пещере Гингемы.");

            NRTYServer::TMessage::TSentenceData& sent = *mes.MutableAnnData()->AddSentences();
            sent.SetText("в далёкой стране, за высокими горами, колдовала в угрюмой глубокой пещере злая волшебница Гингема");
            {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.0196078), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.0002), *stream);
            }
        }

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CheckSearchResults(messages);

        ReadFactors(Factors, "пещере", "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()));
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.94007);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestEmptyAnnotations, TestAnnFactorsHelper)
public:
    bool Run() override {
        {
            TVector<NRTYServer::TMessage> messages;
            GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
            for (auto& mes : messages)
                mes.MutableDocument()->MutableAnnData()->Clear();
            IndexMessages(messages, DISK, 1);
            ReopenIndexers();
            CheckSearchResults(messages);
        }
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (auto& mes : messages)
            mes.MutableDocument()->MutableAnnData()->Clear();
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messages);
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        CheckSearchResults(messages);
        return true;
    }

};

START_TEST_DEFINE_PARENT(TestAnnRecognizer, TestAnnFactorsHelper)
    bool Run() override {
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, false);

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CheckSomeFactors(GetAllKps(messages));
        return true;
    }

    bool InitConfig() override {
        TestAnnFactorsHelper::InitConfig();
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DefaultLanguage"] = "";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnNormalizer, TestAnnFactorsHelper)
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (auto& mes : messages)
            mes.MutableDocument()->MutableAnnData()->Clear();
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messages);

        {
            TSet<TString> indexes = Controller->GetFinalIndexes();
            for (auto& dir: indexes) {
                Controller->ProcessCommand("stop");
                INFO_LOG << "Index dir " << dir << Endl;

                CHECK_TEST_TRUE(!TFsPath(dir + "/indexann.fat").Exists());
                CHECK_TEST_TRUE(!TFsPath(dir + "/indexann.key").Exists());
                CHECK_TEST_TRUE(!TFsPath(dir + "/indexann.inv").Exists());
                CHECK_TEST_TRUE(!TFsPath(dir + "/indexann.sent").Exists());
            }
        }
        Controller->RestartServer();

        TString newFactorsFileName = GetResourcesDirectory() + "/factors/ann_streams.cfg";
        (*ConfigDiff)["Searcher.FactorsInfo"] = newFactorsFileName;
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DefaultLanguage"] = "rus";
        (*ConfigDiff)["Components"] = NRTYServer::AnnComponentName;

        ApplyConfig();
        CHECK_TEST_TRUE(CheckIndexAnnFiles());

        return true;
    }

    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnIndexingIntense, TestAnnFactorsHelper)
    void Generate(TVector<NRTYServer::TMessage>& messages) {
        GenerateInput(messages, 1000, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetBody("Test" + ToString(i) + "text");
            messages[i].MutableDocument()->MutableAnnData()->Clear();
            if ((i % 5) == 0) {
                continue;
            }
            NRTYServer::TMessage::TSentenceData& sent = *messages[i].MutableDocument()->MutableAnnData()->AddSentences();
            sent.SetText("Text");
            sent.SetTextLanguage(2);
            if ((i % 5) == 1) {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.393), *stream);
            }
            if ((i % 5) == 2) {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.785), *stream);
            }
            if ((i % 5) == 3) {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.197), *stream);
            }
            if ((i % 5) == 4) {
                NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
                NRTYServer::TMessage::TStreamData* stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_CORRECTED_CTR", TAnnFormats::GetStreamRawData(0.393), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_ONE_CLICK", TAnnFormats::GetStreamRawData(0.785), *stream);
                stream = reg.AddStreams();
                NSaas::AddSimpleStream("DT_LONG_CLICK", TAnnFormats::GetStreamRawData(0.197), *stream);
            }
        }
    }

    bool Check(const TVector<NRTYServer::TMessage>& messages) {
        for (ui32 i = 0; i < messages.size(); ++i) {
            TVector<TDocSearchInfo> results;
            QuerySearch(messages[i].GetDocument().GetUrl() + "&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
            if (results.size() != 1) {
                PrintInfoServer();
                TEST_FAILED("Test failed: " + ToString(results.size()));
            }

            const TDocSearchInfo& dsi = results[0];
            TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
            DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
            TDocInfo di(*jsonDocInfoPtr);
            auto& annInfo = di.GetAnnotationsInfo();
            if ((i % 5) == 0) {
                CHECK_TEST_EQ(annInfo.size(), 0);
                continue;
            } else {
                CHECK_TEST_EQ(annInfo.size(), 3);
            }

            CHECK_TEST_EQ(annInfo[0].Stream, "DT_LONG_CLICK");
            CHECK_TEST_EQ(annInfo[1].Stream, "DT_CORRECTED_CTR");
            CHECK_TEST_EQ(annInfo[2].Stream, "DT_ONE_CLICK");

            TStringStream ss;
            ss << annInfo[0].Value << " " << annInfo[1].Value << " " << annInfo[2].Value;

            if ((i % 5) == 1) {
                CHECK_TEST_EQ(ss.Str(), "0 100 0");
            }
            if ((i % 5) == 2) {
                CHECK_TEST_EQ(ss.Str(), "0 0 200");
            }
            if ((i % 5) == 3) {
                CHECK_TEST_EQ(ss.Str(), "50 0 0");
            }
            if ((i % 5) == 4) {
                CHECK_TEST_EQ(ss.Str(), "50 100 200");
            }
        }
        return true;
    }


    bool CheckFinalIndexes() {
        TSet<TString> indexes = Controller->GetFinalIndexes();
        for (auto& index : indexes) {
            NRTYArchive::TMultipartConfig cfg;
            TArchiveOwner::TPtr data = TArchiveOwner::Create(index + "/indexann", cfg);
            auto iter = data->CreateIterator();
            while (iter->IsValid()) {
                TSingleDocAccessor accessor(iter->GetDocid(), iter->GetDocument());
                NRTYAnn::TStreamIterator strIter(&accessor, new NRTYAnn::TSimpleVectorAccessor<ui8>(3));
                strIter.Restart(NIndexAnn::THitMask(iter->GetDocid()));
                ui32 stCount = 0;
                TVector<ui8> streams;
                while(strIter.Valid()) {
                    stCount++;
                    ui32 data = strIter.Current().Value();
                    streams.push_back(static_cast<ui8>(data));
                    strIter.Next();
                }
                CHECK_TEST_EQ(stCount, 3);
                TStringStream ss;
                ss << (ui32)streams[0] << " " << (ui32)streams[1] << " " << (ui32)streams[2];
                INFO_LOG << "Streams=" << ss.Str() << Endl;
                CHECK_TEST_TRUE(ss.Str() == "50 100 200" || ss.Str() == "50 0 0" || ss.Str() == "0 100 0" || ss.Str() == "0 0 200");
                iter->Next();
            }
        }
        Controller->RestartServer();
        return true;
    }

    bool Run() override {
        (*ConfigDiff)["Indexer.Disk.Threads"] = "8";
        (*ConfigDiff)["Components.Ann.DataType"] = "COMPTRIE";
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        Generate(messages);

        TIndexerClient::TContext indexingCtx;
        indexingCtx.CountThreads = 32;
        indexingCtx.DoWaitReply = true;
        IndexMessages(messages, DISK, indexingCtx);
        ReopenIndexers();

        CHECK_TEST_TRUE(CheckFinalIndexes());

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        CHECK_TEST_TRUE(Check(messages));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnReindexDoc, TestAnnFactorsHelper)
    bool Run() override {
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".SkipUnknownStreams"] = "true";
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages);

        TVector<NRTYServer::TMessage> m1;
        m1.push_back(messages[2]);
        IndexMessages(m1, DISK, 1);
        IndexMessages(m1, DISK, 1);

        ReopenIndexers();

        CheckSearchResults(m1);

        ReadFactors(Factors, "пещере", "&kps=" + ToString(m1[0].GetDocument().GetKeyPrefix()));
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.94007);

        IndexMessages(m1, DISK, 1);

        CheckSearchResults(m1);

        ReadFactors(Factors, "пещере", "&kps=" + ToString(m1[0].GetDocument().GetKeyPrefix()));
        Factors.CheckFactor("CorrectedCtrQueryMatchPrediction", 0);
        Factors.CheckFactor("CorrectedCtrBm15V4K5", 0.000605085);
        Factors.CheckFactor("CorrectedCtrValueWcmAvg", 0.0156863);
        Factors.CheckFactor("CorrectedCtrBm15StrictK2", 0.94007);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestTurnOnAnnotations, TestAnnFactorsHelper)
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (auto& mes : messages)
            mes.MutableDocument()->MutableAnnData()->Clear();
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CheckSearchResults(messages);

        TString newFactorsFileName = GetResourcesDirectory() + "/factors/ann_streams.cfg";
        (*ConfigDiff)["Searcher.FactorsInfo"] = newFactorsFileName;
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DefaultLanguage"] = "rus";
        (*ConfigDiff)["Components"] = NRTYServer::AnnComponentName;
        ApplyConfig();

        CHECK_TEST_TRUE(CheckIndexAnnFiles());

        TVector<NRTYServer::TMessage> messagesWithStreams;
        GenerateMessages(messagesWithStreams);
        IndexMessages(messagesWithStreams, DISK, 1);
        ReopenIndexers();

        CHECK_TEST_TRUE(CheckIndexAnnFiles());

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        CHECK_TEST_TRUE(CheckIndexAnnFiles(!GetIsPrefixed()));
        CheckSearchResults(messages);
        CheckSomeFactors(GetAllKps(messagesWithStreams));
        return true;
    }

    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestCtrAnnFactors, TestAnnFactorsHelper)
public:
    bool Run() override {
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages);

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CheckSomeFactors(GetAllKps(messages));

        TVector<NRTYServer::TMessage> messagesToDel;
        messagesToDel.push_back(BuildDeleteMessage(messages[2]));
        IndexMessages(messagesToDel, DISK, 1);

        CheckSomeFactors(GetAllKps(messages), true);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestAnnFactorsMerge, TestAnnFactorsHelper)
public:
    bool Run() override {
        SetFactorsConfig("ann_streams.cfg");

        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages);

        TVector<NRTYServer::TMessage> messages0;
        messages0.push_back(messages[0]);
        messages0.push_back(messages[3]);
        IndexMessages(messages0, DISK, 1);
        ReopenIndexers();

        TVector<NRTYServer::TMessage> messages1;
        messages1.push_back(messages[1]);
        messages1.push_back(messages[2]);
        IndexMessages(messages1, DISK, 1);
        ReopenIndexers();

        CheckSomeFactors(GetAllKps(messages));

        TVector<NRTYServer::TMessage> messagesToDel;
        messagesToDel.push_back(BuildDeleteMessage(messages[2]));
        IndexMessages(messagesToDel, DISK, 1);

        CheckSomeFactors(GetAllKps(messages), true);

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        CheckSomeFactors(GetAllKps(messages), true);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestTwoAnnIndexes, TestAnnFactorsHelper)
    THolder<NRTYFactors::TConfig> FactorsConfig;

    NRTYServer::TMessage CreateMessage(const TString& text, const TString& annotation, const TString& stream, float value, ui32 region) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        messages[0].MutableDocument()->MutableAnnData()->Clear();
        messages[0].MutableDocument()->ClearBody();
        auto zone = messages[0].MutableDocument()->MutableRootZone()->AddChildren();
        zone->SetName("description");
        zone->SetText(text);

        NRTYServer::TMessage::TSentenceData& sent = *messages[0].MutableDocument()->MutableAnnData()->AddSentences();
        sent.SetText(annotation);
        sent.SetTextLanguage(2);
        NRTYServer::TMessage::TRegionData& reg = *sent.AddStreamsByRegion();
        reg.SetRegion(region);
        NRTYServer::TMessage::TStreamData* streamData = reg.AddStreams();

        TString data = TAnnFormats::GetStreamRawData(value);
        INFO_LOG << "Save " << stream << " as " << Base64Encode(data) << Endl;
        NSaas::AddSimpleStream(stream, data, *streamData);

        return messages[0];
    }

    bool CheckMessage(const TString& query, const TString& stream, float value, ui32 region, ui64 kps, TMap<TString, float> expectedFactors) {
        TString encodedVal = Base64Encode(TAnnFormats::GetStreamRawData(value));
        INFO_LOG << "Check stream " << stream << " with value " << encodedVal << Endl;
        TString name;
        ui32 index;
        for (const auto& st : FactorsConfig->GetAnnStreams()) {
            if (st.second->CheckFactorName(stream, index)) {
                name = st.first;
                break;
            }
        }
        if (!name) {
            ythrow yexception() << "invalid stream " << stream;
        }
        const NRTYFactors::TSimpleFactorDescription& streamInfo = FactorsConfig->GetAnnStreams(name)->GetFactorsInfo()->GetFactor(index);

        TSet<TString> indexes = Controller->GetFinalIndexes();
        for (auto& indexName : indexes) {
            NRTYArchive::TMultipartConfig cfg;
            TArchiveOwner::TPtr data = TArchiveOwner::Create(indexName + "/" + name, cfg);
            auto iter = data->CreateIterator();
            INFO_LOG << "Streams for " << data->GetPath() << Endl;
            while (iter->IsValid()) {
                TAtomicSharedPtr<NRTYAnn::TAnnDataAccessor> dataAccessor(new NRTYAnn::TAnnDataAccessor(*FactorsConfig->GetAnnStreams(name)));
                TSingleDocAccessor accessor(iter->GetDocid(), iter->GetDocument());
                NRTYAnn::TStreamIterator strIter(&accessor, dataAccessor, dataAccessor.Get());
                strIter.Restart(NIndexAnn::THitMask(iter->GetDocid()));
                while(strIter.Valid()) {
                    ui8 data = strIter.Current().Value();
                    if (strIter.Current().Region() != region) {
                        ythrow yexception() << "Incorrect region" << region;
                    }

                    TString encoded = Base64Encode(TStringBuf(reinterpret_cast<const char*>(&data), sizeof(data)));
                    if (strIter.Current().Stream() == streamInfo.IndexGlobal) {
                        if (encoded != encodedVal) {
                            ythrow yexception() << "Incorrect data for stream " << stream << " " << encoded << "(" << encodedVal << ")";
                        }
                    } else {
                        if ("AA==" != encoded) {
                            ythrow yexception() << "Stream data should be empty";
                        }
                    }
                    strIter.Next();
                }
                iter->Next();
            }
        }
        Controller->RestartServer();

        ReadFactors(Factors, query, "&relev=relevgeo=" + ToString(region) + "&kps=" + ToString(kps));
        for (auto factor : Factors.GetData()) {
            const auto& name = factor.first;
            INFO_LOG << "Check factor " << name << Endl;
            if (expectedFactors.contains(name)) {
                Factors.CheckFactor(name, expectedFactors[name]);
            } else {
                Factors.CheckFactor(name, 0.0f);
            }

        }
        return true;
    }

    bool Run() override {
        TString newFactorsFileName = GetResourcesDirectory() + "/factors/ann_two_indexes.cfg";
        FactorsConfig.Reset(new NRTYFactors::TConfig(newFactorsFileName.data()));
        (*ConfigDiff)["Searcher.FactorsInfo"] = newFactorsFileName;
        ApplyConfig();

        TMap<TString, float> factors;
        factors["CorrectedCtrQueryMatchPrediction"] = 0.898039f;
        factors["CorrectedCtrBm15V4K5"] = 0.999846f;
        factors["CorrectedCtrValueWcmAvg"] = 0.898039f;
        factors["CorrectedCtrBm15StrictK2"] = 0.998888f;

        CHECK_TEST_TRUE(DoRun("DT_CORRECTED_CTR", factors));
        Controller->ProcessCommand("clear_index");
        Controller->RestartServer();

        factors.clear();
        factors["OneClickQueryMatchPrediction"] = 0.898039f;
        factors["OneClickBocmWeightedW1K3"] = 0.997823;
        factors["OneClickBclmWeightedK3"] = 0.988987f;
        factors["OneClickBm15V4K5"] = 0.999846f;
        factors["OneClickBm15MaxK3"] = 0.988987f;
        factors["OneClickBocmWeightedMaxK1"] = 0.999978f;
        factors["OneClickValueWcmPrediction"] = 0.898039f;
        factors["OneClickBm15AK4"] = 0.645161f;
        factors["OneClickValueWcmAvg"] = 0.898039f;
        factors["OneClickValueWcmMax"] = 0.898039f;
        factors["OneClickBclmPlainW1K3"] = 0.998888f;
        factors["OneClickBm15StrictK2"] = 0.998888f;

        CHECK_TEST_TRUE(DoRun("DT_ONE_CLICK", factors));
        Controller->ProcessCommand("clear_index");
        Controller->RestartServer();

        factors.clear();
        factors["SimpleClickQueryMatchPrediction"] = 0.898039f;
        factors["SimpleClickAnnotationMatchPrediction"] = 0.0f;
        factors["SimpleClickBclmWeightedK3"] = 0.988987f;
        factors["OneClickBm15AK4"] = 0.645161f;

        CHECK_TEST_TRUE(DoRun("DT_SIMPLE_CLICK", factors));
        Controller->ProcessCommand("clear_index");
        Controller->RestartServer();

        factors.clear();
        factors["YabarTimeQueryMatchPrediction"] = 0.898039f;
        factors["YabarTimeAnnotationMatchPrediction"] = 0.0f;
        factors["YabarTimeAnnotationMatchPredictionWeighted"] = 0.0f;
        factors["OneClickBm15AK4"] = 0.645161f;
        CHECK_TEST_TRUE(DoRun("DT_YABAR_TIME", factors));

        return true;
    }

    bool DoRun(const TString& stream, TMap<TString, float>& factors) {
        TVector<NRTYServer::TMessage> messages;
        NRTYServer::TMessage message = CreateMessage("Black and white rabbit", "White rabbit", stream, 0.9f, 225);
        messages.push_back(message);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CHECK_TEST_TRUE(CheckMessage("white", stream, 0.9f, 225, messages[0].GetDocument().GetKeyPrefix(), factors));

        if (stream == "DT_CORRECTED_CTR") {
            factors["CorrectedCtrQueryMatchPrediction"] = 0.0f;

            CHECK_TEST_TRUE(CheckMessage("rabbit", stream, 0.9f, 225, messages[0].GetDocument().GetKeyPrefix(), factors));
            factors["CorrectedCtrValueWcmAvg"] = 0.746521f;
            factors["CorrectedCtrBm15V4K5"] = 0.831151f;
            factors["CorrectedCtrBm15StrictK2"] = 0.830354f;

            CHECK_TEST_TRUE(CheckMessage("black rabbit", stream, 0.9f, 225, messages[0].GetDocument().GetKeyPrefix(), factors));
            factors["CorrectedCtrValueWcmAvg"] = 0.0f;
            factors["CorrectedCtrBm15V4K5"] = 0.0f;
            factors["CorrectedCtrBm15StrictK2"] = 0.0f;
            CHECK_TEST_TRUE(CheckMessage("black", stream, 0.9f, 225, messages[0].GetDocument().GetKeyPrefix(), factors));
        }

        return true;
    }
};
