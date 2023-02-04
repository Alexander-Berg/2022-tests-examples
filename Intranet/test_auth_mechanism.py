from django_yauth.authentication_mechanisms.dev import UserFromSettingsAuthBackend
from django_yauth.authentication_mechanisms.base import BaseMechanism


class Mechanism(UserFromSettingsAuthBackend, BaseMechanism):
    """
    Механизм аутентификации для тестов.
    Берёт из настроек логин тестового юзера.
    """
    def extract_params(self, request):
        return {'login': self.fetch_login(request)}
