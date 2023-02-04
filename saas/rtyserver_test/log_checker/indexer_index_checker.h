#pragma once

#include "log_checker.h"

class TIndexerProxyIndexLogChecker : public TLogChecker {
private:
    using TLogChecker::TLogChecker;

    NUtil::ILog::TPtr CreateLog() const override;
    NUtil::ILogReader::TPtr CreateLogReader(const TString& fileName) const override;

    static TFactory::TRegistrator<TIndexerProxyIndexLogChecker> Registrator;

public:
    TString GetLoggerType() const override;
    TRtyTestNodeType GetBinaryType() const override;
};
