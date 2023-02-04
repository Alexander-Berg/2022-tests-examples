import requests


class ForbiddenError(requests.HTTPError):
    pass


class BadRequestError(requests.HTTPError):
    pass


class UnauthorizedError(requests.HTTPError):
    pass


class GoneError(requests.HTTPError):
    pass


class UnprocessableEntityError(requests.HTTPError):
    pass


class LockedError(requests.HTTPError):
    pass


class ServerError(requests.HTTPError):
    pass


class NotFoundError(requests.HTTPError):
    pass
