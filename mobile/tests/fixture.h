#pragma once

#include <yandex/maps/navikit/mocks/mock_route.h>

#include <yandex/maps/mapkit/directions/driving/route.h>
#include <yandex/maps/mapkit/geometry/ostream_helpers.h>
#include <yandex/maps/mapkit/geometry/tools.h>

using namespace yandex::maps;
using namespace yandex::maps::mapkit;
using namespace yandex::maps::mapkit::directions::driving;
using namespace yandex::maps::mapkit::geometry;

class MyRoute: public MockRoute {
public:
    MyRoute(
        std::vector<Point> points,
        std::vector<Section> sections,
        std::vector<JamSegment> jams,
        std::vector<StandingSegment> standings)
     : points_(std::make_shared<geometry::Polyline>(std::move(points)))
     , sections_(std::make_shared<runtime::bindings::SharedVector<Section>>(std::move(sections)))
     , jams_(std::make_shared<runtime::bindings::Vector<JamSegment>>(std::move(jams)))
     , standings_(std::make_shared<runtime::bindings::Vector<StandingSegment>>(std::move(standings)))
    {}

    virtual const std::shared_ptr<runtime::bindings::SharedVector<Section>>& sections() const override
    {return sections_;}

    virtual const std::shared_ptr<geometry::Polyline>& geometry() const override
    {return points_;}

    virtual const std::shared_ptr<runtime::bindings::Vector<JamSegment>>& jamSegments() const override
    {return jams_;}

    virtual const std::shared_ptr<runtime::bindings::SharedVector<Event>>& events() const override
    {return events_;}

    virtual const std::shared_ptr<runtime::bindings::Vector<StandingSegment>>& standingSegments() const override
    {return standings_;}

    virtual const geometry::PolylinePosition& position() const override
    {return position_;}

    virtual void setPosition(const geometry::PolylinePosition& position) override
    {position_ = position;}

    void setEvents(const std::vector<Event>& events)
    {
        events_ = std::make_shared<runtime::bindings::SharedVector<Event>>(events);
    }

private:
    std::shared_ptr<geometry::Polyline> points_;
    std::shared_ptr<runtime::bindings::SharedVector<Section>> sections_;
    std::shared_ptr<runtime::bindings::Vector<JamSegment>> jams_;
    std::shared_ptr<runtime::bindings::SharedVector<Event>> events_;
    std::shared_ptr<runtime::bindings::Vector<StandingSegment>> standings_;

    geometry::PolylinePosition position_;
};

class Fixture {
public:
    std::unique_ptr<MyRoute> makeRoute(
        std::vector<Point> points,
        std::vector<Section> sections,
        std::vector<JamSegment> jams,
        std::vector<StandingSegment> standings)
    {
        return std::make_unique<MyRoute>(
            std::move(points), std::move(sections), std::move(jams), std::move(standings));
    }

    Section makeSection(const std::string& streetName, const Subpolyline& geometry)
    {
        SectionMetadata meta;
        meta.annotation->toponym = streetName;

        return Section {
            std::move(meta),
            geometry
        };
    }
};
