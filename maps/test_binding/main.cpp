#include <maps/pylibs/boost_python_utils/include/converters.h>

#include <boost/python.hpp>

#include <initializer_list>
#include <set>
#include <vector>

namespace maps::pylibs::boost_python_utils::test_binding {

template<template<typename... Args> typename Container, typename T>
Container<T> makeContainer(std::initializer_list<T> values)
{
    return Container<T>(values);
}

auto getIntVec() {
    return makeContainer<std::vector, int>({1, 2, 3});
}

auto getStringVec() {
    return makeContainer<std::vector, std::string>({"a", "bc"});
}

auto getIntVecVec() {
    return makeContainer<std::vector, std::vector<int>>({{1, 2, 3}});
}

#if PY_MAJOR_VERSION >= 3
auto getBytes() {
    return Bytes({97, 98, 99});
}

auto setBytes(const Bytes& bytes) {
    return bytes[0] == 97 && bytes[1] == 98 && bytes[2] == 99;
}
#endif

BOOST_PYTHON_MODULE(test_binding)
{
    using namespace boost::python;

    registerConverter<std::vector<int>>();
    // test that repetitive call will not cause any problems
    registerConverter<std::vector<int>>();
    registerConverter<std::vector<std::string>>();

    registerConverter<std::vector<std::vector<int>>>();

    boost::python::def("get_int_vec", &getIntVec);
    boost::python::def("get_string_vec", &getStringVec);
    boost::python::def("get_int_vec_vec", &getIntVecVec);

#if PY_MAJOR_VERSION >= 3
    registerBytesConverter();

    boost::python::def("get_bytes", &getBytes);
    boost::python::def("set_bytes", &setBytes);
#endif
}

} // namespace maps::pylibs::boost_python_utils::test_binding
