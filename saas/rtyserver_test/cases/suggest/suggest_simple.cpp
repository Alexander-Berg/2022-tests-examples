#include <saas/api/clientapi.h>
#include <saas/rtyserver/common/sharding.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/components/zones_makeup/read_write_makeup_manager.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/api/factors_erf.h>
#include <util/string/vector.h>
#include <util/system/fs.h>
#include <kernel/keyinv/indexfile/searchfile.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/folder/filelist.h>
#include <util/random/shuffle.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestSuggestTestsHelper)
public:
    bool TestResults(ui32 kffRlv, const TString& kpsInfo) {
        TVector<TDocSearchInfo> results;
        kffRlv <<= 23;
        TQuerySearchContext context;
        context.AttemptionsCount = 10;
        context.ResultCountRequirement = kffRlv ? 2 : 0;
        QuerySearch("на" + kpsInfo, results, context);
        CHECK_TEST_EQ(results.size(), kffRlv ? 2 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "наталья сорокина сатурова", 52 * kffRlv);
            CHECK_DSI_URL_RLV(results[1], "наталья", 20 * kffRlv);
        }
        QuerySearch("на" + kpsInfo + "&pron=suggesttypesimple", results);
        CHECK_TEST_EQ(results.size(), kffRlv ? 2 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "наталья сорокина сатурова", 52 * kffRlv);
            CHECK_DSI_URL_RLV(results[1], "наталья", 20 * kffRlv);
        }
        QuerySearch("н" + kpsInfo + "&numdoc=3&pron=suggesttypesimple", results);
        CHECK_TEST_EQ(results.size(), kffRlv ? 3 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "нижний новгород", 80 * kffRlv);
            CHECK_DSI_URL_RLV(results[1], "нижегородская область", 60 * kffRlv);
            CHECK_DSI_URL_RLV(results[2], "наталья сорокина сатурова", 52 * kffRlv);
        }
        QuerySearch("н" + kpsInfo + "&numdoc=1", results);
        CHECK_TEST_EQ(results.size(), kffRlv ? 1 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "нижний новгород", 80 * kffRlv);
        }
        QuerySearch("сс" + kpsInfo, results);
        CHECK_TEST_EQ(results.size(), kffRlv ? 1 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "мви фпс рф мвпку кгб ссср впу", 12 * kffRlv);
        }
        QuerySearch("ни" + kpsInfo, results);
        CHECK_TEST_EQ(results.size(), kffRlv ? 3 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "нижний новгород", 80 * kffRlv);
            CHECK_DSI_URL_RLV(results[1], "нижегородская область", 60 * kffRlv);
            CHECK_DSI_URL_RLV(results[2], "нижний новгород россия", 40 * kffRlv);
        }
        QuerySearch("ро" + kpsInfo, results);
        CHECK_TEST_EQ(results.size(), kffRlv ? 3 : 0);
        if (kffRlv) {
            CHECK_DSI_URL_RLV(results[0], "россия", 80 * kffRlv);
            CHECK_DSI_URL_RLV(results[1], "нижний новгород россия", 40 * kffRlv);
            CHECK_DSI_URL_RLV(results[2], "москва россия", 20 * kffRlv);
        }
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["IndexGenerator"] = "Suggest";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "0";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "200";
        (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToReject"] = "200";
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/suggest_factors.cfg";
        SetMergerParams(true, 1, -1, mcpTIME, 500000);
        SetEnabledRepair();
        return true;
    }

};

