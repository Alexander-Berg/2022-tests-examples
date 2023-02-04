#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestQTree, TTestMarksPool::OneBackendOnly)
inline void AddSearchAttr(NRTYServer::TMessage::TDocument& doc, const char* name, const char* value, NRTYServer::TAttribute::TAttributeType type) {
    NRTYServer::TAttribute& attr = *doc.AddSearchAttributes();
    attr.SetType(type);
    attr.SetName(name);
    attr.SetValue(value);
}

void PrepareDoc(NRTYServer::TMessage::TDocument& doc) {
    doc.ClearSearchAttributes();
    doc.ClearGroupAttributes();

    AddSearchAttr(doc, "i_geoid", "225", NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    AddSearchAttr(doc, "i_age", "3", NRTYServer::TAttribute::INTEGER_ATTRIBUTE);

    const TString body =
        "<product>"
        "<z_propset"
        " i_sdk_max=\"16777215\""
        " i_sdk_min=\"8\""
        " s_audio_low_latency=\"0\""
        " s_camera_flash=\"0\""
        " s_faketouch_multitouch_distinct=\"0\""
        " s_faketouch_multitouch_jazzhand=\"0\""
        " s_nfc=\"0\""
        " s_sensor_barometer=\"0\""
        " s_sensor_gyroscope=\"0\""
        " s_telephony_cdma=\"0\""
        " s_type_television=\"0\""
        " s_usb_host=\"0\""
        ">"
        "screen normal hdpi"
        "</z_propset>"
        "</product>";
    doc.SetBody(body);
    doc.SetMimeType("text/xml");
    doc.SetKeyPrefix(1);

}
bool Run() override {
    if(!GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, true);
    PrepareDoc(*messages[0].MutableDocument());
    IndexMessages(messages, REALTIME, 1);
    Sleep(TDuration::Seconds(1));
    TVector<TDocSearchInfo> results;
    const TString query = "1&kps=1&qtree=cHicxZfPb-NEFMfnTX40Gegq215KOGxkabdJxcFtKWojihASoscKCYEiIdd23NpsYgc7hSSAyAVRBAik1WoFXFaRyrESuxcOi8QJ8RcgJJCQ9sSRExcOy3g8djLOD5NQRE7xm3lff9574_ds8hJ5PIcKaA2VcBnLaAUVkYQ20BaqhnZURjJ6PnOQOUSvoaOsCWeAPgf0JaABoHuAvgNEf8V3yCtcKk9dgEplLEU9MZgebDPBzIggOkC-oPnnT79_mw8lA5cBwHYkLKNdeGHNF_aVqDAEjkfIhB7qM8fiAyC3YeT2fiTLnqKrbePEcbuKVWcYT0g78s7esabJe4a2p6mb2tOqphl7z2gMbzkJT5QcwHS9-fBvp8iLnB5z-ml1yB5kGdeUKtAsvB7Lw5KlnBgOz0Bqa2uHaS4lxRq6DcD3EeO5kcM0Ht8uPSbzH72IR2cutZZ4hF8BUWdxZTaZxpxkgZfIVmZswUpIxy6m00lhilPltIxk2AVO_eOTUV1QCf5NXe7mmRIuYOYxWyl9CNOVvsfkfnjUc1wq6-muYdhF2JcU9gRVn93fLpWF01pdn3ZW1yulMk9plVax9G4pvGJ5q5R6Sst1Wp7RrpYlDpydUKQHX48icyaxOJ0cFKDIl-jjQF1oUeDwFg6q4q4TvliANfDVd9HLcFbD5xdazn-E6C1XTdTB7sakjR_V8N37NfhG3NvDOXwEI4fxV4h6VZRB23GbaqMIigT7M2J8-JcQY-AlxugFMQZLNEbqMhZjsJgU4_VJG_s1lBDeJ2nyG47FlzbrLav4KZZKN_xie_WbStOy6THZ3KEWblA71eeooeLv8RT1tG45SsN5W2nQU2Tr3apMF_wz1TRcVTluqJ7JTcfqTaPtnOqm0jxttK3gb93y2patt2fteUPt9UzVrvM99rHO_3mG7Tmuoqmu0zTahiuaT7qu4-lOy-DmttEwWqZjdxW93lRDY7dlsJW3LM9ybG499TTFdDxKValIoPBSpyeU-oefHz41UmuWQbHSd4CVmq3QQvsOYqWvE7bGygeT6wyszvFt-OyiBueJ21Jn5xfCcYDx4yAFXQfT5kYPgnjlR3EUNrs_8GU1uw-AKLF3gXx05pgk3gymUD6p1w_9BkCd5hurMY7UCIfaWYxD7czPIS3n3i_0gSVdnC-_LF9Wyr8A4sXm6-qERzgIWgoG7WpS0JMUaPiSPF8ZPgNixdiuiG1EwLqShBVzXoDoHpCP46-L1xLamMB4LYkxSe0_hg776uVAh2oLQL9HXo0hZ1ibF8CSPwuYzwK3vwPkzdj9V8aHiwCzkgQzQeCyyaL5tihZJLAA2S0gzRhZIT5iBa5CEteY-wJU9MPGiVFdHZvxAtbVJKxx_wW4Phz_sCHDtwwBiCQBjTjOTxKbMv_X5XC6zd63oUedIcvzlo--M0Zm4KTJHM3AocfoMPSzFSAM_0mF3KNHqUK_n6YAaf9V4J9Yhv5_A-kVcGQ%2C";
    QuerySearch(query, results);
    if (results.size() != 0)
        ythrow yexception() << "result size = " << results.size();
    return true;
}
bool InitConfig() override {
    const TString xmlParserConfig =
        "<XmlParser>\n"
            "<DOCTYPE>\n"
                "<Zones>\n"
                    "_ : _\n"
                "</Zones>\n"
                "<Attributes>\n"
                   "i_sdk_min : INTEGER,any/z_propset.i_sdk_min\n"
                   "i_sdk_max : INTEGER,any/z_propset.i_sdk_max\n"
                   "_ : LITERAL,any/z_propset._\n"
                "</Attributes>\n"
            "</DOCTYPE>\n"
        "</XmlParser>\n";
    const TString xmlParserFileName = GetRunDir() + "/xmlparser.conf";
    TUnbufferedFileOutput out(xmlParserFileName);
    out << xmlParserConfig;
    SetIndexerParams(ALL, 10, 1);
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = xmlParserFileName;
    return true;
}
};
