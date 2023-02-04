#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/libs/yacare/marshalling.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>
#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/hashing.h>

using namespace maps::automotive;

namespace maps::automotive::store_internal {

namespace {

const std::string API_PREFIX = "/store/1.x/headunit_set";
const TString SET_A_NAME = "head_unit-set.1";
const TString SET_B_NAME = "head_unit-set.2";
const TString SET_A_HEADIDS[] = {
    "a-bc_", "123", "1xf8", 
};
const TString SET_B_HEADIDS[] = {
    "x.yz", "890", "0xff"
};
const TString NEW_HEADID = "newheadid123";
const TString INVALID_SET_NAME = "head unit set";
const TString INVALID_HEADID = "abc123!";

template <typename HeadIds>
HeadUnitSet initSet(const TString& setName, const HeadIds& headids)
{
    HeadUnitSet set;
    set.set_name(setName);
    for (const auto& headid: headids) {
        *set.add_headids() = headid;
    };
    return set;
}

template <typename T>
void ensureAPIResult(const std::string& handle, const T& value)
{
    auto response = mockGet(handle);
    ASSERT_EQ(200, response.status);
    T responseValue;
    parseFromString(response.body, /*out*/ responseValue);
    ASSERT_EQ(value, responseValue);
}

} // namespace

static bool operator==(
    const HeadUnitSet& setA, const HeadUnitSet& setB)
{
    if (setA.name() != setB.name()) {
        return false;
    }

    std::unordered_set<TString> headIdsA;
    std::unordered_set<TString> headIdsB;
    std::copy(
        setA.headids().begin(),
        setA.headids().end(),
        std::inserter(headIdsA, headIdsA.begin()));
    std::copy(
        setB.headids().begin(),
        setB.headids().end(),
        std::inserter(headIdsB, headIdsB.begin()));

    return headIdsA == headIdsB;
}

static bool operator==(
    const HeadUnitSets& setsA, const HeadUnitSets& setsB)
{
    std::unordered_map<TString, HeadUnitSet> mapA;
    std::unordered_map<TString, HeadUnitSet> mapB;

    for(const auto& set: setsA.headunit_sets()) {
        mapA[set.name()] = set;
    }

    for(const auto& set: setsB.headunit_sets()) {
        mapB[set.name()] = set;
    }

    return mapA == mapB;
}

struct HeadUnitSetApi: public AppContextPostgresFixture
{
    HeadUnitSetApi()
        : managerProd(makeUserInfo("manager-prod"))
    {}

    yacare::tests::UserInfoFixture managerProd;
};


TEST_F(HeadUnitSetApi, GetEmpty)
{
    ensureAPIResult(API_PREFIX, HeadUnitSets{});
}

TEST_F(HeadUnitSetApi, AddEmpty)
{
    HeadUnitSets emptySets;
    ASSERT_EQ(400, mockPut(API_PREFIX, printToString(emptySets)).status);

    ensureAPIResult(API_PREFIX, emptySets);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
}

TEST_F(HeadUnitSetApi, AddEmpytSet)
{
    HeadUnitSets sets;
    auto set = sets.add_headunit_sets();
    set->set_name(SET_A_NAME);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
}

TEST_F(HeadUnitSetApi, AddRemoveSingleSet)
{
    HeadUnitSets sets;
    auto set = sets.add_headunit_sets();
    *set = initSet(SET_A_NAME, SET_A_HEADIDS);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);

    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX, HeadUnitSets{});
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);

    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX, HeadUnitSets{});
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
}

TEST_F(HeadUnitSetApi, AddTwoSetsRemoveFirst)
{
    HeadUnitSets sets;

    auto setA = sets.add_headunit_sets();
    *setA = initSet(SET_A_NAME, SET_A_HEADIDS);

    auto setB = sets.add_headunit_sets();
    *setB = initSet(SET_B_NAME, SET_B_HEADIDS);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *setA);
    ensureAPIResult(API_PREFIX + "/" + SET_B_NAME, *setB);

    sets.mutable_headunit_sets()->DeleteSubrange(0, 1);

    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX, sets);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX + "/" + SET_B_NAME, *setB);

    ASSERT_EQ(204, mockDelete(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX, sets);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX + "/" + SET_B_NAME, *setB);
}

TEST_F(HeadUnitSetApi, InvalidSetName)
{
    HeadUnitSets sets;

    auto setA = sets.add_headunit_sets();
    *setA = initSet(SET_A_NAME, SET_A_HEADIDS);

    auto setB = sets.add_headunit_sets();
    *setB = initSet(SET_B_NAME, SET_B_HEADIDS);
    setB->set_name(INVALID_SET_NAME);

    ASSERT_EQ(422, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, HeadUnitSets{});
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
    ASSERT_EQ(422, mockGet(API_PREFIX + "/" + INVALID_SET_NAME).status);
    ASSERT_EQ(422, mockDelete(API_PREFIX + "/" + INVALID_SET_NAME).status);
}

TEST_F(HeadUnitSetApi, InvalidHeadId)
{
    HeadUnitSets sets;

    auto setA = sets.add_headunit_sets();
    *setA = initSet(SET_A_NAME, SET_A_HEADIDS);

    auto setB = sets.add_headunit_sets();
    *setB = initSet(SET_B_NAME, SET_B_HEADIDS);
    *setB->add_headids() = INVALID_HEADID;

    ASSERT_EQ(422, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, HeadUnitSets{});
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_A_NAME).status);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
    ASSERT_EQ(422, mockGet(API_PREFIX + "/" + INVALID_SET_NAME).status);
}

