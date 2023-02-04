#pragma once

#include <maps/mobile/libs/directions/driving/remainders.h>

#include <yandex/maps/mapkit/directions/driving/route.h>
#include <yandex/maps/mapkit/directions/driving/vehicle_options.h>
#include <yandex/maps/runtime/exception.h>
#include <yandex/maps/runtime/assert.h>
#include <yandex/maps/mapkit/geometry/tools.h>

namespace yandex::maps::mapkit {

namespace unit_test {

class NotImplemented: public runtime::LogicError {
public:
    NotImplemented(const std::string& methodName,
            const std::string& className=std::string()):
        runtime::LogicError(makeWhat(methodName, className))
    {}

private:
    static std::string makeWhat(const std::string& methodName,
        const std::string& className)
    {
        if (className.empty()) {
            return std::string("Method '") + methodName +
                "' is not implemented";
        }
        return std::string("Method '") + methodName + "' in class '" +
            className + "' is not implemented";
    }
};

class NotMocked: public runtime::LogicError {
public:
    NotMocked(const std::string& methodName,
            const std::string& className=std::string()):
        runtime::LogicError(makeWhat(methodName, className))
    {}

private:
    static std::string makeWhat(const std::string& methodName,
        const std::string& className)
    {
        if (className.empty()) {
            return std::string("Data for method '") + methodName +
                "' is not mocked";
        }
        return std::string("Data for method '") + methodName +
            "' in class '" + className + "' is not mocked";
    }
};

}

namespace directions::driving {
namespace unit_test {

class MockRoute : public Route {
public:
    MockRoute()
    {}

    /** Create simplest route with given geomtery and one section */
    explicit MockRoute(
            const geometry::Polyline& geometry,
            bool builtOffline = false,
            bool predicted = false):
        sections_(
            std::make_shared<runtime::bindings::SharedVector<Section>>()),
        geometry_(std::make_shared<geometry::Polyline>(geometry))
    {
        REQUIRE(
            geometry.points->size() > 1,
            "Can't mock route with empty geometry");
        const auto lastSegmentIndex =
            static_cast<unsigned>(geometry.points->size() - 2);
        sections_->push_back({{}, {{0, 0}, {lastSegmentIndex, 1.0}}});
        speedLimits_ = std::make_shared<runtime::bindings::Vector<boost::optional<float>>>(
                geometry.points->size() - 1, boost::none);
        tollRoads_ = std::make_shared<runtime::bindings::Vector<TollRoad>>();
        metadata_->flags.builtOffline = builtOffline;
        metadata_->flags.predicted = predicted;
        standingSegments_ = std::make_shared<runtime::bindings::Vector<StandingSegment>>();
        wayPoints_ = std::make_shared<runtime::bindings::Vector<geometry::PolylinePosition>>();
        wayPoints_->push_back({0, 0});
        wayPoints_->push_back(geometry::polylineEnd(geometry));
        remainders_ = std::make_shared<RemaindersImpl>(geometry, Weight());
    }

    std::shared_ptr<mapkit::navigation::Remainders> remainders() const
    {
        return remainders_;
    };

    virtual bool areConditionsOutdated() const override
    {
        return false;
    }

    virtual void addConditionsListener(
            const std::shared_ptr<ConditionsListener>&) override
    {
        // empty implementation is OK since conditions are never updated
    }

    virtual void removeConditionsListener(
            const std::shared_ptr<ConditionsListener>&) override
    {
        // empty implementation is OK since conditions are never updated
    }

    virtual unsigned int sectionIndex(unsigned int /*segmentIndex*/) const override
    {
        throw NotImplemented("sectionIndex", "RouteMock");
    }

    virtual const std::string& routeId() const override { return routeId_; }

    virtual const std::shared_ptr<RouteMetadata>& metadata() const override
    {
        return metadata_;
    }

    virtual const std::shared_ptr<
            runtime::bindings::SharedVector<Section>>& sections() const override
    {
        if (sections_)
            return sections_;
        throw NotMocked("sections", "RouteMock");
    }

