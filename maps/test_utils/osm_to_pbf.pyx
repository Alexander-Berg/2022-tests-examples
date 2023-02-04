# cython: c_string_type=unicode, c_string_encoding=ascii
# distutils: language = c++

from libcpp.string cimport string


cdef extern from "maps/garden/libs/osm/test_utils/osm_to_pbf.h" namespace "maps::garden::modules::osm_to_yt::test":
    void convertOsmToPbf(
        const string& inputFile,
        const string& outputFile
    )


def convert_osm_to_pbf(
    input_file: str,
    output_file: str
):
    convertOsmToPbf(input_file, output_file)
