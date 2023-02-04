#include "maps/b2bgeo/libs/jwt/include/key_providers/constant_key_providers.h"

#include "maps/b2bgeo/libs/jwt/include/jwt_issuer.h"
#include "maps/b2bgeo/libs/jwt/include/jwt_verifier.h"
#include "maps/b2bgeo/libs/jwt/include/payload_builders.h"

#include <library/cpp/testing/gtest/gtest.h>

#define PICOJSON_USE_INT64

namespace maps::b2bgeo::jwt {

namespace {

IssuerName testIssuerName()
{
    return IssuerName("test_issuer");
}

VerifierName testVerifierName()
{
    return VerifierName("test_verifier");
}

IssuerName wrongIssuerName()
{
    return IssuerName("wrong_issuer");
}

KeyId testKeyId()
{
    return KeyId("keyId");
}

JwtIssuer getTestJwtIssuer(const IssuerName& issuerName)
{
    const auto testPrivateKeyValue = PrivateKeyValue(
        "-----BEGIN EC PRIVATE KEY-----\n"
        "MHcCAQEEIG1r8A+PSeHyWz6ZgpG6cdN/6opleFKOFbxy21CfPjiboAoGCCqGSM49\n"
        "AwEHoUQDQgAE2KrZZEyknmfmntaTIM7aK5ARM+RYEuF5bVExJhYimCQ82QDcWXOL\n"
        "6JZcPozv45ikjmtUqCr3G8lVsJ/8AIRQ0w==\n"
        "-----END EC PRIVATE KEY-----");

    return JwtIssuer(
        issuerName,
        std::make_unique<ConstantPrivateKeyProvider>(
            PrivateKey(testPrivateKeyValue, testKeyId())));
}

JwtVerifier getTestJwtVerifier(const VerifierName& verifierName)
{
    const auto testPublicKey = PublicKeyValue(
        "-----BEGIN PUBLIC KEY-----\n"
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2KrZZEyknmfmntaTIM7aK5ARM+RY\n"
        "EuF5bVExJhYimCQ82QDcWXOL6JZcPozv45ikjmtUqCr3G8lVsJ/8AIRQ0w==\n"
        "-----END PUBLIC KEY-----");

    IssuerNamesToKeysMapping keysMapping{
        {testIssuerName(), {{testKeyId(), testPublicKey}}},
        {wrongIssuerName(), {{testKeyId(), testPublicKey}}}};

    return JwtVerifier(
        verifierName,
        std::make_unique<ConstantPublicKeysProvider>(std::move(keysMapping)));
}

struct TestApikeyPayload {
    static constexpr auto TYPE = "test_apikey_payload";

    static TestApikeyPayload fromPayloadJson(const picojson::object& payload)
    {
        return TestApikeyPayload(
            getField<int64_t>(payload, "uid"),
            getField<std::string>(payload, "login"));
    }

    TestApikeyPayload(int64_t uid, std::string login)
        : uid(uid), login(std::move(login))
    { }

    picojson::object serialize() const
    {
        picojson::object valueObj;
        valueObj["uid"] = picojson::value(uid);
        valueObj["login"] = picojson::value(login);

        picojson::object subject;
        subject["type"] = picojson::value(TestApikeyPayload::TYPE);
        subject["value"] = picojson::value(std::move(valueObj));

        picojson::object payloadObj;

        payloadObj["subject"] = picojson::value(subject);

        return payloadObj;
    }

    int64_t uid;
    std::string login;
};

std::string makeTestJwt(
    const JwtIssuer& issuer,
    const std::chrono::minutes& jwtLifeSpanMinutes = std::chrono::minutes(15))
{
    const auto payload = TestApikeyPayload(12345, "testLogin");
    const Audience audience = {testVerifierName()};
    return issuer.issue(payload, audience, jwtLifeSpanMinutes);
}

} // anonymous namespace

TEST(JwtManagerTest, TestSignAndCheckJwt)
{
    const auto issuer = getTestJwtIssuer(testIssuerName());
    const auto verifier = getTestJwtVerifier(testVerifierName());

    const auto signedJwt = makeTestJwt(issuer);
    const auto verifiedPayload = verifier.verify(signedJwt, testIssuerName());
    const auto payloadVariant =
        buildSubjectPayload<TestApikeyPayload>(verifiedPayload);

    EXPECT_TRUE(std::holds_alternative<TestApikeyPayload>(payloadVariant));
    const auto payload = std::get<TestApikeyPayload>(payloadVariant);

    EXPECT_EQ(payload.uid, 12345);
    EXPECT_EQ(payload.login, "testLogin");
}

TEST(JwtManagerTest, TestWrongIssuerJwt)
{
    const auto issuer = getTestJwtIssuer(testIssuerName());
    const auto verifier = getTestJwtVerifier(testVerifierName());

    const auto signedJwt = makeTestJwt(issuer);

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        verifier.verify(signedJwt, wrongIssuerName()),
        SignatureVerificationError,
        "claim iss does not match expected");
}

TEST(JwtManagerTest, TestWrongAudienceJwt)
{
    const auto issuer = getTestJwtIssuer(testIssuerName());
    const auto verifier = getTestJwtVerifier(VerifierName("wrong_verifier"));

    const auto signedJwt = makeTestJwt(issuer);

    EXPECT_THROW(verifier.verify(signedJwt, testIssuerName()), WrongAudienceError);
}

TEST(JwtManagerTest, TestExpiredJwt)
{
    const auto issuer = getTestJwtIssuer(testIssuerName());
    const auto verifier = getTestJwtVerifier(testVerifierName());

    const auto expiredToken = makeTestJwt(issuer, std::chrono::minutes(0));

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        verifier.verify(expiredToken, testIssuerName()),
        SignatureVerificationError,
        "token expired");
}

} // namespace maps::b2bgeo::jwt
