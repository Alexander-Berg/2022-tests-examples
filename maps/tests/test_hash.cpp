#include <maps/factory/libs/common/hash.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <unordered_map>
#include <atomic>

namespace maps::factory::tests {
namespace {
struct TestStruct {
    int a;
    std::string b;

    auto introspect() const { return std::tie(a, b); }
};

enum class TestEnum : int {
    First = 0,
    Second = 1,
};
} // namespace

Y_UNIT_TEST_SUITE(hash_should) {

Y_UNIT_TEST(empty_hash)
{
    const Hash128 hash{};
    EXPECT_EQ(hash.toString(), "AAAAAAAAAAAAAAAAAAAAAA==");
}

Y_UNIT_TEST(add_string)
{
    const Hash128 hash1{"test test test test test test test"};
    EXPECT_EQ(hash1.toString(), "WRYi_U1iXf35MoRuTiew9g==");
    Hash128 hash2{};
    const std::string str = "test test test test test test test";
    hash2.add(str);
    EXPECT_EQ(hash2.toString(), "WRYi_U1iXf35MoRuTiew9g==");
}

Y_UNIT_TEST(add_value)
{
    const Hash128 hash(42);
    EXPECT_EQ(hash.toString(), "xgcyI6OKvpPRaMn2Kvg1pQ==");
}

Y_UNIT_TEST(add_values)
{
    Hash128 hash{};
    hash.add(1);
    hash.add("test");
    EXPECT_EQ(hash.toString(), "28twVEV8LwZHqmSHCuipag==");
}

Y_UNIT_TEST(add_introsepctable)
{
    Hash128 hash{};
    TestStruct s{1, "test"};
    hash.add(s);
    EXPECT_EQ(hash.toString(), "28twVEV8LwZHqmSHCuipag==");
}

Y_UNIT_TEST(add_struct_fields)
{
    Hash128 hash{};
    TestStruct s{1, "test"};
    hash.add(s.a);
    hash.add(s.b);
    EXPECT_EQ(hash.toString(), "28twVEV8LwZHqmSHCuipag==");
}

Y_UNIT_TEST(add_vector)
{
    Hash128 hash{};
    hash.add(Eigen::Vector3d{1.2, 3.4, 5.6});
    Hash128 expected{};
    const double data[]{1.2, 3.4, 5.6};
    expected.add(data, sizeof(data));
    EXPECT_EQ(hash, expected);
    EXPECT_EQ(hash.toString(), expected.toString());
}

Y_UNIT_TEST(add_enum)
{
    Hash128 hash{};
    hash.add(TestEnum::First).add(TestEnum::Second);
    Hash128 expected{};
    expected.add(0).add(1);
    EXPECT_EQ(hash, expected);
    EXPECT_EQ(hash.toString(), expected.toString());
}

Y_UNIT_TEST(compare)
{
    const Hash128 hash1{"some string"};
    const Hash128 hash2{"some string"};
    const Hash128 hash3{"some other string"};
    EXPECT_EQ(hash1, hash2);
    EXPECT_NE(hash1, hash3);
    EXPECT_NE(hash2, hash3);
    EXPECT_EQ(hash1.toString(), hash2.toString());
    EXPECT_NE(hash1.toString(), hash3.toString());
    EXPECT_NE(hash2.toString(), hash3.toString());
}

Y_UNIT_TEST(use_in_unordered_map)
{
    const Hash128 hash1{"some string"};
    const Hash128 hash2{"some other string"};
    const Hash128 hash3{"some third string"};
    std::unordered_map<Hash128, int> map{
        {hash1, 1},
        {hash2, 2},
        {hash3, 3},
    };
    EXPECT_EQ(map[hash1], 1);
    EXPECT_EQ(map[hash2], 2);
    EXPECT_EQ(map[hash3], 3);
}

Y_UNIT_TEST(use_in_atomic)
{
    std::atomic<Hash128> hash{};
    EXPECT_EQ(hash.load(), Hash128(0, 0));
    hash.store(Hash128(42));
    EXPECT_EQ(hash.load(), Hash128(42));
}

Y_UNIT_TEST(next)
{
    Hash128 hash{};
    Hash128 hash2 = hash.next();
    Hash128 hash3 = hash2.next();
    EXPECT_NE(hash, hash2);
    EXPECT_NE(hash2, hash3);
    EXPECT_NE(hash3, hash);
}

} // suite

} // namespace maps::factory::tests