START_TEST_DEFINE_PARENT(TestSuggestBuildIndex, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    texts.push_back("<body111>Мама мыла раму зачем-то</body111>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    Controller->RestartServer(false);

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);

    IndexMessages(messages, REALTIME, 1);
    Controller->RestartServer(false);
    Controller->ProcessCommand("do_all_merger_tasks");

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);
    TJsonPtr info = Controller->GetServerInfo();
    NJson::TJsonValue::TArray jsonArr;
    CHECK_TEST_EQ(info->GetArray(&jsonArr), true);
    NJson::TJsonValue countSearchable = jsonArr[0]["searchable_docs"];
    if (!countSearchable.IsInteger()) {
        ythrow yexception() << "there is no countSearchable: " << info->GetStringRobust() << Endl;
    }
    CHECK_TEST_EQ((ui32)countSearchable.GetInteger(), (ui32)texts.size());

    TString reply;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards, NSaas::TShardsDispatcher::TContext(NSaas::UrlHash), reply)) {
        ERROR_LOG << "Detach failed: " << reply << Endl;
        return false;
    }
    INFO_LOG << reply << Endl;
    CHECK_TEST_TRUE(reply.StartsWith("rbtorrent"));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestRepair, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    BreakSomething();

    Controller->RestartServer(true);
    Controller->WaitIsRepairing();
    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);
    return true;
}

virtual void BreakSomething() {
}
};

START_TEST_DEFINE_PARENT(TestSuggestRepairDDK, TTestSuggestRepairCaseClass)
void BreakSomething() override {
    Controller->RestartServer(false);
    TSet<TString> dirs = Controller->GetFinalIndexes();
    for (auto it = dirs.begin(); it != dirs.end(); it++) {
        TString indexDdk = *it + "/indexddk.rty";
        CHECK_WITH_LOG(TFsPath(indexDdk).Exists());
        CHECK_WITH_LOG(TFsPath(indexDdk + ".hdr").Exists());
        TFsPath(indexDdk).ForceDelete();
        TFsPath(indexDdk + ".hdr").ForceDelete();
    }
}
};

START_TEST_DEFINE_PARENT(TestSuggestRepairErf, TTestSuggestRepairCaseClass)
void BreakSomething() override {
    Controller->RestartServer(false);
    TSet<TString> dirs = Controller->GetFinalIndexes();
    for (auto it = dirs.begin(); it != dirs.end(); it++) {
        TString indexErf = *it + "/index_sg_erf";
        CHECK_WITH_LOG(TFsPath(indexErf).Exists());
        CHECK_WITH_LOG(TFsPath(indexErf + ".hdr").Exists());
        TFsPath(indexErf).ForceDelete();
        TFsPath(indexErf + ".hdr").ForceDelete();
    }
}
};

