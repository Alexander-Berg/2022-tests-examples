# cython: c_string_type=unicode, c_string_encoding=ascii
# distutils: language = c++

from libcpp.string cimport string

from maps.garden.sdk.cython.exceptions cimport raisePyError


cdef extern from "maps/garden/modules/osm_borders_src/tests/coverage_check.h" namespace "maps::garden::modules::osm_borders_src::tests" nogil:
    string getRegionMetaFromCoverage(
        double lon,
        double lat,
        const string& coverageFileName,
        const string& layerName
    ) except +raisePyError


def meta_of_point_in_coverage(lon: double, lat: double, coverage_file_name: string, layer_name: string) -> str:
    cdef string meta
    with nogil:
        meta = getRegionMetaFromCoverage(lon, lat, coverage_file_name, layer_name)

    return meta
