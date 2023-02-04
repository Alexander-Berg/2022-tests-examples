#include "trie_search_request.h"

#include <saas/rtyserver/components/trie/test_utils/test_utils.h>

#include <kernel/saas_trie/test_utils/fake_disk_trie.h>
#include <kernel/saas_trie/test_utils/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

namespace NRTYServer {
    namespace {
        struct TFakeStorage {
            TFakeStorage(NTrie::TDocsVector docs)
                : Docs(std::move(docs))
            {
                TVector<std::pair<TString, ui64>> trieData;
                for (ui64 docId = 0, nDocs = Docs.size(); docId < nDocs; ++docId) {
                    Y_ENSURE(!Docs[docId].empty());
                    Y_ENSURE(Docs[docId].front().first == "url");
                    trieData.emplace_back("0\t" + Docs[docId].front().second, docId);
                }
                FakeTrie = NSaasTrie::NTesting::CreateFakeDiskTrie(trieData);
            }

            const NSaasTrie::ITrieStorage& GetTrie() const {
                return *FakeTrie.first;
            }

            void DoSearch(TTrieSearchRequest& request) {
                while (request.NextIterator()) {
                    auto trieIterator = request.CreateIterator(GetTrie());
                    FillReport(*trieIterator, request.GetReport());
                }
            }

            void FillReport(NSaasTrie::ITrieStorageIterator& trieIterator, ISimpleReportBuilder& report) {
                if (trieIterator.AtEnd()) {
                    return;
                }
                do {
                    ui64 docId = trieIterator.GetValue();
                    Y_ENSURE(docId < Docs.size());
                    auto protoDocument = NTrie::MakeDocument(Docs[docId]);
                    report.AddDocument(protoDocument);
                } while (trieIterator.Next());
            }

        private:
            NTrie::TDocsVector Docs;
            NSaasTrie::NTesting::TFakeDiskTrieContext FakeTrie;
        };

