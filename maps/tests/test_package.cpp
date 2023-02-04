#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/common/namespace.h>
#include <maps/automotive/store_internal/lib/dao/introspection.h>
#include <maps/automotive/store_internal/lib/dao/package.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/yacare/helpers.h>
#include <maps/automotive/store_internal/yacare/tests/package.h>

namespace maps::automotive::store_internal {

using namespace ::testing;

TEST_F(PackageFixture, GetOne)
{
    auto pkg = packageWithDependency();
    auto response = mockGet(API_PREFIX + packageId(pkg.id()));
    ASSERT_EQ(200, response.status);
    EXPECT_EQ(printToString(pkg), TString(response.body));
}

TEST_F(PackageFixture, http_4xx)
{
    auto pkg = packageNoRollout();
    auto id = packageId(pkg.id());
    auto name = pkg.id().name();
    EXPECT_EQ(200, mockGet(API_PREFIX + id).status);
    EXPECT_EQ(404, mockGet(API_PREFIX + name + "/0").status);

    EXPECT_EQ(405, mockPost(API_PREFIX + id).status);
    EXPECT_EQ(405, mockPost(API_PREFIX).status);

    auto body = printToString(pkg.metadata());
    EXPECT_EQ(404, mockPut(API_PREFIX + name + "/7751", body).status);

    auto idWithRollout = packageId(packageWithRollout().id());
    EXPECT_EQ(400, mockPut(API_PREFIX + name).status);
    EXPECT_EQ(400, mockPut(API_PREFIX + name, body).status);
    EXPECT_EQ(400, mockPut(API_PREFIX + id).status);
    EXPECT_EQ(400, mockPost("/store/1.x/package").status);
    EXPECT_EQ(400, mockGet(API_PREFIX + name).status);
    EXPECT_EQ(200, mockGet(API_PREFIX + idWithRollout).status);
    EXPECT_EQ(400, mockGet(API_PREFIX + idWithRollout + "/unwanted").status);
    EXPECT_EQ(400, mockGet(API_PREFIX + name + "/version_code").status);
    EXPECT_EQ(400, mockPut(API_PREFIX + name + "/version_code").status);
    EXPECT_EQ(400, mockDelete(API_PREFIX + name + "/version_code").status);
    EXPECT_EQ(400, mockGet(API_PREFIX + name + "/-1").status);
    EXPECT_EQ(400, mockPut(API_PREFIX + name + "/-1").status);
    EXPECT_EQ(400, mockDelete(API_PREFIX + name + "/-1").status);
}

TEST_F(PackageFixture, GetOneCorrupted)
{
    {
        auto txn = dao::makeWriteableTransaction();
        txn->exec(R"(update package set title='["not object but array"]')");
        txn->commit();
    }
    auto response = mockGet(API_PREFIX + packageId(packageNoRollout().id()));
    ASSERT_EQ(500, response.status);
}

TEST_F(PackageFixture, Put)
{
    auto pkg = packageNoRollout();
    auto id = packageId(pkg.id());
    auto& metadata = *pkg.mutable_metadata();
    metadata.set_app_name("New");
    metadata.set_version_name("New");
    metadata.mutable_title()->operator[]("ru_RU") = "New RU";
    metadata.mutable_title()->operator[]("tr_TR") = "New TR";
    metadata.mutable_release_notes()->operator[]("en_US") = "New US";
    metadata.mutable_release_notes()->operator[]("tr_TR") = "New TR";
    auto body = printToString(pkg.metadata());
    ASSERT_EQ(204, mockPut(API_PREFIX + id, body).status);

    pkg.set_url(packageUrl(pkg));
    auto response = mockGet(API_PREFIX + id);
    ASSERT_EQ(200, response.status);
    EXPECT_EQ(printToString(pkg), TString(response.body));
}

TEST_F(PackageFixture, LocaleValidationPut)
{
    auto pkg = packageNoRollout();
    auto id = packageId(pkg.id());
    auto& metadata = *pkg.mutable_metadata();

    (*metadata.mutable_title())["it_IS"] = "OK";
    ASSERT_EQ(204, mockPut(API_PREFIX + id, printToString(metadata)).status);

    (*metadata.mutable_title())["en_EN"] = "";
    ASSERT_EQ(400, mockPut(API_PREFIX + id, printToString(metadata)).status);

    metadata.mutable_title()->clear();
    (*metadata.mutable_release_notes())["en_EN"] = "";
    ASSERT_EQ(400, mockPut(API_PREFIX + id, printToString(metadata)).status);
}

TEST_F(PackageFixture, LocaleValidationPost)
{
    auto pkg = packageNoRollout();
    pkg.mutable_id()->set_name("newsuperapp");
    auto id = packageId(pkg.id());

    pkg.set_url("bucket/path");
    EXPECT_CALL(s3Mock(), HeadObject(_))
        .WillRepeatedly(Return(s3::model::HeadObjectResult()
            .WithETag(R"("md5")")));
    EXPECT_CALL(s3Mock(), CopyObject(_))
        .WillRepeatedly(Throw(std::runtime_error(
            "POST accepted but further actions are not important in this test")));

    (*pkg.mutable_metadata()->mutable_title())["it_IS"] = "OK";
    ASSERT_EQ(202, mockPost("/store/1.x/package", printToString(pkg)).status);

    (*pkg.mutable_metadata()->mutable_title())["en_EN"] = "";
    ASSERT_EQ(400, mockPost("/store/1.x/package", printToString(pkg)).status);

    pkg.mutable_metadata()->mutable_title()->clear();
    (*pkg.mutable_metadata()->mutable_release_notes())["en_EN"] = "";
    ASSERT_EQ(400, mockPost("/store/1.x/package", printToString(pkg)).status);
}

TEST_F(PackageFixture, Delete)
{
    {
        auto pkg = packageNoRollout();
        auto id = packageId(pkg.id());
        ASSERT_EQ(204, mockDelete(API_PREFIX + id).status);
        EXPECT_EQ(404, mockGet(API_PREFIX + id).status);
    }
    { // Cannot delete package with rollouts
        auto pkg = packageWithRollout();
        auto id = packageId(pkg.id());
        ASSERT_EQ(422, mockDelete(API_PREFIX + id).status);
        EXPECT_EQ(200, mockGet(API_PREFIX + id).status);
    }
    { // Can delete package after all rollouts deleted
        auto roBody = printToString(defaultPackageRollout());
        ASSERT_EQ(204, mockDelete("/store/1.x/rollout/package/config", roBody).status);
        auto pkg = packageWithRollout();
        auto id = packageId(pkg.id());
        ASSERT_EQ(204, mockDelete(API_PREFIX + id).status);
        EXPECT_EQ(404, mockGet(API_PREFIX + id).status);
    }
}

TEST_F(PackageFixture, ForceDelete)
{
    {
        auto pkg = packageNoRollout();
        auto id = packageId(pkg.id());
        ASSERT_EQ(204, mockDelete(API_PREFIX + id + "?force=true").status);
        EXPECT_EQ(404, mockGet(API_PREFIX + id).status);
    }
    { // Can delete package with rollouts using flag force=true
        auto pkg = packageWithRollout();
        auto id = packageId(pkg.id());
        ASSERT_EQ(422, mockDelete(API_PREFIX + id).status);
        EXPECT_EQ(200, mockGet(API_PREFIX + id).status);
        ASSERT_EQ(204, mockDelete(API_PREFIX + id + "?force=true").status);
        EXPECT_EQ(404, mockGet(API_PREFIX + id).status);
    }
}

TEST_F(PackageFixture, GetAllProto)
{
    http::MockRequest request(
        http::GET, http::URL("http://localhost/store/1.x/package"));
    request.headers["Accept"] = "application/x-protobuf";
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(200, response.status);

    Packages packages;
    Y_PROTOBUF_SUPPRESS_NODISCARD packages.ParseFromString(TString(response.body));

    ASSERT_EQ(3, packages.packages().size());
    std::unordered_map<Package::Id, const Package*, introspection::Hasher> pkgById;
    for (const auto& pkg: packages.packages()) {
        pkgById[pkg.id()] = &pkg;
    }

    auto noRollout = packageNoRollout();
    auto withRollout = packageWithRollout();
    auto withDependency = packageWithDependency();
    EXPECT_EQ(printToString(noRollout), printToString(*pkgById.at(noRollout.id())));
    EXPECT_EQ(printToString(withRollout), printToString(*pkgById.at(withRollout.id())));
    EXPECT_EQ(printToString(withDependency), printToString(*pkgById.at(withDependency.id())));
}

TEST_F(PackageFixture, Post)
{
    Package pkg = buildPackage("new_pkg", 1);
    pkg.mutable_id()->set_flavor("flavor");
    auto& metadata = *pkg.mutable_metadata();
    metadata.set_app_name("ru.paket.new");
    metadata.set_version_name("v1");
    metadata.mutable_title()->clear();
    metadata.mutable_title()->operator[]("ru") = "Old RU title";
    metadata.mutable_release_notes()->clear();
    metadata.mutable_release_notes()->operator[]("ru") = "Old RU notes";
    auto dependency = pkg.mutable_metadata()->add_depends();
    *dependency->mutable_feature() = theFeature().id();
    dependency->mutable_feature()->set_version_minor(5);
    pkg.set_url("incoming/package");
    pkg.set_size(102030);
    auto id = packageId(pkg.id());

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    ASSERT_EQ(ToString(Dao::CreationStatus::Created), createPkg(pkg).status());

    pkg.set_url(packageUrl(pkg));
    {
        auto rsp = mockGet(API_PREFIX + id);
        ASSERT_EQ(200, rsp.status);
        Package addedPkg;
        parseFromString(rsp.body, addedPkg);
        PROTO_EQ(pkg, addedPkg);
    }
    {
        http::MockRequest request(
            http::GET, http::URL("http://localhost" + API_PREFIX + id));
        request.headers["Accept"] = "application/x-protobuf";
        auto rsp = yacare::performTestRequest(request);
        ASSERT_EQ(200, rsp.status);
        Package addedPkg;
        Y_PROTOBUF_SUPPRESS_NODISCARD addedPkg.ParseFromString(TString(rsp.body));
        PROTO_EQ(pkg, addedPkg);
    }
}

TEST_F(PackageFixture, PostEmptyTranslations)
{
    Package pkg = buildPackage("new_pkg", 1);
    pkg.mutable_id()->set_flavor("flavor");
    auto& metadata = *pkg.mutable_metadata();
    metadata.set_app_name("ru.paket.new");
    metadata.set_version_name("v1");
    metadata.mutable_title()->clear();
    metadata.mutable_release_notes()->clear();

    pkg.set_url("incoming/package");
    pkg.set_size(112233);
    auto id = packageId(pkg.id());

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    ASSERT_EQ(ToString(Dao::CreationStatus::Created), createPkg(pkg).status());

    auto rsp = mockGet(API_PREFIX + id);
    ASSERT_EQ(200, rsp.status);
    Package addedPkg;
    parseFromString(rsp.body, addedPkg);
    EXPECT_EQ(3, addedPkg.metadata().title_size());
    EXPECT_EQ("Название", addedPkg.metadata().title().at("ru"));
    EXPECT_EQ("TR\ntitle", addedPkg.metadata().title().at("tr"));
    EXPECT_EQ("African title", addedPkg.metadata().title().at("af"));
    EXPECT_EQ(3, addedPkg.metadata().release_notes_size());
    EXPECT_EQ("Что нового", addedPkg.metadata().release_notes().at("ru"));
    EXPECT_EQ("TR\nwhatsnew", addedPkg.metadata().release_notes().at("tr"));
    EXPECT_EQ("African whatsnew", addedPkg.metadata().release_notes().at("af"));
}

TEST_F(PackageFixture, PostTeamcity)
{
    Package pkg = buildPackage("teamcity", 1);
    pkg.mutable_id()->set_flavor("");
    auto& metadata = *pkg.mutable_metadata();
    metadata.set_app_name("teamcity");
    metadata.set_version_name("v1");
    metadata.mutable_title()->operator[]("ru") = "Teamcity";
    const auto redirectUrl = "redirected/to/package";
    pkg.set_size(102030);

    pkg.set_url("https://teamcity.yandex-team.ru/repository/download/hand/some.apk");
    auto id = packageId(pkg.id());
    auto mock = http::addMock(pkg.url(), [&](const auto&) {
        auto rsp = http::MockResponse::withStatus(302);
        rsp.headers.emplace(
            "Location", std::string("https://s3.mds.yandex.net/") + redirectUrl);
        return rsp;
    });

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    pkg.set_url(redirectUrl);
    ASSERT_EQ(
        ToString(Dao::CreationStatus::Created),
        createPkg(pkg).status());

    auto rsp = mockGet(API_PREFIX + id);
    ASSERT_EQ(200, rsp.status);
}

TEST_F(PackageFixture, PostIncomingNotFound)
{
    Package pkg = buildPackage("new_pkg", 1);
    pkg.set_url("incoming/package");
    auto id = packageId(pkg.id());

    EXPECT_CALL(s3Mock(), HeadObject(_))
        .WillRepeatedly(Return(notFoundOutcome()));

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    ASSERT_EQ(400, mockPost("/store/1.x/package", printToString(pkg)).status);
}

TEST_F(PackageFixture, PostExisting)
{
    Package pkg = packageNoRollout();
    pkg.set_url("incoming/package");
    auto id = packageId(pkg.id());

    EXPECT_CALL(s3Mock(), HeadObject(_))
        .WillRepeatedly(Return(s3::model::HeadObjectResult()
            .WithETag(R"("MD5")")));

    ASSERT_FALSE(pkg.id().has_flavor());
    ASSERT_EQ(200, mockGet(API_PREFIX + id).status);
    EXPECT_EQ(422, mockPost("/store/1.x/package", printToString(pkg)).status);

    pkg = packageWithRollout();
    pkg.set_url("incoming/package");
    id = packageId(pkg.id());
    ASSERT_TRUE(pkg.id().has_flavor());
    ASSERT_EQ(200, mockGet(API_PREFIX + id).status);
    EXPECT_EQ(422, mockPost("/store/1.x/package", printToString(pkg)).status);
}

TEST_F(PackageFixture, PostWhileCreatingTheSamePackage)
{
    Package pkg = buildPackage("PostWhileCreatingTheSamePackage", 1);
    pkg.set_url("incoming/package");
    auto id = packageId(pkg.id());

    EXPECT_CALL(s3Mock(), HeadObject(_))
        .WillRepeatedly(Return(s3::model::HeadObjectResult()
            .WithETag(R"("MD5")")));

    EXPECT_CALL(s3Mock(), CopyObject(_))
        .WillOnce(DoAll(
            InvokeWithoutArgs([&pkg] {
                EXPECT_EQ(409, mockPost("/store/1.x/package", printToString(pkg)).status);
            }),
            Throw(std::runtime_error("Shit happens"))));

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    EXPECT_EQ(202, mockPost("/store/1.x/package", printToString(pkg)).status);
    EXPECT_EQ(
        ToString(Dao::CreationStatus::Error),
        waitCreationStatus(API_PREFIX + "creation_status/" + id).status());
}

TEST_F(PackageFixture, PostFailed)
{
    Package pkg = buildPackage("new_pkg", 1);
    pkg.set_url("incoming/package");
    auto id = packageId(pkg.id());

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);

