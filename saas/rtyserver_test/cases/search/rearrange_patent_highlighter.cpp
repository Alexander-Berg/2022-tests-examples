#include <saas/rtyserver_test/testerlib/rtyserver_test.h>


namespace {
    // request tree for "body"
    constexpr auto requestTree = "cHicXY-tqgJRFEa_78yAx8EwaJGTZIqDaTCJScRgFMPlMkmG"
                                 "EbUoaFGTUYsINpMIVvEn324Un-Q-gsczWNxpsxdrwXZ-nIyE"
                                 "i7wswBcBsnY0imcKHkooo5qRtmbQDAFqaKKFX3TQ_18_z3JD"
                                 "7IgDjXIl_gg9d-LBOECF9SMlXSjDPfgM2NoK43Ncc8w5z3e5"
                                 "gjaXoTieorRLZQ26E9-KUlpl0Sy24szPRVJfRFGzPqZi3Pgu"
                                 "rEKxv4W8fCKxcamY1KwkktYRazDsJZW5kGJB6FdedOQx1Q,,";

    // request tree for "code"
    constexpr auto requestTreeCode = "emVOBAAAAAAAACi1L_1gTgO9EAAGXWNBMI0TAOxVsHfQ"
                                     "eAsIAmY9pYIsumZMBP_GAYzp_UE2EQ026igkzUC3h3Do"
                                     "rhZT4xLJjZBld_L8P2Z8kfDb3zN933tSAE8AUwCWpjzQ"
                                     "uWyyb74J274If0WUjULUUXwT_zRfNCvwhz_2-ICn15U1"
                                     "jtS72PDljS2Nq4JQcTZEDR6IgIRGGUjIVhwcFQEpykYh"
                                     "SoCnrquncfS7gCIwoxAFDZDUwudzvqwtUTYKUdLx7ycd"
                                     "nu98elLrKlvXdV3-yR-x6Z9fzgRgcS4d9Vkd73hyfpSb"
                                     "-KcAK0hcSKFBzje2LKEFBl9BKUhfw3gwDprbWxqXHTaW"
                                     "DcSGeR9QVWSCdZkD3tMSIH1qWsAgxEmNFo8ZzG126ZjZ"
                                     "cBCHEwVJQEDSkjW3GZe-1JjB3DrIUOn4MIy1dMRgKRkU"
                                     "VHIPdzy914iy_skf8Tr6UdZT00tPw1jLRwxmD3c8v9ao"
                                     "bA9WpJesuZ3QovIRWlTq48PcrmwsGyUfIebWw0bSolI_"
                                     "hNjcqmwpGhSEYkSJgyfB8Pn82jpRQMVKNwDHbx7zZiq4"
                                     "z1XlAnrgilbBJTuQjfVcsUroyUOCEi6aI2KHs180rHKA"
                                     "lHhkTzgA8RIl3APHahJnjcCRiEGLKKIQED1r6GMziBZv"
                                     "MBsTZQ0C6AMcA6EI0YWxd45W1vBi3UwnWBkOnEFbvoEM"
                                     "OAFcMpAXNoSiMypQA8GBEVcoA_kIPuRDMaYe";

    constexpr auto missingAttr = "gta_missing";
    constexpr auto missingInput = "a text without keywords";
    constexpr auto missingOutput = "a text without keywords";

    constexpr auto singleAttr = "gta_single";
    constexpr auto singleInput = "a text with body keyword";
    constexpr auto singleOutput = "a text with \a[body\a] keyword";

    constexpr auto pluralAttr = "gta_plural";
    constexpr auto pluralInput = "something about bodies and stuff";
    constexpr auto pluralOutput = "something about \a[bodies\a] and stuff";

    constexpr auto bodyAttr = "gta_body";
    constexpr auto bodyInput = "<body>body</body>";
    constexpr auto bodyOutput = "\a[body\a]";

