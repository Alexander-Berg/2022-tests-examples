#pragma once

#include <yweb/robot/kiwi/clientlib/params/reader.h>

namespace NOxygen {
    class IObjectInputStream;
    class TObjectContext;
}

class TFromTuplesGenerator {
public:
    TFromTuplesGenerator(const TString& inputFile);

    NOxygen::TObjectContext GetExportData();

protected:
    TIFStream FileInput;
    THolder<NOxygen::IObjectInputStream> InputStream;
};

void ExportFromDump(const TString& dump, ui16 port = 0, const TString& host = "localhost", ui16 rps = 1);
