#pragma once

#include <yandex/maps/wiki/revision/common.h>
#include <maps/wikimap/mapspro/libs/revision/sql_strings.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/local_postgres/include/instance.h>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

const UserID TEST_UID = 131; // Hello, Bacek ;)

inline unsigned char
fromHex(char c) {
    if (c >= 'A' && c <= 'F') {
        return c - 'A' + 10;
    }
    if (c >= 'a' && c <= 'f') {
        return c - 'a' + 10;
    }
    ASSERT(c >= '0' && c <= '9');
    return c - '0';
}

inline std::string
decodeWKB(const std::string& str) {
    ASSERT(!(str.size() & 1));
    ASSERT(str.size() >= 16);

    std::string result;
    const char *s = str.c_str();
    for (; *s; s += 2) {
        unsigned char value = (fromHex(*s) << 4) + fromHex(s[1]);
        result.push_back(value);
    }
    return result;
}

class DbFixture
{
public:
    DbFixture();

    virtual ~DbFixture() = default;

    void createSchema();

    std::string connectionString();

    pqxx::connection& getConnection();

    void setTestData(const std::string& sqlPath);
    void setTestData();

private:
    local_postgres::Database instance_;
    std::unique_ptr<pqxx::connection> conn_;
};

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
