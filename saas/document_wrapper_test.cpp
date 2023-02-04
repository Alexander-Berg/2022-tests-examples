#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

#include "document_wrapper.h"

using NRTYServer::NTrie::TDocumentWrapper;

namespace NRTYServer {
    namespace NTrie {
        namespace {
            NMetaProtocol::TDocument TestDoc(const TString& url) {
                NMetaProtocol::TDocument doc;
                doc.SetUrl(url);
                return doc;
            }

            TEST(TDocumentWrapperSuite, GoodDoc1) {
                THashMap<TString, ui32> suffixMap{
                    {"\tb\tc", 42},
                    {"\tb", 62},
                };
                auto doc = TestDoc("aa\tb\tc");
                TDocumentWrapper wrapper(doc, '\t', 0, suffixMap);
                EXPECT_EQ(wrapper.GetPrefix(), "aa");
                EXPECT_EQ(wrapper.GetSuffix(), 42);
                EXPECT_EQ(&wrapper.GetDocument(), &doc);

                auto doc2 = TestDoc("a\tb");
                TDocumentWrapper wrapper2(doc2, '\t', 0, suffixMap);
                EXPECT_TRUE(wrapper2 < wrapper);
                EXPECT_FALSE(wrapper < wrapper2);

                auto doc3 = TestDoc("aa\tb");
                TDocumentWrapper wrapper3(doc3, '\t', 0, suffixMap);
                EXPECT_TRUE(wrapper < wrapper3);
                EXPECT_FALSE(wrapper3 < wrapper);
            }

            TEST(TDocumentWrapperSuite, GoodDoc2) {
                auto doc = TestDoc("aa\tb\tc");
                TDocumentWrapper wrapper(doc, '\t', 1, {{"\tc", 71}});
                EXPECT_EQ(wrapper.GetPrefix(), "aa\tb");
                EXPECT_EQ(wrapper.GetSuffix(), 71);
                EXPECT_EQ(&wrapper.GetDocument(), &doc);
            }

            TEST(TDocumentWrapperSuite, BadDoc1) {
                auto doc = TestDoc("aa\tb");
                TDocumentWrapper wrapper(doc, '\t', 1, {{"\tb", 62}});
                EXPECT_EQ(wrapper.GetPrefix(), "");
                EXPECT_EQ(wrapper.GetSuffix(), 0);
                EXPECT_EQ(&wrapper.GetDocument(), &doc);
            }

            TEST(TDocumentWrapperSuite, BadDoc2) {
                auto doc = TestDoc("aa");
                TDocumentWrapper wrapper(doc, '\t', 0, {{"\tb", 62}});
                EXPECT_EQ(wrapper.GetPrefix(), "");
                EXPECT_EQ(wrapper.GetSuffix(), 0);
                EXPECT_EQ(&wrapper.GetDocument(), &doc);
            }
        }
    }
}
