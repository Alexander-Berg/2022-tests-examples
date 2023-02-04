#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/charset/wide.h>

START_TEST_DEFINE(TestSynonyms)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "");
    messages[0].MutableDocument()->SetBody("<xml><text>opera</text></xml>");
    messages[0].MutableDocument()->SetMimeType("text/xml");
    messages[1].MutableDocument()->SetBody("<xml><text>опера</text></xml>");
    messages[1].MutableDocument()->SetMimeType("text/xml");
    messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    const TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());

    TString query = "text:((опера | opera))" + kps;
    TString queryWithTree = query + "&qtree=cHicdVGxSgNBEH2zuYR1SXEoQrhKrjBnqmAlqSTVVRKsNJUGAoeNElMEq4AIAUEEO-0UW0kTURBjbXWx8gvsBD_B2b09XAJe9W72vTczb9SuKkv4qMgVRKKORZW-ptN0Mhum44DWwlK_O-g3ohrW0VCyyEQwEZuI0cIO9krJ46c3IlwSrgm35MhrW6ppzUvW3NNuAUJYP2_eL6Hcy1BvCXVs0NDUNGp-kyQfgdMmRMQj1CkWrTvBE7FHb1XJnKA7RMRabKOjWCrSMXdcSuQAvRtSjpMvKhTBUAuj6Xtb3D90pE-BnrDgai1-cepPFotAzM7-cDoxuMwuHjd6ti_ErAtHcerUzzVOMKATIan9QVIug7_gjVQ8d6vi4VG3t6_PRP9eKPn6ce-TSUyUBybJrBDqjCim1lUeYayyF584vywUGtlIYCKxMVSPzfALvAjlEAFV7RqC1xDZAUNPTx8h-_sFl-6I6A%2C%2C";

    QuerySearch(query, results);
    if (results.size() != 2)
        ythrow yexception() << "result size = " << results.size() << ". Query = " << query;

    QuerySearch(queryWithTree, results);
    if (results.size() != 2)
        ythrow yexception() << "result size = " << results.size();
    ReopenIndexers();
    QuerySearch(queryWithTree, results);
    if (results.size() != 2)
        ythrow yexception() << "result size = " << results.size();
    return true;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 10, 1);
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    return true;
}
};