        TEST(TTrieSearchRequestSuite, PrefixSearchSingle) {
            auto fakeTrie = NSaasTrie::NTesting::CreateFakeDiskTrie({
                {"0\tapple\tred", 10},
                {"0\torange\tred", 20},
                {"0\tpeach\tyellow", 30}
            });
            TCgiParameters cgi{
                {"text", "peach"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, nullptr, false, "", "", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            EXPECT_TRUE(request.NextIterator());
            EXPECT_EQ(&request.GetReport(), &report);

            NSaasTrie::NTesting::CheckIterator(request.CreateIterator(*fakeTrie.first), {{"\tyellow", 30}});

            EXPECT_FALSE(request.NextIterator());
        }

        TEST(TTrieSearchRequestSuite, PrefixSearchDouble) {
            auto fakeTrie = NSaasTrie::NTesting::CreateFakeDiskTrie({
                {"0\tapple\tgreen", 10},
                {"0\torange\tred", 20},
                {"0\tpeach\tyellow", 30}
            });
            TCgiParameters cgi{
                {"text", "orange"},
                {"text", "apple"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, nullptr, false, "", "", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            EXPECT_TRUE(request.NextIterator());
            EXPECT_EQ(&request.GetReport(), &report);

            NSaasTrie::NTesting::CheckIterator(request.CreateIterator(*fakeTrie.first), {
                {"\tred", 20},
                {"\tgreen", 10}
            });

            EXPECT_FALSE(request.NextIterator());
        }

        TString MakeComplexKey(const TString& mainKey, bool lastRealmUnique, const TVector<TVector<TString>>& realms) {
            NSaasTrie::TComplexKey key;
            key.SetMainKey(mainKey);
            if (lastRealmUnique) {
                key.SetLastRealmUnique(true);
            }
            size_t realmIndex = 0;
            for (auto& realm : realms) {
                TString realmName = ToString(realmIndex);
                key.AddKeyRealms(realmName);
                auto protoRealm = key.AddAllRealms();
                protoRealm->SetName(realmName);
                for (auto& elem : realm) {
                    protoRealm->AddKey(elem);
                }
                ++realmIndex;
            }
            return SerializeToCgi(key, true);
        }

        TEST(TTrieSearchRequestSuite, ComplexKeySearchSingleNotUnique) {
            auto fakeTrie = NSaasTrie::NTesting::CreateFakeDiskTrie({
                {"0\t.1.5\tapple\t0", 10},
                {"0\t.1.5\torange\t100", 20},
                {"0\t.1.5\tpeach\t0", 30},
                {"0\t.1.5\tapple\t100", 40},
                {"0\t.1.5\tapple\t255", 50}
            });
            auto keyStr = MakeComplexKey(".1.5", false, {
                {"\tapple"},
                {"\t255", "\t100", "\t0"}
            });
            TCgiParameters cgi{
                {"text", keyStr}
            };
            TMap<TString, TString> trieParams{
                {"key_type", "complex_key_packed"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, &trieParams, false, "", "", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            EXPECT_TRUE(request.NextIterator());
            EXPECT_EQ(&request.GetReport(), &report);

            NSaasTrie::NTesting::CheckIterator(request.CreateIterator(*fakeTrie.first), {
                {"0\t.1.5\tapple\t255", 50},
                {"0\t.1.5\tapple\t100", 40},
                {"0\t.1.5\tapple\t0", 10}
            });

            EXPECT_FALSE(request.NextIterator());
        }

        TEST(TTrieSearchRequestSuite, ComplexKeySearchDoubleNotUnique) {
            auto fakeTrie = NSaasTrie::NTesting::CreateFakeDiskTrie({
                {"0\t.1.5\tapple\t0", 10},
                {"0\t.1.5\torange\t100", 20},
                {"0\t.1.5\tpeach\t0", 30},
                {"0\t.1.5\tapple\t100", 40},
                {"0\t.1.5\tapple\t255", 50}
            });
            auto keyStr = MakeComplexKey(".1.5", false, {
                {"\tapple"},
                {"\t100", "\t0"}
            });
            auto keyStr2 = MakeComplexKey(".1.5", false, {
                {"\tpeach"},
                {"\t100", "\t0"}
            });
            TCgiParameters cgi{
                {"text", keyStr},
                {"text", keyStr2}
            };
            TMap<TString, TString> trieParams{
                {"key_type", "complex_key_packed"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, &trieParams, false, "", "", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            EXPECT_TRUE(request.NextIterator());
            EXPECT_EQ(&request.GetReport(), &report);

            NSaasTrie::NTesting::CheckIterator(request.CreateIterator(*fakeTrie.first), {
                {"0\t.1.5\tapple\t100", 40},
                {"0\t.1.5\tapple\t0", 10},
                {"0\t.1.5\tpeach\t0", 30}
            });

            EXPECT_FALSE(request.NextIterator());
        }

        TEST(TTrieSearchRequestSuite, ComplexKeySearchSingleUnique) {
            TFakeStorage fakeStorage({
                {
                    {"url", ".1.5\tapple\t0"},
                    {"QDSaaS:prop1", "value11"},
                    {"QDSaaS:prop2", "value12"}
                },
                {
                    {"url", ".1.5\torange\t100"},
                    {"QDSaaS:prop1", "value21"},
                    {"QDSaaS:prop2", "value22"}
                },
                {
                    {"url", ".1.5\tpeach\t0"},
                    {"QDSaaS:prop1", "value31"},
                    {"QDSaaS:prop2", "value32"}
                },
                {
                    {"url", ".1.5\tapple\t100"},
                    {"QDSaaS:prop1", "value41"},
                    {"QDSaaS:prop2", "value42"}
                },
                {
                    {"url", ".1.5\tapple\t255"},
                    {"QDSaaS:prop1", "value51"},
                    {"QDSaaS:prop2", "value52"}
                }
            });
            auto keyStr = MakeComplexKey(".1.5", true, {
                {"\tapple", "\torange"},
                {"\t255", "\t100", "\t0"}
            });
            TCgiParameters cgi{
                {"text", keyStr}
            };
            TMap<TString, TString> trieParams{
                {"key_type", "complex_key_packed"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, &trieParams, false, "", "QDSaaS:", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            fakeStorage.DoSearch(request);

            report.ExpectDocs({
                {
                    {"url", ".1.5\tapple\t255"},
                    {"QDSaaS:prop1", "value51"},
                    {"QDSaaS:prop2", "value52"}
                },
                {
                    {"url", ".1.5\tapple\t100"}
                },
                {
                    {"url", ".1.5\tapple\t0"}
                },
                {
                    {"url", ".1.5\torange\t100"},
                    {"QDSaaS:prop1", "value21"},
                    {"QDSaaS:prop2", "value22"}
                }
            });
        }

        TEST(TTrieSearchRequestSuite, ComplexKeySearchDoubleUnique) {
            TFakeStorage fakeStorage({
                {
                    {"url", ".1.5\tapple\t0"},
                    {"QDSaaS:prop1", "value11"},
                    {"QDSaaS:prop2", "value12"}
                },
                {
                    {"url", ".1.5\torange\t100"},
                    {"QDSaaS:prop1", "value21"},
                    {"QDSaaS:prop2", "value22"}
                },
                {
                    {"url", ".1.5\tpeach\t0"},
                    {"QDSaaS:prop1", "value31"},
                    {"QDSaaS:prop2", "value32"}
                },
                {
                    {"url", ".1.5\tapple\t100"},
                    {"QDSaaS:prop1", "value41"},
                    {"QDSaaS:prop2", "value42"}
                },
                {
                    {"url", ".1.5\tapple\t255"},
                    {"QDSaaS:prop1", "value51"},
                    {"QDSaaS:prop2", "value52"}
                }
            });
            auto keyStr = MakeComplexKey(".1.5", true, {
                {"\torange"},
                {"\t255", "\t100", "\t0"}
            });
            auto keyStr2 = MakeComplexKey(".1.5", true, {
                {"\tapple"},
                {"\t255", "\t100", "\t0"}
            });
            TCgiParameters cgi{
                {"text", keyStr},
                {"text", keyStr2}
            };
            TMap<TString, TString> trieParams{
                {"key_type", "complex_key_packed"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, &trieParams, false, "", "QDSaaS:", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            fakeStorage.DoSearch(request);

            report.ExpectDocs({
                {
                    {"url", ".1.5\torange\t100"},
                    {"QDSaaS:prop1", "value21"},
                    {"QDSaaS:prop2", "value22"}
                },
                {
                    {"url", ".1.5\tapple\t255"},
                    {"QDSaaS:prop1", "value51"},
                    {"QDSaaS:prop2", "value52"}
                },
                {
                    {"url", ".1.5\tapple\t100"}
                },
                {
                    {"url", ".1.5\tapple\t0"}
                }
            });
        }

        TEST(TTrieSearchRequestSuite, ComplexKeySearchMixed) {
            TFakeStorage fakeStorage({
                {
                    {"url", ".1.5\tapple\t0"},
                    {"QDSaaS:prop1", "value11"},
                    {"QDSaaS:prop2", "value12"}
                },
                {
                    {"url", ".1.5\torange\t100"},
                    {"QDSaaS:prop1", "value21"},
                    {"QDSaaS:prop2", "value22"}
                },
                {
                    {"url", ".1.5\tpeach\t0"},
                    {"QDSaaS:prop1", "value31"},
                    {"QDSaaS:prop2", "value32"}
                },
                {
                    {"url", ".1.5\tapple\t100"},
                    {"QDSaaS:prop1", "value41"},
                    {"QDSaaS:prop2", "value42"}
                },
                {
                    {"url", ".1.5\tapple\t255"},
                    {"QDSaaS:prop1", "value51"},
                    {"QDSaaS:prop2", "value52"}
                }
            });
            auto keyStr = MakeComplexKey(".1.5", true, {
                {"\torange"},
                {"\t255", "\t100", "\t0"}
            });
            auto keyStr2 = MakeComplexKey(".1.5", true, {
                {"\tapple"},
                {"\t255", "\t100", "\t0"}
            });
            auto keyStr3 = MakeComplexKey(".1.5", false, {
                {"\tpeach"},
                {"\t255", "\t100", "\t0"}
            });
            TCgiParameters cgi{
                {"text", keyStr},
                {"text", keyStr2},
                {"text", keyStr3}
            };
            TMap<TString, TString> trieParams{
                {"key_type", "complex_key_packed"}
            };
            NTrie::TFakeReportBuilder report;
            TTrieSearchRequest request(cgi, &trieParams, false, "", "QDSaaS:", report);

            EXPECT_TRUE(request.IsValid());
            EXPECT_TRUE(request.GetMaxDocs() > 0);

            fakeStorage.DoSearch(request);

            report.ExpectDocs({
                {
                    {"url", ".1.5\tpeach\t0"},
                    {"QDSaaS:prop1", "value31"},
                    {"QDSaaS:prop2", "value32"}
                },
                {
                    {"url", ".1.5\torange\t100"},
                    {"QDSaaS:prop1", "value21"},
                    {"QDSaaS:prop2", "value22"}
                },
                {
                    {"url", ".1.5\tapple\t255"},
                    {"QDSaaS:prop1", "value51"},
                    {"QDSaaS:prop2", "value52"}
                },
                {
                    {"url", ".1.5\tapple\t100"}
                },
                {
                    {"url", ".1.5\tapple\t0"}
                }
            });
        }
    }
}
