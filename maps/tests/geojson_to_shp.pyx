# cython: c_string_type=unicode, c_string_encoding=ascii
# distutils: language = c++

from libcpp.string cimport string


cdef extern from "maps/garden/modules/osm_coastlines_src/tests/geojson_to_shp.h" namespace "maps::garden::modules::osm_coastlines_src::test":
    void convertGeoJsonToSHP(const string& inFilePath, const string& outFilePath) 


def convert_geojson_to_shp(in_file_path: str, out_file_path: str):
    convertGeoJsonToSHP(in_file_path, out_file_path)
