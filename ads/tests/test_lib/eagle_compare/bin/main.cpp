#include <ads/bsyeti/tests/test_lib/eagle_compare/lib/compare.h>
#include <ads/bsyeti/tests/test_lib/eagle_answers_proto/answers.pb.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/logger/global/global.h>

#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/stream/file.h>


int main(int argc, const char** argv) {
    DoInitGlobalLog(CreateLogBackend("cout", static_cast<ELogPriority>(6), true));
    NLastGetopt::TOpts options;
    options.SetFreeArgsNum(2);
    options.SetFreeArgTitle(0, "msg1", "canonfile path");
    options.SetFreeArgTitle(1, "msg2", "newfile path");
    TString msg1Path;
    TString msg2Path;
    TVector<TString> uids;
    options
        .AddLongOption("uid", "-- uid")
        .RequiredArgument()
        .Handler1T<TString>([&] (const TString& uid) {
            uids.push_back(uid);
        });

    NLastGetopt::TOptsParseResult argsOpts(&options, argc, argv);

    msg1Path = argsOpts.GetFreeArgs().at(0);
    msg2Path = argsOpts.GetFreeArgs().at(1);

    NTestsResult::TTests testPack1;
    {
        TFileInput in(msg1Path);
        testPack1.ParseFromArcadiaStream(&in);
    }
    NTestsResult::TTests testPack2;
    {
        TFileInput in(msg2Path);
        testPack2.ParseFromArcadiaStream(&in);
    }
    if (NBSYeti::CompareAnswers(testPack1, testPack2, uids)) {
        return 0;
    }
    return 1;
}

