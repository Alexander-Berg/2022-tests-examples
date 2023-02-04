# -*- coding: utf-8 -*-

import traceback
import six


class StartrekError(Exception):
    """Base class for all Startrek client errors."""


class StartrekClientError(StartrekError):
    """Base class for exceptions on client side."""


@six.python_2_unicode_compatible
class UnencodableValue(StartrekClientError, ValueError):
    """Failure to put a value into a json-envelope."""

    value = None

    def __init__(self, value):
        self.value = value
        super(UnencodableValue, self).__init__(value)

    def __str__(self):
        return "Unable to json-encode value {value!r} of type {type!r}".format(
            value=self.value,
            type=type(self.value),
        )


@six.python_2_unicode_compatible
class StartrekRequestError(StartrekError, IOError):
    """Connection failure below HTTP layer."""

    original_error = None

    def __init__(self, original_error):
        self.original_error = original_error
        super(StartrekRequestError, self).__init__(original_error)

    def __str__(self):
        lines = ["Failed to communicate with server:"]
        lines.extend(
            line.rstrip('\n')
            for line in traceback.format_exception_only(
                type(self.original_error),
                self.original_error,
            )
        )
        return "\n".join(lines)


@six.python_2_unicode_compatible
class StartrekServerError(StartrekError, IOError):
    """Base class for error conditions on server side."""

    response = None
    status_code = None
    reason = None
    errors = None
    error_messages = None

    def __init__(self, response):
        self.response = response
        self.status_code = response.status_code
        self.reason = response.reason
        try:
            error = response.json()
        except Exception:
            pass
        else:
            self.errors = error.get('errors')
            self.error_messages = error.get('errorMessages')
        super(StartrekServerError, self).__init__(response)

    def __str__(self):
        lines = []
        if self.__class__ is StartrekServerError:
            # Include informational header for unrecognized exceptions
            # In presence a subclass name, it is superfluous.
            lines.append("({}) {}".format(self.status_code, self.reason))
        if self.errors:
            lines.extend(
                "- {}: {}".format(key, message)
                for key, message in sorted(self.errors.items())
            )
        if self.error_messages:
            lines.extend(self.error_messages)
        return "\n".join(lines)


class OutOfRetries(StartrekServerError):

    def __str__(self):
        return "\n".join((
            "Out of retries, last error was:",
            super(OutOfRetries, self).__str__()
        ))


class BadRequest(StartrekServerError):
    pass


class Forbidden(StartrekServerError):
    pass


class NotFound(StartrekServerError):
    pass


class PreconditionFailed(StartrekServerError):
    pass


class UnprocessableEntity(StartrekServerError):
    pass


class PreconditionRequired(StartrekServerError):
    pass


STATUS_CODES = {
    400: BadRequest,
    403: Forbidden,
    404: NotFound,
    412: PreconditionFailed,
    422: UnprocessableEntity,
    428: PreconditionRequired,
}
