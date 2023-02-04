#include "report_filter.h"

#include <saas/rtyserver/components/trie/test_utils/test_utils.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <library/cpp/testing/unittest/registar.h>

namespace NRTYServer {
    namespace NTrie {
        namespace {
            void DoTest(const TDocsVector& input, const TDocsVector& expectedOutput) {
                THashMap<TString, ui32> suffixMap = {
                    {"\t255", 0},
                    {"\t0", 1}
                };
                {
                    TFakeReportBuilder reportBuilder;
                    TReportFilter filter(reportBuilder, '\t', 1, suffixMap);
                    for (auto& inp : input) {
                        auto doc = MakeDocument(inp);
                        filter.AddDocument(doc);
                    }
                    filter.Finalize("QDSaaS:");
                    reportBuilder.ExpectDocs(expectedOutput);
                }
                {
                    TFakeReportBuilder reportBuilder;
                    TReportFilter filter(reportBuilder, '\t', 1, suffixMap);
                    TList<NMetaProtocol::TDocument> docs;
                    for (auto& inp : input) {
                        docs.emplace_back(MakeDocument(inp));
                    }
                    filter.AddDocuments(std::move(docs));
                    filter.Finalize("QDSaaS:");
                    reportBuilder.ExpectDocs(expectedOutput);
                }
            }

            TEST(TTrieReportFilterSuite, NoDocsToFilter) {
                TDocsVector inputDocs = {
                    {
                        {"url", "url1"},
                        {"some_prop", "some_value1"},
                        {"QDSaaS:ns1", "value11"}
                    },
                    {
                        {"url", "url2"},
                        {"some_prop", "some_value2"},
                        {"QDSaaS:ns2", "value21"}
                    },
                    {
                        {"url", "url3"},
                        {"some_prop", "some_value3"},
                        {"QDSaaS:ns1", "value12"},
                        {"QDSaaS:ns2", "value22"}
                    }
                };
                DoTest(inputDocs, inputDocs);
            }

            TEST(TTrieReportFilterSuite, NoDuplicateProps) {
                TDocsVector inputDocs = {
                    {
                        {"url", "url1"},
                        {"some_prop", "some_value1"},
                        {"QDSaaS:ns1", "value11"}
                    },
                    {
                        {"url", ".7\turl2\t0"},
                        {"some_prop", "some_value2"},
                        {"QDSaaS:ns2", "value21"}
                    },
                    {
                        {"url", ".7\turl2\t255"},
                        {"some_prop", "some_value3"},
                        {"QDSaaS:ns1", "value12"},
                        {"QDSaaS:ns3", "value33"}
                    },
                    {
                        {"url", ".7\turl4\t0"},
                        {"some_prop", "some_value4"},
                        {"QDSaaS:ns1", "value14"},
                        {"QDSaaS:ns2", "value24"},
                        {"QDSaaS:ns3", "value34"}
                    }
                };
                TDocsVector expectedDocs = {
                    {
                        {"url", "url1"},
                        {"some_prop", "some_value1"},
                        {"QDSaaS:ns1", "value11"}
                    },
                    {
                        {"url", ".7\turl2\t255"},
                        {"some_prop", "some_value3"},
                        {"QDSaaS:ns1", "value12"},
                        {"QDSaaS:ns3", "value33"}
                    },
                    {
                        {"url", ".7\turl2\t0"},
                        {"some_prop", "some_value2"},
                        {"QDSaaS:ns2", "value21"}
                    },
                    {
                        {"url", ".7\turl4\t0"},
                        {"some_prop", "some_value4"},
                        {"QDSaaS:ns1", "value14"},
                        {"QDSaaS:ns2", "value24"},
                        {"QDSaaS:ns3", "value34"}
                    }
                };
                DoTest(inputDocs, expectedDocs);
            }
            TEST(TTrieReportFilterSuite, DuplicateProps) {
                TDocsVector inputDocs = {
                    {
                        {"url", "url1"},
                        {"some_prop", "some_value1"},
                        {"QDSaaS:ns1", "value11"}
                    },
                    {
                        {"url", ".7\turl2\t0"},
                        {"QDSaaS:ns2", "value22"},
                        {"QDSaaS:ns3", "value32"},
                        {"some_prop", "some_value2"},
                        {"QDSaaS:ns1", "value12"}
                    },
                    {
                        {"url", ".7\turl2\t255"},
                        {"QDSaaS:ns1", "value11"},
                        {"QDSaaS:ns3", "value31"},
                        {"some_prop", "some_value3"}
                    },
                    {
                        {"url", ".7\turl4\t0"},
                        {"some_prop", "some_value4"},
                        {"QDSaaS:ns1", "value14"},
                        {"QDSaaS:ns2", "value24"},
                        {"QDSaaS:ns3", "value34"}
                    }
                };
                TDocsVector expectedDocs = {
                    {
                        {"url", "url1"},
                        {"some_prop", "some_value1"},
                        {"QDSaaS:ns1", "value11"}
                    },
                    {
                        {"url", ".7\turl2\t255"},
                        {"QDSaaS:ns1", "value11"},
                        {"QDSaaS:ns3", "value31"},
                        {"some_prop", "some_value3"}
                    },
                    {
                        {"url", ".7\turl2\t0"},
                        {"QDSaaS:ns2", "value22"},
                        {"some_prop", "some_value2"},
                    },
                    {
                        {"url", ".7\turl4\t0"},
                        {"some_prop", "some_value4"},
                        {"QDSaaS:ns1", "value14"},
                        {"QDSaaS:ns2", "value24"},
                        {"QDSaaS:ns3", "value34"}
                    }
                };
                DoTest(inputDocs, expectedDocs);
            }
        }
    }
}

