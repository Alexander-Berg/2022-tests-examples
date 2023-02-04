# cython: c_string_type=unicode, c_string_encoding=utf-8

from libcpp cimport bool
from libcpp.pair cimport pair
from libcpp.string cimport string

from maps.garden.sdk.cython.exceptions cimport raisePyError


cdef extern from "<maps/garden/modules/masstransit_tester/lib/validate.h>" namespace "maps::garden::modules::masstransit_tester":
    cdef cppclass MasstransitDataValidationConfig:
        double stopsMaxRelativeDifference;
        double routesMaxRelativeDifference;
        double threadsMaxRelativeDifference;


    cdef pair[bool, string] validateData(
        const string& masstransitDataToValidate,
        const string& masstransitDataGroundTruth,
        const MasstransitDataValidationConfig& validationConfig) except +raisePyError;


def validate_data(to_validate, ground_truth, validation_config):
    cdef MasstransitDataValidationConfig validationConfig
    validationConfig.stopsMaxRelativeDifference = validation_config.stops_max_relative_difference
    validationConfig.routesMaxRelativeDifference = validation_config.routes_max_relative_difference
    validationConfig.threadsMaxRelativeDifference = validation_config.threads_max_relative_difference

    return validateData(to_validate, ground_truth, validationConfig)
