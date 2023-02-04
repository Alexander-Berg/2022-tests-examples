#include <maps/automotive/store_internal/tests/mocks.h>

#include <library/cpp/uri/qargs.h>
#include <maps/automotive/store_internal/tests/helpers.h>

namespace maps::automotive::store_internal {

namespace {

std::string parseKeysetId(const std::string& tankerUri)
{
    using namespace NUri;
    TUri uri;
    EXPECT_EQ(TState::ParsedOK,
        uri.Parse(tankerUri, TFeature::FeaturesDefault | TFeature::FeatureSchemeKnown));
    std::string keyset;
    TQueryArgFilter filter = [](const TQueryArg& arg, void* data) {
        if (arg.Name == "keyset-id") {
            auto keysetId = static_cast<std::string*>(data);
            *keysetId = arg.Value;
        }
        return true;
    };
    NUri::TQueryArgProcessing qArgs(TQueryArg::FeatureFilter, filter, &keyset);
    qArgs.Process(uri);
    return keyset;
}

http::MockHandle mockTankerForCache()
{
    return http::addMock("https://tanker-beta.yandex-team.ru/api/legacy/keysets/json/",
        [](const http::MockRequest& request) {
            return readTestData(
                "tests/tanker/" + parseKeysetId(request.url.toString()) + ".json");
        });
}

http::MockHandle mockHttp(int httpStatus, const http::URL& url)
{
    return http::addMock(
        url,
        [=](const http::MockRequest&) {
            return http::MockResponse().withStatus(httpStatus);
        });
}

} // namespace

MockedS3Client::MockedS3Client(const Config::S3& cfg)
    : S3Client(cfg, std::make_unique<s3::S3ClientMock>())
{}

AppContextMock::AppContextMock()
    : AppContext(getConfig())
    , s3_(s3config())
    , tankerMock_("")
    , teamcityMock_("")
    , teamcityBrowserMock_("")
{
    {
        auto mock = mockTankerForCache();
        initTanker();
    }
    initLogs();
    initTvm();
    tankerMock_ = mockHttp(
        500, "https://tanker-beta.yandex-team.ru/api/legacy/keysets/json/");
    teamcityMock_ = mockHttp(
        500, "https://teamcity.yandex-team.ru/repository/download/");
    teamcityBrowserMock_ = mockHttp(
        500, "https://teamcity.browser.yandex-team.ru/repository/download/");
}

AppContextFixture::AppContextFixture()
{
    s3::initAwsApi();
    g_ctx.reset(new AppContextMock());
    maps::log8::setLevel(maps::log8::Level::DEBUG);
}

AppContextFixture::~AppContextFixture()
{
    maps::log8::setLevel(maps::log8::Level::INFO);
    g_ctx.reset();
}

s3::model::HeadObjectOutcome notFoundOutcome()
{
    Aws::Client::AWSError<Aws::S3::S3Errors> err;
    err.SetResponseCode(Aws::Http::HttpResponseCode::NOT_FOUND);
    return s3::model::HeadObjectOutcome(err);
}

}
