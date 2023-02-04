# -*- coding: utf-8 -*-

from rest_framework import status
from rest_framework.exceptions import APIException, ParseError


class BadRequestAPIError(APIException):
    status_code = status.HTTP_400_BAD_REQUEST


class NoDeviceInfoAPIError(APIException):
    status_code = 418


class GatewayTimeoutAPIError(APIException):
    status_code = status.HTTP_504_GATEWAY_TIMEOUT
    default_detail = 'Gateway Timeout'


class KnownParseError(ParseError):
    """ Validation error that should not be logged """
    pass
