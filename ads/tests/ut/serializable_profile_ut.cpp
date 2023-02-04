#include <ads/bsyeti/big_rt/lib/serializable_profile/tests/profiles/test.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

using namespace NBigRT::NTests;

TEST(SerializableProfile, InitialState) {
    TTestProfile profile(1);

    EXPECT_EQ(1UL, profile.Get().GetProfileID());
    EXPECT_FALSE(profile.Get().HasStructuredColumn1());
    EXPECT_FALSE(profile.Get().HasSimpleColumn());
    EXPECT_FALSE(profile.Get().HasStructuredColumn2());
    EXPECT_FALSE(profile.Get().HasServiceFields());

    EXPECT_FALSE(profile.IsDirty());

    auto mutation = profile.Flush();

    EXPECT_EQ(3UL, std::tuple_size_v<decltype(mutation.Value)>);
    EXPECT_TRUE(mutation.Empty());
    EXPECT_FALSE(mutation.IsClearing);
}

TEST(SerializableProfile, SimpleModification) {
    TTestProfile profile(1);

    profile.Mutable()->SetSimpleColumn(1);

    EXPECT_TRUE(profile.IsDirty());

    auto mutation = profile.Flush();

    EXPECT_FALSE(mutation.Empty());
    EXPECT_FALSE(mutation.IsClearing);
    EXPECT_FALSE(std::get<0>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<0>(mutation.Value).Patch.has_value());
    EXPECT_EQ(1, std::get<1>(mutation.Value));
    EXPECT_FALSE(std::get<2>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Patch.has_value());
}

TEST(SerializableProfile, Clear) {
    TTestProfile profile(1);

    profile.Clear();

    EXPECT_TRUE(profile.IsDirty());

    auto mutation = profile.Flush();

    EXPECT_FALSE(mutation.Empty());
    EXPECT_TRUE(mutation.IsClearing);
    EXPECT_FALSE(std::get<0>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<0>(mutation.Value).Patch.has_value());
    EXPECT_FALSE(std::get<1>(mutation.Value).has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Patch.has_value());
}


TEST(SerializableProfile, EmbeddedMessageModification) {
    TTestProfile profile(1);

    profile.Mutable()->MutableStructuredColumn1()->SetStringValue("NewValue");

    auto mutation = profile.Flush();
    EXPECT_FALSE(mutation.Empty());
    EXPECT_FALSE(mutation.IsClearing);
    EXPECT_TRUE(std::get<0>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<0>(mutation.Value).Patch.has_value());
    EXPECT_FALSE(std::get<1>(mutation.Value).has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Patch.has_value());
}

TEST(SerializableProfile, MutableEqualsGet) {
    TTestProfile profile(1);

    profile.Mutable()->MutableStructuredColumn1()->SetStringValue("NewValue1");
    profile.Mutable()->MutableStructuredColumn1()->SetIntValue(1);
    profile.Mutable()->SetSimpleColumn(2);
    profile.Mutable()->MutableStructuredColumn2()->SetStringValue("NewValue2");
    profile.Mutable()->MutableStructuredColumn2()->SetIntValue(3);
    profile.Mutable()->MutableStructuredColumn2()->AddRepeatedStringValue("RepeatedValue");

    EXPECT_THAT(*profile.Mutable(), NGTest::EqualsProto(profile.Get()));
}


TEST(SerializableProfile, FlushDoesntChangeProfile) {
    TTestProfile profile(1);

    profile.Mutable()->MutableStructuredColumn1()->SetStringValue("NewValue1");
    profile.Mutable()->MutableStructuredColumn1()->SetIntValue(1);
    profile.Mutable()->SetSimpleColumn(2);
    profile.Mutable()->MutableStructuredColumn2()->SetStringValue("NewValue2");
    profile.Mutable()->MutableStructuredColumn2()->SetIntValue(3);
    profile.Mutable()->MutableStructuredColumn2()->AddRepeatedStringValue("RepeatedValue");

    auto originalProto = profile.Get();

    static_cast<void>(profile.Flush());

    EXPECT_THAT(profile.Get(), NGTest::EqualsProto(originalProto));
    EXPECT_THAT(*profile.Mutable(), NGTest::EqualsProto(originalProto));
}

TEST(SerializableProfile, MutableWithoutMutationDoesntAffectPatch) {
    TTestProfile profile(1);

    profile.Mutable()->MutableStructuredColumn1()->SetStringValue("NewValue1");
    profile.Mutable()->MutableStructuredColumn1()->SetIntValue(1);
    profile.Mutable()->SetSimpleColumn(2);
    profile.Mutable()->MutableStructuredColumn2()->SetStringValue("NewValue2");
    profile.Mutable()->MutableStructuredColumn2()->SetIntValue(3);
    profile.Mutable()->MutableStructuredColumn2()->AddRepeatedStringValue("RepeatedValue");

    auto originalProto = profile.Get();

    static_cast<void>(profile.Flush());

    profile.Mutable()->MutableStructuredColumn1();
    profile.Mutable()->SetSimpleColumn(2);
    profile.Mutable()->MutableStructuredColumn2()->SetStringValue("NewValue2");
    profile.Mutable()->MutableStructuredColumn2()->SetIntValue(3);

    auto mutation = profile.Flush();
    EXPECT_TRUE(mutation.Empty());
    EXPECT_FALSE(mutation.IsClearing);
    EXPECT_FALSE(std::get<0>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<0>(mutation.Value).Patch.has_value());
    EXPECT_FALSE(std::get<1>(mutation.Value).has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Base.has_value());
    EXPECT_FALSE(std::get<2>(mutation.Value).Patch.has_value());

    EXPECT_THAT(profile.Get(), NGTest::EqualsProto(originalProto));
    EXPECT_THAT(*profile.Mutable(), NGTest::EqualsProto(originalProto));
}
