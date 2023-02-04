#include "ki_dumpers.h"

#include <robot/jupiter/library/rtdoc/printer/text_utils.h>

#include <library/cpp/logger/global/global.h>

TString TRTYKeyInvDumper::EscapeKey(const TStringBuf& key) {
    return NRtDocTest::TTextDiffHelper::EscapeKey(key);
}

TVector<TString> TRTYKeyInvDumper::ZipDiffDumps(const TVector<TString>& a, const TVector<TString>& b) {
    return NRtDocTest::TTextDiffHelper::ZipDiffDumps(a, b);
}

bool TRTYKeyInvDumper::ZipDiffHelper(const TVector<TString>& expected, const TVector<TString>& actual, const TFsPath expectedFile, const TFsPath actualFile) {
    const TFsPath diffOutput = TString(actualFile) + ".diff";
    const bool isSame = NRtDocTest::TTextDiffHelper::ZipDiffHelper(expected, actual, expectedFile, actualFile, diffOutput);
    if (!isSame) {
        INFO_LOG << "CompareDumps failed: " << actualFile.Basename() << " does not match. Diff saved to " << diffOutput << Endl;
    }
    return isSame;
}