    ASSERT_EQ(
        ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::CopyFailed).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);

    ASSERT_EQ(
        ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::HeadEmpty).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);

    ASSERT_EQ(
        ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::HeadFailed).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
}

TEST_F(PackageFixture, PostSameMD5)
{
    auto originalPkg = packageNoRollout();
    auto response = mockGet(API_PREFIX + packageId(originalPkg.id()));
    ASSERT_FALSE(originalPkg.id().has_flavor());
    ASSERT_EQ(200, response.status);
    parseFromString(response.body, originalPkg);
    originalPkg.set_url("incoming/package");

    auto quotedMd5 = "\"" + originalPkg.md5() + "\"";
    { // same (name, version_code, flavor), different MD5
        EXPECT_CALL(s3Mock(), HeadObject(_))
            .WillOnce(Return(s3::model::HeadObjectResult().WithETag(R"("different_MD5")")));
        ASSERT_EQ(422, mockPost("/store/1.x/package", printToString(originalPkg)).status);
    }
    { // same MD5, different name
        auto pkg = originalPkg;
        pkg.mutable_id()->set_name("same_md5_different_name");
        EXPECT_CALL(s3Mock(), HeadObject(_))
            .WillOnce(Return(s3::model::HeadObjectResult().WithETag(quotedMd5)));
        ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
        ASSERT_EQ(400, mockPost("/store/1.x/package", printToString(pkg)).status);
        ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
    }
    { // same MD5, different version_code
        auto pkg = originalPkg;
        pkg.mutable_id()->set_version_code(originalPkg.id().version_code() + 1);
        EXPECT_CALL(s3Mock(), HeadObject(_))
            .WillOnce(Return(s3::model::HeadObjectResult().WithETag(quotedMd5)));
        ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
        ASSERT_EQ(400, mockPost("/store/1.x/package", printToString(pkg)).status);
        ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
    }
    Package::Id id1;
    { // same MD5, different flavor
        auto pkg = originalPkg;
        pkg.mutable_id()->set_flavor("same_md5_different_flavor");
        ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
        ASSERT_EQ(ToString(Dao::CreationStatus::Created), createPkg(pkg).status());
        ASSERT_EQ(200, mockGet(API_PREFIX + packageId(pkg.id())).status);
        id1 = pkg.id();
    }
    Package::Id id2;
    { // same MD5, another flavor
        auto pkg = originalPkg;
        pkg.mutable_id()->set_flavor("same_md5_another_flavor");
        ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
        ASSERT_EQ(ToString(Dao::CreationStatus::Created), createPkg(pkg).status());
        ASSERT_EQ(200, mockGet(API_PREFIX + packageId(pkg.id())).status);
        id2 = pkg.id();
    }

    // delete all packages with that MD5
    ASSERT_EQ(204, mockDelete(API_PREFIX + packageId(id1)).status);
    ASSERT_EQ(204, mockDelete(API_PREFIX + packageId(id2)).status);
    ASSERT_EQ(204, mockDelete(API_PREFIX + packageId(originalPkg.id())).status);

    // make sure we can create package with the same MD5 but other name
    auto pkg = originalPkg;
    pkg.mutable_id()->set_name("same_md5_different_name");
    ASSERT_EQ(404, mockGet(API_PREFIX + packageId(pkg.id())).status);
    ASSERT_EQ(ToString(Dao::CreationStatus::Created), createPkg(pkg).status());
    ASSERT_EQ(200, mockGet(API_PREFIX + packageId(pkg.id())).status);
}

