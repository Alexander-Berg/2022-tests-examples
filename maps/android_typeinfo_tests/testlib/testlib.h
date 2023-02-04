#pragma once

#include <typeinfo>

class TestException{};

__attribute__ ((__visibility__("default"))) void throwTestException();

__attribute__ ((__visibility__("default"))) const std::type_info& testExceptionTypeInfo();

