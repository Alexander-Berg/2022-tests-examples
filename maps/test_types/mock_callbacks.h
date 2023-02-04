#pragma once

#include "mock_storage.h"
#include "../util/io.h"
#include "../events_data.h"

#include <yandex/maps/wiki/topo/editor.h>
#include <yandex/maps/wiki/topo/cache.h>
#include <yandex/maps/wiki/topo/storage.h>

#include <boost/test/test_tools.hpp>

#include <set>
#include <list>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

template <class EventID, class EventT> EventID makeId(const EventT& event);

template <class EventT, class EventID, class EventDataT>
class MockCallbackBase
{
public:
    typedef EventT Event;
    typedef EventDataT EventData;
    typedef std::list<EventDataT> EventsDataList;
    typedef std::list<EventID> EventIdsList;

    explicit MockCallbackBase(
            const EventsDataList& expectedEventsData)
        : expectedEventsData_(expectedEventsData)
    {}

    const EventIdsList& unprocessedEventIds() const { return unprocessedEventIds_; }

protected:
    void removeUnusedEvent(const EventT& event) const
    {
        auto it = std::remove_if(
            unprocessedEventIds_.begin(), unprocessedEventIds_.end(),
            [&] (const EventID& id) { return id == makeId<EventID, EventT>(event); });
        unprocessedEventIds_.erase(it, unprocessedEventIds_.end());
    }

    template <class RequestedEventT>
    typename EventsDataList::const_iterator cfind(const RequestedEventT& event) const
    {
        const EventID id = makeId<EventID, RequestedEventT>(event);
        auto idPred = [id] (const EventDataT& eventData)
        {
            return makeId<EventID, EventDataT>(eventData) == id;
        };
        auto it = std::find_if(
            expectedEventsData_.begin(), expectedEventsData_.end(), idPred);
        BOOST_REQUIRE_MESSAGE(
            it != expectedEventsData_.end(),
            "Event data not found for topo event, id: " << util::print(id)
        );
        return it;
    }

    template <class RequestedEventT>
    typename EventsDataList::iterator find(const RequestedEventT& event) const
    {
        const EventID id = makeId<EventID, RequestedEventT>(event);
        auto idPred = [id] (const EventDataT& eventData)
        {
            return makeId<EventID, EventDataT>(eventData) == id;
        };
        auto it = std::find_if(
            expectedEventsData_.begin(), expectedEventsData_.end(), idPred);
        BOOST_REQUIRE_MESSAGE(
            it != expectedEventsData_.end(),
            "Event data not found for topo event, id: " << util::print(id)
        );
        return it;
    }

    void processImpl(const EventT& /*event*/) const;

    mutable EventsDataList expectedEventsData_;
    mutable EventIdsList unprocessedEventIds_;
};

template <class EventT, class EventID, class EventDataT>
class MockTopoCallback
    : public topo::TopoCallback<EventT>
    , public MockCallbackBase<EventT, EventID, EventDataT>
{
public:
    typedef EventT Event;
    typedef EventDataT EventData;
    typedef typename MockCallbackBase<EventT, EventID, EventDataT>::EventsDataList EventsDataList;
    typedef typename MockCallbackBase<EventT, EventID, EventDataT>::EventIdsList EventIdsList;

    explicit MockTopoCallback(
            const EventsDataList& expectedEventsData)
        : MockCallbackBase<EventT, EventID, EventDataT>(expectedEventsData)
    {}

    virtual void process(const Event& event) const
    {
        processImpl(event);
        this->removeUnusedEvent(event);
    }

private:

    void processImpl(const Event& /*event*/) const;
};

template <class RequestT, class EventT, class EventID, class EventDataT>
class MockCallback
    : public topo::Callback<RequestT, EventT>
    , public MockCallbackBase<EventT, EventID, EventDataT>
{
public:
    typedef RequestT Request;
    typedef EventT Event;
    typedef EventDataT EventData;
    typedef typename MockCallbackBase<EventT, EventID, EventDataT>::EventsDataList EventsDataList;
    typedef typename MockCallbackBase<EventT, EventID, EventDataT>::EventIdsList EventIdsList;

    explicit MockCallback(
            const EventsDataList& expectedEventsData)
        : MockCallbackBase<EventT, EventID, EventDataT>(expectedEventsData)
    {}

    virtual void processRequest(Request& request) const
    {
        processRequestImpl(request);
        removeProcessedRequest(request);
    }

    virtual void processEvent(const Event& event) const
    {
        processEventImpl(event);
        this->removeUnusedEvent(event);
    }

    const EventIdsList& unprocessedRequestIds() const { return unprocessedRequestIds_; }

private:
    void removeProcessedRequest(Request& request) const
    {
        auto pred =
            [&] (const EventID& id) { return id == makeId<EventID, Request>(request); };
        auto it = std::remove_if(
            unprocessedRequestIds_.begin(), unprocessedRequestIds_.end(), pred);
        unprocessedRequestIds_.erase(it, unprocessedRequestIds_.end());
    }

    void processRequestImpl(Request& /*request*/) const;
    void processEventImpl(const Event& /*event*/) const;

    mutable EventIdsList unprocessedRequestIds_;
};

typedef
    MockTopoCallback<
        topo::AddEdgeEvent, EdgeID, topo::AddEdgeEventData>
    MockTopoAddEdgeCallback;

typedef
    MockTopoCallback<
        topo::MoveEdgeEvent, EdgeID, topo::MoveEdgeEventData>
    MockTopoMoveEdgeCallback;

typedef
    MockTopoCallback<
        topo::DeleteEdgeEvent, EdgeID, topo::DeleteEdgeEventData>
    MockTopoDeleteEdgeCallback;


typedef
    MockCallback<
        topo::SplitEdgeRequest, topo::SplitEdgeEvent, EdgeID, topo::SplitEdgeEventData>
    MockSplitEdgeCallback;

typedef
    MockCallback<
        topo::MergeEdgesRequest, topo::MergeEdgesEvent, NodeID, topo::MergeEdgesEventData>
    MockMergeEdgesCallback;

typedef
    MockCallback<
        topo::DeleteEdgeRequest, topo::DeleteEdgeEvent, EdgeID, topo::DeleteEdgeEventData>
    MockDeleteEdgeCallback;

typedef
    MockCallback<
        topo::MergeNodesRequest,
        topo::MergeNodesEvent,
        std::pair<NodeID, NodeID>,
        topo::MergeNodesEventData>
    MockMergeNodesCallback;

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