TEST_F(PackageFixture, PostMultipartMD5)
{
    Package pkg = buildPackage("multipart", 1);
    pkg.set_url("incoming/multipart");
    pkg.set_size(90210);
    auto pkgId = packageId(pkg.id());
    ASSERT_EQ(404, mockGet(API_PREFIX + pkgId).status);

    ASSERT_EQ(ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::CopiedCopyFailed).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + pkgId).status);

    ASSERT_EQ(ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::CopiedHeadEmpty).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + pkgId).status);

    ASSERT_EQ(ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::CopiedHeadFailed).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + pkgId).status);

    ASSERT_EQ(ToString(Dao::CreationStatus::Error),
        createPkg(pkg, PackageFixture::CopiedDeleteFailed).status());
    ASSERT_EQ(404, mockGet(API_PREFIX + pkgId).status);

    ASSERT_EQ(ToString(Dao::CreationStatus::Created),
        createPkg(pkg, PackageFixture::CopiedNoFailure).status());
    auto rsp = mockGet(API_PREFIX + pkgId);
    ASSERT_EQ(200, rsp.status);
    pkg.set_url(packageUrl(pkg));
    EXPECT_EQ(printToString(pkg), TString(rsp.body));
}

TEST_F(PackageFixture, BadTitle)
{
    Package pkg = buildPackage("BadTitle", 1);
    pkg.mutable_metadata()->mutable_title()->clear();
    (*pkg.mutable_metadata()->mutable_title())["en"] = "EN";

    ASSERT_EQ(400, mockPost("/store/1.x/package", printToString(pkg)).status);

    auto pkgId = packageId(packageNoRollout().id());
    ASSERT_EQ(400, mockPut(API_PREFIX + pkgId, printToString(pkg.metadata())).status);

    pkg.mutable_metadata()->mutable_title()->clear();
    (*pkg.mutable_metadata()->mutable_title())["ru_RU"] = "RU";
    ASSERT_EQ(204, mockPut(API_PREFIX + pkgId, printToString(pkg.metadata())).status);
}