    virtual const std::shared_ptr<geometry::Polyline>& geometry() const override
    {
        if (geometry_)
            return geometry_;
        throw NotMocked("geometry", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<JamSegment>>& jamSegments() const override
    {
        if (jamSegments_)
            return jamSegments_;
        throw NotMocked("jamSegments", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::SharedVector<Event>>& events() const override
    {
        if (events_)
            return events_;
        throw NotMocked("events", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<boost::optional<float>>>& speedLimits() const override
    {
        return speedLimits_;
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<AnnotationSchemeID>>& annotationSchemes() const override
    {
        if (annotationSchemes_)
            return annotationSchemes_;
        throw NotMocked("annotationSchemes", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::SharedVector<LaneSign>>& laneSigns() const override
    {
        if (laneSigns_)
            return laneSigns_;
        throw NotMocked("laneSigns", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::SharedVector<DirectionSign>>& directionSigns() const override
    {
        if (directionSigns_)
            return directionSigns_;
        throw NotMocked("directionSigns", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<RestrictedEntry>>& restrictedEntries() const override
    {
        throw runtime::LogicError() << "MockRoute::restrictedEntries not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<TrafficLight>>& trafficLights() const override
    {
        throw runtime::LogicError() << "MockRoute::trafficLights not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<PedestrianCrossing>>& pedestrianCrossings() const override
    {
        throw runtime::LogicError() << "MockRoute::pedestrianCrossing not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<SpeedBump>>& speedBumps() const override
    {
        if (speedBumps_) {
            return speedBumps_;
        }
        throw NotMocked("speedBumps", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<RailwayCrossing>>& railwayCrossings() const override
    {
        if (railwayCrossings_) {
            return railwayCrossings_;
        }
        throw NotMocked("railwayCrossings", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<RuggedRoad>>& ruggedRoads() const override
    {
        throw runtime::LogicError() << "MockRoute::ruggedRoads not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<FordCrossing>>& fordCrossings() const override
    {
        throw runtime::LogicError() << "MockRoute::fordCrossings not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<TollRoad>>& tollRoads() const override
    {
        if (tollRoads_)
            return tollRoads_;
        throw NotMocked("tollRoads", "RouteMock");
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<RestrictedTurn>>& restrictedTurns() const override
    {
        throw runtime::LogicError() << "MockRoute::tollRoads not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<StandingSegment>>& standingSegments() const override
    {
        return standingSegments_;
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<RoadVehicleRestriction>>& roadVehicleRestrictions() const override
    {
        throw runtime::LogicError() << "MockRoute::roadVehicleRestrictions not implemented";
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<ManoeuvreVehicleRestriction>>& manoeuvreVehicleRestrictions() const override
    {
        throw runtime::LogicError() << "MockRoute::manoeuvreVehicleRestrictions not implemented";
    }

    virtual const boost::optional<annotations::AnnotationLanguage>& annotationLanguage() const override
    {
        return annotationLanguage_;
    }

    virtual const VehicleOptions& vehicleOptions() const override
    {
        return vehicleOptions_;
    }

    virtual const geometry::PolylinePosition& position() const override
    {
        return position_;
    }

    virtual void setPosition(const geometry::PolylinePosition& position) override
    {
        position_ = position;
    }

    virtual const std::shared_ptr<::yandex::maps::mapkit::navigation::RoutePosition> routePosition() const override
    {
        throw runtime::LogicError() << "MockRoute::routePosition not implemented";
    }

    virtual std::shared_ptr<RouteMetadata> metadataAt(
        const geometry::PolylinePosition&) const override
    {
        throw runtime::LogicError() << "MockRoute::metadataAt not implemented";
    }

    void setRouteId(const std::string& routeId) { routeId_ = routeId; }

    void setRequestPoints(std::shared_ptr<runtime::bindings::SharedVector<RequestPoint>> points)
    {
        requestPoints_ = std::move(points);
    }

    void setJamSegments(std::shared_ptr<runtime::bindings::Vector<JamSegment>> segments)
    {
        jamSegments_ = std::move(segments);
    }

    void setHasTolls(bool hasTolls)
    {
        ASSERT(metadata_);
        metadata_->flags.hasTolls = hasTolls;
    }

    virtual const std::shared_ptr<
        runtime::bindings::SharedVector<RequestPoint>>& requestPoints() const override
    {
        return requestPoints_;
    }

    virtual const std::shared_ptr<
        runtime::bindings::Vector<geometry::PolylinePosition>>& wayPoints() const override
    {
        if (!wayPoints_) {
            throw NotMocked("wayPoints", "RouteMock");
        }
        return wayPoints_;
    }

    void setLegIndex(unsigned int legIndex) override {
        legIndex_ = legIndex;
    }

    virtual void requestConditionsUpdate() override {}

    unsigned int legIndex() const override {
        return legIndex_;
    }

    std::shared_ptr<RouteMetadata> metadata_ = std::make_shared<RouteMetadata>();
    std::shared_ptr<runtime::bindings::SharedVector<Section>> sections_;
    std::shared_ptr<geometry::Polyline> geometry_;
    std::shared_ptr<runtime::bindings::Vector<JamSegment>> jamSegments_;
    std::shared_ptr<runtime::bindings::SharedVector<Event>> events_;
    boost::optional<annotations::AnnotationLanguage> annotationLanguage_;
    VehicleOptions vehicleOptions_;
    geometry::PolylinePosition position_;
    std::shared_ptr<runtime::bindings::Vector<boost::optional<float>>> speedLimits_;
    std::string routeId_ = "MockRouteId";
    std::shared_ptr<runtime::bindings::Vector<AnnotationSchemeID>> annotationSchemes_;
    std::shared_ptr<runtime::bindings::SharedVector<LaneSign>> laneSigns_;
    std::shared_ptr<runtime::bindings::SharedVector<DirectionSign>> directionSigns_;
    std::shared_ptr<runtime::bindings::SharedVector<RequestPoint>> requestPoints_;
    std::shared_ptr<runtime::bindings::Vector<geometry::PolylinePosition>> wayPoints_;
    unsigned int legIndex_ = 0;
    std::shared_ptr<runtime::bindings::Vector<TollRoad>> tollRoads_;
    std::shared_ptr<runtime::bindings::Vector<StandingSegment>> standingSegments_;
    std::shared_ptr<mapkit::navigation::Remainders> remainders_;

    std::shared_ptr<runtime::bindings::Vector<SpeedBump>> speedBumps_;
    std::shared_ptr<runtime::bindings::Vector<RailwayCrossing>> railwayCrossings_;

protected:
    typedef yandex::maps::mapkit::unit_test::NotImplemented NotImplemented;
    typedef yandex::maps::mapkit::unit_test::NotMocked NotMocked;
};

} // namespace unit_test
} // namespace directions::driving
} // namespace yandex::maps::mapkit
