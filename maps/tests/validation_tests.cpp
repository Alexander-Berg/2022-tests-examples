#include <maps/wikimap/mapspro/libs/flat_range/include/validation.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::flat_range::tests {

Y_UNIT_TEST_SUITE(validation_tests)
{

Y_UNIT_TEST(flat_range)
{
    UNIT_ASSERT(validate({{"1-20", ""}}).empty());
    UNIT_ASSERT(validate({{"21", ""}}).empty());
    UNIT_ASSERT(validate({{"А", ""}}).empty());
    UNIT_ASSERT(validate({{"1БВГ", ""}}).empty());
    UNIT_ASSERT(validate({{"23G", ""}}).empty());
    UNIT_ASSERT(validate({{"А1-А3", ""}}).empty());
    UNIT_ASSERT(validate({{"1.1-1.10", ""}}).empty());
    UNIT_ASSERT(validate({{"1.1-10.1", ""}}).empty());
    UNIT_ASSERT(validate({{"2000-2100", ""}}).empty());

    UNIT_ASSERT(validate({{"а", ""}}).contains(ValidationResult::BadFlatRange));
    UNIT_ASSERT(validate({{"1-А", ""}}).contains(ValidationResult::BadFlatRange));
    UNIT_ASSERT(validate({{"2Б-2В", ""}}).contains(ValidationResult::BadFlatRange));
    UNIT_ASSERT(validate({{"15-15А", ""}}).contains(ValidationResult::BadFlatRange));
    UNIT_ASSERT(validate({{"1-3000", ""}}).contains(ValidationResult::TooManyFlatsPerEntrance));

    UNIT_ASSERT(validate({{"0-10", ""}}).contains(ValidationResult::FlatsOutOfRange));
    UNIT_ASSERT(validate({{"1-10000", ""}}).contains(ValidationResult::FlatsOutOfRange));
    UNIT_ASSERT(validate({{"36-35", ""}}).contains(ValidationResult::FlatsReverseOrder));
}

Y_UNIT_TEST(level_range)
{
    UNIT_ASSERT(validate({{"1-10", "1-5"}}).empty());
    UNIT_ASSERT(validate({{"1-10", "4"}}).empty());
    UNIT_ASSERT(validate({{"1-10", "А1"}}).empty());
    UNIT_ASSERT(validate({{"1-10", "А1-А2"}}).empty());
    UNIT_ASSERT(validate({{"1-10", "1.1-1.5"}}).empty());
    UNIT_ASSERT(validate({{"1-10", "1.1-5.1"}}).empty());

    UNIT_ASSERT(validate({{"1-10", "g"}}).contains(ValidationResult::BadLevelRange));
    UNIT_ASSERT(validate({{"1-10", "1-А"}}).contains(ValidationResult::BadLevelRange));
    UNIT_ASSERT(validate({{"1-10", "2Б-2В"}}).contains(ValidationResult::BadLevelRange));
    UNIT_ASSERT(validate({{"1-10", "3-3А"}}).contains(ValidationResult::BadLevelRange));

    UNIT_ASSERT(validate({{"1-10", "-1-8"}}).contains(ValidationResult::LevelsOutOfRange));
    UNIT_ASSERT(validate({{"1-10", "1-1000"}}).contains(ValidationResult::LevelsOutOfRange));
    UNIT_ASSERT(validate({{"1-10", "6-2"}}).contains(ValidationResult::LevelsReverseOrder));
}

} // Y_UNIT_TEST_SUITE(validation_tests)

} // namespace maps::wiki::flat_range::tests
