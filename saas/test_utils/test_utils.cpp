#include "test_utils.h"

#include <library/cpp/testing/unittest/gtest.h>

#include <util/generic/list.h>

namespace NRTYServer {
    namespace NTrie {
        NMetaProtocol::TDocument MakeDocument(const TPropsVector& props) {
            EXPECT_FALSE(props.empty());
            EXPECT_EQ(props.front().first, "url");
            NMetaProtocol::TDocument doc;
            doc.SetUrl(props.front().second);
            if (props.size() == 1) {
                return doc;
            }
            auto& archiveInfo = *doc.MutableArchiveInfo();
            for (size_t i = 1, imax = props.size(); i < imax; ++i) {
                auto& prop = *archiveInfo.AddGtaRelatedAttribute();
                prop.SetKey(props[i].first);
                prop.SetValue(props[i].second);
            }
            return doc;
        }

        void TFakeReportBuilder::AddDocument(NMetaProtocol::TDocument& doc) {
            TPropsVector props;
            if (!doc.HasArchiveInfo()) {
                props.reserve(1);
                props.emplace_back("url", doc.GetUrl());
            } else {
                auto& archiveInfo = doc.GetArchiveInfo();
                props.reserve(archiveInfo.GtaRelatedAttributeSize() + 1);
                props.emplace_back("url", doc.GetUrl());
                for (auto& prop : archiveInfo.GetGtaRelatedAttribute()) {
                    props.emplace_back(prop.GetKey(), prop.GetValue());
                }
            }
            ActualDocs.emplace_back(std::move(props));
        }

        void TFakeReportBuilder::AddDocuments(TList<NMetaProtocol::TDocument> docs) {
            for (auto& doc : docs) {
                AddDocument(doc);
            }
        }

        void TFakeReportBuilder::ExpectDocs(const TDocsVector& expectedDocs) {
            EXPECT_EQ(ActualDocs, expectedDocs);
        }
    }
}

