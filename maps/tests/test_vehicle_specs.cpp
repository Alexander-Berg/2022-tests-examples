#include <maps/b2bgeo/libs/cost_matrices/common.h>
#include <maps/b2bgeo/libs/cost_matrices/vehicle_specs.h>

#include "maps/b2bgeo/libs/cost_matrices/proto/vehicle_classes.pb.h"

#include <library/cpp/testing/unittest/registar.h>

using namespace maps::b2bgeo::cost_matrices;

using ProtoClasses = maps::b2bgeo::cost_matrices::proto::VehicleClasses;

Y_UNIT_TEST_SUITE(test_vehicle_specs)
{
    Y_UNIT_TEST(roundtrip_does_not_change_data)
    {
        const VehicleClasses classes{
            {VehicleSpecs::create<RoutingMode::DRIVING>(), "same_class"},
            {VehicleSpecs::create<RoutingMode::WALKING>(), "same_class"},
            {VehicleSpecs::create<RoutingMode::TRANSIT>(), "another_class"},
            {VehicleSpecs::create<RoutingMode::TRUCK>(),
             "long enough class id with spaces"},
            {VehicleSpecs::create<RoutingMode::TRUCK>(
                VehicleSpecs::Truck{1.0}),
             ""},
            {VehicleSpecs::create<RoutingMode::TRUCK>(
                maps::b2bgeo::cost_matrices::VehicleSpecs::Truck{1.0, 2.0, 3.0, 4.0}),
             "all_specs_set"}.
            {VehicleSpecs::create<RoutingMode::BICYCLE>(), "bicycle_class_id"}};

        ProtoClasses proto;
        asProtoObject(classes, proto);

        const auto parsedClasses = vehicleClassesFromProtoObject(proto);
        UNIT_ASSERT_EQUAL(classes, parsedClasses);
    }

    Y_UNIT_TEST(empty_classes_are_processed_without_errors)
    {
        const VehicleClasses classes;

        ProtoClasses proto;
        asProtoObject(classes, proto);

        const auto parsedClasses = vehicleClassesFromProtoObject(proto);
        UNIT_ASSERT_EQUAL(classes, parsedClasses);
    }

    Y_UNIT_TEST(default_vehicle_specs_is_created_with_default_routing_mode)
    {
        UNIT_ASSERT_EQUAL(
            VehicleSpecs{},
            VehicleSpecs::create<DEFAULT_ROUTING_MODE>());
    }
}
