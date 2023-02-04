from django.conf import settings
from django.contrib.auth.models import AnonymousUser
from django.http import HttpRequest


def prepare_request(method='GET', user=None, page=None):
    """Вернуть объект, похожий на HttpRequest.

    Объект подходит для тестирования и отладки текстовых полей в гридах
    @return: object
    """

    request = HttpRequest()

    setattr(request, 'method', method)
    setattr(request, 'user', user or AnonymousUser())
    org = None

    if settings.IS_BUSINESS and user:
        org = user.orgs.first()

    setattr(request, 'org', org)
    if page:
        setattr(request, 'page', page)
    setattr(request, 'LANGUAGE_CODE', 'en')
    setattr(request, 'user_auth', None)

    return request
