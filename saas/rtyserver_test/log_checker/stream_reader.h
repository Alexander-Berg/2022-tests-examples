#pragma once

#include "abstract.h"

#include <util/stream/input.h>

namespace NUtil {

    class IStreamLogRecord : public ILogRecord {
    public:
        virtual void ParseLine(const TString& line) = 0;
    };

    class TStreamLogReader : public ILogReader {
    public:
        TStreamLogReader(IInputStream& stream, const TString& description);
        ILogRecord::TPtr Next() override;
        TString GetDescription() const override;

    private:
        virtual TString PrepareLine(const TString& line) const;
        virtual IStreamLogRecord* CreateLogRecord(const TString& description) const = 0;

    private:
        IInputStream& Stream;
        const TString Description;
        size_t LineNo;
    };

}
