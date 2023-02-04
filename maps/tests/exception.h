#pragma once

#include <maps/garden/sdk/cpp/exceptions.h>

#include <maps/libs/common/include/exception.h>
#include <mapreduce/yt/interface/errors.h>
#include <util/generic/bt_exception.h>
#include <util/generic/yexception.h>

#include <stdexcept>

void raiseStdException() {
    throw std::runtime_error("std_exception");
}

void raiseMapsException() {
    throw maps::RuntimeError("map_exception");
}

void raiseYexception() {
    ythrow NYT::TRequestRetriesTimeout() << "request retries timeout";
}

void raiseRestartableYexception() {
    ythrow TWithBackTrace<yexception>() << "yexception";
}

void raiseTErrorResponseException() {
    ythrow NYT::TErrorResponse(400, "") << "400 client error";
}

void raiseRestartableTErrorResponseException() {
    ythrow NYT::TErrorResponse(0, "") << "unknown error";
}

void raiseAutotestsFailedError() {
    throw maps::garden::AutotestsFailedError("Input data is invalid");
}

void raiseDataValidationWarning() {
    throw maps::garden::DataValidationWarning("datavalidation warning");
}
