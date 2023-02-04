#include "../include/tools.h"

#include <yandex/maps/renderer5/postgres/IPostgresTransactionProvider.h>
#include <yandex/maps/renderer5/styles/Pattern.h>
#include <maps/renderer/libs/image/include/image_storage.h>
#include <yandex/maps/renderer/io/resource.h>

#include <pqxx/pqxx>

#include <zlib.h>

#include <fstream>
#include <iostream>

using namespace maps::renderer;
using namespace maps::renderer5;
using namespace maps::renderer5::postgres;
using namespace maps::tilerenderer4;
using namespace maps::tilerenderer4::test;

void tools::bufferToFile(
    const char* buf, size_t sz, const std::string& fname)
{
    std::ofstream outfile(fname.c_str(), std::ofstream::binary);
    outfile.write(buf, sz);
    outfile.close();
}

agg::rendering_buffer tools::loadPng(const std::string& fileName)
{
    static image::ImageStorage storage;
    auto img = storage.getOrAddImage(
        fileName, [&] { return io::readResource(fileName); });
    return image::asRenBuffer(*img);
}

bool tools::isEqualBuffers(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2)
{
    return
        buffer1.width() == buffer2.width() &&
        buffer1.height() == buffer2.height() &&
        (memcmp(buffer1.buf(), buffer2.buf(), buffer1.width() * buffer1.height() * 4) == 0);
}

int tools::unzip_file(const char* infileName, const char* outfileName)
{
    gzFile infile = gzopen(infileName, "rb");
    FILE* outfile = fopen(outfileName, "wb");
    if (!infile || !outfile) return -1;

    char buffer[1024];
    int num_read = 0;
    while ((num_read = gzread(infile, buffer, sizeof(buffer))) > 0)
    {
       fwrite(buffer, 1, num_read, outfile);
    }

    gzclose(infile);
    fclose(outfile);
    return 0;
}

size_t tools::countRowsFromTable(
    pqxx::transaction_base& transaction,
    const std::string& tableName)
{
    std::string query = "SELECT COUNT(*) FROM " + tableName;
    pqxx::result result = transaction.exec(query);
    return std::atoll(result[0][0].c_str());
}
