# cython: c_string_type=unicode, c_string_encoding=utf-8

from libcpp.string cimport string
from libcpp.vector cimport vector


cdef extern from "maps/routing/matrix_router/data_preparation/pytests/test_clustering/wrapper.h" namespace "maps::routing::matrix_router":
    vector[vector[float]] prepareSlicesWrapper(const string&, const string&, float, size_t, double, double) except+


def prepare_slices_wrapper(
        routesTable,
        timePartsTable,
        defaultSpeed,
        maxTestRoutesSize,
        minRouteLength=2000,
        minRouteTravelTime=120):
    return prepareSlicesWrapper(
        routesTable,
        timePartsTable,
        defaultSpeed,
        maxTestRoutesSize,
        minRouteLength,
        minRouteTravelTime)
