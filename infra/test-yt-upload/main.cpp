#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/library/skynet_table/skynet_table_writer.h>

#include <library/cpp/getopt/last_getopt.h>

#include <util/datetime/base.h>
#include <util/string/builder.h>
#include <util/stream/format.h>

#define KB (ui64)1024
#define MB (1024 * KB)
#define GB (1024 * MB)

#define BLOCK_SIZE (1 * MB)

void DoYtUpload(const TString& ytProxy, const TString& ytPath, size_t numFiles, size_t fileSize) {
    auto client = NYT::CreateClient(ytProxy);
    auto skynetWriter = NYtSkynetTable::CreateSkynetWriter(client, ytPath);

    size_t pieceIdx = 0;
    const size_t fileBlocks = fileSize / BLOCK_SIZE;
    unsigned char buffer[BLOCK_SIZE];
    std::fill_n(buffer, sizeof(buffer), 0xde);

    // add timestamp to make the new resource unique
    auto now = TInstant::Now().ToRfc822StringLocal();
    std::copy(std::cbegin(now), std::cend(now), std::begin(buffer) + sizeof(pieceIdx));

    NYT::IFileWriterPtr writer;
    for (size_t i = 0; i < numFiles; ++i) {
        auto writer = skynetWriter->AppendFile(TStringBuilder() << "test" << i);
        for (size_t j = 0; j < fileBlocks; ++j) {
            std::memcpy(buffer, &pieceIdx, sizeof(pieceIdx));
            writer->Write(buffer, sizeof(buffer));
            ++pieceIdx;
        }
        writer->Finish();
    }
}

int main(int argc, char* argv[])
{
    try {
        using namespace NLastGetopt;

        // option names beginning with "--yt" are reserved:
        // https://yt.yandex-team.ru/docs/api/c++/c++_api#init
        NYT::Initialize(argc, argv);

        TOpts opts = TOpts::Default();
        opts.AddHelpOption('h');
        opts.AddVersionOption('v');

        TOpt& optYtProxy = opts.AddLongOption("proxy", "YT cluster to use.")
            .RequiredArgument();
        TOpt& optYtPath = opts.AddLongOption("path", "Path to blob table in YT.")
            .RequiredArgument();
        TOpt& optNumFiles = opts.AddLongOption('n', "file-num", "Number of files in resource.")
            .Optional().RequiredArgument().DefaultValue(1);
        TOpt& optFileSize = opts.AddLongOption("file-size", "Size of each file in MB.")
            .RequiredArgument();
        TOptsParseResult res(&opts, argc, argv);

        size_t numFiles = FromString<size_t>(res.Get(&optNumFiles));
        size_t fileSize = MB * FromString<size_t>(res.Get(&optFileSize));

        DoYtUpload(res.Get(&optYtProxy), res.Get(&optYtPath), numFiles, fileSize);
    } catch (...) {
        Cerr << CurrentExceptionMessage() << Endl;
        return 1;
    }

    return 0;
}
