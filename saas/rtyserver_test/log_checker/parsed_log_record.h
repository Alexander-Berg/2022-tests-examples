#pragma once

#include "stream_reader.h"

#include <util/generic/map.h>

namespace NUtil {

    class TParsedLogRecord : public IStreamLogRecord {
    public:
        typedef TMap<TString, TString> TValues;

        TParsedLogRecord(const TString& description);
        const TValues& GetValues() const;
        int Compare(TPtr record, TString& info) const override;
        bool IsValuableRecord() const override;
        bool IsValuableKey(const TString& key) const override;

    private:
        bool IsLessOrEqual(const TParsedLogRecord& record, TString& info) const;

    protected:
        const TString Description;
        TValues Values;
    };

}
