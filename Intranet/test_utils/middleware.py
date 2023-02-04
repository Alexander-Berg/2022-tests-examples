from django.contrib.auth.models import AnonymousUser
from django.http import HttpResponseRedirect
from django.utils.deprecation import MiddlewareMixin
from django_yauth.exceptions import TwoCookiesRequired
from django_yauth.user import AnonymousYandexUser, YandexUser
from tvm2.ticket import ServiceTicket, UserTicket


class TestAuthMiddleware(MiddlewareMixin):
    """
    Аутентификация для использования в тестах
    """

    def process_request(self, request):
        user = request.META.get('UNITTEST_USER', None)

        is_anonymous = user is None
        login = user and user.get_username()
        email = login and login + '@yandex-team.ru'

        tvm_client_id = request.META.get('UNITTEST_TVM_CLIENT', '123')
        mechanism_name = request.META.get('UNITTEST_MECHANISM_NAME', 'cookie')

        service_ticket = None
        user_ticket = None

        if mechanism_name == 'tvm':
            service_ticket = ServiceTicket(
                {
                    'src': tvm_client_id,
                    'dst': '123',
                    'debug_string': '',
                    'logging_string': '',
                    'scopes': [],
                }
            )

            user_ticket = UserTicket(
                {
                    'debug_string': '',
                    'logging_string': '',
                    'scopes': [],
                    'uids': [],
                    'default_uid': '',
                }
            )

        class FakeMechanism:
            def __init__(self):
                self.mechanism_name = mechanism_name

        if is_anonymous:
            request.user = AnonymousUser()
            request.yauser = AnonymousYandexUser()
        else:
            request.user = user
            request.yauser = YandexUser(
                uid='000000',
                is_lite=False,
                fields={},
                need_reset=False,
                emails=[email],
                default_email=email,
                mechanism=FakeMechanism(),
                service_ticket=service_ticket,
                user_ticket=user_ticket,
            )

            request.yauser.login = user.username

    def process_exception(self, request, exception):
        if isinstance(exception, TwoCookiesRequired):
            return HttpResponseRedirect('/')  # Редирект на паспорт, но тесты проверяют только код 302.