TEST_F(HeadUnitSetApi, AddRemoveIndividualHeadId)
{
    ASSERT_EQ(
        422,
        mockPut(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);

    HeadUnitSets sets;
    auto set = sets.add_headunit_sets();
    *set = initSet(SET_A_NAME, SET_A_HEADIDS);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);

    *set->add_headids() = NEW_HEADID;

    ASSERT_EQ(
        204,
        mockPut(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);

    ASSERT_EQ(
        204,
        mockPut(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);

    ASSERT_EQ(
        422,
        mockPut(API_PREFIX + "/" + SET_B_NAME + "/" + NEW_HEADID).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);

    set->mutable_headids()->RemoveLast();

    ASSERT_EQ(
        204,
        mockDelete(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);

    ASSERT_EQ(
        204,
        mockDelete(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);
}

TEST_F(HeadUnitSetApi, ReplaceSet)
{
    HeadUnitSets sets;
    auto set = sets.add_headunit_sets();
    *set = initSet(SET_A_NAME, SET_A_HEADIDS);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);

    *set = initSet(SET_A_NAME, SET_B_HEADIDS);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *set);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
}

TEST_F(HeadUnitSetApi, TryReuseHeadId)
{
    HeadUnitSets sets;
    auto setA = sets.add_headunit_sets();
    *setA = initSet(SET_A_NAME, SET_A_HEADIDS);

    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);
    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *setA);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);

    auto setB = sets.add_headunit_sets();
    *setB = initSet(SET_B_NAME, SET_A_HEADIDS);

    ASSERT_EQ(422, mockPut(API_PREFIX, printToString(sets)).status);

    sets.mutable_headunit_sets()->RemoveLast();

    ensureAPIResult(API_PREFIX, sets);
    ensureAPIResult(API_PREFIX + "/" + SET_A_NAME, *setA);
    ASSERT_EQ(404, mockGet(API_PREFIX + "/" + SET_B_NAME).status);
}

TEST_F(HeadUnitSetApi, TestForceDelete)
{
    const std::string PACKAGE_ROLLOUT_API_URL =
        "/store/1.x/rollout/package/config";

    const std::string FIRMWARE_ROLLOUT_API_URL =
        "/store/1.x/rollout/firmware/config";

    auto response = mockGet(PACKAGE_ROLLOUT_API_URL);
    ASSERT_EQ(200, response.status);
    auto packageRolloutBody = response.body;

    response = mockGet(FIRMWARE_ROLLOUT_API_URL);
    ASSERT_EQ(200, response.status);
    auto firmwareRolloutBody = response.body;

    HeadUnitSets sets;
    auto set = sets.add_headunit_sets();
    *set = initSet(SET_A_NAME, SET_A_HEADIDS);
    ASSERT_EQ(204, mockPut(API_PREFIX, printToString(sets)).status);

    {
        PackageRollout ro;
        ro.set_branch("testing");
        ro.mutable_headunit()->set_type("taxi");
        *ro.mutable_package_id() = packageNoRollout().id();
        ro.set_headunit_set_name(SET_A_NAME);
        auto roBody = printToString(ro);
        ASSERT_EQ(204, mockPut(PACKAGE_ROLLOUT_API_URL, roBody).status);
    }

    {
        FirmwareRollout ro;
        buildFirmwareRollout(ro, "testing", firmwareNoRollout().id());
        ro.set_headunit_set_name(SET_A_NAME);
        ro.mutable_headunit()->set_vendor("no");
        auto roBody = printToString(ro);
        ASSERT_EQ(204, mockPut(FIRMWARE_ROLLOUT_API_URL, roBody).status);
    }

    ASSERT_EQ(422, mockDelete(API_PREFIX + "/" + SET_A_NAME).status);
    ensureAPIResult(API_PREFIX, sets);

    ASSERT_EQ(
        204,
        mockDelete(API_PREFIX + "/" + SET_A_NAME + "?force=true").status);

    response = mockGet(PACKAGE_ROLLOUT_API_URL);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(packageRolloutBody, response.body);

    response = mockGet(FIRMWARE_ROLLOUT_API_URL);
    ASSERT_EQ(200, response.status);
    ASSERT_EQ(firmwareRolloutBody, response.body);

    ensureAPIResult(API_PREFIX, HeadUnitSets{});
}

TEST_F(HeadUnitSetApi, forbidden)
{
    HeadUnitSets sets;
    auto set = sets.add_headunit_sets();
    set->set_name(SET_A_NAME);

    for (const std::string& user: {"key-manager-prod", "key-manager", "viewer-victor"}) {
        yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
        ASSERT_EQ(401, mockPut(API_PREFIX, printToString(sets)).status);
        ASSERT_EQ(401, mockPut(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);
        ASSERT_EQ(401, mockDelete(API_PREFIX + "/" + SET_A_NAME + "/" + NEW_HEADID).status);
        ASSERT_EQ(401, mockDelete(API_PREFIX + "/" + SET_A_NAME).status);
    }
}

} // maps::automotive::store_internal
