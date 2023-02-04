#include "pg_dump_output_normalizer.h"

#include <maps/libs/common/include/exception.h>
#include <boost/algorithm/string/replace.hpp>

namespace maps::goods {

PgDumpOutputNormalizer::PgDumpOutputNormalizer(const maps::local_postgres::Database& db)
    : dbname(db.dbname())
    , user(db.user())
{}

PgDumpOutputNormalizer::PgDumpOutputNormalizer(std::string dbname, std::string user)
    : dbname(std::move(dbname))
    , user(std::move(user))
{}

std::string PgDumpOutputNormalizer::normalize(std::istream& pgDumpOutput)
{
    for (std::string line; std::getline(pgDumpOutput, line);) {
        addLine(std::move(line));
    }
    return extractResult();
}

void PgDumpOutputNormalizer::addLine(std::string line)
{
    performBasicReplacements(line);

    if (isCreateTableBeginStatement(line)) {
        insideOfCreateTable = true;
    }

    if (insideOfCreateTable) {
        addCreateTableStatementLine(std::move(line));
    } else {
        addLineToResult(line);
    }
}

std::string PgDumpOutputNormalizer::extractResult()
{
    if (insideOfCreateTable) {
        dumpCreateTableBufferToResult();
    }
    return std::move(result);
}

void PgDumpOutputNormalizer::performBasicReplacements(std::string& line) const
{
    boost::replace_all(line, dbname, "<dbname>");
    boost::replace_all(line, user, "<user>");
}

bool PgDumpOutputNormalizer::isCreateTableBeginStatement(std::string_view line) const
{
    return line.rfind("CREATE TABLE ", 0) == 0;
}

bool PgDumpOutputNormalizer::isCreateTableEndStatement(std::string_view line) const
{
    return line.rfind(");", 0) == 0;
}

void PgDumpOutputNormalizer::addCreateTableStatementLine(std::string&& line)
{
    createTableLines.push_back(std::move(line));
    if (isCreateTableEndStatement(createTableLines.back())) {
        sortCreateTableColumns();
        dumpCreateTableBufferToResult();
    }
}

void PgDumpOutputNormalizer::fixCreateTableColumnsCommas()
{
    if (createTableLines.size() > 2) {
        REQUIRE(isCreateTableBeginStatement(createTableLines.front()), "Unexpected first line in CREATE TABLE");
        REQUIRE(isCreateTableEndStatement(createTableLines.back()), "Unexpected last line in CREATE TABLE");

        const auto firstColumnIt = createTableLines.begin() + 1;
        const auto lastColumnIt = createTableLines.end() - 2;
        for (auto it = firstColumnIt; it != lastColumnIt; it++) {
            if (!it->empty() && it->back() != ',') {
                it->push_back(',');
            }
        }
        if (!lastColumnIt->empty() && lastColumnIt->back() == ',') {
            lastColumnIt->pop_back();
        }
    }
}

void PgDumpOutputNormalizer::sortCreateTableColumns()
{
    if (createTableLines.size() > 2) {
        REQUIRE(isCreateTableBeginStatement(createTableLines.front()), "Unexpected first line in CREATE TABLE");
        REQUIRE(isCreateTableEndStatement(createTableLines.back()), "Unexpected last line in CREATE TABLE");

        const auto firstColumnIt = createTableLines.begin() + 1;
        const auto endStatementIt = createTableLines.end() - 1;
        std::sort(firstColumnIt, endStatementIt);

        fixCreateTableColumnsCommas();
    }
}

void PgDumpOutputNormalizer::dumpCreateTableBufferToResult()
{
    for (const auto& createTableLine : createTableLines) {
        addLineToResult(createTableLine);
    }
    createTableLines.clear();
    insideOfCreateTable = false;
}

void PgDumpOutputNormalizer::addLineToResult(const std::string& line)
{
    result += line;
    result += "\n";
}

} // namespace maps::goods