TEST_F(PackageFixture, CreateMultipleFlavors)
{
    auto pkg = packageWithRollout();
    ASSERT_TRUE(pkg.id().has_flavor());
    { // add package with new flavor
        auto txn = dao::makeWriteableTransaction();
        pkg.mutable_id()->set_flavor("new-flavor");
        pkg.set_md5("md5v1");
        PackageDao(*txn).create(pkg);
        txn->commit();
    }
    {
        auto rsp = mockGet(API_PREFIX + packageId(pkg.id()));
        ASSERT_EQ(200, rsp.status);
        pkg.set_url(packageUrl(pkg));
        EXPECT_EQ(printToString(pkg), TString(rsp.body));
    }
    { // add package with empty flavor
        auto txn = dao::makeWriteableTransaction();
        pkg.mutable_id()->clear_flavor();
        pkg.set_md5("md5v2");
        PackageDao(*txn).create(pkg);
        txn->commit();
    }
    {
        auto rsp = mockGet(API_PREFIX + packageId(pkg.id()));
        ASSERT_EQ(200, rsp.status);
        pkg.set_url(packageUrl(pkg));
        EXPECT_EQ(printToString(pkg), TString(rsp.body));
    }
}

TEST_F(PackageFixture, PostDependentNoFw)
{
    Package pkg = packageNoRollout();
    pkg.mutable_id()->set_name("another_name");
    auto dep = pkg.mutable_metadata()->add_depends();
    dep->mutable_feature()->set_name("another_feature");
    dep->mutable_feature()->set_version_major(1);
    dep->mutable_feature()->set_version_minor(1);
    pkg.set_url("incoming/package");
    auto id = packageId(pkg.id());
    pkg.set_md5("00112233445566778899aabbccddeeff");
    auto path = packagePath(pkg);

    EXPECT_CALL(s3Mock(), HeadObject(_))
        .WillOnce(Return(s3::model::HeadObjectResult()
            .WithETag("\"" + pkg.md5() + "\"")));

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    ASSERT_EQ(422, mockPost("/store/1.x/package", printToString(pkg)).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
}

TEST_F(PackageFixture, PostScopedNoFw)
{
    Package pkg = packageNoRollout();
    pkg.mutable_id()->set_name("another_name");
    auto dep = pkg.mutable_metadata()->add_depends();
    dep->mutable_feature()->set_name("another_feature");
    dep->mutable_feature()->set_version_major(1);
    dep->mutable_feature()->set_version_minor(1);
    dep->add_headunit()->set_type("taxi");
    pkg.set_url("incoming/package");
    pkg.set_md5("00112233445566778899aabbccddeeff");
    auto id = packageId(pkg.id());

    http::MockHandle tankerMock("");
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    ASSERT_EQ(
        ToString(Dao::CreationStatus::Created),
        createPkg(pkg).status());
    ASSERT_EQ(200, mockGet(API_PREFIX + id).status);
}

TEST_F(PackageFixture, NullEmptyFlavor)
{
    auto pkg = packageNoRollout();
    auto id = packageId(pkg.id());
    ASSERT_FALSE(pkg.id().has_flavor());

    (*pkg.mutable_metadata()->mutable_title())["ru"] = "NullEmptyFlavor";
    ASSERT_EQ(204, mockPut(API_PREFIX + id + "/", printToString(pkg.metadata())).status);

    {
        Package updatedPkg;
        auto rsp = mockGet(API_PREFIX + id);
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, updatedPkg);
        PROTO_EQ(pkg, updatedPkg);
    }
    {
        Package updatedPkg;
        auto rsp = mockGet(API_PREFIX + id + "/");
        ASSERT_EQ(200, rsp.status);
        parseFromString(rsp.body, updatedPkg);
        PROTO_EQ(pkg, updatedPkg);
    }

    ASSERT_EQ(204, mockDelete(API_PREFIX + id + "/").status);
    ASSERT_EQ(404, mockDelete(API_PREFIX + id + "/").status);
    ASSERT_EQ(404, mockDelete(API_PREFIX + id).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + id + "/").status);
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
}

