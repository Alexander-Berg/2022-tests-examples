#pragma once

#include <agg_rendering_buffer.h>

#include <pqxx/transaction>

#include <string>

namespace maps { namespace tilerenderer4 { namespace test {

struct xyz
{
    unsigned int x;
    unsigned int y;
    unsigned int z;
};

namespace tools {

void bufferToFile(
    const char* buf, size_t sz, const std::string& fname);

agg::rendering_buffer loadPng(const std::string& fname);

bool isEqualBuffers(
    const agg::rendering_buffer& buffer1,
    const agg::rendering_buffer& buffer2);

int unzip_file(const char* infileName, const char* outfileName);

size_t countRowsFromTable(
    pqxx::transaction_base& transaction,
    const std::string& tableName);

} // namespace tools

} } } // namespace maps::tilerenderer4::test
