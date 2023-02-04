# coding=utf-8
from django.utils import translation


class RussianLocaleMiddleware(object):
    def process_request(self, request):
        language = 'ru'
        translation.activate(language)
        request.LANGUAGE_CODE = translation.get_language()
