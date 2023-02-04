from django_yauth.user import YandexTestUser
from rest_framework.test import (
    APIClient as ClientBase,
    ForceAuthClientHandler as HandlerBase,
)


class ForceAuthClientHandler(HandlerBase):
    """
    Фейковая ya-авторизация в тестах.
    Используется при вызове force_authenticate у client.
    Первым аргументом в client.force_authenticate нужно прокинуть
    логин пользователя, от которого отправляется запрос.
    """
    def _force_authenticate(self, request):
        login = self._force_user
        if not login:
            return
        request.yauser = YandexTestUser(
            login=login,
            uid='123456',
            language='ru',
            default_email='{}@yandex-team.ru'.format(login),
            need_reset=False,
        )

    def get_response(self, request):
        self._force_authenticate(request)
        return super().get_response(request)


class APIClient(ClientBase):

    def __init__(self, enforce_csrf_checks=False, **defaults):
        super().__init__(enforce_csrf_checks, **defaults)
        self.handler = ForceAuthClientHandler(enforce_csrf_checks)
