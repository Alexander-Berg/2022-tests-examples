#pragma once

#include "logger.h"

#include <util/datetime/base.h>
#include <util/string/cast.h>

#include <regex>

struct TLogRow {
    TInstant Time;
    NSolomon::NAgent::ELogLevel Level;
    TString FileName;
    ui32 LineNumber;
    TString Message;
};

static NSolomon::NAgent::ELogLevel ParseLogLevel(TStringBuf str) {
    using NSolomon::NAgent::ELogLevel;

    if ("FATAL" == str) return ELogLevel::FATAL;
    if ("ERROR" == str) return ELogLevel::ERROR;
    if ("WARN " == str) return ELogLevel::WARN;
    if ("INFO " == str) return ELogLevel::INFO;
    if ("DEBUG" == str) return ELogLevel::DEBUG;
    if ("TRACE" == str) return ELogLevel::TRACE;

    ythrow yexception() << "unknown log level: '" << str << '\'';
}

static TLogRow ParseLogRow(const TString& str) {
    static std::regex rowRe(
                "^([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}) " // (1) time
                "([A-Z ]{5}) "                                                         // (2) level
                "\\{([a-z0-9_\\.]+):"                                                  // (3) file name
                "([0-9]+)\\}: "                                                        // (4) line number
                "([^\n]*)\n?$"                                                         // (5) message
                , std::regex_constants::extended);

    std::cmatch match;
    bool isMatch = std::regex_match(str.c_str(), match, rowRe);

    Y_ENSURE(isMatch, "log row does not match format: '" << str << '\'');
    Y_ENSURE(match.size() == 6u, "expected 10 groups in log row: '" << str << '\'');

    TLogRow logRow;
    logRow.Time = TInstant::ParseIso8601Deprecated(match[1].str());
    logRow.Level = ParseLogLevel(match[2].str());
    logRow.FileName = match[3].str();
    logRow.LineNumber = FromString<ui32>(match[4].str());
    logRow.Message = match[5].str();
    return logRow;
}
