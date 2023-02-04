#pragma once

#include <saas/library/report_builder/abstract.h>

#include <search/idl/meta.pb.h>

#include <util/generic/vector.h>

namespace NRTYServer {
    namespace NTrie {
        using TPropsVector = TVector<std::pair<TString, TString>>;
        using TDocsVector = TVector<TPropsVector>;

        NMetaProtocol::TDocument MakeDocument(const TPropsVector& props);

        struct TFakeReportBuilder : ISimpleReportBuilder {
            void AddDocument(NMetaProtocol::TDocument& doc) override final;
            void AddDocuments(TList<NMetaProtocol::TDocument> docs) override final;
            void ExpectDocs(const TDocsVector& expectedDocs);
        private:
            TDocsVector ActualDocs;
        };
    }
}

