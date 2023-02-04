#include <yandex_io/libs/jwt/jwt.h>

#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <iostream>
#include <stdexcept>
#include <unordered_map>

using namespace quasar;

Y_UNIT_TEST_SUITE_F(TestJwt, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testUtilsJwtEncode)
    {
        /* Библиотека JWT перед конвертацией сортирует ключи в алфавитном порядке,
         * поэтому очень важно, чтобы в этом токене ключи тоже были отсортированы,
         * иначе actual с expected не сойдется.
         */
        const std::string expectedJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJnbGFnb2wiLCJleHAiOjYwLCJpc3MiOiJxdWFzYXItYmFja2VuZCIsInBsdCI6InBsYXRmb3JtIiwic3ViIjoiZGV2aWNlSWQifQ.3L56DRLPE0YGcRTG6pR6owPEl3wXMdNkF-ud0_oZWzs";

        std::unordered_map<std::string, JwtValue> keyValues({{"aud", "glagol"},
                                                             {"exp", 60},
                                                             {"iss", "quasar-backend"},
                                                             {"plt", "platform"},
                                                             {"sub", "deviceId"}});

        const std::string& jwtToken = encodeJWT(keyValues, JWT_ALG_HS256, "key");

        UNIT_ASSERT_VALUES_EQUAL(jwtToken, expectedJwtToken);

        auto token = decodeJWT(jwtToken, "key");
        UNIT_ASSERT_VALUES_EQUAL(getStringGrantFromJWT(token.get(), "aud"), "glagol");
        UNIT_ASSERT_VALUES_EQUAL(jwt_get_grant_int(token.get(), "exp"), 60);
        UNIT_ASSERT_VALUES_EQUAL(getStringGrantFromJWT(token.get(), "iss"), "quasar-backend");
        UNIT_ASSERT_VALUES_EQUAL(getStringGrantFromJWT(token.get(), "plt"), "platform");
        UNIT_ASSERT_VALUES_EQUAL(getStringGrantFromJWT(token.get(), "sub"), "deviceId");

        UNIT_ASSERT_EXCEPTION(getLongGrantFromJWT(token.get(), "..."), std::runtime_error);
    }
}
