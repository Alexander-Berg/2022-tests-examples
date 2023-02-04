#include "stream_reader.h"

#include <library/cpp/logger/global/global.h>
#include <util/string/cast.h>
#include <util/string/strip.h>

namespace NUtil {

    TStreamLogReader::TStreamLogReader(IInputStream& stream, const TString& description)
        : Stream(stream)
        , Description(description)
        , LineNo(0)
    {
    }

    ILogRecord::TPtr TStreamLogReader::Next() {
        TString line;
        if (!Stream.ReadLine(line))
            return nullptr;

        line = PrepareLine(line);
        IStreamLogRecord* record = CreateLogRecord("line #" + ToString(++LineNo) + " in " + Description);
        CHECK_WITH_LOG(record);
        record->ParseLine(line);

        return record;
    }

    TString TStreamLogReader::GetDescription() const {
        return Description;
    }

    TString TStreamLogReader::PrepareLine(const TString& line) const {
        return Strip(line);
    }

}
