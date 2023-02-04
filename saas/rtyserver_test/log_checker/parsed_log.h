#pragma once

#include "abstract.h"

#include <util/generic/vector.h>

namespace NUtil {

    class TParsedLog : public ILog {
    public:
        typedef TVector<ILogRecord::TPtr> TRecords;

        void Read(ILogReader::TPtr reader) override;
        int Compare(TPtr log, TString& info) const override;
        bool IsValuableRecord(ILogRecord::TPtr record) const override;
        const TString& GetDescription() const;

    private:
        virtual void CompareStart() const {}
        virtual void CompareFinish() const {}

    protected:
        TString Description;
        TRecords Records;
    };

}
