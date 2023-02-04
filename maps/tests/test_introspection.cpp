#include <maps/factory/libs/common/introspection.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::tests {

Y_UNIT_TEST_SUITE(introspection_should) {

struct Obj : Comparable<Obj>, Hashable<Obj>, Printable<Obj> {
    Obj() = default;

    Obj(int a, const std::string& b)
        : a(a)
        , b(b) {}

    int a{};
    std::string b{};

    auto introspect() const noexcept { return std::tie(a, b); }
};

Y_UNIT_TEST(compare)
{
    Obj o1{1, "b"};
    Obj o2{1, "a"};
    Obj o3{1, "b"};
    Obj o4{3, "a"};
    EXPECT_EQ(o1, o1);
    EXPECT_EQ(o1, o3);
    EXPECT_NE(o1, o2);
    EXPECT_NE(o1, o4);
    EXPECT_GT(o4, o1);
    EXPECT_LT(o2, o1);
}

Y_UNIT_TEST(hash)
{
    const THashSet<Obj> set{{1, "b"}, {1, "a"}, {1, "b"}, {3, "a"},};
    EXPECT_THAT(set, UnorderedElementsAre(Obj(1, "b"), Obj(1, "a"), Obj(3, "a")));
}

Y_UNIT_TEST(print)
{
    std::ostringstream ss;
    ss << Obj(1, "b") << "; " << Obj(1, "a") << "; " << Obj(3, "a");
    EXPECT_EQ(ss.str(), "(1, b); (1, a); (3, a)");
}

} // suite

} // namespace maps::factory::tests
