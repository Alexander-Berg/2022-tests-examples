#include "file_reader.h"

namespace NUtil {

    TLogFileReader::TLogFileReader(const TString& fileName)
        : TStreamLogReader(Input, "file '" + fileName + "'")
        , Input(fileName)
    {
    }

}
