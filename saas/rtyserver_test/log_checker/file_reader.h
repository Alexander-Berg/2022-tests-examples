#pragma once

#include "stream_reader.h"

#include <util/stream/file.h>

namespace NUtil {

    class TLogFileReader : public TStreamLogReader {
    public:
        TLogFileReader(const TString& fileName);

    private:
        TUnbufferedFileInput Input;
    };

}
