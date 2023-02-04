#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestMetaSearchZeroDocsInGroups)
bool Run() override {
    const int CountMessages = 20;
    TAttrMap::value_type map1, map2;
    map1["to_group"] = 1;
    map2["to_group"] = 2;
    map1["to_sort"] = 1000;
    map2["to_sort"] = 1000;
    TVector<NRTYServer::TMessage> messagesForMemory1, messagesForMemory2;
    GenerateInput(messagesForMemory1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(CountMessages, map1));
    GenerateInput(messagesForMemory2, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(CountMessages, map2));
    IndexMessages(messagesForMemory1, REALTIME, 1);
    ReopenIndexers();
    IndexMessages(messagesForMemory2, REALTIME, 1);
    CheckSearchResults(messagesForMemory1);
    CheckSearchResults(messagesForMemory2);

    TVector<TDocSearchInfo> results;
    QuerySearch("\"body\"&g=1.to_group.10000.0.-1.0.0.-1.to_sort.0..0.0", results, nullptr, nullptr, true);

    return true;
}
};
