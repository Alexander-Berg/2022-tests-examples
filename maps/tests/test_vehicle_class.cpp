#include "maps/b2bgeo/libs/traffic_info/vehicle_class.h"

#include <maps/b2bgeo/libs/cost_matrices/common.h>
#include <maps/b2bgeo/libs/cost_matrices/vehicle_specs.h>

#include <maps/libs/json/include/value.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/unittest/registar.h>
#include <iterator>

using namespace maps::b2bgeo::traffic_info;
using namespace maps::b2bgeo::cost_matrices;

namespace {
const auto DEFAULT_CONFIG = VehicleClassesConfig::fromJson(
    maps::json::Value::fromString(NResource::Find("truck_classes.json")));
}

Y_UNIT_TEST_SUITE(test_vehicle_class)
{
    Y_UNIT_TEST(empty_input_is_processed_without_errors)
    {
        const auto vehicleToClass = mapVehiclesToClasses({}, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses({}, DEFAULT_CONFIG, false);

        UNIT_ASSERT(vehicleToClass.empty());
        UNIT_ASSERT(classes.empty());
    }

    Y_UNIT_TEST(general_truck_class_is_used_if_no_specs_given)
    {
        const auto specs = VehicleSpecs::create<RoutingMode::TRUCK>();
        const std::vector<VehicleSpecs> vehicles{specs, specs};
        const auto vehicleToClass = mapVehiclesToClasses(vehicles, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses(vehicleToClass, DEFAULT_CONFIG, false);

        const VehicleClasses expectedVehicleToClass{
            {specs, VehicleClass::TRUCK.id}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        const std::vector<VehicleClass> expectedClasses{VehicleClass::TRUCK};
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(specific_truck_class_is_used_if_specs_are_given)
    {
        const auto specs = VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{1.0});
        const std::vector<VehicleSpecs> vehicles{specs, specs};
        const auto vehicleToClass = mapVehiclesToClasses(vehicles, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses(vehicleToClass, DEFAULT_CONFIG, false);

        const VehicleClasses expectedVehicleToClass{{specs, "truck_N1"}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        const std::vector<VehicleClass> expectedClasses{
            DEFAULT_CONFIG.truckClasses.front()};
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(
        specific_truck_class_is_not_used_if_enable_vehicle_classes_is_false)
    {
        const auto specs = VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{1.0});
        const std::vector<VehicleSpecs> vehicles{specs, specs};
        const auto vehicleToClass = mapVehiclesToClasses(vehicles, DEFAULT_CONFIG, false);
        const auto classes = extractUsedClasses(vehicleToClass, DEFAULT_CONFIG, false);

        const VehicleClasses expectedVehicleToClass{
            {specs, VehicleClass::TRUCK.id}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        const std::vector<VehicleClass> expectedClasses{VehicleClass::TRUCK};
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(non_truck_vehicles_are_mapped_based_on_routing_modes)
    {
        const auto transitSpecs =
            VehicleSpecs::create<RoutingMode::TRANSIT>();
        const auto drivingSpecs =
            VehicleSpecs::create<RoutingMode::DRIVING>();
        const std::vector<VehicleSpecs> vehicles{transitSpecs, drivingSpecs};
        const auto vehicleToClass = mapVehiclesToClasses(vehicles, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses(vehicleToClass, DEFAULT_CONFIG, false);

        const VehicleClasses expectedVehicleToClass{
            {transitSpecs, VehicleClass::TRANSIT.id},
            {drivingSpecs, VehicleClass::DRIVING.id}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        const std::vector<VehicleClass> expectedClasses{
            VehicleClass::DRIVING, VehicleClass::TRANSIT};
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(classes_are_assigned_to_themselves)
    {
        std::vector<VehicleSpecs> vehicles{
            VehicleClass::WALKING.restrictions,
            VehicleClass::TRANSIT.restrictions,
            VehicleClass::DRIVING.restrictions,
            VehicleClass::BICYCLE.restrictions,
        };
        std::transform(
            std::next(DEFAULT_CONFIG.truckClasses.begin()), // remove first class, so that we are in limit
            DEFAULT_CONFIG.truckClasses.end(),
            std::back_inserter(vehicles),
            [](const auto& cls) { return cls.restrictions; });

        const auto vehicleToClass = mapVehiclesToClasses(vehicles, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses(vehicleToClass, DEFAULT_CONFIG, false);

        VehicleClasses expectedVehicleToClass{
            {VehicleClass::WALKING.restrictions, VehicleClass::WALKING.id},
            {VehicleClass::TRANSIT.restrictions, VehicleClass::TRANSIT.id},
            {VehicleClass::DRIVING.restrictions, VehicleClass::DRIVING.id},
            {VehicleClass::BICYCLE.restrictions, VehicleClass::BICYCLE.id},
        };
        std::transform(
            std::next(DEFAULT_CONFIG.truckClasses.begin()),
            DEFAULT_CONFIG.truckClasses.end(),
            std::inserter(expectedVehicleToClass, expectedVehicleToClass.end()),
            [](const auto& cls) { return std::make_pair(cls.restrictions, cls.id); });

        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        std::vector<VehicleClass> expectedClasses{
                VehicleClass::WALKING,
                VehicleClass::TRANSIT,
                VehicleClass::DRIVING,
                VehicleClass::BICYCLE};
        std::copy(
            std::next(DEFAULT_CONFIG.truckClasses.begin()),
            DEFAULT_CONFIG.truckClasses.end(),
            std::back_inserter(expectedClasses));
        std::reverse(expectedClasses.begin(), expectedClasses.end());
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(
        trucks_are_assigned_to_different_classes_depending_on_specs)
    {
        // height fits Truck N2, the rest fits Truck N1
        const auto first = VehicleSpecs::create<RoutingMode::TRUCK>(
            VehicleSpecs::Truck{3.0, 1.0, 1.0, 1.0});
        // Width fits Truck N2
        const auto second = VehicleSpecs::create<RoutingMode::TRUCK>(
            VehicleSpecs::Truck{{}, 2.3});
        // Empty specs fits minimal requirements (Truck N1)
        const auto third = VehicleSpecs::create<RoutingMode::TRUCK>();
        // Length fits Truck N2, maxWeight fits Truck N3
        const auto fourth = VehicleSpecs::create<RoutingMode::TRUCK>(
            VehicleSpecs::Truck{{}, {}, 6.5, 22.0});
        // Length doesn't fit any class (Truck N6 is largest available)
        const auto fifth = VehicleSpecs::create<RoutingMode::TRUCK>(
            VehicleSpecs::Truck{{}, {}, 1000.0});
        // First with 3.9 height is Truck N4
        const auto sixth = VehicleSpecs::create<RoutingMode::TRUCK>(
            VehicleSpecs::Truck{3.5});
        // Height fits Truck N4, length fits Truck N5
        const auto seventh = VehicleSpecs::create<RoutingMode::TRUCK>(
            VehicleSpecs::Truck{3.5, {}, 20.0});

        const std::vector<VehicleSpecs> vehicles{
            first,
            second,
            third,
            fourth,
            fifth,
            sixth,
            seventh};
        const auto vehicleToClass = mapVehiclesToClasses(vehicles, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses(vehicleToClass, DEFAULT_CONFIG, false);

        const VehicleClasses expectedVehicleToClass{
            {first, "truck_N2"},
            {second, "truck_N2"},
            {third, "truck_N1"},
            {fourth, "truck_N4"}, // truck_N3 is removed due to truck class limit
            {fifth, "truck_N6"},
            {sixth, "truck_N4"},
            {seventh, "truck_N5"},
        };
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        auto expectedClasses = DEFAULT_CONFIG.truckClasses;
        std::reverse(expectedClasses.begin(), expectedClasses.end());
        expectedClasses.erase(
            std::remove_if(
                expectedClasses.begin(),
                expectedClasses.end(),
                [](const auto& cls) { return cls.id == "truck_N3"; }),
            expectedClasses.end());
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(force_waliking_class)
    {
        const auto vehicleToClass = mapVehiclesToClasses({}, DEFAULT_CONFIG);
        const auto classes = extractUsedClasses({}, DEFAULT_CONFIG, true);

        UNIT_ASSERT(vehicleToClass.empty());

        const std::vector<VehicleClass> expectedClasses{
            VehicleClass::WALKING};
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(max_truck_class_is_selected_when_only_one_is_allowed)
    {
        const VehicleClassesConfig config{DEFAULT_CONFIG.truckClasses, 1};

        const auto first =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.0});
        const auto second =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{100});

        const auto vehicleToClass = mapVehiclesToClasses({first, second}, config);
        const auto classes = extractUsedClasses(vehicleToClass, config, false);

        const VehicleClasses expectedVehicleToClass{
            {first, "truck_N6"}, {second, "truck_N6"}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);

        const std::vector<VehicleClass> expectedClasses{DEFAULT_CONFIG.truckClasses.back()};
        UNIT_ASSERT_EQUAL(classes, expectedClasses);
    }

    Y_UNIT_TEST(trucks_are_evenly_distributed_between_suitable_classes)
    {
        const VehicleClassesConfig config{DEFAULT_CONFIG.truckClasses, 2};
        const auto first =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{2.5});
        const auto second =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.2});
        const auto third =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.4});
        const auto fourth =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.9});

        const auto vehicleToClass = mapVehiclesToClasses({first, second, third, fourth}, config);
        const VehicleClasses expectedVehicleToClass{
            {first, "truck_N2"}, {second, "truck_N2"}, {third, "truck_N4"}, {fourth, "truck_N4"}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);
    }


    Y_UNIT_TEST(trucks_are_not_limited_if_limit_is_zero)
    {
        const VehicleClassesConfig config{DEFAULT_CONFIG.truckClasses, 0};
        const auto first =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{2.5});
        const auto second =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.2});
        const auto third =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.4});
        const auto fourth =
            VehicleSpecs::create<RoutingMode::TRUCK>(VehicleSpecs::Truck{3.9});

        const auto vehicleToClass = mapVehiclesToClasses({first, second, third, fourth}, config);
        const VehicleClasses expectedVehicleToClass{
            {first, "truck_N1"}, {second, "truck_N2"}, {third, "truck_N3"}, {fourth, "truck_N4"}};
        UNIT_ASSERT_EQUAL(vehicleToClass, expectedVehicleToClass);
    }
}
