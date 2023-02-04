#include <maps/jams/renderer2/common/create_map/lib/util.h>

#include <yandex/maps/mms/declare_fields.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::jams::renderer::tests {

template <typename P>
struct DummyMms {
    int value;

    MMS_DECLARE_FIELDS(value);
};

Y_UNIT_TEST_SUITE(util_test)
{
    Y_UNIT_TEST(does_not_break_mmaped_file)
    {
        const std::string filename = "output.mms.1";

        constexpr int firstValue = 1;
        constexpr int secondValue = 2;

        DummyMms<mms::Standalone> firstWriter;
        firstWriter.value = firstValue;
        atomicWriteMms(filename, firstWriter);

        mms::Holder2<DummyMms<mms::Mmapped>> firstReader(filename);
        EXPECT_EQ(firstReader->value, firstValue);

        DummyMms<mms::Standalone> secondWriter;
        secondWriter.value = secondValue;
        atomicWriteMms(filename, secondWriter);
        mms::Holder2<DummyMms<mms::Mmapped>> secondReader(filename);

        EXPECT_EQ(firstReader->value, firstValue);
        EXPECT_EQ(secondReader->value, secondValue);
    }
}

} // namespace maps::jams::renderer::tests
