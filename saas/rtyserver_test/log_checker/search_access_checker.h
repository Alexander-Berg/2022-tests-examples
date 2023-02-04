#pragma once

#include "log_checker.h"

class TSearchProxyAccessLogChecker : public TLogChecker {
private:
    using TLogChecker::TLogChecker;

    NUtil::ILogReader::TPtr CreateLogReader(const TString& fileName) const override;

    static TFactory::TRegistrator<TSearchProxyAccessLogChecker> Registrator;

public:
    TRtyTestNodeType GetBinaryType() const override;
    TString GetLoggerType() const override;
};
