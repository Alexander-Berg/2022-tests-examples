#pragma once

#include "abstract.h"

#include <saas/rtyserver_test/common/test_abstract.h>

#include <library/cpp/object_factory/object_factory.h>
#include <library/cpp/logger/global/global.h>

#include <util/generic/ptr.h>

class TLogChecker {
public:
    typedef NObjectFactory::TParametrizedObjectFactory<TLogChecker, TString, TString> TFactory;
    typedef TAtomicSharedPtr<TLogChecker> TPtr;

    TLogChecker(const TString& logType);
    virtual ~TLogChecker() {}

    void Prepare(const TString& logFileName, bool prefixed, const TString& testName, const TString& resourcesDir);
    void Check();

    virtual TString GetLoggerType() const = 0;
    virtual TRtyTestNodeType GetBinaryType() const = 0;

private:
    NUtil::ILog::TPtr ReadLog(const TString& fileName) const;
    virtual NUtil::ILog::TPtr CreateLog() const;
    virtual NUtil::ILogReader::TPtr CreateLogReader(const TString& fileName) const = 0;

protected:
    bool Prefixed;

private:
    TString LogType;
    TString LogFileName;
    TString CanonFileName;
    NUtil::ILog::TPtr CanonLog;
};
