import logging

from rest_framework.exceptions import NotFound, APIException
from rest_framework.status import HTTP_503_SERVICE_UNAVAILABLE

jafar_logger = logging.getLogger('jafar')


class PromoOfferNotFound(NotFound):
    default_detail = 'Could not find suitable offer'


class JafarException(APIException):
    default_detail = 'Jafar exception'
    status_code = HTTP_503_SERVICE_UNAVAILABLE

    def __init__(self, request=None, response=None):
        self.request = request
        self.response = response
        self.log()
        super(JafarException, self).__init__()

    def log(self):
        extra = {}
        if self.request:
            extra['jafar_request_body'] = self.request.body
            extra['jafar_url'] = self.request.url
        if self.response:
            extra['jafar_response'] = self.response.content
            extra['status_code'] = self.response.status_code
        jafar_logger.info(self.message, extra=extra)


class JafarHTTPException(JafarException):
    default_detail = 'Jafar HTTP error'


class JafarReadTimeout(JafarException):
    default_detail = 'Jafar read timeout'


class JafarConnectionError(JafarException):
    default_detail = 'Jafar connection error'


class NoRecommendationException(NotFound):
    default_detail = 'No recommendations found'


class ClientSaveRaceException(APIException):
    status_code = HTTP_503_SERVICE_UNAVAILABLE
    default_detail = 'Race condition on client save'
