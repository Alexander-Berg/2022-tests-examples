#include <maps/mobile/server/init/lib/uniqueid.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <vector>
#include <algorithm>
#include <random>

using namespace maps::mobile::init;

namespace {

constexpr size_t TEST_SIZE = 100000;

template<typename Generator, typename Validator>
void generalTest(Generator& generator, Validator& validator)
{
    std::vector<std::string> ids(TEST_SIZE);
    std::generate(std::begin(ids), std::end(ids), [&generator](){ return generator(generateUniqueData()); });

    // Correctness
    for (auto& id : ids) {
        UNIT_ASSERT_EQUAL(validator(id), true);
    }

    // Uniqueness
    std::sort(std::begin(ids), std::end(ids));
    for (size_t i = 1; i < ids.size(); ++i) {
        UNIT_ASSERT_UNEQUAL(ids[i - 1], ids[i]);
    }

    UNIT_ASSERT_EQUAL(validator(""), false);
    UNIT_ASSERT_EQUAL(validator("a"), false);
    UNIT_ASSERT_EQUAL(validator("azxy`#+(oeu\000oeirthbosetd"), false);
    UNIT_ASSERT_EQUAL(validator("b14b09b4857269035941375180daaacab301d00d6"), false);
    UNIT_ASSERT_EQUAL(validator("b14b09b4857269035941*\0105180daaacab301d00d6"), false);
}

std::string randomId()
{
    static constexpr char validChars[] = "0123456789abcdef";
    static std::default_random_engine engine;
    static std::uniform_int_distribution<int> randDist(0, 15);

    std::string result("a");

    // 40 = 2 * MD5_DIGEST_LENGTH + 2 * MAC_LENGTH
    for (int i = 0; i < 40; ++i) {
        result += validChars[randDist(engine)];
    }

    return result;
}

template<typename Generator, typename Validator>
void securityTest(Generator& generator, Validator& validator)
{
    auto validSample = generator(generateUniqueData());
    auto randSample = randomId();
    UNIT_ASSERT_EQUAL(validSample[0], randSample[0]);
    UNIT_ASSERT_EQUAL(validSample.size(), randSample.size());

    for (unsigned i = 0; i < TEST_SIZE; ++i) {
        auto id = randomId();
        UNIT_ASSERT_EQUAL(validator(id), false);
    }
}

}

Y_UNIT_TEST_SUITE(uniqueid_uid_test)
{
    Y_UNIT_TEST(general)
    {
        generalTest(generateValidMiid, isValidMiid);
    }

    Y_UNIT_TEST(security)
    {
        securityTest(generateValidMiid, isValidMiid);
    }
}
