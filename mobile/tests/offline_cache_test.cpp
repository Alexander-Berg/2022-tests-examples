#include "../offline_cache/offline_cache_list_presenter_creator.h"

#include <yandex/maps/navikit/mocks/mock_offline_cache_manager.h>
#include <yandex/maps/navi/mocks/mock_list_view.h>

#include <yandex/maps/mapkit/offline_cache/region.h>

#include <yandex/maps/runtime/async/dispatcher.h>

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navi::ui::tests {

using namespace offline_cache;
using namespace mapkit::offline_cache;
using namespace testing;

namespace {

const std::string ROMAN_EMPIRE = "Римская империя";

class MyRegion : public Region {
public:
    MyRegion(const std::string& country, const std::string& name)
    {
        this->name = name;
        this->country = country;
    }
};

class MyOfflineCacheManager : public MockOfflineCacheManager {
public:
    void loadRegions() { regions_.push_back(MyRegion(ROMAN_EMPIRE, "Италия")); }

    virtual std::shared_ptr<runtime::bindings::Vector<Region>> regions() override
    {
        auto result = std::make_shared<runtime::bindings::Vector<Region>>();
        result->reserve(regions_.size());
        for (const auto& region : regions_)
            result->push_back(region);
        return result;
    }

private:
    std::vector<Region> regions_;
};

class OfflineCacheTestFixture {
protected:
    OfflineCacheTestFixture()
    {
        runtime::async::ui()->spawn([this] { setUp(); }).wait();
    }

    void setUp()
    {
        offlineCacheManager_ = std::make_unique<MyOfflineCacheManager>();
        EXPECT_CALL(*offlineCacheManager_, addRegionListener(_));
        EXPECT_CALL(*offlineCacheManager_, addRegionListUpdatesListener(_));

        offlineCacheManager_->loadRegions();
        data_ = createOfflineCacheData(offlineCacheManager_.get());
    }

    std::unique_ptr<MyOfflineCacheManager> offlineCacheManager_;
    std::shared_ptr<OfflineCacheData> data_;
};

}  // namespace

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Tests
//

class MyObject {
public:
    ~MyObject() { state_ = 0; }
    bool isValid() const { return state_ == 0x777; }

private:
    int state_ = 0x777;
};

BOOST_FIXTURE_TEST_SUITE(OfflineCacheTests, OfflineCacheTestFixture)

BOOST_AUTO_TEST_CASE(testCountryListPresenter)
{
    runtime::async::ui()
        ->spawn([this] {
            auto presenter = createOfflineCacheCountryListPresenter(
                data_.get(),
                offlineCacheManager_.get(),
                [this, obj = MyObject()](const Region* region) -> ItemFactory {
                    ASSERT(obj.isValid());
                    return createRegionItemFactory(
                        offlineCacheManager_.get(), region, nullptr, /* isClosest= */ true);
                },
                ROMAN_EMPIRE, /* completedOnly= */ false);

            auto view = std::make_shared<common::MockListView>();
            EXPECT_CALL(*view, updateItems());

            presenter->setView(view);
            for (int i = 0; i < presenter->itemCount(); ++i)
                presenter->createItem(i);
        })
        .wait();
}

BOOST_AUTO_TEST_SUITE_END()

}  // namespace yandex
