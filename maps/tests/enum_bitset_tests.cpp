#include <maps/libs/common/include/enum_bitset.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::common::tests {

enum class Flag: unsigned {
    Flag1,
    Flag2,
    Flag3,
    EnumBitsetFlagsEnd
};
using Flags = EnumBitset<Flag>;

TEST(enum_bitset, shouldDefaultInitialize)
{
    Flags flags;
    EXPECT_FALSE(flags[Flag::Flag1]);
    EXPECT_FALSE(flags[Flag::Flag2]);
    EXPECT_FALSE(flags[Flag::Flag3]);
}

TEST(enum_bitset, shouldInitalizeFromValue)
{
    Flags flags(Flag::Flag2);
    EXPECT_FALSE(flags[Flag::Flag1]);
    EXPECT_TRUE(flags[Flag::Flag2]);
    EXPECT_FALSE(flags[Flag::Flag3]);
}

TEST(enum_bitset, shouldInitalizeFromInitializerListValues)
{
    Flags flags({Flag::Flag1, Flag::Flag3});
    EXPECT_TRUE(flags[Flag::Flag1]);
    EXPECT_FALSE(flags[Flag::Flag2]);
    EXPECT_TRUE(flags[Flag::Flag3]);
}

TEST(enum_bitset, shouldSet)
{
    {
        Flags flags;

        flags.set();
        EXPECT_TRUE(flags[Flag::Flag1]);
        EXPECT_TRUE(flags[Flag::Flag2]);
        EXPECT_TRUE(flags[Flag::Flag3]);
    }

    {
        Flags flags;

        flags.set(Flag::Flag2);
        EXPECT_FALSE(flags[Flag::Flag1]);
        EXPECT_TRUE(flags[Flag::Flag2]);
        EXPECT_FALSE(flags[Flag::Flag3]);
    }

    {
        Flags flags;

        auto flagRef = flags[Flag::Flag1];
        flagRef = true;
        EXPECT_TRUE(flags.isSet(Flag::Flag1));
    }
}

TEST(enum_bitset, shouldReset)
{
    {
        Flags flags;
        flags.set();

        flags.reset();
        EXPECT_FALSE(flags[Flag::Flag1]);
        EXPECT_FALSE(flags[Flag::Flag2]);
        EXPECT_FALSE(flags[Flag::Flag3]);
    }

    {
        Flags flags;
        flags.set();

        flags.reset(Flag::Flag2);
        EXPECT_TRUE(flags[Flag::Flag1]);
        EXPECT_FALSE(flags[Flag::Flag2]);
        EXPECT_TRUE(flags[Flag::Flag3]);
    }
}

TEST(enum_bitset, shouldTestFlagsDifferentWays)
{
    Flags flags(Flag::Flag2);

    {
        Flags::Reference flagRef = flags[Flag::Flag1];
        EXPECT_FALSE(flags[Flag::Flag1]);
        EXPECT_FALSE(flags.isSet(Flag::Flag1));
        EXPECT_FALSE(flagRef);
    }

    {
        Flags::Reference flagRef = flags[Flag::Flag2];
        EXPECT_TRUE(flags[Flag::Flag2]);
        EXPECT_TRUE(flags.isSet(Flag::Flag2));
        EXPECT_TRUE(flagRef);
    }
}

TEST(enum_bitset, shouldFlip)
{
    {
        Flags flags;
        flags.set(Flag::Flag2);

        flags.flip();
        EXPECT_TRUE(flags[Flag::Flag1]);
        EXPECT_FALSE(flags[Flag::Flag2]);
        EXPECT_TRUE(flags[Flag::Flag3]);
    }

    {
        Flags flags;

        flags.flip(Flag::Flag2);
        EXPECT_FALSE(flags[Flag::Flag1]);
        EXPECT_TRUE(flags[Flag::Flag2]);
        EXPECT_FALSE(flags[Flag::Flag3]);
    }
}

TEST(enum_bitset, shouldSetByReference)
{
    Flags flags;

    flags[Flag::Flag2] = true;
    EXPECT_FALSE(flags[Flag::Flag1]);
    EXPECT_TRUE(flags[Flag::Flag2]);
    EXPECT_FALSE(flags[Flag::Flag3]);
}

TEST(enum_bitset, shouldCheckPredicates)
{
    Flags flags;

    EXPECT_TRUE(flags.none());
    EXPECT_FALSE(flags.any());
    EXPECT_FALSE(flags.all());

    flags.set(Flag::Flag2);
    EXPECT_FALSE(flags.none());
    EXPECT_TRUE(flags.any());
    EXPECT_FALSE(flags.all());

    flags.set();
    EXPECT_FALSE(flags.none());
    EXPECT_TRUE(flags.any());
    EXPECT_TRUE(flags.all());
}

