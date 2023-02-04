#pragma once

#include "../test_tools/test_suite.h"
#include "../test_types/save_edge_test_data.h"
#include "../test_types/move_node_test_data.h"
#include "../test_types/save_objects_test_data.h"
#include "../test_types/snap_nodes_test_data.h"
#include "../test_types/merge_edges_test_data.h"
#include "../test_types/face_validation_test_data.h"

#include <maps/libs/common/include/exception.h>

#include <memory>
#include <map>
#include <set>
#include <string>

namespace maps {
namespace wiki {
namespace topo {
namespace test {

typedef TestSuitesHolder<
        SaveEdgeTestData,
        MoveNodeTestData,
        SaveObjectsTestData,
        SnapNodesTestData,
        MergeEdgesTestData,
        FaceValidationTestData>
    MainTestSuite;

MainTestSuite* mainTestSuite();

#define TEST_SUITE_START( suite_name, test_type )\
namespace ns_##suite_name {\
\
typedef TestSuite< test_type > Suite_##suite_name;\
typedef test_type value_type;\
Suite_##suite_name* suite() \
{\
    static Suite_##suite_name suite_##suite_name_( #suite_name ); \
    return &suite_##suite_name_; \
}\

#define TEST_DATA( test_name )\
struct test_name {\
    static const value_type* createTest();\
    test_name(value_type t) { suite()->add( #test_name , std::move(t)); }\
};\
static test_name testRegistrar_##test_name = value_type\

#define TEST_SUITE_END( suite_name )\
}\
struct suiteRegistrar_##suite_name \
{\
    suiteRegistrar_##suite_name() { mainTestSuite()->add(ns_##suite_name::suite()); }\
};\
static suiteRegistrar_##suite_name suiteRegistrarInst_##suite_name;\

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
