#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/libs/edge_persistent_index/include/persistent_index.h>
#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <yandex/maps/jams/static_graph2/persistent_index.h>

#include <algorithm>
#include <fstream>
#include <random>

using namespace maps::road_graph;
using namespace maps::road_graph::literals;

namespace mjs = maps::jams::static_graph2;

const auto TEMPORARY_FILE_NAME = BinaryPath("test_case.fb");
const auto PREGENERATED_FILENAME =
    BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");

std::pair<std::vector<uint64_t>, std::vector<uint32_t>> genRandomIds(
    const size_t totalIds = 4327)
{
    std::vector<uint64_t> longIds(totalIds);
    std::vector<uint32_t> shortIds(totalIds);

    std::mt19937_64 gen;
    gen.seed(42);

    std::uniform_int_distribution<uint64_t> dis64;
    std::uniform_int_distribution<uint32_t> dis32(1, 3);

    for (auto& id: longIds) {
        id = dis64(gen);
    }

    uint32_t curEdgeId = 0;
    for (auto& id: shortIds) {
        curEdgeId += dis32(gen);
        id = curEdgeId;
    }

    return {longIds, shortIds};
}

std::pair<std::vector<LongEdgeId>, std::vector<EdgeId>> generateTestFile(
    const std::string& filename = TEMPORARY_FILE_NAME,
    const size_t totalIds = 4327)
{
    auto [longIds, shortIds] = genRandomIds(totalIds);

    PersistentIndexBuilder persistentIndexBuilder("test_version");
    persistentIndexBuilder.reserve(longIds.size());

    std::vector<LongEdgeId> taggedLongIds(longIds.size());
    for (size_t i = 0; i < totalIds; i++) {
        taggedLongIds[i] = LongEdgeId(longIds[i]);
    }

    std::vector<EdgeId> taggedShortIds(shortIds.size());
    for (size_t i = 0; i < totalIds; i++) {
        taggedShortIds[i] = EdgeId(shortIds[i]);
        persistentIndexBuilder.setEdgePersistentId(
            taggedShortIds[i], taggedLongIds[i]);
    }

    std::ofstream fout(filename);
    persistentIndexBuilder.save(fout);

    return {taggedLongIds, taggedShortIds};
}

void generateTestFile(const std::string& filename,
    const std::vector<LongEdgeId>& longIds, const std::vector<EdgeId>& shortIds) {
    PersistentIndexBuilder persistentIndexBuilder("test");

    persistentIndexBuilder.reserve(longIds.size());
    for (size_t i = 0; i < longIds.size(); i++) {
        persistentIndexBuilder.setEdgePersistentId(shortIds[i], longIds[i]);
    }

    std::ofstream fout(filename);
    persistentIndexBuilder.save(fout);
}

void createExplicitTestFile(){
    generateTestFile(
        TEMPORARY_FILE_NAME,
        {LongEdgeId(1), LongEdgeId(UINT64_MAX - 1), LongEdgeId(0), LongEdgeId(47),
            LongEdgeId(UINT64_MAX)},
        {4_e, 1_e, 2675843_e, 0_e, 431_e});
}

