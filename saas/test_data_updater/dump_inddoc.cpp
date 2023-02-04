#include "dump_inddoc.h"

#include <saas/tools/distcl/lib/inddoc.h>

#include <saas/api/clientapi.h>
#include <saas/util/logging/tskv_log.h>
#include <saas/util/hex.h>

#include <yweb/protos/indexeddoc.pb.h>

#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/json/json_reader.h>
#include <library/cpp/protobuf/json/proto2json.h>

#include <google/protobuf/text_format.h>
#include <util/stream/file.h>
#include <util/string/cast.h>
#include <util/string/strip.h>
#include <util/generic/yexception.h>

using namespace NLastGetopt;

static bool ProcessNext(const TString& line, TIndDocOutput& output) {
    if (line.length() <= 10 && StripString(line).empty())
        return true;

    TMap<TString, TString> parseResult;
    NUtil::TTSKVRecordParser::Parse<'\t', '='>(line, parseResult);
    if (parseResult["tskv_format"] != "saas-log-json-dump")
        return true;

    NJson::TJsonValue json;

    TStringInput is(parseResult["message"]);
    Y_ENSURE_EX(NJson::ReadJsonTree(&is, true, &json), yexception() << "Incorrect json for unixtime=" << parseResult["unixtime"]);

    auto action = MakeAtomicShared<NSaas::TAction>();
    action->ParseFromJson(json);

    NSaas::TAction::TActionType type = action->GetActionType();
    if (type != NSaas::TAction::atAdd && type != NSaas::TAction::atModify)
        return true;
    if (!action->HasIndexedDoc())
        return true;

    TMaybe<TString> fileName = output.NextFile();
    if (!fileName.Defined())
        return false; //requested amount of documents has been written

    Cout << fileName << " : " << action->GetDocument().GetUrl() << Endl;
    output.Write(*fileName, action->GetDocument().GetIndexedDoc());
    return true;
}

int main_dump2inddoc(int argc, const char** argv) {
    TIndDocOutput::TOptions inddocOpts;
    THolder<TIndDocOutput> inddoc;

    TOpts options = TOpts::Default();
    options.AddHelpOption();
    options.AddVersionOption();
    options.AddLongOption('i', "input", "indexerproxy dump file").RequiredArgument("INPUT").Required();
    options.AddLongOption("docs-dir", "output directory for inddoc files").StoreResult(&inddocOpts.OutputDir).DefaultValue(".");
    options.AddLongOption("docs-begin", "inddoc index range begin").StoreResult(&inddocOpts.NumBegin).DefaultValue("1");
    options.AddLongOption("docs-end", "inddoc index range end").StoreResult(&inddocOpts.NumEnd).DefaultValue("1000");
    TOptsParseResult res(&options, argc, argv);

    TIndDocOutput output(inddocOpts);

    TFileInput input(res.Get<TString>("input"));

    for (TString line; input.ReadLine(line);) {
        if (line.empty() || line.length() <= 10 && StripString(line).empty())
            continue;

        bool finished = false;
        try {
            finished = !ProcessNext(line, output);
        } catch (...) {
            Cerr << "ERROR: " << CurrentExceptionMessage() << Endl;
        }

        if (finished)
            break;
    }

    return EXIT_SUCCESS;
}
