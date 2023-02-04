#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/common/namespace.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/libs/local_postgres/include/instance.h>
#include <maps/libs/log8/include/log8.h>

#include <filesystem>
#include <regex>
#include <unordered_set>

TEST(migrations, upgrade)
{
    maps::local_postgres::Database db;
    db.createExtension("hstore");
    db.applyMigrations(masi::appSourcePath("migrations"));
}

TEST(migrations, unique_versions)
{
    std::regex regex("V(\\d\\d\\d)__.*_upgrade.sql");
    std::unordered_set<int> versions;
    const auto path = masi::appSourcePath("migrations");
    for (const auto& entry: std::filesystem::directory_iterator(path)) {
        std::smatch match;
        std::string path = entry.path().filename().string(); 
        if (entry.is_regular_file() 
            && std::regex_match(path, match, regex)) 
        {
            auto [_, inserted] = versions.insert(std::stoi(match[1]));
            if (!inserted) {
                ERROR() << "Migration " << path << " has duplicate number";
            }
            ASSERT_TRUE(inserted);
        }
    }
}