Y_UNIT_TEST_SUITE(PersistentIndexFilemapped) {

Y_UNIT_TEST(TestFindEdgeIdExplicit) {
    createExplicitTestFile();
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    UNIT_ASSERT_EQUAL(0_e, persistentIndex.findShortId(LongEdgeId(47)).value());
    UNIT_ASSERT_EQUAL(4_e, persistentIndex.findShortId(LongEdgeId(1)).value());
    UNIT_ASSERT_EQUAL(2675843_e,
        persistentIndex.findShortId(LongEdgeId(0)).value());
    UNIT_ASSERT_EQUAL(1_e,
        persistentIndex.findShortId(LongEdgeId(UINT64_MAX - 1)).value());
    UNIT_ASSERT_EQUAL(431_e,
        persistentIndex.findShortId(LongEdgeId(UINT64_MAX)).value());

    auto id = persistentIndex.findShortId(LongEdgeId(2));
    UNIT_ASSERT(!id);
}

Y_UNIT_TEST(TestFindLongEdgeIdExplicit) {
    createExplicitTestFile();
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    UNIT_ASSERT_EQUAL(LongEdgeId(1), persistentIndex.findLongId(4_e).value());
    UNIT_ASSERT_EQUAL(LongEdgeId(47), persistentIndex.findLongId(0_e).value());
    UNIT_ASSERT_EQUAL(LongEdgeId(UINT64_MAX - 1),
        persistentIndex.findLongId(1_e).value());
    UNIT_ASSERT_EQUAL(LongEdgeId(UINT64_MAX),
        persistentIndex.findLongId(431_e).value());
    UNIT_ASSERT_EQUAL(LongEdgeId(0),
        persistentIndex.findLongId(2675843_e).value());

    auto id = persistentIndex.findLongId(13_e);
    UNIT_ASSERT(!id);
}

Y_UNIT_TEST(TestFindLongIdFromData)
{
    PersistentIndex persistentIndex(PREGENERATED_FILENAME);

    UNIT_ASSERT_EQUAL(LongEdgeId(2326584817928535218ULL),
        persistentIndex.findLongId(197686_e).value());
    UNIT_ASSERT_EQUAL(LongEdgeId(10710410755214454439ULL),
        persistentIndex.findLongId(204282_e).value());
    UNIT_ASSERT_EQUAL(LongEdgeId(4591398318513191828ULL),
        persistentIndex.findLongId(294_e).value());

    auto id = persistentIndex.findLongId(9_e);
    UNIT_ASSERT(!id);
}

Y_UNIT_TEST(TestFindShortIdFromData)
{
    PersistentIndex persistentIndex(PREGENERATED_FILENAME);

    UNIT_ASSERT_EQUAL(197686_e,
        persistentIndex.findShortId(LongEdgeId(2326584817928535218ULL)).value());
    UNIT_ASSERT_EQUAL(204282_e,
        persistentIndex.findShortId(LongEdgeId(10710410755214454439ULL)).value());
    UNIT_ASSERT_EQUAL(294_e,
        persistentIndex.findShortId(LongEdgeId(4591398318513191828ULL)).value());

    auto id = persistentIndex.findShortId(LongEdgeId(136909428672063078ULL));
    UNIT_ASSERT(!id);
}

Y_UNIT_TEST(TestFindEdgeId) {
    auto [longIds, shortIds] = generateTestFile(TEMPORARY_FILE_NAME);
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    for (size_t i = 0; i < longIds.size(); i++) {
        UNIT_ASSERT_EQUAL(shortIds[i],
            persistentIndex.findShortId(longIds[i]).value());
    }
}

Y_UNIT_TEST(TestFindLongEdgeId) {
    auto [longIds, shortIds] = generateTestFile(TEMPORARY_FILE_NAME);
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    for (size_t i = 0; i < shortIds.size(); i++) {
        UNIT_ASSERT_EQUAL(longIds[i],
            persistentIndex.findLongId(shortIds[i]).value());
    }
}

Y_UNIT_TEST(LongShortLong) {
    auto [longIds, shortIds] = generateTestFile(TEMPORARY_FILE_NAME);
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    for (const auto& id: longIds) {
        UNIT_ASSERT_EQUAL(
            id,
            persistentIndex.findLongId(
                persistentIndex.findShortId(id).value()).value());
    }
}

Y_UNIT_TEST(ShortLongShort) {
    auto [longIds, shortIds] = generateTestFile(TEMPORARY_FILE_NAME);
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    for (const auto& id: shortIds) {
        UNIT_ASSERT_EQUAL(
            id,
            persistentIndex.findShortId(
                persistentIndex.findLongId(id).value()).value());
    }
}

Y_UNIT_TEST(TestSize) {
    auto [longIds, shortIds] = generateTestFile(TEMPORARY_FILE_NAME);
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    UNIT_ASSERT_EQUAL(persistentIndex.size(), longIds.size());
}

Y_UNIT_TEST(DuplicateLongIdResolutionCompatibility) {
    const size_t totalIds = 123541;
    auto[longIds, shortIds] = genRandomIds(totalIds);

    // Add some longId duplicates
    for (size_t i = 2; i < 15; i++) {
        longIds[totalIds / i] = longIds[i];
        longIds[totalIds - i] = longIds[0];
    }

    PersistentIndexBuilder persistentIndexBuilder("test_version");
    persistentIndexBuilder.reserve(longIds.size());

    mjs::PersistentIndexBuilder mmsPersistentIndexBuilder;
    for (size_t i = 0; i < totalIds; i++) {
        persistentIndexBuilder.setEdgePersistentId(
            EdgeId(shortIds[i]), LongEdgeId(longIds[i]));
        mmsPersistentIndexBuilder.add(mjs::LongId{longIds[i]}, shortIds[i]);
    }
    mmsPersistentIndexBuilder.build();
    mjs::PersistentIndex mmsPersistentIndex(mmsPersistentIndexBuilder);

    std::ofstream fout(TEMPORARY_FILE_NAME);
    persistentIndexBuilder.save(fout);
    fout.close();
    PersistentIndex persistentIndex(TEMPORARY_FILE_NAME);

    for (const auto& shortId: shortIds) {
        UNIT_ASSERT_VALUES_EQUAL(
            persistentIndex.findLongId(EdgeId(shortId))->value(),
            mmsPersistentIndex.findLongId(shortId)->value());
    }
}

Y_UNIT_TEST(DuplicateShortIdFails)
{
    PersistentIndexBuilder persistentIndexBuilder("test_version");
    persistentIndexBuilder.setEdgePersistentId(1_e, LongEdgeId(1));
    persistentIndexBuilder.setEdgePersistentId(1_e, LongEdgeId(2));

    std::ofstream fout(TEMPORARY_FILE_NAME);
    UNIT_ASSERT_EXCEPTION(persistentIndexBuilder.save(fout), maps::RuntimeError);
}

Y_UNIT_TEST(EmptyPersistentIndex)
{
    PersistentIndexBuilder("test_version").build();
}
}