    constexpr auto tagsAttr = "gta_tags";
    constexpr auto tagsInput = "simple body, <b>bold body</b>, <i>italic body</i> and <br /> whatever";
    constexpr auto tagsOutput = "simple \a[body\a], <b>bold \a[body\a]</b>, <i>italic \a[body\a]</i> and <br /> whatever";

    constexpr auto codeAttr = "gta_code";
    constexpr auto codeInput = "code<code>code</code>code";
    constexpr auto codeOutput = "\a[code\a]<code>\a[code\a]</code>\a[code\a]";

    void FillDocAttributes(TVector<NRTYServer::TMessage>& messages) {
        for (auto& message : messages) {
            auto& doc = *message.MutableDocument();

            auto* gta = doc.AddDocumentProperties();
            gta->SetName(missingAttr);
            gta->SetValue(missingInput);

            gta = doc.AddDocumentProperties();
            gta->SetName(singleAttr);
            gta->SetValue(singleInput);

            gta = doc.AddDocumentProperties();
            gta->SetName(pluralAttr);
            gta->SetValue(pluralInput);

            gta = doc.AddDocumentProperties();
            gta->SetName(bodyAttr);
            gta->SetValue(bodyInput);

            gta = doc.AddDocumentProperties();
            gta->SetName(tagsAttr);
            gta->SetValue(tagsInput);

            gta = doc.AddDocumentProperties();
            gta->SetName(codeAttr);
            gta->SetValue(codeInput);
        }
    }

} // end namespace


START_TEST_DEFINE(TestRearrangePatentHighlighter)

bool Run() override {
    bool prefixed = GetIsPrefixed();

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    FillDocAttributes(messages);

    TString kps;
    if (prefixed) {
        messages[0].MutableDocument()->SetKeyPrefix(1);
        kps = "&kps=1";
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    CHECK_TEST_EQ(messages.size(), 1);
    const auto& document = messages.front().GetDocument();

    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> docProperties;
    THashMultiMap<TString, TString> searchProperties;

    auto search = [&results, &docProperties, &searchProperties, &document, this](auto requestTree, auto ...attrs) {
        const auto query = TString::Join(
            "url:\"", document.GetUrl(), "\"",
            "&timeout=100000000",
            "&kps=", ToString(document.GetKeyPrefix()),
            "&hl_qtree=", requestTree,
            TString::Join("&hl_gta=", attrs)...);

        QuerySearch(query, results, &docProperties, &searchProperties, true);
    };

    search(requestTree, missingAttr, singleAttr, pluralAttr, bodyAttr, tagsAttr);

    CHECK_TEST_EQ(docProperties.size(), 1);
    auto attributes = docProperties.front();

    const auto missingIt = attributes->find(missingAttr);
    CHECK_TEST_TRUE(missingIt != attributes->end());
    CHECK_TEST_EQ(missingIt->second, missingOutput);

    const auto singleIt = attributes->find(singleAttr);
    CHECK_TEST_TRUE(singleIt != attributes->end());
    CHECK_TEST_EQ(singleIt->second, singleOutput);

    const auto pluralIt = attributes->find(pluralAttr);
    CHECK_TEST_TRUE(pluralIt != attributes->end());
    CHECK_TEST_EQ(pluralIt->second, pluralOutput);

    const auto bodyIt = attributes->find(bodyAttr);
    CHECK_TEST_TRUE(bodyIt != attributes->end());
    CHECK_TEST_EQ(bodyIt->second, bodyOutput);

    const auto tagsIt = attributes->find(tagsAttr);
    CHECK_TEST_TRUE(tagsIt != attributes->end());
    CHECK_TEST_EQ(tagsIt->second, tagsOutput);

    search(requestTreeCode, codeAttr);

    CHECK_TEST_EQ(docProperties.size(), 1);
    attributes = docProperties.front();

    const auto codeIt = attributes->find(codeAttr);
    CHECK_TEST_TRUE(codeIt != attributes->end());
    CHECK_TEST_EQ(codeIt->second, codeOutput);

    return true;
}

bool InitConfig() override {
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "PatentHighlighter";
    return true;
}

}; // TestPatentHighlighter
