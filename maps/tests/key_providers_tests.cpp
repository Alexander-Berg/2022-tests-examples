#include "maps/b2bgeo/libs/jwt/include/key_providers/constant_key_providers.h"
#include "maps/b2bgeo/libs/jwt/include/key_providers/env_key_providers.h"

#include "maps/b2bgeo/libs/jwt/include/errors.h"

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::b2bgeo::jwt {

TEST(ConstantPrivateKeyGetterTest, TestGetKey)
{
    const auto provider = ConstantPrivateKeyProvider(
        PrivateKey(PrivateKeyValue("test_private_key"), KeyId("keyId")));
    const auto& privateKey = provider.getPrivateKey();
    EXPECT_EQ(privateKey.value.get(), "test_private_key");
    EXPECT_EQ(privateKey.id.get(), "keyId");
}

TEST(ConstantPublicKeysGetterTest, TestGetKeys)
{
    const IssuerNamesToKeysMapping keysMapping{
        {IssuerName("issuer1"),
         {{KeyId("keyId"), PublicKeyValue("test_public_key1")}}},
        {IssuerName("issuer2"),
         {{KeyId("keyId"), PublicKeyValue("test_public_key2")}}}};
    const auto provider = ConstantPublicKeysProvider(keysMapping);

    EXPECT_EQ(
        provider.getPublicKeyForIssuer(IssuerName("issuer1"), KeyId("keyId"))
            .get(),
        "test_public_key1");
    EXPECT_EQ(
        provider.getPublicKeyForIssuer(IssuerName("issuer2"), KeyId("keyId"))
            .get(),
        "test_public_key2");

    EXPECT_THROW(
        provider.getPublicKeyForIssuer(
            IssuerName("issuer2"), KeyId("unexistent_key_id")),
        KeyNotFoundError);
    EXPECT_THROW(
        provider.getPublicKeyForIssuer(
            IssuerName("unexistent_issuer"), KeyId("keyId")),
        KeyNotFoundError);
}

TEST(EnvPrivateKeyGetterTest, TestGetKey)
{
    const auto provider =
        EnvPrivateKeyProvider("PRIVATE_KEY", "PRIVATE_KEY_ID");
    const auto& privateKey = provider.getPrivateKey();
    EXPECT_EQ(privateKey.value.get(), "env_private_key");
    EXPECT_EQ(privateKey.id.get(), "env_private_key_id");
}

TEST(EnvPublicKeysGetterTest, TestGetKeys)
{
    const IssuerNamesToEnvVarNameMapping envVarMapping{
        {IssuerName("issuer1"), "PUBLIC_KEY_MAP"}};
    const auto provider = EnvPublicKeysProvider(envVarMapping);
    EXPECT_EQ(
        provider.getPublicKeyForIssuer(IssuerName("issuer1"), KeyId("keyId"))
            .get(),
        "env_public_key");
}

TEST(EnvPrivateKeyGetterTest, TestUnexistentEnvVar)
{
    EXPECT_THROW(
        EnvPrivateKeyProvider("UNEXISTENT_ENV_VAR", "PRIVATE_KEY_ID"),
        EnvVarNotFoundError);
    EXPECT_THROW(
        EnvPrivateKeyProvider("PRIVATE_KEY", "UNEXISTENT_ENV_VAR"),
        EnvVarNotFoundError);
}

TEST(EnvPublicKeysGetterTest, TestUnexistentEnvVar)
{
    const IssuerNamesToEnvVarNameMapping envVarMapping{
        {IssuerName("issuer1"), "UNEXISTENT_ENV_VAR"}};
    EXPECT_THROW((EnvPublicKeysProvider(envVarMapping)), EnvVarNotFoundError);
}

} // namespace maps::b2bgeo::jwt
