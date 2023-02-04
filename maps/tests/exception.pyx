# cython: c_string_type=unicode, c_string_encoding=ascii

from maps.garden.sdk.cython.exceptions cimport raisePyError

cdef extern from "<maps/garden/sdk/cython/tests/exception.h>":
    void raiseStdException() except +raisePyError
    void raiseMapsException() except +raisePyError
    void raiseYexception() except +raisePyError
    void raiseRestartableYexception() except +raisePyError
    void raiseTErrorResponseException() except +raisePyError
    void raiseRestartableTErrorResponseException() except +raisePyError
    void raiseAutotestsFailedError() except +raisePyError
    void raiseDataValidationWarning() except +raisePyError


def raise_std_exception():
    raiseStdException()


def raise_maps_exception():
    raiseMapsException()


def raise_yexception():
    raiseYexception()


def raise_restartable_yexception():
    raiseRestartableYexception()


def raise_error_response_exception():
    raiseTErrorResponseException()


def raise_restartable_error_response_exception():
    raiseRestartableTErrorResponseException()


def raise_autotests_failed_error():
    raiseAutotestsFailedError()


def raise_datavalidation_warning():
    raiseDataValidationWarning()
