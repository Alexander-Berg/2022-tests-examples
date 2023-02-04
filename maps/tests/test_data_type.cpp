#include <maps/factory/libs/image/data_type.h>

#include <maps/factory/libs/common/functional.h>

#include <maps/factory/libs/unittest/tests_common.h>

namespace maps::factory::image::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(data_type_should) {

const TDataType du = TDataType::Unknown;
const TDataType db = TDataType::Byte;
const TDataType dui16 = TDataType::UInt16;
const TDataType di16 = TDataType::Int16;
const TDataType dui32 = TDataType::UInt32;
const TDataType di32 = TDataType::Int32;
const TDataType df32 = TDataType::Float32;
const TDataType df64 = TDataType::Float64;

Y_UNIT_TEST(check_type)
{
    const std::vector<TDataType> types{du, db, dui16, di16, dui32, di32, df32, df64};
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsFloat(); }),
        ElementsAre(false, false, false, false, false, false, true, true));
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsInt(); }),
        ElementsAre(false, true, true, true, true, true, false, false));
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsByte(); }),
        ElementsAre(false, true, false, false, false, false, false, false));
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsUnknown(); }),
        ElementsAre(true, false, false, false, false, false, false, false));
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsKnown(); }),
        ElementsAre(false, true, true, true, true, true, true, true));
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsCv(); }),
        ElementsAre(false, true, true, true, false, true, true, true));
    EXPECT_THAT(map(types, [](auto&& t) { return t.IsInt(); }),
        ElementsAre(false, true, true, true, true, true, false, false));
}

Y_UNIT_TEST(check_size)
{
    const std::vector<TDataType> types{db, dui16, di16, dui32, di32, df32, df64};
    EXPECT_THAT(map(types, [](auto&& t) { return t.SizeBytes(); }),
        ElementsAre(1u, 2u, 2u, 4u, 4u, 4u, 8u));
    EXPECT_THROW(Y_UNUSED(du.SizeBytes()), RuntimeError);
}

Y_UNIT_TEST(dispatch)
{
    const std::vector<TDataType> types{db, dui16, di16, di32, df32, df64};
    const auto dispatch = [&](TDataType type) {
        return type.DispatchCv([&](auto v) { return TDataType::From(v); });
    };
    EXPECT_THAT(map(types, dispatch), ElementsAreArray(types));
    EXPECT_THROW(Y_UNUSED(dispatch(du)), RuntimeError);
    EXPECT_THROW(Y_UNUSED(dispatch(dui32)), RuntimeError);
}

} // suite

} // namespace maps::factory::image::tests
