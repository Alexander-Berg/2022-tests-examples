#include <maps/libs/common/include/hex.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <cstdint>
#include <string>
#include <vector>

namespace maps::tests {

namespace {

constexpr const char* HEX_UPPER_PTR = "0BADC0DE";
constexpr const char* HEX_LOWER_PTR = "deadbeef";
const std::string HEX_UPPER_STRING(HEX_UPPER_PTR);
const std::string HEX_LOWER_STRING(HEX_LOWER_PTR);
const std::vector<uint8_t> HEX_UPPER_DECODED{11, 173, 192, 222};
const std::vector<uint8_t> HEX_LOWER_DECODED{222, 173, 190, 239};

} //namespace

TEST(test_hex_encode_and_decode, test_decoding)
{
    EXPECT_EQ(hexDecode(HEX_UPPER_PTR), HEX_UPPER_DECODED);
    EXPECT_EQ(hexDecode(HEX_UPPER_STRING), HEX_UPPER_DECODED);
    EXPECT_EQ(hexDecode(HEX_LOWER_PTR), HEX_LOWER_DECODED);
    EXPECT_EQ(hexDecode(HEX_LOWER_STRING), HEX_LOWER_DECODED);
}

TEST(test_hex_encode_and_decode, test_decoding_throws)
{
    const std::string SPACE_CHARS("\n\t\v ");
    const std::string PANGRAM("Jived fox nymph grabs quick waltz");

    EXPECT_THROW(hexDecode(SPACE_CHARS), Exception);
    EXPECT_THROW(hexDecode(PANGRAM), Exception);
}

TEST(test_hex_encode_and_decode, test_decoding_byte)
{
    EXPECT_EQ(hexDecodeByte('f', '9'), 0xF9);
    EXPECT_EQ(hexDecodeByte('2', 'E'), 0x2E);
    EXPECT_EQ(hexDecodeByte('0', '0'), 0x00);

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(hexDecodeByte('0', 'x'), maps::Exception, "0x");
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(hexDecodeByte('0', ' '), maps::Exception, "0 ");
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(hexDecodeByte('0', '\0'), maps::Exception, "0");
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(hexDecodeByte('0', '\255'), maps::Exception, "0\255");
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(hexDecodeByte('%', 'F'), maps::Exception, "%F");
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(hexDecodeByte('-', '+'), maps::Exception, "-+");
}

TEST(test_hex_encode_and_decode, test_encoding)
{
    EXPECT_EQ(
        hexEncodeUppercase(HEX_UPPER_DECODED),
        HEX_UPPER_STRING
    );

    EXPECT_EQ(
        hexEncodeLowercase(HEX_LOWER_DECODED),
        HEX_LOWER_STRING
    );
}

//encoding never throws if compiles (and has enough memory)

} //namespace maps::tests