TEST(enum_bitset, shouldCount)
{
    Flags flags;

    EXPECT_EQ(flags.count(), 0u);

    flags.set(Flag::Flag2);
    EXPECT_EQ(flags.count(), 1u);

    flags.set();
    EXPECT_EQ(flags.count(), 3u);
}

TEST(enum_bitset, shouldCompare)
{
    {   // (EnumBitset, EnumBitset)
        EXPECT_TRUE(Flags({Flag::Flag1, Flag::Flag3}) == Flags({Flag::Flag1, Flag::Flag3}));
        EXPECT_FALSE(Flags({Flag::Flag1, Flag::Flag3}) != Flags({Flag::Flag1, Flag::Flag3}));
        EXPECT_FALSE(Flags({Flag::Flag1, Flag::Flag3}) == Flags(Flag::Flag1));
        EXPECT_TRUE(Flags({Flag::Flag1, Flag::Flag3}) != Flags(Flag::Flag1));
    }

    {   // (Enum, EnumBitset)
        EXPECT_TRUE(Flag::Flag2 == Flags(Flag::Flag2));
        EXPECT_FALSE(Flag::Flag2 != Flags(Flag::Flag2));
        EXPECT_FALSE(Flag::Flag2 == Flags(Flag::Flag1));
        EXPECT_TRUE(Flag::Flag2 != Flags(Flag::Flag1));
    }

    {   // (EnumBitset, Enum)
        EXPECT_TRUE(Flags(Flag::Flag2) == Flag::Flag2);
        EXPECT_FALSE(Flags(Flag::Flag2) != Flag::Flag2);
        EXPECT_FALSE(Flags(Flag::Flag1) == Flag::Flag2);
        EXPECT_TRUE(Flags(Flag::Flag1) != Flag::Flag2);
    }
}

TEST(enum_bitset, shouldMakeLogicalOperations)
{
    {   // (EnumBitset, EnumBitset)
        EXPECT_TRUE(~Flags(Flag::Flag2) == Flags({Flag::Flag1, Flag::Flag3}));
        EXPECT_TRUE((Flags(Flag::Flag1) & Flags({Flag::Flag1, Flag::Flag3})) == Flags(Flag::Flag1));
        EXPECT_TRUE((Flags(Flag::Flag1) | Flags({Flag::Flag1, Flag::Flag3})) == Flags({Flag::Flag1, Flag::Flag3}));
        EXPECT_TRUE((Flags(Flag::Flag1) ^ Flags({Flag::Flag1, Flag::Flag2})) == Flags(Flag::Flag2));
    }

    {   // (Enum, EnumBitset)
        EXPECT_TRUE((Flag::Flag1 & Flags({Flag::Flag1, Flag::Flag3})) == Flags(Flag::Flag1));
        EXPECT_TRUE((Flag::Flag1 | Flags({Flag::Flag1, Flag::Flag3})) == Flags({Flag::Flag1, Flag::Flag3}));
        EXPECT_TRUE((Flag::Flag1 ^ Flags({Flag::Flag1, Flag::Flag2})) == Flags(Flag::Flag2));
    }

    {   // (EnumBitset, Enum)
        EXPECT_TRUE((Flags({Flag::Flag1, Flag::Flag3}) & Flag::Flag1) == Flags(Flag::Flag1));
        EXPECT_TRUE((Flags({Flag::Flag1, Flag::Flag3}) | Flag::Flag1) == Flags({Flag::Flag1, Flag::Flag3}));
        EXPECT_TRUE((Flags({Flag::Flag1, Flag::Flag2}) ^ Flag::Flag1) == Flags(Flag::Flag2));
    }

    {
        Flags flags(Flag::Flag2);
        flags &= Flags({Flag::Flag2, Flag::Flag3});
        EXPECT_TRUE(flags == Flags(Flag::Flag2));
    }

    {
        Flags flags(Flag::Flag2);
        flags |= Flags({Flag::Flag2, Flag::Flag3});
        EXPECT_TRUE(flags == Flags({Flag::Flag2, Flag::Flag3}));
    }

    {
        Flags flags(Flag::Flag2);
        flags ^= Flags({Flag::Flag2, Flag::Flag3});
        EXPECT_TRUE(flags == Flags(Flag::Flag3));
    }
}

} // namespace maps::common::tests
