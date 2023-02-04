#include <maps/goods/lib/goods_db/schema/tests/pg_dump_output_normalizer.h>

#include <library/cpp/testing/gtest/gtest.h>

using std::string_literals::operator "" s;

TEST(pg_dump_output_normalizer, normalize_create_table)
{
    maps::goods::PgDumpOutputNormalizer normalizer("test_database"s, "test_user"s);

    constexpr const char* inputQuery = R"(
CREATE TABLE IF NOT EXISTS abc (
    c VARCHAR NOT NULL UNIQUE,
    b IMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    a BIGSERIAL PRIMARY KEY
);
)";
    auto stream = std::stringstream(inputQuery);
    const auto result = normalizer.normalize(stream);

    const auto expectedResult = R"(
CREATE TABLE IF NOT EXISTS abc (
    a BIGSERIAL PRIMARY KEY,
    b IMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    c VARCHAR NOT NULL UNIQUE
);
)"s;
    ASSERT_EQ(result, expectedResult);
}
