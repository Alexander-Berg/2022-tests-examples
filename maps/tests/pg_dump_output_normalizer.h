#pragma once

#include <maps/libs/local_postgres/include/instance.h>

namespace maps::goods {

class PgDumpOutputNormalizer {
public:
    PgDumpOutputNormalizer(const maps::local_postgres::Database& db);
    PgDumpOutputNormalizer(std::string dbname, std::string user);

    std::string normalize(std::istream& pgDumpOutput);
    void addLine(std::string line);
    std::string extractResult();

private:
    void performBasicReplacements(std::string& line) const;

    bool isCreateTableBeginStatement(std::string_view line) const;
    bool isCreateTableEndStatement(std::string_view line) const;

    void addCreateTableStatementLine(std::string&& line);

    void fixCreateTableColumnsCommas();
    void sortCreateTableColumns();

    void dumpCreateTableBufferToResult();

    void addLineToResult(const std::string& line);

private:
    std::string dbname;
    std::string user;

    bool insideOfCreateTable = false;
    std::vector<std::string> createTableLines;

    std::string result;
};

} // namespace maps::goods
