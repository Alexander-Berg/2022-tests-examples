#include "testlib.h"

__attribute__ ((__visibility__("default"))) void throwTestException() {
    throw TestException();
}

__attribute__ ((__visibility__("default"))) const std::type_info& testExceptionTypeInfo() {
    return typeid(TestException);
}
