#pragma once

#include "parsed_log_record.h"

#include <library/cpp/string_utils/scan/scan.h>
#include <util/generic/yexception.h>

namespace NUtil {

    template<char sep = '\t', char sepKV = '='>
    class TTskvLogRecord : public TParsedLogRecord {
    public:
        TTskvLogRecord(const TString& description)
            : TParsedLogRecord(description)
        {
        }

        void operator()(const TStringBuf& key, const TStringBuf& value) {
            if (Values.find(key) != Values.end())
                ythrow yexception() << "Duplicate field '" << key << "' in " << Description;
            Values[(TString)key] = KeepValue((TString)key) ? value : "";
        }
        void ParseLine(const TString& line) override {
            ScanKeyValue<true, sep, sepKV>(line, *this);
        }

    private:

        virtual bool KeepValue(const TString& /*key*/) const {
            return true;
        }
    };

}