START_TEST_DEFINE_PARENT(TestSuggestSimple, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title some-attr=\"123\">Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestFreaks, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("aaa bbb ccc ddd eee fff rrr ggg ddd aaa ddd ddd ddd");
    texts.push_back("111 222 333 444 555 666 777");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/html");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("aaa" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 0);

    QuerySearch("777" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 0);

    QuerySearch("666" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "6";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToReject"] = "12";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestBlacklist, TestSuggestTestsHelper)

void AddIntSearchAttribute(NRTYServer::TMessage& m, const TString& name, ui32 value) {
    auto sa = m.MutableDocument()->AddSearchAttributes();
    sa->set_name(name);
    sa->set_value(ToString(value));
    sa->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("aaa ccc www"); // enable suggest (no attr)
    texts.push_back("aaa ddd xxx"); // disable suggest
    texts.push_back("bbb ccc yyy"); // enable suggest (wrong attr value)
    texts.push_back("bbb ddd zzz"); // disable suggest
    const TVector<TStringBuf> needSuggest{"aaa", "bbb", "ccc", "www", "yyy"};
    const TVector<TStringBuf> noSuggest{"111", "ddd", "xxx", "zzz"};

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/html");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }

    AddIntSearchAttribute(messages[1], "i_no_suggest", 1);
    AddIntSearchAttribute(messages[2], "i_no_suggest", 0);
    AddIntSearchAttribute(messages[3], "i_abc_unrelated", 1);
    AddIntSearchAttribute(messages[3], "i_no_suggest", 1);

    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    for (const auto &str: needSuggest) {
        TVector<TDocSearchInfo> results;
        QuerySearch(str + kpsInfo, results);
        CHECK_TEST_GREATER(results.size(), 0);
    }

    for (const auto &str: noSuggest) {
        TVector<TDocSearchInfo> results;
        QuerySearch(str + kpsInfo, results);
        CHECK_TEST_EQ(results.size(), 0);
    }
    return true;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    (*ConfigDiff)["ComponentsConfig.Suggest.AttributesBlacklist"] = "garbage:xxx;i_no_suggest:1;i_no_suggest:888;garbage:zzz";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestCustomZones, TestSuggestTestsHelper)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < 1; ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->ClearBody();
        NSaas::TAction action;
        action.ParseFromProtobuf(messages[i]);
        NSaas::TDocument& doc = action.GetDocument();
        doc.AddZone("aaTitle1").SetText("psycho");
        doc.AddZone("pscaption1").SetText("Satılık");
        messages[i].Clear();
        messages[i].MergeFrom(action.ToProtobuf());
    }
    TString kpsInfo;
    if (GetIsPrefixed())
        kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("psycho" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("satılık&pron=suggesttypesimple" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("sa&pron=suggesttypesimple" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("satılık" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("sa" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestSimpleRemove, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 100);

    Controller->RestartServer();

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestSelfModify, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 100);

    Controller->RestartServer();

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestModify, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    texts.push_back("asdasdasas");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    messages.erase(messages.begin() + 2);

    Controller->RestartServer();

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);

    messages[0].MutableDocument()->SetBody("вася");
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TVector<TDocSearchInfo> results;
    QuerySearch("ва" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "вася", 10 << 23);
    QuerySearch("на" + kpsInfo + "&pron=suggesttypesimple", results);
    CHECK_TEST_EQ(results.size(), 2);
    CHECK_DSI_URL_RLV(results[0], "наталья сорокина сатурова", 20 << 23);
    CHECK_DSI_URL_RLV(results[1], "наталья", 10 << 23);
    QuerySearch("на" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 2);
    CHECK_DSI_URL_RLV(results[0], "наталья сорокина сатурова", 20 << 23);
    CHECK_DSI_URL_RLV(results[1], "наталья", 10 << 23);

    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestSuggestDiskSearchHelper, TestSuggestTestsHelper)
bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    SetEnabledDiskSearch();
    SetMergerParams(true, 1, -1, mcpCONTINUOUS, 500000);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestDiskSearch, TestSuggestDiskSearchHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    texts.push_back("asdasdasas");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    messages.erase(messages.begin() + 2);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 30;
    QuerySearch("на" + kpsInfo, results, context);

    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(TestResults(1, kpsInfo), true));

    messages[0].MutableDocument()->SetBody("вася");
    messages[1].MutableDocument()->SetBody("вася1");
    IndexMessages(messages, REALTIME, 1);

    context.ResultCountRequirement = 2;

    QuerySearch("вася" + kpsInfo, results, context);
    CHECK_TEST_EQ(results.size(), 2);

    QuerySearch("ва" + kpsInfo, results, context);
    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 2));

    QuerySearch("на" + kpsInfo, results);
    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));

    return true;
}
bool InitConfig() override {
    if (!TestSuggestDiskSearchHelper::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Memory.TimeToLiveSec"] = "2";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestDiskSearchByFlagOnly, TestSuggestDiskSearchHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    texts.push_back("asdasdasas");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, DISK, 1);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.ResultCountRequirement = 2;
    context.AttemptionsCount = 10;
    try {
        QuerySearch("на" + kpsInfo, results, context);
    } catch (...) {
    }
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
bool InitConfig() override {
    if (!TestSuggestDiskSearchHelper::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Memory.TimeToLiveSec"] = "2";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestMultiToken, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("вася1");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("ва" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "вася1", 10 << 23);
    QuerySearch("на" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestManyWords, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("вася бил маша");
    texts.push_back("костя имел вася");
    texts.push_back("маша съела костя");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("вася костя" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "костя имел вася", 10 << 23);

    QuerySearch("маша ва" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "вася бил маша", 10 << 23);

    QuerySearch("машу ва" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestDelete, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    texts.push_back("asdasdassdasd");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    messages.erase(messages.begin() + 2);

    Controller->RestartServer();

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    CHECK_TEST_EQ(TestResults(1, kpsInfo), true);

    messages[0].SetMessageType(NRTYServer::TMessage::DELETE_DOCUMENT);
    messages[0].MutableDocument()->ClearBody();
    messages[1].SetMessageType(NRTYServer::TMessage::DELETE_DOCUMENT);
    messages[1].MutableDocument()->ClearBody();
    IndexMessages(messages, REALTIME, 1);

    CHECK_TEST_EQ(TestResults(0, kpsInfo), true);

    TVector<TDocSearchInfo> results;
    QuerySearch("asd" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "asdasdassdasd", 10 << 23);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestManyKpssClose, TestSuggestTestsHelper)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    TVector<TString> texts;
    for (ui32 i = 1; i < 300 * GetShardsNumber(); ++i) {
        texts.push_back(ToString(i));
    }
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(i + 1);
        messages[i].MutableDocument()->SetBody(texts[i] + " body");
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 20;
    QuerySearch("1&sgkps=10,100&numdoc=10", results, context);
    CHECK_TEST_EQ(results.size(), 2);
    CHECK_DSI_URL_RLV(results[0], "10 body", 10 << 23);
    CHECK_DSI_URL_RLV(results[1], "100 body", 10 << 23);

    QuerySearch("1&sgkps=100&numdoc=10&pron=suggesttypesimple", results, context);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "100 body", 10 << 23);

    ReopenIndexers();

    return true;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    SetIndexerParams(ALL, 50, -1, 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestManyKpssRestart, TestSuggestTestsHelper)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    TVector<TString> texts;
    for (ui32 i = 1; i < 300; ++i) {
        texts.push_back(ToString(i));
    }
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(i + 1);
        messages[i].MutableDocument()->SetBody(texts[i] + " body");
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 10;
    QuerySearch("1&sgkps=10,100&numdoc=10", results, context);
    CHECK_TEST_EQ(results.size(), 2);
    CHECK_DSI_URL_RLV(results[0], "10 body", 10 << 23);
    CHECK_DSI_URL_RLV(results[1], "100 body", 10 << 23);

    QuerySearch("1&sgkps=100&numdoc=10&pron=suggesttypesimple", results, context);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "100 body", 10 << 23);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestLength, TestSuggestTestsHelper)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("мама мыла раму");
    texts.push_back("тузик ест орехи");
    texts.push_back("<aaTitle1>Мама мыла раму зачем-то</aaTitle1>");
    texts.push_back("здесь стоит длинное-длинное-длинное-слово");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    TString kpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("мыл" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_DSI_URL_RLV(results[0], "мама мыла", 32 << 23);

    QuerySearch("рам" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 0);

    QuerySearch("здесь" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("длинное" + kpsInfo, results);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    (*ConfigDiff)["Components"] = "";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "2";
    (*ConfigDiff)["ComponentsConfig.Suggest.MaxWordLength"] = "10";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestFilter, TestSuggestTestsHelper)
TString KpsInfo;
bool Run() override {
    TVector<TString> texts;
    texts.push_back("&#x1F534;çek satış ve sikiş");
    texts.push_back("&#x1F534;çek&#x1F534;satış bölümünde arıza var ss");
    texts.push_back("&#x1F534;hakkında&#x1F534;çek satış &amp;#9733; takas bölümü");
    texts.push_back("reis abv");
    texts.push_back("rei abv");

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    KpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("çek satış" + KpsInfo, results);
    CHECK_TEST_EQ(results.size(), 2);

    if (results[0].GetUrl() != "hakkında çek satış takas") {
        CHECK_TEST_EQ(results[1].GetUrl(), "hakkında çek satış takas");
    }

    QuerySearch("reis" + KpsInfo, results);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[0].GetUrl(), "reis abv");

    return true;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    TString filterPath = GetResourcesDirectory() + "/suggest_filter/";
    (*ConfigDiff)["Components"] = "";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "4";
    (*ConfigDiff)["ComponentsConfig.Suggest.FilterDictionaries"] = filterPath + "tr.porno.dic;" + filterPath + "tr.porno.pref";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestNumbers, TestSuggestTestsHelper)
TString KpsInfo;
bool Run() override {
    TVector<TString> texts;
    texts.push_back("lg gl phone");
    texts.push_back("lg g3 phone");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    KpsInfo = "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    Controller->RestartServer();
    CHECK_TEST_EQ(DoCheck("l", 2), true);
    CHECK_TEST_EQ(DoCheck("lg", 2), true);
    CHECK_TEST_EQ(DoCheck("lg ", 2), true);
    CHECK_TEST_EQ(DoCheck("lg g", 2), true);
    CHECK_TEST_EQ(DoCheck("lg gl", 1), true);
    CHECK_TEST_EQ(DoCheck("lg g3", 1), true);
    CHECK_TEST_EQ(DoCheck("lg g3 ", 1), true);
    CHECK_TEST_EQ(DoCheck("lg g3 p", 1), true);
    CHECK_TEST_EQ(DoCheck("lg p", 2), true);

    return true;
}

bool DoCheck(const TString& req, ui32 count) {
    TVector<TDocSearchInfo> results;
    QuerySearch(req + KpsInfo, results);
    DEBUG_LOG << "Request: " << req << ", expected: " << count << ", actual: " << results.size() << Endl;
    return results.size() == count;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    (*ConfigDiff)["Components"] = "";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "4";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestRanking, TestSuggestTestsHelper)
TString KpsInfo;
bool Run() override {
    TVector<TString> texts;
    texts.push_back("iphone price");
    texts.push_back("iphone price in russia");
    texts.push_back("iphone 6 price info");
    texts.push_back("iphone battery replace price");
    Shuffle(texts.begin(), texts.end());
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    KpsInfo = "&relev=formula=advanced&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    Controller->RestartServer();

    TVector<TDocSearchInfo> results;
    QuerySearch("iphone price" + KpsInfo, results);
    for (size_t i = 0; i < results.size(); i++) {
        DEBUG_LOG << "url: " << results[i].GetUrl() << "; relevance: " << results[i].GetRelevance() << Endl;
    }

    CHECK_TEST_EQ(results.size(), 4);
    CHECK_TEST_EQ(results[0].GetUrl(), "iphone price");
    CHECK_TEST_EQ(results[1].GetUrl(), "iphone price in russia");
    CHECK_TEST_EQ(results[2].GetUrl(), "iphone 6 price info");
    CHECK_TEST_EQ(results[3].GetUrl(), "iphone battery replace price");
    CHECK_TEST_LESS(results[1].GetRelevance(), results[0].GetRelevance());
    CHECK_TEST_LESS(results[2].GetRelevance(), results[1].GetRelevance());
    CHECK_TEST_LESS(results[3].GetRelevance(), results[2].GetRelevance());

    return true;
}

bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    (*ConfigDiff)["Components"] = "";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "4";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSuggestIndexCompatibility, TestSuggestTestsHelper)
bool Run() override {

    return true;
}
bool InitConfig() override {
    if (!TestSuggestTestsHelper::InitConfig())
        return false;
    (*ConfigDiff)["Components"] = "";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "4";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    (*ConfigDiff)["Searcher.FactorsInfo"] = "";
    return true;
}
};