TEST_F(PackageFixture, NotExistingApp)
{
    auto response = mockGet("/store/1.x/package");
    ASSERT_EQ(200, response.status);
    auto defaultBody = response.body;

    Package pkg = buildPackage("new_pkg", 1);
    pkg.mutable_id()->set_flavor("flavor");
    auto& metadata = *pkg.mutable_metadata();
    metadata.set_app_name("non.existing.app");
    metadata.set_version_name("v1");
    pkg.set_url("incoming/package");
    pkg.set_size(102030);
    auto id = packageId(pkg.id());

    EXPECT_CALL(s3Mock(), HeadObject(_))
        .WillRepeatedly(Return(s3::model::HeadObjectResult()
            .WithETag(R"("md5")")));

    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);
    ASSERT_EQ(422, mockPost("/store/1.x/package", printToString(pkg)).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + id).status);

    response = mockGet("/store/1.x/package");
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(defaultBody, response.body);
}

TEST_F(PackageFixture, forbidden)
{
    Package pkg = buildPackage("forbidden_pkg", 1);
    pkg.set_url("incoming/package");
    auto pkgId = packageId(pkg.id());
    for (const std::string& user: {"key-manager-prod", "key-manager", "viewer-victor"}) {
        yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
        setupMocks(pkg, FailedStep::ValidationFailed);
        ASSERT_EQ(401, mockPost("/store/1.x/package", printToString(pkg)).status);
        ASSERT_EQ(401, mockPut(API_PREFIX + pkgId, printToString(pkg.metadata())).status);
        ASSERT_EQ(401, mockDelete(API_PREFIX + pkgId).status);
    }
    yacare::tests::UserInfoFixture user{makeUserInfo("manager")};
    ASSERT_EQ(401, mockDelete(API_PREFIX + pkgId + "?force=true").status);
}

} // namespace maps::automotive::store_internal
