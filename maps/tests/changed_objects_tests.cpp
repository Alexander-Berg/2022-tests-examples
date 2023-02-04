#include "helpers.h"

#include <yandex/maps/wiki/validator/validator.h>
#include <yandex/maps/wiki/validator/area_of_interest.h>
#include <yandex/maps/wiki/validator/categories.h>

#include <maps/wikimap/mapspro/libs/validator/loader/data_source.h>
#include <maps/wikimap/mapspro/libs/validator/common/categories_list.h>

#include <library/cpp/testing/unittest/registar.h>

namespace rev = maps::wiki::revision;

namespace maps {
namespace wiki {
namespace validator {
namespace tests {

namespace {

constexpr size_t THREAD_COUNT = 1;

const std::vector<TCategoryId> HYDRO_CATEGORIES = {
    categories::HYDRO::id(),
    categories::HYDRO_FC::id(),
    categories::HYDRO_FC_EL::id(),
    categories::HYDRO_FC_JC::id(),
    categories::HYDRO_NM::id(),
};

struct CategoriesLoader
{
public:
    CategoriesLoader(
            DataSource& dataSource,
            ThreadPool& threadPool)
        : dataSource_(dataSource)
        , threadPool_(threadPool)
    {
    }

    void loadCategory(const TCategoryId& categoryId)
    {
        std::set<TCategoryId> categoriesGuard;
        loadCategoryImpl(categoryId, categoriesGuard);
    }

private:
    void loadCategoryImpl(
        const TCategoryId& categoryId,
        std::set<TCategoryId>& categoriesGuard)
    {
        if (loadedCategories_.count(categoryId)) {
            return;
        }

        categoriesGuard.insert(categoryId);

        auto dependencies = dataSource_.dependencies(categoryId, LoaderType::LoadBySelectedObjects);
        for (const auto& depId : dependencies) {
            UNIT_ASSERT(!categoriesGuard.count(depId));
            loadCategoryImpl(depId, categoriesGuard);
        }

        dataSource_.load(categoryId, LoaderType::LoadBySelectedObjects, threadPool_);

        loadedCategories_.insert(categoryId);

        categoriesGuard.erase(categoryId);
    }

    DataSource& dataSource_;
    ThreadPool& threadPool_;
    std::set<TCategoryId> loadedCategories_;
};

void checkDataSource(
    const ValidatorConfig& validatorConfig,
    DBID headCommitId,
    TId selectedObjectId,
    const TCategoryId& selectedCategoryId,
    ThreadPool& threadPool)
{
    DataSource dataSource(
        validatorConfig,
        CheckCardinality::No,
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId,
        AreaOfInterest(),
        { selectedObjectId });

    UNIT_ASSERT(dataSource.hasSelectedObjectIds());

    for (const auto& categoryId : HYDRO_CATEGORIES) {
        if (categoryId == selectedCategoryId) {
            UNIT_ASSERT(!dataSource.selectedObjectIds(categoryId).empty());
        } else {
            UNIT_ASSERT(dataSource.selectedObjectIds(categoryId).empty());
        }
    }

    CategoriesLoader loader(dataSource, threadPool);
    for (const auto& categoryId : HYDRO_CATEGORIES) {
         loader.loadCategory(categoryId);
    }

    for (const auto& categoryId : HYDRO_CATEGORIES) {
        UNIT_ASSERT_VALUES_EQUAL(dataSource.collection(categoryId).objectIds().size(), 1);
    }
}

void checkCycles(
    const TCategoryId& categoryId,
    std::set<TCategoryId>& categoriesGuard,
    DataSource& dataSource)
{
    categoriesGuard.insert(categoryId);

    auto dependencies = dataSource.dependencies(categoryId, LoaderType::LoadBySelectedObjects);
    for (const auto& depId : dependencies) {
        UNIT_ASSERT_C(!categoriesGuard.count(depId), "Cycle is found: category " << depId << " appears twice");
        checkCycles(depId, categoriesGuard, dataSource);
    }

    categoriesGuard.erase(categoryId);
}

} // namespace

Y_UNIT_TEST_SUITE_F(changed_objects, DbFixture) {

Y_UNIT_TEST(test_wrong_load_call)
{
    const DBID headCommitId = loadJson(*revisionPgPool(), dataPath("hydro.data.json"));

    ThreadPool threadPool(THREAD_COUNT);

    DataSource dataSource(
        validatorConfig,
        CheckCardinality::No,
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        headCommitId, // head commit id
        AreaOfInterest(),
        {});

    UNIT_ASSERT(!dataSource.hasSelectedObjectIds());

    UNIT_CHECK_GENERATED_EXCEPTION(
        dataSource.load(categories::HYDRO::id(), LoaderType::LoadFromAoi, threadPool),
        maps::LogicError);

    for (const auto& categoryId : HYDRO_CATEGORIES) {
        UNIT_ASSERT_VALUES_EQUAL(dataSource.collection(categoryId).objectIds().size(), 0);
    }
}

Y_UNIT_TEST(test_loading_changed_objects)
{
    const DBID headCommitId = loadJson(*revisionPgPool(), dataPath("hydro.data.json"));

    ThreadPool threadPool(THREAD_COUNT);

    checkDataSource(validatorConfig, headCommitId, 10, categories::HYDRO::id(), threadPool);
    checkDataSource(validatorConfig, headCommitId, 11, categories::HYDRO_FC::id(), threadPool);
    checkDataSource(validatorConfig, headCommitId, 12, categories::HYDRO_FC_EL::id(), threadPool);
}

Y_UNIT_TEST(test_cycles_in_dependencies)
{
    DataSource dataSource(
        validatorConfig,
        CheckCardinality::No,
        *revisionPgPool(),
        rev::TRUNK_BRANCH_ID,
        0, //head commit id
        AreaOfInterest(),
        { });

    for (const auto& categoryId : allCategoryIds()) {
        std::set<TCategoryId> categoriesGuard;
        checkCycles(categoryId, categoriesGuard, dataSource);
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace tests
} // namespace validator
} // namespace wiki
} // namespace maps
