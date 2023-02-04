import json
import random
import ylog
from django.conf import settings
from django.utils.deprecation import MiddlewareMixin
from os import urandom
from raven.contrib.django.raven_compat.models import client as raven_client
from time import sleep
from uuid import UUID

from yaphone.advisor.advisor.models.profile import Locale
from yaphone.advisor.common.tools import get_http_header
from yaphone.localization import LocalizationUser, UserSpecificConfig


# TODO: Use one from plus.utils.middleware when migrate to Arcadia
# noinspection DuplicatedCode
def add_log_context(**kwargs):
    raven_client.tags_context(kwargs)
    for key, value in kwargs.items():
        ylog.context.put_to_context(key, value)


# TODO: Use one from plus.utils.middleware when migrate to Arcadia
class FixEmptyHostMiddleWare(MiddlewareMixin):
    """
    Used to fix bug with response code 400 when binding to [::] and empty Host header.
    Django tries to use SERVER_NAME as host in django.http.request:HttpRequest.get_host()
    but "::" does not fit django.http.request:host_validation_re regular expression.

    This fix works with Django=1.8.4. Please review it when upgrading django.
    """

    @staticmethod
    def process_request(request):
        if 'HTTP_HOST' not in request.META and request.META.get('SERVER_NAME', '').startswith(':'):
            request.META['SERVER_NAME'] = 'localhost'


# TODO: Use one from plus.utils.middleware when migrate to Arcadia
# noinspection DuplicatedCode
class LoggingContextMiddleware:
    DEFAULT_LOGGING_HEADERS = {'request_id': 'X-Request-Id'}

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        logging_headers = self.DEFAULT_LOGGING_HEADERS.copy()
        additional_logging_headers = getattr(settings, 'ADDITIONAL_LOGGING_HEADERS', None)
        if additional_logging_headers:
            logging_headers.update(additional_logging_headers)
        logging_kwargs = {
            'path': request.path,
            'host': request.get_host()
        }
        for parameter, header in logging_headers.items():
            header_value = get_http_header(request, header)
            if header_value:
                logging_kwargs[parameter] = header_value
        add_log_context(**logging_kwargs)
        return self.get_response(request)


class DegradationMiddleware(MiddlewareMixin):
    """
    Used for server API degradation in testing. https://st.yandex-team.ru/ADVISOR-985
    """

    SLEEP_TIME = 30

    @staticmethod
    def get_degradation_mode(request):
        try:
            user = LocalizationUser(uuid=UUID(get_http_header(request, 'X_YAUUID')))

            accept_language = get_http_header(request, 'ACCEPT_LANGUAGE')
            if accept_language is not None:
                try:
                    locale = Locale(accept_language)
                    user.language = locale.language
                    user.country = locale.territory
                except Locale.WrongFormat:
                    pass
        except (ValueError, TypeError):
            return None

        user_config = UserSpecificConfig(user=user, project='launcher')
        return user_config.get_value('degradation', default_value=None, log_missing=False)

    def process_response(self, request, response):
        degradation_modes = {
            'binary_data': self.degradate_random_binary,
            'all_nulls': self.degradate_all_nulls,
            'empty_answer': self.degradate_empty,
            'sleep': self.degradate_sleep,
            'bad_status_code': self.degradate_status_code,
            'bad_length': self.degradate_content_length,
            'invalid_json': self.degradate_json,
        }

        mode = self.get_degradation_mode(request)
        if mode == 'random':
            mode = random.choice(degradation_modes.keys())
        if mode in degradation_modes:
            degradation_modes[mode](response)
            response['X-YaDegradation'] = mode
        return response

    @staticmethod
    def degradate_content_length(response):
        response['Content-Encoding'] = 'ololo'
        response['Content-Length'] = random.randint(1, 1024 * 1024 * 1024)

    @staticmethod
    def degradate_status_code(response):
        response.status_code = random.choice((500, 404, 400, 600, 999))

    # noinspection PyUnusedLocal
    @staticmethod
    def degradate_sleep(response):
        sleep(DegradationMiddleware.SLEEP_TIME)

    @staticmethod
    def degradate_empty(response):
        response.content = ''

    @staticmethod
    def degradate_random_binary(response):
        response.content = urandom(50 * 1024 * 1024)

    @staticmethod
    def degradate_all_nulls(response):
        def replace_nulls(container):
            if isinstance(container, dict):
                return {k: replace_nulls(v) for k, v in container.iteritems()}
            elif isinstance(container, list):
                return [replace_nulls(v) for v in container]
            else:
                return None

        json_content = json.loads(response.content)
        response.content = json.dumps(replace_nulls(json_content))

    @staticmethod
    def degradate_json(response):
        response.content = response.content.replace("}", "").replace('"', "'")
