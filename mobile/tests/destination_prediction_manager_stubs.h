#pragma once

#include <yandex/maps/navikit/providers/bookmarks/bookmarks_provider.h>
#include <yandex/maps/navikit/providers/places/places_provider.h>
#include <yandex/maps/navikit/ride_history/ride_type_provider.h>

#include <yandex/maps/mapkit/search/search_manager.h>

#include <yandex/maps/runtime/async/future.h>

#include <thread>

namespace yandex::maps {

namespace mapkit::search {

class SessionStub : public Session {
public:
    SessionStub(runtime::async::Handle workHandle) : workHandle_(std::move(workHandle)) { }

    virtual void cancel() override { }
    virtual void retry(
        const OnSearchResponse& /* onSearchResponse */,
        const OnSearchError& /* onSearchError */) override
    {
    }

    virtual bool hasNextPage() const override { return { }; }
    virtual void fetchNextPage(
        const OnSearchResponse& /* onSearchResponse */,
        const OnSearchError& /* onSearchError */) override
    {
    }

    virtual void setFilters(
        const std::shared_ptr<runtime::bindings::SharedVector<BusinessFilter>>& /* filters */) override
    {
    }

    virtual void setSortByDistance(const mapkit::geometry::Geometry& /* origin */) override { }
    virtual void resetSort() override { }

    virtual void setSearchArea(const mapkit::geometry::Geometry& /* area */) override { }
    virtual void setSearchOptions(const SearchOptions& /* searchOptions */) override { }

    virtual void resubmit(
        const OnSearchResponse& /* onSearchResponse */,
        const OnSearchError& /* onSearchError */) override
    {
    }

private:
    runtime::async::Handle workHandle_;
};

class SearchManagerStub : public SearchManager {
public:
    SearchManagerStub(runtime::TimeInterval workDuration) : workDuration_(workDuration) { }

    virtual std::unique_ptr<Session> submit(
        const std::string& /* text */,
        const geometry::Geometry& /* geometry */,
        const SearchOptions& /* searchOptions */,
        const Session::OnSearchResponse& /* onSearchResponse */,
        const Session::OnSearchError& /* onSearchError */) override
    {
        return { };
    }

    virtual std::unique_ptr<Session> submit(
        const std::string& /* text */,
        const std::shared_ptr<geometry::Polyline>& /* polyline */,
        const geometry::Geometry& /* geometry */,
        const SearchOptions& /* searchOptions */,
        const Session::OnSearchResponse& /* onSearchResponse */,
        const Session::OnSearchError& /* onSearchError */) override
    {
        return { };
    }

    virtual std::unique_ptr<Session> submit(
        const geometry::Point& /* point */,
        const boost::optional<int>& /* zoom */,
        const SearchOptions& /* searchOptions */,
        const Session::OnSearchResponse& /* onSearchResponse */,
        const Session::OnSearchError& onSearchError) override
    {
        return std::make_unique<SessionStub>(runtime::async::global()->spawn([=] {
            runtime::async::sleepFor(workDuration_);

            runtime::async::ui()->spawn([=] {
                onSearchError({ });
            }).wait();
        }));
    }

    virtual std::unique_ptr<Session> resolveURI(
        const std::string& /* uri */,
        const SearchOptions& /* searchOptions */,
        const Session::OnSearchResponse& /* onSearchResponse */,
        const Session::OnSearchError& /* onSearchError */) override
    {
        return { };
    }

    virtual std::unique_ptr<Session> searchByURI(
        const std::string& /* uri */,
        const SearchOptions& /* searchOptions */,
        const Session::OnSearchResponse& /* onSearchResponse */,
        const Session::OnSearchError& /* onSearchError */) override
    {
        return { };
    }

    virtual std::unique_ptr<GoodsRegisterSession> requestGoodsRegister(
        const std::string& /* uri */,
        const GoodsRegisterSession::OnGoodsRegisterResponse& /* onGoodsRegisterResponse */,
        const GoodsRegisterSession::OnGoodsRegisterError& /* onGoodsRegisterError */) override
    {
        return { };
    }

    virtual std::unique_ptr<BookingSearchSession> findBookingOffers(
        const std::string& /* uri */,
        const boost::optional<BookingRequestParams>& /* params */,
        const BookingSearchSession::OnBookingSearchResponse& /* onBookingSearchResponse */,
        const BookingSearchSession::OnBookingSearchError& /* onBookingSearchError */) override
    {
        return { };
    }

    virtual std::unique_ptr<SuggestSession> createSuggestSession() override { return { }; }

private:
    runtime::TimeInterval workDuration_;
};

} // namespace mapkit::search

namespace navikit {

namespace providers::bookmarks {

class BookmarksProviderStub : public BookmarksProvider {
public:
    virtual std::shared_ptr<runtime::bindings::SharedVector<bookmarks::BookmarksCollection>>
        bookmarksCollections() const override
    {
        return std::make_shared<
            runtime::bindings::SharedVector<bookmarks::BookmarksCollection>>();
    }
};

}

namespace providers::places {

class PlacesProviderStub : public PlacesProvider {
public:
    virtual boost::optional<PlaceInfo> homeInfo() const override { return { }; }
    virtual boost::optional<PlaceInfo> workInfo() const override { return { }; }
};

}

namespace ride_history {

class RideTypeProviderStub : public RideTypeProvider {
public:
    virtual RideType currentRideType() const override { return { }; }
};

} // namespace ride_history

} // namespace navikit

} // namespace yandex::maps
