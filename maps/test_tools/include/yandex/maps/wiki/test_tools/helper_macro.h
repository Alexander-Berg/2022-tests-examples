#pragma once

#include <yandex/maps/wiki/test_tools/suite.h>

#define TEST_SUITE_START( suite_name, test_type )\
namespace ns_##suite_name {\
\
typedef maps::wiki::test_tools::TestSuite< test_type > Suite;\
typedef test_type value_type;\
static Suite* suite() \
{\
    static Suite suite_( #suite_name ); \
    return &suite_; \
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

