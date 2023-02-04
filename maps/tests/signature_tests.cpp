#include <maps/infra/apiteka/signature/lib/include/signature.h>

#include <maps/libs/http/include/url.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <memory>

namespace maps::apiteka::signature::tests {

namespace {

const yacare::Request& makeRequest(const http::URL& url)
{
    static std::unique_ptr<yacare::RequestBuilder> b;
    b = std::make_unique<yacare::RequestBuilder>();
    b->putenv("HTTP_HOST", url.host());
    b->putenv("PATH_INFO", url.path());
    if (!url.params().empty())
        b->putenv("QUERY_STRING", url.params());
    b->readBody([](char*, size_t) { return false; });
    return b->request();
}

} // namespace

TEST(validate_request, valid_signature)
{
    EXPECT_NO_THROW(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&api_key=123&signature=B6KAPRHcGLeE6mH5Pf0bl0kjLnbalUPyWRkl-D_sVLo="),
            "test"));

    EXPECT_NO_THROW(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&api_key=123&signature=B6KAPRHcGLeE6mH5Pf0bl0kjLnbalUPyWRkl-D_sVLo="),
            "test",
            AllowUnsigned::Yes));

    EXPECT_NO_THROW(
        validateRequest(makeRequest(
            "https://example.com/api?action=info%2Cxml&api_key=123&signature=1Q9AqkbBqTDaw0_3NOHOQ6HZpsVH-XcuEjZiPhs9YJw="),
            "test"));

    // base64 standard
    EXPECT_NO_THROW(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&api_key=123&signature=B6KAPRHcGLeE6mH5Pf0bl0kjLnbalUPyWRkl%2BD/sVLo="),
            "test"));

    EXPECT_NO_THROW(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&api_key=123&signature=7jPMCdE6QUWIY5GfWuN_GQ97j7MPcncyfdrkE6ypI2w="),
            ""));
}

TEST(validate_request, missing_signature)
{
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        validateRequest(makeRequest("https://example.com/api"), ""),
        yacare::errors::Forbidden,
        "signature parameter required");

    EXPECT_NO_THROW(
        validateRequest(makeRequest("https://example.com/api"), "", AllowUnsigned::Yes));
}

TEST(validate_request, invalid_signature)
{
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&api_key=123&signature=7jPMCdE6QUWIY5GfWuN_GQ97j7MPcncyfdrkE6ypI2w="),
            "test",
            AllowUnsigned::Yes),
        yacare::errors::Forbidden,
        "invalid signature");

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&signature=B6KAPRHcGLeE6mH5Pf0bl0kjLnbalUPyWRkl-D_sVLo=&api_key=123"),
            "test"),
        yacare::errors::BadRequest,
        "signature must be the last parameter");

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        validateRequest(makeRequest(
            "https://example.com/api?action=info,xml&api_key=123&signature=&signature=invalid"),
            "test",
            AllowUnsigned::Yes),
        yacare::errors::BadRequest,
        "signature must be the last parameter");
}

TEST(validate_url_signature, valid_signature)
{
    static constexpr std::string_view url{
        "/api?action=info,xml&api_key=123&signature=B6KAPRHcGLeE6mH5Pf0bl0kjLnbalUPyWRkl-D_sVLo="};
    EXPECT_NO_THROW(validateUrl(url, "test"));
    EXPECT_THROW(validateUrl(url, ""), yacare::errors::Forbidden);
}

TEST(validate_url_signature, url_with_encoded_padding)
{
    static constexpr std::string_view urlWithPercentEncodedPadding{
        "/api?action=info&api_key=123&signature=lNzxsug8AALLa_CegqEBzN7cuAYa5DU8US6Gr5njWf4%3D"};
    static constexpr std::string_view secret{"topsecret"};
    EXPECT_NO_THROW(validateUrl(urlWithPercentEncodedPadding, secret));
}

TEST(validate_url_signature, url_without_padding)
{
    static constexpr std::string_view urlWithoutPadding{
        "/api?action=info&api_key=123&signature=lNzxsug8AALLa_CegqEBzN7cuAYa5DU8US6Gr5njWf4"};
    static constexpr std::string_view secret{"topsecret"};
    EXPECT_NO_THROW(validateUrl(urlWithoutPadding, secret));
}

TEST(validate_url_signature, value_as_signature_confusion)
{
    static constexpr std::string_view urlDupSignature{
        "/api?action=info&search_test=signature=&api_key=123&signature=0fZH8_XrAC3JxzFZpmK3mHLTc_WUWHuFRmxOkiPMGG0="};
    static constexpr std::string_view secret{"topsecret"};
    EXPECT_NO_THROW(validateUrl(urlDupSignature, secret));
}

TEST(validate_url_signature, url_with_misleading_params)
{
    static constexpr std::string_view urlMisleadingParams{
        "/api?action=info&add_signature=somesig&api_key=123&signature=ZtPZBzoUk5xup0tCNDscgn-Yp_ts6pMefZFrB-jNeFk="};
    static constexpr std::string_view secret{"topsecret"};
    EXPECT_NO_THROW(validateUrl(urlMisleadingParams, secret));
}

TEST(validate_url_signature, empty_signature_value)
{
    static constexpr std::string_view urlMisleadingParams{
        "/api?action=info&api_key=123&signature="};
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        validateUrl(urlMisleadingParams, {}),
        yacare::errors::Forbidden,
        "invalid");
}

TEST(validuate_url_signature, empty_url_is_fine)
{
    EXPECT_NO_THROW(validateUrl({}, {}, AllowUnsigned::Yes));
}

} // namespace maps::apiteka::signature::tests
