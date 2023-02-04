#include <maps/factory/libs/storage/ftp_storage.h>
#include <maps/factory/libs/storage/local_storage.h>

#include <maps/libs/cmdline/include/cmdline.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/log8/include/log8.h>

#include <boost/filesystem.hpp>

using namespace maps;
using namespace factory::storage;

int main(int argc, char** argv) try
{
    cmdline::Parser parser(
        "Test FTP storage manually.\n"
        "Run 'chmod +x ./install.sh && ./install.sh;'\n"
        "Run tool 'ya make && ./manual-ftp test -u user:password';' where user:password are your current linux user and password.\n"
        "Run 'chmod +x ./cleanup.sh && ./cleanup.sh;'"
    );

    auto logLevelOpt = parser
        .string('l', "log-level")
        .help("Log level.");

    auto userOpt = parser
        .string('u', "user")
        .defaultValue("")
        .help("User or User:Password.");

    parser.parse(argc, argv);

    REQUIRE(!parser.argv().empty(), "Should set positional arguments.");

    const auto mode = parser.argv().front();

    if (logLevelOpt.defined()) {
        log8::setLevel(logLevelOpt);
    } else {
        log8::setLevel(log8::Level::INFO);
    }

    if (mode == "test") {
        INFO() << "Start testing scenario";
        const FtpStoragePtr ftpDir = ftpStorage("ftp://localhost/check_ftp_storage", userOpt);
        INFO() << "Directory " << ftpDir->absPath();
        {
            INFO() << "List files";
            for (const auto& path: ftpDir->list(Select::Files)) {
                INFO() << "  " << path;
            }
            INFO() << "List directories";
            for (const auto& path: ftpDir->list(Select::Directories)) {
                INFO() << "  " << path;
            }
            INFO() << "List files recursive";
            for (const auto& path: ftpDir->list(Select::FilesRecursive)) {
                INFO() << "  " << path;
            }
        }

        {
            const auto path = boost::filesystem::unique_path("tmp-1-%%%%-%%%%.txt");
            INFO() << "Create file " << path;
            const auto ftpFile = ftpDir->file(path);
            ASSERT(!ftpFile->exists());
            ftpFile->writeString({});
            ASSERT(ftpFile->exists());
            ASSERT(ftpFile->size() == 0);

            INFO() << "Overwrite";
            const std::string str = "test line 1\ntest line 2";
            ftpFile->writeString(str);
            ASSERT(ftpFile->exists());
            ASSERT(ftpFile->size() == Bytes(str.size()));

            INFO() << "Check list files";
            auto files = ftpDir->list(Select::Files);
            ASSERT(files.count(path) == 1);

            INFO() << "Read file";
            const std::string result = ftpFile->readToString();
            ASSERT(str == result);

            INFO() << "Remove file";
            ftpFile->remove();
            ASSERT(!ftpFile->exists());
        }

        {
            auto localDir = FilesystemStorage::makeTmp("manual-ftp-tests");
            auto srcFile = localDir->file(boost::filesystem::unique_path("tmp-src-%%%%-%%%%.txt"));
            const auto path = boost::filesystem::unique_path("tmp-2-%%%%-%%%%.txt");
            const auto ftpFile = ftpDir->file(path);
            INFO() << "Copy from file " << srcFile->absPath() << " to " << ftpFile->absPath();
            const std::string str = "test src file 1\ntest src file 2";
            srcFile->writeString(str);
            srcFile->copy(*ftpFile);
            ASSERT(ftpFile->exists());
            ASSERT(ftpFile->size() == Bytes(str.size()));
            ASSERT(ftpFile->readToString() == str);

            auto dstFile = localDir->file(boost::filesystem::unique_path("tmp-dst-%%%%-%%%%.txt"));
            INFO() << "Copy to file " << dstFile->absPath();
            ftpFile->copy(*dstFile);
            ASSERT(dstFile->readToString() == str);

            INFO() << "Remove";
            srcFile->remove();
            dstFile->remove();
            ftpFile->remove();
        }
    } else if (mode == "ls") {
        REQUIRE(parser.argv().size() == 2, "Should set path");
        const auto path = parser.argv().at(1);
        const FtpStoragePtr ftpDir = ftpStorage(path, userOpt);
        INFO() << "Listing " << ftpDir->absPath();
        const auto files = ftpDir->list(Select::Directories);
        for (const auto& file: files) {
            INFO() << file;
        }
    }

    INFO() << "Done";
} catch (const maps::Exception& ex) {
    FATAL() << ex;
    return 1;
} catch (const std::exception& ex) {
    FATAL() << ex.what();
    return 1;
}
